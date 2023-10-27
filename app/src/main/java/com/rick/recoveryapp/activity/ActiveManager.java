package com.rick.recoveryapp.activity;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;

import com.common.base.BaseFrameLayout;
import com.rick.recoveryapp.R;


public class ActiveManager extends BaseFrameLayout{

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


    }

    @Override
    protected int getLayout() {
        return R.layout.activity_manager;
    }
}
