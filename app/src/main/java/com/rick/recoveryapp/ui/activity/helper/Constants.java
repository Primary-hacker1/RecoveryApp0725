package com.rick.recoveryapp.ui.activity.helper;

public interface Constants {
    String BT_CONNECTED = "BT_CONNECTED";//连接状态
    String BT_PROTOCOL = "BT_PROTOCOL";
    String BT_ECG = "BT_ECG";
    String BT_RECONNECTED = "BT_RECONNECTED";//重新连接
    String active = "主动模式";
    String passive = "被动模式";
    String intelligence = "智能模式";

    //            String macAddress = "001B10F04B60";
    String macAddress = "001B10F1EE6E";
    String ecgAddress = "E3ADBA1DF806";
    //    String bloodAddress = "A4C138421CF3";
    String oxygenAddress = "00A0503BD222";

    //            String ecgAddress = "D208AABB37AE";
    String bloodAddress = "A4C13844160C";

    int passiveTime = 300000;//被动模式训练时间默认300000

//            String oxygenAddress = "00A0503BCBAC";

}
