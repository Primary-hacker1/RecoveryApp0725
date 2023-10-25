import com.common.network.LogUtils
import com.rick.recoveryapp.bluetooth.BtDataPro
import com.rick.recoveryapp.utils.CRC16Util

object SerialPort{
    fun getCmdCode(zuli: Int, blood_measure: String, isBegin: Boolean): String? {
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
        LogUtils.d("GetCmdCode"+"ActiveFragemt,获取指令")
        return splicingStr + CRC16 + cmd_end
    }

}