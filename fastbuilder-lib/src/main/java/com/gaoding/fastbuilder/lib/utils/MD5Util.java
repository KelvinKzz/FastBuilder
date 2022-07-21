package com.gaoding.fastbuilder.lib.utils;

import java.security.MessageDigest;

public class MD5Util {

    private static final char[] HEX_CHAR = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    /**
     * 获取MD5
     *
     * @param input 字符串
     * @return
     */
    public static String getMd5(String input) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(input.getBytes());
            return toHexString(md5.digest());
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * byte数组转成十六进制字符串
     *
     * @param b
     * @return
     * @throws Exception
     */
    public static String toHexString(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte value : b) {
            sb.append(HEX_CHAR[(value & 0xf0) >>> 4]);
            sb.append(HEX_CHAR[value & 0x0f]);
        }
        return sb.toString();
    }
}