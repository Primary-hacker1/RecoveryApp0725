package com.rick.recoveryapp.utils;

public class BaseUtil {

    private static long lastClickTime;

    public static boolean isFastDoubleClick() {//快速双击创建多个bug
        long time = System.currentTimeMillis();
        if (time - lastClickTime < 500) {
            return true;
        }
        lastClickTime = time;
        return false;
    }

    private static long lastClickTime100;

    public static boolean isFastDoubleClick200() {//发送相同命令100毫秒内忽略一次
        long time = System.currentTimeMillis();
        if (time - lastClickTime100 < 200) {
            return true;
        }
        lastClickTime100 = time;
        return false;
    }

}
