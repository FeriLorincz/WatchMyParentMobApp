package com.feri.watchmyparent.mobile.infrastructure.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;

import com.feri.watchmyparent.mobile.R;
import com.feri.watchmyparent.mobile.domain.enums.SensorType;
import com.feri.watchmyparent.mobile.domain.valueobjects.SensorReading;
import com.feri.watchmyparent.mobile.presentation.ui.dashboard.DashboardActivity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

// REMOVED @AndroidEntryPoint - This was causing the ASM transformation error
// BroadcastReceiver-ul nu poate folosi Hilt dependency injection în mod standard
public class FallDetectionReceiver extends BroadcastReceiver {

    private static final String TAG = "FallDetectionReceiver";
    private static final String CHANNEL_ID = "fall_detection_alerts";
    private static final int NOTIFICATION_ID = 9001;

    // ✅ REMOVED @Inject - Nu mai folosim Hilt injection
    // În schimb, vom obține serviciul prin alte mijloace

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "📡 BroadcastReceiver triggered with action: " + intent.getAction());

        if ("com.samsung.health.FALL_DETECTION".equals(intent.getAction())) {
            handleFallDetection(context, intent);
        }
    }

    private void handleFallDetection(Context context, Intent intent) {
        try {
            Log.e(TAG, "🚨 FALL DETECTED by Samsung Galaxy Watch 7!");

            // Extract data from intent
            long timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis());
            double confidence = intent.getDoubleExtra("confidence", 100.0);
            String location = intent.getStringExtra("location");
            String deviceId = intent.getStringExtra("deviceId");

            Log.e(TAG, "📊 Fall Detection Details:");
            Log.e(TAG, "   Timestamp: " + new java.util.Date(timestamp));
            Log.e(TAG, "   Confidence: " + confidence + "%");
            Log.e(TAG, "   Location: " + location);
            Log.e(TAG, "   Device: " + deviceId);

            // Create sensor reading
            SensorReading fallReading = createFallDetectionReading(timestamp, confidence, deviceId);

            // Process fall detection
            processFallDetection(context, fallReading);

            // Send immediate notification
            sendFallDetectionNotification(context, confidence);

            // ✅ FIXED: Send data through alternative method (nu prin Hilt injection)
            sendFallDetectionDataAlternative(context, fallReading);

        } catch (Exception e) {
            Log.e(TAG, "❌ Error processing fall detection", e);
        }
    }

    private SensorReading createFallDetectionReading(long timestamp, double confidence, String deviceId) {
        SensorReading fallReading = new SensorReading(SensorType.FALL_DETECTION, 1.0); // 1.0 = fall detected

        fallReading.setTimestamp(LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()));

        // Enhanced metadata
        String metadata = String.format(
                "confidence=%.2f,source=samsung_health_sdk,device=%s,alert=true,severity=high",
                confidence, deviceId != null ? deviceId : "samsung_galaxy_watch_7");
        fallReading.setMetadata(metadata);

        fallReading.setDeviceId(deviceId != null ? deviceId : "samsung_galaxy_watch_7");
        fallReading.setConnectionType("SAMSUNG_HEALTH_BROADCAST");
        fallReading.setAccuracy(confidence);

        return fallReading;
    }

    private void processFallDetection(Context context, SensorReading fallReading) {
        Log.e(TAG, "🚨 Processing fall detection alert...");

        // 1. Log critical event
        logCriticalEvent(fallReading);

        // 2. Start emergency protocols (if needed)
        startEmergencyProtocols(context, fallReading);

        // 3. Update application state (optional)
        updateApplicationState(context);
    }

    private void logCriticalEvent(SensorReading fallReading) {
        // Log structured pentru monitoring systems
        String criticalLog = String.format(
                "CRITICAL_EVENT: {\"type\":\"FALL_DETECTION\", \"timestamp\":\"%s\", " +
                        "\"confidence\":%.2f, \"device\":\"%s\", \"alert\":\"HIGH_PRIORITY\"}",
                fallReading.getTimestamp().toString(),
                fallReading.getAccuracy(),
                fallReading.getDeviceId()
        );
        Log.e("CRITICAL_MONITORING", criticalLog);
    }

    private void startEmergencyProtocols(Context context, SensorReading fallReading) {
        Log.e(TAG, "🚨 Emergency protocols activated for fall detection");
        // Emergency protocols implementation
    }

    private void updateApplicationState(Context context) {
        try {
            Intent appIntent = new Intent(context, DashboardActivity.class);
            appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            appIntent.putExtra("FALL_DETECTION_ALERT", true);
            context.startActivity(appIntent);
        } catch (Exception e) {
            Log.e(TAG, "❌ Could not update application state", e);
        }
    }

    private void sendFallDetectionNotification(Context context, double confidence) {
        try {
            createNotificationChannel(context);

            Intent notificationIntent = new Intent(context, DashboardActivity.class);
            notificationIntent.putExtra("FALL_DETECTION_ALERT", true);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_watch_notification)
                    .setContentTitle("🚨 FALL DETECTED!")
                    .setContentText(String.format("Samsung Galaxy Watch 7 detected a fall (%.1f%% confidence)", confidence))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setVibrate(new long[]{0, 1000, 500, 1000})
                    .setDefaults(NotificationCompat.DEFAULT_ALL);

            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_ID, builder.build());

            Log.e(TAG, "🔔 Fall detection notification sent");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error sending fall detection notification", e);
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Fall Detection Alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Critical alerts for fall detection from Samsung Galaxy Watch 7");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    // ✅ ALTERNATIVE METHOD: Send data fără Hilt injection
    private void sendFallDetectionDataAlternative(Context context, SensorReading fallReading) {
        try {
            // Salvăm datele într-un SharedPreferences sau fișier temporar
            // pentru ca serviciul principal să le ia și să le trimită prin Kafka

            android.content.SharedPreferences prefs = context.getSharedPreferences("fall_detection", Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();

            editor.putString("last_fall_timestamp", fallReading.getTimestamp().toString());
            editor.putFloat("last_fall_confidence", (float) fallReading.getAccuracy());
            editor.putString("last_fall_device", fallReading.getDeviceId());
            editor.putBoolean("pending_fall_transmission", true);
            editor.apply();

            Log.d(TAG, "✅ Fall detection data saved for transmission by main service");

            // Trigger main service to process the pending fall detection
            Intent serviceIntent = new Intent(context, WatchDataCollectionService.class);
            serviceIntent.putExtra("PROCESS_FALL_DETECTION", true);
            context.startService(serviceIntent);

        } catch (Exception e) {
            Log.e(TAG, "❌ Error in alternative fall detection data transmission", e);
        }
    }

    // Static method pentru testarea fall detection din cod
    public static void simulateFallDetection(Context context) {
        Log.d(TAG, "🧪 Simulating fall detection for testing...");

        Intent fallIntent = new Intent("com.samsung.health.FALL_DETECTION");
        fallIntent.putExtra("timestamp", System.currentTimeMillis());
        fallIntent.putExtra("confidence", 95.0);
        fallIntent.putExtra("location", "Test Location");
        fallIntent.putExtra("deviceId", "samsung_galaxy_watch_7_test");

        context.sendBroadcast(fallIntent);
    }
}