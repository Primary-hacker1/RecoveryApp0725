package com.example.usbprint.print;



/**
 * 打印机连接回调接口实现类
 */
public class RunPrintServiceImp implements
        com.dynamixsoftware.printingsdk.IServiceCallback {
    private String TAG = this.getClass().getName();

    private ServiceConnected connected;

    public RunPrintServiceImp(ServiceConnected connected) {
        this.connected = connected;
    }

    @Override
    public void onServiceConnected() {
        MLog.e("连接打印机服务成功");
        connected.serviceConnectedStatus(true);
    }

    @Override
    public void onServiceDisconnected() {
        MLog.e( "打印机服务断开连接");
        connected.serviceConnectedStatus(false);
    }

}
