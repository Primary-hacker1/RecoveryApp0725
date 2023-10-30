package com.rick.recoveryapp.activity;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;

import com.common.base.BaseFrameLayout;
import com.rick.recoveryapp.R;
import com.rick.recoveryapp.databinding.ActivityManagerBinding;


public class ActiveManager extends BaseFrameLayout<ActivityManagerBinding> {

    public ActivityManagerBinding binding;

    public ActiveManager(@NonNull Context context) {
        super(context);
    }

    public ActiveManager(@NonNull Context context, @NonNull AttributeSet attributes) {
        super(context, attributes);
    }

    public ActiveManager(@NonNull Context context, @NonNull AttributeSet attributes, int i) {
        super(context, attributes, i);
    }

    @Override
    public void initView() {
        binding = baseBinding;


    }

    @Override
    protected int getLayout() {
        return R.layout.activity_manager;
    }
}
