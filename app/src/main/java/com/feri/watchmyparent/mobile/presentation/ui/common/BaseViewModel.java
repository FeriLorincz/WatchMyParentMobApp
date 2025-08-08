package com.feri.watchmyparent.mobile.presentation.ui.common;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public abstract class BaseViewModel extends ViewModel {

    // ✅ CORECTAT: Eliminat asteriskurile din nume
    protected final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    protected final MutableLiveData<String> _error = new MutableLiveData<>();
    protected final MutableLiveData<String> _success = new MutableLiveData<>();

    // Public getters
    public LiveData<Boolean> getIsLoading() { return _isLoading; }
    public LiveData<String> getError() { return _error; }
    public LiveData<String> getSuccess() { return _success; }

    // Protected setters for subclasses
    protected void setLoading(boolean loading) {
        _isLoading.setValue(loading);
    }

    protected void setError(String errorMessage) {
        _error.setValue(errorMessage);
        setLoading(false);
    }

    protected void setSuccess(String successMessage) {
        _success.setValue(successMessage);
        setLoading(false);
    }

    // ✅ ADĂUGAT: Helper methods pentru clear messages
    protected void clearError() {
        _error.setValue(null);
    }

    protected void clearSuccess() {
        _success.setValue(null);
    }

    protected void clearMessages() {
        _error.setValue(null);
        _success.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Cleanup logic can be added here if needed
    }
}