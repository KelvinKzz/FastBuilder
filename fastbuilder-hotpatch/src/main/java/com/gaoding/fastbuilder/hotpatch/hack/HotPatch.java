package com.gaoding.fastbuilder.hotpatch.hack;

import com.gaoding.fastbuilder.hotpatch.util.LogUtil;
import com.gaoding.fastbuilder.hotpatch.util.ReflectUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import dalvik.system.DexClassLoader;

/**
 * Created by hp on 2016/4/11.
 */
public class HotPatch {

    public static boolean dexInject() {
        File apkFile = new File(HotPatchApplication.getContext().getExternalCacheDir(), "patch_dex.jar");
        if (!apkFile.exists()) {
            return false;
        }
        File patchDir = HotPatchApplication.getContext().getDir("patchDir", 0);
        File patchJar = new File(patchDir, "patch_dex.jar");
        try {
            copyFile(apkFile.getAbsolutePath(), patchJar.getAbsolutePath());
            inject(patchJar.getAbsolutePath());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            LogUtil.e("patch_dex.jar 注入失败" + e.toString());
        }

        return false;
    }

    public static void copyFile(String src, String destFilePath) throws IOException {
        FileOutputStream out = new FileOutputStream(destFilePath);

        FileInputStream in = new FileInputStream(src);

        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }


    public static Object getObject() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        Class<?> cl = Class.forName("dalvik.system.BaseDexClassLoader");
        Object pathList = ReflectUtil.getField(cl, "pathList", HotPatchApplication.getContext().getClassLoader());
        return ReflectUtil.getField(pathList.getClass(), "dexElements", pathList);
    }

    public static void inject(String path) {
        File file = new File(path);
        if (file.exists()) {
            try {
                // 获取classes的dexElements
                Class<?> cl = Class.forName("dalvik.system.BaseDexClassLoader");
                Object pathList = ReflectUtil.getField(cl, "pathList", HotPatchApplication.getContext().getClassLoader());
                Object baseElements = ReflectUtil.getField(pathList.getClass(), "dexElements", pathList);

                // 获取patch_dex的dexElements（需要先加载dex）
                String dexopt = HotPatchApplication.getContext().getDir("dexopt", 0).getAbsolutePath();
                DexClassLoader dexClassLoader = new DexClassLoader(path, dexopt, dexopt, HotPatchApplication.getContext().getClassLoader());
                Object obj = ReflectUtil.getField(cl, "pathList", dexClassLoader);
                Object dexElements = ReflectUtil.getField(obj.getClass(), "dexElements", obj);

                // 合并两个Elements
                Object combineElements = ReflectUtil.combineArray(dexElements, baseElements);

                // 将合并后的Element数组重新赋值给app的classLoader
                ReflectUtil.setField(pathList.getClass(), "dexElements", pathList, combineElements);

            } catch (Exception e) {
                e.printStackTrace();
                LogUtil.e("注入失败");
            }
        } else {
            LogUtil.e(file.getAbsolutePath() + "does not exists");
        }
    }
}
