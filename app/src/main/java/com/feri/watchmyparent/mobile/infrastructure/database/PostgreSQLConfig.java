package com.feri.watchmyparent.mobile.infrastructure.database;

import android.util.Log;
import com.feri.watchmyparent.mobile.BuildConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PostgreSQLConfig {

    private static final String TAG = "PostgreSQLConfig";

    // ✅ REAL PostgreSQL Configuration
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private boolean isOfflineMode = false;

    @Inject
    public PostgreSQLConfig() {

        // ✅ Use BuildConfig for dynamic configuration
        String host = BuildConfig.DEBUG ? "192.168.0.91" : "db.watchmyparent.com";
        String port = BuildConfig.DEBUG ? "5432" : "5432";
        String database = BuildConfig.DEBUG ? "watch_my_parent" : "watch_my_parent";

        this.jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", host, port, database);
        this.username = "postgres";
        this.password = "Atelierele12"; // In production, use secure method

        Log.d(TAG, "✅ PostgreSQL Config initialized: " + (BuildConfig.DEBUG ? "DEBUG" : "PRODUCTION"));
        Log.d(TAG, "🔗 Database URL: " + jdbcUrl);

//        // ✅ Use BuildConfig for dynamic IP configuration
//        if (BuildConfig.DEBUG) {
//            // Debug - folosește IP-ul local al computerului tău
//            this.jdbcUrl = "jdbc:postgresql://192.168.0.91:5432/watch_my_parent";
//        } else {
//            // Production - folosește server-ul real
//            this.jdbcUrl = "jdbc:postgresql://10.0.2.2:5432/watch_my_parent";  // localhost pentru emulator
//        }
//
//        this.username = "postgres";
//        this.password = "Atelierele12";
//
//        Log.d(TAG, "✅ PostgreSQL Config initialized: " + (BuildConfig.DEBUG ? "DEBUG" : "PRODUCTION"));
//        Log.d(TAG, "🔗 Database URL: " + jdbcUrl);
    }

    public CompletableFuture<Connection> getConnection() {
        // Dacă suntem în modul offline, returnăm o promisiune eșuată imediat
        if (isOfflineMode) {
            CompletableFuture<Connection> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Application is in offline mode - PostgreSQL not available"));
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "🔄 Attempting real PostgreSQL connection...");

                // Setăm timeout pentru conexiune la 15 secunde
                java.util.Properties props = new java.util.Properties();
                props.setProperty("user", username);
                props.setProperty("password", password);
                props.setProperty("connectTimeout", "15");
                props.setProperty("socketTimeout", "30");
                props.setProperty("loginTimeout", "15");

                // Încarcă driver-ul PostgreSQL
                Class.forName("org.postgresql.Driver");

                // Încercăm să ne conectăm REAL
                Connection connection = DriverManager.getConnection(jdbcUrl, props);

                // Test conexiunea
                if (connection != null && !connection.isClosed()) {
                    Log.d(TAG, "✅ REAL PostgreSQL connection established successfully!");
                    Log.d(TAG, "📊 Database: " + connection.getCatalog());
                    return connection;
                } else {
                    throw new SQLException("Connection is null or closed");
                }

            } catch (ClassNotFoundException e) {
                Log.e(TAG, "❌ PostgreSQL driver not found", e);
                isOfflineMode = true;
                throw new RuntimeException("PostgreSQL driver not found", e);
            } catch (SQLException e) {
                Log.e(TAG, "❌ REAL PostgreSQL connection failed: " + e.getMessage(), e);
                Log.w(TAG, "⚠️ Switching to offline mode - PostgreSQL unavailable");
                isOfflineMode = true;
                throw new RuntimeException("Real PostgreSQL connection failed", e);
            } catch (Exception e) {
                Log.e(TAG, "❌ Unexpected error during PostgreSQL connection", e);
                isOfflineMode = true;
                throw new RuntimeException("Unexpected database error", e);
            }
        });
    }

    public void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
                Log.d(TAG, "🔌 Real PostgreSQL connection closed");
            } catch (SQLException e) {
                Log.e(TAG, "❌ Error closing PostgreSQL connection", e);
            }
        }
    }

    public CompletableFuture<Boolean> testConnection() {
        if (isOfflineMode) {
            Log.w(TAG, "⚠️ PostgreSQL in offline mode, skipping connection test");
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            Connection connection = null;
            try {
                Log.d(TAG, "🧪 Testing REAL PostgreSQL connection...");

                connection = getConnection().join();
                if (connection != null && !connection.isClosed()) {
                    // Test cu un query simplu
                    boolean isValid = connection.isValid(10); // 10 seconds timeout

                    if (isValid) {
                        Log.d(TAG, "✅ PostgreSQL connection test PASSED");
                        return true;
                    } else {
                        Log.w(TAG, "⚠️ PostgreSQL connection test FAILED - invalid connection");
                        return false;
                    }
                } else {
                    Log.w(TAG, "⚠️ PostgreSQL connection test FAILED - null connection");
                    return false;
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ PostgreSQL connection test FAILED", e);
                isOfflineMode = true;
                return false;
            } finally {
                if (connection != null) {
                    closeConnection(connection);
                }
            }
        });
    }

    // Metodă pentru a reseta modul offline și a încerca din nou conexiunea
    public void resetOfflineMode() {
        Log.d(TAG, "🔄 Resetting offline mode - will retry PostgreSQL connection");
        this.isOfflineMode = false;
    }

    // Getter pentru a verifica dacă suntem în modul offline
    public boolean isOfflineMode() {
        return isOfflineMode;
    }

    public String getConnectionInfo() {
        return "PostgreSQL: " + jdbcUrl + " (User: " + username + ")";
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }
}



/*
    private static final String TAG = "PostgreSQLConfig";
    // Configurații pentru producție
    private static final String PROD_DB_URL = "jdbc:postgresql://your-server.com:5432/watchmyparent_database";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "Atelierele12";

    // Configurații pentru dezvoltare locală
    private static final String DEV_DB_URL = "jdbc:postgresql://10.0.2.2:5432/watchmyparent_database";
    private static final String LOCAL_DB_URL = "jdbc:postgresql://localhost:5432/watchmyparent_database";

    // Alternative IP-uri pentru development
    private static final String DEV_DB_URL_REAL_IP = "jdbc:postgresql://192.168.0.91:5432/watchmyparent_database";
    private static final String DEV_DB_URL_ALT = "jdbc:postgresql://192.168.1.100:5432/watchmyparent_database";

    public static CompletableFuture<Connection> getConnection() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Încarcă driver-ul PostgreSQL
                Class.forName("org.postgresql.Driver");

                // Alege URL-ul în funcție de environment
                String url = getConnectionUrl();

                Log.d(TAG, "Attempting to connect to: " + url);
                Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD);

                Log.d(TAG, "Successfully connected to PostgreSQL database");
                return connection;

            } catch (ClassNotFoundException e) {
                Log.e(TAG, "PostgreSQL driver not found", e);
                throw new RuntimeException("PostgreSQL driver not found", e);
            } catch (SQLException e) {
                Log.e(TAG, "Failed to connect to PostgreSQL database", e);
                throw new RuntimeException("Database connection failed", e);
            }
        });
    }

    private static String getConnectionUrl() {
        if (BuildConfig.DEBUG) {

//            "http://192.168.0.91:8080/",    // IP-ul calculatorului tău
//                    "http://10.0.2.2:8080/",        // Emulator Android
//                    "http://192.168.1.100:8080/",   // Posibil IP alternativ
//                    "http://127.0.0.1:8080/"        // localhost

            // Pentru emulator Android Studio: 10.0.2.2
            // Pentru device fizic: IP-ul real al computerului

            return DEV_DB_URL;              // Pentru emulator Android: 10.0.2.2
            // return DEV_DB_URL_REAL_IP;   // Pentru device fizic: IP-ul real
            // return DEV_DB_URL_ALT;       // IP alternativ
            // return LOCAL_DB_URL;         // localhost (pentru test direct)
        } else {
            return PROD_DB_URL;
        }
    }

    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
                Log.d(TAG, "Database connection closed");
            } catch (SQLException e) {
                Log.e(TAG, "Error closing database connection", e);
                // Handle the exception appropriately
            }
        }
    }

    // Test connection
    public static CompletableFuture<Boolean> testConnection() {
        return getConnection()
                .thenApply(connection -> {
                    try {
                        // Testează cu un query simplu
                        boolean isValid = connection.isValid(5); // 5 seconds timeout
                        closeConnection(connection);
                        return isValid;
                    } catch (SQLException e) {
                        Log.e(TAG, "Connection validation failed", e);
                        closeConnection(connection);
                        return false;
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Database connection test failed" + throwable.getMessage());
                    return false;
                });
    }

    // Obține informații despre conexiune pentru debugging
    public static CompletableFuture<String> getConnectionInfo() {
        return getConnection()
                .thenApply(connection -> {
                    try {
                        String info = "Database: " + connection.getCatalog() +
                                ", URL: " + connection.getMetaData().getURL() +
                                ", User: " + connection.getMetaData().getUserName();
                        closeConnection(connection);
                        return info;
                    } catch (SQLException e) {
                        closeConnection(connection);
                        return "Error getting connection info: " + e.getMessage();
                    }
                });
    }
}

 */