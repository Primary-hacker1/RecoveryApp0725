/*
 * Copyright (C) 2021 xuexiangjys(xuexiangjys@163.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.rick.recoveryapp.ui.activity;

import static com.rick.recoveryapp.R.*;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Toast;


import com.rick.recoveryapp.ui.activity.helper.Constants;
import com.rick.recoveryapp.ui.activity.bean.AddressBean;
import com.rick.recoveryapp.ui.activity.u3d.U3DActivity;
import com.rick.recoveryapp.ui.BaseApplication;
import com.rick.recoveryapp.base.XPageActivity;
import com.rick.recoveryapp.ui.dialog.DialogActivity;
import com.rick.recoveryapp.ui.service.BluetoothChatServiceX;
import com.rick.recoveryapp.ui.activity.helper.BtDataProX;
import com.rick.recoveryapp.ui.service.BtKeepService;
import com.rick.recoveryapp.ui.service.BtReceiver;
import com.rick.recoveryapp.databinding.ActivityMainBinding;
import com.rick.recoveryapp.entity.LiveMessage;
import com.rick.recoveryapp.utils.BaseUtil;
import com.rick.recoveryapp.utils.DateUtil;
import com.rick.recoveryapp.utils.LiveDataBus;
import com.rick.recoveryapp.utils.LocalConfig;
import com.xuexiang.xui.utils.StatusBarUtils;
import com.xuexiang.xutil.XUtil;
import com.xuexiang.xutil.common.ClickUtils;
import com.xuexiang.xutil.tip.ToastUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class AdminMainActivity extends XPageActivity implements ClickUtils.OnClick2ExitListener, BtReceiver.Listener {


    Context context;
    Intent intent;
    ActivityMainBinding binding;
    BtDataProX btDataPro;
    public static AdminMainActivity instance;

    public static void newAdminMainActivity(Context context, AddressBean addressBean) {
        Intent intent = new Intent(context, AdminMainActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable("AddressBean", addressBean);
        intent.putExtra("bundle", bundle);
        context.startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("ActivitytTest", "开始调用onCreate方法");
        try {
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            StatusBarUtils.translucent(this);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            context = this;
            instance = this;
            btDataPro = new BtDataProX();

            SharedPreferences shared = getSharedPreferences("Personal", MODE_PRIVATE);
            boolean isfer = shared.getBoolean("isfer", true);
            if (isfer) {
                LoginActivity.loginActivity.finish();
                SharedPreferences.Editor editor = shared.edit();
                editor.putBoolean("isfer", false);
                editor.commit();
            }
            // mBtReceiver = new BtReceiver(context, this);//注册蓝牙广播
            // binding.mainTxtBt.setCenterString("蓝牙未连接");
            if (LocalConfig.daoSession == null) {
                BaseApplication myApp = (BaseApplication) getApplication();
                LocalConfig.daoSession = myApp.getDaoSession();
            }
            initClick();
            init();
            binding.mainTxtDate.setCenterString(DateUtil.getWeekOfDate(new Date()));
            binding.mainTxtDate.setCenterBottomString(DateUtil.getNowDate());

            if (!LocalConfig.isControl) {
                Intent intent = new Intent(this, BtKeepService.class);
                startService(intent);
            }
            controlBT();


            Intent intent = getIntent();
            if (intent == null) {
                return;
            }
            Bundle bundle = intent.getBundleExtra("bundle");

            if (bundle == null) {
                return;
            }

            AddressBean bean = (AddressBean) bundle.getParcelable("AddressBean");

            if (bean == null) {
                return;
            }

            LiveDataBus.get().with(Constants.BT_RECONNECTED).observe(this, v -> {
                if (BaseUtil.isFastDoubleClick()) {
                    return;
                }

//                AddressBean addressBean = SharedPreferencesUtils.Companion.getInstance().getAddressString();
//                if (addressBean != null) {
//                    btDataPro.sendBTMessage(btDataPro.
//                            GetCmdCode(addressBean.getEcg(),
//                                    addressBean.getBloodPressure(),
//                                    addressBean.getBloodOxygen()));
//                } else {
//                    btDataPro.sendBTMessage(SerialPort.Companion.sendCmdAddress(bean));
//                }

//                LogUtils.e(tag + addressBean);
            });


        } catch (Exception ex) {
            Toast.makeText(context, ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void controlBT() {
        LiveDataBus.get().with(Constants.BT_CONNECTED).observe(this, v -> {
            if (v instanceof LiveMessage) {
                LiveMessage msg = (LiveMessage) v;
                try {
                    if (msg.getIsConnt()) {
                        Log.d("BT_CONNECTED1", LocalConfig.isControl + " 1");
                        binding.mainImgLink.setBackgroundResource(drawable.img_bt_open);
                        binding.mainImgLink.setEnabled(false);
                        Toast.makeText(AdminMainActivity.this, msg.getMessage(), Toast.LENGTH_SHORT).show();
                        btDataPro.sendBTMessage(btDataPro.getCONNECT_CLOSE());
//                        AddressBean addressBean = SharedPreferencesUtils.Companion.getInstance().getAddressString();
//                        if (addressBean != null) {
//                            btDataPro.sendBTMessage(btDataPro.
//                                    GetCmdCode(addressBean.getEcg(),
//                                            addressBean.getBloodPressure(),
//                                            addressBean.getBloodOxygen()));
//                        }
                    } else {
                        Log.d("BT_CONNECTED1", LocalConfig.isControl + " 2");
                        binding.mainImgLink.setBackgroundResource(drawable.img_bt_close);
                        binding.mainImgLink.setEnabled(true);
                        if (!msg.getMessage().isEmpty()) {
                            Toast.makeText(AdminMainActivity.this, msg.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception e) {
                    Log.d("AdminMainActivity", Objects.requireNonNull(e.getMessage()));
                }
            }
        });
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (BaseApplication.mConnectService != null) {
            //蓝牙闲置状态
            if (BaseApplication.mConnectService.getState() == BluetoothChatServiceX.STATE_NONE) {
                if (BaseApplication.liveMessage != null) {
                    binding.mainImgLink.setBackgroundResource(drawable.img_bt_close);
                    binding.mainImgLink.setEnabled(true);
                    //监听其他蓝牙主设备
                    BaseApplication.mConnectService.start();
                }
                //蓝牙已连接
            } else if (BaseApplication.mConnectService.getState() == BluetoothChatServiceX.STATE_CONNECTED) {
                if (BaseApplication.liveMessage != null) {
                    binding.mainImgLink.setBackgroundResource(drawable.img_bt_open);
                    binding.mainImgLink.setEnabled(false);
                }
            }
        }
    }

    public void initClick() {
        binding.mainImgLink.setOnClickListener(v -> {
            if (BaseUtil.isFastDoubleClick()) {
                return;
            }
            BaseApplication.AutoConnect();
        });

        binding.mainImgActive.setOnClickListener(v -> {
            if (LocalConfig.isControl) {
                LocalConfig.ModType = 0;
                intent = new Intent(context, DialogActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(context, "请先连接蓝牙设备！", Toast.LENGTH_SHORT).show();
            }
        });

        binding.mainImgPassive.setOnClickListener(v -> {
            if (LocalConfig.isControl) {
                LocalConfig.ModType = 1;
                intent = new Intent(context, DialogActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(context, "请先连接蓝牙设备！", Toast.LENGTH_SHORT).show();
            }
        });

        binding.mainImgIntelligence.setOnClickListener(v -> {
            if (LocalConfig.isControl) {
                LocalConfig.ModType = 2;
                intent = new Intent(context, DialogActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(context, "请先连接蓝牙设备！", Toast.LENGTH_SHORT).show();
            }
        });

        binding.mainImgEntertainment.setOnClickListener(v -> {
            try {
                if (LocalConfig.isControl) {
                    LocalConfig.ModType = 3;
                    intent = new Intent(context, DialogActivity.class);
                    startActivity(intent);
                    // finish();
                } else {
                    Toast.makeText(context, "请先连接蓝牙设备！", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception ex) {
                ToastUtils.toast(ex.getMessage());
            }
        });

        binding.mainImgHistory.setOnClickListener(v -> {
            intent = new Intent(context, HistoryActivity.class);
            startActivity(intent);
            finish();
        });

        binding.mainImgSetting.setOnClickListener(v -> {
            intent = new Intent(context, SettingActivity.class);
            startActivity(intent);
            finish();
        });
    }

    // 初始化方法
    public void init() {
        // 新时间
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        binding.mainTxtTime.setCenterString(sdf.format(new Date()));
        // 更新时间的广播
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        registerReceiver(receiver, filter);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            assert action != null;
            if (action.equals(Intent.ACTION_TIME_TICK)) {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                binding.mainTxtTime.setCenterString(sdf.format(new Date()));
            }
        }
    };

    /**
     * 菜单、返回键响应
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            ClickUtils.exitBy2Click(2000, AdminMainActivity.this);
        }
        return true;
    }

    @Override
    public void onRetry() {
        Toast.makeText(context, "再按一次退出程序", Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onExit() {
        btDataPro.sendBTMessage(btDataPro.getCONNECT_CLOSE());
        if (BaseApplication.mConnectService != null) {
            BaseApplication.mConnectService.stop();
        }
        BaseApplication.mBluetoothAdapter = null;
        BaseApplication.getContext().unregisterReceiver(BaseApplication.mBtReceiver);
        Intent stopIntent = new Intent(this, BtKeepService.class);
        stopService(stopIntent);
        if (U3DActivity.u3dinstance != null) {
            U3DActivity.u3dinstance.finish();
        }

        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
        XUtil.exitApp();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("ActivitytTest", "onDestroy");
        btDataPro = null;
    }

    @Override
    public void foundDev(BluetoothDevice dev) {

    }

    @Override
    public void foundBT() {

    }
}
