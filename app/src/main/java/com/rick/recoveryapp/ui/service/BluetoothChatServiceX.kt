package com.rick.recoveryapp.ui.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import com.rick.recoveryapp.ui.activity.BluetoothChat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import java.util.UUID

/**
 * 这个类做了所有设置和管理与其它蓝牙设备连接的工作。
 * 它有一个线程监听传入连接，一个线程与设备进行连接，还有一个线程负责连接后的数据传输。
 */
class BluetoothChatServiceX(private val mHandler: Handler) {
    private val MESSAGE_STATE_CHANGE = 1
    val MESSAGE_READ = 2
    val MESSAGE_WRITE = 3
    private val MESSAGE_DEVICE_NAME = 4
    private val MESSAGE_TOAST = 5

    // 成员变量
    private val mAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var mAcceptThread: AcceptThread? = null
    private var mConnectThread: ConnectThread? = null
    private var mConnectedThread: ConnectedThread? = null
    private var mState: Int

    //蓝牙接收延迟时间
    private var delayTime = 100

    /**
     * 构造函数。 准备新的BluetoothChat会话。
     *
     * A Handler to send messages back to the UI Activity
     */
    init {
        mState = STATE_NONE
    }

    @get:Synchronized
    @set:Synchronized
    var state: Int
        /**
         * 返回当前连接状态。
         */
        get() = mState
        /**
         * 设置聊天连接的当前状态
         *
         * @param state 定义当前连接状态的整数
         */
        private set(state) {
            if (D) Log.d(
                TAG,
                "setState() $mState -> $state"
            )
            mState = state

            // Give the new state to the Handler so the UI Activity can update
            mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget()
        }

    /**
     * 启动聊天服务。 特别地启动AcceptThread以在侦听（服务器）模式下开始会话。
     * 由Activity onResume（）调用
     */
    @Synchronized
    fun start() {
        if (D) Log.d(TAG, "start")

        // 取消尝试建立连接的任何线程
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }

        // 取消当前运行连接的任何线程
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

        // 启动线程以监听BluetoothServerSocket
        if (mAcceptThread == null) {
            mAcceptThread = AcceptThread()
            mAcceptThread!!.start()
        }
        state = STATE_LISTEN
    }

    /**
     * 启动ConnectThread以启动与远程设备的连接。
     *
     * @param device 要连接的BluetoothDevice
     */
    @Synchronized
    fun connect(device: BluetoothDevice) {
        if (D) Log.d(
            TAG,
            "connect to: $device"
        )

        // 取消尝试建立连接的任何线程
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread!!.cancel()
                mConnectThread = null
            }
        }

        // 取消当前运行连接的任何线程
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

        // 启动线程以与给定设备连接
        mConnectThread = ConnectThread(device)
        mConnectThread!!.start()
        state =
            STATE_CONNECTING
    }

    /**
     * 启动ConnectedThread以开始管理蓝牙连接
     *
     * @param socket 在其上进行连接的BluetoothSocket
     * @param device 已连接的BluetoothDevice
     */
    @SuppressLint("MissingPermission")
    @Synchronized
    fun connected(socket: BluetoothSocket?, device: BluetoothDevice) {
        if (D) Log.d(TAG, "connected")

        // 取消完成连接的线程
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }

        // 取消当前运行连接的任何线程
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

        // 取消接受线程，因为我们只想连接到一个设备
        if (mAcceptThread != null) {
            mAcceptThread!!.cancel()
            mAcceptThread = null
        }

        // 启动线程以管理连接并执行传输
        mConnectedThread = ConnectedThread(socket)
        mConnectedThread!!.start()

        //将连接的设备的名称发送回UI活动
        val msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME)
        val bundle = Bundle()
        bundle.putString(DEVICE_NAME, device.name)
        msg.data = bundle
        mHandler.sendMessage(msg)
        state = STATE_CONNECTED
    }

    /**
     * Stop all threads
     */
    @Synchronized
    fun stop() {
        if (D) Log.d(TAG, "stop")
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }
        if (mAcceptThread != null) {
            mAcceptThread!!.cancel()
            mAcceptThread = null
        }
        state = STATE_NONE
    }

    /**
     * 以线程锁（不同步）方式写入ConnectedThread
     *
     * @param out 要写入的字节
     * @see ConnectedThread.write
     */
    fun write(out: ByteArray) {
        //创建临时对象
        var r: ConnectedThread?
        // 同步ConnectedThread的副本
        synchronized(this) {
            if (mState != STATE_CONNECTED) return
            r = mConnectedThread
        }
        //执行写入不同步
        r?.write(out)
    }

    /**
     * 指示连接尝试失败并通知UI活动.
     */
    private fun connectionFailed() {
        state = STATE_LISTEN

        // 将失败消息发送回活动
        val msg = mHandler.obtainMessage(MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(TOAST, "无法自动连接设备，请手动连接")
        msg.data = bundle
        mHandler.sendMessage(msg)
    }

    /**
     * 指示连接已丢失并通知UI活动
     */
    private fun connectionLost() {
        state = STATE_LISTEN

        // 将失败消息发送回活动
        val msg = mHandler.obtainMessage(MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(TOAST, "丢失设备连接")
        Log.d("TOAST", "丢失设备连接")
        msg.data = bundle
        mHandler.sendMessage(msg)
    }

    /**
     * 此线程在侦听传入连接时运行。 它的行为像一个服务器端。
     * 它运行直到接受连接
     * (或直到取消).
     */
    private inner class AcceptThread @SuppressLint("MissingPermission") constructor() : Thread() {
        // 本地服务器套接字
        private val mmServerSocket: BluetoothServerSocket?

        init {
            var tmp: BluetoothServerSocket? = null

            // 创建一个新的侦听服务器套接字
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID)
            } catch (e: IOException) {
                Log.e(TAG, "listen() failed", e)
            }
            mmServerSocket = tmp
        }

        override fun run() {
            if (D) Log.d(
                TAG,
                "BEGIN mAcceptThread$this"
            )
            name = "AcceptThread"
            var socket: BluetoothSocket? = null

            // 如果我们没有连接，请监听服务器插座
            while (mState != STATE_CONNECTED) {
                try {
                    // 这是一个阻塞调用，只会在成功的连接或异常返回
                    if (mmServerSocket != null) {
                        socket = mmServerSocket.accept()
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "accept() failed", e)
                    break
                }

                // 如果连接被接受
                if (socket != null) {
                    synchronized(this@BluetoothChatServiceX) {
                        when (mState) {
                            STATE_LISTEN, STATE_CONNECTING ->                                 // 状况正常。 启动连接的线程。
                                connected(socket, socket.remoteDevice)

                            STATE_NONE, STATE_CONNECTED ->                                 //未准备就绪或已连接。 终止新套接字。
                                try {
                                    socket.close()
                                } catch (e: IOException) {
                                    Log.e(
                                        TAG,
                                        "Could not close unwanted socket",
                                        e
                                    )
                                }

                            else -> {}
                        }
                    }
                }
            }
            if (D) Log.i(TAG, "END mAcceptThread")
        }

        fun cancel() {
            if (D) Log.d(
                TAG,
                "cancel $this"
            )
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of server failed", e)
            }
        }
    }

    /**
     * 尝试与设备建立传出连接时，此线程运行。类似于一个客户端
     * 它直通; 连接
     * 成功或失败。
     */
    private inner class ConnectThread @SuppressLint("MissingPermission") constructor(private val mmDevice: BluetoothDevice) :
        Thread() {
        private val mmSocket: BluetoothSocket?

        init {
            var tmp: BluetoothSocket? = null

            // 获取与给定的蓝牙设备的连接的BluetoothSocket
            try {
                tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID)
            } catch (e: IOException) {
                Log.e(TAG, "create() failed", e)
            }
            mmSocket = tmp
        }

        @SuppressLint("MissingPermission")
        override fun run() {
            Log.i(TAG, "BEGIN mConnectThread")
            name = "ConnectThread"
            // 始终取消发现，因为它会减慢连接速度
            mAdapter.cancelDiscovery()
            //连接到BluetoothSocket
            try {
                mmSocket?.connect()
            } catch (e: IOException) {
                connectionFailed()
                // Close the socket
                try {
                    // 这是一个阻塞调用，只会在成功的连接或异常返回
                    mmSocket!!.close()
                } catch (e2: IOException) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2)
                }
                // 启动服务以重新启动侦听模式
                this@BluetoothChatServiceX.start()
                return
            }

            // 重置ConnectThread，因为我们完成了
            synchronized(this@BluetoothChatServiceX) { mConnectThread = null }

            // 启动连接的线程
            connected(mmSocket, mmDevice)
        }

        fun cancel() {
            try {
                mmSocket!!.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect socket failed", e)
            }
        }
    }

    /**
     * 此线程在与远程设备的连接期间运行。
     * 它处理所有传入和传出传输。
     */
    private inner class ConnectedThread(socket: BluetoothSocket?) : Thread() {
        private val mmSocket: BluetoothSocket?
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?

        init {
            Log.d(TAG, "create ConnectedThread")
            mmSocket = socket
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            // 获取BluetoothSocket输入和输出流
            try {
                if (socket?.inputStream != null) {
                    tmpIn = socket.inputStream
                }
                tmpOut = socket?.outputStream
            } catch (e: IOException) {
                Log.e(TAG, "temp sockets not created", e)
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut
        }

        override fun run() {
            Log.i(TAG, "BEGIN mConnectedThread")
            val buffer = ByteArray(1024)
            var bytes: Int
            var readMessage: String
            while (true) {
                try {
                    if (mmInStream == null) {
                        return
                    }
                    val availableBytes = mmInStream.available()
                    if (availableBytes > 0) {
                        bytes = mmInStream.read(buffer)
                        val msg = Message()
                        val data = Bundle()
                        // readMessage = new String(buffer,0,bytes);
                        readMessage = bytesToHexString(buffer, bytes)
                        data.putString("BTdata", readMessage)
                        msg.what = BluetoothChat.MESSAGE_READ
                        msg.data = data
                        mHandler.sendMessage(msg)
                    }
                    try {
                        sleep(delayTime.toLong()) //当有数据流入时，线程休眠一段时间，默认100ms
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "disconnected", e)
                    connectionLost()
                    break
                }
            }
        }

        // 将字节数组转化为16进制字符串，确定长度
        fun bytesToHexString(bytes: ByteArray, a: Int): String {
            val result = StringBuilder()
            for (i in 0 until a) {
                var hexString = Integer.toHexString(bytes[i].toInt() and 0xFF) // 将高24位置0
                if (hexString.length == 1) {
                    hexString = "0$hexString"
                }
                result.append(hexString.uppercase(Locale.getDefault())).append(" ")
            }
            return result.toString()
        }

        /**
         * 写入连接的OutStream。
         *
         * @param buffer The bytes to write
         */
        fun write(buffer: ByteArray) {
            if (buffer.isEmpty()) {
                return
            }
            try {
                mmOutStream?.write(buffer)
                //将发送的消息共享回UI活动
                mHandler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer)
                    .sendToTarget()
            } catch (e: IOException) {
                Log.e(TAG, "Exception during write", e)
            }
        }

        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect socket failed", e)
            }
        }
    }

    companion object {
        // Debugging
        private const val TAG = "BluetoothChatService"
        private const val D = true

        // 创建服务器套接字时SDP记录的名称
        private const val NAME = "BluetoothChat"

        // 该应用的唯一UUID
        private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        // 指示当前连接状态的常量
        const val STATE_NONE = 0 // we're doing nothing
        const val STATE_LISTEN = 1 // 现在正在侦听传入连接
        const val STATE_CONNECTING = 2 // 现在启动传出连接
        const val STATE_CONNECTED = 3 // 现在连接到远程设备

        // 来自BluetoothChatService Handler的关键名
        const val DEVICE_NAME = "device_name"
        const val TOAST = "toast"
    }
}