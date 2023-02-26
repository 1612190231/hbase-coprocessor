package com.luck.utils;

/**
 * @author luchengkai
 * @description
 * @date 2022/1/26 13:37
 */

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 使用hbase java api对hbase的特定表按条件进行合并，由于未找到合适的api，所以hbase的region大小从hdfs上获取，如果有java api能实现该功能希望大家告知
 * 阀值：lower_size：小region，直接合并，默认100M
 *     upper_size：大region，按条件合并，默认5G
 *     region_max_size：最大region大小，建议和region split的阀值大小相同，默认10G
 *     least_region_count：最少region个数，默认10个
 * 算法：首先从hdfs的hbase存储路径获得table的region名称和size对应关系的map，然后通过api获取排序好的region实例List，然后遍历该List进行两两合并
 * 规则：（待合并的region定义为A,B两个region）
 * 当前region大小小于upper_size：
 * 如果A region为空，则将当前region赋值给A
 * 如果A region不为空，则将当前region赋值给B，则直接调用api对A,B进行合并，成功之后清空A,B继续遍历
 * 当前region大小大于upper_size：
 * 如果A region为空，放弃处理该region，直接继续遍历
 * 如果A region不为空，并且A的大小小于lower_size或者A+当前的大小小于region_max_size，则将当前region赋值给B，则直接调用api对A,B进行合并，成功之后清空A,B继续遍历
 * @author taochy
 *
 *
 */
public class MergeUtil {

    //小region，直接合并，默认100M
    private static long lower_size = 100 * 1024 * 1024L;
    //大region，按条件合并，默认5G
    private static long upper_size = 5 * 1024 * 1024 * 1024L;
    //最大region大小，建议和region split的阀值大小相同，默认10G
    private static long region_max_size = 10 * 1024 * 1024 * 1024L;
    //最少region个数，默认10个
    private static int least_region_count = 10;


    public static void main(String[] args) {
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.rootdir", "hdfs://hb1:9000/hbase");
        conf.set("hbase.master", "hb1:60000");
        conf.set("hbase.zookeeper.property.clientPort", "2181");
        conf.set("hbase.zookeeper.quorum",
                "hb1:2181,hb2:2181,hb3:2181,hd4:2181,hd5:2181,hd6:2181,hd7:2181,hd8:2181,hd9:2181,hd10:2181,hd11:2181");
        mergeRegion4Table(conf, "staticlog_2018");
    }

    /**
     * 按表进行region的merge
     * @param conf
     * @param tableName
     */
    public static void mergeRegion4Table(Configuration conf, String tableName) {

        FileSystem fs = null;
        conf.set("fs.defaultFS", "hdfs://hb1:9000");
        conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
        Map<String, Long> tableRegionSizeMap = null;
        long currentTimeMillis = 0L;
        long currentTimeMillis2 = 0L;
        int merge_count = 0;

        try {
            fs = FileSystem.get(URI.create("hdfs://hb1:9000"), conf);
            tableRegionSizeMap = getTableRegionSizeMap(fs, tableName);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Connection connection = ConnectionFactory.createConnection(conf);
            Admin admin = connection.getAdmin();
            List<HRegionInfo> tableRegions = admin.getTableRegions(TableName.valueOf(tableName));
            System.out.println("table " + tableName + " has " + tableRegions.size() + " regions!");
            // 合并的执行条件，大于最小的region个数
            if (tableRegions.size() > least_region_count) {
                byte[] regionA = null;
                long sizeA = 0L;
                byte[] regionB = null;
                for (HRegionInfo info : tableRegions) {
                    String regionName = info.getRegionNameAsString();
                    regionName = regionName.substring(regionName.lastIndexOf(".") - 32, regionName.lastIndexOf("."));
                    Long size = tableRegionSizeMap.get(regionName);
                    if (size != 0L) {
                        // region大小小于upper_size
                        if (size < upper_size) {
                            if (regionA == null) {
                                //如果A region为空，则将当前region赋值给A
                                regionA = info.getRegionName();
                                sizeA = size;
                            } else {
                                //如果A region不为空，则将当前region赋值给B，则直接调用api对A,B进行合并，成功之后清空A,B继续遍历
                                regionB = info.getRegionName();
                                currentTimeMillis = System.currentTimeMillis();
                                admin.mergeRegions(regionA, regionB, true);
                                currentTimeMillis2 = System.currentTimeMillis();
                                System.out.println(Bytes.toString(regionA) + " & " + Bytes.toString(regionB)
                                        + "merge cost " + (currentTimeMillis2 - currentTimeMillis) + " milliseconds!");
                                regionA = null;
                                regionB = null;
                                sizeA = 0L;
                                merge_count++;
                            }
                        } else {
                            // region大小大于upper_size
                            if (regionA == null) {
                                //如果A region为空，放弃处理该region，直接继续遍历
                                continue;
                            } else {
                                //如果A region不为空，并且A的大小小于lower_size或者A+当前的大小小于region_max_size，则将当前region赋值给B，则直接调用api对A,B进行合并，成功之后清空A,B继续遍历
                                if (sizeA < lower_size || (sizeA + size) < region_max_size) {
                                    regionB = info.getRegionName();
                                    currentTimeMillis = System.currentTimeMillis();
                                    admin.mergeRegions(regionA, regionB, true);
                                    currentTimeMillis2 = System.currentTimeMillis();
                                    System.out.println(
                                            Bytes.toString(regionA) + " & " + Bytes.toString(regionB) + "merge cost "
                                                    + (currentTimeMillis2 - currentTimeMillis) + " milliseconds!");
                                    regionA = null;
                                    regionB = null;
                                    sizeA = 0L;
                                    merge_count++;
                                } else {
                                    regionA = null;
                                    sizeA = 0L;
                                }
                            }
                        }
                    } else {
                        System.out.println(regionName + "is empty,ignore!");
                    }
                }
                admin.close();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("merge run " + merge_count + " times,continue!");
        // if(merge_count != 0){
        // System.out.println("merge run "+ merge_count +" times,continue!");
        // //递归操作，直到不需要继续合并
        // mergeRegion4Table(conf,tableName);
        // }else {
        // System.out.println("no need to merge any more,finish!");
        // }
    }

    /**
     * 获得table的region名称和size对应关系的map
     * @param fs
     * @param tableName
     * @return
     */
    public static Map<String, Long> getTableRegionSizeMap(FileSystem fs, String tableName) {
        Map<String, Long> tableRegionSizeMap = new HashMap<>();
        String hbase_path = "/hbase/data/";
        if (tableName.contains(":")) {
            String[] split = tableName.split(":");
            hbase_path = hbase_path + split[0] + "/" + split[1];
        } else {
            hbase_path = hbase_path + "default/" + tableName;
        }
        Path table_path = new Path(hbase_path);
        try {
            FileStatus[] files = fs.listStatus(table_path);
            System.out.println(files.length);
            for (FileStatus file : files) {
                String name = file.getPath().getName();
                long length = fs.getContentSummary(file.getPath()).getLength();
                tableRegionSizeMap.put(name, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tableRegionSizeMap;
    }
}
