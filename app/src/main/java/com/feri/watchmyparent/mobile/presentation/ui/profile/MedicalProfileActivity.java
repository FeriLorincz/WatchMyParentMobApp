package com.feri.watchmyparent.mobile.presentation.ui.profile;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import com.feri.watchmyparent.mobile.R;
import com.feri.watchmyparent.mobile.presentation.ui.common.BaseActivity;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MedicalProfileActivity extends BaseActivity {

    private MedicalProfileViewModel viewModel;
    private EditText etDiseases, etMedications, etAthleticHistory;
    private CheckBox cbGdprConsent, cbDisclaimerAccepted, cbEmergencyEntry;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical_profile);

        viewModel = new ViewModelProvider(this).get(MedicalProfileViewModel.class);

        setupToolbar((Toolbar) findViewById(R.id.toolbar), "Medical Profile", true);
        initializeViews();
        observeViewModel();

        viewModel.loadMedicalProfile();
    }

    private void initializeViews() {
        etDiseases = findViewById(R.id.et_diseases);
        etMedications = findViewById(R.id.et_medications);
        etAthleticHistory = findViewById(R.id.et_athletic_history);
        cbGdprConsent = findViewById(R.id.cb_gdpr_consent);
        cbDisclaimerAccepted = findViewById(R.id.cb_disclaimer_accepted);
        cbEmergencyEntry = findViewById(R.id.cb_emergency_entry);
        btnSave = findViewById(R.id.btn_save);

        btnSave.setOnClickListener(v -> saveMedicalProfile());
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, this::showLoading);
        viewModel.getError().observe(this, this::showError);
        viewModel.getSuccess().observe(this, this::showSuccess);
    }

    private void saveMedicalProfile() {
        showSuccess("Medical profile saved (MVP - not implemented)");
    }
}
