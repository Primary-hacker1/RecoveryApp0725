package com.rick.recoveryapp.ui.activity.bean

import com.common.network.LogUtils
import com.rick.recoveryapp.ui.service.BtDataPro
import com.rick.recoveryapp.utils.CRC16Util

class SerialPort {
    enum class Type {
        ACTIVE, SUBJECT, INTELLIGENT
    }

    companion object {

        val tag: String = SerialBean::class.java.name

        fun sendCmdAddress(bean: AddressBean):String{
            return bean.getAddressSerial()
        }

        fun getCmdCode(//前面太复杂要一直判断type，这里自动判断
                serialBean: SerialBean
        ): String {
            LogUtils.d(tag + serialBean.toStringRun())

            var getCmdCode = ""
            if (serialBean.type == Type.ACTIVE) {
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
                val zuliHex = "0" + BtDataPro.decToHex(serialBean.zuli!!)
                var avtive_status = "10"
                if (serialBean.isBegin == true) {
                    avtive_status = "11"
                }
                val splicingStr =
                        (cmd_head + sport_mode + avtive_status + active_direction + zuliHex + spasms_lv
                                + speed_lv + time_lv + serialBean.blood_measure)
                val CRC16 = CRC16Util.getCRC16(splicingStr)
                getCmdCode = splicingStr + CRC16 + cmd_end
            }
            if (serialBean.type == Type.SUBJECT) {
                val cmd_head = "A88408"
                //包头
                val sport_mode = "02"
                //运动模式
                val active_direction = "21"
                //运动方向
                //设定时间
                val cmd_end = "ED" //结尾
                val zuliHex = "00"
                val spasmsHex = "0" + BtDataPro.decToHex(serialBean.spasms_lv!!)
                var speedHex = ""
                speedHex = if (serialBean.speed_lv!! >= 16) {
                    BtDataPro.decToHex(serialBean.speed_lv!!)
                } else {
                    "0" + BtDataPro.decToHex(serialBean.speed_lv!!)
                }
                var timeHex = ""
                timeHex = if (serialBean.time_lv!! >= 16) {
                    BtDataPro.decToHex(Math.toIntExact(serialBean.time_lv!!))
                } else {
                    "0" + BtDataPro.decToHex(Math.toIntExact(serialBean.time_lv!!))
                }
                var avtive_status = "10"
                if (serialBean.isBegin!!) {
                    avtive_status = "11"
                }
                val splicingStr =
                        (cmd_head + sport_mode + avtive_status + active_direction + zuliHex + spasmsHex
                                + speedHex + timeHex + serialBean.blood_measure)
                val CRC16 = CRC16Util.getCRC16(splicingStr)
                getCmdCode = splicingStr + CRC16 + cmd_end
            }
            if (serialBean.type == Type.INTELLIGENT) {
                val cmd_head = "A88408"
                //包头
                val sport_mode = "00"
                //运动模式
                val active_direction = "20"
                //运动方向
                // spasms_lv = "00",                 //痉挛等级
                //    speed_lv = "00",                  //速度设定
                val time_lv = "00"
                //设定时间
                val cmd_end = "ED" //结尾
                val zuliHex = "0" + BtDataPro.decToHex(serialBean.zuli!!)
                val spasmsHex = "0" + BtDataPro.decToHex(serialBean.spasms_lv!!)
                val speedHex: String = if (serialBean.speed_lv!! >= 16) {
                    BtDataPro.decToHex(serialBean.speed_lv!!)
                } else {
                    "0" + BtDataPro.decToHex(serialBean.speed_lv!!)
                }
                var avtive_status = "10"
                if (serialBean.isBegin!!) {
                    avtive_status = "11"
                }
                val splicingStr =
                        (cmd_head + sport_mode + avtive_status + active_direction + zuliHex + spasmsHex
                                + speedHex + time_lv + serialBean.blood_measure)
                val CRC16 = CRC16Util.getCRC16(splicingStr)
                getCmdCode = splicingStr + CRC16 + cmd_end
            }
            return getCmdCode
        }

    }

}