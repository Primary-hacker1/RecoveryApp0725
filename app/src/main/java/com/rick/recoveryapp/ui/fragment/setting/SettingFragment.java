/*
 * Copyright (C) 2021 xuexiangjys(xuexiangjys@163.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.rick.recoveryapp.ui.fragment.setting;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.efs.sdk.base.core.util.Log;
import com.rick.recoveryapp.R;
import com.rick.recoveryapp.ui.dialog.MacDrDialog;
import com.rick.recoveryapp.ui.activity.bean.AddressBean;
import com.rick.recoveryapp.ui.activity.bean.SharedPreferencesUtils;
import com.rick.recoveryapp.ui.adapter.BtDevAdapter;
import com.rick.recoveryapp.ui.activity.helper.BtDataProX;
import com.rick.recoveryapp.ui.service.BtReceiver;
import com.rick.recoveryapp.databinding.FragmentSettingBinding;
import com.rick.recoveryapp.greendao.MacDrDao;
import com.rick.recoveryapp.utils.LocalConfig;
import com.rick.recoveryapp.utils.XToastUtils;
import com.xuexiang.xui.utils.KeyboardUtils;
import com.xuexiang.xui.widget.dialog.DialogLoader;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;
import com.xuexiang.xui.widget.dialog.materialdialog.simplelist.MaterialSimpleListAdapter;
import com.xuexiang.xui.widget.dialog.materialdialog.simplelist.MaterialSimpleListItem;
import com.xuexiang.xui.widget.dialog.strategy.InputInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SettingFragment extends Fragment implements BtReceiver.Listener, BtDevAdapter.Listener {

    public static SettingFragment settingFragment;

    FragmentSettingBinding binding;
    MacDrDao macDrDao;
    public static BtReceiver mBtReceiver;
    private final BtDevAdapter mBtDevAdapter = new BtDevAdapter(this);
    int i;//记录点击次数
    long time;//记录时间差
    String flag;//标记点击
    Boolean isLock = true;
    BtDataProX btDataPro;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        settingFragment = this;
        binding = FragmentSettingBinding.inflate(getLayoutInflater(), container, false);
        macDrDao = LocalConfig.daoSession.getMacDrDao();
        btDataPro = new BtDataProX();
        initClinck();
        GetMac();


        mBtReceiver = new BtReceiver(LocalConfig.SettingContext, this);//注册蓝牙广播

        return binding.getRoot();
    }

    public void initClinck() {
        binding.settingBtnUnlock.setOnClickListener(v -> DialogLoader.getInstance().showInputDialog(
                LocalConfig.SettingContext,
                R.drawable.icon_warning,
                getString(R.string.tip_warning),
                getString(R.string.content_warning),
                new InputInfo(InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        | InputType.TYPE_TEXT_FLAG_CAP_WORDS,
                        getString(R.string.hint_please_input_password)),
                (dialog, input) -> XToastUtils.toast(input.toString()),
                getString(R.string.lab_continue),
                (dialog, which) -> {
                    KeyboardUtils.hideSoftInput(dialog);
                    dialog.dismiss();
                    if (dialog instanceof MaterialDialog) {
                        assert ((MaterialDialog) dialog).getInputEditText() != null;
                        String changePass = ((MaterialDialog) dialog).getInputEditText().getText().toString();
                        if (changePass.equals("654321")) {
                            Intent in = new Intent(LocalConfig.SettingContext, MacDrDialog.class);
                            in.putExtra("isfer", "setting");
                            startActivity(in);

                        } else {
                            Toast.makeText(LocalConfig.SettingContext, "密码输入错误！", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                getString(R.string.lab_change),
                (dialog, which) -> {
                    KeyboardUtils.hideSoftInput(dialog);
                    dialog.dismiss();
                }));
    }

    public void GetMac() {
        AddressBean addressBean = SharedPreferencesUtils.Companion.getInstance().getAddressString();

        if (addressBean != null) {
            binding.settingMacBluetooth.setCenterString(addressBean.getMacAddress());
            binding.settingMacEcgmac.setCenterString(addressBean.getEcg());
            binding.settingMacBloodmac.setCenterString(addressBean.getBloodPressure());
            binding.settingMacOxygenmac.setCenterString(addressBean.getBloodOxygen());
        }
    }

    //
    @Override
    public void onItemClick(BluetoothDevice dev) {
        i++;
        time = System.currentTimeMillis() - time;//记录时间差
        if (i == 1) {
            flag = dev.getAddress();//第一次点击，获取当前position
            Log.d("onItemClick", "第一次点击" + flag);
        } else {
            //第二次点击，判断是否是当前position
            if (Objects.equals(flag, dev.getAddress())) {
                if (time < 1000) {
                    //是双击操作
                    i = 0;
                    time = 0;
                    Log.d("onItemClick", "双击了" + flag);
                    if (isLock) {
                        Toast.makeText(LocalConfig.SettingContext, "请先解锁按钮！", Toast.LENGTH_SHORT).show();
                    } else {

                        showPictureItemDialog(dev.getAddress());
                    }

                } else {
                    //不是双击操作
                    i = 0;
                    time = 0;
                    flag = dev.getAddress();
                    i++;//记录点击次数
                    time = System.currentTimeMillis() - time;//继续记录时间差
                    Log.d("onItemClick", "2次点击时间间隔大于1s" + flag);
                }
            } else {
                //不是，初始化
                i = 0;
                time = 0;
                flag = dev.getAddress();
                i++;//记录点击次数
                time = System.currentTimeMillis() - time;//记录时间差
            }
        }
    }

    /**
     * 带图标条目的Dialog
     */
    private void showPictureItemDialog(String mac) {
        List<MaterialSimpleListItem> list = new ArrayList<>();
        list.add(new MaterialSimpleListItem.Builder(getContext())
                .content(R.string.lab_mac)
                .icon(R.drawable.mac)
                .build());
        list.add(new MaterialSimpleListItem.Builder(getContext())
                .content(R.string.lab_ecg)
                .icon(R.drawable.bpm)
                .build());
        list.add(new MaterialSimpleListItem.Builder(getContext())
                .content(R.string.lab_blood)
                .icon(R.drawable.mmhg_s)
                .build());
        list.add(new MaterialSimpleListItem.Builder(getContext())
                .content(R.string.lab_oxygen)
                .icon(R.drawable.hbo2)
                .build());
        final MaterialSimpleListAdapter adapter = new MaterialSimpleListAdapter(list)
                .setOnItemClickListener((dialog, index, item) -> ChangeTxt(item.getContent().toString(), mac));
        new MaterialDialog.Builder(getContext()).adapter(adapter, null).show();
    }

    public void ChangeTxt(String content, String mac) {
        switch (content) {
            case "主板蓝牙":
                binding.settingMacBluetooth.setCenterString(mac);
                break;

            case "心电蓝牙":
                binding.settingMacEcgmac.setCenterString(mac);
                break;

            case "血压蓝牙":
                binding.settingMacBloodmac.setCenterString(mac);
                break;

            case "血氧蓝牙":
                binding.settingMacOxygenmac.setCenterString(mac);
                break;

        }

    }

    @Override
    public void foundDev(BluetoothDevice dev) {
        mBtDevAdapter.add(dev);
    }

    @Override
    public void foundBT() {

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
