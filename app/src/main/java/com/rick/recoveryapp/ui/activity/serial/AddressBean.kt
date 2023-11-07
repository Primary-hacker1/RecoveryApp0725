package com.rick.recoveryapp.ui.activity.serial

import com.common.network.LogUtils
import com.rick.recoveryapp.utils.CRC16Util

data class AddressBean(var macAddress: String? = "",//mac地址
                       var ecg: String? = "",//心电地址
                       var bloodPressure: String? = "",//血压地址
                       var bloodOxygen: String? = ""//血氧地址
) {

    private val tag = AddressBean::class.java.name;

    fun getAddressSerial(): String {
        val head = "A8850C"
        val addressBean = head + macAddress + ecg + bloodPressure + bloodOxygen
        val crc16 = CRC16Util.getCRC16(addressBean)
        val end = "ED"

        val allAddress = addressBean + crc16 + end
        LogUtils.d(tag + allAddress)
        return allAddress
    }
}