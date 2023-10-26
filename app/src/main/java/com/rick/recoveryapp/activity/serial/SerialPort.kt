package com.rick.recoveryapp.activity.serial

import android.util.Log
import com.common.network.LogUtils
import com.rick.recoveryapp.bluetooth.BtDataPro
import com.rick.recoveryapp.utils.CRC16Util

class SerialPort{
    companion object {
        //主动模式串口消息
        fun getCmdCode(zuli: Int, blood_measure: String, isBegin: Boolean): String {
            val cmd_head = "A88408"
            //包头
            val sport_mode = "01"
            //运动模式
            val active_direction = "20"
            //运动方向
            val spasms_lv = "00"
            //痉挛等级
            val speed_lv = "00"
            //速度设定
            val time_lv = "00"
            //设定时间
            val cmd_end = "ED" //结尾
            val zuliHex = "0" + BtDataPro.decToHex(zuli)
            var avtive_status = "10"
            if (isBegin) {
                avtive_status = "11"
            }
            val splicingStr =
                (cmd_head + sport_mode + avtive_status + active_direction + zuliHex + spasms_lv
                        + speed_lv + time_lv + blood_measure)
            val CRC16 = CRC16Util.getCRC16(splicingStr)
            LogUtils.d("GetCmdCode" + "ActiveFragemt,获取指令")
            return splicingStr + CRC16 + cmd_end
        }
        //被动模式串口消息
        fun getCmdCode(
            blood_measure: String,
            isBegin: Boolean,
            spasms_lv: Int,
            speed_lv: Int,
            time_lv: Long
        ): String {
            val cmd_head = "A88408"
            //包头
            val sport_mode = "02"
            //运动模式
            val active_direction = "21"
            //运动方向
            //设定时间
            val cmd_end = "ED" //结尾
            val zuliHex = "00"
            val spasmsHex = "0" + BtDataPro.decToHex(spasms_lv)
            var speedHex = ""
            speedHex = if (speed_lv >= 16) {
                BtDataPro.decToHex(speed_lv)
            } else {
                "0" + BtDataPro.decToHex(speed_lv)
            }
            var timeHex = ""
            timeHex = if (time_lv >= 16) {
                BtDataPro.decToHex(Math.toIntExact(time_lv))
            } else {
                "0" + BtDataPro.decToHex(Math.toIntExact(time_lv))
            }
            var avtive_status = "10"
            if (isBegin) {
                avtive_status = "11"
            }
            val splicingStr =
                (cmd_head + sport_mode + avtive_status + active_direction + zuliHex + spasmsHex
                        + speedHex + timeHex + blood_measure)
            val CRC16 = CRC16Util.getCRC16(splicingStr)
            Log.d("GetCmdCode", "PassiveFragment,获取指令")
            return splicingStr + CRC16 + cmd_end
        }
    }

}