package com.feri.watchmyparent.mobile.presentation.ui.profile;

import com.feri.watchmyparent.mobile.presentation.ui.common.BaseViewModel;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class PersonalDataViewModel extends BaseViewModel{

    @Inject
    public PersonalDataViewModel() {
    }

    public void loadUserData() {
        // MVP implementation - basic loading
        setLoading(false);
    }
}
