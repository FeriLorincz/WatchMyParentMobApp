package com.feri.watchmyparent.mobile.presentation.ui.profile;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import com.feri.watchmyparent.mobile.R;
import com.feri.watchmyparent.mobile.presentation.ui.common.BaseActivity;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PersonalDataActivity extends BaseActivity {

    private PersonalDataViewModel viewModel;
    private EditText etFirstName, etLastName, etEmail, etPhone;
    private EditText etCity, etStreet, etNumber, etCountry;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_data);

        viewModel = new ViewModelProvider(this).get(PersonalDataViewModel.class);

        setupToolbar((Toolbar) findViewById(R.id.toolbar), "Personal Data", true);
        initializeViews();
        observeViewModel();

        viewModel.loadUserData();
    }

    private void initializeViews() {
        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etCity = findViewById(R.id.et_city);
        etStreet = findViewById(R.id.et_street);
        etNumber = findViewById(R.id.et_number);
        etCountry = findViewById(R.id.et_country);
        btnSave = findViewById(R.id.btn_save);

        btnSave.setOnClickListener(v -> savePersonalData());
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, this::showLoading);
        viewModel.getError().observe(this, this::showError);
        viewModel.getSuccess().observe(this, this::showSuccess);
    }

    private void savePersonalData() {
        // Simple implementation for MVP
        showSuccess("Personal data saved (MVP - not implemented)");
    }
}
