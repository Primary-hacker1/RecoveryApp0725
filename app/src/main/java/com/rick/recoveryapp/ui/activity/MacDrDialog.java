package com.rick.recoveryapp.ui.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.rick.recoveryapp.R;
import com.rick.recoveryapp.ui.BaseApplication;
import com.rick.recoveryapp.greendao.MacDrDao;
import com.rick.recoveryapp.ui.activity.helper.UriConfig;
import com.rick.recoveryapp.ui.activity.bean.AddressBean;
import com.rick.recoveryapp.ui.activity.bean.SharedPreferencesUtils;
import com.rick.recoveryapp.utils.HideKeyboard;
import com.rick.recoveryapp.utils.LocalConfig;
import com.xuexiang.xpage.base.XPageActivity;
import com.xuexiang.xui.widget.button.ButtonView;
import com.xuexiang.xui.widget.dialog.DialogLoader;
import com.xuexiang.xutil.XUtil;

public class MacDrDialog extends XPageActivity {

    EditText macdialog_bule, macdialog_ecg, macdialog_blood, macdialog_oxygen;
    ButtonView macdialog_close, macdialog_save;
    Context context;
    MacDrDao macDrDao;
    String isfer = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_macdialog);
        initViwe();
        Intent intent = getIntent();
        isfer = intent.getStringExtra("isfer");

        macDrDao = LocalConfig.daoSession.getMacDrDao();

        initClick();

        SharedPreferencesUtils sharedPreferencesUtils =  SharedPreferencesUtils.Companion.getInstance();

        AddressBean addressBean = sharedPreferencesUtils.getAddressString();

        if (addressBean != null) {

            macdialog_bule.setText(addressBean.getMacAddress());
            macdialog_ecg.setText(addressBean.getEcg());
            macdialog_blood.setText(addressBean.getBloodPressure());
            macdialog_oxygen.setText(addressBean.getBloodOxygen());

        } else {
            Toast.makeText(context, "蓝牙地址获取失败！", Toast.LENGTH_SHORT).show();
        }

    }

    private void initViwe() {
        macdialog_bule = findViewById(R.id.macdialog_bule);
        macdialog_ecg = findViewById(R.id.macdialog_ecg);
        macdialog_blood = findViewById(R.id.macdialog_blood);
        macdialog_oxygen = findViewById(R.id.macdialog_oxygen);
        macdialog_close = findViewById(R.id.macdialog_close);
        macdialog_save = findViewById(R.id.macdialog_save);


        if (UriConfig.test) {
//            String macAddress = "001B10F04B60";
            String macAddress = "001B10F1EE6E";

            String ecgAddress = "E3ADBA1DF806";
            String bloodAddress = "A4C138421CF3";
            String oxygenAddress = "00A0503BD222";

//            String ecgAddress = "D208AABB37AE";
//            String bloodAddress = "A4C13844160C";
//            String oxygenAddress = "00A0503BCBAC";

            macdialog_bule.setText(macAddress);
            macdialog_ecg.setText(ecgAddress);
            macdialog_blood.setText(bloodAddress);
            macdialog_oxygen.setText(oxygenAddress);
        }
    }

    public void initClick() {

        macdialog_close.setOnClickListener(v -> finish());

        macdialog_save.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View v) {
                String blue = macdialog_bule.getText().toString();
                String ecg = macdialog_ecg.getText().toString();
                String blood = macdialog_blood.getText().toString();
                String oxygen = macdialog_oxygen.getText().toString();

                if (blue.length() != 12 || ecg.length() != 12 || blood.length() != 12 || oxygen.length() != 12) {
                    Toast.makeText(context, "输入框内容不足12位，请检查！", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (isfer == null) {
                    return;
                }

                String macAddress = macdialog_bule.getText().toString();
                String ecgAddress = macdialog_ecg.getText().toString();
                String bloodAddress = macdialog_blood.getText().toString();
                String oxygenAddress = macdialog_oxygen.getText().toString();

                SharedPreferencesUtils.Companion.getInstance()
                        .setAddressString(new AddressBean(macAddress,
                                ecgAddress,
                                bloodAddress,
                                oxygenAddress
                        ));//sp储存

                if (isfer.equals("Y")) {

                    AdminMainActivity.newAdminMainActivity(context, new AddressBean(macAddress,
                            ecgAddress, bloodAddress, oxygenAddress));
                    finish();
                } else if (isfer.equals("setting")) {
                    DialogLoader.getInstance().showConfirmDialog(
                            context,
                            getString(R.string.setting_out),
                            getString(R.string.lab_ok),
                            (dialog, which) -> {
                                dialog.dismiss();
                                if (BaseApplication.mConnectService != null)
                                    BaseApplication.mConnectService.stop();
                                BaseApplication.mBluetoothAdapter.enable();

                                android.os.Process.killProcess(android.os.Process.myPid());
                                System.exit(0);
                                XUtil.exitApp();

                            },
                            getString(R.string.lab_null),
                            (dialog, which) -> dialog.dismiss()
                    );

                }
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (HideKeyboard.isShouldHideKeyboard(v, ev)) {
                hideKeyboard(v.getWindowToken());
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * @param token 获取InputMethodManager，隐藏软键盘
     */
    private void hideKeyboard(IBinder token) {
        if (token != null) {
            InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            im.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

}
