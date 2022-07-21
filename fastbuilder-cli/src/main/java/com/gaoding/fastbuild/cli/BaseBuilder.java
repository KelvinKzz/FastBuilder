package com.gaoding.fastbuild.cli;

import com.gaoding.fastbuilder.lib.utils.FileScanHelper;
import com.gaoding.fastbuilder.lib.utils.FileUtil;
import com.gaoding.fastbuilder.lib.utils.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BaseBuilder {

    protected Map<String, FileScanHelper.FileInfo> mFileMap = new HashMap<>();
    protected Set<String> mCompileList = new HashSet<>();//编译文件
    protected Set<FileScanHelper.FileInfo> mModifyCompileFileList = new HashSet<>();//修改的编译文件
    protected String mFileInfoPath; // 文件消息路径
    protected List<String> mPathList;//java或资源文件路径

    public BaseBuilder(String fileInfoPath, List<String> pathList) {
        mFileInfoPath = fileInfoPath;
        mPathList = pathList;
    }

    protected void preStart() {
        readInfoFile();
    }

    protected void readInfoFile() {
        FileScanHelper helper = new FileScanHelper();
        for (String path : mPathList) {
            if (FileUtil.dirExists(path)) {
                Log.i("扫描" + path);
                scanFile(helper, path);
            }
        }
        //读取收集的资源文件，与现在扫描的文件进行对比
        mFileMap = FileScanHelper.readFile(mFileInfoPath);
        for (FileScanHelper.FileInfo info : helper.pathList) {
            FileScanHelper.FileInfo search = mFileMap.get(info.path);
            if (search == null) {
                Log.i("增加 " + info.path);
                mCompileList.add(info.path);
            } else if (!search.equals(info)) {
                Log.i("修改 " + info.path);
                mCompileList.add(info.path);
                mModifyCompileFileList.add(info);
            }
        }
    }

    protected void scanFile(FileScanHelper helper, String path) {
        helper.scan(path);
    }
}
