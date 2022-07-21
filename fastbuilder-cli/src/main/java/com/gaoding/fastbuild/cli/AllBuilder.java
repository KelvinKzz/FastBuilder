package com.gaoding.fastbuild.cli;

import com.gaoding.fastbuild.cli.utils.BuildUtils;
import com.gaoding.fastbuilder.lib.utils.Log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @description: Created by zhisui on 2022/3/22
 * E-Mail Address: zhisui@gaoding.com
 */
public class AllBuilder {

    public static void main(String[] args) throws Exception {
        BuildUtils.initConfig();
        new AllBuilder().start();
    }

    private AtomicBoolean hasCompile = new AtomicBoolean(false);

    public AllBuilder() {

    }

    public void start() {
        long start = System.currentTimeMillis();
        CountDownLatch countDownLatch = new CountDownLatch(2);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new DexBuilder().start(new OnBuildListener() {
                        @Override
                        public void onBuildFinish(int buildType, boolean hasCompileFile) {
                            hasCompile.compareAndSet(false, hasCompileFile);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                countDownLatch.countDown();
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new ResAapt2Builder().start(new OnBuildListener() {
                        @Override
                        public void onBuildFinish(int buildType, boolean hasCompileFile) {
                            hasCompile.compareAndSet(false, hasCompileFile);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                countDownLatch.countDown();
            }
        }).start();
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (!hasCompile.get()) {
            Log.i("编译完成 未检测到修改");
            return;
        }
        Log.i("编译完成 检测到修改 重新启动应用");
        Log.i("增量完成时间：" + (System.currentTimeMillis() - start) / 1000 + "秒");
        Main.restart();
    }

}
