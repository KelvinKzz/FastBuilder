package com.gaoding.fastbuilder.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * 扫描java--》扫描缓存class--》增量编译class--》打包dex（dex/cache/classes）-->推倒sd卡--》重启app
 */
class HotDexTask : DefaultTask() {

    @TaskAction
    fun generateHotDex() {

    }

    @Override
    override fun getGroup():String {
        return "HotDex"
    }

    @Override
    override fun getDescription():String {
        return "增量编译Dex"
    }

}