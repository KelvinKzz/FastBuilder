package com.gaoding.fastbuilder.lib.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtil {

    public static void changeResApk(String apkPath, String apkResPath) {
        String destDirPath = apkPath + "_out";
        unZip(apkPath, destDirPath);
        zip(destDirPath, apkResPath);
    }

    public static List<String> unZip(String inputFile, String destDirPath) {
        Log.i("unZip inputFile " + inputFile);
        Log.i("unZip destDirPath " + destDirPath);
        File srcFile = new File(inputFile);//获取当前压缩文件
        // 判断源文件是否存在
        if (!srcFile.exists()) {
            return null;
        }
        List<String> pathList = new ArrayList<>();
        //开始解压
        //构建解压输入流
        ZipInputStream zIn = null;
        OutputStream out = null;
        BufferedOutputStream bos = null;
        try {
            zIn = new ZipInputStream(new FileInputStream(srcFile));
            ZipEntry entry = null;
            File file = null;
            while ((entry = zIn.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    file = new File(destDirPath, entry.getName());
                    if (!file.exists()) {
                        new File(file.getParent()).mkdirs();//创建此文件的上级目录
                    }
                    out = new FileOutputStream(file);
                    bos = new BufferedOutputStream(out);
                    int len = -1;
                    byte[] buf = new byte[2048];
                    while ((len = zIn.read(buf)) != -1) {
                        bos.write(buf, 0, len);
                    }
                    FileUtil.safeClose(bos);
                    FileUtil.safeClose(out);
                    pathList.add(file.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileUtil.safeClose(bos);
            FileUtil.safeClose(out);
            FileUtil.safeClose(zIn);
        }
        return pathList;
    }

    /**
     * 递归压缩目录结构
     *
     * @param zipOutputStream
     * @param file
     * @param parentFileName
     */
    private static void directory(ZipOutputStream zipOutputStream, File file, String parentFileName) {
        if (file == null || !file.exists()) {
            return;
        }
        if (!file.isDirectory()) {
            zipFile(zipOutputStream, file, parentFileName);
            return;
        }
        File[] files = file.listFiles();
        if (files != null) {
            for (File fileTemp : files) {
                String parentFileNameTemp = StringUtil.isEmpty(parentFileName) ? fileTemp.getName() : parentFileName + "/" + fileTemp.getName();
                directory(zipOutputStream, fileTemp, parentFileNameTemp);
            }
        }
    }

    private static void zipFile(ZipOutputStream zos, File file, String fileName) {
        FileInputStream in = null;
        try {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zos.putNextEntry(zipEntry);
            in = new FileInputStream(file);
            startCopy(zos, in);
            zos.closeEntry();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileUtil.safeClose(in);
        }
    }

    public static void zip(String zipPath, String path) {
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new FileOutputStream(zipPath));
            zos.setComment("Bale tdp!");
            zos.setLevel(Deflater.BEST_COMPRESSION);
            zos.setMethod(Deflater.DEFLATED);

            //文件
            directory(zos, new File(path), "");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            FileUtil.safeClose(zos);
        }
    }

    //追加
    public static void append(String zipPath, String newZipPath, List<String> appendFileList) {
        ZipOutputStream zos = null;
        InputStream input = null;
        File newCompressedFile = new File(newZipPath);
        if (newCompressedFile.exists()) {
            newCompressedFile.delete();
        }
        try {
            ZipFile compressedFile = new ZipFile(zipPath);
            zos = new ZipOutputStream(new FileOutputStream(newZipPath));
            zos.setComment("Bale tdp!");
            zos.setLevel(Deflater.BEST_COMPRESSION);
            zos.setMethod(Deflater.DEFLATED);
            //
            for (String appendFile : appendFileList) {
                if (!"".equals(appendFile)) {
                    File f = new File(appendFile);
                    ZipEntry fileEntry = new ZipEntry(f.getName());
                    zos.putNextEntry(fileEntry);
                    input = new FileInputStream(f);
                    startCopy(zos, input);
                }
            }
            Enumeration<? extends ZipEntry> e = compressedFile.entries();
            while (e.hasMoreElements()) {
                ZipEntry entry = e.nextElement();
                zos.putNextEntry(entry);
                if (!entry.isDirectory()) {
                    startCopy(zos, compressedFile.getInputStream(entry));
                }
                zos.closeEntry();
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            FileUtil.safeClose(zos);
        }
    }

    public static void startCopy(ZipOutputStream zos, InputStream input) {
        try {
            byte[] buf = new byte[2048];
            int len;
            while ((len = input.read(buf)) > 0) {
                zos.write(buf, 0, len);
            }
            zos.closeEntry();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            FileUtil.safeClose(input);
        }
    }

    public static void ZipFolder(String srcFileString, String zipFile) {
        //创建ZIP
        try {
            ZipOutputStream outZip = new ZipOutputStream(new FileOutputStream(zipFile));
            //创建文件
            File file = new File(srcFileString);
            //压缩
            ZipFiles(file.getParent() + File.separator, file.getName(), outZip);
            //完成和关闭
            outZip.finish();
            outZip.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void ZipFiles(String folderString, String fileString, ZipOutputStream zipOutputSteam) {
        try {
            if (zipOutputSteam == null)
                return;
            File file = new File(folderString + fileString);
            if (file.isFile()) {
                ZipEntry zipEntry = new ZipEntry(fileString);
                FileInputStream inputStream = new FileInputStream(file);
                zipOutputSteam.putNextEntry(zipEntry);
                int len;
                byte[] buffer = new byte[4096];
                while ((len = inputStream.read(buffer)) != -1) {
                    zipOutputSteam.write(buffer, 0, len);
                }
                zipOutputSteam.closeEntry();
            } else {
                //文件夹
                String fileList[] = file.list();
                //没有子文件和压缩
                if (fileList.length <= 0) {
                    ZipEntry zipEntry = new ZipEntry(fileString + File.separator);
                    zipOutputSteam.putNextEntry(zipEntry);
                    zipOutputSteam.closeEntry();
                }
                //子文件和递归
                for (int i = 0; i < fileList.length; i++) {
                    ZipFiles(folderString + fileString + "/", fileList[i], zipOutputSteam);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //追加dex,so等文件
    public static void appendApk(String zipPath, String newZipPath, List<String> dexList, List<String> soList, String assets) {
        ZipOutputStream zos = null;
        File newCompressedFile = new File(newZipPath);
        if (newCompressedFile.exists()) {
            newCompressedFile.delete();
        }
        ZipFile compressedFile = null;
        try {
            compressedFile = new ZipFile(zipPath);
            zos = new ZipOutputStream(new FileOutputStream(newZipPath));
            zos.setComment("Bale tdp!");
            zos.setLevel(Deflater.BEST_COMPRESSION);
            zos.setMethod(Deflater.DEFLATED);
            //增加dex
            int index = 1;
            for (String appendFile : dexList) {
                if (!"".equals(appendFile)) {
                    File f = new File(appendFile);
                    String dexName = "classes.dex";
                    if (index > 1) {
                        dexName = "classes" + index + ".dex";
                    }
                    index++;
                    zipFile(zos, f, dexName);
                }
            }
            //增加so
            List<String> soAddList = new ArrayList<>();
            for (String appendFile : soList) {
//                Log.i("so:" + appendFile);
                File f = new File(appendFile);
                String name = "lib" + "/" + f.getParentFile().getName() + "/" + f.getName();//符号分享有影响
//                Log.i(name);
                if (!soAddList.contains(name)) {
                    soAddList.add(name);//去重
                    zipFile(zos, f, name);
                }
            }

            //文件 assets
            directory(zos, new File(assets), "assets");

            //原来的res
            Enumeration<? extends ZipEntry> e = compressedFile.entries();
            while (e.hasMoreElements()) {
                ZipEntry entry = e.nextElement();
                zos.putNextEntry(entry);
                if (!entry.isDirectory()) {
                    startCopy(zos, compressedFile.getInputStream(entry));
                }
                zos.closeEntry();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            FileUtil.safeClose(zos);
            FileUtil.safeClose(compressedFile);
        }
    }

    public static boolean checkZipFile(String path, String name) {
        ZipEntry zipEntry = null;
        FileInputStream fileInputStream = null;
        ZipInputStream zipInputStream = null;
        boolean isOk = false;
        try {
            Pattern pattern = Pattern.compile(name);
            File file = new File(path);
            if (file.exists()) { //判断文件是否存在
                fileInputStream = new FileInputStream(path);
                zipInputStream = new ZipInputStream(fileInputStream, Charset.forName("GBK"));
                while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                    Matcher matcher = pattern.matcher(zipEntry.getName());
                    if (matcher.find()) {
                        Log.i("** ok ** " + zipEntry.getName());
                        isOk = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileUtil.safeClose(fileInputStream);
            FileUtil.safeClose(zipInputStream);
        }
        Log.i("** isOk ** " + isOk);
        return isOk;
    }

}
