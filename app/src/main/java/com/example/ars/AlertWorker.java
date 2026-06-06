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
import com.example.ars.models.UserCrop;
import com.example.ars.models.WeatherAlert;
import com.example.ars.models.WeatherData;
import com.example.ars.models.WeatherResponse;
import com.example.ars.utils.SharedPreferencesHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import retrofit2.Response;

public class AlertWorker extends Worker {

    private static final String TAG = "ALERT_WORKER";
    private static final String CHANNEL_ID = "weather_alerts";
    private static final long NOTIFICATION_COOLDOWN_MS = TimeUnit.HOURS.toMillis(2);
    private static final String PREFS_NAME = "AlertPrefs";

    public AlertWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        return checkAndNotify();
    }

    // Публичный метод для принудительной проверки из Activity
    public Result checkAndNotify() {
        SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(getApplicationContext());

        if (prefsHelper.getUser() == null) {
            Log.d(TAG, "Пользователь не авторизован");
            return Result.success();
        }

        int userId = prefsHelper.getUser().getId();
        Log.d(TAG, "=== ЗАПУСК ПРОВЕРКИ УВЕДОМЛЕНИЙ для пользователя: " + userId + " ===");

        try {
            ApiService apiService = RetrofitClient.getApiService();

            // Проверяем системные оповещения
            Response<List<WeatherAlert>> alertsResponse = apiService.checkAlerts(userId).execute();
            if (alertsResponse.isSuccessful() && alertsResponse.body() != null) {
                Log.d(TAG, "Найдено системных оповещений: " + alertsResponse.body().size());
                processAlerts(alertsResponse.body(), userId);
            } else {
                Log.d(TAG, "Системных оповещений нет");
            }

            // Проверяем погодные условия для растений
            Log.d(TAG, "Проверка погодных условий для растений...");
            checkUserCropsWeather(userId, apiService);

            // Очищаем старые ключи
            cleanOldKeys(userId);

            Log.d(TAG, "=== ПРОВЕРКА УВЕДОМЛЕНИЙ ЗАВЕРШЕНА ===");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка: " + e.getMessage(), e);
            return Result.retry();
        }
    }

    private void processAlerts(List<WeatherAlert> alerts, int userId) {
        SharedPreferences prefs = getSharedPreferences(userId);
        int lastShownId = prefs.getInt("last_shown_alert_id", -1);
        int maxId = lastShownId;

        for (WeatherAlert alert : alerts) {
            String alertKey = getAlertKey(userId, "system_alert_" + alert.getId());

            // Проверяем, не показывали ли уже это оповещение сегодня
            if (!isNotificationShownToday(alertKey, alert.getAlertDate())) {
                String alertType = getAlertType(alert.getAlertText());
                String title = alertType + " " + formatAlertDate(alert.getAlertDate());

                showNotification(alert.getAlertText(), alert.getAlertDate(), title, alertKey);
                markNotificationShown(alertKey, alert.getAlertDate());
                Log.d(TAG, "Показано системное оповещение: " + alert.getAlertText());
            }

            if (alert.getId() > maxId) {
                maxId = alert.getId();
            }
        }

        if (maxId > lastShownId) {
            prefs.edit().putInt("last_shown_alert_id", maxId).apply();
        }
    }

    private void checkUserCropsWeather(int userId, ApiService apiService) {
        try {
            Response<List<UserCrop>> cropsResponse = apiService.getUserCrops(userId).execute();
            if (!cropsResponse.isSuccessful() || cropsResponse.body() == null) {
                Log.d(TAG, "Нет растений для проверки");
                return;
            }

            List<UserCrop> userCrops = cropsResponse.body();
            Log.d(TAG, "Найдено растений: " + userCrops.size());

            Map<Integer, List<WeatherData>> weatherCache = new HashMap<>();

            for (UserCrop crop : userCrops) {
                if (crop.getArea() == null || crop.getArea().getRegionId() == null) {
                    Log.d(TAG, "У растения " + crop.getName() + " нет региона");
                    continue;
                }

                int regionId = crop.getArea().getRegionId();

                if (!weatherCache.containsKey(regionId)) {
                    Response<WeatherResponse> weatherResponse =
                            apiService.getWeatherByRegionId(regionId).execute();
                    if (weatherResponse.isSuccessful() && weatherResponse.body() != null) {
                        weatherCache.put(regionId, weatherResponse.body().getWeather());
                        Log.d(TAG, "Загружена погода для региона " + regionId);
                    }
                }

                List<WeatherData> weatherList = weatherCache.get(regionId);
                if (weatherList == null || weatherList.isEmpty()) {
                    continue;
                }

                String today = getTodayDate();
                WeatherData todayWeather = null;
                for (WeatherData wd : weatherList) {
                    if (wd.getDate() != null && wd.getDate().equals(today)) {
                        todayWeather = wd;
                        break;
                    }
                }

                if (todayWeather != null) {
                    Log.d(TAG, "Проверка условий для растения: " + crop.getName());
                    checkAndNotifyForCrop(crop, todayWeather, userId);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка проверки погоды для растений: " + e.getMessage(), e);
        }
    }

    private void checkAndNotifyForCrop(UserCrop crop, WeatherData weather, int userId) {
        String cropName = crop.getName();
        String areaName = crop.getArea() != null ? crop.getArea().getName() : "участке";

        StringBuilder warnings = new StringBuilder();
        String warningKey = null;

        double tempMin = parseDouble(weather.getTemperatureMin());
        double tempMax = parseDouble(weather.getTemperatureMax());
        double humidity = parseDouble(weather.getHumidityMin());
        double wind = parseDouble(weather.getWindMax());
        double precipitation = parseDouble(weather.getPrecipitation());

        Log.d(TAG, cropName + ": temp=" + tempMin + "-" + tempMax +
                ", влажность=" + humidity + ", ветер=" + wind + ", осадки=" + precipitation);

        if (crop.getCrop() != null) {
            warningKey = checkCropConditions(crop.getCrop(), tempMin, tempMax, humidity, wind, precipitation,
                    cropName, areaName, warnings, userId);
        } else if (crop.getIndividualCrop() != null) {
            warningKey = checkIndividualCropConditions(crop.getIndividualCrop(), tempMin, tempMax, humidity, wind, precipitation,
                    cropName, areaName, warnings, userId);
        }

        if (warnings.length() > 0 && warningKey != null) {
            String dateStr = weather.getDate() != null ? weather.getDate() : getTodayDate();
            if (!isNotificationShownToday(warningKey, dateStr)) {
                Log.d(TAG, "Показываем уведомление для " + cropName + ": " + warnings.toString());
                showNotification(warnings.toString(), weather.getDate(), cropName, warningKey);
                markNotificationShown(warningKey, dateStr);
            } else {
                Log.d(TAG, "Уведомление для " + cropName + " уже показывалось сегодня");
            }
        }
    }

    private String checkCropConditions(com.example.ars.models.Crop crop, double tempMin, double tempMax,
                                       double humidity, double wind, double precipitation,
                                       String cropName, String areaName, StringBuilder warnings, int userId) {
        String baseKey = getAlertKey(userId, "crop_" + crop.getId());
        StringBuilder keyBuilder = new StringBuilder(baseKey + "_");
        boolean hasWarning = false;

        if (crop.getMinTemp() != null && tempMin < crop.getMinTemp()) {
            warnings.append("❄ Слишком холодно! ").append(String.format("%.1f°C", tempMin))
                    .append(" (норма от ").append(crop.getMinTemp()).append("°C)\n");
            keyBuilder.append("temp_low");
            hasWarning = true;
        } else if (crop.getMaxTemp() != null && tempMax > crop.getMaxTemp()) {
            warnings.append("Слишком жарко! ").append(String.format("%.1f°C", tempMax))
                    .append(" (норма до ").append(crop.getMaxTemp()).append("°C)\n");
            keyBuilder.append("temp_high");
            hasWarning = true;
        }

        if (crop.getMinHumidity() != null && humidity < crop.getMinHumidity()) {
            warnings.append("Низкая влажность! ").append(String.format("%.0f%%", humidity))
                    .append(" (норма от ").append(crop.getMinHumidity()).append("%)\n");
            keyBuilder.append("hum_low");
            hasWarning = true;
        } else if (crop.getMaxHumidity() != null && humidity > crop.getMaxHumidity()) {
            warnings.append("Высокая влажность! ").append(String.format("%.0f%%", humidity))
                    .append(" (норма до ").append(crop.getMaxHumidity()).append("%)\n");
            keyBuilder.append("hum_high");
            hasWarning = true;
        }

        if (crop.getMaxWind() != null && wind > crop.getMaxWind()) {
            warnings.append("Сильный ветер! ").append(String.format("%.1f м/с", wind))
                    .append(" (норма до ").append(crop.getMaxWind()).append(" м/с)\n");
            keyBuilder.append("wind");
            hasWarning = true;
        }

        if (crop.getNeededPrecipitation() != null && precipitation > crop.getNeededPrecipitation()) {
            warnings.append("Много осадков! ").append(String.format("%.1f мм", precipitation))
                    .append(" (норма до ").append(crop.getNeededPrecipitation()).append(" мм)\n");
            keyBuilder.append("precip");
            hasWarning = true;
        }

        if (hasWarning && warnings.length() > 0) {
            warnings.insert(0, cropName + " на " + areaName + ":\n");
            return keyBuilder.toString();
        }
        return null;
    }

    private String checkIndividualCropConditions(com.example.ars.models.IndividualUserCrop crop, double tempMin, double tempMax,
                                                 double humidity, double wind, double precipitation,
                                                 String cropName, String areaName, StringBuilder warnings, int userId) {
        String baseKey = getAlertKey(userId, "ind_crop_" + crop.getId());
        StringBuilder keyBuilder = new StringBuilder(baseKey + "_");
        boolean hasWarning = false;

        if (crop.getMinTemp() != null && tempMin < crop.getMinTemp()) {
            warnings.append("❄ Слишком холодно! ").append(String.format("%.1f°C", tempMin))
                    .append(" (норма от ").append(crop.getMinTemp()).append("°C)\n");
            keyBuilder.append("temp_low");
            hasWarning = true;
        } else if (crop.getMaxTemp() != null && tempMax > crop.getMaxTemp()) {
            warnings.append("Слишком жарко! ").append(String.format("%.1f°C", tempMax))
                    .append(" (норма до ").append(crop.getMaxTemp()).append("°C)\n");
            keyBuilder.append("temp_high");
            hasWarning = true;
        }

        if (crop.getMinHumidity() != null && humidity < crop.getMinHumidity()) {
            warnings.append("Низкая влажность! ").append(String.format("%.0f%%", humidity))
                    .append(" (норма от ").append(crop.getMinHumidity()).append("%)\n");
            keyBuilder.append("hum_low");
            hasWarning = true;
        } else if (crop.getMaxHumidity() != null && humidity > crop.getMaxHumidity()) {
            warnings.append("Высокая влажность! ").append(String.format("%.0f%%", humidity))
                    .append(" (норма до ").append(crop.getMaxHumidity()).append("%)\n");
            keyBuilder.append("hum_high");
            hasWarning = true;
        }

        if (crop.getMaxWind() != null && wind > crop.getMaxWind()) {
            warnings.append("Сильный ветер! ").append(String.format("%.1f м/с", wind))
                    .append(" (норма до ").append(crop.getMaxWind()).append(" м/с)\n");
            keyBuilder.append("wind");
            hasWarning = true;
        }

        if (crop.getNeededPrecipitation() != null && precipitation > crop.getNeededPrecipitation()) {
            warnings.append("Много осадков! ").append(String.format("%.1f мм", precipitation))
                    .append(" (норма до ").append(crop.getNeededPrecipitation()).append(" мм)\n");
            keyBuilder.append("precip");
            hasWarning = true;
        }

        if (hasWarning && warnings.length() > 0) {
            warnings.insert(0, cropName + " на " + areaName + ":\n");
            return keyBuilder.toString();
        }
        return null;
    }

    private SharedPreferences getSharedPreferences(int userId) {
        return getApplicationContext().getSharedPreferences(PREFS_NAME + "_user_" + userId, Context.MODE_PRIVATE);
    }

    private String getAlertKey(int userId, String key) {
        return "user_" + userId + "_" + key;
    }

    private boolean isNotificationShownToday(String key, String dateStr) {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String storedKey = key;
        String lastShown = prefs.getString(storedKey, "");
        String today = dateStr != null ? dateStr : getTodayDate();

        if (lastShown.equals(today)) {
            Log.d(TAG, "Уведомление " + key + " уже показывалось сегодня");
            return true;
        }

        long lastShownTime = prefs.getLong(storedKey + "_time", 0);
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShownTime < NOTIFICATION_COOLDOWN_MS) {
            Log.d(TAG, "Уведомление " + key + " в кулдауне (" +
                    TimeUnit.MILLISECONDS.toMinutes(NOTIFICATION_COOLDOWN_MS - (currentTime - lastShownTime)) + " мин)");
            return true;
        }

        return false;
    }

    private void markNotificationShown(String key, String dateStr) {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String today = dateStr != null ? dateStr : getTodayDate();
        prefs.edit()
                .putString(key, today)
                .putLong(key + "_time", System.currentTimeMillis())
                .apply();
        Log.d(TAG, "Отметили уведомление " + key + " как показанное в " + today);
    }

    private void cleanOldKeys(int userId) {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        long weekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
        Map<String, ?> allEntries = prefs.getAll();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String key = entry.getKey();
            if (key.endsWith("_time") && key.contains("user_" + userId + "_")) {
                long time = (long) entry.getValue();
                if (time < weekAgo) {
                    String baseKey = key.substring(0, key.length() - 5);
                    editor.remove(key);
                    editor.remove(baseKey);
                    Log.d(TAG, "Удалён старый ключ: " + baseKey);
                }
            }
        }
        editor.apply();
    }

    private String getAlertType(String alertText) {
        if (alertText == null) return "Погодное предупреждение";

        String lowerText = alertText.toLowerCase();
        if (lowerText.contains("гроз")) return "Гроза";
        if (lowerText.contains("шторм")) return "Шторм";
        if (lowerText.contains("ветер")) return "Сильный ветер";
        if (lowerText.contains("дождь")) return "Дождь";
        if (lowerText.contains("снег")) return "Снегопад";
        if (lowerText.contains("жара")) return "Жара";
        if (lowerText.contains("заморозки")) return "Заморозки";

        return "Погодное предупреждение";
    }

    private String formatAlertDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "";
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = inputFormat.parse(dateStr);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            return "";
        }
    }

    private String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private void showNotification(String message, String dateFromApi, String title, String uniqueKey) {
        Context context = getApplicationContext();
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String timeDisplay = "";
        if (dateFromApi != null && !dateFromApi.isEmpty()) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date date = inputFormat.parse(dateFromApi);
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM", Locale.getDefault());
                timeDisplay = " (" + outputFormat.format(date) + ")";
            } catch (Exception e) {
                timeDisplay = "";
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Погодные оповещения",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Оповещения о погодных условиях для ваших растений");
            channel.enableVibration(true);
            channel.setBypassDnd(true);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title + timeDisplay)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 200, 500});

        int notificationId = Math.abs(uniqueKey.hashCode());
        manager.notify(notificationId, builder.build());

        Log.d(TAG, "Показано уведомление: " + title);
    }

    private double parseDouble(String value) {
        if (value == null || value.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value.replace(",", "."));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public static void checkNow(Context context) {
        try {
            // Создаём экземпляр с пустыми параметрами
            AlertWorker worker = new AlertWorker(context, null);

            // Вызываем doWork напрямую
            Result result = worker.doWork();

            if (result == Result.success()) {
                Log.d(TAG, "Ручная проверка уведомлений выполнена успешно");
            } else {
                Log.d(TAG, "Ручная проверка уведомлений завершилась с ошибкой");
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при ручной проверке: " + e.getMessage(), e);
        }
    }
}