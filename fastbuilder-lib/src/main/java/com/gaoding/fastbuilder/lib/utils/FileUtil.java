package com.gaoding.fastbuilder.lib.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FileUtil {
    public static int BUFFER_SIZE = 16384;

    public static void writeFile(Collection<String> list, String out) {
        if (CollectUtil.isEmpty(list)) {
            return;
        }
        StringBuffer sb = new StringBuffer();
        for (String file : list) {
            sb.append(file).append("\n");
        }
        writeFile(out, sb.toString());
    }

    public static void writeFile(String out, String s) {
        FileOutputStream file = null;
        try {
            file = new FileOutputStream(out);
            file.write(s.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            safeClose(file);
        }
    }

    public static boolean ensumeDir(String path) {
        return ensumeDir(new File(path));
    }

    public static boolean ensumeDir(File file) {
        if (file == null) {
            return false;
        }
        if (!fileExists(file.getAbsolutePath())) {
            return file.mkdirs();
        }
        return true;
    }

    public static boolean ensumeFile(String path) {
        return ensumeFile(new File(path));
    }

    public static boolean ensumeFile(File file) {
        if (file == null) {
            return false;
        }
        if (!fileExists(file.getAbsolutePath())) {
            try {
                return file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public static boolean fileExists(String filePath) {
        if (filePath == null) {
            return false;
        }

        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            return true;
        }
        return false;
    }

    public static boolean dirExists(String filePath) {
        if (filePath == null) {
            return false;
        }

        File file = new File(filePath);
        if (file.exists() && file.isDirectory()) {
            return true;
        }
        return false;
    }

    public static boolean deleteFile(String filePath) {
        if (filePath == null) {
            return true;
        }

        File file = new File(filePath);
        if (file.exists()) {
            return file.delete();
        }
        return true;
    }

    public static boolean deleteFile(File file) {
        if (file == null) {
            return true;
        }
        if (file.exists()) {
            return file.delete();
        }
        return true;
    }

    public static boolean isLegalFile(File file) {
        if (file == null) {
            return false;
        }
        return file.exists() && file.isFile() && file.length() > 0;
    }

    public static long getFileSizes(File f) {
        if (f == null) {
            return 0;
        }
        long size = 0;
        if (f.exists() && f.isFile()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(f);
                size = fis.available();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                safeClose(fis);
            }
        }
        return size;
    }

    public static boolean deleteDir(String path) {
        return deleteDir(new File(path));
    }

    public static boolean deleteDir(File file) {
        if (file == null || (!file.exists())) {
            return false;
        }
        if (file.isFile()) {
            file.delete();
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                deleteDir(files[i]);
            }
        }
        file.delete();
        return true;
    }

    public static void cleanDir(File dir) {
        if (dir.exists()) {
            FileUtil.deleteDir(dir);
            dir.mkdirs();
        }
    }

    public static void copyResourceUsingStream(String name, File dest) throws IOException {
        FileOutputStream os = null;
        File parent = dest.getParentFile();
        if (parent != null && (!parent.exists())) {
            parent.mkdirs();
        }
        InputStream is = null;

        try {
            is = FileUtil.class.getResourceAsStream("/" + name);
            os = new FileOutputStream(dest, false);

            byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            safeClose(os);
            safeClose(is);
        }
    }

    public static void copyFileUsingStream(File source, File dest) throws IOException {
        FileInputStream is = null;
        FileOutputStream os = null;
        File parent = dest.getParentFile();
        if (parent != null && (!parent.exists())) {
            parent.mkdirs();
        }
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest, false);

            byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            dest.setLastModified(source.lastModified());
        } finally {
            safeClose(os);
            safeClose(is);
        }
    }

    public static void write2file(byte[] content, File dest) throws IOException {
        FileOutputStream os = null;
        File parent = dest.getParentFile();
        if (parent != null && (!parent.exists())) {
            parent.mkdirs();
        }
        try {
            os = new FileOutputStream(dest, false);
            os.write(content);
        } finally {
            safeClose(os);
        }
    }

    public static String readContents(String path) {
        if (fileExists(path)) {
            try {
                return new String(readContents(new File(path)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static byte[] readContents(File file) throws IOException {
        int bufferSize = BUFFER_SIZE;
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            int length;
            byte[] buffer = new byte[bufferSize];
            byte[] bufferCopy;
            while ((length = bis.read(buffer, 0, bufferSize)) != -1) {
                bufferCopy = new byte[length];
                System.arraycopy(buffer, 0, bufferCopy, 0, length);
                output.write(bufferCopy);
            }
            return output.toByteArray();
        }
    }

    public static byte[] readStream(InputStream is) throws IOException {
        int bufferSize = BUFFER_SIZE;
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             BufferedInputStream bIn = new BufferedInputStream(is)) {
            int length;
            byte[] buffer = new byte[bufferSize];
            byte[] bufferCopy;
            while ((length = bIn.read(buffer, 0, bufferSize)) != -1) {
                bufferCopy = new byte[length];
                System.arraycopy(buffer, 0, bufferCopy, 0, length);
                output.write(bufferCopy);
            }
            return output.toByteArray();
        }
    }


    public static void scanDir(File file, List<String> pathList) {
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (File f : files) {
                    scanDir(f, pathList);
                }
            } else {
                pathList.add(file.getAbsolutePath());
            }
        }
    }

    /**
     * @param dataPath 文件路径
     * @return
     */
    public static List<String> getStrings(String dataPath) {
        List<String> singerList = new ArrayList<>();
        if (!fileExists(dataPath)) {
            Log.i("file no Exists : " + dataPath);
            return singerList;
        }
        InputStream is;
        BufferedReader reader = null;
        try {
            is = new FileInputStream(dataPath);

            String line;
            reader = new BufferedReader(new InputStreamReader(is));
            line = reader.readLine();
            int count = 0;
            while (line != null) {
                singerList.add(line);
                line = reader.readLine();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            safeClose(reader);
        }
        return singerList;
    }

    public static void safeClose(Closeable reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //复制方法
    public static void copy(String src, String des) throws Exception {
        //初始化文件复制
        File file1 = new File(src);
        //把文件里面内容放进数组
        File[] fs = file1.listFiles();
        //初始化文件粘贴
        File file2 = new File(des);
        //判断是否有这个文件有不管没有创建
        if (!file2.exists()) {
            file2.mkdirs();
        }
        //遍历文件及文件夹
        for (File f : fs) {
            if (f.isFile()) {
                //文件
                fileCopy(f.getPath(), des + "/" + f.getName()); //调用文件拷贝的方法
            } else if (f.isDirectory()) {
                //文件夹
                copy(f.getPath(), des + "/" + f.getName());//继续调用复制方法      递归的地方,自己调用自己的方法,就可以复制文件夹的文件夹了
            }
        }

    }

    /**
     * 文件复制的具体方法
     */
    public static void fileCopy(String src, String des) {
        //io流固定格式
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(src);
            fos = new FileOutputStream(des);
            bis = new BufferedInputStream(fis);
            bos = new BufferedOutputStream(fos);
            int i = -1;//记录获取长度
            byte[] bt = new byte[2014];//缓冲区
            while ((i = bis.read(bt)) != -1) {
                bos.write(bt, 0, i);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileUtil.safeClose(bis);
            FileUtil.safeClose(bos);
            FileUtil.safeClose(fis);
            FileUtil.safeClose(fos);
        }
    }

    /**
     * 文件复制的具体方法
     */
    public static void fileCopy(File src, File des) throws Exception {
        if (!des.getParentFile().exists()) {
            des.getParentFile().mkdirs();
        }
        //io流固定格式
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(src));
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(des));
        int i = -1;//记录获取长度
        byte[] bt = new byte[16384];//缓冲区
        while ((i = bis.read(bt)) != -1) {
            bos.write(bt, 0, i);
        }
        safeClose(bis);
        safeClose(bos);
    }

    public static void writeAppend(String path, String conent) {
        BufferedWriter out = null;
        try {
            File file = new File(path);
            if (!file.exists()) {
                file.createNewFile();
            }

            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path, true)));
            out.write(conent + "\r\n");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            safeClose(out);
        }
    }
}
