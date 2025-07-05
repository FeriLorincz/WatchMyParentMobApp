package com.feri.watchmyparent.mobile.presentation.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.feri.watchmyparent.mobile.R;
import com.feri.watchmyparent.mobile.application.dto.SensorConfigurationDTO;
import com.feri.watchmyparent.mobile.application.dto.SensorDataDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SensorCardAdapter extends RecyclerView.Adapter<SensorCardAdapter.SensorCardViewHolder> {

    private List<SensorConfigurationDTO> configurations = new ArrayList<>();
    private Map<String, SensorDataDTO> sensorDataMap = new HashMap<>();

    private final OnSensorToggleListener toggleListener;
    private final OnFrequencyChangeListener frequencyListener;

    public interface OnSensorToggleListener {
        void onSensorToggle(SensorConfigurationDTO config);
    }

    public interface OnFrequencyChangeListener {
        void onFrequencyChange(SensorConfigurationDTO config, int newFrequency);
    }

    public SensorCardAdapter(OnSensorToggleListener toggleListener, OnFrequencyChangeListener frequencyListener) {
        this.toggleListener = toggleListener;
        this.frequencyListener = frequencyListener;
    }

    public void updateConfigurations(List<SensorConfigurationDTO> configs) {
        this.configurations.clear();
        if (configs != null) {
            this.configurations.addAll(configs);
        }
        notifyDataSetChanged();
    }

    public void updateSensorData(List<SensorDataDTO> sensorData) {
        sensorDataMap.clear();
        if (sensorData != null) {
            for (SensorDataDTO data : sensorData) {
                if (data.getSensorType() != null) {
                    sensorDataMap.put(data.getSensorType().getCode(), data);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SensorCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sensor_card, parent, false);
        return new SensorCardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SensorCardViewHolder holder, int position) {
        if (position < configurations.size()) {
            SensorConfigurationDTO config = configurations.get(position);
            SensorDataDTO data = null;

            if (config.getSensorType() != null) {
                data = sensorDataMap.get(config.getSensorType().getCode());
            }

            holder.bind(config, data);
        }
    }

    @Override
    public int getItemCount() {
        return configurations.size();
    }

    class SensorCardViewHolder extends RecyclerView.ViewHolder {
        private final TextView sensorNameText;
        private final TextView currentValueText;
        private final TextView frequencyText;
        private final SwitchCompat enabledSwitch;
        private final SeekBar frequencySeekBar;

        public SensorCardViewHolder(@NonNull View itemView) {
            super(itemView);
            sensorNameText = itemView.findViewById(R.id.tv_sensor_name);
            currentValueText = itemView.findViewById(R.id.tv_current_value);
            frequencyText = itemView.findViewById(R.id.tv_frequency);
            enabledSwitch = itemView.findViewById(R.id.switch_enabled);
            frequencySeekBar = itemView.findViewById(R.id.seekbar_frequency);
        }

        public void bind(SensorConfigurationDTO config, SensorDataDTO data) {
            if (config == null) return;

            // Sensor name
            if (config.getSensorType() != null) {
                sensorNameText.setText(config.getDisplayName());
            } else {
                sensorNameText.setText("Unknown Sensor");
            }

            // Current value
            if (data != null) {
                currentValueText.setText(data.getFormattedValue());
                currentValueText.setVisibility(View.VISIBLE);
            } else {
                currentValueText.setText("No data");
                currentValueText.setVisibility(View.VISIBLE);
            }

            // Frequency display
            frequencyText.setText(config.getFormattedFrequency());

            // Switch setup
            enabledSwitch.setOnCheckedChangeListener(null);
            enabledSwitch.setChecked(config.isEnabled());
            enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (toggleListener != null) {
                    toggleListener.onSensorToggle(config);
                }
            });

            // SeekBar setup
            frequencySeekBar.setMin(config.getMinFrequency());
            frequencySeekBar.setMax(config.getMaxFrequency());
            frequencySeekBar.setProgress(config.getFrequencySeconds());

            frequencySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        int seconds = Math.max(config.getMinFrequency(), progress);
                        frequencyText.setText(formatFrequency(seconds));
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    // Not needed
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    int newFrequency = Math.max(config.getMinFrequency(), seekBar.getProgress());
                    if (frequencyListener != null) {
                        frequencyListener.onFrequencyChange(config, newFrequency);
                    }
                }
            });
        }

        private String formatFrequency(int seconds) {
            if (seconds < 60) {
                return seconds + " seconds";
            } else if (seconds < 3600) {
                int minutes = seconds / 60;
                return minutes + " minute" + (minutes != 1 ? "s" : "");
            } else {
                int hours = seconds / 3600;
                return hours + " hour" + (hours != 1 ? "s" : "");
            }
        }
    }
}
