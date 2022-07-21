package com.gaoding.fastbuilder.lib.utils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 文件扫描
 */
public class FileScanHelper {

    public List<FileInfo> pathList = new ArrayList<>();

    private void scan(String root, String[] suffix) {
        scan(root, new File(root), suffix);
    }

    private void scan(String root, File file, String[] suffix) {
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                assert files != null;
                for (File f : files) {
                    scan(root, f, suffix);
                }
            } else {
                if (can(file, suffix)) {
                    pathList.add(new FileInfo(root, file.getAbsolutePath(), file.lastModified()));
                }
            }
        }
    }

    private boolean can(File file, String[] suffix) {
        if (file != null && file.isFile() && suffix != null && suffix.length > 0) {
            for (String s : suffix) {
                if ("*".equals(s)) {
                    return true;
                }
                if (file.getAbsolutePath().endsWith(s)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void scanJava(String root) {
        scan(root, ".java");
    }

    public void scanKt(String root) {
        scan(root, ".kt");
    }

    public void scanJavaAndKotlin(String root) {
        scan(root, new String[]{".java", ".kt"});
    }

    public void scan(String root) {
        scan(root, "*");
    }

    public void scan(String root, String suffix) {
        scan(root, new String[]{suffix});
    }

    public static boolean isEmpty(List list) {
        return list == null || list.size() == 0;
    }

    public static Map<String, FileInfo> readFile(String dataPath) {
        Map<String, FileInfo> map = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(dataPath)))){
            String line;
            line = reader.readLine();
            while (line != null) {
                if (line.equals("")) {
                    line = reader.readLine();
                    continue;
                }
                String[] data = line.split(",");
                if (data.length == 2) {
                    map.put(data[0], new FileInfo("", data[0], Long.parseLong(data[1])));
                }
                line = reader.readLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }


    public static void writeFile(List<FileInfo> list, String out) {
        if (list == null || list.isEmpty()) {
            return;
        }
        StringBuffer sb = new StringBuffer();
        for (FileInfo info : list) {
            sb.append(info.path).append(",")
                    .append(info.lastModified).append("\n");
        }
        FileUtil.writeFile(out, sb.toString());
    }

    public static void safeClose(Closeable reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static class FileInfo {
        public String root;
        public String path;
        public long lastModified;

        public FileInfo(String root, String path, long lastModified) {
            this.root = root;
            this.path = path;
            this.lastModified = lastModified;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FileInfo fileInfo = (FileInfo) o;
            return lastModified == fileInfo.lastModified && Objects.equals(path, fileInfo.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path, lastModified);
        }
    }


}
