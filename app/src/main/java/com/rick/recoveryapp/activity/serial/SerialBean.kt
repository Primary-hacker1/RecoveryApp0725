package com.rick.recoveryapp.activity.serial

data class SerialBean(
        var type: SerialPort.Type? = SerialPort.Type.ACTIVE,
        var zuli: Int? = -1,//阻力
        var blood_measure: String? = "",
        var isBegin: Boolean? = false,
        var speed_lv: Int? = -1,
        var spasms_lv: Int? = -1,
        var time_lv: Long? = 0L//运动时间
){
    fun clearData(){
        type = SerialPort.Type.ACTIVE
        zuli = 0
        blood_measure="50"
        isBegin = false
        speed_lv = -1
        spasms_lv = -1
        time_lv = 0L
    }
}