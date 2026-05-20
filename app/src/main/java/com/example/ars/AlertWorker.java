package com.example.ars;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.ars.api.ApiService;
import com.example.ars.api.RetrofitClient;
import com.example.ars.models.WeatherAlert;
import com.example.ars.utils.SharedPreferencesHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Response;

public class AlertWorker extends Worker {

    public AlertWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(getApplicationContext());
        int userId = prefsHelper.getLastUserId();

        if (userId == -1) {
            return Result.success();
        }

        try {
            ApiService apiService = RetrofitClient.getApiService();
            Response<List<WeatherAlert>> response = apiService.checkAlerts(userId).execute();

            if (response.isSuccessful() && response.body() != null) {
                SharedPreferences prefs = getApplicationContext().getSharedPreferences("GardenAppPrefs", Context.MODE_PRIVATE);
                int lastShownId = prefs.getInt("last_shown_alert_id", -1);
                int maxId = lastShownId;

                for (WeatherAlert alert : response.body()) {
                    if (alert.getId() > lastShownId) {
                        showNotification(alert.getAlertText(), alert.getAlertDate());
                        if (alert.getId() > maxId) {
                            maxId = alert.getId();
                        }
                    }
                }
                prefs.edit().putInt("last_shown_alert_id", maxId).apply();
                return Result.success();
            }
            return Result.retry();
        } catch (Exception e) {
            Log.e("ALERT_WORKER", "Ошибка: " + e.getMessage());
            return Result.retry();
        }
    }

    private void showNotification(String message, String dateFromApi) {
        String channelId = "alerts";
        Context context = getApplicationContext();
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String timeDisplay = "Оповещение";
        if (dateFromApi != null && !dateFromApi.isEmpty()) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date date = inputFormat.parse(dateFromApi);

                SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMMM", Locale.getDefault());
                timeDisplay = outputFormat.format(date);
            } catch (Exception e) {
                Log.e("ALERT_WORKER", "Ошибка формата даты: " + e.getMessage());
                timeDisplay = dateFromApi;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Погодные оповещения", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Внимание: " + timeDisplay)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        manager.notify(1, builder.build());
    }
}