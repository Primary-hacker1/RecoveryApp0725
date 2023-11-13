package com.example.usbprint.print;

import static com.example.usbprint.print.MLog.e;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.widget.Toast;

import com.dynamixsoftware.intentapi.IntentAPI;
import com.dynamixsoftware.printingsdk.DriverHandle;
import com.dynamixsoftware.printingsdk.DriversSearchEntry;
import com.dynamixsoftware.printingsdk.IDiscoverListener;
import com.dynamixsoftware.printingsdk.IFindDriversListener;
import com.dynamixsoftware.printingsdk.ISetupPrinterListener;
import com.dynamixsoftware.printingsdk.Printer;
import com.dynamixsoftware.printingsdk.PrintingSdk;
import com.dynamixsoftware.printingsdk.Result;
import com.dynamixsoftware.printingsdk.ResultType;
import com.tx.printlib.Const;
import com.tx.printlib.UsbPrinter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * 使用这个工具类，必须要在application的onCreate里面初始化,且只可以初始化一次,函数直接调用即可，预防内存泄漏
 * 由于目前实现打印功能是使用广播来告诉PrintHand这个软件我需要打印某文件，所以打印机连接相关的可以在PrintHand里面进行连接，而为我们要做的只是通过intentApi来告诉PrintHand需要答应的文件
 * 以下的查找打印机和连接打印机相关的函数，暂时不起作用。
 */
public class PrintUtil implements ServiceConnected {
    @SuppressLint("StaticFieldLeak")
    private static Context mContext;
    //    目前用不着使用这个
    @SuppressLint("StaticFieldLeak")
    private static PrintingSdk printingSdk;
    private static RunPrintServiceImp printServiceImp;
    @SuppressLint("StaticFieldLeak")
    private static IntentAPI intentApi;
    public static boolean printServiceConnected = false;
    private static volatile PrintUtil instance = null;
    private static RunServiceCallbackImp serviceCallbackImp;
    private static PrintCallBackImp printCallBackImp;
    /*热敏打印机连接*/
    private static UsbPrinter mUsbPrinter;
    //打印成功或失败回调接口
    private static PrintCallBack printCallBack;
    //这个路径可以用于保存因为打印机未连接导致的打印失败的路径，待打印完毕后可以清空
    public static String printFilePath;

    private PrintUtil(Context context) {
        mContext = context;
        printServiceImp = new RunPrintServiceImp(this);
        printCallBackImp = new PrintCallBackImp();
        serviceCallbackImp = new RunServiceCallbackImp();
        init();
    }

    private static void init() {
        try {
            printingSdk = new PrintingSdk(mContext);
            intentApi = new IntentAPI(mContext);

            printingSdk.startService(printServiceImp);

            intentApi.runService(serviceCallbackImp);
            intentApi.setPrintCallback(printCallBackImp);
        } catch (Exception exception) {
            e("打印SDK初始化异常" + exception.getMessage());
        }
    }

    public static void getInstance(Context context) {
        if (instance == null) {
            synchronized (PrintUtil.class) {
                if (instance == null)
                    instance = new PrintUtil(context);
            }
        }
    }

    /**
     * 用热敏打印机打印内容
     *
     * @param
     */

    public static void doPrintingWithThermalPrinter(PrintCallBack callBack) {
        printCallBack = callBack;
        if (mUsbPrinter == null) {
            mUsbPrinter = new UsbPrinter(mContext);
            mUsbPrinter.init();
        }
        try {
            final UsbDevice dev = getCorrectDevice();
            if (dev == null) {
                e("没找到热敏打印机");
            }
            if (!mUsbPrinter.open(dev)) {
                e("没连接热敏打印机");
            }
            if (dev != null && mUsbPrinter.open(dev)) {
//                mUsbPrinter.doFunction(Const.TX_FEED, 80, 0);
                mUsbPrinter.doFunction(Const.TX_ALIGN, Const.TX_ALIGN_LEFT, 0);
                mUsbPrinter.outputStringLn("xxxxxxx");
                mUsbPrinter.resetFont();
                mUsbPrinter.doFunction(Const.TX_ALIGN, 1, 0);
                mUsbPrinter.doFunction(Const.TX_FEED, 60, 0);
                mUsbPrinter.doFunction(Const.TX_QR_DOTSIZE, 8, 0);
                mUsbPrinter.doFunction(Const.TX_QR_ERRLEVEL, Const.TX_QR_ERRLEVEL_M, 0);
                mUsbPrinter.printQRcode("xxxxx");
                mUsbPrinter.doFunction(Const.TX_FEED, 140, 0);
                mUsbPrinter.doFunction(Const.TX_CUT, Const.TX_CUT_FULL, 2);
                mUsbPrinter.doFunction(Const.TX_FEED, 20, 0);
                callBack.onPrintStatus(true);
            } else {
                callBack.onPrintStatus(false);
                e("热敏打印机连接异常或未找到热敏打印机");
            }
        } catch (Exception e) {
            e.printStackTrace();
            e("热敏打印机连接异常" + e.getMessage());
            callBack.onPrintStatus(false);
        }
    }


    // 热敏打印机
    private static UsbDevice getCorrectDevice() {
        final UsbManager usbMgr = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        final Map<String, UsbDevice> devMap = usbMgr.getDeviceList();
        for (String name : devMap.keySet()) {
            if (null != devMap.get(name)) {
                e("ManufacturerName:" + devMap.get(name).getManufacturerName());
                if (UsbPrinter.checkPrinter(devMap.get(name)) &&
                        !devMap.get(name).getManufacturerName().contains("HP")) {
                    return devMap.get(name);
                }
            }
        }
        return null;
    }


    /**
     * 使用Usb打印机打印
     * 告诉PrintHand要打印的文件
     *
     * @param filePath 文件路径
     */


    public static void doPrintingWithUsb(String filePath, PrintCallBack callBack) {
        printCallBack = callBack;
        printFilePath = filePath;
        if (isInit()) {
            new Thread(() -> {
                try {
                    if (printServiceConnected) {
                        serviceCallbackImp.setPrintCallBack(callBack);
                        printCallBackImp.setPrintCallBack(callBack);
                        intentApi.print("PrintingSample", "application/ms-word", Uri.parse("file://" + filePath));
                        e("打印文件路径:" + filePath);
                    } else {
                        e("打印机未连接或者打印服务未启动");
                        callBack.onPrintStatus(false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    callBack.onPrintStatus(false);
                }
            }).start();
        }
    }

    /**
     * 关闭打印机
     */
    public static void cancelAllService() {
        if (intentApi != null && intentApi.isServiceRunning()) {
            intentApi.stopService(null);
            try {
                intentApi.setServiceCallback(null);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            try {
                intentApi.setPrintCallback(null);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        if (null != printingSdk && printingSdk.isServiceRunning())
            printingSdk.stopService();
        if (null != mUsbPrinter)
            mUsbPrinter.close();

    }


    /**
     * 连接PrintHand打印服务成功回调
     */
    @Override
    public void serviceConnectedStatus(boolean status) {
        if (status) {
            if (ConfigUtil.getIsConnectPrinterSuccess())
                connect();
            else
                Toast.makeText(mContext, "打印机未配置,请联系维护人员配置打印机", Toast.LENGTH_SHORT).show();
        } else {
            mainLopHandler.post(() -> {
                printServiceConnected = false;
                Toast.makeText(mContext, "打印机断开连接,请联系维护人员", Toast.LENGTH_SHORT).show();
                if (printCallBack != null) {
                    printCallBack.onPrintStatus(false);
                }
            });
        }
    }

    private final Handler mainLopHandler = new Handler(Looper.getMainLooper());


    private static boolean isInit() {
        if (null == mContext) {
            e("请到application里面初始化util");
            return false;
        } else {
            return true;
        }
    }


    /**
     * 初始化连接SDK成功后，直接开始连接打印机
     */
    private void connect() {
        findPrintersWithUSB(printerBeanList -> {
            for (PrinterBean printerBean : printerBeanList) {
                if (Objects.requireNonNull(ConfigUtil.getPrintUsbData()).getName().equals(printerBean.getName())) {
                    findDriver(printerBean, devList -> {
                        for (DriversSearchEntryBean driversSearchEntryBean : devList) {
                            if (Objects.requireNonNull(ConfigUtil.getDriverSearch()).getTransportType().getName().equals(driversSearchEntryBean.getTransportType().getName())) {
                                connectPrinter(printerBean, driversSearchEntryBean, driversSearchEntryBean.getListDriverHandle().get(0), null);
                                break;
                            }
                        }
                    });
                }
            }
        });
    }


    /**
     * WIFI查找打印机
     *
     * @param listener 数据回调接口
     */
    public static void findPrintersWithWIFI(FindDeviceListener listener) {
        if (isInit()) {
            List<Printer> selectPrinterList = new ArrayList<>();
            try {
                printingSdk.startDiscoverWiFi(new IDiscoverListener.Stub() {
                    @Override
                    public void start() {
                    }

                    @Override
                    public void printerFound(List<Printer> list) {
                        selectPrinterList.clear();
                        selectPrinterList.addAll(list);
                    }

                    @Override
                    public void finish(Result result) {
                        List<PrinterBean> printerBeans = new ArrayList<>();
                        if (!selectPrinterList.isEmpty()) {
                            for (Printer sp : selectPrinterList) {
                                PrinterBean printerBean = new PrinterBean();
                                printerBean.setId(sp.getId());
                                printerBean.setPrinterOptions(sp.getOptions());
                                printerBean.setContext(sp.getContext());
                                printerBean.setDescr(sp.getDescription());
                                printerBean.setName(sp.getName());
                                printerBean.setType(sp.getType());
                                printerBean.setOwner(sp.getOwner());
                                printerBean.setTransportTypes(sp.getTransportTypes());
                                printerBeans.add(printerBean);
                            }
                            listener.onSelectDeviceSuccess(printerBeans);
                        } else {
                            e("没有找到WIFI打印机");
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                e("wifi查找打印机异常");
            }
        }
    }

    /**
     * USB查找打印机
     *
     * @param listener 查找参数的回调
     */
    public static void findPrintersWithUSB(FindDeviceListener listener) {
        if (isInit()) {
            List<Printer> selectPrinterList = new ArrayList<>();
            try {
                if (printingSdk != null) {
                    printingSdk.startDiscoverUSB(new IDiscoverListener.Stub() {
                        @Override
                        public void start() {

                        }

                        @Override
                        public void printerFound(List<Printer> list) {
                            selectPrinterList.clear();
                            selectPrinterList.addAll(list);
                        }

                        @Override
                        public void finish(Result result) {
                            List<PrinterBean> printerBeans = new ArrayList<>();
                            if (!selectPrinterList.isEmpty()) {
                                for (Printer sp : selectPrinterList) {
                                    PrinterBean printerBean = new PrinterBean();
                                    printerBean.setId(sp.getId());
                                    printerBean.setPrinterOptions(sp.getOptions());
                                    printerBean.setContext(sp.getContext());
                                    printerBean.setDescr(sp.getDescription());
                                    printerBean.setName(sp.getName());
                                    printerBean.setType(sp.getType());
                                    printerBean.setOwner(sp.getOwner());
                                    printerBean.setTransportTypes(sp.getTransportTypes());
                                    printerBeans.add(printerBean);
                                }
                                listener.onSelectDeviceSuccess(printerBeans);
                            } else {
                                e("没有搜索到USB打印机");
                            }
                        }
                    });
                } else {
                    init();
                    findPrintersWithUSB(listener);
                }
            } catch (Exception e) {
                e.printStackTrace();
                e("usb查找打印机异常");
            }
        }
    }


    /**
     * 查找打印机驱动
     *
     * @param printerBean 打印机基本信息
     * @param listener    找到驱动列表的回调
     */
    public static void findDriver(PrinterBean printerBean, FindDriversListener listener) {
        if (isInit()) {
            Printer printer = new Printer(printerBean.getId(), printerBean.getType(), printerBean.getName(), printerBean.getOwner(),
                    printerBean.getDescr(), printerBean.getPrinterOptions()
                    , printerBean.getTransportTypes(), printerBean.getContext());
            try {
                if (printingSdk != null) {
                    printingSdk.findDrivers(printer, new IFindDriversListener.Stub() {
                        @Override
                        public void start() {
                        }

                        @Override
                        public void finish(List<DriversSearchEntry> driversSearchEntryList) {
                            List<DriversSearchEntryBean> selectPrinterList = new ArrayList<>();
                            if (!driversSearchEntryList.isEmpty()) {
                                for (DriversSearchEntry driversSearchEntry : driversSearchEntryList) {
                                    DriversSearchEntryBean entry = new DriversSearchEntryBean();
                                    if (!driversSearchEntry.getDriverHandlesList().isEmpty()) {
                                        List<DriversSearchEntryBean.mDriverHandle> mDriverHandles = new ArrayList<>();
                                        for (DriverHandle driverHandle : driversSearchEntry.getDriverHandlesList()) {
                                            DriversSearchEntryBean.mDriverHandle mDriverHandleData = new DriversSearchEntryBean.mDriverHandle();
                                            mDriverHandleData.setGeneric(driverHandle.isGeneric());
                                            mDriverHandleData.setId(driverHandle.getId());
                                            mDriverHandleData.setPrinterName(driverHandle.getPrinterName());
                                            mDriverHandles.add(mDriverHandleData);
                                        }
                                        entry.setListDriverHandle(mDriverHandles);
                                    }
                                    entry.setTransportType(driversSearchEntry.getTransportType());
                                    selectPrinterList.add(entry);
                                }
                                listener.onFindDriversSuccess(selectPrinterList);
                            }
                        }
                    });
                } else {
                    init();
                    findDriver(printerBean, listener);
                }
            } catch (Exception e) {
                e.printStackTrace();
                e("查找打印机驱动异常");
            }
        }
    }

    /**
     * 连接打印机
     *
     * @param listener 连接成功回调
     */
    public static void connectPrinter(PrinterBean printerBean, DriversSearchEntryBean driversSearchEntryBean,
                                      DriversSearchEntryBean.mDriverHandle mDriverHandle,
                                      ConnectDeviceListener listener) {
        if (isInit()) {
            Printer printer = new Printer(printerBean.getId(), printerBean.getType(), printerBean.getName(), printerBean.getOwner(),
                    printerBean.getDescr(), printerBean.getPrinterOptions()
                    , printerBean.getTransportTypes(), printerBean.getContext());

            DriverHandle handle = new DriverHandle(mDriverHandle.getId(), mDriverHandle.getPrinterName(), mDriverHandle.isGeneric());

            try {
                assert driversSearchEntryBean != null;
                if (printingSdk != null) {
                    printingSdk.setup(printer, handle, driversSearchEntryBean.getTransportType(), false, new ISetupPrinterListener.Stub() {
                        @Override
                        public void start() {
                            e("开始连接打印机");
                        }

                        @Override
                        public void libraryPackInstallationProcess(int arg0) {
                            e("连接打印机" + arg0);
                        }

                        @Override
                        public void finish(Result result) {
                            if (result.getType() == ResultType.OK) {
                                e("连接USB打印机成功");
                                if (listener != null)
                                    listener.onConnectDeviceStatus(true);
                                printServiceConnected = true;
                            } else {
                                e("连接USB打印机失败");
                                if (listener != null)
                                    listener.onConnectDeviceStatus(false);
                                printServiceConnected = false;
                            }
                        }
                    });
                } else {
                    init();
                    connectPrinter(printerBean, driversSearchEntryBean, mDriverHandle, listener);
                }
            } catch (Exception e) {
                e.printStackTrace();
                e("连接打印机异常");
            }
        }
    }

    public interface ConnectDeviceListener {
        void onConnectDeviceStatus(boolean status);
    }

    public interface FindDeviceListener {
        void onSelectDeviceSuccess(List<PrinterBean> printerBean);
    }

    public interface FindDriversListener {
        void onFindDriversSuccess(List<DriversSearchEntryBean> driversSearchEntries);
    }
}
