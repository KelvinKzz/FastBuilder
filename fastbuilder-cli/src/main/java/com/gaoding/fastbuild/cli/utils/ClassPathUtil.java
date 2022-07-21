package com.gaoding.fastbuild.cli.utils;

import com.gaoding.fastbuilder.lib.utils.FileUtil;
import com.gaoding.fastbuilder.lib.utils.Log;
import com.gaoding.fastbuilder.lib.utils.ZipUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Enumeration;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ClassPathUtil {

    public static boolean checkHasFile(String zipPath, Set<String> list) {
        ZipFile zipFile = null;
        try {
            //指定编码，否则压缩包里面不能有中文目录
            zipFile = new ZipFile(zipPath);
            for (Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements(); ) {
                ZipEntry entry = entries.nextElement();
//                Log.i(entry.getName());
                if (hasClassFile(list, entry.getName())) {
                    return true;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileUtil.safeClose(zipFile);
        }
        return false;
    }

    public static void zipRemoveClass(String zipPath, String outPath, Set<String> list) {
        ZipFile zipFile = null;
        ZipOutputStream zos = null;
        try {
            //指定编码，否则压缩包里面不能有中文目录
            zipFile = new ZipFile(zipPath);
            zos = new ZipOutputStream(new FileOutputStream(outPath));
            for (Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements(); ) {
                ZipEntry entry = entries.nextElement();
//                Log.i(entry.getName());
                if (!hasClassFile(list, entry.getName())) {
                    zos.putNextEntry(entry);
                    ZipUtil.startCopy(zos, zipFile.getInputStream(entry));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileUtil.safeClose(zipFile);
            FileUtil.safeClose(zos);
        }
    }

    private static boolean hasClassFile(Set<String> list, String className) {
        if (className.endsWith(".class")) {
            int index = className.indexOf("$");
            if (index == -1) {
                index = className.lastIndexOf(".");
            }
            String clazz = className.substring(0, index);
            for (String name : list) {
                if (clazz.endsWith(name) || clazz.endsWith(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean fileRemoveClass(File file, Set<String> list) {
        if (file == null || (!file.exists())) {
            return false;
        }
//        Log.i("deleteDir:" + file.getAbsolutePath());
        if (file.isFile()) {
            if (hasClassFile(list, file.getAbsolutePath())) {
                Log.i("remove file class :" + file.getAbsolutePath());
                file.delete();
            }
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                fileRemoveClass(files[i], list);
            }
        }
        return true;
    }
}
