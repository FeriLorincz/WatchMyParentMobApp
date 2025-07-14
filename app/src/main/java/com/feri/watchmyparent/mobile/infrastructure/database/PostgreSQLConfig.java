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

    // Direct PostgreSQL connector that doesn't use JDBC
    private final DirectPostgreSQLConnector directConnector;
    private boolean isOfflineMode = false;

    @Inject
    public PostgreSQLConfig() {
        // Use BuildConfig for dynamic configuration
        String host = BuildConfig.DEBUG ? "192.168.0.91" : "db.watchmyparent.com";
        int port = BuildConfig.DEBUG ? 5432 : 5432;
        String database = BuildConfig.DEBUG ? "watch_my_parent" : "watch_my_parent";
        String username = "postgres";
        String password = "Atelierele12";

        // Create direct connector instead of JDBC
        directConnector = new DirectPostgreSQLConnector(
                host, port, database, username, password);

        Log.d(TAG, "✅ PostgreSQL Config initialized: " + (BuildConfig.DEBUG ? "DEBUG" : "PRODUCTION"));
    }

    // For backward compatibility - return a dummy Connection
    public CompletableFuture<Connection> getConnection() {
        CompletableFuture<Connection> future = new CompletableFuture<>();
        future.completeExceptionally(
                new RuntimeException("JDBC Connection not supported - use direct connector instead"));
        return future;
    }

    // No-op for compatibility
    public void closeConnection(Connection connection) {
        // Nothing to close
    }

    public CompletableFuture<Boolean> testConnection() {
        if (isOfflineMode) {
            Log.w(TAG, "⚠️ PostgreSQL in offline mode, skipping connection test");
            return CompletableFuture.completedFuture(false);
        }

        return directConnector.testConnection()
                .thenApply(success -> {
                    isOfflineMode = !success;
                    return success;
                })
                .exceptionally(throwable -> {
                    isOfflineMode = true;
                    return false;
                });
    }

    public CompletableFuture<Boolean> insertTestData() {
        if (isOfflineMode) {
            Log.w(TAG, "⚠️ PostgreSQL in offline mode, skipping test data insertion");
            return CompletableFuture.completedFuture(false);
        }

        return directConnector.insertTestData();
    }

    public void resetOfflineMode() {
        Log.d(TAG, "🔄 Resetting offline mode - will retry PostgreSQL connection");
        this.isOfflineMode = false;
    }

    public boolean isOfflineMode() {
        return isOfflineMode;
    }

    public String getConnectionInfo() {
        return directConnector.getConnectionInfo();
    }

    public String getJdbcUrl() {
        return "Direct connection (no JDBC)";
    }
}