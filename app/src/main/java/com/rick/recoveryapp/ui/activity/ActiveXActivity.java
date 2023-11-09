package com.rick.recoveryapp.ui.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.common.base.CommonBaseActivity;
import com.common.network.LogUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rick.recoveryapp.R;
import com.rick.recoveryapp.ui.activity.helper.Constants;
import com.rick.recoveryapp.ui.activity.serial.AddressBean;
import com.rick.recoveryapp.ui.activity.serial.SerialBean;
import com.rick.recoveryapp.ui.activity.serial.SerialPort;
import com.rick.recoveryapp.ui.activity.serial.SerialPort.Type;
import com.rick.recoveryapp.ui.BaseApplication;
import com.rick.recoveryapp.ui.service.BtDataPro;
import com.rick.recoveryapp.chart.MyAVG;
import com.rick.recoveryapp.databinding.ActivityActiviteXBinding;
import com.rick.recoveryapp.databinding.ActivityManagerBinding;
import com.rick.recoveryapp.entity.EcgData;
import com.rick.recoveryapp.entity.LiveMessage;
import com.rick.recoveryapp.entity.protocol.PoolMessage;
import com.rick.recoveryapp.entity.protocol.UploadData;
import com.rick.recoveryapp.greendao.ActivitRecordDao;
import com.rick.recoveryapp.greendao.EcgDataDBDao;
import com.rick.recoveryapp.greendao.RecordDetailedDao;
import com.rick.recoveryapp.greendao.entity.ActivitRecord;
import com.rick.recoveryapp.greendao.entity.RecordDetailed;
import com.rick.recoveryapp.ui.activity.serial.SharedPreferencesUtils;
import com.rick.recoveryapp.utils.ActiveTimeTool;
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
import java.util.Timer;
import java.util.TimerTask;

/*
 * "主动模式需要重构界面冗余"
 * */
public class ActiveXActivity extends CommonBaseActivity<ActivityActiviteXBinding> {
    ActivityManagerBinding managerBinding;
    ArrayList<Float> EcgListData;
    static ArrayList<Float> OftenListData;
    RecordDetailedDao recordDetailedDao;
    EcgDataDBDao ecgDataDBDao;
    UploadData uploadData;
    EcgData ecgData;
    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    String controlState = "52";//0x50：默认状态；0x51：启动血压测量；0x52：停止血压测量； 0x53: 清除当前血压测试数据
    BtDataPro btDataPro;
    private Timer timer1;
    private TimerTask timerTask1;
    boolean isBegin = false;
    Context context;
    ActivitRecordDao activitRecordDao;//数据库操作
    public TimeCountTool timeCountTool = TimeCountTool.getInstance();//操作时间
    String timeCount = "";
    double Total_mileage, Calories;
    String Active_B_Diastole_Shrink = "0/0", Active_L_Diastole_Shrink = "0/0";//主动模式被动模式都有
    int BloodEndState = 0; // 0:初始状态  1：需要测量血压   2：血压测量完成

    //被动模式
    ArrayList<Integer> countList;

    static PeterTimeCountRefresh downTimer;//向下定时器

    static long nowTime = 300000;//现在时间
    int resistance = 1;//阻力

    int rotateSpeed = 5;//转速

    int spasmData = 1;//运动方向

    Long activeTime = 0L; //活动时间

    boolean isOk = false; //是否确认

    int spasmCount = 0; //痉挛等级

    //智能模式
    int spasm = 1;//痉挛等级

    public ActiveTimeTool activeTimeTool = ActiveTimeTool.getInstance();


    //通用改革
    SerialBean serialBean = new SerialBean();//串口参数

    ActiveManager activeManager;//布局管理器

    Type type;//进入的模式

    public static void newActiveXActivity(Context context, Type type) {
        Intent intent = new Intent(context, ActiveXActivity.class);
        intent.putExtra("type", type);
        context.startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        StatusBarUtils.translucent(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        context = this;
    }

    @Override
    protected void initView() {
        activeManager = new ActiveManager(this);//自定义layout

        managerBinding = getBinding().acManager.binding;

        activeManager.setManagerBinding(managerBinding);

        initData();//初始化数据

        initViewType();//不同模式不同界面

        initClick();//点击操作

        try {
            initLiveData();

            initSerial();

            PassEcg();
        } catch (Exception ex) {
            ex.getMessage();
        }
    }

    private void initSerial() {

        serialBean.setType(type);

        serialBean.setZuli(1);

        serialBean.setBlood_measure("53");

        serialBean.setBegin(false);

        if (type == Type.SUBJECT) {
            serialBean.setSpeed_lv(5);
            serialBean.setSpasms_lv(5);
        }

        if (type == Type.INTELLIGENT) {
            serialBean.setSpeed_lv(rotateSpeed);
            serialBean.setSpasms_lv(spasm);
        }

        serialBean.setTime_lv(activeTime);

        btDataPro.sendBTMessage(SerialPort.Companion.getCmdCode(serialBean));
    }

    private void initData() {
        Intent intent = getIntent();
        type = (Type) intent.getSerializableExtra("type");//获取点击的模式
        assert type != null;
        LogUtils.d("type==" + type.name());
        btDataPro = new BtDataPro();
        managerBinding.activeTxtMassage.setLeftString(" 患者姓名：" + LocalConfig.userName);
        managerBinding.activeTxtMassage.setLeftBottomString(" 患者编号：" + LocalConfig.medicalNumber);
        BaseApplication myApp = (BaseApplication) getApplication();
        LocalConfig.daoSession = myApp.getDaoSession();
        activitRecordDao = LocalConfig.daoSession.getActivitRecordDao();
        recordDetailedDao = LocalConfig.daoSession.getRecordDetailedDao();

        EcgListData = new ArrayList<>();
        OftenListData = new ArrayList<>();
        managerBinding.activeWaveShowView.resetCanavas();
        ecgDataDBDao = LocalConfig.daoSession.getEcgDataDBDao();
    }

    public void initViewType() {//不同模式不同界面

        serialBean.clearData();//清空数据

        BloodEndState = 0;

        rotateSpeed = 5;//转速

        resistance = 1;//阻力

        spasm = 1;//痉挛

        activeManager.initViewType(type);

        if (type == Type.SUBJECT) {
            nowTime = 300000;
            activeTime = MyTimeUtils.Getminute(nowTime);
            String text1 = MyTimeUtils.formatTime(nowTime);
            managerBinding.passiveTxtDowntimer.setCenterString(text1);
            countList = new ArrayList<>();
        }

        if (type == Type.INTELLIGENT) {
            managerBinding.activeTxtZhuansu.setCenterString(rotateSpeed + "");
            managerBinding.activeTxtResistance.setCenterString(resistance + "");
            managerBinding.activeTxtSpasm.setCenterString(spasm + "");
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
                            managerBinding.activeWaveShowView.showLine(cooY);
                            OftenListData.remove(0);
                        }
                    }
                } catch (Exception e) {
                    LogUtils.d("EcgError" + e.getMessage());
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
//                        Log.d("BT", msg.getObjectName());
                    if (msg.getObjectName().equals(btDataPro.UPLODE_ANSWER)) {
                        UploadData uploadData;
                        uploadData = gson.fromJson(msg.getObjectJson(), UploadData.class);
//                            LogUtils.d("BTBlood"+ uploadData.getBlood_oxy());
                        if (uploadData.getECG().equals("已连接")) {
                            managerBinding.trainButEcg.setBackgroundResource(R.drawable.xindian_ok);
                        } else {
                            managerBinding.trainButEcg.setBackgroundResource(R.drawable.xindian_no);
                        }
                        if (uploadData.getBlood().equals("已连接")) {
                            managerBinding.trainButBp.setBackgroundResource(R.drawable.xueya_ok);
                        } else {
                            managerBinding.trainButBp.setBackgroundResource(R.drawable.xueya_no);
                        }
                        if (uploadData.getBlood_oxy().equals("已连接")) {
                            managerBinding.trainButO2.setBackgroundResource(R.drawable.o2_ok);
                        } else {
                            managerBinding.trainButO2.setBackgroundResource(R.drawable.o2_no);
                        }
                    }
                }
            } else {
                LogUtils.d("BT" + "没有任何数据");
            }

        });

        LiveDataBus.get().with(Constants.BT_CONNECTED).observe(this, v -> {
            if (v instanceof LiveMessage) {
                LiveMessage msg = (LiveMessage) v;
                if (!msg.getIsConnt()) {//未连接

                    managerBinding.trainButEcg.setBackgroundResource(R.drawable.xindian_no);
                    managerBinding.trainButBp.setBackgroundResource(R.drawable.xueya_no);
                    managerBinding.trainButO2.setBackgroundResource(R.drawable.o2_no);

                    managerBinding.activeTxtBoxygen.setCenterString("0");
                    managerBinding.activeTxtO2State.setCenterString("血氧仪未连接");
                    managerBinding.activeTxtBloodstate1.setCenterString("血压仪未连接");
                    managerBinding.activeTxtBloodstate2.setCenterString("血压仪未连接");

                    if (type == Type.INTELLIGENT) {
                        managerBinding.progressViewLeft.setGraduatedEnabled(true);
                        managerBinding.progressViewRight.setGraduatedEnabled(true);
                        managerBinding.activeTxtLeft.setCenterString("0");
                    }

                    if (type == Type.ACTIVE) {
                        int left = 0;
                        managerBinding.progressViewLeft.setGraduatedEnabled(true);
                        managerBinding.progressViewLeft.setEndProgress(Float.parseFloat(LocalConfig.GetProgress((float) left, (float) 50)));
                        managerBinding.activeTxtLeft.setCenterString("0");

                        int right = 0;
                        managerBinding.progressViewRight.setGraduatedEnabled(true);
                        managerBinding.progressViewRight.setEndProgress(Float.parseFloat(LocalConfig.GetProgress((float) right, (float) 50)));
                        managerBinding.activeTxtRight.setCenterString("0");
                    }

                    managerBinding.activeTxtCoory.setCenterString("--");
                    managerBinding.activeTxtEcgstate.setCenterString("心电仪未连接");
                    OftenListData.clear();
                }
            }
        });

        LiveDataBus.get().with(Constants.BT_PROTOCOL).observe(this, v -> {
            if (v instanceof PoolMessage) {
                PoolMessage msg = (PoolMessage) v;
                if (msg.isState()) {
                    int mark = 0;
                    if (msg.getObjectName().equals(btDataPro.UPLODE_ANSWER)) {
                        mark = 1;
                    } else if (msg.getObjectName().equals(btDataPro.ECGDATA_ANSWER)) {
                        mark = 2;
                    } else if (msg.getObjectName().equals(btDataPro.CONTORL_ANSWER)) {
                        mark = 3;
                    }
                    DataDisplay(mark, msg.getObjectJson());
                    if (isBegin) {
                        updatePerogress();
                    }
                } else {
                    Toast.makeText(context, "数据异常", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @SuppressLint("DefaultLocale")
    public void SaveRecord() {
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        String sim = dateFormat.format(date);
        String aduration = "0";
        String pduration = "0";
        if (type == Type.INTELLIGENT) {
            activeTimeTool.stopCount();
            int SurplusTime = timeCountTool.getTime() - activeTimeTool.getTime();
            aduration = activeTimeTool.SurplusTi(activeTimeTool.getTime());
            pduration = activeTimeTool.SurplusTi(SurplusTime);
        }

        ActivitRecord activitRecord = new ActivitRecord();
        activitRecord.setRecordID(LocalConfig.UserID);
        activitRecord.setUserName(LocalConfig.userName);
        activitRecord.setUserNumber(LocalConfig.medicalNumber);
        activitRecord.setRecordTime(sim);
        activitRecord.setLongTime(timeCount);
        activitRecord.setAduration(aduration);
        activitRecord.setPduration(pduration);
        activitRecord.setActivtType(LocalConfig.ModType + "");
        activitRecord.setB_Diastole_Shrink(Active_B_Diastole_Shrink);
        activitRecord.setL_Diastole_Shrink(Active_L_Diastole_Shrink);
        activitRecord.setTotal_mileage(String.format("%.2f", Total_mileage));
        activitRecord.setCalories(String.format("%.2f", Calories));
        activitRecord.setSpasmCount(0 + "");
        activitRecordDao.insert(activitRecord);

        MyAVG myAVG = new MyAVG();
        myAVG.GetAvg(LocalConfig.UserID + "");

        Active_B_Diastole_Shrink = "0/0";
        Active_L_Diastole_Shrink = "0/0";
        timeCountTool.setTime(0);
    }

    //计算总里程,卡路里
    public void getCalories_mileage() {
        double time = timeCountTool.GetSecond();
        double speed = 0;
        double average_speed;
        double resistance = 0;
        double resistanceVal = 0;

        List<RecordDetailed> recordList = recordDetailedDao.queryBuilder().where(//* 查询转速不为“0” 的所有记录集合。集合不为大于0时取平均值
                        RecordDetailedDao.Properties.Speed.notEq("0"),
                        RecordDetailedDao.Properties.RecordID.eq(LocalConfig.UserID))
                .list();
        if (!recordList.isEmpty()) {
            for (int i = 0; i < recordList.size(); i++) {
                speed = recordList.get(i).getSpeed() + speed;
            }
            average_speed = speed / recordList.size(); //平均转速
        } else {
            average_speed = 0;
        }

        double perimeter = (float) (3.14 * 0.102 * 2);

        Total_mileage = average_speed * time * perimeter;//总里程

        if (type == Type.ACTIVE || type == Type.INTELLIGENT) {
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
        }

        if (type == Type.SUBJECT) {
            resistanceVal = 5;
        }

        if (time > 0) {
            Calories = resistanceVal * (time / 60) * average_speed;
        } else {
            Calories = 0;
        }
    }


    public void initClick() {
        managerBinding.btnTest.setOnClickListener(v -> {
            if (type == Type.SUBJECT) {//被动模式直接发送
                AddressBean addressBean = SharedPreferencesUtils.Companion.getInstance().getAddressString();

                if (addressBean != null) {
                    btDataPro.sendBTMessage(btDataPro.
                            GetCmdCode(addressBean.getEcg(),
                                    addressBean.getBloodPressure(),
                                    addressBean.getBloodOxygen()));
                }
                return;
            }

            if (type == Type.ACTIVE) {//主动模式要判断一下是否可以发送
                if (LocalConfig.isControl) {
                    AddressBean addressBean = SharedPreferencesUtils.Companion.getInstance().getAddressString();
                    if (addressBean != null) {
                        btDataPro.sendBTMessage(btDataPro.
                                GetCmdCode(addressBean.getEcg(),
                                        addressBean.getBloodPressure(),
                                        addressBean.getBloodOxygen()));
                    }
                }
            }

        });

        managerBinding.btnClose.setOnClickListener(v -> {
            if (BloodEndState == 1) {
                BloodEndState = 2;//取消测量运动后血压
            } else if (BloodEndState == 0) {
                dialogs();
            }
        });

        //切换训练模式
        managerBinding.stxActiveTitle.setOnClickListener(v -> ChangeDialog(Type.ACTIVE));

        managerBinding.stxPressTitle.setOnClickListener(v -> ChangeDialog(Type.SUBJECT));

        managerBinding.stxIntelligenceTitle.setOnClickListener(v -> ChangeDialog(Type.INTELLIGENT));

        //开始按钮
        managerBinding.activeImgBegin.setOnClickListener(v -> HandlerMessage());

        //设置Rpm加减
        managerBinding.sbRpmUp.setOnClickListener(v -> {
            if (type == Type.ACTIVE) {
                return;
            }
            if (rotateSpeed + 1 <= 60) {
                if (rotateSpeed + 1 >= 30) {
                    if (isOk) {
                        rotateSpeed = rotateSpeed + 1;
                        managerBinding.progressViewZhuansuActicve.setGraduatedEnabled(true);
                        managerBinding.activeTxtZhuansu.setCenterString(rotateSpeed + "");
                    } else {
                        DialogLoader.getInstance().showConfirmDialog(
                                context,
                                getString(R.string.tip_permission),
                                getString(R.string.lab_ok),
                                (dialog, which) -> {
                                    dialog.dismiss();
                                    isOk = true;
                                    rotateSpeed = rotateSpeed + 1;
                                    managerBinding.progressViewZhuansuActicve.setGraduatedEnabled(true);
                                    managerBinding.activeTxtZhuansu.setCenterString(rotateSpeed + "");
                                },
                                getString(R.string.lab_cancel),
                                (dialog, which) -> {
                                    dialog.dismiss();
                                    isOk = false;
                                }
                        );
                    }
                } else {
                    rotateSpeed = rotateSpeed + 1;
                    managerBinding.progressViewZhuansuActicve.setGraduatedEnabled(true);
                    managerBinding.activeTxtZhuansu.setCenterString(rotateSpeed + "");
                }
            } else {
                rotateSpeed = 60;
            }

        });

        managerBinding.sbRpmDown.setOnClickListener(v -> {
            if (isBegin) {
                Toast.makeText(context, "运动中，请勿调节参数！", Toast.LENGTH_SHORT).show();
                return;
            }
            rotateSpeed = rotateSpeed - 1;
            if (rotateSpeed < 0) {
                rotateSpeed = 0;
            } else {
                managerBinding.progressViewZhuansuActicve.setGraduatedEnabled(true);
                managerBinding.activeTxtZhuansu.setCenterString(rotateSpeed + "");
            }
        });

        //被动模式右下角按钮操作
        managerBinding.passiveTimeJia.setOnClickListener(v -> {
            nowTime = nowTime + 300000;
            String text = MyTimeUtils.formatTime(nowTime);
            if (nowTime <= 3600000) {
                managerBinding.passiveTxtDowntimer.setCenterString(text);
            } else {
                nowTime = 3600000;
                text = MyTimeUtils.formatTime(nowTime);
                managerBinding.passiveTxtDowntimer.setCenterString(text);
            }
            activeTime = MyTimeUtils.Getminute(nowTime);

            btDataPro.sendBTMessage(SerialPort.Companion.getCmdCode(
                    new SerialBean(type, resistance, "50", false, rotateSpeed, spasmData, activeTime)));
        });

        managerBinding.passiveTimeJian.setOnClickListener(v -> {
            nowTime = nowTime - 300000;
            String text1 = MyTimeUtils.formatTime(nowTime);
            if (nowTime >= 300000) {
                managerBinding.passiveTxtDowntimer.setCenterString(text1);
            } else {
                nowTime = 300000;
                text1 = MyTimeUtils.formatTime(nowTime);
                managerBinding.passiveTxtDowntimer.setCenterString(text1);
            }
            activeTime = MyTimeUtils.Getminute(nowTime);
            btDataPro.sendBTMessage(SerialPort.Companion.getCmdCode(
                    new SerialBean(type, resistance, "50", false, rotateSpeed, spasmData, activeTime)));
        });


        //设置阻力加减
        managerBinding.activeImbtnJia.setOnClickListener(v -> {
            if (isBegin) {
                Toast.makeText(context, "运动中，请勿调节参数！", Toast.LENGTH_SHORT).show();
                return;
            }

            if (type == Type.INTELLIGENT) {
                if (BloodEndState == 1) {
                    Toast.makeText(context, "还未测量运动后血压！", Toast.LENGTH_SHORT).show();
                } else {
                    resistance = resistance + 1;
                    if (resistance <= 12) {
                        managerBinding.progressViewResistance.setGraduatedEnabled(true);
                        managerBinding.activeTxtResistance.setCenterString(resistance + "");
                    } else {
                        resistance = 12;
                    }
                }
            } else {
                resistance = resistance + 1;
                if (resistance <= 12) {
                    managerBinding.progressViewResistance.setGraduatedEnabled(true);
                    managerBinding.activeTxtResistance.setCenterString(resistance + "");
                } else {
                    resistance = 12;
                }
            }
        });

        managerBinding.activeImbtnMove.setOnClickListener(v -> {
            if (BloodEndState == 1) {
                Toast.makeText(context, "还未测量运动后血压！", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isBegin) {
                Toast.makeText(context, "运动中，请勿调节参数！", Toast.LENGTH_SHORT).show();
                return;
            }
            resistance = resistance - 1;
            if (resistance < 1) {
                resistance = 1;
            } else {
                managerBinding.progressViewResistance.setGraduatedEnabled(true);
                managerBinding.activeTxtResistance.setCenterString(resistance + "");
            }
        });

        //痉挛等级
        managerBinding.passiveSpasmJia.setOnClickListener(v -> {
            if (isBegin) {
                Toast.makeText(context, "运动中，请勿调节参数！", Toast.LENGTH_SHORT).show();
                return;
            }
            spasm = spasm + 1;
            if (spasm <= 12) {
                managerBinding.progressViewSpasm.setGraduatedEnabled(true);
                managerBinding.activeTxtSpasm.setCenterString(spasm + "");
            } else {
                spasm = 12;
            }
        });

        managerBinding.passiveSpasmJian.setOnClickListener(v -> {
            if (isBegin) {
                Toast.makeText(context, "运动中，请勿调节参数！", Toast.LENGTH_SHORT).show();
                return;
            }
            spasm = spasm - 1;
            if (spasm < 1) {
                spasm = 1;
            } else {
                managerBinding.progressViewSpasm.setGraduatedEnabled(true);
                managerBinding.activeTxtSpasm.setCenterString(spasm + "");
            }
        });

        //开始测量血压按钮
        managerBinding.activeImgBlood.setOnClickListener(v -> {
            if (isBegin) {
                Toast.makeText(context, "运动中，不能测量血压！", Toast.LENGTH_SHORT).show();
                return;
            }
            serialBean.clearData();//清空数据

            if (uploadData != null && uploadData.getBlood().equals("已连接")) {
                if (controlState.equals("00") || controlState.equals("52")) {

                    serialBean.setBlood_measure("51");

                    if (type == Type.ACTIVE || type == Type.INTELLIGENT) {
                        btDataPro.sendBTMessage(btDataPro.CONTORL_CODE_BEGIN);
                    }

                } else if (controlState.equals("51")) {

                    serialBean.setBlood_measure("52");

                    managerBinding.activeTxtBlood.setCenterString("点击开始测量血压");
                }

                serialBean.setType(type);

                serialBean.setZuli(resistance);

                serialBean.setBegin(false);


                if (type == Type.SUBJECT) {
                    serialBean.setSpeed_lv(rotateSpeed);
                    serialBean.setSpasms_lv(spasmData);
                }

                if (type == Type.INTELLIGENT) {
                    serialBean.setSpeed_lv(rotateSpeed);
                    serialBean.setSpasms_lv(spasm);
                }

                serialBean.setTime_lv(activeTime);

                btDataPro.sendBTMessage(SerialPort.Companion.getCmdCode(serialBean));


            } else {
                Toast.makeText(context, "血压仪未连接，请检查设备", Toast.LENGTH_SHORT).show();
            }
            //  isBlood = true;
        });
    }

    public void HandlerMessage() {
        if (BloodEndState == 1) {
            Toast.makeText(context, "还未测量运动后血压！", Toast.LENGTH_SHORT).show();
            return;
        }
        String txtBegin = managerBinding.activeTxtBegin.getCenterString();
        if (!txtBegin.equals("开  始")) {

            serialBean.clearData();//清空数据

            serialBean.setType(type);

            serialBean.setBlood_measure("50");

            serialBean.setBegin(false);

            if (type == Type.SUBJECT) {
                serialBean.setSpeed_lv(1);
                serialBean.setSpasms_lv(5);
            }

            if (type == Type.INTELLIGENT) {
                serialBean.setSpeed_lv(rotateSpeed);
                serialBean.setSpasms_lv(spasm);
            }

            serialBean.setTime_lv(0L);

            btDataPro.sendBTMessage(SerialPort.Companion.getCmdCode(serialBean));
            return;
        }
        if (type == Type.SUBJECT && nowTime == 0) {
            return;
        }

        String hightBlood = managerBinding.activeTxtHigh.getCenterString();
        String lowBlood = managerBinding.activeTxtLow.getCenterString();
        if (hightBlood.equals("0") && lowBlood.equals("0")) {
            DialogLoader.getInstance().showConfirmDialog(
                    context,
                    getString(R.string.active_blood),
                    getString(R.string.lab_ok),
                    (dialog, which) -> {
                        dialog.dismiss();
                        serialBean.setType(type);

                        serialBean.setZuli(resistance);

                        serialBean.setBlood_measure("50");

                        serialBean.setBegin(true);

                        if (type == Type.SUBJECT) {

                            serialBean.setSpeed_lv(rotateSpeed);

                            serialBean.setSpasms_lv(spasmData);
                        }

                        if (type == Type.INTELLIGENT) {

                            serialBean.setSpeed_lv(rotateSpeed);

                            serialBean.setSpasms_lv(spasm);
                        }

                        serialBean.setTime_lv(activeTime);

                        btDataPro.sendBTMessage(SerialPort.Companion.getCmdCode(serialBean));

                        timeCountTool.startCount();

                        managerBinding.activeTxtBegin.setCenterString("停  止");

                        managerBinding.activeImgBegin.setBackground(ContextCompat.getDrawable(this, R.drawable.stop));
                    },
                    getString(R.string.lab_cancel),
                    (dialog, which) -> dialog.dismiss()
            );

        } else {
            serialBean.setType(type);

            serialBean.setZuli(resistance);

            serialBean.setBlood_measure("50");

            serialBean.setBegin(true);

            if (type == Type.SUBJECT) {
                serialBean.setSpeed_lv(rotateSpeed);
                serialBean.setSpasms_lv(spasmData);
            }

            if (type == Type.INTELLIGENT) {
                serialBean.setSpeed_lv(rotateSpeed);
                serialBean.setSpasms_lv(spasm);
            }

            serialBean.setTime_lv(activeTime);

            btDataPro.sendBTMessage(SerialPort.Companion.getCmdCode(serialBean));

            timeCountTool.startCount();

            managerBinding.activeTxtBegin.setCenterString("停  止");

            managerBinding.activeImgBegin.setBackground(ContextCompat.getDrawable(this, R.drawable.stop));
        }
        if (type == Type.SUBJECT) {
            managerBinding.passiveTimeJia.setEnabled(false);
            managerBinding.passiveTimeJian.setEnabled(false);

            managerBinding.passiveTimeJia.setVisibility(View.INVISIBLE);
            managerBinding.passiveTimeJian.setVisibility(View.INVISIBLE);
            managerBinding.activeImgBegin.setBackground(ContextCompat.getDrawable(this, R.drawable.stop));
        }
    }


    public void ChangeDialog(Type type) {
        this.type = type;
        if (isBegin) {
            Toast.makeText(context, "运动中，请勿切换模式！", Toast.LENGTH_SHORT).show();
            return;
        }
//        if (BloodEndState == 1 && type == Type.ACTIVE) {
//            Toast.makeText(context, "还未测量运动后血压！", Toast.LENGTH_SHORT).show();
//            return;
//        }
        DialogLoader.getInstance().showConfirmDialog(
                context,
                getString(R.string.active_change),
                getString(R.string.lab_yes),
                (dialog, which) -> {
                    dialog.dismiss();
                    initViewType();
                },
                getString(R.string.lab_no),
                (dialog, which) -> dialog.dismiss()
        );
    }

    public void DataDisplay(int mark, String ObjectJson) {
        switch (mark) {
            case 1:
                uploadData = gson.fromJson(ObjectJson, UploadData.class);
                if (uploadData.getBlood_oxy().equals("已连接")) {
                    if (uploadData.getOxy_vaulestr().equals("手指未插入")
                            || uploadData.getOxy_vaulestr().equals("探头脱落")
                            || uploadData.getOxy_vaulestr().equals("127")) {
                        managerBinding.activeTxtBoxygen.setCenterString("--");
                        if (uploadData.getOxy_vaulestr().equals("127")) {
                            managerBinding.activeTxtO2State.setCenterString("检测中..");
                        } else {
                            managerBinding.activeTxtO2State.setCenterString(uploadData.getOxy_vaulestr());
                        }
                    } else {
                        managerBinding.activeTxtBoxygen.setCenterString(uploadData.getOxy_vaulestr());
                        managerBinding.activeTxtO2State.setCenterString("");
                    }
                } else {
                    managerBinding.activeTxtBoxygen.setCenterString("--");
                    managerBinding.activeTxtO2State.setCenterString(uploadData.getBlood_oxy());
                }
                if (uploadData.getBlood().equals("已连接")) {
                    if (uploadData.getHigh().equals("255") || uploadData.getLow().equals("255")) {
                        managerBinding.activeTxtHigh.setCenterString("0");
                        managerBinding.activeTxtLow.setCenterString("0");
                        managerBinding.activeTxtBloodstate1.setCenterString("测量错误");
                        managerBinding.activeTxtBloodstate2.setCenterString("测量错误");
                    } else {
                        if (BloodEndState == 1) {
                            //运动后血压
                            Active_L_Diastole_Shrink = uploadData.getLow() + "/" + uploadData.getHigh();
                            if (!Active_B_Diastole_Shrink.equals(Active_L_Diastole_Shrink)) {
                                BloodEndState = 2;
                                Toast.makeText(context, "运动后血压测量已完成！", Toast.LENGTH_SHORT).show();
                            }

                        } else if (BloodEndState == 0) {
                            //运动前血压
                            Active_B_Diastole_Shrink = uploadData.getLow() + "/" + uploadData.getHigh();
                        }

                        managerBinding.activeTxtHigh.setCenterString(uploadData.getHigh());
                        managerBinding.activeTxtLow.setCenterString(uploadData.getLow());
                        if (!uploadData.getHigh().equals("0")) {
                            LocalConfig.BloodHight = uploadData.getHigh();
                            LocalConfig.BloodLow = uploadData.getLow();
                        }
                        managerBinding.activeTxtBloodstate1.setCenterString("");
                        managerBinding.activeTxtBloodstate2.setCenterString("");
                    }
                } else {
                    managerBinding.activeTxtHigh.setCenterString(LocalConfig.BloodHight);
                    managerBinding.activeTxtLow.setCenterString(LocalConfig.BloodLow);
                    managerBinding.activeTxtBloodstate1.setCenterString(uploadData.getBlood());
                    managerBinding.activeTxtBloodstate2.setCenterString(uploadData.getBlood());
                    Active_B_Diastole_Shrink = "0/0";
                    Active_L_Diastole_Shrink = "0/0";
                }

                if (uploadData.getECG().equals("已连接")) {
                    managerBinding.activeTxtEcgstate.setCenterString("");
                } else {
                    managerBinding.activeTxtCoory.setCenterString("--");
                    managerBinding.activeTxtEcgstate.setCenterString(uploadData.getECG());
                }
                if (isBegin) {
                    managerBinding.progressViewZhuansuActicve.setGraduatedEnabled(true);
                    managerBinding.activeTxtZhuansu.setCenterString(uploadData.getSpeed());

                    managerBinding.progressViewLeft.setGraduatedEnabled(true);
                    managerBinding.activeTxtLeft.setCenterString(uploadData.getLeft());

                    managerBinding.progressViewRight.setGraduatedEnabled(true);
                    managerBinding.activeTxtRight.setCenterString(uploadData.getRight());

                    managerBinding.progressViewResistance.setGraduatedEnabled(true);
                    managerBinding.activeTxtResistance.setCenterString(uploadData.getSTresistance());
                }
                if (uploadData.getActiveState().equals("停机状态")) {
                    managerBinding.activeTxtBegin.setCenterString("开  始");
                    managerBinding.activeImgBegin.setBackground(ContextCompat.getDrawable(this, R.drawable.begin));
                    //判断是否在运动中
                    if (isBegin) {
                        isBegin = false;
                        stop();
                        timeCount = timeCountTool.stopCount();
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
                                    if (uploadData != null && uploadData.getBlood().equals("已连接")) {

                                        serialBean.setType(type);

                                        serialBean.setZuli(resistance);

                                        serialBean.setBlood_measure("53");

                                        serialBean.setBegin(false);

                                        if (type == Type.SUBJECT) {
                                            serialBean.setSpeed_lv(rotateSpeed);
                                            serialBean.setSpasms_lv(spasmData);
                                        }

                                        if (type == Type.INTELLIGENT) {
                                            serialBean.setSpeed_lv(rotateSpeed);
                                            serialBean.setSpasms_lv(spasm);
                                        }

                                        serialBean.setTime_lv(activeTime);

                                        if (controlState.equals("00") || controlState.equals("52")) {
                                            serialBean.setBlood_measure("51");

                                        } else if (controlState.equals("51")) {
                                            serialBean.setBlood_measure("52");
                                            managerBinding.activeTxtBlood.setCenterString("点击开始测量血压");
                                        }

                                        btDataPro.sendBTMessage(SerialPort.Companion.getCmdCode(serialBean));
                                    } else {
                                        Toast.makeText(context, "血压仪未连接，请检查设备", Toast.LENGTH_SHORT).show();
                                    }
                                },
                                getString(R.string.lab_no),
                                (dialog, which) -> {
                                    dialog.dismiss();
                                    if (type == Type.ACTIVE) {
                                        BloodEndState = 2;
                                    }
                                    if (type == Type.SUBJECT) {
                                        if (spasmCount == 5) {
                                            BloodEndState = 2;
                                        }
                                    }
                                }
                        );
                    }

                } else if (uploadData.getActiveState().equals("运行状态")) {
                    isBegin = true;
                    timeCountTool.startCount();
                    managerBinding.activeTxtBegin.setCenterString("停  止");
                    managerBinding.activeImgBegin.setBackground(ContextCompat.getDrawable(this, R.drawable.stop));
                    if (type == Type.ACTIVE) {//下面是被动操作的所以这里直接弹出方法了
                        return;
                    }
                    isBegin = true;
                    managerBinding.passiveTimeJia.setEnabled(false);
                    managerBinding.passiveTimeJian.setEnabled(false);

                    managerBinding.passiveTimeJia.setVisibility(View.INVISIBLE);
                    managerBinding.passiveTimeJian.setVisibility(View.INVISIBLE);
                    if (downTimer == null) {
                        initCountDownTimer(nowTime);
                        activeTime = MyTimeUtils.Getminute(nowTime);
                    }
                }

                if (type == Type.SUBJECT) {//被动模式的获取痉挛状态
                    if (uploadData.getSpasmState() != -1) {
                        if (uploadData.getSpasmState() > 0) {
                            spasmCount = uploadData.getSpasmState();
                        }
                    }
                }

                if (BloodEndState == 2) {
                    BloodEndState = 0;
                    SaveRecord();
                    btDataPro.sendBTMessage(btDataPro.CONNECT_CLOSE);

                    Intent in = new Intent(context, DataResultsActivity.class);
                    startActivity(in);
                    finish();
                }
                break;

            case 2:
                try {
                    if (type == Type.SUBJECT) {
                        EcgListData = new ArrayList<>();
                    }
                    ecgData = gson.fromJson(ObjectJson, EcgData.class);
                    managerBinding.activeTxtCoory.setCenterString(ecgData.getHeartrate());
                    EcgListData = ecgData.getEcgCoorY();
                    if (EcgListData == null) {
                        managerBinding.activeTxtEcgstate.setCenterString("心电仪佩戴异常！");
                        OftenListData = new ArrayList<>();
                        return;
                    } else {
                        managerBinding.activeTxtEcgstate.setCenterString("");
                        OftenListData.addAll(EcgListData);
                        EcgListData = null;
                    }
                } catch (Exception e) {
                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                break;

            case 3:
                controlState = ObjectJson;
                break;
        }
    }

    public void initCountDownTimer(long millisInFuture) {
        downTimer = new PeterTimeCountRefresh(millisInFuture, 1000);
        downTimer.setOnTimerProgressListener(timeLong -> {
            nowTime = timeLong;
            String text = MyTimeUtils.formatTime(timeLong);
            managerBinding.passiveTxtDowntimer.setCenterString(text);
        });

        //时间结束回调
        downTimer.setOnTimerFinishListener(() -> {
            managerBinding.passiveTxtDowntimer.setCenterString("00:00:00");
            nowTime = 300000;
            stop();
            managerBinding.activeTxtBegin.setCenterString("开  始");
            managerBinding.passiveTimeJia.setEnabled(true);
            managerBinding.passiveTimeJian.setEnabled(true);
            managerBinding.passiveTimeJia.setVisibility(View.VISIBLE);
            managerBinding.passiveTimeJian.setVisibility(View.VISIBLE);
        });
        downTimer.start();
    }

    /**
     * 模拟源源不断的数据源
     */
    public void updatePerogress() {
        try {
            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
            String sim = dateFormat.format(date);

            RecordDetailed recordDetailed = new RecordDetailed();
            recordDetailed.setRecordID(LocalConfig.UserID);
            if (type == Type.ACTIVE) {
                recordDetailed.setActivtType("主动模式");
            }
            if (type == Type.SUBJECT) {
                recordDetailed.setActivtType("被动模式");
            }
            if (type == Type.INTELLIGENT) {
                recordDetailed.setActivtType("智能模式");
            }
            recordDetailed.setRecordTime(sim);
            int zhuansu = Integer.parseInt(managerBinding.activeTxtZhuansu.getCenterString());
            int leftlimb = Integer.parseInt(managerBinding.activeTxtLeft.getCenterString());
            int rightlimb = Integer.parseInt(managerBinding.activeTxtRight.getCenterString());
            int resistance = Integer.parseInt(managerBinding.activeTxtResistance.getCenterString());
            if (type == Type.SUBJECT) {//被动模式
                leftlimb = 0;
                rightlimb = 0;
                resistance = 0;
            }
            recordDetailed.setSpeed(zhuansu);
            recordDetailed.setLeftLimb(leftlimb);
            recordDetailed.setRightLimb(rightlimb);
            recordDetailed.setResistance(resistance);

            int heartRate;
            if (managerBinding.activeTxtCoory.getCenterString().equals("--")) {
                heartRate = 0;
            } else {
                heartRate = Integer.parseInt(managerBinding.activeTxtCoory.getCenterString());
            }
            recordDetailed.setHeartRate(heartRate);

            int Hbo2;
            if (managerBinding.activeTxtBoxygen.getCenterString().equals("--")) {
                Hbo2 = 0;
            } else {
                Hbo2 = Integer.parseInt(managerBinding.activeTxtBoxygen.getCenterString());
            }
            recordDetailed.setHbo2(Hbo2);

            if (type == Type.ACTIVE) {
                spasmData = 0;
            }

            recordDetailed.setSpasm(spasmData);

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
        managerBinding.activeWaveShowView.resetCanavas();

        if (type == Type.SUBJECT) {
            OftenListData = new ArrayList<>();
            managerBinding.activeTxtBegin.setCenterString("开  始");
            managerBinding.passiveTimeJia.setEnabled(true);
            managerBinding.passiveTimeJian.setEnabled(true);
            managerBinding.passiveTimeJia.setVisibility(View.VISIBLE);
            managerBinding.passiveTimeJian.setVisibility(View.VISIBLE);
            nowTime = 300000;
            managerBinding.passiveTxtDowntimer.setCenterString(MyTimeUtils.formatTime(nowTime));

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
        DialogLoader.getInstance().showConfirmDialog(
                context,
                getString(R.string.active_return),
                getString(R.string.lab_yes),
                (dialog, which) -> {
                    dialog.dismiss();
                    if (isBegin) {
                        serialBean.clearData();//恢复默认值

                        serialBean.setType(type);

                        serialBean.setBlood_measure("50");

                        serialBean.setBegin(false);

                        if (type == Type.SUBJECT) {
                            serialBean.setSpeed_lv(1);
                            serialBean.setSpasms_lv(5);
                        }

                        if (type == Type.INTELLIGENT) {
                            serialBean.setSpeed_lv(rotateSpeed);
                            serialBean.setSpasms_lv(spasm);
                        }

                        serialBean.setTime_lv(activeTime);

                        btDataPro.sendBTMessage(SerialPort.Companion.getCmdCode(serialBean));

                    } else {
                        Intent in = new Intent(context, AdminMainActivity.class);
                        startActivity(in);
                        finish();
                    }
                },
                getString(R.string.lab_no),
                (dialog, which) -> dialog.dismiss()
        );
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
        LocalConfig.BloodHight = "0";
        LocalConfig.BloodLow = "0";
        TimeCountTool.setClean();
        if (type == Type.INTELLIGENT) {
            ActiveTimeTool.setClean();
        }
    }

    @NonNull
    @Override
    protected ActivityActiviteXBinding getViewBinding() {
        return ActivityActiviteXBinding.inflate(getLayoutInflater());
    }
}
