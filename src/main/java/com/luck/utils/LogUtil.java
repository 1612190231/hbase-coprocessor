package com.luck.utils;

import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author luchengkai
 * @description 日志处理类
 * @date 2021/4/22 16:29
 */
public class LogUtil {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public void runTimeLog(String type, long endTime, long startTime){
        logger.info(type + " project runtime: "+(endTime-startTime)+"ms");
    }

    public void prepareLog(List list){
        String info = String.join(",", list);
        logger.info("excelData: " + info);
    }

//    public void getValueByTable(ResultScanner rs){
//        for (Result r : rs) {
//            logger.info("rowKey: " + new String(r.getRow()));
//            for (KeyValue keyValue : r.raw()) {
//                logger.info("columnFamily: " + new String(keyValue.getFamily()) + "====value: " + new String(keyValue.getValue()));
//            }
//        }
//    }

    public void print(String s, Object... args) {
        logger.info(s, args);
    }
}
