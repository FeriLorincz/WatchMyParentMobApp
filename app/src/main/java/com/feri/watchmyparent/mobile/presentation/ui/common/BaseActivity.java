package com.feri.watchmyparent.mobile.presentation.ui.common;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public abstract class BaseActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(getClass().getSimpleName(), "Creating activity: " + getClass().getSimpleName());

    }

    protected void setupToolbar(Toolbar toolbar, String title, boolean showBack) {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(showBack);
            getSupportActionBar().setDisplayShowHomeEnabled(showBack);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void showLoading(boolean show) {
        // Override in subclasses to show/hide loading indicator
    }

    protected void showError(String message) {
        // Override in subclasses to show error messages

        Log.e(getClass().getSimpleName(), "Error: " + message);
    }

    protected void showSuccess(String message) {
        // Override in subclasses to show success messages

        Log.d(getClass().getSimpleName(), "Success: " + message);
    }
}
