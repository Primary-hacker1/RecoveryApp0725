package com.rick.recoveryapp.ui.activity.helper

import com.common.network.LogUtils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.rick.recoveryapp.entity.EcgData
import com.rick.recoveryapp.entity.protocol.PoolMessage
import com.rick.recoveryapp.entity.protocol.UploadData
import com.rick.recoveryapp.ui.BaseApplication
import com.rick.recoveryapp.utils.BaseUtil
import com.rick.recoveryapp.utils.CRC16Util
import com.rick.recoveryapp.utils.LiveDataBus
import com.rick.recoveryapp.utils.LocalConfig
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class BtDataProX {
    // 握手指令-发送
    val CONNECT_SEND = "A8810101B03CED"

    // 握手指令-发送
    //   public final String CONNECT_SEND_1 = "A8810101B03CED";
    // 握手指令-暂停
    val CONNECT_CLOSE = "A8810102F03DED"

    // 设备MAC地址绑定
    //public final String MAC_CODE = "A88512  E76B581B5164 A4C138402A4D 00A0503D0264  07F8ED";
    //血氧 00:A0:50:3B:CB:AC   血压 A4:C1:38:44:16:0C   心电 D2:08:AA:BB:37:AE
    val MAC_CODE = "A88512  D2 08 AA BB 37 AE   A4 C1 38 44 16 0C   00 A0 50 3B CB AC   0DA6ED"

    // 开始测量血压
    val CONTORL_CODE_BEGIN = "A884080000000000000051CE87ED"

    // 血压测量结束
    val CONTORL_CODE_END = "A8840800000000000000528E86ED"

    //握手指令应答
    val CONN_CODE_ANSWER = "A8 81 0C"

    //设备信息上传应答
     val UPLODE_ANSWER = "A8 82 0D"

    //心电数据包应答
     val ECGDATA_ANSWER = "A8 83"

    //Mac地址绑定应答
    val MAC_ANSWER = "A8 85 01"

    //设备控制应答（血压仪启动/停止）
     val CONTORL_ANSWER = "A8 84 01"
    private var CMD_CODE: String? = null
    private var MassageStr = ""
    var gson: Gson = GsonBuilder().disableHtmlEscaping().create()
    var count = 0
    private var corePoolSize = 2
    private var maxPoolSize = 2
    private var keepAliveTime = 0L
    fun GetCmdCode(ecgMac: String, bloodMac: String, doxygenMac: String): String {
        val cmdHead = "A88512"
        //包头
        val cmdEnd = "ED" //结尾
        val splicingStr = cmdHead + ecgMac + bloodMac + doxygenMac
        val CRC16 = CRC16Util.getCRC16(splicingStr)
        CMD_CODE = splicingStr + CRC16 + cmdEnd
        LogUtils.d("GetCmdCode" + "ActiveFragment,获取指令")
        return CMD_CODE as String
    }

    fun sendBTMessage(message: String) { //重写发送函数，参数不同。
        if (BaseUtil.isFastDoubleClick200()) {
            LogUtils.e("发送串口消息100m: 多次点击" + BaseUtil.isFastDoubleClick200())
        }
        if (message.isEmpty()) {
            return
        }
        val myQueue: BlockingQueue<String> = LinkedBlockingQueue()
        var executor = ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                TimeUnit.MILLISECONDS,
                LinkedBlockingQueue()
        )
        try {
            executor.execute(Producer(myQueue, message))
        } catch (e: Exception) {
            executor = ThreadPoolExecutor(
                    corePoolSize,
                    maxPoolSize,
                    keepAliveTime,
                    TimeUnit.MILLISECONDS,
                    LinkedBlockingQueue()
            )
            executor.execute(Producer(myQueue, message))
        }
        try {
            // 等待生产者线程完成
            executor.shutdown()
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
            // 向队列发送停止信号
            myQueue.put("STOP")
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        // 清空输出缓冲区
        //     mOutStringBuffer.setLength(0);
        //                    Toast.LENGTH_SHORT).show();
    }

    fun Processing(RXlist: ArrayList<String>, type: String?) {
        // String type = RXlist.get(1);
        val poolMessage: PoolMessage
        when (type) {
            "81" -> Connection(RXlist)
            "82" -> {
                val uploadData = resultsData(RXlist)
                poolMessage = PoolMessage()
                if (uploadData != null) {
                    poolMessage.objectJson = gson.toJson(uploadData)
                    poolMessage.objectName = UPLODE_ANSWER
                    poolMessage.isState = true
                    //   LocalConfig.poolMessage = poolMessage;
                } else {
                    poolMessage.objectJson = null
                    poolMessage.objectName = ""
                    poolMessage.isState = false
                }
                LocalConfig.poolMessage = poolMessage
                LiveDataBus.get().with(Constants.BT_PROTOCOL).postValue(poolMessage)
            }

            "83" -> {
                //EcgDataPro(hexstr);
                val ecgData = EcgDataPro(RXlist)
                poolMessage = PoolMessage()
                if (ecgData != null) {
                    poolMessage.objectJson = gson.toJson(ecgData)
                    poolMessage.objectName = ECGDATA_ANSWER
                    poolMessage.isState = true
                    LiveDataBus.get().with(Constants.BT_ECG).postValue(poolMessage)
                } else {
                    poolMessage.objectJson = null
                    poolMessage.objectName = ""
                    poolMessage.isState = false
                }
                LocalConfig.poolMessage1 = poolMessage
            }

            "85" -> {}
            "84" -> {
                poolMessage = PoolMessage()
                poolMessage.objectJson = equipmentControl(RXlist)
                poolMessage.objectName = CONTORL_ANSWER
                poolMessage.isState = true
                LiveDataBus.get().with(Constants.BT_ECG).postValue(poolMessage)
            }

            else -> {}
        }
    }

    fun U3DProcessing(RXlist: ArrayList<String>): PoolMessage {
        val type = RXlist[1]
        var poolMessage = PoolMessage()
        if (type == "81") {
            Connection(RXlist)
        } else if (type == "82") {
            val uploadData = resultsData(RXlist)
            if (uploadData != null) {
                poolMessage.objectJson = gson.toJson(uploadData)
                poolMessage.objectName = UPLODE_ANSWER
                poolMessage.isState = true
            } else {
                poolMessage.objectJson = null
                poolMessage.objectName = ""
                poolMessage.isState = false
            }
        } else if (type == "83") {
            val ecgData = EcgDataPro(RXlist)
            poolMessage = PoolMessage()
            if (ecgData != null) {
                poolMessage.objectJson = gson.toJson(ecgData)
                poolMessage.objectName = ECGDATA_ANSWER
                poolMessage.isState = true
            } else {
                poolMessage.objectJson = null
                poolMessage.objectName = ""
                poolMessage.isState = false
            }
        }
        return poolMessage
    }

    /**
     * 1.握手指令
     */
    private fun Connection(hexlist: ArrayList<String>) {
//        String[] split = hexstr.toString().split(" ");
        val strbuf = StringBuffer()
        for (i in 3..14) {
            strbuf.append(hexlist[i])
        }
        val DeviceId = strbuf.toString()
        MassageStr = "\n接收到数据：$strbuf\n设备ID号： $DeviceId\n"
    }

    /**
     * 2．设备信息上传
     */
    private fun resultsData(hexList: ArrayList<String>): UploadData? {
        if (hexList.size < 22) {
            return null
        }
        val uploadData = UploadData()
        val oxy_vaulestr: String? = null
        val ActiveType = hexList[4]
        val ActiveState = hexList[5]
        val SpasmState = hexList[6]
        val time = hexList[7] + hexList[8]
        val speed = hexList[9]
        val left = hexList[10]
        val right = hexList[11]

        //阻力 痉挛 速度 设定时间
        val stresistance = hexList[12]
        val stspasm = hexList[13]
        val stspeed = hexList[14]
        val sttime = hexList[15]
        val ECG = hexList[16]
        val blood = hexList[17]
        val blood_oxy = hexList[18]
        val high = hexList[19]
        val low = hexList[20]
        val oxy_vaule = hexList[21]

        //时间
        val times = time.toInt(16)
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        val lt = times.toLong()
        val date = Date(lt)
        uploadData.sTresistance = covert16to10(stresistance).toString()
        uploadData.sTspasm = covert16to10(stspasm).toString()
        uploadData.sTspeed = covert16to10(stspeed).toString()
        uploadData.sTtime = covert16to10(sttime).toString()
        uploadData.time = simpleDateFormat.format(date)
        //运动速度
        uploadData.speed = covert16to10(speed).toString()
        //左对称性
        uploadData.left = covert16to10(left).toString()
        //右对称性
        uploadData.right = covert16to10(right).toString()
        //血压高压
        uploadData.high = covert16to10(high).toString()
        //血压低压
        uploadData.low = covert16to10(low).toString()
        when (oxy_vaule) {
            "AA" -> {
                uploadData.oxy_vaulestr = "手指未插入"
            }

            "FF" -> {
                uploadData.oxy_vaulestr = "探头脱落"
            }

            else -> {
                uploadData.oxy_vaulestr = covert16to10(oxy_vaule).toString()
            }
        }
        return DataStr(uploadData, ActiveType, ActiveState, SpasmState, ECG, blood, blood_oxy)
        //        MassageStr = res + "  " + speedstr + "  " + leftstr + "  " + rightstr + "  "
//                + highstr + "  " + lowstr + "  " + oxy_vaulestr + "  " + information + '\n';
    }

    private fun DataStr(uploadData: UploadData, activeType: String, activeState: String, spasmState: String, ecg: String, blood: String, blood_oxy: String): UploadData {
        val activeName: String = when (activeType) {
            "00" -> "智能模式/非运动"
            "01" -> "主动模式"
            "02" -> "被动模式"
            "0A" -> "智能模式/被动"
            "0B" -> "智能模式/主动"
            else -> "未知"
        }
        uploadData.activeType = activeName
        val stateName: String = when (activeState) {
            "10" -> "停机状态"
            "11" -> "运行状态"
            else -> "未知"
        }
        uploadData.activeState = stateName
        val spasmName: Int = when (spasmState) {
            "20" -> 0
            "21" -> 1
            "22" -> 2
            "23" -> 3
            "24" -> 4
            "25" -> 5
            else -> -1
        }
        uploadData.spasmState = spasmName
        val ecgstr: String = when (ecg) {
            "30" -> "心电仪未连接"
            "31" -> "已连接"
            else -> "未知"
        }
        uploadData.ecg = ecgstr
        val bloodstr: String = when (blood) {
            "40" -> "血压仪未连接"
            "41" -> "已连接"
            else -> "未知"
        }
        uploadData.blood = bloodstr
        val blood_oxystr: String = when (blood_oxy) {
            "50" -> "血氧仪未连接"
            "51" -> "已连接"
            else -> "未知"
        }
        uploadData.blood_oxy = blood_oxystr
        // DataString = activeName + "  " + stateName + "  " + SpasmName + "  " + bloodstr + "  " + ecgstr + "  " + blood_oxystr;
        return uploadData
    }

    /**
     * 设备控制
     */
    private fun equipmentControl(hexStr: ArrayList<String>): String {
        return if (hexStr.size < 4) {
            "52"
        } else {
            hexStr[3]
        }
    }

    /**
     * 心电数据处理
     */
    private fun EcgDataPro(hexStr: ArrayList<String>): EcgData? {
        var ecgData: EcgData? = EcgData()
        val ecgCooy = ArrayList<Float>()
        try {
            if (hexStr.size > 6) {
                val heartrate = hexStr[0]
                val xinlv = covert16to10(heartrate)
                if (xinlv == 0) {
                    ecgData!!.heartrate = "--"
                } else {
                    ecgData!!.heartrate = xinlv.toString() + ""
                }

                //心率的取值是4,5两位，心电的取值从第6位开始，所以i=6;
                val strs = hexStr[2]
                LogUtils.d("ecgData$strs")
                var i = 2
                while (i < hexStr.size - 4) {
                    val str = reverseHex(hexStr[i] + hexStr[i + 1])
                    val hex = str.toInt(16).toShort().toInt()
                    val ecg = 1.0035 * (-hex * 1800) / (4096 * 178.74)
                    var cooy = 0f
                    if (ecg != -80.8434542284271) {
                        cooy = ecg.toFloat()
                    }
                    ecgCooy.add(cooy)
                    i += 2
                }
                LocalConfig.CoorYList = ecgCooy
                ecgData.ecgCoorY = ecgCooy
            } else {
                ecgCooy.add(0f)
                LocalConfig.CoorYList = ecgCooy
                ecgData!!.heartrate = "--"
                ecgData.ecgCoorY = ecgCooy
            }
        } catch (e: Exception) {
            ecgData = null
            android.util.Log.d("ECG", e.message!!)
        }
        return ecgData
    }

    /**
     * 十六进制字符串高低位转换
     */
    private fun reverseHex(hex: String): String {
        val charArray = hex.toCharArray()
        val length = charArray.size
        val times = length / 2
        var c1i = 0
        while (c1i < times) {
            val c2i = c1i + 1
            val c1 = charArray[c1i]
            val c2 = charArray[c2i]
            val c3i = length - c1i - 2
            val c4i = length - c1i - 1
            charArray[c1i] = charArray[c3i]
            charArray[c2i] = charArray[c4i]
            charArray[c3i] = c1
            charArray[c4i] = c2
            c1i += 2
        }
        return String(charArray)
    }

    /**
     * 16进制字符串转10进制
     *
     * @param hexStr
     * @return int
     */
    fun covert16to10(hexStr: String?): Int {
        val bigint = BigInteger(hexStr.toString(), 16)
        return bigint.toInt()
    }

    internal class Producer(private val queue: BlockingQueue<String>, private val data: String) : Runnable {
        override fun run() {
            try {
                queue.put(data)
                var send = ByteArray(0) // 获取 字符串并告诉BluetoothChatService发送
                try {
                    send = hexStr2Bytes(data)
                } catch (e: Exception) {
                    LogUtils.d("hexStr2Bytes" + e.message)
                }
                BaseApplication.mConnectService.write(send) //回调service

                // 延时300毫秒
                Thread.sleep(200)
                LogUtils.e("发送串口消息完毕: " + deleteCharString(data))
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        private fun deleteCharString(sourceString: String): String {
            val deleteString = StringBuilder()
            for (i in sourceString.indices) {
                if (i % 2 == 1) {
                    deleteString.append(sourceString[i]).append(" ")
                } else {
                    deleteString.append(sourceString[i])
                }
            }
            return deleteString.toString()
        }
    }

    companion object{
        // 将16进制字符串转化为字节数组
        fun hexStr2Bytes(paramString: String): ByteArray {
            val i = paramString.length / 2
            val arrayOfByte = ByteArray(i)
            var j = 0
            while (true) {
                if (j >= i) return arrayOfByte
                val k = 1 + j * 2
                val l = k + 1
                arrayOfByte[j] = (0xFF and Integer.decode(
                    "0x" + paramString.substring(j * 2, k)
                            + paramString.substring(k, l))).toByte()
                ++j
            }
        }

        fun decToHex(n: Int): String {
            var n = n
            val r = StringBuilder() //空字符串
            while (n >= 16) {
                val yushu = n % 16
                val shang = n / 16
                if (yushu > 9) { //特殊处理
                    val c = ((yushu - 10) + 'A'.code).toChar()
                    r.append(c) //连接字符c
                } else {
                    r.append(yushu)
                }
                n = shang
            }
            if (n > 9) {
                val c = ((n - 10) + 'A'.code).toChar()
                r.append(c)
            } else {
                r.append(n)
            }
            return reverse(r.toString())
        }
        //反转字符串（反转后就是正确顺序的十六进制数：从下到上的顺序）
        private fun reverse(r: String): String {
            var s = ""
            for (i in r.length - 1 downTo 0) {
                s += r[i]
            }
            return s
        }
    }
}
