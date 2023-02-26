package com.luck.utils;

import java.nio.ByteBuffer;

public class ByteUtil {
    /**
     * 将byte格式数据转换成long
     * @param b  byte格式数据
     * @return long格式数据
     */
    public long convertBytesToLong(byte[] b) {
        ByteBuffer buf = ByteBuffer.wrap(b);
        return buf.getLong();
    }

    /**
     * 将long格式数据转换成byte
     * @param l  long格式数据
     * @return byte格式数据
     */
    public byte[] convertLongToBytes(long l) {
        ByteBuffer buf = ByteBuffer.allocate(Long.SIZE);
        buf.putLong(0, l);
        return buf.array();
    }

    /**
     * 将两个byte数组合并为一个
     * @param data1  要合并的数组1
     * @param data2  要合并的数组2
     * @return 合并后的新数组
     */
    public byte[] mergeBytes(byte[] data1, byte[] data2) {
        byte[] data3 = new byte[data1.length + data2.length];
        System.arraycopy(data1, 0, data3, 0, data1.length);
        System.arraycopy(data2, 0, data3, data1.length, data2.length);
        return data3;
    }
}
