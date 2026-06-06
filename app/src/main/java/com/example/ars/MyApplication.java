package com.example.ars;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        scheduleAlertWorker();
    }

    public static void scheduleAlertWorker() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest alertWorkRequest = new PeriodicWorkRequest.Builder(
                AlertWorker.class,
                1, TimeUnit.MINUTES
        )
                .setConstraints(constraints)
                .addTag("alert_worker")
                .build();

        WorkManager.getInstance().enqueueUniquePeriodicWork(
                "alert_work",
                ExistingPeriodicWorkPolicy.REPLACE,
                alertWorkRequest
        );

        Log.d(TAG, "AlertWorker запланирован");
    }
}