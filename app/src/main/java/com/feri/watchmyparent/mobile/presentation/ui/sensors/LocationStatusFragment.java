package com.feri.watchmyparent.mobile.presentation.ui.sensors;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.feri.watchmyparent.mobile.R;
import com.feri.watchmyparent.mobile.application.dto.LocationDataDTO;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LocationStatusFragment extends Fragment{

    private LocationStatusViewModel viewModel;
    private TextView statusText;
    private TextView coordinatesText;
    private TextView addressText;
    private TextView timestampText;
    private View statusIndicator;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_location_status, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(LocationStatusViewModel.class);

        initializeViews(view);
        observeViewModel();

        viewModel.loadLocationStatus();
    }

    private void initializeViews(View view) {
        statusText = view.findViewById(R.id.tv_status);
        coordinatesText = view.findViewById(R.id.tv_coordinates);
        addressText = view.findViewById(R.id.tv_address);
        timestampText = view.findViewById(R.id.tv_timestamp);
        statusIndicator = view.findViewById(R.id.view_status_indicator);

        // Verificare că view-urile au fost găsite
        if (statusText == null) {
            throw new RuntimeException("tv_status not found in fragment_location_status.xml");
        }
        if (coordinatesText == null) {
            throw new RuntimeException("tv_coordinates not found in fragment_location_status.xml");
        }
        if (addressText == null) {
            throw new RuntimeException("tv_address not found in fragment_location_status.xml");
        }
        if (timestampText == null) {
            throw new RuntimeException("tv_timestamp not found in fragment_location_status.xml");
        }
        if (statusIndicator == null) {
            throw new RuntimeException("view_status_indicator not found in fragment_location_status.xml");
        }
    }

    private void observeViewModel() {
        viewModel.getLocationStatus().observe(getViewLifecycleOwner(), this::updateLocationUI);
    }

    private void updateLocationUI(LocationDataDTO location) {
        if (location == null) {
            statusText.setText("Location Unknown");
            statusIndicator.setBackgroundColor(getResources().getColor(R.color.unknown_gray));
            return;
        }

        statusText.setText(location.getStatus());
        addressText.setText(location.getAddress());
        timestampText.setText(location.getTimestamp().toString());

        if (location.isAtHome()) {
            statusIndicator.setBackgroundColor(getResources().getColor(R.color.home_green));
            coordinatesText.setVisibility(View.GONE);
        } else {
            statusIndicator.setBackgroundColor(getResources().getColor(R.color.away_orange));
            coordinatesText.setText(location.getFormattedCoordinates());
            coordinatesText.setVisibility(View.VISIBLE);
        }
    }
}
