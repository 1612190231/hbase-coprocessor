package com.luck;

import java.io.IOException;
import java.util.*;

import com.google.common.collect.ImmutableList;
import com.luck.entity.BaseInfo;
import com.luck.entity.THRegionInfo;
import com.luck.entity.THServerInfo;
import com.luck.service.OperateService;
import com.luck.service.impl.OperateServiceImpl;
import com.luck.utils.LogUtil;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessor;
import org.apache.hadoop.hbase.coprocessor.RegionObserver;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.QualifierFilter;
import org.apache.hadoop.hbase.filter.RegexStringComparator;
import org.apache.hadoop.hbase.regionserver.InternalScanner;
import org.apache.hadoop.hbase.regionserver.MiniBatchOperationInProgress;
import org.apache.hadoop.hbase.regionserver.Region;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.wal.WALEdit;

public class observTrigger implements RegionObserver, RegionCoprocessor {
    LogUtil logUtil = new LogUtil();

    /* get， 如果 rowkey为admin,那么不返回数据  */

    @Override
    public Optional<RegionObserver> getRegionObserver() {
        return Optional.of((RegionObserver) this);
    }

    @Override
    public void preGetOp(final ObserverContext<RegionCoprocessorEnvironment> e, final Get get, final List<Cell> results)
            throws IOException {

        if (Bytes.equals(get.getRow(), Bytes.toBytes("admin"))) {

            /*
             * Cell c = CellUtil.createCell(get.getRow(), Bytes.toBytes("cf"),
             * Bytes.toBytes("name"), System.currentTimeMillis(), (byte) 4,
             * Bytes.toBytes("1111"));
             *
             * results.add(c);
             */
            e.bypass();

        }

    }

    @Override
    public void postScannerClose(ObserverContext<RegionCoprocessorEnvironment> ctx, InternalScanner s) throws IOException {
        OperateService operateService = new OperateServiceImpl();
        operateService.setSeries("data");
        operateService.setTableName("track_mine");
        operateService.init();
        OperateService regionService = new OperateServiceImpl();
        regionService.setSeries("data");
        regionService.setTableName("region_performance");
        regionService.init();
        OperateService serverService = new OperateServiceImpl();
        serverService.setSeries("data");
        serverService.setTableName("server_performance");
        serverService.init();

        // 把 分区查询次数、服务器查询次数 维护入价值表中
        List<BaseInfo> baseRegionInfos = new ArrayList<>();
        List<BaseInfo> baseServerInfos = new ArrayList<>();
        List<THServerInfo> thServerInfos = operateService.getInfoForSelect();
        for (THServerInfo thServerInfo: thServerInfos){
            // 插入regionServer性能表
            BaseInfo baseServerInfo = new BaseInfo();
            baseServerInfo.setRowKey(thServerInfo.getServerName().toString());
            baseServerInfo.setColumnFamilyList(new ArrayList<>(Collections.singleton("data")));


            List<Map<String, Object>> serverColumnsList = new ArrayList<>();
            serverColumnsList.add(new HashMap<String, Object>(){{
                put("sumHitCount", thServerInfo.getSumHitCount());
            }});
            baseServerInfo.setColumnsList(serverColumnsList);
            baseServerInfos.add(baseServerInfo);

            // 插入region性能表
            for (THRegionInfo thRegionInfo: thServerInfo.getRegionInfos()){
                BaseInfo baseRegionInfo = new BaseInfo();
                baseRegionInfo.setRowKey(thServerInfo.getServerName().toString().split(",")[0]
                        + ";" + thRegionInfo.getRegionName());
                baseRegionInfo.setColumnFamilyList(new ArrayList<>(Collections.singleton("data")));

                // 插入regionSize、regionHitCount
                List<Map<String, Object>> regionColumnsList = new ArrayList<>();
                Map<String, Object> regionColumn = new HashMap<String, Object>(){{
                    put("regionHitCount", thRegionInfo.getHitCount());
                }};
                regionColumnsList.add(regionColumn);
                baseRegionInfo.setColumnsList(regionColumnsList);

                baseRegionInfos.add(baseRegionInfo);
            }
        }
        regionService.addByListRowKey(baseRegionInfos);
        serverService.addByListRowKey(baseServerInfos);

        logUtil.print("postScan SUCCESS!!!!!!!");
    }

    @Override
    public void postBatchMutate(ObserverContext<RegionCoprocessorEnvironment> c, MiniBatchOperationInProgress<Mutation> miniBatchOp) {
        OperateService operateService = new OperateServiceImpl();
        operateService.setSeries("data");
        operateService.setTableName("track_mine");
        operateService.init();

        // 需要获取三组数据：分区大小、分区查询命中次数、节点分区数量、节点查询命中次数
        List<THServerInfo> thServerInfos = null;
        try {
            thServerInfos = operateService.getInfoForPostPut();
        } catch (IOException e) {
            e.printStackTrace();
        }

        OperateService operateRegionService = new OperateServiceImpl();
        operateRegionService.setSeries("data");
        operateRegionService.setTableName("region_performance");
        operateRegionService.init();
//        operateRegionService.createTable(operateRegionService.getTableName(), operateRegionService.getSeries());

        OperateService operateServerService = new OperateServiceImpl();
        operateServerService.setSeries("data");
        operateServerService.setTableName("server_performance");
        operateServerService.init();
//        operateServerService.createTable(operateServerService.getTableName(), operateServerService.getSeries());

        // 分区时间衰减量需要通过scan去额外查询-二级索引
        logUtil.print("GET TIME......");
        QualifierFilter qualifierFilter = new QualifierFilter(CompareOperator.EQUAL, new RegexStringComparator("decayTime"));
        ResultScanner results = operateRegionService.getByFilter(qualifierFilter);
        Map<String, Object> decayTimeMap = new HashMap<>();
        for (Result result : results) {
            List<Cell> cells = result.listCells();
            for (Cell cell : cells){
                String rowkey = Bytes.toString(cell.getRowArray(),cell.getRowOffset(),cell.getRowLength()); // 获取rowkey
                String familyName = Bytes.toString(cell.getFamilyArray(),cell.getFamilyOffset(),cell.getFamilyLength()); // 获取列族名
                String columnName = Bytes.toString(cell.getQualifierArray(),cell.getQualifierOffset(),cell.getQualifierLength()); // 获取列名
                long value = Bytes.toLong(cell.getValueArray(),cell.getValueOffset(),cell.getValueLength());
                decayTimeMap.put(rowkey, value);
                logUtil.print("数据的rowkey为" + rowkey + "列族名为" + familyName + "列名为" + columnName + "列的值为" + value);
            }
        }

        // 把各数据插入维护的价值表中
        List<BaseInfo> baseRegionInfos = new ArrayList<>();
        List<BaseInfo> baseServerInfos = new ArrayList<>();
        for (THServerInfo thServerInfo: thServerInfos){
            // 插入regionServer性能表
            BaseInfo baseServerInfo = new BaseInfo();
            baseServerInfo.setRowKey(thServerInfo.getServerName().toString());
            baseServerInfo.setColumnFamilyList(new ArrayList<>(Collections.singleton("data")));

            List<Map<String, Object>> serverColumnsList = new ArrayList<>();
            Map<String, Object> serverColumn = new HashMap<String, Object>(){{
                put("regionCount", thServerInfo.getRegionCount());
                put("sumHitCount", thServerInfo.getSumHitCount());
            }};
            serverColumnsList.add(serverColumn);
            baseServerInfo.setColumnsList(serverColumnsList);
            baseServerInfos.add(baseServerInfo);

            // 插入region性能表
            for (THRegionInfo thRegionInfo: thServerInfo.getRegionInfos()){
                BaseInfo baseRegionInfo = new BaseInfo();
                baseRegionInfo.setRowKey(thServerInfo.getServerName().toString().split(",")[0]
                        + ";" + thRegionInfo.getRegionName());
                baseRegionInfo.setColumnFamilyList(new ArrayList<>(Collections.singleton("data")));

                // 插入regionSize、regionHitCount
                List<Map<String, Object>> regionColumnsList = new ArrayList<>();
                Map<String, Object> regionColumn = new HashMap<String, Object>(){{
                    put("regionSize", thRegionInfo.getRegionSize());
                    put("regionHitCount", thRegionInfo.getHitCount());
                }};
                // 插入decayTime
                regionColumn.put("decayTime", decayTimeMap.getOrDefault(thRegionInfo.getRegionName(), (long)(System.currentTimeMillis() / 1000)));
                regionColumnsList.add(regionColumn);
                baseRegionInfo.setColumnsList(regionColumnsList);

                baseRegionInfos.add(baseRegionInfo);
            }
        }
        operateRegionService.addByListRowKey(baseRegionInfos);
        operateServerService.addByListRowKey(baseServerInfos);

        logUtil.print("postBatchPut SUCCESS!!!!!!!");
    }

}