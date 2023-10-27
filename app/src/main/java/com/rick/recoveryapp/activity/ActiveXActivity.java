package com.rick.recoveryapp.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
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
import com.jeremyliao.liveeventbus.LiveEventBus;
import com.rick.recoveryapp.R;
import com.rick.recoveryapp.activity.serial.SerialBean;
import com.rick.recoveryapp.activity.serial.SerialPort;
import com.rick.recoveryapp.activity.serial.SerialPort.Type;
import com.rick.recoveryapp.base.BaseApplication;
import com.rick.recoveryapp.bluetooth.BtDataPro;
import com.rick.recoveryapp.chart.MyAVG;
import com.rick.recoveryapp.databinding.ActivityActiviteXBinding;
import com.rick.recoveryapp.entity.EcgData;
import com.rick.recoveryapp.entity.LiveMessage;
import com.rick.recoveryapp.entity.protocol.PoolMessage;
import com.rick.recoveryapp.entity.protocol.UploadData;
import com.rick.recoveryapp.greendao.ActivitRecordDao;
import com.rick.recoveryapp.greendao.EcgDataDBDao;
import com.rick.recoveryapp.greendao.RecordDetailedDao;
import com.rick.recoveryapp.greendao.entity.ActivitRecord;
import com.rick.recoveryapp.greendao.entity.RecordDetailed;
import com.rick.recoveryapp.utils.ActiveTimeTool;
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
    ActivityActiviteXBinding binding;
    int resiDta = 1;
    ArrayList<Float> EcgListData;
    static ArrayList<Float> OftenListData;
    RecordDetailedDao recordDetailedDao;
    EcgDataDBDao ecgDataDBDao;
    UploadData uploadData;
    EcgData ecgData;
    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    String controlState = "52";
    BtDataPro btDataPro;
    private Timer timer1;
    private TimerTask timerTask1;
    boolean isBegin = false;
    Context context;
    ActivitRecordDao activitRecordDao;
    public TimeCountTool timeCountTool = TimeCountTool.getInstance();
    String timeCount = "";
    double Total_mileage, Calories;
    String Active_B_Diastole_Shrink = "0/0", Active_L_Diastole_Shrink = "0/0";//主动模式被动模式都有
    int BloodEndState = 0; // 0:初始状态  1：需要测量血压   2：血压测量完成

    //被动模式
    ArrayList<Integer> countList;

    static PeterTimeCountRefresh downTimer;//向下定时器

    static long nowTime = 300000;//现在时间

    int zhuansu = 5;
    int resistance = 1;//智能模式阻力
    int spasmData = 1;//运动方向

    Long activeTime = 0L; //活动时间

    boolean isOk = false; //是否确认

    int spasmCount = 0; //

    //智能模式
    int spasm = 1;//痉挛等级

    //智能模式
    public ActiveTimeTool activeTimeTool = ActiveTimeTool.getInstance();


    //通用改革
    SerialBean serialBean = new SerialBean();

    Type type;

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
        initData();//初始化数据
        initViewType();//不同模式不同界面
        initClick();//点击操作
        try {
            initLiveData();

            serialBean.setType(type);

            serialBean.setZuli(1);

            serialBean.setBlood_measure("53");

            serialBean.setBegin(false);

            if (type == Type.SUBJECT) {
                serialBean.setSpeed_lv(5);
                serialBean.setSpasms_lv(5);
            }

            if (type == Type.INTELLIGENT) {
                serialBean.setSpeed_lv(zhuansu);
                serialBean.setSpasms_lv(spasm);
            }

            serialBean.setTime_lv(activeTime);

            btDataPro.sendBTMessage(SerialPort.Companion.getCmdCode(serialBean));

            PassEcg();
        } catch (Exception ex) {
            ex.getMessage();
        }
    }

    private void initData() {
        Intent intent = getIntent();
        type = (Type) intent.getSerializableExtra("type");//获取点击的模式
        assert type != null;
        LogUtils.d("type==" + type.name());
        binding = getBinding();
        btDataPro = new BtDataPro();
        binding.activeTxtMassage.setLeftString(" 患者姓名：" + LocalConfig.userName);
        binding.activeTxtMassage.setLeftBottomString(" 患者编号：" + LocalConfig.medicalNumber);
        BaseApplication myApp = (BaseApplication) getApplication();
        LocalConfig.daoSession = myApp.getDaoSession();
        activitRecordDao = LocalConfig.daoSession.getActivitRecordDao();
        recordDetailedDao = LocalConfig.daoSession.getRecordDetailedDao();

        EcgListData = new ArrayList<>();
        OftenListData = new ArrayList<>();
        binding.activeWaveShowView.resetCanavas();
        ecgDataDBDao = LocalConfig.daoSession.getEcgDataDBDao();
    }

    public void initViewType() {//不同模式不同界面

        serialBean.clearData();//清空数据
        zhuansu = 5;
        spasm = 1;

        if (type == Type.ACTIVE) {
            binding.activeTxtResistance.setCenterString("1");
            binding.activeTxtZhuansu.setCenterString("0");
            binding.activeTxtSpasm.setCenterString("0");

            binding.stxActiveTitle.setBottomDividerLineVisibility(View.VISIBLE);
            binding.stxPressTitle.setBottomDividerLineVisibility(View.GONE);
            binding.stxIntelligenceTitle.setBottomDividerLineVisibility(View.GONE);
            binding.llTimeControl.setVisibility(View.GONE);
            binding.llRpmActive.setVisibility(View.VISIBLE);//运动rpm布局不需要
            binding.llRpmIntelligence.setVisibility(View.GONE);//智能被动rpm布局也需要
            binding.llSpasm.setVisibility(View.INVISIBLE);//痉挛等级
            binding.llResistance.setVisibility(View.VISIBLE);//阻力等级

            binding.activeTxtZhuansu.setCenterTextColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.activeTxtResistance.setCenterTextColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.activeTxtSpasm.setCenterTextColor(ContextCompat.getColor(this, R.color.progressLightGray));
            binding.activeTxtLeft.setCenterTextColor(ContextCompat.getColor(this, R.color.Progress_bule));//左肢
            binding.activeTxtRight.setCenterTextColor(ContextCompat.getColor(this, R.color.Progress_bule));//右肢
            binding.stxLeftLimb.setCenterTextColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.stxRightLimb.setCenterTextColor(ContextCompat.getColor(this, R.color.Progress_bule));

            binding.progressViewZhuansuActicve.setEndColor(ContextCompat.getColor(this, R.color.white));
            binding.progressViewZhuansuActicve.setProgressTextColor(ContextCompat.getColor(this, R.color.white));
            binding.progressViewZhuansuActicve.setStartColor(ContextCompat.getColor(this, R.color.white));
            binding.progressViewZhuansuActicve.setTrackColor(ContextCompat.getColor(this, R.color.progressLightBlue));

            binding.progressViewResistance.setEndColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewResistance.setProgressTextColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewResistance.setStartColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewResistance.setTrackColor(ContextCompat.getColor(this, R.color.progressLightBlue));

            binding.progressViewSpasm.setEndColor(ContextCompat.getColor(this, R.color.white));
            binding.progressViewSpasm.setProgressTextColor(ContextCompat.getColor(this, R.color.white));
            binding.progressViewSpasm.setStartColor(ContextCompat.getColor(this, R.color.white));
            binding.progressViewSpasm.setTrackColor(ContextCompat.getColor(this, R.color.progressLightGray));

            binding.progressViewLeft.setEndColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewLeft.setProgressTextColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewLeft.setStartColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewLeft.setTrackColor(ContextCompat.getColor(this, R.color.progressLightBlue));

            binding.progressViewRight.setEndColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewRight.setProgressTextColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewRight.setStartColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewRight.setTrackColor(ContextCompat.getColor(this, R.color.progressLightBlue));
        }

        if (type == Type.SUBJECT) {
            binding.activeWaveShowView.resetCanavas();
            binding.activeTxtZhuansu.setCenterString("5");
            binding.activeTxtSpasm.setCenterString("1");

            binding.stxActiveTitle.setBottomDividerLineVisibility(View.GONE);
            binding.stxPressTitle.setBottomDividerLineVisibility(View.VISIBLE);
            binding.stxIntelligenceTitle.setBottomDividerLineVisibility(View.GONE);
            binding.llTimeControl.setVisibility(View.VISIBLE);
            binding.llRpmActive.setVisibility(View.GONE);
            binding.llRpmIntelligence.setVisibility(View.VISIBLE);
            binding.llSpasm.setVisibility(View.VISIBLE);//痉挛等级
            binding.llResistance.setVisibility(View.INVISIBLE);

            nowTime = 300000;
            activeTime = MyTimeUtils.Getminute(nowTime);
            String text1 = MyTimeUtils.formatTime(nowTime);
            binding.passiveTxtDowntimer.setCenterString(text1);
            countList = new ArrayList<>();

            binding.activeTxtZhuansu.setCenterTextColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.activeTxtResistance.setCenterTextColor(ContextCompat.getColor(this, R.color.white));
            binding.activeTxtSpasm.setCenterTextColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.activeTxtLeft.setCenterTextColor(ContextCompat.getColor(this, R.color.white));//左肢
            binding.activeTxtRight.setCenterTextColor(ContextCompat.getColor(this, R.color.white));//右肢
            binding.stxLeftLimb.setCenterTextColor(ContextCompat.getColor(this, R.color.white));
            binding.stxRightLimb.setCenterTextColor(ContextCompat.getColor(this, R.color.white));

            binding.progressViewZhuansuActicve.setEndColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewZhuansuActicve.setProgressTextColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewZhuansuActicve.setStartColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewZhuansuActicve.setTrackColor(ContextCompat.getColor(this, R.color.progressLightBlue));

            binding.progressViewResistance.setEndColor(ContextCompat.getColor(this, R.color.white));
            binding.progressViewResistance.setProgressTextColor(ContextCompat.getColor(this, R.color.white));
            binding.progressViewResistance.setStartColor(ContextCompat.getColor(this, R.color.white));
            binding.progressViewResistance.setTrackColor(ContextCompat.getColor(this, R.color.progressLightGray));

            binding.progressViewSpasm.setEndColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewSpasm.setProgressTextColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewSpasm.setStartColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewSpasm.setTrackColor(ContextCompat.getColor(this, R.color.progressLightBlue));

            binding.progressViewLeft.setEndColor(ContextCompat.getColor(this, R.color.progressLightGray));
            binding.progressViewLeft.setProgressTextColor(ContextCompat.getColor(this, R.color.progressLightGray));
            binding.progressViewLeft.setStartColor(ContextCompat.getColor(this, R.color.progressLightGray));
            binding.progressViewLeft.setTrackColor(ContextCompat.getColor(this, R.color.progressLightGray));

            binding.progressViewRight.setEndColor(ContextCompat.getColor(this, R.color.progressLightGray));
            binding.progressViewRight.setProgressTextColor(ContextCompat.getColor(this, R.color.progressLightGray));
            binding.progressViewRight.setStartColor(ContextCompat.getColor(this, R.color.progressLightGray));
            binding.progressViewRight.setTrackColor(ContextCompat.getColor(this, R.color.progressLightGray));
        }

        if (type == Type.INTELLIGENT) {
            binding.activeTxtResistance.setCenterString(resistance + "");
            binding.activeTxtZhuansu.setCenterString(zhuansu + "");
            binding.activeTxtSpasm.setCenterString(spasm + "");

            binding.stxActiveTitle.setBottomDividerLineVisibility(View.GONE);
            binding.stxPressTitle.setBottomDividerLineVisibility(View.GONE);
            binding.stxIntelligenceTitle.setBottomDividerLineVisibility(View.VISIBLE);
            binding.llRpmActive.setVisibility(View.GONE);
            binding.llRpmIntelligence.setVisibility(View.VISIBLE);
            binding.llSpasm.setVisibility(View.VISIBLE);//痉挛等级
            binding.llResistance.setVisibility(View.VISIBLE);

            binding.activeTxtZhuansu.setCenterTextColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.activeTxtResistance.setCenterTextColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.activeTxtSpasm.setCenterTextColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.activeTxtLeft.setCenterTextColor(ContextCompat.getColor(this, R.color.Progress_bule));//左肢
            binding.activeTxtRight.setCenterTextColor(ContextCompat.getColor(this, R.color.Progress_bule));//右肢
            binding.stxLeftLimb.setCenterTextColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.stxRightLimb.setCenterTextColor(ContextCompat.getColor(this, R.color.Progress_bule));

            binding.progressViewZhuansuActicve.setEndColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewZhuansuActicve.setProgressTextColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewZhuansuActicve.setStartColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewZhuansuActicve.setTrackColor(ContextCompat.getColor(this, R.color.progressLightBlue));

            binding.progressViewResistance.setEndColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewResistance.setProgressTextColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewResistance.setStartColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewResistance.setTrackColor(ContextCompat.getColor(this, R.color.progressLightBlue));

            binding.progressViewSpasm.setEndColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewSpasm.setProgressTextColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewSpasm.setStartColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewSpasm.setTrackColor(ContextCompat.getColor(this, R.color.progressLightBlue));

            binding.progressViewLeft.setEndColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewLeft.setProgressTextColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewLeft.setStartColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewLeft.setTrackColor(ContextCompat.getColor(this, R.color.progressLightBlue));

            binding.progressViewRight.setEndColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewRight.setProgressTextColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewRight.setStartColor(ContextCompat.getColor(this, R.color.Progress_bule));
            binding.progressViewRight.setTrackColor(ContextCompat.getColor(this, R.color.progressLightBlue));
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
                            binding.activeWaveShowView.showLine(cooY);
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
        LiveEventBus
                .get("BT_PROTOCOL", PoolMessage.class)
                .observe(this, msg -> {
                    if (msg.isState()) {
                        Log.d("BT", msg.getObjectName());
                        if (msg.getObjectName().equals(btDataPro.UPLODE_ANSWER)) {
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
                    } else {
                        Log.d("BT", "没有任何数据");
                    }
                });

        LiveEventBus
                .get("BT_CONNECTED", LiveMessage.class)
                .observe(this, msg -> {
                    if (!msg.getIsConnt()) {//未连接

                        binding.trainButEcg.setBackgroundResource(R.drawable.xindian_no);
                        binding.trainButBp.setBackgroundResource(R.drawable.xueya_no);
                        binding.trainButO2.setBackgroundResource(R.drawable.o2_no);

                        binding.activeTxtBoxygen.setCenterString("0");
                        binding.activeTxtO2State.setCenterString("血氧仪未连接");
                        binding.activeTxtBloodstate1.setCenterString("血压仪未连接");
                        binding.activeTxtBloodstate2.setCenterString("血压仪未连接");

                        if (type == Type.INTELLIGENT) {
                            binding.progressViewLeft.setGraduatedEnabled(true);
                            binding.progressViewRight.setGraduatedEnabled(true);
                            binding.activeTxtLeft.setCenterString("0");
                        }

                        if (type == Type.ACTIVE) {
                            int left = 0;
                            binding.progressViewLeft.setGraduatedEnabled(true);
                            binding.progressViewLeft.setEndProgress(Float.parseFloat(LocalConfig.GetProgress((float) left, (float) 50)));
                            binding.activeTxtLeft.setCenterString("0");

                            int right = 0;
                            binding.progressViewRight.setGraduatedEnabled(true);
                            binding.progressViewRight.setEndProgress(Float.parseFloat(LocalConfig.GetProgress((float) right, (float) 50)));
                            binding.activeTxtRight.setCenterString("0");
                        }

                        binding.activeTxtCoory.setCenterString("--");
                        binding.activeTxtEcgstate.setCenterString("心电仪未连接");
                        OftenListData.clear();


                    }
                });
        LiveEventBus
                .get("BT_PROTOCOL", PoolMessage.class)
                .observe(this,
                        msg -> {
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
        binding.btnTest.setOnClickListener(v -> {
            if (type == Type.SUBJECT) {//被动模式直接发送
                btDataPro.sendBTMessage(btDataPro.GetCmdCode(LocalConfig.ecgmac, LocalConfig.bloodmac, LocalConfig.oxygenmac));
                return;
            }

            if (type == Type.ACTIVE) {//主动模式要判断一下是否可以发送
                if (LocalConfig.isControl) {
                    btDataPro.sendBTMessage(btDataPro.GetCmdCode(LocalConfig.ecgmac, LocalConfig.bloodmac, LocalConfig.oxygenmac));
                }
            }

        });

        binding.btnClose.setOnClickListener(v -> {
            if (BloodEndState == 1) {
                BloodEndState = 2;//取消测量运动后血压
            } else if (BloodEndState == 0) {
                dialogs();
            }
        });

        //切换训练模式
        binding.stxActiveTitle.setOnClickListener(v -> ChangeDialog(Type.ACTIVE));

        binding.stxPressTitle.setOnClickListener(v -> ChangeDialog(Type.SUBJECT));

        binding.stxIntelligenceTitle.setOnClickListener(v -> ChangeDialog(Type.INTELLIGENT));


        binding.activeImgBegin.setOnClickListener(v -> HandlerMessage());

        //设置Rpm加减
        binding.sbRpmUp.setOnClickListener(v -> {
            if (type == Type.ACTIVE) {
                return;
            }
            if (zhuansu + 1 <= 60) {
                if (zhuansu + 1 >= 30) {
                    if (isOk) {
                        zhuansu = zhuansu + 1;
                        binding.progressViewZhuansuActicve.setGraduatedEnabled(true);
                        binding.activeTxtZhuansu.setCenterString(zhuansu + "");
                    } else {
                        DialogLoader.getInstance().showConfirmDialog(
                                context,
                                getString(R.string.tip_permission),
                                getString(R.string.lab_ok),
                                (dialog, which) -> {
                                    dialog.dismiss();
                                    isOk = true;
                                    zhuansu = zhuansu + 1;
                                    binding.progressViewZhuansuActicve.setGraduatedEnabled(true);
                                    binding.activeTxtZhuansu.setCenterString(zhuansu + "");
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
                    binding.progressViewZhuansuActicve.setGraduatedEnabled(true);
                    binding.activeTxtZhuansu.setCenterString(zhuansu + "");
                }
            } else {
                zhuansu = 60;
            }

        });

        binding.sbRpmDown.setOnClickListener(v -> {
            if (isBegin) {
                Toast.makeText(context, "运动中，请勿调节参数！", Toast.LENGTH_SHORT).show();
                return;
            }
            zhuansu = zhuansu - 1;
            if (zhuansu < 0) {
                zhuansu = 0;
            } else {
                binding.progressViewZhuansuActicve.setGraduatedEnabled(true);
                binding.activeTxtZhuansu.setCenterString(zhuansu + "");
            }
        });

        //被动模式右下角按钮操作
        binding.passiveTimeJia.setOnClickListener(v -> {
            nowTime = nowTime + 300000;
            String text = MyTimeUtils.formatTime(nowTime);
            if (nowTime <= 3600000) {
                binding.passiveTxtDowntimer.setCenterString(text);
            } else {
                nowTime = 3600000;
                text = MyTimeUtils.formatTime(nowTime);
                binding.passiveTxtDowntimer.setCenterString(text);
            }
            activeTime = MyTimeUtils.Getminute(nowTime);
            btDataPro.sendBTMessage(SerialPort.Companion.getCmdCode("50", false, spasmData, zhuansu, activeTime));
        });

        binding.passiveTimeJian.setOnClickListener(v -> {
            nowTime = nowTime - 300000;
            String text1 = MyTimeUtils.formatTime(nowTime);
            if (nowTime >= 300000) {
                binding.passiveTxtDowntimer.setCenterString(text1);
            } else {
                nowTime = 300000;
                text1 = MyTimeUtils.formatTime(nowTime);
                binding.passiveTxtDowntimer.setCenterString(text1);
            }
            activeTime = MyTimeUtils.Getminute(nowTime);
            btDataPro.sendBTMessage(SerialPort.Companion.getCmdCode("50", false, spasmData, zhuansu, activeTime));
        });


        //设置阻力加减
        binding.activeImbtnJia.setOnClickListener(v -> {
            if (isBegin) {
                Toast.makeText(context, "运动中，请勿调节参数！", Toast.LENGTH_SHORT).show();
                return;
            }

            if (type == Type.INTELLIGENT) {
                if (BloodEndState == 1) {
                    Toast.makeText(context, "还未测量运动后血压！", Toast.LENGTH_SHORT).show();
                } else {
                    resiDta = resiDta + 1;
                    if (resiDta <= 12) {
                        binding.progressViewResistance.setGraduatedEnabled(true);
                        binding.activeTxtResistance.setCenterString(resiDta + "");
                    } else {
                        resiDta = 12;
                    }
                }
            } else {
                resiDta = resiDta + 1;
                if (resiDta <= 12) {
                    binding.progressViewResistance.setGraduatedEnabled(true);
                    binding.activeTxtResistance.setCenterString(resiDta + "");
                } else {
                    resiDta = 12;
                }
            }
        });

        binding.activeImbtnMove.setOnClickListener(v -> {
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

        //痉挛等级
        binding.passiveSpasmJia.setOnClickListener((View.OnClickListener) v -> {
            if (isBegin) {
                Toast.makeText(context, "运动中，请勿调节参数！", Toast.LENGTH_SHORT).show();
                return;
            }
            spasm = spasm + 1;
            if (spasm <= 12) {
                binding.progressViewSpasm.setGraduatedEnabled(true);
                binding.activeTxtSpasm.setCenterString(spasm + "");
            } else {
                spasm = 12;
            }
        });

        binding.passiveSpasmJian.setOnClickListener(v -> {
            if (isBegin) {
                Toast.makeText(context, "运动中，请勿调节参数！", Toast.LENGTH_SHORT).show();
                return;
            }
            spasm = spasm - 1;
            if (spasm < 1) {
                spasm = 1;
            } else {
                binding.progressViewSpasm.setGraduatedEnabled(true);
                binding.activeTxtSpasm.setCenterString(spasm + "");
            }
        });

        //开始测量血压按钮
        binding.activeImgBlood.setOnClickListener(v -> {
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

                    binding.activeTxtBlood.setCenterString("点击开始测量血压");
                }

                serialBean.setType(type);

                serialBean.setZuli(resiDta);

                serialBean.setBegin(false);


                if (type == Type.SUBJECT) {
                    serialBean.setSpeed_lv(zhuansu);
                    serialBean.setSpasms_lv(spasmData);
                }

                if (type == Type.INTELLIGENT) {
                    serialBean.setSpeed_lv(zhuansu);
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
        String txtBegin = binding.activeTxtBegin.getCenterString();
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
                serialBean.setSpeed_lv(zhuansu);
                serialBean.setSpasms_lv(spasm);
            }

            serialBean.setTime_lv(0L);

            btDataPro.sendBTMessage(SerialPort.Companion.getCmdCode(serialBean));
            return;
        }
        if (type == Type.SUBJECT && nowTime == 0) {
            return;
        }

        String hightBlood = binding.activeTxtHigh.getCenterString();
        String lowBlood = binding.activeTxtLow.getCenterString();
        if (hightBlood.equals("0") && lowBlood.equals("0")) {
            DialogLoader.getInstance().showConfirmDialog(
                    context,
                    getString(R.string.active_blood),
                    getString(R.string.lab_ok),
                    (dialog, which) -> {
                        dialog.dismiss();
                        btDataPro.sendBTMessage(SerialPort.Companion.getCmdCode(resiDta, "50", true));
                        timeCountTool.startCount();
                        binding.activeTxtBegin.setCenterString("停  止");
                        binding.activeImgBegin.setBackground(ContextCompat.getDrawable(this, R.drawable.stop));
                    },
                    getString(R.string.lab_cancel),
                    (dialog, which) -> dialog.dismiss()
            );

        } else {
            btDataPro.sendBTMessage(SerialPort.Companion.getCmdCode(resiDta, "50", true));
            timeCountTool.startCount();
            binding.activeTxtBegin.setCenterString("停  止");
            binding.activeImgBegin.setBackground(ContextCompat.getDrawable(this, R.drawable.stop));
        }
        if (type == Type.SUBJECT) {
            binding.passiveTimeJia.setEnabled(false);
            binding.passiveTimeJian.setEnabled(false);

            binding.passiveTimeJia.setVisibility(View.INVISIBLE);
            binding.passiveTimeJian.setVisibility(View.INVISIBLE);
            binding.activeImgBegin.setBackground(ContextCompat.getDrawable(this, R.drawable.stop));
        }
    }


    public void ChangeDialog(Type type) {
        this.type = type;
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

                        binding.activeTxtHigh.setCenterString(uploadData.getHigh());
                        binding.activeTxtLow.setCenterString(uploadData.getLow());
                        if (!uploadData.getHigh().equals("0")) {
                            LocalConfig.BloodHight = uploadData.getHigh();
                            LocalConfig.BloodLow = uploadData.getLow();
                        }
                        binding.activeTxtBloodstate1.setCenterString("");
                        binding.activeTxtBloodstate2.setCenterString("");
                    }
                } else {
                    binding.activeTxtHigh.setCenterString(LocalConfig.BloodHight);
                    binding.activeTxtLow.setCenterString(LocalConfig.BloodLow);
                    binding.activeTxtBloodstate1.setCenterString(uploadData.getBlood());
                    binding.activeTxtBloodstate2.setCenterString(uploadData.getBlood());
                    Active_B_Diastole_Shrink = "0/0";
                    Active_L_Diastole_Shrink = "0/0";
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
                    binding.activeImgBegin.setBackground(ContextCompat.getDrawable(this, R.drawable.begin));
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

                                        serialBean.setZuli(resiDta);

                                        serialBean.setBlood_measure("53");

                                        serialBean.setBegin(false);

                                        if (type == Type.SUBJECT) {
                                            serialBean.setSpeed_lv(zhuansu);
                                            serialBean.setSpasms_lv(spasmData);
                                        }

                                        if (type == Type.INTELLIGENT) {
                                            serialBean.setSpeed_lv(zhuansu);
                                            serialBean.setSpasms_lv(spasm);
                                        }

                                        serialBean.setTime_lv(activeTime);

                                        if (controlState.equals("00") || controlState.equals("52")) {
                                            serialBean.setBlood_measure("51");

                                        } else if (controlState.equals("51")) {
                                            serialBean.setBlood_measure("52");
                                            binding.activeTxtBlood.setCenterString("点击开始测量血压");
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
                    binding.activeTxtBegin.setCenterString("停  止");
                    binding.activeImgBegin.setBackground(ContextCompat.getDrawable(this, R.drawable.stop));
                    if (type == Type.ACTIVE) {//下面是被动操作的所以这里直接弹出方法了
                        return;
                    }
                    isBegin = true;
                    binding.passiveTimeJia.setEnabled(false);
                    binding.passiveTimeJian.setEnabled(false);

                    binding.passiveTimeJia.setVisibility(View.INVISIBLE);
                    binding.passiveTimeJian.setVisibility(View.INVISIBLE);
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
                    binding.activeTxtCoory.setCenterString(ecgData.getHeartrate());
                    EcgListData = ecgData.getEcgCoorY();
                    if (EcgListData == null) {
                        binding.activeTxtEcgstate.setCenterString("心电仪佩戴异常！");
                        OftenListData = new ArrayList<>();
                        return;
                    } else {
                        binding.activeTxtEcgstate.setCenterString("");
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
            binding.passiveTxtDowntimer.setCenterString(text);
        });

        //时间结束回调
        downTimer.setOnTimerFinishListener(() -> {
            binding.passiveTxtDowntimer.setCenterString("00:00:00");
            nowTime = 300000;
            stop();
            binding.activeTxtBegin.setCenterString("开  始");
            binding.passiveTimeJia.setEnabled(true);
            binding.passiveTimeJian.setEnabled(true);
            binding.passiveTimeJia.setVisibility(View.VISIBLE);
            binding.passiveTimeJian.setVisibility(View.VISIBLE);
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
            int zhuansu = Integer.parseInt(binding.activeTxtZhuansu.getCenterString());
            int leftlimb = Integer.parseInt(binding.activeTxtLeft.getCenterString());
            int rightlimb = Integer.parseInt(binding.activeTxtRight.getCenterString());
            int resistance = Integer.parseInt(binding.activeTxtResistance.getCenterString());
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
        binding.activeWaveShowView.resetCanavas();

        if (type == Type.SUBJECT) {
            OftenListData = new ArrayList<>();
            binding.activeTxtBegin.setCenterString("开  始");
            binding.passiveTimeJia.setEnabled(true);
            binding.passiveTimeJian.setEnabled(true);
            binding.passiveTimeJia.setVisibility(View.VISIBLE);
            binding.passiveTimeJian.setVisibility(View.VISIBLE);
            nowTime = 300000;
            binding.passiveTxtDowntimer.setCenterString(MyTimeUtils.formatTime(nowTime));

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
                            serialBean.setSpeed_lv(zhuansu);
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
