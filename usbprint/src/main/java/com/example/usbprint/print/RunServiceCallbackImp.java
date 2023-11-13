package com.example.usbprint.print;


import com.dynamixsoftware.intentapi.IServiceCallback;
import com.dynamixsoftware.intentapi.Result;

/**
 * @Description 运行打印服务回调
 * @Author 黄瑞欣
 * @Date 2022/3/25 14:03
 * @Version 1.0
 */
public class RunServiceCallbackImp extends IServiceCallback.Stub {
    private PrintCallBack printCallBack;
    private boolean isOpen = false;

    public void setPrintCallBack(PrintCallBack callBack) {
        printCallBack = callBack;
        isOpen = false;
    }


    @Override
    public void onServiceConnected() {

    }

    @Override
    public void onServiceDisconnected() {
        MLog.e("打印机断开连接");
    }

    @Override
    public boolean onRenderLibraryCheck(boolean b, boolean b1) {
        return false;
    }

    /**
     * 下载渲染库,没啥卵用,又不安装
     */
    @Override
    public void onLibraryDownload(int i) {
    }

    /**
     * 打开了要打印的文件,PS:在咱们项目里面也可以判定为开始打印了,可以做其它操作了
     */
    @Override
    public void onFileOpen(int i, int i1) {
        if (!isOpen) {
            MLog.e("打开文件成功，准备开始打印");
            printCallBack.onPrintStatus(true);
            PrintUtil.printFilePath = "";
            isOpen = true;
        }
    }

    @Override
    public String onPasswordRequired() {
        return "password";
    }

    @Override
    public void onError(Result result) {
        MLog.e("打印机出错");
    }

}
