package com.feri.watchmyparent.mobile.presentation.ui.sensors;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.feri.watchmyparent.mobile.application.dto.LocationDataDTO;
import com.feri.watchmyparent.mobile.application.services.LocationApplicationService;
import com.feri.watchmyparent.mobile.presentation.ui.common.BaseViewModel;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class LocationStatusViewModel extends BaseViewModel {

    private final LocationApplicationService locationService;
    private final MutableLiveData<LocationDataDTO> _locationStatus = new MutableLiveData<>();

    private String currentUserId = "demo-user-id";

    @Inject
    public LocationStatusViewModel(LocationApplicationService locationService) {
        this.locationService = locationService;
    }

    public LiveData<LocationDataDTO> getLocationStatus() { return _locationStatus; }

    public void loadLocationStatus() {
        locationService.getCurrentUserLocation(currentUserId)
                .thenAccept(_locationStatus::postValue)
                .exceptionally(throwable -> {
                    setError("Failed to load location status");
                    return null;
                });
    }

    public void updateLocation() {
        locationService.updateUserLocation(currentUserId)
                .thenAccept(_locationStatus::postValue)
                .exceptionally(throwable -> {
                    setError("Failed to update location");
                    return null;
                });
    }
}
