package com.rick.recoveryapp.ui.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Toast;


import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;

import com.common.network.LogUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rick.recoveryapp.R;
import com.rick.recoveryapp.ui.BaseApplication;
import com.rick.recoveryapp.base.XPageActivity;
import com.rick.recoveryapp.ui.activity.helper.UriConfig;
import com.rick.recoveryapp.ui.service.BluetoothChatServiceX;
import com.rick.recoveryapp.ui.activity.helper.BtDataProX;
import com.rick.recoveryapp.chart.MyAVG;
import com.rick.recoveryapp.databinding.ActivityActiviteBinding;
import com.rick.recoveryapp.ui.activity.helper.Constants;
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

@Deprecated
public class ActiveActivity extends XPageActivity {

    private String tag = ActiveActivity.class.getName();
    private boolean isCloseDialog = false;//如果是运动后停止

    String motionHeight;//运动前的高压
    int resiDta = 1, modletype = 0;
    ArrayList<Float> EcgListData;
    static ArrayList<Float> OftenListData;
    RecordDetailedDao recordDetailedDao;
    EcgDataDBDao ecgDataDBDao;
    UploadData uploadData;
    EcgData ecgData;
    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    String ContorlState = "52";
    BtDataProX btDataPro;
    String CMD_CODE = "";
    private Timer timer1;
    private TimerTask timerTask1;
    boolean isBegin = false;
    Context context;
    ActivityActiviteBinding binding;
    ActivitRecordDao activitRecordDao;
    public TimeCountTool timeCountTool = TimeCountTool.getInstance();
    String timecount = "";
    double Total_mileage, Calories;
    String B_Diastole_Shrink = "0/0", L_Diastole_Shrink = "0/0";
    int BloodEndState = 0; // 0:初始状态  1：需要测量血压   2：血压测量完成

    boolean isClickBlood = false;//是否运动前点击了测量血压

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityActiviteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        StatusBarUtils.translucent(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        context = this;

        btDataPro = new BtDataProX();
        itinClick();
        binding.activeTxtMassage.setLeftString(" 患者姓名：" + LocalConfig.userName);
        binding.activeTxtMassage.setLeftBottomString(" 患者编号：" + LocalConfig.medicalNumber);
        BaseApplication myApp = (BaseApplication) getApplication();
        LocalConfig.daoSession = myApp.getDaoSession();
        activitRecordDao = LocalConfig.daoSession.getActivitRecordDao();
        recordDetailedDao = LocalConfig.daoSession.getRecordDetailedDao();

        EcgListData = new ArrayList<>();
        OftenListData = new ArrayList<>();
        binding.activeWaveviewOne.resetCanavas();
        ecgDataDBDao = LocalConfig.daoSession.getEcgDataDBDao();

        try {
            initLiveData();
            btDataPro.sendBTMessage(GetCmdCode(1, "53", false));
            binding.activeTxtResistance.setCenterString("1");
            // btDataPro.sendBTMessage(btDataPro.GetCmdCode(LocalConfig.ecgmac, LocalConfig.bloodmac, LocalConfig.oxygenmac));
            PassEcg();

        } catch (Exception ex) {
            ex.getMessage();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (BaseApplication.mConnectService != null) {
            //蓝牙闲置状态
            if (BaseApplication.mConnectService.getState() == BluetoothChatServiceX.STATE_NONE) {
                if (BaseApplication.liveMessage != null) {
                    binding.mainImgLink.setBackgroundResource(R.drawable.img_bt_close);
                    binding.mainImgLink.setEnabled(true);
                    //监听其他蓝牙主设备
                    BaseApplication.mConnectService.start();
                }
                //蓝牙已连接
            } else if (BaseApplication.mConnectService.getState() == BluetoothChatServiceX.STATE_CONNECTED) {
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
                        binding.mainImgLink.setBackgroundResource(R.drawable.img_bt_open);
                        binding.mainImgLink.setEnabled(false);
                        Toast.makeText(ActiveActivity.this, msg.getMessage(), Toast.LENGTH_SHORT).show();
                        btDataPro.sendBTMessage(btDataPro.getCONNECT_SEND());
                        AddressBean addressBean = SharedPreferencesUtils.Companion.getInstance().getAddressString();
                        if (addressBean != null) {
                            btDataPro.sendBTMessage(btDataPro.
                                    GetCmdCode(addressBean.getEcg(),
                                            addressBean.getBloodPressure(),
                                            addressBean.getBloodOxygen()));
                        }
                    } else {
                        binding.mainImgLink.setBackgroundResource(R.drawable.img_bt_close);
                        binding.mainImgLink.setEnabled(true);
                        if (!msg.getMessage().isEmpty()) {
                            Toast.makeText(ActiveActivity.this, msg.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception e) {
                    LogUtils.e(tag + "AdminMainActivity" + e.getMessage());
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

    public void PassEcg() {
        timer1 = new Timer();
        timerTask1 = new TimerTask() {
            @Override
            public void run() {
                try {
                    if (OftenListData != null) {
                        if (!OftenListData.isEmpty()) {
                            float cooY = OftenListData.get(0);
                            binding.activeWaveviewOne.showLine(cooY);
                            OftenListData.remove(0);
                        } else {
                            //     binding.activeWaveviewOne.showLine(0f);
                            return;
                        }
                    } else {
                        return;
                    }
                } catch (Exception e) {
                    com.efs.sdk.base.core.util.Log.d("EcgError", e.getMessage());
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
                    LogUtils.d(tag + "BT" + msg.getObjectName());
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
                        DataDisplay(msg.getObjectName(), msg.getObjectJson());
                        if (isBegin) {
                            UpdatProgress();
                        }
                    }
                } else {
                    LogUtils.e(tag + "BT" + "没有任何数据");
                }
            }
        });

        LiveDataBus.get().with(Constants.BT_ECG).observe(this, v -> {
            if (v instanceof PoolMessage) {
                PoolMessage msg = (PoolMessage) v;
                DataDisplay(msg.getObjectName(), msg.getObjectJson());
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

                    binding.activeTxtBoxygen.setCenterString("0");
                    binding.activeTxtO2State.setCenterString("血氧仪未连接");
                    binding.activeTxtBloodstate1.setCenterString("血压仪未连接");
                    binding.activeTxtBloodstate2.setCenterString("血压仪未连接");

                    binding.activeTxtCoory.setCenterString("--");
                    binding.activeTxtEcgstate.setCenterString("心电仪未连接");
                    OftenListData.clear();

                    int left = 0;
                    binding.progressViewLeft.setGraduatedEnabled(true);
                    binding.progressViewLeft.setEndProgress(Float.parseFloat(LocalConfig.GetProgress((float) left, (float) 50)));
                    binding.activeTxtLeft.setCenterString("0");

                    int right = 0;
                    binding.progressViewRight.setGraduatedEnabled(true);
                    binding.progressViewRight.setEndProgress(Float.parseFloat(LocalConfig.GetProgress((float) right, (float) 50)));
                    binding.activeTxtRight.setCenterString("0");
                }
            }
        });

        controlBT();

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
        activitRecord.setLongTime(timecount);
        activitRecord.setAduration("0");
        activitRecord.setPduration("0");
        activitRecord.setActivtType(LocalConfig.ModType + "");
        activitRecord.setB_Diastole_Shrink(B_Diastole_Shrink);
        activitRecord.setL_Diastole_Shrink(L_Diastole_Shrink);
        //使用String.format()格式化(四舍五入)
        activitRecord.setTotal_mileage(String.format("%.2f", Total_mileage));
        activitRecord.setCalories(String.format("%.2f", Calories));
        activitRecord.setSpasmCount(0 + "");
        activitRecordDao.insert(activitRecord);

        MyAVG myAVG = new MyAVG();
        myAVG.GetAvg(LocalConfig.UserID + "");

        B_Diastole_Shrink = "0/0";
        L_Diastole_Shrink = "0/0";
        timeCountTool.setTime(0);
    }

    //计算总里程,卡路里
    public void getCalories_mileage() {
        double time = timeCountTool.GetSecond();
        double speed = 0;
        double average_speed;
        double resistance = 0;
        double resistanceVal = 0;
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

        double perimeter = (float) (3.14 * 0.102 * 2);
        Total_mileage = average_speed * time * perimeter;//总里程
        List<RecordDetailed> DetailedList = recordDetailedDao.queryBuilder().where(
                        RecordDetailedDao.Properties.RecordID.eq(LocalConfig.UserID),
                        RecordDetailedDao.Properties.Resistance.notEq(0))
                .list();

        if (!DetailedList.isEmpty()) {
            for (int i = 0; i < DetailedList.size(); i++) {
                resistance = resistance + DetailedList.get(i).getResistance();
            }
            //平均阻力
            int Avg = (int) Math.ceil(resistance / DetailedList.size());
            resistanceVal = LocalConfig.Getvalue(Avg);
        }

        if (time > 0) {
            Calories = resistanceVal * (time / 60) * average_speed;
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
                dialogs(true);
            }
        });

        binding.activeTitlePress.setOnClickListener(v -> {
            if (!LocalConfig.isControl) {
                Toast.makeText(this, R.string.bluetoothIsNotConnected, Toast.LENGTH_SHORT).show();
                return;
            }
            modletype = 1;
            ChangeDialog();
        });

        binding.activeTitleIntelligence.setOnClickListener(v -> {
            if (!LocalConfig.isControl) {
                Toast.makeText(this, R.string.bluetoothIsNotConnected, Toast.LENGTH_SHORT).show();
                return;
            }
            modletype = 2;
            ChangeDialog();
        });

        binding.activeImgBegin.setOnClickListener(v -> {
            if (BaseUtil.isFastDoubleClick()) {
                return;
            }
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

        binding.activeImbtnJia.setOnClickListener(v -> {
            if (!LocalConfig.isControl) {
                Toast.makeText(this, R.string.bluetoothIsNotConnected, Toast.LENGTH_SHORT).show();
                return;
            }
            if (isBegin) {
                Toast.makeText(context, "运动中，请勿调节参数！", Toast.LENGTH_SHORT).show();
                return;
            }
            if (BloodEndState == 1) {
                Toast.makeText(context, "还未测量运动后血压！", Toast.LENGTH_SHORT).show();
                return;
            }
            resiDta = resiDta + 1;
            if (resiDta <= 12) {
                binding.progressViewResistance.setGraduatedEnabled(true);
                binding.activeTxtResistance.setCenterString(resiDta + "");
            } else {
                resiDta = 12;
            }
        });

        binding.activeImbtnMove.setOnClickListener(v -> {
            if (!LocalConfig.isControl) {
                Toast.makeText(this, R.string.bluetoothIsNotConnected, Toast.LENGTH_SHORT).show();
                return;
            }
            if (BloodEndState == 1) {
                Toast.makeText(context, "还未测量运动后血压！", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isBegin) {
                Toast.makeText(context, "运动中，请勿调节参数！", Toast.LENGTH_SHORT).show();
                return;
            }
            resiDta = resiDta - 1;
            if (resiDta < 1) {
                resiDta = 1;
            } else {
                binding.progressViewResistance.setGraduatedEnabled(true);
                binding.activeTxtResistance.setCenterString(resiDta + "");
            }
        });

        binding.activeImgBlood.setOnClickListener(v -> {
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
                    if (ContorlState.equals("00") || ContorlState.equals("52")) {
                        btDataPro.sendBTMessage(GetCmdCode(resiDta, "51", false));
                    } else if (ContorlState.equals("51")) {
                        btDataPro.sendBTMessage(GetCmdCode(resiDta, "52", false));
                        binding.activeTxtBlood.setCenterString("点击开始测量血压");
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
        try {

            String srs = binding.activeTxtBegin.getCenterString();
            if (srs.equals("开  始")) {

                String highBlood = binding.activeTxtHigh.getCenterString();
                String lowBlood = binding.activeTxtLow.getCenterString();
                if (highBlood.equals("0") && lowBlood.equals("0")) {
                    DialogLoader.getInstance().showConfirmDialog(
                            context,
                            getString(R.string.active_blood),
                            getString(R.string.lab_ok),
                            (dialog, which) -> {
                                dialog.dismiss();
                                btDataPro.sendBTMessage(GetCmdCode(resiDta, "50", true));
                                timeCountTool.startCount();
                                binding.activeTxtBegin.setCenterString("停  止");

                                binding.activeImgBegin.setBackground(ContextCompat
                                        .getDrawable(this, R.drawable.stop));
                            },
                            getString(R.string.lab_cancel),
                            (dialog, which) -> {
                                dialog.dismiss();
                                // timeTask.start();
                            }
                    );

                } else {
                    btDataPro.sendBTMessage(GetCmdCode(resiDta, "50", true));
                    timeCountTool.startCount();
                    binding.activeTxtBegin.setCenterString("停  止");
                    binding.activeImgBegin.setBackground(ContextCompat
                            .getDrawable(this, R.drawable.stop));
                }
            } else {
                btDataPro.sendBTMessage(GetCmdCode(0, "50", false));
            }
        } catch (Exception e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void ChangeDialog() {
        if (isBegin) {
            Toast.makeText(context, "运动中，请勿切换模式！", Toast.LENGTH_SHORT).show();
            return;
        }
        if (BloodEndState == 1) {
            Toast.makeText(context, "还未测量运动后血压！", Toast.LENGTH_SHORT).show();
            return;
        }

        DialogLoader.getInstance().showConfirmDialog(
                context,
                getString(R.string.active_change),
                getString(R.string.lab_yes),
                (dialog, which) -> {
                    dialog.dismiss();
                    if (modletype == 1) {
                        LocalConfig.ModType = 1;
                        Intent in = new Intent(context, PassiveActivity.class);
                        startActivity(in);
                        finish();
                    } else if (modletype == 2) {
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

    public String GetCmdCode(int zuli, String blood_measure, boolean isBegin) {
        String cmd_head = "A88408",              //包头
                sport_mode = "01",                //运动模式
                active_direction = "20",          //运动方向
                spasms_lv = "00",                 //痉挛等级
                speed_lv = "00",                  //速度设定
                time_lv = "00",                   //设定时间
                cmd_end = "ED";                   //结尾
        String zuliHex = "0" + BtDataProX.Companion.decToHex(zuli);
        String avtive_status = "10";
        if (isBegin) {
            avtive_status = "11";
        }
        String splicingStr = cmd_head + sport_mode + avtive_status + active_direction + zuliHex + spasms_lv
                + speed_lv + time_lv + blood_measure;
        String CRC16 = CRC16Util.getCRC16(splicingStr);
        CMD_CODE = splicingStr + CRC16 + cmd_end;
        com.efs.sdk.base.core.util.Log.d("GetCmdCode", "ActiveFragemt,获取指令");
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

                    LogUtils.e(tag + "结束测量血压值成功！");
                };

                if (UriConfig.test) {
                    uploadData.setBlood("已连接");
                    if (isCloseDialog) {
                        uploadData.setHigh("150");
                        uploadData.setLow("80");
                    } else {
                        if (isClickBlood) {
                            uploadData.setHigh("120");
                            uploadData.setLow("60");
                        }
                    }
                }

                if (uploadData.getBlood_oxy().equals("已连接")) {
                    if (uploadData.getOxy_vaulestr().equals("手指未插入")
                            || uploadData.getOxy_vaulestr().equals("探头脱落")
                            || uploadData.getOxy_vaulestr().equals("127")) {
                        binding.activeTxtBoxygen.setCenterString("--");
                        if (uploadData.getOxy_vaulestr().equals("127")) {
                            binding.activeTxtO2State.setCenterString("检测中..");
                        } else {
                            binding.activeTxtO2State.setCenterString(uploadData.getOxy_vaulestr());
                        }
                    } else {
                        binding.activeTxtBoxygen.setCenterString(uploadData.getOxy_vaulestr());
                        binding.activeTxtO2State.setCenterString("");
                    }
                } else {
                    binding.activeTxtBoxygen.setCenterString("--");
                    binding.activeTxtO2State.setCenterString(uploadData.getBlood_oxy());
                }
                if (uploadData.getBlood().equals("已连接")) {
                    if (uploadData.getHigh().equals("255") || uploadData.getLow().equals("255")) {
                        binding.activeTxtHigh.setCenterString("0");
                        binding.activeTxtLow.setCenterString("0");
                        binding.activeTxtBloodstate1.setCenterString("测量错误");
                        binding.activeTxtBloodstate2.setCenterString("测量错误");
                    } else {
                        if (!uploadData.getHigh().equals("0")) {
                            LocalConfig.BloodHight = uploadData.getHigh();
                            LocalConfig.BloodLow = uploadData.getLow();

                            if (isClickBlood) {//是否点击过测量血压
//                                if (B_Diastole_Shrink.equals("0/0")) {
//                                    B_Diastole_Shrink = uploadData.getLow() + "/" + uploadData.getHigh();
//                                }
//                                if (isCloseDialog) {
//                                    if (!Objects.equals(motionHeight, uploadData.getHigh())) {//运动完测量血压
//                                        motionHeight = uploadData.getHigh();
//                                        observerHigh.onChanged(motionHeight);
//                                        L_Diastole_Shrink = uploadData.getLow() + "/" + uploadData.getHigh();
//                                    }
//                                }
//                                if (Objects.equals(B_Diastole_Shrink,
//                                        L_Diastole_Shrink)) {//训练前训练后不可能血压相同，一样的话就把训练前的改成0/0
//                                    B_Diastole_Shrink = "0" + "/" + "0";
//                                    LogUtils.e(tag + "B_Diastole_Shrink==" + B_Diastole_Shrink);
//                                }

                                if (BloodEndState == 1) {
                                    //运动后血压
                                    L_Diastole_Shrink = uploadData.getLow() + "/" + uploadData.getHigh();
                                    if (!B_Diastole_Shrink.equals(L_Diastole_Shrink)) {
                                        BloodEndState = 2;
                                        Toast.makeText(context, "运动后血压测量已完成！", Toast.LENGTH_SHORT).show();
                                    }

                                } else if (BloodEndState == 0) {
                                    //运动前血压
                                    B_Diastole_Shrink = uploadData.getLow() + "/" + uploadData.getHigh();
                                }
                            } else {
                                if (isCloseDialog) {//是否点击了运动后测量血压
                                    B_Diastole_Shrink = "0" + "/" + "0";//训练前血压
                                    if (!Objects.equals(motionHeight, uploadData.getHigh())) {//运动完测量血压
                                        motionHeight = uploadData.getHigh();
                                        observerHigh.onChanged(motionHeight);
                                        L_Diastole_Shrink = uploadData.getLow() + "/" + uploadData.getHigh();
                                    }
                                } else {//点的否
                                    B_Diastole_Shrink = uploadData.getLow() + "/" + uploadData.getHigh();//训练前血压
                                    L_Diastole_Shrink = "0" + "/" + "0";//训练后血压
                                }
                            }
                        }

                        binding.activeTxtHigh.setCenterString(uploadData.getHigh());
                        binding.activeTxtLow.setCenterString(uploadData.getLow());
                        binding.activeTxtBloodstate1.setCenterString("");
                        binding.activeTxtBloodstate2.setCenterString("");

                    }
                } else {
                    binding.activeTxtHigh.setCenterString(LocalConfig.BloodHight);
                    binding.activeTxtLow.setCenterString(LocalConfig.BloodLow);
                    binding.activeTxtBloodstate1.setCenterString(uploadData.getBlood());
                    binding.activeTxtBloodstate2.setCenterString(uploadData.getBlood());
                    B_Diastole_Shrink = "0/0";
                    L_Diastole_Shrink = "0/0";
                }

                if (uploadData.getECG().equals("已连接")) {
                    binding.activeTxtEcgstate.setCenterString("");
                } else {
                    binding.activeTxtCoory.setCenterString("--");
                    binding.activeTxtEcgstate.setCenterString(uploadData.getECG());
                }
                if (isBegin) {
                    binding.progressViewZhuansuActicve.setGraduatedEnabled(true);
                    binding.activeTxtZhuansu.setCenterString(uploadData.getSpeed());

                    binding.progressViewLeft.setGraduatedEnabled(true);
                    binding.activeTxtLeft.setCenterString(uploadData.getLeft());

                    binding.progressViewRight.setGraduatedEnabled(true);
                    binding.activeTxtRight.setCenterString(uploadData.getRight());

                    binding.progressViewResistance.setGraduatedEnabled(true);
                    binding.activeTxtResistance.setCenterString(uploadData.getSTresistance());
                }
                if (uploadData.getActiveState().equals("停机状态")) {
                    binding.activeTxtBegin.setCenterString("开  始");
                    binding.activeImgBegin.setBackground(getResources().getDrawable(R.drawable.begin));
                    //判断是否在运动中
                    if (isBegin) {
                        isBegin = false;
                        stop();
                        timecount = timeCountTool.stopCount();
                        getCalories_mileage();

                        //下位机停止，提示是否测量血压
                        //是：进行血压测量，判断血压仪连接状态
                        //否：直接进入结算界面
                        DialogLoader.getInstance().showConfirmDialog(
                                context,
                                getString(R.string.active_blood_end),
                                getString(R.string.lab_yes),
                                (dialog, which) -> {
                                    dialog.dismiss();
                                    BloodEndState = 1;
                                    isCloseDialog = true;
                                    if (uploadData != null && uploadData.getBlood().equals("已连接")) {
                                        if (ContorlState.equals("00") || ContorlState.equals("52")) {
                                            btDataPro.sendBTMessage(GetCmdCode(resiDta, "51", false));
                                        } else if (ContorlState.equals("51")) {
                                            btDataPro.sendBTMessage(GetCmdCode(resiDta, "52", false));
                                            ContorlState = "52";
                                            binding.activeTxtBlood.setCenterString("点击开始测量血压");
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

                } else if (uploadData.getActiveState().equals("运行状态")) {
                    isBegin = true;
                    timeCountTool.startCount();
                    binding.activeTxtBegin.setCenterString("停  止");
                    binding.activeImgBegin.setBackground(getResources().getDrawable(R.drawable.stop));
                }

                if (BloodEndState == 2) {
                    BloodEndState = 0;
                    SaveRecord();
                    btDataPro.sendBTMessage(btDataPro.getCONNECT_CLOSE());

                    Intent in = new Intent(context, DataResultsActivity.class);
                    // in.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(in);
                    finish();
                }
                break;

            case 2:
                try {
                    ecgData = gson.fromJson(ObjectJson, EcgData.class);
                    binding.activeTxtCoory.setCenterString(ecgData.getHeartrate());
                    EcgListData = ecgData.getEcgCoorY();
                    if (EcgListData == null) {
                        binding.activeTxtEcgstate.setCenterString("心电仪佩戴异常！");
                        OftenListData = new ArrayList<>();
                        //   binding.activeWaveviewOne.showLine(0f);
                        return;
                    } else {
                        // Float
                        binding.activeTxtEcgstate.setCenterString("");
                        OftenListData.addAll(EcgListData);
                        EcgListData = null;
                    }
                } catch (Exception e) {
                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                break;

            case 3:
                ContorlState = ObjectJson;
                if (ContorlState.equals("51")) {
                    //   binding.activeTxtBlood.setCenterString("测量中");
                } else if (ContorlState.equals("52")) {
                    //     binding.activeTxtBlood.setCenterString("点击开始测量血压");
                } else {
                    //  binding.activeTxtBlood.setCenterString("点击开始测量血压");
                }
                break;
        }
    }

    /**
     * 模拟源源不断的数据源
     */
    public void UpdatProgress() {
        try {
            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String sim = dateFormat.format(date);

            RecordDetailed recordDetailed = new RecordDetailed();
            recordDetailed.setRecordID(LocalConfig.UserID);
            recordDetailed.setActivtType("主动模式");
            recordDetailed.setRecordTime(sim);
            int zhuansu = Integer.parseInt(binding.activeTxtZhuansu.getCenterString());
            recordDetailed.setSpeed(zhuansu);
            int leftlimb = Integer.parseInt(binding.activeTxtLeft.getCenterString());
            recordDetailed.setLeftLimb(leftlimb);
            int rightlimb = Integer.parseInt(binding.activeTxtRight.getCenterString());
            recordDetailed.setRightLimb(rightlimb);
            int resistance = Integer.parseInt(binding.activeTxtResistance.getCenterString());
            recordDetailed.setResistance(resistance);

            int heartRate;
            if (binding.activeTxtCoory.getCenterString().equals("--")) {
                heartRate = 0;
            } else {
                heartRate = Integer.parseInt(binding.activeTxtCoory.getCenterString());
            }
            recordDetailed.setHeartRate(heartRate);

            int Hbo2;
            if (binding.activeTxtBoxygen.getCenterString().equals("--")) {
                Hbo2 = 0;
            } else {
                Hbo2 = Integer.parseInt(binding.activeTxtBoxygen.getCenterString());
            }
            recordDetailed.setHbo2(Hbo2);

            recordDetailed.setSpasm(0);
            recordDetailedDao.insert(recordDetailed);
            //500表示调用schedule方法后等待500ms后调用run方法，50表示以后调用run方法的时间间隔

        } catch (Exception e) {
            Toast.makeText(context, "数据库异常" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void stop() {
        OftenListData = new ArrayList<>();

        if (timer1 != null) {
            timer1.cancel();
            timer1.purge();
            timer1 = null;
        }
        if (timerTask1 != null) {
            timerTask1.cancel();
            timerTask1 = null;
        }
        binding.activeWaveviewOne.resetCanavas();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            dialogs(false);
            return false;
        }
        return true;
    }

    public void dialogs(boolean isReturn) {
        //  timeTask.interrupt();
        DialogLoader.getInstance().showConfirmDialog(
                context,
                getString(R.string.active_return),
                getString(R.string.lab_yes),
                (dialog, which) -> {
                    dialog.dismiss();
                    if (isBegin) {
                        btDataPro.sendBTMessage(GetCmdCode(0, "50", false));
                    }

                    if (!isBegin) {
                        Intent in = new Intent(context, AdminMainActivity.class);
                        // in.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(in);
                        finish();
                    }
                },
                getString(R.string.lab_no),
                (dialog, which) -> {
                    dialog.dismiss();
                }
        );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
        LocalConfig.BloodHight = "0";
        LocalConfig.BloodLow = "0";
        TimeCountTool.setClean();
//        btDataPro.sendBTMessage(btDataPro.getCONNECT_CLOSE());
    }
}
