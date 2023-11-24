package com.rick.recoveryapp.ui.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Toast;

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
import com.rick.recoveryapp.databinding.ActivityIntelligenceBinding;
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
import com.rick.recoveryapp.utils.ActiveTimeTool;
import com.rick.recoveryapp.utils.BaseUtil;
import com.rick.recoveryapp.utils.CRC16Util;
import com.rick.recoveryapp.utils.LiveDataBus;
import com.rick.recoveryapp.utils.LocalConfig;
import com.rick.recoveryapp.utils.PeterTimeCountRefresh;
import com.rick.recoveryapp.utils.TimeCountTool;
import com.xuexiang.xui.utils.StatusBarUtils;
import com.xuexiang.xui.widget.dialog.DialogLoader;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

@Deprecated
public class IntelligenceActivity extends XPageActivity {
    private boolean isCloseDialog = false;//如果是运动后停止

    boolean isClickBlood = false;//是否运动前点击了测量血压
    String motionHeight;//运动前的高压
    int modletype = 0;
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
    ActivitRecordDao activitRecordDao;
    public TimeCountTool timeCountTool = TimeCountTool.getInstance();
    public ActiveTimeTool activeTimeTool = ActiveTimeTool.getInstance();
    String timecount = "";
    double Total_mileage, Calories;
    boolean isOk = false;
    static PeterTimeCountRefresh downTimer;
    int zhuansuData = 5, resistance = 1, spasm = 1;
    ActivityIntelligenceBinding binding;
    String B_Diastole_Shrink = "0/0", L_Diastole_Shrink = "0/0";
    int spasmCount = 0;
    int BloodEndState = 0; // 0:初始状态  1：需要测量血压   2：血压测量完成


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityIntelligenceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        StatusBarUtils.translucent(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        context = this;

        btDataPro = new BtDataProX();
        itinClick();
        binding.activeTxtMassage.setLeftString(" 患者姓名：" + LocalConfig.userName);
        binding.activeTxtMassage.setLeftBottomString(" 患者编号：" + LocalConfig.medicalNumber + "");
        BaseApplication myApp = (BaseApplication) getApplication();
        LocalConfig.daoSession = myApp.getDaoSession();
        activitRecordDao = LocalConfig.daoSession.getActivitRecordDao();
        recordDetailedDao = LocalConfig.daoSession.getRecordDetailedDao();
        EcgListData = new ArrayList<>();
        OftenListData = new ArrayList<>();
        binding.IntelligenceWaveviewOne.resetCanavas();

        ecgDataDBDao = LocalConfig.daoSession.getEcgDataDBDao();

        try {
            initLiveData();
            btDataPro.sendBTMessage(GetCmdCode(resistance, "53", false, zhuansuData, spasm));
            binding.intelligenceTxtZhuansu.setCenterString(zhuansuData + "");
            binding.intelligenceTxtResistance.setCenterString(resistance + "");
            binding.intelligenceTxtSpasm.setCenterString(spasm + "");
            PassEcg();
        } catch (Exception ex) {
            ex.getMessage();
        }

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

    public void PassEcg() {
        timer1 = new Timer();
        timerTask1 = new TimerTask() {
            @Override
            public void run() {
                try {
                    if (OftenListData != null) {
                        if (!OftenListData.isEmpty()) {
                            float cooY = OftenListData.get(0);
                            binding.IntelligenceWaveviewOne.showLine(cooY);
                            OftenListData.remove(0);
                        } else {
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
                    Log.d("BT", msg.getObjectName());
                    if (msg.getObjectName().equals(btDataPro.getUPLODE_ANSWER())) {
                        UploadData uploadData = new UploadData();
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
                    }
                } else {
                    Log.d("BT", "没有任何数据");
                }
            }
        });

        LiveDataBus.get().with(Constants.BT_ECG).observe(this, v -> {
            if (v instanceof PoolMessage) {
                PoolMessage msg = (PoolMessage) v;
                DataDisplay(msg.getObjectName(), msg.getObjectJson());
                if (isBegin) {
                    UpdatProgress();
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

                    binding.inteTxtBoxygen.setCenterString("0");
                    binding.inteTxtO2State.setCenterString("血氧仪未连接");
//                            binding.inteTxtHigh.setCenterString(LocalConfig.B_Diastole_Shrink);
//                            binding.inteTxtLow.setCenterString(LocalConfig.L_Diastole_Shrink);
                    binding.inteTxtBloodstate1.setCenterString("血压仪未连接");
                    binding.inteTxtBloodstate1.setCenterString("血压仪未连接");

//                            LocalConfig.B_Diastole_Shrink = "0/0";
//                            LocalConfig.L_Diastole_Shrink = "0/0";

                    int left = 0;
                    binding.progressViewLeft.setGraduatedEnabled(true);
                    //   binding.progressViewLeft.setEndProgress(Float.parseFloat(LocalConfig.GetProgress((float) left, (float) 50)));
                    //  binding.progressViewLeft.startProgressAnimation();
                    binding.intelligenceTxtLeft.setCenterString("0");

                    int right = 0;
                    binding.progressViewRight.setGraduatedEnabled(true);
                    //binding.progressViewRight.setEndProgress(Float.parseFloat(LocalConfig.GetProgress((float) right, (float) 50)));
                    //  binding.progressViewRight.startProgressAnimation();
                    binding.intelligenceTxtLeft.setCenterString("0");

                    binding.inteTxtCoory.setCenterString("0");
                    binding.inteTxtEcgstate.setCenterString("心电仪未连接");
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
                        Log.d("BT_CONNECTED1", LocalConfig.isControl + " 1");
                        binding.mainImgLink.setBackgroundResource(R.drawable.img_bt_open);
                        binding.mainImgLink.setEnabled(false);
                        Toast.makeText(IntelligenceActivity.this, msg.getMessage(), Toast.LENGTH_SHORT).show();
                        btDataPro.sendBTMessage(btDataPro.getCONNECT_SEND());
                        AddressBean addressBean = SharedPreferencesUtils.Companion.getInstance().getAddressString();
                        if (addressBean != null) {
                            btDataPro.sendBTMessage(btDataPro.
                                    GetCmdCode(Objects.requireNonNull(addressBean.getEcg()),
                                            Objects.requireNonNull(addressBean.getBloodPressure()),
                                            Objects.requireNonNull(addressBean.getBloodOxygen())));
                        }
                    } else {
                        Log.d("BT_CONNECTED1", LocalConfig.isControl + " 2");
                        binding.mainImgLink.setBackgroundResource(R.drawable.img_bt_close);
                        binding.mainImgLink.setEnabled(true);
                        if (!msg.getMessage().isEmpty()) {
                            Toast.makeText(IntelligenceActivity.this, msg.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception e) {
                    Log.d("AdminMainActivity", e.getMessage());
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
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String sim = dateFormat.format(date);
        activeTimeTool.stopCount();

        int SurplusTime = timeCountTool.getTime() - activeTimeTool.getTime();

        String Pduration = activeTimeTool.SurplusTi(SurplusTime);

        String Aduration = activeTimeTool.SurplusTi(activeTimeTool.getTime());

        ActivitRecord activitRecord = new ActivitRecord();
        activitRecord.setRecordID(LocalConfig.UserID);
        activitRecord.setUserName(LocalConfig.userName);
        activitRecord.setUserNumber(LocalConfig.medicalNumber);
        activitRecord.setRecordTime(sim);
        activitRecord.setLongTime(timecount);
        activitRecord.setAduration(Aduration);
        activitRecord.setPduration(Pduration);
        activitRecord.setActivtType(LocalConfig.ModType + "");
        activitRecord.setB_Diastole_Shrink(B_Diastole_Shrink);
        activitRecord.setL_Diastole_Shrink(L_Diastole_Shrink);
        //使用String.format()格式化(四舍五入)
        activitRecord.setTotal_mileage(String.format("%.2f", Total_mileage));
        activitRecord.setCalories(String.format("%.2f", Calories));
        activitRecord.setSpasmCount(spasmCount + "");
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
        /**
         * 查询转速不为“0” 的所有记录集合。集合不为大于0时取平均值
         * */
        List<RecordDetailed> recordList = recordDetailedDao.queryBuilder().where(
                        RecordDetailedDao.Properties.Speed.notEq("0"),
                        RecordDetailedDao.Properties.RecordID.eq(LocalConfig.UserID))
                .list();
        if (recordList.size() > 0) {
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
        /*********************************************/
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
//            String.format("%.2f", resistance);
            resistanceVal = LocalConfig.Getvalue(Avg);
        }
//        double perimeter = (float) (3.14 * 0.102 * 2);

        if (time > 0) {
//            double K_index = 30d / (400d / (Math.ceil(Total_mileage) / Math.ceil(time)));//指数K
            Calories = resistanceVal * (time / 60) * average_speed;
        } else {
            Calories = 0;
        }
    }

    public void itinClick() {
        binding.btnTest.setOnClickListener(v -> {
            //  btDataPro.sendBTMessage(btDataPro.MAC_CODE);

            AddressBean addressBean = SharedPreferencesUtils.Companion.getInstance().getAddressString();

            if (addressBean != null) {
                btDataPro.sendBTMessage(btDataPro.
                        GetCmdCode(addressBean.getEcg(),
                                addressBean.getBloodPressure(),
                                addressBean.getBloodOxygen()));
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

        binding.intelligenceTitleActive.setOnClickListener(v -> {
            if (!LocalConfig.isControl) {
                Toast.makeText(this, R.string.bluetoothIsNotConnected, Toast.LENGTH_SHORT).show();
                return;
            }
            modletype = 1;
            if (BloodEndState == 1) {
                Toast.makeText(context, "还未测量运动后血压！", Toast.LENGTH_SHORT).show();
                return;
            }
            ChangeDialog();
        });

        binding.intelligenceTitlePassive.setOnClickListener(v -> {
            if (!LocalConfig.isControl) {
                Toast.makeText(this, R.string.bluetoothIsNotConnected, Toast.LENGTH_SHORT).show();
                return;
            }
            modletype = 2;
            if (BloodEndState == 1) {
                Toast.makeText(context, "还未测量运动后血压！", Toast.LENGTH_SHORT).show();
                return;
            }
            ChangeDialog();
        });

        binding.intelligenceImgBegin.setOnClickListener(v -> {
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

        binding.inteImgBlood.setOnClickListener(v -> {
            if (BaseUtil.isFastDoubleClick()) {
                return;
            }
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
                    isClickBlood = true;
                    if (ContorlState.equals("00") || ContorlState.equals("52")) {
                        btDataPro.sendBTMessage(GetCmdCode(resistance, "51", false, zhuansuData, spasm));
//                        btDataPro.sendBTMessage(btDataPro.getCONTORL_CODE_BEGIN());
                    } else if (ContorlState.equals("51")) {
                        btDataPro.sendBTMessage(GetCmdCode(resistance, "52", false, zhuansuData, spasm));
                        ContorlState = "52";
                        binding.inteTxtBlood.setCenterString("点击开始测量血压");
                    }
                } else {
                    Toast.makeText(context, "血压仪未连接，请检查设备", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        binding.inteJiaZhuansu.setOnClickListener(v -> {
            if (!LocalConfig.isControl) {
                Toast.makeText(this, R.string.bluetoothIsNotConnected, Toast.LENGTH_SHORT).show();
                return;
            }
            if (isBegin) {
                Toast.makeText(context, "运动中，请勿调节参数！", Toast.LENGTH_SHORT).show();
                return;
            }
            if (zhuansuData + 1 <= 60) {
                if (zhuansuData + 1 >= 30) {
                    if (isOk) {
                        zhuansuData = zhuansuData + 1;
                        binding.progressViewZhuansuIntelligence.setGraduatedEnabled(true);
//                            binding.progressViewZhuansuIntelligence.setEndProgress(Float.parseFloat(LocalConfig.GetProgress((float) zhuansuData, (float) 60)));
//                            binding.progressViewZhuansuIntelligence.startProgressAnimation();
                        binding.intelligenceTxtZhuansu.setCenterString(zhuansuData + "");

                    } else {
                        DialogLoader.getInstance().showConfirmDialog(
                                context,
                                getString(R.string.tip_permission),
                                getString(R.string.lab_ok),
                                (dialog, which) -> {
                                    dialog.dismiss();
                                    isOk = true;
                                    zhuansuData = zhuansuData + 1;
                                    binding.progressViewZhuansuIntelligence.setGraduatedEnabled(true);
//                                        binding.progressViewZhuansuIntelligence.setEndProgress(Float.parseFloat(LocalConfig.GetProgress((float) zhuansuData, (float) 60)));
//                                        binding.progressViewZhuansuIntelligence.startProgressAnimation();
                                    binding.intelligenceTxtZhuansu.setCenterString(zhuansuData + "");
                                    //    btDataPro.sendBTMessage(GetCmdCode(resistance, "50", false, zhuansuData, spasm));
                                },
                                getString(R.string.lab_cancel),
                                (dialog, which) -> {
                                    dialog.dismiss();
                                    isOk = false;
                                }
                        );
                    }

                } else {
                    zhuansuData = zhuansuData + 1;
                    binding.progressViewZhuansuIntelligence.setGraduatedEnabled(true);
//                        binding.progressViewZhuansuIntelligence.setEndProgress(Float.parseFloat(LocalConfig.GetProgress((float) zhuansuData, (float) 60)));
//                        binding.progressViewZhuansuIntelligence.startProgressAnimation();
                    binding.intelligenceTxtZhuansu.setCenterString(zhuansuData + "");
                    //  btDataPro.sendBTMessage(GetCmdCode(resistance, "50", false, zhuansuData, spasm));
                }
//                    binding.progressViewZhuansuIntelligence.setGraduatedEnabled(true);
//                    binding.progressViewZhuansuIntelligence.setEndProgress(Float.parseFloat(LocalConfig.GetProgress((float) zhuansuData, (float) 60)));
//                    binding.progressViewZhuansuIntelligence.startProgressAnimation();
//                    binding.intelligenceTxtZhuansu.setCenterString(zhuansuData + "");
                //  btDataPro.sendBTMessage(GetCmdCode(resistance, "50", false, zhuansuData, spasm));
            } else {
                zhuansuData = 60;
                //  btDataPro.sendBTMessage(GetCmdCode(resistance, "50", false, zhuansuData, spasm));
                return;
            }
        });

        binding.inteJianZhuansu.setOnClickListener(v -> {
            if (!LocalConfig.isControl) {
                Toast.makeText(this, R.string.bluetoothIsNotConnected, Toast.LENGTH_SHORT).show();
                return;
            }
            if (isBegin) {
                Toast.makeText(context, "运动中，请勿调节参数！", Toast.LENGTH_SHORT).show();
                return;
            }
            zhuansuData = zhuansuData - 1;
            if (zhuansuData < 0) {
                zhuansuData = 0;
                //   btDataPro.sendBTMessage(GetCmdCode(resistance, "50", false, zhuansuData, spasm));
                return;
            } else {
                binding.progressViewZhuansuIntelligence.setGraduatedEnabled(true);
                // binding.progressViewZhuansuIntelligence.setEndProgress(Float.parseFloat(LocalConfig.GetProgress((float) zhuansuData, (float) 60)));
                //   binding.progressViewZhuansuIntelligence.startProgressAnimation();
                binding.intelligenceTxtZhuansu.setCenterString(zhuansuData + "");
                //   btDataPro.sendBTMessage(GetCmdCode(resistance, "50", false, zhuansuData, spasm));
            }
        });

        binding.inteJiaResistance.setOnClickListener(v -> {
            if (!LocalConfig.isControl) {
                Toast.makeText(this, R.string.bluetoothIsNotConnected, Toast.LENGTH_SHORT).show();
                return;
            }
            if (isBegin) {
                Toast.makeText(context, "运动中，请勿调节参数！", Toast.LENGTH_SHORT).show();
                return;
            }
            resistance = resistance + 1;
            if (resistance <= 12) {
                binding.progressViewResistance.setGraduatedEnabled(true);
                //  binding.progressViewResistance.setEndProgress(Float.parseFloat(LocalConfig.GetProgress((float) resistance, (float) 12)));
                //   binding.progressViewResistance.startProgressAnimation();
                binding.intelligenceTxtResistance.setCenterString(resistance + "");
                //   btDataPro.sendBTMessage(GetCmdCode(resistance, "50", false, zhuansuData, spasm));
            } else {
                resistance = 12;
                //   btDataPro.sendBTMessage(GetCmdCode(resistance, "50", false, zhuansuData, spasm));
                return;
            }
        });

        binding.inteJianResistance.setOnClickListener(v -> {
            if (!LocalConfig.isControl) {
                Toast.makeText(this, R.string.bluetoothIsNotConnected, Toast.LENGTH_SHORT).show();
                return;
            }
            if (isBegin) {
                Toast.makeText(context, "运动中，请勿调节参数！", Toast.LENGTH_SHORT).show();
                return;
            }
            resistance = resistance - 1;
            if (resistance < 1) {
                resistance = 1;
                //   btDataPro.sendBTMessage(GetCmdCode(resistance, "50", false, zhuansuData, spasm));
                return;
            } else {
                binding.progressViewResistance.setGraduatedEnabled(true);
                //  binding.progressViewResistance.setEndProgress(Float.parseFloat(LocalConfig.GetProgress((float) resistance, (float) 12)));
                //    binding.progressViewResistance.startProgressAnimation();
                binding.intelligenceTxtResistance.setCenterString(resistance + "");
                //   btDataPro.sendBTMessage(GetCmdCode(resistance, "50", false, zhuansuData, spasm));
            }
        });

        binding.inteJiaSpasm.setOnClickListener(v -> {
            if (!LocalConfig.isControl) {
                Toast.makeText(this, R.string.bluetoothIsNotConnected, Toast.LENGTH_SHORT).show();
                return;
            }
            if (isBegin) {
                Toast.makeText(context, "运动中，请勿调节参数！", Toast.LENGTH_SHORT).show();
                return;
            }
            spasm = spasm + 1;
            if (spasm <= 12) {
                binding.progressViewSpasm.setGraduatedEnabled(true);
                //  binding.progressViewSpasm.setEndProgress(Float.parseFloat(LocalConfig.GetProgress((float) spasm, (float) 12)));
                //   binding.progressViewSpasm.startProgressAnimation();
                binding.intelligenceTxtSpasm.setCenterString(spasm + "");
                //    btDataPro.sendBTMessage(GetCmdCode(resistance, "50", false, zhuansuData, spasm));
            } else {
                spasm = 12;
                //   btDataPro.sendBTMessage(GetCmdCode(resistance, "50", false, zhuansuData, spasm));
                return;
            }
        });

        binding.inteJianSpasm.setOnClickListener(v -> {
            if (!LocalConfig.isControl) {
                Toast.makeText(this, R.string.bluetoothIsNotConnected, Toast.LENGTH_SHORT).show();
                return;
            }
            if (isBegin) {
                Toast.makeText(context, "运动中，请勿调节参数！", Toast.LENGTH_SHORT).show();
                return;
            }
            spasm = spasm - 1;
            if (spasm < 1) {
                spasm = 1;
                //    btDataPro.sendBTMessage(GetCmdCode(resistance, "50", false, zhuansuData, spasm));
                return;
            } else {
                binding.progressViewSpasm.setGraduatedEnabled(true);
                // binding.progressViewSpasm.setEndProgress(Float.parseFloat(LocalConfig.GetProgress((float) spasm, (float) 12)));
                //  binding.progressViewSpasm.startProgressAnimation();
                binding.intelligenceTxtSpasm.setCenterString(spasm + "");
                //    btDataPro.sendBTMessage(GetCmdCode(resistance, "50", false, zhuansuData, spasm));
            }
        });
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
                    if (modletype == 1) {
                        LocalConfig.ModType = 0;
                        Intent in = new Intent(context, ActiveActivity.class);
                        startActivity(in);
                        finish();
                    } else if (modletype == 2) {
                        LocalConfig.ModType = 1;
                        Intent in = new Intent(context, PassiveActivity.class);
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

    public String GetCmdCode(int zuli, String blood_measure, boolean isBegin, int speed_lv, int spasms_lv) {
        String cmd_head = "A88408",              //包头
                sport_mode = "00",                //运动模式
                active_direction = "20",          //运动方向
                // spasms_lv = "00",                 //痉挛等级
                //    speed_lv = "00",                  //速度设定
                time_lv = "00",                   //设定时间
                cmd_end = "ED";                   //结尾
        String zuliHex = "0" + BtDataProX.Companion.decToHex(zuli);
        String spasmsHex = "0" + BtDataProX.Companion.decToHex(spasms_lv);
        String speedHex = "";
        if (speed_lv >= 16) {
            speedHex = BtDataProX.Companion.decToHex(speed_lv);
        } else {
            speedHex = "0" + BtDataProX.Companion.decToHex(speed_lv);
        }

        String avtive_status = "10";
        if (isBegin) {
            avtive_status = "11";
        }

        String splicingStr = cmd_head + sport_mode + avtive_status + active_direction + zuliHex + spasmsHex
                + speedHex + time_lv + blood_measure;
        String CRC16 = CRC16Util.getCRC16(splicingStr);
        CMD_CODE = splicingStr + CRC16 + cmd_end;
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
//                        uploadData.setHigh("150");
//                        uploadData.setLow("80");
                    } else {
                        if(isClickBlood){
                            uploadData.setHigh("120");
                            uploadData.setLow("60");
                        }
                    }
                }

                if (isBegin) {
                    spasm = Integer.parseInt(uploadData.getSTspasm());
                    binding.progressViewSpasm.setGraduatedEnabled(true);
//                    binding.progressViewSpasm.setEndProgress(Float.parseFloat(LocalConfig.GetProgress((float) spasm, (float) 12)));
//                    binding.progressViewSpasm.startProgressAnimation();
                    binding.intelligenceTxtSpasm.setCenterString(uploadData.getSTspasm());

                    zhuansuData = Integer.parseInt(uploadData.getSTspeed());
                    binding.progressViewZhuansuIntelligence.setGraduatedEnabled(true);
//                    binding.progressViewZhuansuIntelligence.setEndProgress(Float.parseFloat(LocalConfig.GetProgress((float) zhuansuData, (float) 60)));
//                    binding.progressViewZhuansuIntelligence.startProgressAnimation();
                    binding.intelligenceTxtZhuansu.setCenterString(uploadData.getSTspeed());

                    resistance = Integer.parseInt(uploadData.getSTresistance());
                    binding.progressViewResistance.setGraduatedEnabled(true);
//                    binding.progressViewResistance.setEndProgress(Float.parseFloat(LocalConfig.GetProgress((float) resistance, (float) 12)));
//                    binding.progressViewResistance.startProgressAnimation();
                    binding.intelligenceTxtResistance.setCenterString(uploadData.getSTresistance());
                }

                if (uploadData.getBlood_oxy().equals("已连接")) {
                    if (uploadData.getOxy_vaulestr().equals("手指未插入")
                            || uploadData.getOxy_vaulestr().equals("探头脱落")
                            || uploadData.getOxy_vaulestr().equals("127")) {
                        binding.inteTxtBoxygen.setCenterString("--");
                        if (uploadData.getOxy_vaulestr().equals("127")) {
                            binding.inteTxtO2State.setCenterString("检测中..");
                        } else {
                            binding.inteTxtO2State.setCenterString(uploadData.getOxy_vaulestr());
                        }
                    } else {
                        binding.inteTxtBoxygen.setCenterString(uploadData.getOxy_vaulestr());
                        binding.inteTxtO2State.setCenterString("");
                    }
                } else {
                    binding.inteTxtBoxygen.setCenterString("--");
                    binding.inteTxtO2State.setCenterString(uploadData.getBlood_oxy());
                }

                if (uploadData.getBlood().equals("已连接")) {

                    if (uploadData.getHigh().equals("255") || uploadData.getLow().equals("255")) {
                        binding.inteTxtHigh.setCenterString("0");
                        binding.inteTxtLow.setCenterString("0");
                        binding.inteTxtBloodstate1.setCenterString("测量错误");
                        binding.inteTxtBloodstate2.setCenterString("测量错误");
                    } else {

                        if (!uploadData.getHigh().equals("0")) {
                            LocalConfig.BloodHight = uploadData.getHigh();
                            LocalConfig.BloodLow = uploadData.getLow();

                            if (isClickBlood) {//是否点击过测量血压
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

                        binding.inteTxtHigh.setCenterString(uploadData.getHigh());
                        binding.inteTxtLow.setCenterString(uploadData.getLow());
                        binding.inteTxtBloodstate1.setCenterString("");
                        binding.inteTxtBloodstate2.setCenterString("");
                    }
                } else {
                    binding.inteTxtHigh.setCenterString(LocalConfig.BloodHight);
                    binding.inteTxtLow.setCenterString(LocalConfig.BloodLow);
                    binding.inteTxtBloodstate1.setCenterString(uploadData.getBlood());
                    binding.inteTxtBloodstate2.setCenterString(uploadData.getBlood());
                }

                if (uploadData.getECG().equals("已连接")) {
                    binding.inteTxtEcgstate.setCenterString("");
                } else {
                    binding.inteTxtCoory.setCenterString("--");
                    binding.inteTxtEcgstate.setCenterString(uploadData.getECG());
                }

                int left = Integer.parseInt(uploadData.getLeft());
                binding.progressViewLeft.setGraduatedEnabled(true);
                // binding.progressViewLeft.setEndProgress(Float.parseFloat(LocalConfig.GetProgress((float) left, (float) 50)));
                //  binding.progressViewLeft.startProgressAnimation();
                binding.intelligenceTxtLeft.setCenterString(left + "");

                int right = Integer.parseInt(uploadData.getRight());
                binding.progressViewRight.setGraduatedEnabled(true);
                //binding.progressViewRight.setEndProgress(Float.parseFloat(LocalConfig.GetProgress((float) right, (float) 50)));
                // binding.progressViewRight.startProgressAnimation();
                binding.intelligenceTxtRight.setCenterString(right + "");

                if (uploadData.getActiveState().equals("停机状态")) {
                    if (isBegin) {
                        isBegin = false;
                        binding.intelligenceImgBegin.setBackground(getResources().getDrawable(R.drawable.begin));
                        binding.intelligenceTxtBegin.setCenterString("开  始");
                        stop();
                        timecount = timeCountTool.stopCount();
                        activeTimeTool.stopCount();
                        getCalories_mileage();
                        if (uploadData.getSpasmState() != -1) {
                            if (spasmCount == 5) {
                                DialogLoader.getInstance().showConfirmDialog(
                                        context,
                                        getString(R.string.active_out),
                                        getString(R.string.lab_yes),
                                        (dialog, which) -> {

                                            BloodEndState = 1;
                                            if (uploadData != null && uploadData.getBlood().equals("已连接")) {
                                                if (ContorlState.equals("00") || ContorlState.equals("52")) {
                                                    btDataPro.sendBTMessage(GetCmdCode(resistance, "51", false, zhuansuData, spasm));
                                                    btDataPro.sendBTMessage(btDataPro.getCONTORL_CODE_BEGIN());
                                                } else if (ContorlState.equals("51")) {
                                                    btDataPro.sendBTMessage(GetCmdCode(resistance, "52", false, zhuansuData, spasm));
                                                    ContorlState = "52";
                                                    binding.inteTxtBlood.setCenterString("点击开始测量血压");
                                                }
                                            } else {
                                                Toast.makeText(context, "血压仪未连接，请检查设备", Toast.LENGTH_SHORT).show();
                                            }
                                        },
                                        getString(R.string.lab_no),
                                        (dialog, which) -> {
                                            dialog.dismiss();
                                        }
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
                                                if (ContorlState.equals("00") || ContorlState.equals("52")) {
                                                    btDataPro.sendBTMessage(GetCmdCode(resistance, "51", false, zhuansuData, spasm));
//                                                    btDataPro.sendBTMessage(btDataPro.getCONTORL_CODE_BEGIN());
                                                } else if (ContorlState.equals("51")) {
                                                    btDataPro.sendBTMessage(GetCmdCode(resistance, "52", false, zhuansuData, spasm));
                                                    ContorlState = "52";
                                                    binding.inteTxtBlood.setCenterString("点击开始测量血压");
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
                    binding.intelligenceImgBegin.setBackground(getResources().getDrawable(R.drawable.stop));
                    binding.intelligenceTxtBegin.setCenterString("停  止");
                    timeCountTool.startCount();
                    String ActiveType = uploadData.getActiveType();
                    if (ActiveType.equals("智能模式/主动")) {
                        activeTimeTool.startCount();
                    } else if (ActiveType.equals("智能模式/被动")) {
                        activeTimeTool.stopCount();
                    } else {
                        activeTimeTool.stopCount();
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
                    // in.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(in);
                    finish();
                }
                break;

            case 2:
                try {
                    EcgListData = null;
                    ecgData = gson.fromJson(ObjectJson, EcgData.class);
                    binding.inteTxtCoory.setCenterString(ecgData.getHeartrate());
                    EcgListData = ecgData.getEcgCoorY();
                    if (EcgListData == null) {
                        binding.inteTxtEcgstate.setCenterString("心电仪佩戴异常！");
                        binding.IntelligenceWaveviewOne.showLine(0f);
//                        binding.IntelligenceWaveviewTwo.showLine(0f);
//                        binding.IntelligenceWaveviewThree.showLine(0f);
                    } else {
                        // Float
                        for (int i = 0; i < EcgListData.size(); i++) {
                            Float cooY = EcgListData.get(i);
                            OftenListData.add(cooY);
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                break;

            case 3:
                ContorlState = ObjectJson;
                break;
        }
    }

    public void HandlerMessage() {
        String txts = binding.intelligenceTxtBegin.getCenterString();

        if (txts.equals("开  始")) {

            String highblood = binding.inteTxtHigh.getCenterString();
            String lowblood = binding.inteTxtLow.getCenterString();
            if (highblood.equals("0") && lowblood.equals("0")) {
                DialogLoader.getInstance().showConfirmDialog(
                        context,
                        getString(R.string.active_blood),
                        getString(R.string.lab_ok),
                        (dialog, which) -> {
                            dialog.dismiss();
                            btDataPro.sendBTMessage(GetCmdCode(resistance, "50", true, zhuansuData, spasm));
                            binding.intelligenceImgBegin.setBackground(getResources().getDrawable(R.drawable.stop));
                            binding.intelligenceTxtBegin.setCenterString("停  止");
                            timeCountTool.startCount();
                        },
                        getString(R.string.lab_cancel),
                        (dialog, which) -> {
                            dialog.dismiss();
                            // timeTask.start();
                        }
                );

            } else {
                btDataPro.sendBTMessage(GetCmdCode(resistance, "50", true, zhuansuData, spasm));
                binding.intelligenceImgBegin.setBackground(getResources().getDrawable(R.drawable.stop));
                binding.intelligenceTxtBegin.setCenterString("停  止");
                timeCountTool.startCount();
            }

        } else {
            btDataPro.sendBTMessage(GetCmdCode(resistance, "50", false, zhuansuData, spasm));
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
            recordDetailed.setActivtType("智能模式");
            recordDetailed.setRecordTime(sim);
            int zhuansu = Integer.parseInt(binding.intelligenceTxtZhuansu.getCenterString());
            recordDetailed.setSpeed(zhuansu);
            int leftlimb = Integer.parseInt(binding.intelligenceTxtLeft.getCenterString());
            recordDetailed.setLeftLimb(leftlimb);
            int rightlimb = Integer.parseInt(binding.intelligenceTxtRight.getCenterString());
            recordDetailed.setRightLimb(rightlimb);
            int resistance = Integer.parseInt(binding.intelligenceTxtResistance.getCenterString());
            recordDetailed.setResistance(resistance);

            int heartRate;
            if (binding.inteTxtCoory.getCenterString().equals("--")) {
                heartRate = 0;
            } else {
                heartRate = Integer.parseInt(binding.inteTxtCoory.getCenterString());
            }
            recordDetailed.setHeartRate(heartRate);

            int Hbo2;
            if (binding.inteTxtBoxygen.getCenterString().equals("--")) {
                Hbo2 = 0;
            } else {
                Hbo2 = Integer.parseInt(binding.inteTxtBoxygen.getCenterString());
            }
            recordDetailed.setHbo2(Hbo2);

            recordDetailed.setSpasm(spasm);
            //  recordDetailed.setSpasmCount(0);
            recordDetailedDao.insert(recordDetailed);
            //500表示调用schedule方法后等待500ms后调用run方法，50表示以后调用run方法的时间间隔
        } catch (Exception e) {
            Toast.makeText(context, "数据库异常" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
                        btDataPro.sendBTMessage(GetCmdCode(resistance, "50", false, zhuansuData, spasm));
                    }

                    if (!isBegin) {
                        Intent in = new Intent(context, AdminMainActivity.class);
                        //   in.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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

    public void stop() {
        if (timer1 != null) {
            timer1.cancel();
            timer1.purge();
            timer1 = null;
        }
        if (timerTask1 != null) {
            timerTask1.cancel();
            timerTask1 = null;
        }
        if (downTimer != null) {
            downTimer.cancel();
            downTimer = null;
        }
        OftenListData = new ArrayList<>();
        // LocalConfig.spasmCount = "0";
        binding.IntelligenceWaveviewOne.resetCanavas();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
        LocalConfig.BloodHight = "0";
        LocalConfig.BloodLow = "0";
        TimeCountTool.setClean();
        ActiveTimeTool.setClean();
    }
}
