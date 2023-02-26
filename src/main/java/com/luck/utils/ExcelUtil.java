package com.luck.utils;

/**
 * @author luchengkai
 * @description excel操作类
 * @date 2021/4/20 14:11
 */

import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ExcelUtil {

    // 去读Excel的方法readExcel，该方法的入口参数为一个File对象
    public List<List<Object>> readExcel(File file) {
        try {
            // 创建输入流，读取Excel
            InputStream is = new FileInputStream(file.getAbsolutePath());
            // jxl提供的Workbook类
            Workbook wb = Workbook.getWorkbook(is);
            // Excel的页签数量
            List<List<Object>> outerList=new ArrayList<List<Object>>();
            // 每个页签创建一个Sheet对象
            Sheet sheet = wb.getSheet(0);
            // sheet.getRows()返回该页的总行数
            for (int i = 0; i < sheet.getRows(); i++) {
                List<Object> innerList=new ArrayList<Object>();
                // sheet.getColumns()返回该页的总列数
                for (int j = 0; j < sheet.getColumns(); j++) {
                    String cellinfo = sheet.getCell(j, i).getContents();
                    if(cellinfo.isEmpty()){
                        continue;
                    }
                    innerList.add(cellinfo);
//                        System.out.print(cellinfo);
                }
                outerList.add(innerList);
//                    System.out.println();
            }
            return outerList;
        } catch (BiffException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}

