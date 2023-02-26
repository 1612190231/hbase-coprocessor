package com.luck.service;

import com.luck.entity.BaseInfo;
import com.luck.entity.THServerInfo;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.RegionLoad;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.filter.Filter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author luchengkai
 * @description Hbase接口类
 * @date 2021/4/20 13:52
 */
public interface OperateService {

    String getSeries();

    void setSeries(String series);

    String getTableName();

    void setTableName(String tableName);

    List<ServerName> getServerNames();

    void setServerNames() throws IOException;

    // 初始化
    void init();

    //创建表
    void createTable(String tableName, String seriesStr);

    // 创建表 - 预分区
    void createTable(String tableName, String seriesStr, byte[][] startKey);

    //添加数据---按列族单条
    void add(String columnFamily, String rowKey, Map<String, Object> columns);

    //添加数据---全表
    void addByRowKey(List<BaseInfo> baseInfos);

    //添加数据---全表
    void addByListRowKey(List<BaseInfo> baseInfo);

    //根据rowkey获取数据
    Map<String, String> getByRowKey(String rowKey);

    //根据rowkey和column获取数据
    String getBySeries(String rowKey, String column);

    //根据table查询所有数据
    ResultScanner getByTable();

    //根据filter筛选数据
    ResultScanner getByFilter(Filter filter);

    //删除表
    void dropTable(String tableName);

    // 获取分区数据-postPut
    List<THServerInfo> getInfoForPostPut() throws IOException;

    // 获取查询命中次数数据
    List<THServerInfo> getInfoForSelect() throws IOException;

    // 获取分区数据-postScan
    List<THServerInfo> getInfoForPostScan() throws IOException;

    // 计算分区价值
    long calculateRegionValue(Long rowKey) throws IOException;

    // 计算分区价值
    long calculateRegionValue(HRegionInfo hRegionInfo) throws IOException;

    //    // 查询regions
    List<HRegionInfo> getRegions() throws IOException;
    //
//    // 统计region的大小
    Map<String, RegionLoad> getRegionLoad(List<ServerName> serverNames) throws IOException;

}
