package com.luck.entity;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author luchengkai
 * @description region实体类
 * @date 2022/5/20 14:17
 */
public class THRegionInfo implements Serializable {
    String regionName;  //分区名
    String tableNmae;   // 表名
    long regionSize;    // 分区大小
    long decayTime;     // 衰减时间量
    long hitCount;      // 分区查询命中次数

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getTableNmae() {
        return tableNmae;
    }

    public void setTableNmae(String tableNmae) {
        this.tableNmae = tableNmae;
    }

    public long getRegionSize() {
        return regionSize;
    }

    public void setRegionSize(long regionSize) {
        this.regionSize = regionSize;
    }

    public long getDecayTime() {
        return decayTime;
    }

    public void setDecayTime(long decayTime) {
        this.decayTime = decayTime;
    }

    public long getHitCount() {
        return hitCount;
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }
}
