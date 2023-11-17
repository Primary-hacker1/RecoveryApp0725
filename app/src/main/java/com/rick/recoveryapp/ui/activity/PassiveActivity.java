package com.rick.recoveryapp.ui.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;

import com.common.network.LogUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rick.recoveryapp.R;
import com.rick.recoveryapp.ui.service.BluetoothChatService;
import com.rick.recoveryapp.ui.activity.helper.Constants;
import com.rick.recoveryapp.ui.activity.helper.UriConfig;
import com.rick.recoveryapp.ui.BaseApplication;
import com.rick.recoveryapp.base.XPageActivity;
import com.rick.recoveryapp.ui.activity.helper.BtDataProX;
import com.rick.recoveryapp.chart.MyAVG;
import com.rick.recoveryapp.databinding.ActivityPassiveBinding;
import com.rick.recoveryapp.entity.EcgData;
import com.rick.recoveryapp.entity.LiveMessage;
import com.rick.recoveryapp.entity.protocol.PoolMessage;
import com.rick.recoveryapp.entity.protocol.UploadData;
import com.rick.recoveryapp.greendao.ActivitRecordDao;
import com.rick.recoveryapp.greendao.EcgDataDBDao;
import com.rick.recoveryapp.greendao.RecordDetailedDao;
import com.rick.recoveryapp.greendao.entity.ActivitRecord;
import com.rick.recoveryapp.greendao.entity.RecordDetailed;
import com.rick.recoveryapp.ui.activity.bean.AddressBean;
import com.rick.recoveryapp.ui.activity.bean.SharedPreferencesUtils;
import com.rick.recoveryapp.utils.BaseUtil;
import com.rick.recoveryapp.utils.CRC16Util;
import com.rick.recoveryapp.utils.LiveDataBus;
import com.rick.recoveryapp.utils.LocalConfig;
import com.rick.recoveryapp.utils.MyTimeUtils;
import com.rick.recoveryapp.utils.PeterTimeCountRefresh;
import com.rick.recoveryapp.utils.TimeCountTool;
import com.xuexiang.xui.utils.StatusBarUtils;
import com.xuexiang.xui.widget.dialog.DialogLoader;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

/*
 * "被动模式需要重构界面冗余"
 * */
@Deprecated()
public class PassiveActivity extends XPageActivity {
    private boolean isCloseDialog = false;//如果是运动后停止
    String motionHeight;//运动前的高压
    int modleType = 0;
    ArrayList<Float> EcgListData;
    static ArrayList<Float> OftenListData;
    ArrayList<Integer> countList;
    RecordDetailedDao recordDetailedDao;
    EcgDataDBDao ecgDataDBDao;
    UploadData uploadData;
    EcgData ecgData;
    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    String contorlState = "52";
    BtDataProX btDataPro;
    String CMD_CODE = "";
    private Timer timer1;
    private TimerTask timerTask1;
    boolean isBegin = false;//是否在测量血压
    Context context;
    ActivitRecordDao activitRecordDao;
    public TimeCountTool timeCountTool = TimeCountTool.getInstance();
    String timeCount = "";
    double Total_mileage, Calories;
    ActivityPassiveBinding binding;

    static PeterTimeCountRefresh downTimer;
    static long nowTime = Constants.passiveTime;
    int zhuansu = 5;
    int spasmData = 1;
    Long activeTime = 0L;
    boolean isOk = false;
    String Passive_B_Diastole_Shrink = "0/0", Passive_L_Diastole_Shrink = "0/0";
    int spasmCount = 0;
    int BloodEndState = 0; // 0:初始状态  1：需要测量血压   2：血压测量完成
    boolean isNotBt = false;
    boolean isClickBlood = false;//是否运动前点击了测量血压

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityPassiveBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        StatusBarUtils.translucent(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        context = this;

        isNotBt = LocalConfig.isControl;

        btDataPro = new BtDataProX();
        itinClick();
        binding.activeTxtMassage.setLeftString(" 患者姓名：" + LocalConfig.userName);
        binding.activeTxtMassage.setLeftBottomString(" 患者编号：" + LocalConfig.medicalNumber);
        BaseApplication myApp = (BaseApplication) getApplication();
        LocalConfig.daoSession = myApp.getDaoSession();
        activitRecordDao = LocalConfig.daoSession.getActivitRecordDao();
        recordDetailedDao = LocalConfig.daoSession.getRecordDetailedDao();
        nowTime = Constants.passiveTime;
        activeTime = MyTimeUtils.Getminute(nowTime);
        String text1 = MyTimeUtils.formatTime(nowTime);
        binding.passiveTxtDowntimer.setCenterString(text1);
        EcgListData = new ArrayList<>();
        OftenListData = new ArrayList<>();
        countList = new ArrayList<>();
        binding.passiveWaveviewOne.resetCanavas();
        ecgDataDBDao = LocalConfig.daoSession.getEcgDataDBDao();

        try {
            initLiveData();
            binding.passiveWaveviewOne.resetCanavas();
            btDataPro.sendBTMessage(GetCmdCode("53", false, 5, 5, activeTime));
            binding.passiveTxtZhuansu.setCenterString(zhuansu + "");
            binding.passiveTxtSpasm.setCenterString(spasmData + "");
            PassEcg();
        } catch (Exception ex) {
            ex.getMessage();
        }

    }

    public void PassEcg() {
        timer1 = new Timer();
        timerTask1 = new TimerTask() {
            @Override
            public void run() {
                try {
                    if (OftenListData != null) {
                        if (!OftenListData.isEmpty()) {
                            float cooY = OftenListData.get(0);
                            binding.passiveWaveviewOne.showLine(cooY);
                            OftenListData.remove(0);
                        }
                    }
                } catch (Exception e) {
                    LogUtils.d(tag + "EcgError" + e.getMessage());
                }
            }
        };
        timer1.schedule(timerTask1, 1, 10);
    }

    public void initLiveData() {
        LiveDataBus.get().with(Constants.BT_PROTOCOL).observe(this, v -> {
            if (v instanceof PoolMessage) {
                PoolMessage msg = (PoolMessage) v;
                if (msg.isState()) {
//                    LogUtils.d("BT" + msg.getObjectName());
                    if (msg.getObjectName().equals(btDataPro.getUPLODE_ANSWER())) {
                        UploadData uploadData;
                        uploadData = gson.fromJson(msg.getObjectJson(), UploadData.class);
                        //  uploadData.getECG(),uploadData.getBlood(),uploadData.getBlood_oxy()
                        if (uploadData.getECG().equals("已连接")) {
                            binding.trainButEcg.setBackgroundResource(R.drawable.xindian_ok);
                        } else {
                            binding.trainButEcg.setBackgroundResource(R.drawable.xindian_no);
                        }
                        if (uploadData.getBlood().equals("已连接")) {
                            binding.trainButBp.setBackgroundResource(R.drawable.xueya_ok);
                        } else {
                            binding.trainButBp.setBackgroundResource(R.drawable.xueya_no);
                        }
                        if (uploadData.getBlood_oxy().equals("已连接")) {
                            binding.trainButO2.setBackgroundResource(R.drawable.o2_ok);
                        } else {
                            binding.trainButO2.setBackgroundResource(R.drawable.o2_no);
                        }
                    }

                    DataDisplay(msg.getObjectName(), msg.getObjectJson());

                } else {
                    LogUtils.d(tag + "BT" + "没有任何数据");
                }
            } else {
                LogUtils.d(tag + "BT" + "没有任何数据");
            }

        });

        LiveDataBus.get().with(Constants.BT_ECG).observe(this, v -> {
            if (v instanceof PoolMessage) {
                PoolMessage msg = (PoolMessage) v;

                DataDisplay(msg.getObjectName(), msg.getObjectJson());

                if (isBegin) {
                    updateProgress();
                }
            }
        });

        LiveDataBus.get().with(Constants.BT_CONNECTED).observe(this, v -> {
            if (v instanceof LiveMessage) {
                LiveMessage msg = (LiveMessage) v;
                if (msg.getState().equals("蓝牙设备未连接")) {
                    isBegin = false;//恢复不然退出不了界面
                    binding.mainImgLink.setBackgroundResource(R.drawable.img_bt_close);
                    binding.mainImgLink.setEnabled(true);
                    //监听其他蓝牙主设备
                    BaseApplication.mConnectService.start();
                }

                if (!msg.getIsConnt()) {
                    //未连接
                    binding.trainButEcg.setBackgroundResource(R.drawable.xindian_no);
                    binding.trainButBp.setBackgroundResource(R.drawable.xueya_no);
                    binding.trainButO2.setBackgroundResource(R.drawable.o2_no);

                    //未连接
                    binding.passiveTxtO2State.setCenterString("血氧仪未连接");
                    binding.passiveTxtBoxygen.setCenterString("0");

                    binding.passiveTxtBloodstate1.setCenterString("血压仪未连接");
                    binding.passiveTxtBloodstate2.setCenterString("血压仪未连接");

                    binding.passiveTxtCoory.setCenterString("0");
                    binding.passiveTxtEcgstate.setCenterString("心电仪未连接");
                    OftenListData.clear();
                }
            }
        });

        controlBT();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();

        if (BaseApplication.mConnectService != null) {
            //蓝牙闲置状态
            if (BaseApplication.mConnectService.getState() == BluetoothChatService.STATE_NONE) {
                if (BaseApplication.liveMessage != null) {
                    binding.mainImgLink.setBackgroundResource(R.drawable.img_bt_close);
                    binding.mainImgLink.setEnabled(true);
                    //监听其他蓝牙主设备
                    BaseApplication.mConnectService.start();
                }
                //蓝牙已连接
            } else if (BaseApplication.mConnectService.getState() == BluetoothChatService.STATE_CONNECTED) {
                if (BaseApplication.liveMessage != null) {
                    binding.mainImgLink.setBackgroundResource(R.drawable.img_bt_open);
                    binding.mainImgLink.setEnabled(false);
                }
            }
        }
    }


    public void controlBT() {
        LiveDataBus.get().with(Constants.BT_CONNECTED).observe(this, v -> {
            if (v instanceof LiveMessage) {
                LiveMessage msg = (LiveMessage) v;
                try {
                    if (msg.getIsConnt()) {
                        LogUtils.d(tag + "BT_CONNECTED1" + LocalConfig.isControl + " 1");
                        binding.mainImgLink.setBackgroundResource(R.drawable.img_bt_open);
                        binding.mainImgLink.setEnabled(false);
                        Toast.makeText(PassiveActivity.this, msg.getMessage(), Toast.LENGTH_SHORT).show();
                        btDataPro.sendBTMessage(btDataPro.getCONNECT_SEND());
                        AddressBean addressBean = SharedPreferencesUtils.Companion.getInstance().getAddressString();
                        if (addressBean != null) {
                            btDataPro.sendBTMessage(btDataPro.
                                    GetCmdCode(Objects.requireNonNull(addressBean.getEcg()),
                                            Objects.requireNonNull(addressBean.getBloodPressure()),
                                            Objects.requireNonNull(addressBean.getBloodOxygen())));
                        }
                    } else {
                        LogUtils.d(tag + "BT_CONNECTED1" + LocalConfig.isControl + " 2");
                        binding.mainImgLink.setBackgroundResource(R.drawable.img_bt_close);
                        binding.mainImgLink.setEnabled(true);
                        if (!msg.getMessage().isEmpty()) {
                            Toast.makeText(PassiveActivity.this, msg.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception e) {
                    LogUtils.d(tag + "AdminMainActivity" + Objects.requireNonNull(e.getMessage()));
                }
            }
        });

        LiveDataBus.get().with(Constants.BT_RECONNECTED).observe(this, v -> {//关掉mac设备也要重连
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    btDataPro.sendBTMessage(btDataPro.getCONNECT_SEND());
                }
            }, 1000);
        });
    }

    @SuppressLint("DefaultLocale")
    public void SaveRecord() {

        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        String sim = dateFormat.format(date);
        ActivitRecord activitRecord = new ActivitRecord();
        activitRecord.setRecordID(LocalConfig.UserID);
        activitRecord.setUserName(LocalConfig.userName);
        activitRecord.setUserNumber(LocalConfig.medicalNumber);
        activitRecord.setRecordTime(sim);
        activitRecord.setLongTime(timeCount);
        activitRecord.setAduration("0");
        activitRecord.setPduration("0");
        activitRecord.setActivtType(LocalConfig.ModType + "");
        activitRecord.setB_Diastole_Shrink(Passive_B_Diastole_Shrink);
        activitRecord.setL_Diastole_Shrink(Passive_L_Diastole_Shrink);
        //使用String.format()格式化(四舍五入)
        activitRecord.setTotal_mileage(String.format("%.2f", Total_mileage));
        activitRecord.setCalories(String.format("%.2f", Calories));
        activitRecord.setSpasmCount(spasmCount + "");
        activitRecordDao.insert(activitRecord);
        MyAVG myAVG = new MyAVG();
        myAVG.GetAvg(LocalConfig.UserID + "");
        Passive_B_Diastole_Shrink = "0/0";
        Passive_L_Diastole_Shrink = "0/0";
        timeCountTool.setTime(0);
    }

    public void getCalories_mileage() { //计算总里程,卡路里
        double time = timeCountTool.GetSecond();
        double speed = 0;
        double average_speed;
        List<RecordDetailed> recordList = recordDetailedDao.queryBuilder().where(
                        RecordDetailedDao.Properties.Speed.notEq("0"),
                        RecordDetailedDao.Properties.RecordID.eq(LocalConfig.UserID))
                .list();
        if (!recordList.isEmpty()) {
            for (int i = 0; i < recordList.size(); i++) {
                speed = recordList.get(i).getSpeed() + speed;
            }
            //平均转速
            average_speed = speed / recordList.size();
        } else {
            average_speed = 0;
        }

        LogUtils.e(tag + "recordList" + recordList + "平均转速==" + average_speed);

        double perimeter = (float) (3.14 * 0.102 * 2);
        Total_mileage = average_speed * time * perimeter;//总里程

        if (time > 0) {
            Calories = 5 * (time / 60) * average_speed;
        } else {
            Calories = 0;
        }
    }

    public void itinClick() {

        binding.btnTest.setOnClickListener(v -> {
            if (!LocalConfig.isControl) {
                Toast.makeText(this, R.string.bluetoothIsNotConnected, Toast.LENGTH_SHORT).show();
                return;
            }
            AddressBean addressBean = SharedPreferencesUtils.Companion.getInstance().getAddressString();

            if (addressBean != null) {
                btDataPro.sendBTMessage(btDataPro.
                        GetCmdCode(Objects.requireNonNull(addressBean.getEcg()),
                                Objects.requireNonNull(addressBean.getBloodPressure()),
                                Objects.requireNonNull(addressBean.getBloodOxygen())));
            }
        });

        binding.trainBtnReturn.setOnClickListener(v -> {
            if (BloodEndState == 1) {
                //取消测量运动后血压
                BloodEndState = 2;
            } else if (BloodEndState == 0) {
                dialogs();
            }
        });

        binding.passiveTitleActive.setOnClickListener(v -> {
            modleType = 1;
            if (BloodEndState == 1) {
                Toast.makeText(context, "还未测量运动后血压！", Toast.LENGTH_SHORT).show();
                return;
            }
            ChangeDialog();
        });

        binding.passiveTitleIntelligence.setOnClickListener(v -> {
            if (!LocalConfig.isControl) {
                Toast.makeText(this, R.string.bluetoothIsNotConnected, Toast.LENGTH_SHORT).show();
                return;
            }
            modleType = 2;
            if (BloodEndState == 1) {
                Toast.makeText(context, "还未测量运动后血压！", Toast.LENGTH_SHORT).show();
                return;
            }
            ChangeDialog();
        });

        binding.passiveImgBegin.setOnClickListener(v -> {
            if (BaseUtil.isFastDoubleClick()) {
                return;
            }
//            LogUtils.e(tag + "点击了一次开始");
            if (!LocalConfig.isControl) {
                Toast.makeText(this, R.string.bluetoothIsNotConnected, Toast.LENGTH_SHORT).show();
                return;
            }
            if (BloodEndState == 1) {
                Toast.makeText(context, "还未测量运动后血压！", Toast.LENGTH_SHORT).show();
                return;
            }
            HandlerMessage();
        });

        binding.passiveZhuansuJia.setOnClickListener(v -> {
            if (!LocalConfig.isControl) {
                Toast.makeText(this, R.string.bluetoothIsNotConnected, Toast.LENGTH_SHORT).show();
                return;
            }
            if (isBegin) {
                Toast.makeText(context, "运动中，请勿调节参数！", Toast.LENGTH_SHORT).show();
                return;
            }
            if (zhuansu + 1 <= 60) {
                if (zhuansu + 1 >= 30) {
                    if (isOk) {
                        zhuansu = zhuansu + 1;
                        binding.progressViewZhuansuPassive.setGraduatedEnabled(true);
                        binding.passiveTxtZhuansu.setCenterString(zhuansu + "");
                    } else {
                        DialogLoader.getInstance().showConfirmDialog(
                                context,
                                getString(R.string.tip_permission),
                                getString(R.string.lab_ok),
                                (dialog, which) -> {
                                    dialog.dismiss();
                                    isOk = true;
                                    zhuansu = zhuansu + 1;
                                    binding.progressViewZhuansuPassive.setGraduatedEnabled(true);
                                    binding.passiveTxtZhuansu.setCenterString(zhuansu + "");
                                },
                                getString(R.string.lab_cancel),
                                (dialog, which) -> {
                                    dialog.dismiss();
                                    isOk = false;
                                }
                        );
                    }
                } else {
                    zhuansu = zhuansu + 1;
                    binding.progressViewZhuansuPassive.setGraduatedEnabled(true);
                    binding.passiveTxtZhuansu.setCenterString(zhuansu + "");
                }
            } else {
                zhuansu = 60;
            }
        });

        binding.passiveZhuansuJian.setOnClickListener(v -> {
            if (!LocalConfig.isControl) {
                Toast.makeText(this, R.string.bluetoothIsNotConnected, Toast.LENGTH_SHORT).show();
                return;
            }
            if (isBegin) {
                Toast.makeText(context, "运动中，请勿调节参数！", Toast.LENGTH_SHORT).show();
                return;
            }
            zhuansu = zhuansu - 1;
            if (zhuansu < 0) {
                zhuansu = 0;
            } else {
                binding.progressViewZhuansuPassive.setGraduatedEnabled(true);
                binding.passiveTxtZhuansu.setCenterString(zhuansu + "");
            }
        });

        binding.passiveSpasmJia.setOnClickListener(v -> {
            if (!LocalConfig.isControl) {
                Toast.makeText(this, R.string.bluetoothIsNotConnected, Toast.LENGTH_SHORT).show();
                return;
            }
            if (isBegin) {
                Toast.makeText(context, "运动中，请勿调节参数！", Toast.LENGTH_SHORT).show();
                return;
            }
            spasmData = spasmData + 1;
            if (spasmData <= 12) {
                binding.progressViewSpasm.setGraduatedEnabled(true);
                binding.passiveTxtSpasm.setCenterString(spasmData + "");
            } else {
                spasmData = 12;
            }
        });

        binding.passiveSpasmJian.setOnClickListener(v -> {
            if (!LocalConfig.isControl) {
                Toast.makeText(this, R.string.bluetoothIsNotConnected, Toast.LENGTH_SHORT).show();
                return;
            }
            if (isBegin) {
                Toast.makeText(context, "运动中，请勿调节参数！", Toast.LENGTH_SHORT).show();
                return;
            }
            spasmData = spasmData - 1;
            if (spasmData < 1) {
                spasmData = 1;
            } else {
                binding.progressViewSpasm.setGraduatedEnabled(true);
                binding.passiveTxtSpasm.setCenterString(spasmData + "");
            }
        });

        binding.passiveTimeJia.setOnClickListener(v -> {
            if (!LocalConfig.isControl) {
                Toast.makeText(this, R.string.bluetoothIsNotConnected, Toast.LENGTH_SHORT).show();
                return;
            }
            nowTime = nowTime + Constants.passiveTime;
            String text = MyTimeUtils.formatTime(nowTime);
            if (nowTime > Constants.passiveTime) {
                nowTime = Constants.passiveTime;
                text = MyTimeUtils.formatTime(nowTime);
            }
            binding.passiveTxtDowntimer.setCenterString(text);
            activeTime = MyTimeUtils.Getminute(nowTime);
            btDataPro.sendBTMessage(GetCmdCode("50", false,
                    spasmData, zhuansu, activeTime));
        });

        binding.passiveTimeJian.setOnClickListener(v -> {
            if (!LocalConfig.isControl) {
                Toast.makeText(this, R.string.bluetoothIsNotConnected,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            nowTime = nowTime - Constants.passiveTime;
            String text1 = MyTimeUtils.formatTime(nowTime);
            if (nowTime < Constants.passiveTime) {
                nowTime = Constants.passiveTime;
                text1 = MyTimeUtils.formatTime(nowTime);
            }
            binding.passiveTxtDowntimer.setCenterString(text1);
            activeTime = MyTimeUtils.Getminute(nowTime);
            btDataPro.sendBTMessage(GetCmdCode("50", false,
                    spasmData, zhuansu, activeTime));
        });

        binding.passiveImgBlood.setOnClickListener(v -> {
            if (!LocalConfig.isControl) {
                Toast.makeText(this, R.string.bluetoothIsNotConnected, Toast.LENGTH_SHORT).show();
                return;
            }
            if (isBegin) {
                Toast.makeText(context, "运动中，不能测量血压！", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                if (uploadData != null && uploadData.getBlood().equals("已连接")) {
                    isClickBlood = true;//判断是否测量过血压
                    if (contorlState.equals("00") || contorlState.equals("52")) {
                        btDataPro.sendBTMessage(GetCmdCode("51", false, spasmData, zhuansu, activeTime));
                    } else if (contorlState.equals("51")) {
                        btDataPro.sendBTMessage(GetCmdCode("52", false, spasmData, zhuansu, activeTime));
                        contorlState = "52";
                        binding.passiveTxtBlood.setCenterString("点击开始测量血压");
                    }
                } else {
                    Toast.makeText(context, "血压仪未连接，请检查设备", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void HandlerMessage() {
        String txt = binding.passiveTxtBegin.getCenterString();
        if (txt.equals("开  始") && nowTime != 0) {

            String highBlood = binding.passiveTxtHigh.getCenterString();
            String lowBlood = binding.passiveTxtLow.getCenterString();
            if (highBlood.equals("0") && lowBlood.equals("0")) {
                DialogLoader.getInstance().showConfirmDialog(
                        context,
                        getString(R.string.active_blood),
                        getString(R.string.lab_ok),
                        (dialog, which) -> {
                            dialog.dismiss();
                            binding.passiveTimeJia.setEnabled(false);
                            binding.passiveTimeJian.setEnabled(false);

                            binding.passiveTimeJia.setVisibility(View.INVISIBLE);
                            binding.passiveTimeJian.setVisibility(View.INVISIBLE);

                            binding.passiveImgBegin.setBackground(ContextCompat
                                    .getDrawable(this, R.drawable.stop));

                            binding.passiveTxtBegin.setCenterString("停  止");
                            initCountDownTimer(nowTime);
                            timeCountTool.startCount();
                            activeTime = MyTimeUtils.Getminute(nowTime);
                            btDataPro.sendBTMessage(GetCmdCode("50",
                                    true, spasmData, zhuansu, activeTime));
                        },
                        getString(R.string.lab_cancel),
                        (dialog, which) -> dialog.dismiss()
                );
            } else {
                binding.passiveTimeJia.setEnabled(false);
                binding.passiveTimeJian.setEnabled(false);

                binding.passiveTimeJia.setVisibility(View.INVISIBLE);
                binding.passiveTimeJian.setVisibility(View.INVISIBLE);
                binding.passiveImgBegin.setBackground(ContextCompat
                        .getDrawable(this, R.drawable.stop));

                binding.passiveTxtBegin.setCenterString("停  止");
                initCountDownTimer(nowTime);
                timeCountTool.startCount();
                activeTime = MyTimeUtils.Getminute(nowTime);
                btDataPro.sendBTMessage(GetCmdCode("50", true,
                        spasmData, zhuansu, activeTime));
            }


        } else {
            btDataPro.sendBTMessage(GetCmdCode("50", false,
                    5, 1, 0L));
        }
    }

    public void initCountDownTimer(long millisInFuture) {
        downTimer = new PeterTimeCountRefresh(millisInFuture, 1000);
        downTimer.setOnTimerProgressListener(timeLong -> {
            nowTime = timeLong;
            String text = MyTimeUtils.formatTime(timeLong);
            binding.passiveTxtDowntimer.setCenterString(text);
        });

        //时间结束回调
        downTimer.setOnTimerFinishListener(() -> {

            binding.passiveTxtDowntimer.setVisibility(View.GONE);

            binding.passiveTxtTimeDown.setCenterString("00:00:00");//有bug必须加上这个不然会继续倒计时

            binding.passiveTxtTimeDown.setVisibility(View.VISIBLE);

            nowTime = Constants.passiveTime;
            stop();
            binding.passiveTxtBegin.setCenterString("开  始");
            binding.passiveTimeJia.setEnabled(true);
            binding.passiveTimeJian.setEnabled(true);
            binding.passiveTimeJia.setVisibility(View.VISIBLE);
            binding.passiveTimeJian.setVisibility(View.VISIBLE);
        });
        downTimer.start();
    }

    public void ChangeDialog() {

        if (isBegin) {
            Toast.makeText(context, "运动中，请勿切换模式！", Toast.LENGTH_SHORT).show();
            return;
        }
        DialogLoader.getInstance().showConfirmDialog(
                context,
                getString(R.string.active_change),
                getString(R.string.lab_yes),
                (dialog, which) -> {
                    dialog.dismiss();
                    if (modleType == 1) {
                        LocalConfig.ModType = 0;
                        Intent in = new Intent(context, ActiveActivity.class);
                        startActivity(in);
                        finish();
                    } else if (modleType == 2) {
                        LocalConfig.ModType = 2;
                        Intent in = new Intent(context, IntelligenceActivity.class);
                        startActivity(in);
                        finish();
                    }
                },
                getString(R.string.lab_no),
                (dialog, which) -> dialog.dismiss()
        );
    }

    public String GetCmdCode(String blood_measure, boolean isBegin, int spasms_lv, int speed_lv, Long time_lv) {
        String cmd_head = "A88408",              //包头
                sport_mode = "02",                //运动模式
                active_direction = "21",          //运动方向
                //设定时间
                cmd_end = "ED";                   //结尾
        String resistanceHex = "00";//阻力
        String spasmsHex = "0" + BtDataProX.Companion.decToHex(spasms_lv);
        String speedHex;
        if (speed_lv >= 16) {
            speedHex = BtDataProX.Companion.decToHex(speed_lv);
        } else {
            speedHex = "0" + BtDataProX.Companion.decToHex(speed_lv);
        }

        String timeHex;
        if (time_lv >= 16) {
            timeHex = BtDataProX.Companion.decToHex(Math.toIntExact(time_lv));
        } else {
            timeHex = "0" + BtDataProX.Companion.decToHex(Math.toIntExact(time_lv));
        }

        String avtive_status = "10";
        if (isBegin) {
            avtive_status = "11";
        }
        String splicingStr = cmd_head + sport_mode + avtive_status
                + active_direction + resistanceHex + spasmsHex
                + speedHex + timeHex + blood_measure;
        String CRC16 = CRC16Util.getCRC16(splicingStr);
        CMD_CODE = splicingStr + CRC16 + cmd_end;
        LogUtils.d(tag + "GetCmdCode" + "PassiveFragment,获取指令");
        return CMD_CODE;
    }

    public void DataDisplay(String msg, String ObjectJson) {

        if (msg.isEmpty()) {
            return;
        }

        int mark = 0;
        if (msg.equals(btDataPro.getUPLODE_ANSWER())) {
            mark = 1;
        } else if (msg.equals(btDataPro.getECGDATA_ANSWER())) {
            mark = 2;
        } else if (msg.equals(btDataPro.getCONTORL_ANSWER())) {
            mark = 3;
        }

        LogUtils.e(tag + "mark" + mark + ObjectJson);

        switch (mark) {
            case 1:
                uploadData = gson.fromJson(ObjectJson, UploadData.class);

                Observer<String> observerHigh = s -> {//运动完需要重新测量血压，血压那边一直传值，不同的话再跳转到结束页面

                    BloodEndState = 2;

                    LogUtils.e(tag + "第二次结束测量血压值结束！");
                };

                if (UriConfig.test) {
                    uploadData.setBlood("已连接");
                    if (isCloseDialog) {
                        uploadData.setHigh("150");
                        uploadData.setLow("80");
                    } else {
//                        uploadData.setHigh("120");
//                        uploadData.setLow("60");
                    }
                }

                if (isBegin) {
                    spasmData = Integer.parseInt(uploadData.getSTspasm());
                    binding.progressViewSpasm.setGraduatedEnabled(true);
                    binding.passiveTxtSpasm.setCenterString(uploadData.getSTspasm());

                    zhuansu = Integer.parseInt(uploadData.getSTspeed());
                    binding.progressViewZhuansuPassive.setGraduatedEnabled(true);
                    binding.passiveTxtZhuansu.setCenterString(uploadData.getSTspeed());
                }

                if (uploadData.getBlood_oxy().equals("已连接")) {
                    if (uploadData.getOxy_vaulestr().equals("手指未插入")
                            || uploadData.getOxy_vaulestr().equals("探头脱落")
                            || uploadData.getOxy_vaulestr().equals("127")) {
                        binding.passiveTxtBoxygen.setCenterString("--");
                        if (uploadData.getOxy_vaulestr().equals("127")) {
                            binding.passiveTxtO2State.setCenterString("检测中..");
                        } else {
                            binding.passiveTxtO2State.setCenterString(uploadData.getOxy_vaulestr());
                        }
                    } else {
                        binding.passiveTxtBoxygen.setCenterString(uploadData.getOxy_vaulestr());
                        binding.passiveTxtO2State.setCenterString("");
                    }
                } else {
                    binding.passiveTxtBoxygen.setCenterString("--");
                    binding.passiveTxtO2State.setCenterString(uploadData.getBlood_oxy());
                }
                if (uploadData.getBlood().equals("已连接")) {
                    if (uploadData.getHigh().equals("255") || uploadData.getLow().equals("255")) {
                        binding.passiveTxtHigh.setCenterString("0");
                        binding.passiveTxtLow.setCenterString("0");
                        binding.passiveTxtBloodstate1.setCenterString("测量错误");
                        binding.passiveTxtBloodstate2.setCenterString("测量错误");
                    } else {

                        if (!uploadData.getHigh().equals("0")) {
                            LocalConfig.BloodHight = uploadData.getHigh();
                            LocalConfig.BloodLow = uploadData.getLow();

                            if (!Objects.equals(motionHeight, uploadData.getHigh())) {
                                motionHeight = uploadData.getHigh();
                                if (isCloseDialog) {//运动测量后的血压，自动修改成测量完成，然后关闭界面
                                    observerHigh.onChanged(motionHeight);
                                    Passive_B_Diastole_Shrink = uploadData.getLow() + "/" + uploadData.getHigh();
                                }
                            }
                        }
                        binding.passiveTxtBloodstate1.setCenterString("");
                        binding.passiveTxtBloodstate2.setCenterString("");

                        binding.passiveTxtHigh.setCenterString(uploadData.getHigh());
                        binding.passiveTxtLow.setCenterString(uploadData.getLow());

                        if(isClickBlood){//是否点击过测量血压
                            Passive_L_Diastole_Shrink = "0" + "/" + "0";
                            Passive_B_Diastole_Shrink = uploadData.getLow() + "/" + uploadData.getHigh();
                            return;
                        }

                        if (Passive_B_Diastole_Shrink.equals("0/0")) {//测量血压值
                            Passive_B_Diastole_Shrink = uploadData.getLow() + "/" + uploadData.getHigh();
                        } else {
                            Passive_L_Diastole_Shrink = uploadData.getLow() + "/" + uploadData.getHigh();
                        }
                    }
                } else {

                    binding.passiveTxtHigh.setCenterString(LocalConfig.BloodHight);
                    binding.passiveTxtLow.setCenterString(LocalConfig.BloodLow);
                    binding.passiveTxtBloodstate1.setCenterString(uploadData.getBlood());
                    binding.passiveTxtBloodstate2.setCenterString(uploadData.getBlood());
                }

                if (uploadData.getECG().equals("已连接")) {
                    binding.passiveTxtEcgstate.setCenterString("");
                } else {
                    binding.passiveTxtCoory.setCenterString("--");
                    binding.passiveTxtEcgstate.setCenterString(uploadData.getECG());
                }

                if (uploadData.getActiveState().equals("停机状态")) {
                    if (isBegin) {
                        isBegin = false;
                        stop();
                        binding.passiveImgBegin.setBackground(ContextCompat
                                .getDrawable(this, R.drawable.begin));
                        binding.passiveTxtBegin.setCenterString("开  始");
                        nowTime = Integer.parseInt(uploadData.getSTtime()) * 60 * 1000L;
                        activeTime = MyTimeUtils.Getminute(nowTime);
                        String text1 = MyTimeUtils.formatTime(nowTime);
                        binding.passiveTxtDowntimer.setCenterString(text1);
                        timeCount = timeCountTool.stopCount();
                        getCalories_mileage();
                        if (uploadData.getSpasmState() != -1) {
                            if (spasmCount == 5) {
                                DialogLoader.getInstance().showConfirmDialog(
                                        context,
                                        getString(R.string.active_out),
                                        getString(R.string.lab_yes),
                                        (dialog, which) -> {
                                            dialog.dismiss();
                                            BloodEndState = 1;
                                            if (uploadData != null && uploadData.getBlood().equals("已连接")) {
                                                if (contorlState.equals("00") || contorlState.equals("52")) {
                                                    btDataPro.sendBTMessage(GetCmdCode("51", false, spasmData, zhuansu, activeTime));
                                                } else if (contorlState.equals("51")) {
                                                    btDataPro.sendBTMessage(GetCmdCode("52", false, spasmData, zhuansu, activeTime));
                                                    contorlState = "52";
                                                    binding.passiveTxtBlood.setCenterString("点击开始测量血压");
                                                }
                                            } else {
                                                Toast.makeText(context, "血压仪未连接，请检查设备", Toast.LENGTH_SHORT).show();
                                            }
                                        },
                                        getString(R.string.lab_no),
                                        (dialog, which) -> dialog.dismiss()
                                );
                            } else {
                                DialogLoader.getInstance().showConfirmDialog(
                                        context,
                                        getString(R.string.active_blood_end),
                                        getString(R.string.lab_yes),
                                        (dialog, which) -> {
                                            dialog.dismiss();
                                            BloodEndState = 1;
                                            isCloseDialog = true;
                                            if (uploadData != null && uploadData.getBlood().equals("已连接")) {
                                                if (contorlState.equals("00") || contorlState.equals("52")) {
                                                    btDataPro.sendBTMessage(GetCmdCode("51", false, spasmData, zhuansu, activeTime));
                                                } else if (contorlState.equals("51")) {
                                                    btDataPro.sendBTMessage(GetCmdCode("52", false, spasmData, zhuansu, activeTime));
                                                    contorlState = "52";
                                                    binding.passiveTxtBlood.setCenterString("点击开始测量血压");
                                                }
                                            } else {
                                                Toast.makeText(context, "血压仪未连接，请检查设备", Toast.LENGTH_SHORT).show();
                                            }
                                        },
                                        getString(R.string.lab_no),
                                        (dialog, which) -> {
                                            dialog.dismiss();
                                            BloodEndState = 2;
                                        }
                                );
                            }
                        }


                    }
                } else if (uploadData.getActiveState().equals("运行状态")) {
                    isBegin = true;
                    binding.passiveTimeJia.setEnabled(false);
                    binding.passiveTimeJian.setEnabled(false);

                    binding.passiveTimeJia.setVisibility(View.INVISIBLE);
                    binding.passiveTimeJian.setVisibility(View.INVISIBLE);
                    binding.passiveImgBegin.setBackground(ContextCompat
                            .getDrawable(this, R.drawable.stop));
                    binding.passiveTxtBegin.setCenterString("停  止");
                    if (downTimer == null) {
                        initCountDownTimer(nowTime);
                        timeCountTool.startCount();
                        activeTime = MyTimeUtils.Getminute(nowTime);
                    }
                }

                if (uploadData.getSpasmState() != -1) {

                    if (uploadData.getSpasmState() > 0) {
                        spasmCount = uploadData.getSpasmState();
                    }
                }

                if (BloodEndState == 2) {
                    BloodEndState = 0;
                    SaveRecord();
                    btDataPro.sendBTMessage(btDataPro.getCONNECT_CLOSE());

                    Intent in = new Intent(context, DataResultsActivity.class);
                    startActivity(in);
                    finish();
                }
                break;

            case 2:
                try {
                    EcgListData = new ArrayList<>();
                    ecgData = gson.fromJson(ObjectJson, EcgData.class);
                    binding.passiveTxtCoory.setCenterString(ecgData.getHeartrate());
                    EcgListData = ecgData.getEcgCoorY();
                    if (EcgListData == null) {
                        binding.passiveTxtEcgstate.setCenterString("心电仪佩戴异常！");
                        return;
                    } else {
                        OftenListData.addAll(EcgListData);
                    }
                } catch (Exception e) {
                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                    LogUtils.d(tag + "ArrayList" + Objects.requireNonNull(e.getMessage()));
                }
                break;

            case 3:
                contorlState = ObjectJson;
                break;
        }
    }

    /**
     * 模拟源源不断的数据源
     */
    public void updateProgress() {
        try {

            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
            String sim = dateFormat.format(date);

            RecordDetailed recordDetailed = new RecordDetailed();
            recordDetailed.setRecordID(LocalConfig.UserID);
            recordDetailed.setActivtType("被动模式");
            recordDetailed.setRecordTime(sim);
            recordDetailed.setSpeed(zhuansu);
            recordDetailed.setLeftLimb(0);
            recordDetailed.setRightLimb(0);
            recordDetailed.setResistance(0);

            int heartRate;
            if (binding.passiveTxtCoory.getCenterString().equals("--")) {
                heartRate = 0;
            } else {
                heartRate = Integer.parseInt(binding.passiveTxtCoory.getCenterString());
            }
            recordDetailed.setHeartRate(heartRate);

            int Hbo2;
            if (binding.passiveTxtBoxygen.getCenterString().equals("--")) {
                Hbo2 = 0;
            } else {
                Hbo2 = Integer.parseInt(binding.passiveTxtBoxygen.getCenterString());
            }
            recordDetailed.setHbo2(Hbo2);

            recordDetailed.setSpasm(spasmData);
            recordDetailedDao.insert(recordDetailed);
            //500表示调用schedule方法后等待500ms后调用run方法，50表示以后调用run方法的时间间隔
        } catch (Exception e) {
            Toast.makeText(context, "数据库异常" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            dialogs();
            return false;
        }
        return true;
    }

    public void dialogs() {
        //  timeTask.interrupt();
        DialogLoader.getInstance().showConfirmDialog(
                context,
                getString(R.string.active_return),
                getString(R.string.lab_yes),
                (dialog, which) -> {
                    dialog.dismiss();
                    if (isBegin) {
                        btDataPro.sendBTMessage(GetCmdCode("50", false, 5, 1, 0L));
                    }
                    if (isNotBt) {
                        Intent in = new Intent(context, AdminMainActivity.class);
                        startActivity(in);
                        finish();
                    }
                },
                getString(R.string.lab_no),
                (dialog, which) -> dialog.dismiss()
        );
    }

    public void stop() {
        if (downTimer != null && nowTime != 0) {
            downTimer.cancel();
            downTimer = null;
        }
        if (timer1 != null) {
            timer1.cancel();
            timer1.purge();
            timer1 = null;
        }
        if (timerTask1 != null) {
            timerTask1.cancel();
            timerTask1 = null;
        }
        OftenListData = new ArrayList<>();
        binding.passiveWaveviewOne.resetCanavas();
        binding.passiveTxtBegin.setCenterString("开  始");
        binding.passiveTimeJia.setEnabled(true);
        binding.passiveTimeJian.setEnabled(true);
        binding.passiveTimeJia.setVisibility(View.VISIBLE);
        binding.passiveTimeJian.setVisibility(View.VISIBLE);
        nowTime = Constants.passiveTime;
        binding.passiveTxtDowntimer.setCenterString(MyTimeUtils.formatTime(nowTime));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
        LocalConfig.BloodHight = "0";
        LocalConfig.BloodLow = "0";
        TimeCountTool.setClean();
    }
}
