package com.feri.watchmyparent.mobile.infrastructure.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

import pub.devrel.easypermissions.BuildConfig;
import timber.log.Timber;

public class PostgreSQLConfig {

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

                Timber.d("Attempting to connect to: %s", url);
                Connection connection = DriverManager.getConnection(url, DB_USER, DB_PASSWORD);

                Timber.d("Successfully connected to PostgreSQL database");
                return connection;

            } catch (ClassNotFoundException e) {
                Timber.e(e, "PostgreSQL driver not found");
                throw new RuntimeException("PostgreSQL driver not found", e);
            } catch (SQLException e) {
                Timber.e(e, "Failed to connect to PostgreSQL database");
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
                Timber.d("Database connection closed");
            } catch (SQLException e) {
                Timber.e(e, "Error closing database connection");
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
                        Timber.e(e, "Connection validation failed");
                        closeConnection(connection);
                        return false;
                    }
                })
                .exceptionally(throwable -> {
                    Timber.e(throwable, "Database connection test failed");
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