package com.luck.entity;

import org.apache.hadoop.hbase.ServerName;

import java.io.Serializable;
import java.util.List;

/**
 * @author luchengkai
 * @description regionServer实体类
 * @date 2022/5/20 14:17
 */
public class THServerInfo implements Serializable {
    ServerName ServerName;          //分区服务器名
    String tableName;        // 表名
    List<THRegionInfo> regionInfos;    // 节点下分区信息
    long regionCount;        // 节点分区个数
    long sumHitCount;        // 节点分区的总查询命中次数
    long sumHitTime;         // 节点分区的查询性能

    public ServerName getServerName() {
        return ServerName;
    }

    public void setServerName(ServerName serverName) {
        this.ServerName = serverName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<THRegionInfo> getRegionInfos() {
        return regionInfos;
    }

    public void setRegionInfos(List<THRegionInfo> regionInfos) {
        this.regionInfos = regionInfos;
    }

    public long getRegionCount() {
        return regionCount;
    }

    public void setRegionCount(long regionCount) {
        this.regionCount = regionCount;
    }

    public long getSumHitCount() {
        return sumHitCount;
    }

    public void setSumHitCount(long sumHitCount) {
        this.sumHitCount = sumHitCount;
    }

    public long getSumHitTime() {
        return sumHitTime;
    }

    public void setSumHitTime(long sumHitTime) {
        this.sumHitTime = sumHitTime;
    }
}
