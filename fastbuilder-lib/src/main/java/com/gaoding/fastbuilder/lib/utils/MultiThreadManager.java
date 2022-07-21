package com.gaoding.fastbuilder.lib.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

public class MultiThreadManager<T> {

    private List<Runnable<T>> mTaskList = new ArrayList<>();

    public void addTask(Runnable<T> task) {
        mTaskList.add(task);
    }

    public List<T> start() {
        if (mTaskList.isEmpty()) {
            Log.i("无任务停止");
            return null;
        }
        long start = System.currentTimeMillis();
        //创建执行线程池
        ForkJoinPool pool = new ForkJoinPool();

        //创建任务
        List<SubTask<T>> list = new ArrayList<>();
        for (Runnable<T> runnable : mTaskList) {
            SubTask<T> task = new SubTask<>(runnable);
            list.add(task);
        }
        //提交任务
        AllTask<T> task = new AllTask<>(list);
        ForkJoinTask<List<T>> result = pool.submit(task);
        try {
            List<T> l = result.get();
            return l;
        } catch (Exception e) {
            e.printStackTrace();
        }
        pool.shutdown();
        return null;
    }

    static class SubTask<T> extends RecursiveTask<T> {

        Runnable<T> task;

        public SubTask(Runnable<T> task) {
            this.task = task;
        }

        @Override
        protected T compute() {
            return task.run();
        }
    }

    static class AllTask<T> extends RecursiveTask<List<T>> {
        List<SubTask<T>> list;

        public AllTask(List<SubTask<T>> list) {
            this.list = list;
        }

        @Override
        protected List<T> compute() {
            List<T> data = new ArrayList<>();
            for (SubTask<T> task : list) {
                task.fork();
            }
            for (SubTask<T> task : list) {
                data.add(task.join());
            }
            return data;
        }
    }

    public interface Runnable<T> {
        T run();
    }
}
