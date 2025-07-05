package com.feri.watchmyparent.mobile.presentation.ui.profile;

import com.feri.watchmyparent.mobile.presentation.ui.common.BaseViewModel;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MedicalProfileViewModel extends BaseViewModel{

    @Inject
    public MedicalProfileViewModel() {
    }

    public void loadMedicalProfile() {
        setLoading(false);
    }
}
