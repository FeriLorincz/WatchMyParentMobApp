package com.feri.watchmyparent.mobile.application.interfaces;

import java.util.concurrent.CompletableFuture;

public interface DataTransmissionService {

    CompletableFuture<Boolean> transmitData(Object data, String userId);
    CompletableFuture<Boolean> retryFailedTransmissions(String userId);
    CompletableFuture<Integer> getPendingTransmissionCount(String userId);
}
