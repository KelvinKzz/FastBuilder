package com.gaoding.fastbuilder.lib.utils;

public class Log {

    private static boolean sShowLog = true;

    public static void showLog(boolean showLog) {
        sShowLog = showLog;
    }

    public static void i(String s) {
        if (sShowLog) {
            System.out.println("[FastBuilder] " + s);
        }
    }

    public static void e(String s) {
        if (sShowLog) {
            System.out.println("[FastBuilder Error] " + s);
        }
    }

    public static String getStackTraceString(Throwable tr) {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append(tr.getMessage()).append("\n");
            for (StackTraceElement element : tr.getStackTrace()) {
                sb.append(element.toString()).append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
