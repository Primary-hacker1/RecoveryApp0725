package com.rick.recoveryapp.ui.activity.serial

import android.os.Parcel
import android.os.Parcelable
import com.common.network.LogUtils
import com.rick.recoveryapp.utils.CRC16Util

data class AddressBean(var macAddress: String? = "",//mac地址
                       var ecg: String? = "",//心电地址
                       var bloodPressure: String? = "",//血压地址
                       var bloodOxygen: String? = ""//血氧地址
) : Parcelable {

    private val tag = AddressBean::class.java.name

    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString()) {
    }

    fun getAddressSerial(): String {
        val head = "A88512"
        val addressBean = head + ecg + bloodPressure + bloodOxygen
        val crc16 = CRC16Util.getCRC16(addressBean)
        val end = "ED"

        val allAddress = addressBean + crc16 + end

        LogUtils.d(tag + allAddress)

        return allAddress
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(macAddress)
        parcel.writeString(ecg)
        parcel.writeString(bloodPressure)
        parcel.writeString(bloodOxygen)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AddressBean> {
        override fun createFromParcel(parcel: Parcel): AddressBean {
            return AddressBean(parcel)
        }

        override fun newArray(size: Int): Array<AddressBean?> {
            return arrayOfNulls(size)
        }
    }
}
