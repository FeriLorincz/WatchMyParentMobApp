package com.feri.watchmyparent.mobile.infrastructure.services;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.inject.Inject;
import javax.inject.Singleton;

// Gestionează starea rețelei și notifică despre schimbările de conectivitate
@Singleton
public class NetworkStateManager {

    private static final String TAG = "NetworkStateManager";

    private final Context context;
    private final ConnectivityManager connectivityManager;
    private final CopyOnWriteArrayList<NetworkStateListener> listeners = new CopyOnWriteArrayList<>();

    // Network state tracking
    private boolean isNetworkAvailable = false;
    private boolean isWifiConnected = false;
    private boolean isMobileConnected = false;
    private String currentNetworkType = "None";
    private NetworkCapabilities currentCapabilities = null;

    // Network callback
    private ConnectivityManager.NetworkCallback networkCallback;

    @Inject
    public NetworkStateManager(Context context) {
        this.context = context;
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        Log.d(TAG, "✅ NetworkStateManager initialized");
        initializeNetworkMonitoring();
    }

    //Inițializează monitorizarea rețelei
    private void initializeNetworkMonitoring() {
        try {
            // Verifică starea inițială
            updateCurrentNetworkState();

            // Configurează callback-ul pentru schimbări de rețea
            setupNetworkCallback();

            Log.d(TAG, "🌐 Network monitoring initialized - Current state: " +
                    (isNetworkAvailable ? "✅ Available" : "❌ Unavailable"));

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to initialize network monitoring", e);
        }
    }

    // Configurează callback-ul pentru monitorizarea rețelei
    private void setupNetworkCallback() {
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                Log.d(TAG, "🌐 Network available: " + network);
                updateNetworkState(true, network);
            }

            @Override
            public void onLost(Network network) {
                Log.w(TAG, "🌐 Network lost: " + network);
                updateNetworkState(false, null);
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
                Log.d(TAG, "🌐 Network capabilities changed: " + network);
                updateNetworkCapabilities(capabilities);
            }
        };

        // Înregistrează callback-ul
        NetworkRequest.Builder builder = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);

        connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
        Log.d(TAG, "✅ Network callback registered");
    }

    // Actualizează starea curentă a rețelei
    private void updateCurrentNetworkState() {
        try {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork != null) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                if (capabilities != null) {
                    updateNetworkState(true, activeNetwork);
                    updateNetworkCapabilities(capabilities);
                } else {
                    updateNetworkState(false, null);
                }
            } else {
                updateNetworkState(false, null);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error updating network state", e);
            updateNetworkState(false, null);
        }
    }

    // Actualizează starea rețelei și notifică listeners
    private void updateNetworkState(boolean available, Network network) {
        boolean wasAvailable = isNetworkAvailable;
        isNetworkAvailable = available;

        if (wasAvailable != available) {
            Log.i(TAG, "🌐 Network state changed: " + (available ? "✅ AVAILABLE" : "❌ UNAVAILABLE"));

            // Notifică listeners despre schimbarea stării
            for (NetworkStateListener listener : listeners) {
                try {
                    if (available) {
                        listener.onNetworkAvailable();
                    } else {
                        listener.onNetworkUnavailable();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error notifying network state listener", e);
                }
            }
        }
    }

    // Actualizează capabilitățile rețelei
    private void updateNetworkCapabilities(NetworkCapabilities capabilities) {
        currentCapabilities = capabilities;

        boolean wasWifiConnected = isWifiConnected;
        boolean wasMobileConnected = isMobileConnected;

        isWifiConnected = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        isMobileConnected = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);

        // Determină tipul de rețea
        if (isWifiConnected) {
            currentNetworkType = "WiFi";
        } else if (isMobileConnected) {
            currentNetworkType = "Mobile";
        } else {
            currentNetworkType = "Other";
        }

        // Logează schimbările importante
        if (wasWifiConnected != isWifiConnected || wasMobileConnected != isMobileConnected) {
            Log.i(TAG, "🌐 Network type changed to: " + currentNetworkType);

            // Notifică listeners despre schimbarea tipului de rețea
            for (NetworkStateListener listener : listeners) {
                try {
                    listener.onNetworkTypeChanged(currentNetworkType);
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error notifying network type listener", e);
                }
            }
        }
    }

    // Verifică dacă rețeaua este disponibilă
    public boolean isNetworkAvailable() {
        return isNetworkAvailable;
    }

    // Verifică dacă este conectat la WiFi
    public boolean isWifiConnected() {
        return isWifiConnected;
    }

    // Verifică dacă este conectat la rețeaua mobilă
    public boolean isMobileConnected() {
        return isMobileConnected;
    }

    // Obține tipul curent de rețea
    public String getCurrentNetworkType() {
        return currentNetworkType;
    }

    // Verifică dacă conexiunea este măsurată (mobile data)
    public boolean isMeteredConnection() {
        if (currentCapabilities != null) {
            return !currentCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
        }
        return isMobileConnected; // Presupunem că mobile e metered
    }

    // Obține viteza estimată a conexiunii
    public String getConnectionSpeed() {
        if (currentCapabilities == null) return "Unknown";

        if (currentCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            if (isWifiConnected) {
                return "High Speed (WiFi)";
            } else if (isMobileConnected) {
                // Încearcă să determine tipul de conexiune mobilă
                if (currentCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    return "Variable Speed (Mobile)";
                }
            }
        }

        return "Limited";
    }

    // Execută un test manual de conectivitate
    public CompletableFuture<NetworkTestResult> performNetworkTest() {
        return CompletableFuture.supplyAsync(() -> {
            NetworkTestResult result = new NetworkTestResult();

            try {
                updateCurrentNetworkState();

                result.isAvailable = isNetworkAvailable;
                result.networkType = currentNetworkType;
                result.isMetered = isMeteredConnection();
                result.connectionSpeed = getConnectionSpeed();
                result.testTimestamp = java.time.LocalDateTime.now();

                // Test ping simplu (optional - poate fi extins)
                if (isNetworkAvailable) {
                    result.pingSuccessful = testInternetConnectivity();
                }

                Log.d(TAG, "🔍 Network test completed: " + result);

            } catch (Exception e) {
                Log.e(TAG, "❌ Network test failed", e);
                result.error = e.getMessage();
            }

            return result;
        });
    }

    //Test simplu de conectivitate la internet
    private boolean testInternetConnectivity() {
        try {
            // Verifică rapid dacă avem conexiune validată
            if (currentCapabilities != null) {
                return currentCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "❌ Internet connectivity test failed", e);
            return false;
        }
    }

    // Adaugă listener pentru schimbări de rețea
    public void addNetworkStateListener(NetworkStateListener listener) {
        listeners.add(listener);
        Log.d(TAG, "➕ Added network state listener (total: " + listeners.size() + ")");

        // Notifică imediat cu starea curentă
        try {
            if (isNetworkAvailable) {
                listener.onNetworkAvailable();
            } else {
                listener.onNetworkUnavailable();
            }
            listener.onNetworkTypeChanged(currentNetworkType);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error notifying new listener", e);
        }
    }

    //Elimină listener pentru schimbări de rețe
    public void removeNetworkStateListener(NetworkStateListener listener) {
        boolean removed = listeners.remove(listener);
        if (removed) {
            Log.d(TAG, "➖ Removed network state listener (remaining: " + listeners.size() + ")");
        }
    }

    // Obține statusul detaliat al rețelei
    public NetworkStatus getDetailedNetworkStatus() {
        NetworkStatus status = new NetworkStatus();
        status.isAvailable = isNetworkAvailable;
        status.isWifiConnected = isWifiConnected;
        status.isMobileConnected = isMobileConnected;
        status.networkType = currentNetworkType;
        status.isMetered = isMeteredConnection();
        status.connectionSpeed = getConnectionSpeed();
        status.listenersCount = listeners.size();

        return status;
    }

    // Cleanup când serviciul se oprește
    public void shutdown() {
        try {
            if (networkCallback != null && connectivityManager != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
                Log.d(TAG, "✅ Network callback unregistered");
            }

            listeners.clear();
            Log.d(TAG, "✅ NetworkStateManager shut down");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error during NetworkStateManager shutdown", e);
        }
    }

    // Interface pentru listeners
    public interface NetworkStateListener {
        void onNetworkAvailable();
        void onNetworkUnavailable();
        void onNetworkTypeChanged(String networkType);
    }

    // Rezultatul testului de rețea
    public static class NetworkTestResult {
        public boolean isAvailable = false;
        public String networkType = "None";
        public boolean isMetered = false;
        public String connectionSpeed = "Unknown";
        public boolean pingSuccessful = false;
        public java.time.LocalDateTime testTimestamp;
        public String error = null;

        @Override
        public String toString() {
            return String.format("Network Test: %s, Type: %s, Speed: %s, Ping: %s",
                    isAvailable ? "Available" : "Unavailable",
                    networkType, connectionSpeed,
                    pingSuccessful ? "OK" : "Failed");
        }
    }

    // Status detaliat al rețelei
    public static class NetworkStatus {
        public boolean isAvailable = false;
        public boolean isWifiConnected = false;
        public boolean isMobileConnected = false;
        public String networkType = "None";
        public boolean isMetered = false;
        public String connectionSpeed = "Unknown";
        public int listenersCount = 0;

        public String getSummary() {
            return String.format("Network: %s %s | Speed: %s | Listeners: %d",
                    isAvailable ? "✅" : "❌",
                    networkType,
                    connectionSpeed,
                    listenersCount);
        }
    }
}