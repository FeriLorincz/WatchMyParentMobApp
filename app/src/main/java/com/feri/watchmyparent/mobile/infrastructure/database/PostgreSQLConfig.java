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

    // Configurare pentru conexiunea la baza de date
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private boolean isOfflineMode = false;

    @Inject
    public PostgreSQLConfig() {
        // Folosim aceleași valori configurate anterior
        this.jdbcUrl = "jdbc:postgresql://10.0.2.2:5432/watch_my_parent";  // localhost pentru emulator
        this.username = "postgres";
        this.password = "Atelierele12";
    }

    public CompletableFuture<Connection> getConnection() {
        // Dacă suntem în modul offline, returnăm o promisiune eșuată imediat
        if (isOfflineMode) {
            CompletableFuture<Connection> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Application is in offline mode"));
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Setăm timeout pentru conexiune la 10 secunde
                java.util.Properties props = new java.util.Properties();
                props.setProperty("user", username);
                props.setProperty("password", password);
                props.setProperty("connectTimeout", "10");

                // Încercăm să ne conectăm
                Class.forName("org.postgresql.Driver");
                Connection connection = DriverManager.getConnection(jdbcUrl, props);
                Log.d(TAG, "Successfully connected to PostgreSQL database");
                return connection;
            } catch (Exception e) {
                Log.e(TAG, "Failed to connect to PostgreSQL database", e);
                // Dacă conexiunea eșuează, activăm modul offline
                isOfflineMode = true;
                throw new RuntimeException("Database connection failed", e);
            }
        });
    }

    public void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
                Log.d(TAG, "Database connection closed");
            } catch (SQLException e) {
                Log.e(TAG, "Error closing database connection", e);
            }
        }
    }

    public CompletableFuture<Boolean> testConnection() {
        if (isOfflineMode) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            Connection connection = null;
            try {
                connection = getConnection().join();
                if (connection != null && !connection.isClosed()) {
                    return true;
                }
                return false;
            } catch (Exception e) {
                Log.e(TAG, "Database connection test failed", e);
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
        this.isOfflineMode = false;
    }

    // Getter pentru a verifica dacă suntem în modul offline
    public boolean isOfflineMode() {
        return isOfflineMode;
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