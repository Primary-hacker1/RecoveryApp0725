package com.example.usbprint.print;

import static com.example.usbprint.print.MLog.e;

import android.os.RemoteException;

import com.dynamixsoftware.intentapi.IPrintCallback;
import com.dynamixsoftware.intentapi.Result;
import com.dynamixsoftware.intentapi.ResultType;

/**
 * @Description 打印机打印工作流程回调实现类
 * @Author 黄瑞欣
 * @Date 2022/3/25 11:26
 * @Version 1.0
 */
public class PrintCallBackImp extends IPrintCallback.Stub {

    private PrintCallBack printCallBack;

    public void setPrintCallBack(PrintCallBack callBack) {
        printCallBack = callBack;
    }

    @Override
    public void start() {
          e("start");
    }

    @Override
    public void startingPrintJob() {
          e("startingPrintJob");
    }

    @Override
    public void preparePage(int pageNum) {
          e("preparePage" + pageNum);
    }

    @Override
    public void sendingPage(int pageNum, int progress) {
        if (progress == 100) {
            printCallBack.onPrintStatus(true);
        }
          e("sendingPage" + progress);
    }

    @Override
    public boolean needCancel() {
          e("needCancel");
        return false;
    }

    @Override
    public void finishingPrintJob() {
          e("finishingPrintJob");
        printCallBack.onPrintStatus(true);
    }

    @Override
    public void finish(Result result, int pagesPrinted) throws RemoteException {
        if (result.getType() == ResultType.OK) {
              e("finish");
            printCallBack.onPrintStatus(true);
        }
    }
    

}
