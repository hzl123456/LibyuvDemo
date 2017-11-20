package com.hzl.libyuvdemo;

import android.app.Activity;
import android.app.Application;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 作者：请叫我百米冲刺 on 2017/11/20 上午10:53
 * 邮箱：mail@hezhilin.cc
 */

public class MainApplication extends Application {

    private static MainApplication INSTANCE;

    private static Activity CURRENT_ACTIVITY;

    public static MainApplication getInstance() {
        return INSTANCE;
    }

    public static void setCurrentActivity(Activity activity) {
        CURRENT_ACTIVITY = activity;
    }

    public static Activity getCurrentActivity() {
        return CURRENT_ACTIVITY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
        initExceptionHandler();
    }


    /**
     * 错误进行崩溃处理
     **/
    private void initExceptionHandler() {
        final Thread.UncaughtExceptionHandler dueh = Thread.getDefaultUncaughtExceptionHandler();
        /* 处理未捕捉异常 */
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                FileOutputStream fos = null;
                PrintStream ps = null;
                try {
                    File path = INSTANCE.getExternalCacheDir();
                    if (!path.isDirectory()) {
                        path.mkdirs();
                    }
                    fos = new FileOutputStream(path.getAbsolutePath() + File.separator + "crash_log.txt", true);
                    ps = new PrintStream(fos);
                    ps.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINESE).format(new Date(System.currentTimeMillis())));
                    ex.printStackTrace(ps);
                } catch (FileNotFoundException e) {
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                        }
                    }
                    if (ps != null) {
                        ps.close();
                    }
                }
                dueh.uncaughtException(thread, ex);
            }
        });
    }
}