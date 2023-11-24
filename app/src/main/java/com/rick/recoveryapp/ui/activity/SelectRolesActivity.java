package com.rick.recoveryapp.ui.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.rick.recoveryapp.ui.activity.u3d.U3DActivity;
import com.rick.recoveryapp.ui.activity.u3d.U3DDialogActivity;
import com.rick.recoveryapp.base.XPageActivity;
import com.rick.recoveryapp.ui.activity.helper.BtDataProX;
import com.rick.recoveryapp.databinding.ActivitySelectrolesBinding;
import com.rick.recoveryapp.ui.activity.u3d.U3DFactory;
import com.rick.recoveryapp.utils.APKVersionInfoUtils;
import com.rick.recoveryapp.utils.LocalConfig;
import com.xuexiang.xhttp2.XHttp;
import com.xuexiang.xhttp2.callback.SimpleCallBack;
import com.xuexiang.xhttp2.exception.ApiException;
import com.xuexiang.xui.utils.StatusBarUtils;

import java.util.Objects;

public class SelectRolesActivity extends XPageActivity {

    ActivitySelectrolesBinding binding;
    Context context;
    BtDataProX btDataPro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            binding = ActivitySelectrolesBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            StatusBarUtils.translucent(this);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            context = this;

            initClick();
            LocalConfig.ip = "180.102.132.148";
            LocalConfig.sex = -1;
            btDataPro = new BtDataProX();

            binding.Box1.setEnabled(false);
            binding.Box2.setEnabled(false);
            binding.Box3.setEnabled(false);
            binding.Box4.setEnabled(false);

            binding.Box1.setChecked(false);
            binding.Box2.setChecked(false);
            binding.Box3.setChecked(false);
            binding.Box4.setChecked(false);
            //  CheckVersion();

        } catch (Exception e) {
            Log.d("error", Objects.requireNonNull(e.getMessage()));
            //     android.view.InflateException: Binary XML file line #19 in com.rick.recoveryapp:layout/xui_dialog_loading: Binary XML file line #19 in com.rick.recoveryapp:layout/xui_dialog_loading: Error inflating class <unknown>
        }
    }

    public void initClick() {
        binding.initTxtSetting.setOnClickListener(v -> {
            Intent in = new Intent(context, U3DDialogActivity.class);
            startActivity(in);
        });

        binding.initTxtNext.setOnClickListener(v -> {
            if (!LocalConfig.netWorkCheck(context)) {
                Toast.makeText(context, "当前网络异常，请检查网络连接", Toast.LENGTH_SHORT).show();
                return;
            }

            if (LocalConfig.sex < 0) {
                Toast.makeText(context, "请先选择角色，再进入情景模式", Toast.LENGTH_SHORT).show();
            } else {
                System.out.println("111111111111111111");
//                    if (BaseApplication.mConnectService != null)
                btDataPro = null;
                AdminMainActivity.instance.finish();
                U3DActivity.newU3DActivity(this);

                finish();
            }
        });

        binding.initImgRole1.setOnClickListener(v -> {
            if (LocalConfig.sex == 3) {
                binding.Box1.setChecked(false);
                LocalConfig.sex = -1;
            } else {
                LocalConfig.sex = 3;
                binding.Box1.setChecked(true);
                binding.Box2.setChecked(false);
                binding.Box3.setChecked(false);
                binding.Box4.setChecked(false);
            }
        });

        binding.initImgRole2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (LocalConfig.sex == 2) {
                    binding.Box2.setChecked(false);
                    LocalConfig.sex = -1;
                } else {
                    LocalConfig.sex = 2;
                    binding.Box1.setChecked(false);
                    binding.Box2.setChecked(true);
                    binding.Box3.setChecked(false);
                    binding.Box4.setChecked(false);
                }
            }
        });

        binding.initImgRole3.setOnClickListener(v -> {
            if (LocalConfig.sex == 1) {
                binding.Box3.setChecked(false);
                LocalConfig.sex = -1;
            } else {
                LocalConfig.sex = 1;
                binding.Box1.setChecked(false);
                binding.Box2.setChecked(false);
                binding.Box3.setChecked(true);
                binding.Box4.setChecked(false);
            }
        });

        binding.initImgRole4.setOnClickListener(v -> {
            if (LocalConfig.sex == 0) {
                binding.Box4.setChecked(false);
                LocalConfig.sex = -1;
            } else {
                LocalConfig.sex = 0;
                binding.Box1.setChecked(false);
                binding.Box2.setChecked(false);
                binding.Box3.setChecked(false);
                binding.Box4.setChecked(true);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        btDataPro = null;
        System.out.println("SelectRolesActivity已被销毁========");
    }
}
