package com.rick.recoveryapp.ui.service;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

import com.common.network.LogUtils;
import com.rick.recoveryapp.ui.BaseApplication;
import com.rick.recoveryapp.ui.activity.helper.Constants;
import com.rick.recoveryapp.entity.LiveMessage;
import com.rick.recoveryapp.entity.RDMessage;
import com.rick.recoveryapp.utils.LiveDataBus;
import com.rick.recoveryapp.utils.LocalConfig;

import java.util.Timer;
import java.util.TimerTask;

/**
 * 监听蓝牙广播-各种状态
 */
public class BtReceiver extends BroadcastReceiver {
    private static final String TAG = BtReceiver.class.getSimpleName();
    private final Listener mListener;
    public static LiveMessage liveMessage = null;
    public static RDMessage rdMessage = null;

    public BtReceiver(Context cxt, Listener listener) {
        mListener = listener;
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);//蓝牙开关状态
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);//蓝牙开始搜索
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);//蓝牙搜索结束

        filter.addAction(BluetoothDevice.ACTION_FOUND);//蓝牙发现新设备(未配对的设备)
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);//在系统弹出配对框之前(确认/输入配对码)
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);//设备配对状态改变
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);//最底层连接建立
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);//最底层连接断开

        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED); //BluetoothAdapter连接状态
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED); //BluetoothHeadset连接状态
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED); //BluetoothA2dp连接状态
        cxt.registerReceiver(this, filter);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null)
            return;
        Log.i(TAG, "===" + action);
        BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (dev != null)
            Log.i(TAG, "BluetoothDevice: " + dev.getName() + ", " + dev.getAddress());
        switch (action) {
            case BluetoothAdapter.ACTION_STATE_CHANGED:
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                if (state == 10) {
                    LocalConfig.isControl = false;
                    liveMessage = new LiveMessage();
                    liveMessage.setIsConnt(false);
                    liveMessage.setState("蓝牙设备未连接");
                    BaseApplication.mConnectedDeviceName = "";
                    LiveDataBus.get().with(Constants.BT_CONNECTED).setValue(liveMessage);
                }

                if (state == 12) {
//                    LocalConfig.isControl = true;
//                    mListener.foundBT();
//                    rdMessage = new RDMessage();
//                    rdMessage.setIsConnt(false);
//                    rdMessage.setState("蓝牙设备已重联");
//                    LiveEventBus.get("BT_RECONNECTED")
//                            .post(rdMessage);
                }


                LogUtils.e(TAG + "STATE:  " + state + "---" + "LocalConfig.isControl= "
                        + LocalConfig.isControl);
                break;
            case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                LogUtils.e(TAG + "BluetoothAdapter.ACTION_DISCOVERY_STARTED");
                break;
            case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                LogUtils.e(TAG + "BluetoothAdapter.ACTION_DISCOVERY_FINISHED");
                break;

            case BluetoothDevice.ACTION_FOUND:
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MAX_VALUE);
                Log.i(TAG, "EXTRA_RSSI:" + rssi);
                LogUtils.e(TAG + "BluetoothAdapter.ACTION_FOUND");
                mListener.foundDev(dev);
                break;
            case BluetoothDevice.ACTION_PAIRING_REQUEST: //在系统弹出配对框之前，实现自动配对，取消系统配对框
                try {
                    abortBroadcast();//终止配对广播，取消系统配对框
                    assert dev != null;
                    boolean ret = dev.setPin("1234".getBytes()); //设置PIN配对码(必须是固定的)
                } catch (Exception e) {
                    e.printStackTrace();
                }
                LogUtils.e(TAG + "BluetoothAdapter.ACTION_PAIRING_REQUEST");
                break;
            case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                LogUtils.e(TAG + "BluetoothAdapter.ACTION_BOND_STATE_CHANGED");
                Log.i(TAG, "BOND_STATE: " + intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, 0));
                break;
            case BluetoothDevice.ACTION_ACL_CONNECTED:
                LogUtils.e(TAG + "已连接到 " + BluetoothDevice.ACTION_ACL_CONNECTED);
                LocalConfig.isControl = true;

                rdMessage = new RDMessage();
                rdMessage.setIsConnt(false);
                rdMessage.setState("蓝牙设备已重联");
                LiveDataBus.get().with(Constants.BT_RECONNECTED).postValue(rdMessage);

                break;
            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                liveMessage = new LiveMessage();
                liveMessage.setIsConnt(false);
                liveMessage.setState("蓝牙设备未连接");
                BaseApplication.mConnectedDeviceName = "";
                LocalConfig.isControl = false;
                LiveDataBus.get().with(Constants.BT_CONNECTED).setValue(liveMessage);

                // intent = new Intent();
//                intent = new Intent(BluetoothDevice.ACTION_ACL_DISCONNECTED);
//                context.sendBroadcast(intent);
                Toast.makeText(context, "蓝牙设备已断开", Toast.LENGTH_SHORT).show();
                break;

            case BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED:
                LogUtils.e(TAG + "BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED");
                Log.i(TAG, "CONN_STATE: " + intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, 0));
                break;
            case BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED:
            case BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED:
                LogUtils.e(TAG + "BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED+ACTION_CONNECTION_STATE_CHANGED");
                Log.i(TAG, "CONN_STATE: " + intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, 0));
                break;
        }
    }

    public interface Listener {
        void foundDev(BluetoothDevice dev);

        void foundBT();
    }
}