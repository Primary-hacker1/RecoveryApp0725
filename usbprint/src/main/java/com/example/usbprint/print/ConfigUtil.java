package com.example.usbprint.print;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;


/**
 * 配置工具类
 */
public class ConfigUtil {
    private static SharedPreferences sharedPreferences;
    private static ConfigUtil instance;
    private static SharedPreferences.Editor editor;

    private ConfigUtil(Context context) {
        init(context);
    }

    public static void getInstance(Context context) {
        if (instance == null) {
            synchronized (ConfigUtil.class) {
                if (instance == null)
                    instance = new ConfigUtil(context);
            }
        }
    }

    private void init(Context context) {
        sharedPreferences = context.getSharedPreferences("config", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }



    //设置是否连接打印机
    public static void setIsConnectPrinterSuccess(boolean is) {
        editor.putBoolean("isConnectPrinterSuccess", is);
        editor.apply();
    }

    public static boolean getIsConnectPrinterSuccess() {
        return sharedPreferences.getBoolean("isConnectPrinterSuccess", false);
    }

    public static void setPrinter(PrinterBean printer) {
        String printerData = new Gson().toJson(printer);
        editor.putString("printer", printerData);
        editor.apply();
    }

    //    获取配置的打印机数据
    public static PrinterBean getPrintUsbData() {
        String data = sharedPreferences.getString("printer", "");
        if (!TextUtils.isEmpty(data)) {
            PrinterBean printer = new Gson().fromJson(data, PrinterBean.class);
            return printer;
        }
        return null;
    }

    //    保存设备驱动数据方便后续配置时候拿出来用
    public static void setDriverSearch(DriversSearchEntryBean bean) {
        String driversSearch = new Gson().toJson(bean);
        editor.putString("driversSearch", driversSearch);
        editor.apply();
    }

    public static DriversSearchEntryBean getDriverSearch() {
        String data = sharedPreferences.getString("driversSearch", "");
        if (!TextUtils.isEmpty(data)) {
            DriversSearchEntryBean handle =
                    new Gson().fromJson(data, DriversSearchEntryBean.class);
            return handle;
        }
        return null;
    }

    public static void setDriverSearchHandle(DriversSearchEntryBean.mDriverHandle bean) {
        String driversSearch = new Gson().toJson(bean);
        editor.putString("driversSearchHandle", driversSearch);
        editor.apply();
    }

    public static DriversSearchEntryBean.mDriverHandle getDriverSearchHandle() {
        String data = sharedPreferences.getString("driversSearchHandle", "");
        if (!TextUtils.isEmpty(data)) {
            DriversSearchEntryBean.mDriverHandle handle =
                    new Gson().fromJson(data, DriversSearchEntryBean.mDriverHandle.class);
            return handle;
        }
        return null;
    }

}
