package com.feri.watchmyparent.mobile.application.interfaces;

import com.feri.watchmyparent.mobile.application.dto.WatchConnectionStatusDTO;
import java.util.concurrent.CompletableFuture;

public interface WatchConnectionManager {

    CompletableFuture<WatchConnectionStatusDTO> connect();
    CompletableFuture<WatchConnectionStatusDTO> disconnect();
    CompletableFuture<Boolean> isConnected();
    CompletableFuture<Boolean> isDeviceAvailable();
    WatchConnectionStatusDTO getCurrentStatus();
}
