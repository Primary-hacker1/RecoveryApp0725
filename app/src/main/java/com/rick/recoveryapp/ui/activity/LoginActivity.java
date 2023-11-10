/*
 * Copyright (C) 2019 xuexiangjys(xuexiangjys@163.com)
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

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.method.TransformationMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.rick.recoveryapp.R;
import com.rick.recoveryapp.ui.BaseApplication;
import com.rick.recoveryapp.base.XPageActivity;
import com.rick.recoveryapp.databinding.ActivityLoginBinding;
import com.rick.recoveryapp.greendao.ActivitRecordDao;
import com.rick.recoveryapp.greendao.MacDrDao;
import com.rick.recoveryapp.greendao.entity.ActivitRecord;
import com.rick.recoveryapp.greendao.EcgDataDBDao;
import com.rick.recoveryapp.ui.activity.bean.AddressBean;
import com.rick.recoveryapp.ui.activity.bean.SharedPreferencesUtils;
import com.rick.recoveryapp.utils.HideKeyboard;
import com.rick.recoveryapp.utils.LocalConfig;
import com.xuexiang.xui.utils.StatusBarUtils;
import com.xuexiang.xui.widget.dialog.DialogLoader;
import com.xuexiang.xutil.XUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 登录页面
 *
 * @author xuexiang
 * @since 2019-11-17 22:21
 */
public class LoginActivity extends XPageActivity {

    String user, password;
    private boolean isHideFirst = true;  // 输入框密码是否是隐藏的，默认为true
    Context context;
    ActivityLoginBinding binding;
    private static final int REQUEST_COARSE_LOCATION = 0;
    // MiniLoadingDialog mMiniLoadingDialog;
    LocationManager locationManager;
    EcgDataDBDao ecgDataDao;
    MacDrDao macDrDao;
    ActivitRecordDao activitRecordDao;
    int uid = 0;
    public static LoginActivity loginActivity;
    private SharedPreferences sharedPreferences;

    // BluetoothAdapter mBluetoothAdapter;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            context = this;
            //  mMiniLoadingDialog = WidgetUtils.getMiniLoadingDialog(context);
            binding = ActivityLoginBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            loginActivity = this;
            //设置界面全屏
            StatusBarUtils.translucent(this);
            uid = getPackageUid(context, getProcessName());
            /**
             * 初始化数据库
             */
            BaseApplication myApp = (BaseApplication) getApplication();
            LocalConfig.daoSession = myApp.getDaoSession();
            ecgDataDao = LocalConfig.daoSession.getEcgDataDBDao();
            macDrDao = LocalConfig.daoSession.getMacDrDao();
            activitRecordDao = LocalConfig.daoSession.getActivitRecordDao();
            sharedPreferences = getSharedPreferences("Personal", MODE_PRIVATE);
            AgainInto();

            initClick();

            openBlueTooth();

        } catch (Exception e) {
            Log.d("1234567890", e.getMessage());
        }
    }

    private void AgainInto() {
        //如果获取为空就返回默认值
        boolean ck = sharedPreferences.getBoolean("ck_password", false);

        //如果是记住密码
        if (ck) {
            String name = sharedPreferences.getString("name", "");
            String password = sharedPreferences.getString("password", "");
            binding.loginEtxtUser.setText(name);
            binding.loginEtxtPassword.setText(password);
            //记住密码打上√
            binding.savePassword.setChecked(true);
        }
    }

    @SuppressLint("MissingPermission")
    private void openBlueTooth() {

        if (bluePermission()) {
            BaseApplication.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        } else {
            return;
        }

        if (!BaseApplication.mBluetoothAdapter.isEnabled()) {// 判断是否打开蓝牙
            //弹出对话框提示用户是后打开
            //  Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //startActivityForResult(intent, SEARCH_CODE);
            // 不做提示，强行打开
            BaseApplication.mBluetoothAdapter.enable();
        }
    }

    //Android12蓝牙权限申请
    private boolean bluePermission() {
        if (Build.VERSION.SDK_INT > 30) {
            if (ContextCompat.checkSelfPermission(this,
                    "android.permission.BLUETOOTH_SCAN")
                    != PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this,
                    "android.permission.BLUETOOTH_ADVERTISE")
                    != PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this,
                    "android.permission.BLUETOOTH_CONNECT")
                    != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        "android.permission.BLUETOOTH_SCAN",
                        "android.permission.BLUETOOTH_ADVERTISE",
                        "android.permission.BLUETOOTH_CONNECT"}, 1);
                return false;
            }
        }
        return true;
    }

    /**
     * @return 获取当前进程名称
     */
    public static String getProcessName() {
        try {
            File file = new File("/proc/" + android.os.Process.myPid() + "/" + "cmdline");
            BufferedReader mBufferedReader = new BufferedReader(new FileReader(file));
            String processName = mBufferedReader.readLine().trim();
            mBufferedReader.close();
            return processName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //获取已安装应用的 uid，-1 表示未安装此应用或程序异常
    public static int getPackageUid(Context context, String packageName) {
        try {
            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(packageName, 0);
            //   Logger.d(applicationInfo.uid);
            return applicationInfo.uid;
        } catch (Exception e) {
            return -1;
        }
    }

    public void initClick() {
        binding.loginBtnLogin.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View v) {
                user = binding.loginEtxtUser.getText().toString();
                password = binding.loginEtxtPassword.getText().toString();
                if (user.isEmpty() || password.isEmpty()) {
                    Toast.makeText(context, "请输入正确的登录账号或密码", Toast.LENGTH_LONG).show();
                    // XToastUtils.warning("请输入正确的登录账号或密码");
                    binding.loginEtxtUser.setText("");
                    binding.loginEtxtPassword.setText("");
                } else {
                    if (BaseApplication.mBluetoothAdapter != null) {// 判断是否打开蓝牙
                        // 不做提示，强行打开
                        if (!BaseApplication.mBluetoothAdapter.isEnabled()) {
                            BaseApplication.mBluetoothAdapter.enable();
                        }
                        boolean gpsEnabled = Settings.Secure.isLocationProviderEnabled(getContentResolver(), LocationManager.GPS_PROVIDER);
                        if (!gpsEnabled) {
                            OpenGPS();
                        } else {
                            LoginJudgment();
                        }
                    } else {
                        Toast.makeText(context, "将进行设备连接，请开启平板蓝牙开关", Toast.LENGTH_LONG).show();
                        //  LoginJudgment();
                    }
                }
            }
        });

        binding.imageView.setOnClickListener(v -> {
            if (isHideFirst) {
                binding.imageView.setImageResource(R.drawable.visible_click);
                //密文
                HideReturnsTransformationMethod method1 = HideReturnsTransformationMethod.getInstance();
                binding.loginEtxtPassword.setTransformationMethod(method1);
                isHideFirst = false;
            } else {
                binding.imageView.setImageResource(R.drawable.visible);
                //密文
                TransformationMethod method = PasswordTransformationMethod.getInstance();
                binding.loginEtxtPassword.setTransformationMethod(method);
                isHideFirst = true;
            }
            // 光标的位置
            int index = binding.loginEtxtPassword.getText().toString().length();
            binding.loginEtxtPassword.setSelection(index);
        });

        binding.loginTxtBreak.setOnClickListener(v -> DialogLoader.getInstance().showConfirmDialog(
                context,
                getString(R.string.login_break),
                getString(R.string.lab_yes),
                (dialog, which) -> {
                    dialog.dismiss();

                    SharedPreferencesUtils.Companion.getInstance().logout();

                    XUtil.exitApp();
                },
                getString(R.string.lab_no),
                (dialog, which) -> {
                    dialog.dismiss();
                }
        ));

    }

    public void OpenGPS() {

        locationManager = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(context, "GPS未开启,请手动开启", Toast.LENGTH_SHORT).show();
            Intent callGPSSettingIntent = new Intent(
                    Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(callGPSSettingIntent);
        }
    }

    //系统方法,从requestPermissions()方法回调结果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //确保是我们的请求
        if (requestCode == REQUEST_COARSE_LOCATION) {
            if (grantResults[0] == PERMISSION_GRANTED) {
                LocalConfig.falg = true;
            } else {
                LocalConfig.falg = false;
                Toast.makeText(context, "权限开启失败，无法连接到蓝牙设备！", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    //  用户登录接口
    public void LoginJudgment() {
        boolean isfer = sharedPreferences.getBoolean("isfer", true);
        if (isfer) {
            if (user.equals("admin") && password.equals("123456")) {
                deleteTime();
                if (binding.savePassword.isChecked()) {
                    //把用户名和密码保存在SharedPreferences中
                    sharedPreferences.edit().putBoolean("ck_password", true).apply();
                } else {//没有勾选
                    sharedPreferences.edit().putBoolean("ck_password", false).apply();
                }
                sharedPreferences.edit().putString("name", user).apply();
                sharedPreferences.edit().putString("password", password).apply();
                //第一次进入跳转 进入设备蓝牙绑定界面
//                Intent in = new Intent(context, MacDrDialog.class);
//                in.putExtra("isfer", "Y");
//                startActivity(in);

                if (BaseApplication.mConnectService != null)
                    BaseApplication.mConnectService.stop();

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                BaseApplication.mBluetoothAdapter.enable();


                AdminMainActivity.newAdminMainActivity(context, new AddressBean());
                finish();
            } else {
                Toast.makeText(context, "请输入正确的登录账号或密码", Toast.LENGTH_SHORT).show();
                binding.loginEtxtUser.setText("");
                binding.loginEtxtPassword.setText("");
                // return;
            }
        } else {
            String Shname = sharedPreferences.getString("name", "");
            String Shpassword = sharedPreferences.getString("password", "");
            if (user.equals(Shname) && password.equals(Shpassword)) {
                deleteTime();
                if (binding.savePassword.isChecked()) {
                    //把用户名和密码保存在SharedPreferences中
                    sharedPreferences.edit().putString("name", user).apply();
                    sharedPreferences.edit().putString("password", password).apply();
                    sharedPreferences.edit().putBoolean("ck_password", true).apply();
                } else {//没有勾选
                    sharedPreferences.edit().putBoolean("ck_password", false).apply();
                }
                //第二次进入跳转 进入主界面
                AdminMainActivity.newAdminMainActivity(this
                        , new AddressBean());
                finish();
            } else {
                Toast.makeText(context, "请输入正确的登录账号或密码", Toast.LENGTH_SHORT).show();
                binding.loginEtxtUser.setText("");
                binding.loginEtxtPassword.setText("");
            }
        }


    }

    //自动删除2个月前的数据
    public void deleteTime() {
        try {
            //查询表中所有数据
            List<ActivitRecord> Rlist = activitRecordDao.loadAll();

            //循环比较时长
            for (ActivitRecord Alist : Rlist) {

                SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd" ,Locale.CHINA);
                Date curDate = new Date(System.currentTimeMillis());
                String crentime = formatter.format(curDate);

                long RecordID = Alist.getID();
                String RecordTime = Alist.getRecordTime();
                long difference = dateDiff(RecordTime, crentime);
                //设置删除的过期时长
                if (difference >= 7) {
                    activitRecordDao.deleteByKey(RecordID);
                }
            }
        } catch (Exception e) {
            Log.d("1", Objects.requireNonNull(e.getMessage()));
        }
    }

    //计算两日期的时间间隔
    public long dateDiff(String startTime, String endTime) {
        // 按照传入的格式生成一个simpledateformate对象
        //  SimpleDateFormat sd = new SimpleDateFormat(format);
        SimpleDateFormat sd = new SimpleDateFormat("yyyy/MM/dd", Locale.CHINA);
        long nd = 1000 * 24 * 60 * 60;// 一天的毫秒数
        long diff;
        long day = 0;
        try {
            // 获得两个时间的毫秒时间差异
            diff = sd.parse(endTime).getTime() - sd.parse(startTime).getTime();
            day = diff / nd;// 计算差多少天
            if (day >= 1) {
                return day;
            } else {
                if (day == 0) {
                    return 1;
                } else {
                    return 0;
                }
            }
        } catch (Exception e) {
        }
        return 0;
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

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (uid > 0) {
            boolean rstA = isAppRunning(context, getProcessName());
            boolean rstB = isProcessRunning(context, uid);
            if (rstA || rstB) {
                //指定包名的程序正在运行中
                Log.d("LoginActivity", "指定包名的程序正在运行中");
            } else {
                //指定包名的程序未在运行中
                Log.d("LoginActivity", "指定包名的程序未在运行中");
            }
        }  //应用未安装
    }

    /**
     * 方法描述：判断某一应用是否正在运行
     * Created by cafeting on 2017/2/4.
     *
     * @param context     上下文
     * @param packageName 应用的包名
     * @return true 表示正在运行，false 表示没有运行
     */
    public static boolean isAppRunning(Context context, String packageName) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(100);
        if (list.isEmpty()) {
            return false;
        }
        for (ActivityManager.RunningTaskInfo info : list) {
            assert info.baseActivity != null;
            if (info.baseActivity.getPackageName().equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断某一 uid 的程序是否有正在运行的进程，即是否存活
     * Created by cafeting on 2017/2/4.
     *
     * @param context 上下文
     * @param uid     已安装应用的 uid
     * @return true 表示正在运行，false 表示没有运行
     */
    public static boolean isProcessRunning(Context context, int uid) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> runningServiceInfos = am.getRunningServices(200);
        if (!runningServiceInfos.isEmpty()) {
            for (ActivityManager.RunningServiceInfo appProcess : runningServiceInfos) {
                if (uid == appProcess.uid) {
                    return true;
                }
            }
        }
        return false;
    }

}
