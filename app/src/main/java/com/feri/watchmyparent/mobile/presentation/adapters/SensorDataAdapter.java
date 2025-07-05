package com.feri.watchmyparent.mobile.presentation.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.feri.watchmyparent.mobile.R;
import com.feri.watchmyparent.mobile.application.dto.SensorDataDTO;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SensorDataAdapter extends RecyclerView.Adapter<SensorDataAdapter.SensorViewHolder>{

    private List<SensorDataDTO> sensorData = new ArrayList<>();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public void updateData(List<SensorDataDTO> newData) {
        this.sensorData.clear();
        if (newData != null) {
            this.sensorData.addAll(newData);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SensorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sensor_data, parent, false);
        return new SensorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SensorViewHolder holder, int position) {
        if (position < sensorData.size()) {
            SensorDataDTO sensor = sensorData.get(position);
            holder.bind(sensor);
        }
    }

    @Override
    public int getItemCount() {
        return sensorData.size();
    }

    static class SensorViewHolder extends RecyclerView.ViewHolder {
        private final TextView sensorNameText;
        private final TextView sensorValueText;
        private final TextView timestampText;
        private final View transmissionIndicator;

        public SensorViewHolder(@NonNull View itemView) {
            super(itemView);
            sensorNameText = itemView.findViewById(R.id.tv_sensor_name);
            sensorValueText = itemView.findViewById(R.id.tv_sensor_value);
            timestampText = itemView.findViewById(R.id.tv_timestamp);
            transmissionIndicator = itemView.findViewById(R.id.view_transmission_indicator);
        }

        public void bind(SensorDataDTO sensor) {
            if (sensor == null) return;

            // Sensor name
            if (sensor.getSensorType() != null) {
                sensorNameText.setText(sensor.getSensorType().getDisplayName());
            } else {
                sensorNameText.setText("Unknown Sensor");
            }

            // Sensor value
            sensorValueText.setText(sensor.getFormattedValue());

            // Timestamp
            if (sensor.getTimestamp() != null) {
                timestampText.setText(sensor.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            } else {
                timestampText.setText("--:--:--");
            }

            // Transmission status indicator
            int indicatorColor = sensor.isTransmitted()
                    ? R.color.transmitted_green
                    : R.color.pending_orange;

            if (transmissionIndicator != null) {
                transmissionIndicator.setBackgroundColor(
                        itemView.getContext().getColor(indicatorColor)
                );
            }
        }
    }
}
