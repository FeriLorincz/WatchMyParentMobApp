package com.feri.watchmyparent.mobile.presentation.ui.contacts;

import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.feri.watchmyparent.mobile.R;
import com.feri.watchmyparent.mobile.presentation.ui.common.BaseActivity;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class EmergencyContactsActivity extends BaseActivity{

    private EmergencyContactsViewModel viewModel;
    private RecyclerView rvContacts;
    private FloatingActionButton fabAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_contacts);

        viewModel = new ViewModelProvider(this).get(EmergencyContactsViewModel.class);

        setupToolbar((Toolbar) findViewById(R.id.toolbar), "Emergency Contacts", true);
        initializeViews();
        observeViewModel();

        viewModel.loadContacts();
    }

    private void initializeViews() {
        rvContacts = findViewById(R.id.rv_contacts);
        fabAdd = findViewById(R.id.fab_add);

        rvContacts.setLayoutManager(new LinearLayoutManager(this));

        fabAdd.setOnClickListener(v -> {
            showSuccess("Add contact (MVP - not implemented)");
        });
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, this::showLoading);
        viewModel.getError().observe(this, this::showError);
        viewModel.getSuccess().observe(this, this::showSuccess);
    }
}
