package com.feri.watchmyparent.mobile.presentation.ui.common;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public abstract class BaseViewModel extends ViewModel {

    protected final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    protected final MutableLiveData<String> _error = new MutableLiveData<>();
    protected final MutableLiveData<String> _success = new MutableLiveData<>();

    public LiveData<Boolean> getIsLoading() { return _isLoading; }
    public LiveData<String> getError() { return _error; }
    public LiveData<String> getSuccess() { return _success; }

    protected void setLoading(boolean loading) {
        _isLoading.setValue(loading);
    }

    protected void setError(String error) {
        _error.setValue(error);
        setLoading(false);
    }

    protected void setSuccess(String success) {
        _success.setValue(success);
        setLoading(false);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}
