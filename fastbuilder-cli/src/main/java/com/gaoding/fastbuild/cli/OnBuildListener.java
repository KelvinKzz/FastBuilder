package com.gaoding.fastbuild.cli;

/**
 * @description: Created by zhisui on 2022/3/22
 * E-Mail Address: zhisui@gaoding.com
 */
public interface OnBuildListener {
    int BUILD_TYPE_DEX = 0;
    int BUILD_TYPE_RES = 1;

    void onBuildFinish(int buildType, boolean hasCompileFile);
}
