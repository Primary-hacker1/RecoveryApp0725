package com.rick.recoveryapp.ui.activity.u3d;

import com.common.network.LogUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rick.recoveryapp.ui.activity.helper.BtDataProX;
import com.rick.recoveryapp.entity.ConnectData;
import com.rick.recoveryapp.utils.CRC16Util;
import com.rick.recoveryapp.utils.LocalConfig;
import com.unity3d.player.UnityPlayer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class U3DFactory {
    private static final String tag = U3DFactory.class.getName();
    static BtDataProX btDataPro = new BtDataProX();
    static String CMD_CODE = "";
    static String IP = LocalConfig.ip;
    static int sex = LocalConfig.sex;
    static Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public static String GetCmdCode(int zuli, String blood_measure, boolean isBegin, int speed_lv, int spasms_lv) {
        String cmd_head = "A88408",              //包头
                sport_mode = "00",                //运动模式
                active_direction = "20",          //运动方向
                //痉挛等级
                //速度设定
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

    public static void connect() {
        ConnectData connectData = new ConnectData();
        connectData.setIp(IP);
        connectData.setPort("1883");

        Date date1 = new Date();
        SimpleDateFormat dateFormat1 = new SimpleDateFormat("mmss", Locale.CHINA);
        String sim1 = dateFormat1.format(date1);
        connectData.setId(sim1);
        connectData.setUsername("admin");
        connectData.setPasswd("password");
        connectData.setBianhao(LocalConfig.medicalNumber);
        connectData.setName(LocalConfig.userName);
        LogUtils.e(tag + LocalConfig.userName);
        connectData.setSex(sex);//性别
        UnityPlayer.UnitySendMessage("DataManager", "Connect", Analysis(connectData));
    }

    public static String Analysis(Object obj) {
        return gson.toJson(obj);
    }


}
