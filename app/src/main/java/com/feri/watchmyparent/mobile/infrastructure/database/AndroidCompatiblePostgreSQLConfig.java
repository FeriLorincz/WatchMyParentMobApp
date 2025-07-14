package com.feri.watchmyparent.mobile.infrastructure.database;

import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

//Android-compatible PostgreSQL connection handler that avoids JMX dependencies

public class AndroidCompatiblePostgreSQLConfig {

    private static final String TAG = "AndroidPostgreSQL";

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private boolean isOfflineMode = false;

    public AndroidCompatiblePostgreSQLConfig(String host, String port, String database,
                                             String username, String password) {
        this.jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", host, port, database);
        this.username = username;
        this.password = password;

        Log.d(TAG, "✅ Android PostgreSQL Config initialized");
        Log.d(TAG, "🔗 Database URL: " + jdbcUrl);
    }

    public CompletableFuture<Connection> getConnection() {
        if (isOfflineMode) {
            CompletableFuture<Connection> future = new CompletableFuture<>();
            future.completeExceptionally(
                    new RuntimeException("Application is in offline mode - PostgreSQL not available"));
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "🔄 Attempting PostgreSQL connection...");

                // Set connection properties
                Properties props = new Properties();
                props.setProperty("user", username);
                props.setProperty("password", password);
                props.setProperty("connectTimeout", "15");
                props.setProperty("socketTimeout", "30");
                props.setProperty("loginTimeout", "15");

                // Disable JMX explicitly - the key part that fixes the issue
                props.setProperty("jmxEnabled", "false");

                // Load the PostgreSQL driver
                Class.forName("org.postgresql.Driver");

                // Attempt connection
                Connection connection = DriverManager.getConnection(jdbcUrl, props);

                if (connection != null && !connection.isClosed()) {
                    Log.d(TAG, "✅ PostgreSQL connection established successfully!");
                    return connection;
                } else {
                    throw new SQLException("Connection is null or closed");
                }
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "❌ PostgreSQL driver not found", e);
                isOfflineMode = true;
                throw new RuntimeException("PostgreSQL driver not found", e);
            } catch (SQLException e) {
                Log.e(TAG, "❌ PostgreSQL connection failed: " + e.getMessage(), e);
                Log.w(TAG, "⚠️ Switching to offline mode - PostgreSQL unavailable");
                isOfflineMode = true;
                throw new RuntimeException("PostgreSQL connection failed", e);
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
                Log.d(TAG, "🔌 PostgreSQL connection closed");
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
                Log.d(TAG, "🧪 Testing PostgreSQL connection...");

                connection = getConnection().join();
                if (connection != null && !connection.isClosed()) {
                    // Test with a simple query
                    try (PreparedStatement stmt = connection.prepareStatement("SELECT 1");
                         ResultSet rs = stmt.executeQuery()) {

                        boolean hasResults = rs.next();
                        Log.d(TAG, "✅ PostgreSQL connection test PASSED");
                        return true;
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

    public void resetOfflineMode() {
        Log.d(TAG, "🔄 Resetting offline mode - will retry PostgreSQL connection");
        this.isOfflineMode = false;
    }

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
