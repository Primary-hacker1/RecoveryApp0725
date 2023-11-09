package com.rick.recoveryapp.ui;

import static com.xuexiang.xupdate.entity.UpdateError.ERROR.CHECK_NO_NEW_VERSION;

import android.annotation.SuppressLint;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.TextView;

import com.common.network.LogUtils;
import com.jeremyliao.liveeventbus.LiveEventBus;
import com.rick.recoveryapp.ui.activity.helper.Constants;
import com.rick.recoveryapp.ui.activity.helper.UriConfig;
import com.rick.recoveryapp.ui.service.BluetoothChatService;
import com.rick.recoveryapp.ui.service.BtReceiver;
import com.rick.recoveryapp.entity.LiveMessage;
import com.rick.recoveryapp.greendao.DaoMaster;
import com.rick.recoveryapp.greendao.DaoSession;
import com.rick.recoveryapp.greendao.GreenDaoContext;
import com.rick.recoveryapp.greendao.GreenDaoUpgradeHelper;
import com.rick.recoveryapp.greendao.MacDrDao;
import com.rick.recoveryapp.http.OKHttpUpdateHttpService;
import com.rick.recoveryapp.ui.service.BtDataPro;
import com.rick.recoveryapp.ui.activity.serial.AddressBean;
import com.rick.recoveryapp.ui.activity.serial.SharedPreferencesUtils;
import com.rick.recoveryapp.utils.LiveDataBus;
import com.rick.recoveryapp.utils.LocalConfig;
import com.umeng.commonsdk.UMConfigure;
import com.xuexiang.xhttp2.XHttpSDK;
import com.xuexiang.xui.XUI;
import com.xuexiang.xupdate.XUpdate;
import com.xuexiang.xupdate.proxy.IFileEncryptor;
import com.xuexiang.xupdate.utils.UpdateUtils;
import com.xuexiang.xutil.tip.ToastUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Created by Administrator on 2017/4/5.
 */

public class BaseApplication extends Application implements BtReceiver.Listener {

    private static final String tag = BaseApplication.class.getName();
    private static DaoSession daoSession;
    private static Context context;
    // 来自BluetoothChatService Handler的消息类型
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    static TextView mConversationView;
    public static BtReceiver mBtReceiver;

    // 来自BluetoothChatService Handler的关键名
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    // Intent请求代码
    public static LiveMessage liveMessage = null;

    public static BluetoothChatService mConnectService = null;
    // 已连接设备的名字
    public static String mConnectedDeviceName = "";
    public static BluetoothAdapter mBluetoothAdapter = null;

    static BtDataPro btDataPro;
    static ArrayList<String> FirstList = new ArrayList<>();
    static ArrayList<String> SecondList = new ArrayList<>();
    ArrayList<String> mPermissionList = new ArrayList<>();
    static StringBuffer dataStr = new StringBuffer();
    static MacDrDao macDrDao;

    @Override
    public void onCreate() {
        super.onCreate();

        context = getApplicationContext();

        btDataPro = new BtDataPro();

        initUpdate();

        setDatabase(new GreenDaoContext());

        //友盟SDK
        UMConfigure.preInit(this, "61b6ed7ae014255fcbaf7e77", "umeng");
        //添加注释
        UMConfigure.init(this, "61b6ed7ae014255fcbaf7e77", "umeng", UMConfigure.DEVICE_TYPE_PHONE, "");
        XUI.init(this); //初始化UI框架
        XUI.debug(true);  //开启UI框架调试日志

        LiveEventBus
                .config()
                .lifecycleObserverAlwaysActive(true);
        //蓝牙初始化
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBtReceiver = new BtReceiver(context, this);//注册蓝牙广播
        mConversationView = new TextView(context);
    }

    public static Context getContext() {
        return context;
    }

    private void initUpdate() {
        XUpdate.get()
                .debug(true)
                //默认设置只在wifi下检查版本更新
                .isWifiOnly(false)
                //默认设置使用get请求检查版本
                .isGet(true)
                //默认设置非自动模式，可根据具体使用配置
                .isAutoMode(false)
                //设置默认公共请求参数
                .param("versionCode", UpdateUtils.getVersionCode(this))
                .param("appKey", getPackageName())
                //设置版本更新出错的监听
                .setOnUpdateFailureListener(error -> {
                    error.printStackTrace();
                    //对不同错误进行处理
                    if (error.getCode() != CHECK_NO_NEW_VERSION) {
                        ToastUtils.toast(error.toString());
                    }
                })
                //设置是否支持静默，默认是true
                .supportSilentInstall(false)
                //这个必须设置！实现网络请求功能。
                .setIUpdateHttpService(new OKHttpUpdateHttpService())
                .setIFileEncryptor(new IFileEncryptor() {
                    @Override
                    public String encryptFile(File file) {
                        return null;
                    }

                    @Override
                    public boolean isFileValid(String encrypt, File file) {
                        return true;
                    }
                })
                //这个必须初始化
                .init(this);

        XHttpSDK.init(this);   //初始化网络请求框架，必须首先执行
        //XHttpSDK.setBaseUrl("http://124.221.169.68:8088");
        XHttpSDK.setBaseUrl(UriConfig.BASE_URL);
    }

    /**
     * greendao数据库初始化
     */
    private void initDreenDao(Context context) {
        DaoMaster.DevOpenHelper devOpenHelper = new DaoMaster.DevOpenHelper(context, "GreenData.db");
        SQLiteDatabase db = devOpenHelper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
    }

    /**
     * 蓝牙设备初始化
     */
    @SuppressLint("MissingPermission")
    private void initBt() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!mBluetoothAdapter.isEnabled()) {
            //直接开启蓝牙
            mBluetoothAdapter.enable();
        }
        if (mBluetoothAdapter == null) {
            return;
        }
    }

    /**
     * 配置greenDao
     */
    private void setDatabase(Context context) {

        // 通过DaoMaster 的内部类 DevOpenHelper，你可以得到一个便利的SQLiteOpenHelper 对象。
        // 可能你已经注意到了，你并不需要去编写「CREATE TABLE」这样的 SQL 语句，因为greenDAO。
        // 注意：默认的DaoMaster.DevOpenHelper 会在数据库升级时，删除所有的表，意味着这将导致数据的丢失。
        // 所以，在正式的项目中，你还应该做一层封装，来实现数据库的安全升级。
        GreenDaoUpgradeHelper mHelper = new GreenDaoUpgradeHelper(context, "RecoveryApp_DB", null);
        SQLiteDatabase db = mHelper.getWritableDatabase();
        // 注意：该数据库连接属于DaoMaster，所以多个 Session 指的是相同的数据库连接。
        DaoMaster daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
//        mDaoMaster = new DaoMaster(db);
//        mDaoSession = mDaoMaster.newSession();
    }

    /**
     * @return 获取 DaoSession
     */
    public DaoSession getDaoSession() {
        return daoSession;
    }

    // 该Handler从BluetoothChatService中获取信息
    private static final Handler mHandler = new Handler(Objects.requireNonNull(Looper.myLooper())) {
        @SuppressLint("HandlerLeak")
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            LocalConfig.isControl = true;
                            liveMessage = new LiveMessage();
                            liveMessage.setIsConnt(true);
                            liveMessage.setMessage("已连接到 " + mConnectedDeviceName);
                            liveMessage.setState(mConnectedDeviceName);
                            LiveDataBus.get().with(Constants.BT_CONNECTED).postValue(liveMessage);
                            LogUtils.e(tag + "已连接到 " + mConnectedDeviceName);
                            break;

                        case BluetoothChatService.STATE_CONNECTING:
                            liveMessage = new LiveMessage();
                            liveMessage.setIsConnt(false);
                            liveMessage.setMessage("");//正在连接。。。
                            liveMessage.setState("");
                            LiveDataBus.get().with(Constants.BT_CONNECTED).postValue(liveMessage);
                            LogUtils.e(tag + "正在连接。。。 ");
                            break;

                        case BluetoothChatService.STATE_LISTEN:
                            mConnectedDeviceName = "";
                            break;

                        case BluetoothChatService.STATE_NONE:
                            break;
                    }
                    break;

                case MESSAGE_WRITE:

                    break;

                case MESSAGE_READ:
                    dataStr.setLength(0);
                    Bundle data = msg.getData();
                    dataStr.append(data.getString("BTdata"));
                    String endStr = "";
                    String[] split = dataStr.toString().split(" ");
                    for (int i = 0; i < split.length; i++) {
                        endStr = split[i];
                        if (endStr.equals("A8")) {
                            if (i == 0 && split[i + 1].equals("82")) {
                                FirstList = new ArrayList<String>();
                                SecondList = new ArrayList<String>();
                                FirstList.add(endStr);
                            } else if (i + 1 == split.length && split[i - 1].equals("0A")) {
                                FirstList = new ArrayList<String>();
                                SecondList = new ArrayList<String>();
                                FirstList.add(endStr);
                            } else {
                                FirstList.add(endStr);
                            }
                        } else {
                            FirstList.add(endStr);
                        }
                    }
                    if (FirstList.size() > 4) {
                        //解析“数据长度”
                        int Datalength = btDataPro.covert16to10(FirstList.get(3));
                        //判断（包头,功能码，包序号，数据长度）+数据实体=总数据长度-数据包尾长度
                        if (FirstList.size() == Datalength + 8) {
                            //前22个数据为非心电数据，第22个开始才是心电数据
                            for (int i = 0; i < 22; i++) {
                                if (!FirstList.isEmpty()) {
                                    SecondList.add(FirstList.get(0));
                                    FirstList.remove(0);
                                } else {
                                    return;
                                }
                            }
                            btDataPro.Processing(SecondList, "82");//其他一般数据
                            btDataPro.Processing(FirstList, "83");//心电数据
                        }
                    }
                    break;

                case MESSAGE_DEVICE_NAME:
                    // 保存已连接设备的名称
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    break;

                case MESSAGE_TOAST:
                    liveMessage = new LiveMessage();
                    liveMessage.setIsConnt(false);
                    liveMessage.setState("蓝牙设备未连接");
                    if (Objects.equals(mConnectedDeviceName, "")) {
                        liveMessage.setMessage(msg.getData().getString(TOAST));
                    }
                    if (mConnectService != null) {
                        mConnectService.stop();
                    }
                    LocalConfig.isControl = false;
                    mConnectedDeviceName = "";
                    LiveDataBus.get().with(Constants.BT_CONNECTED).postValue(liveMessage);
                    break;
            }
        }
    };

    //自动连接
    @SuppressLint("MissingPermission")
    public static void AutoConnect() {

        mConnectService = new BluetoothChatService(mHandler);

//        try {

        AddressBean addressBean = SharedPreferencesUtils.Companion.getInstance().getAddressString();

        if (addressBean == null) {
            LogUtils.e(tag + addressBean + "--" + "地址获取失败！");
            return;
        }

        String address = addressBean.getMacAddress();

        if (address == null) {
            LogUtils.e(tag + addressBean + "--" + "地址获取失败！");
            return;
        }

        if (address.isEmpty()) {
            LogUtils.e(tag + addressBean + "--" + "地址获取失败！");
            return;
        }

        address = deleteCharString(address);

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

        String target_device_name = device.getName();

        if (target_device_name.equals(mConnectedDeviceName)) {
            return;
        }

        mConnectService.connect(device);

//        if (target_device_name == null) {
//            mConnectService.connect(device);
//        } else {
//            if (mConnectedDeviceName == null) {
//                return;
//            }
//            if (target_device_name.equals(mConnectedDeviceName)) {
//                return;
//            } else {
//                mConnectService.connect(device);
//            }
//        }
    }

    public static String deleteCharString(String sourceString) {
        StringBuilder deleteString = new StringBuilder();
        for (int i = 0; i < sourceString.length(); i++) {
            if (i < 11 && i % 2 == 1) {
                deleteString.append(sourceString.charAt(i)).append(":");
            } else {
                deleteString.append(sourceString.charAt(i));
            }
        }
        return deleteString.toString();
    }

    @Override
    public void foundDev(BluetoothDevice dev) {

    }

    @Override
    public void foundBT() {

    }

}
