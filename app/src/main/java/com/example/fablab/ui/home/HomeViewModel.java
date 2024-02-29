package com.example.fablab.ui.home;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {
    private final MutableLiveData<Boolean> isGridLayoutVisible = new MutableLiveData<>();

    public MutableLiveData<Boolean> getIsGridLayoutVisible() {
        return isGridLayoutVisible;
    }

    public void setIsGridLayoutVisible(boolean isVisible) {
        isGridLayoutVisible.setValue(isVisible);
    }


}