package com.example.mediaplayer.helpers;

import android.app.Application;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BaseApplication extends Application {
    /*
     * Gets the number of available cores
     * (not always the same as the maximum number of cores)
     */
    private static final int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();

    // Instantiates the queue of Runnables as a LinkedBlockingQueue
    private static final BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>();

    // Sets the amount of time an idle thread waits before terminating
    private static final int KEEP_ALIVE_TIME = 1;
    // Sets the Time Unit to seconds
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

    private static ExecutorService executorService;

    public static ExecutorService getExecutorService(){
        if (executorService == null){
            executorService = new ThreadPoolExecutor(
                    NUMBER_OF_CORES,       // Initial pool size
                    NUMBER_OF_CORES,       // Max pool size
                    KEEP_ALIVE_TIME,
                    KEEP_ALIVE_TIME_UNIT,
                    workQueue
            );
        }
        return executorService;
    }
}
