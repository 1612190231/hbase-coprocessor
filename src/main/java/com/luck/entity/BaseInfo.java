package com.luck.entity;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author luchengkai
 * @description k-v实体类
 * @date 2021/4/22 14:17
 */
public class BaseInfo implements Serializable {
    String rowKey;          //key
    List<String> columnFamilyList;    //列族
    List<Map<String, Object>> columnsList;    //值

    public String getRowKey() { return rowKey; }

    public void setRowKey(String rowKey) { this.rowKey = rowKey; }

    public List<String> getColumnFamilyList() { return columnFamilyList; }

    public void setColumnFamilyList(List<String> columnFamilyList) { this.columnFamilyList = columnFamilyList; }

    public List<Map<String, Object>> getColumnsList() { return columnsList; }

    public void setColumnsList(List<Map<String, Object>> columnsList) { this.columnsList = columnsList; }
}
