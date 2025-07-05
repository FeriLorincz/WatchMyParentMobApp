package com.feri.watchmyparent.mobile.presentation.ui.contacts;

import com.feri.watchmyparent.mobile.presentation.ui.common.BaseViewModel;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class EmergencyContactsViewModel extends BaseViewModel {

    @Inject
    public EmergencyContactsViewModel() {
    }

    public void loadContacts() {
        setLoading(false);
    }
}
