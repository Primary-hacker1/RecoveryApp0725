package com.rick.recoveryapp.ui.activity;/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;


import com.rick.recoveryapp.R;
import com.rick.recoveryapp.ui.service.BluetoothChatServiceX;

import java.io.UnsupportedEncodingException;
import java.util.Objects;

/**
 * 显示通信信息的主Activity。
 */
public class BluetoothChat extends Activity {
    // Debugging
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;

    // 来自BluetoothChatService Handler的消息类型
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // 来自BluetoothChatService Handler的关键名
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    private static final int REQUEST_ENABLE_BT = 2;

    // 布局视图
//    private TextView mTitle;
    private TextView mConversationView;
    private TextView outcount;
    private TextView incount;

    private TextView view;


    // 声明button按钮
    private Button mSendButton;
    private Button stop;

    private Button search;
    private Button disc;
    // 用来保存存储的文件名
    public String filename = "";
    // 保存用数据缓存
    private int countin = 0;
    private int countout = 0;

    // 已连接设备的名称
    private String mConnectedDeviceName = null;
    // 输出流缓冲区
    private StringBuffer mOutStringBuffer;

    // 本地蓝牙适配器
    private BluetoothAdapter mBluetoothAdapter = null;
    // 用于通信的服务
    private BluetoothChatServiceX mChatService = null;
    // CheckBox用
    private boolean inhex = true;
    private boolean outhex = true;
    private boolean auto = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (D)
            Log.e(TAG, "+++ ON CREATE +++");
        // 设置窗口布局
       // requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_bluetooth_chat_layout);


        //布局控件初始化函数，注册相关监听器
        init();

        // 获取本地蓝牙适配器
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // 如果没有蓝牙适配器，则不支持
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "蓝牙不可用", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        search.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                search();
            }
        });

        disc.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Toast.makeText(BluetoothChat.this, "该设备已设置为可在300秒内发现，且可连接",
                        Toast.LENGTH_SHORT).show();
                ensureDiscoverable();
            }
        });
    }

    public void search() {
//        Intent serverIntent = new Intent(BluetoothChat.this,
//                DeviceListActivity.class);
//        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }

    private void init() {
        // 通过findViewById获得CheckBox对象
        // 声明复选按钮
        CheckBox in16 = (CheckBox) findViewById(R.id.in16);
        CheckBox autosend = (CheckBox) findViewById(R.id.autosend);
        CheckBox out16 = (CheckBox) findViewById(R.id.out16);

        // 注册事件监听器
        in16.setOnCheckedChangeListener(listener);
        autosend.setOnCheckedChangeListener(listener);
        out16.setOnCheckedChangeListener(listener);
        // 获得button的对象
        search = (Button) findViewById(R.id.search);
        disc = (Button) findViewById(R.id.discoverable1);

        mSendButton = (Button) findViewById(R.id.button_send);
        stop = findViewById(R.id.button_stop);
        //获取选择控件的值

        // 设置custom title
//        mTitle = (TextView) findViewById(R.id.title_left_text);
//        mTitle.setText(R.string.activity_name);
//        mTitle = (TextView) findViewById(R.id.title_right_text);
        view = (TextView) findViewById(R.id.edit_text_out);

    }


    // 响应事件监听
    private OnCheckedChangeListener listener = new OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                                     boolean isChecked) {
            // in16被选中
            if (buttonView.getId() == R.id.in16) {
                if (isChecked) {
                    Toast.makeText(BluetoothChat.this, "16进制显示",
                            Toast.LENGTH_SHORT).show();
                    inhex = true;
                } else
                    inhex = false;
            }

            //out16选中
            if (buttonView.getId() == R.id.out16) {
                if (isChecked) {
                    Toast.makeText(BluetoothChat.this, "16进制发送",
                            Toast.LENGTH_SHORT).show();
                    outhex = true;
                } else
                    outhex = false;
            }
            // 自动发送被选中
            if (buttonView.getId() == R.id.autosend) {
                if (isChecked) {
                    Toast.makeText(BluetoothChat.this, "自动发送",
                            Toast.LENGTH_SHORT).show();
                    auto = true;
                } else
                    auto = false;
            }
        }
    };


    @SuppressLint("MissingPermission")
    @Override
    public void onStart() {
        super.onStart();
        if (D)
            Log.e(TAG, "++ ON START ++");


        //如果BT未打开，请求启用。
        // 然后在onActivityResult期间调用setupChat（）
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // 否则，设置聊天会话
        } else {

            if (mChatService == null)
                setupChat();
            else {
                try {
                    mChatService.wait(100);


                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    //300秒内搜索
    @SuppressLint("MissingPermission")
    private void ensureDiscoverable() {
        if (D)
            Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            //设置本机蓝牙可让发现
            discoverableIntent.putExtra(
                    BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    //自动发送
    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            String message = view.getText().toString();
            sendMessage(message);
            // 初始化输出流缓冲区
            mOutStringBuffer = new StringBuffer("");

        }
    };


    //初始化
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        mConversationView = (TextView) findViewById(R.id.in);
        mConversationView.setMovementMethod(ScrollingMovementMethod
                .getInstance());// 使TextView接收区可以滚动
        outcount = (TextView) findViewById(R.id.outcount);
        incount = (TextView) findViewById(R.id.incount);
        outcount.setText("0");
        incount.setText("0");

        mSendButton.setOnClickListener(v -> {
            // 使用编辑文本小部件的内容发送消息
            TextView view = (TextView) findViewById(R.id.edit_text_out);

            String message = "A8810101B03CED";
            sendMessage(message);
        });

        stop.setOnClickListener(v -> {
            String message = "A8810102F03DED";
            sendMessage(message);
        });

        // 初始化BluetoothChatService以执行app_incon_bluetooth连接
        mChatService = new BluetoothChatServiceX( mHandler);

        AutoConnect();
        //初始化外发消息的缓冲区
        mOutStringBuffer = new StringBuffer();
    }


    //重写发送函数，参数不同。
    private void sendMessage(String message) {
        // 确保已连接
        if (mChatService.getState() != BluetoothChatServiceX.STATE_CONNECTED) {
            Toast.makeText(this, "未连接", Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        // 检测是否有字符串发送
        if (!message.isEmpty()) {
            // 获取 字符串并告诉BluetoothChatService发送
            byte[] send;//回调service
            if (outhex) {
                send = hexStr2Bytes(message);

            } else {
                send = message.getBytes();
            }
            mChatService.write(send);//回调service
            // 清空输出缓冲区
            mOutStringBuffer.setLength(0);
        } else {
            Toast.makeText(this, "发送内容不能为空",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // 将16进制字符串转化为字节数组
    public static byte[] hexStr2Bytes(String paramString) {
        int i = paramString.length() / 2;

        byte[] arrayOfByte = new byte[i];
        int j = 0;
        while (true) {
            if (j >= i)
                return arrayOfByte;
            int k = 1 + j * 2;
            int l = k + 1;
            arrayOfByte[j] = (byte) (0xFF & Integer.decode(
                    "0x" + paramString.substring(j * 2, k)
                            + paramString.substring(k, l)).intValue());
            ++j;
        }
    }

    // 将字节数组转化为16进制字符串，不确定长度
    public static String Bytes2HexString(byte[] b) {
        String ret = "";
        for (int i =0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);// 将高24位置0
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            ret += hex.toUpperCase();
        }
        return ret;
    }



    // 将字节数组转化为16进制字符串，确定长度
    public static String bytesToHexString(byte[] bytes, int a) {
        String result = "";
        for (int i = 0; i < a; i++) {
            String hexString = Integer.toHexString(bytes[i] & 0xFF);// 将高24位置0
            if (hexString.length() == 1) {
                hexString = '0' + hexString;
            }
            result += hexString.toUpperCase()+" ";
        }
        return result;
    }


    // 该Handler从BluetoothChatService中获取信息
    private final Handler mHandler = new Handler(Objects.requireNonNull(Looper.myLooper())) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if (D)
                        Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);


                    switch (msg.arg1) {
                        case BluetoothChatServiceX.STATE_CONNECTED:
                            incount.setText("已连接");
                            incount.append(mConnectedDeviceName);
                            mConversationView.setText(null);
                            break;

                        case BluetoothChatServiceX.STATE_CONNECTING:
                            incount.setText("正在连接。。。");
                            break;

                        case BluetoothChatServiceX.STATE_LISTEN:
                        case BluetoothChatServiceX.STATE_NONE:
                            incount.setText("没有连接");
                            break;
                    }
                    break;

                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // 自动发送
                    if (auto) {
                        // 自动发送模块
                        mHandler.postDelayed(runnable, 1000);
                    } else {
                        mHandler.removeCallbacks(runnable);
                    }
                    // 发送计数
                    if (outhex) {
                        String writeMessage = Bytes2HexString(writeBuf);
                        countout += writeMessage.length() / 2;
                        outcount.setText("" + countout);
                    } else {
                        String writeMessage = null;
                        try {
                            writeMessage = new String(writeBuf, "GBK");
                        } catch (UnsupportedEncodingException e1) {
                            e1.printStackTrace();
                        }
                        assert writeMessage != null;
                        countout += writeMessage.length();
                        outcount.setText("" + countout);
                    }
                    break;
                case MESSAGE_READ:
                  //  dataStr=new StringBuffer();
                    byte[] readBuf = (byte[]) msg.obj;

                    //检错误码计算函数

                    if (inhex == true) {
                       // dataStr.setLength(0);
                    //    mConversationView.setText(null);
                        String readMessage = bytesToHexString(readBuf, msg.arg1)+"*";
                        mConversationView.append(readMessage);
                      //  dataStr.append(mConversationView.getText().toString());
                        Log.d("DataStr", mConversationView.getText().toString());
                        // 接收计数，更显UI
//                        countin += readMessage.length() / 2;
//                        incount.setText("" + countin);
                     //   mConversationView.setText(null);
                    } else {
                        String readMessage = null;
                        try {
                            readMessage = new String(readBuf, 0, msg.arg1, "GBK");
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        mConversationView.append(readMessage);
                        // 接收计数，更新UI
                        assert readMessage != null;
                        countin += readMessage.length();
                        incount.setText("" + countin);
                    }
                    break;
                case MESSAGE_DEVICE_NAME:
                    // 保存已连接设备的名称
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(),
                                    "连接到 " + mConnectedDeviceName, Toast.LENGTH_SHORT)
                            .show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(),
                                    msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
                            .show();
                    break;
            }
        }
    };


    //自动连接
    @SuppressLint("MissingPermission")
    public void AutoConnect() {
        try {
            String address = "00:1B:10:F1:EE:7E";
            BluetoothDevice device = mBluetoothAdapter
                    .getRemoteDevice(address);
            mChatService.connect(device);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 保存按键响应函数
    public void onSaveButtonClicked(View v) {
       // Save();
    }

    // 清屏按键响应函数
    public void onClearButtonClicked(View v) {
        mConversationView.setText(null);
        view.setText(null);
        return;
    }

    // 清除计数按键响应函数
    public void onClearCountButtonClicked(View v) {
        countin = 0;
        countout = 0;
        outcount.setText("0");
        incount.setText("0");
        return;
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (D)
            Log.e(TAG, "+ ON RESUME +");
        // 在onResume（）中执行此检查包括在onStart（）期间未启用BT的情况，
        // 因此我们暂停启用它...
        // onResume（）将在ACTION_REQUEST_ENABLE活动时被调用返回.
        if (mChatService != null) {
            // 只有状态是STATE_NONE，我们知道我们还没有启动蓝牙
            if (mChatService.getState() == BluetoothChatServiceX.STATE_NONE) {
                // 启动BluetoothChat服务
                mChatService.start();
            }
        }

    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if (D)
            Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if (D)
            Log.e(TAG, "-- ON STOP --");
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        // 停止蓝牙通信连接服务
        if (mChatService != null)
            mChatService.stop();
        if (D)
            Log.e(TAG, "--- ON DESTROY ---");
    }

    @Override
    public void onBackPressed() {
        exit();
    }

    public void exit() {
        //返回页面标志
        boolean exit = true;
        this.finish();
    }
}