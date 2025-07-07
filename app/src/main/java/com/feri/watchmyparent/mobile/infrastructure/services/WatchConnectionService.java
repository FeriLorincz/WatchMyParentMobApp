package com.feri.watchmyparent.mobile.infrastructure.services;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;

import com.feri.watchmyparent.mobile.infrastructure.watch.SamsungWatchManager;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WatchConnectionService {

    private static final String TAG = "WatchConnectionService";

    private final Context context;
    private final SamsungWatchManager samsungWatchManager;
    private boolean isConnected = false;

    @Inject
    public WatchConnectionService(@NonNull Context context, SamsungWatchManager samsungWatchManager) {
        this.context = context;
        this.samsungWatchManager = samsungWatchManager;
    }

    public CompletableFuture<Boolean> connectWatch() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Connecting to Samsung Watch");
                CompletableFuture<Boolean> connectFuture = samsungWatchManager.connect();
                boolean success = connectFuture.join();
                isConnected = success;

                if (success) {
                    Log.d(TAG, "Samsung Watch connected successfully");
                } else {
                    Log.d(TAG, "Failed to connect to Samsung Watch");
                }

                return success;
            } catch (Exception e) {
                Log.e(TAG, "Error connecting to Samsung Watch", e);
                isConnected = false;
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> disconnectWatch() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Disconnecting from Samsung Watch");
                CompletableFuture<Boolean> disconnectFuture = samsungWatchManager.disconnect();
                boolean success = disconnectFuture.join();
                isConnected = !success;

                if (success) {
                    Log.d(TAG, "Samsung Watch disconnected");
                } else {
                    Log.d(TAG, "Failed to disconnect Samsung Watch");
                }

                return success;
            } catch (Exception e) {
                Log.e(TAG, "Error disconnecting from Samsung Watch", e);
                return false;
            }
        });
    }

    public boolean isConnected() {
        return isConnected;
    }
}