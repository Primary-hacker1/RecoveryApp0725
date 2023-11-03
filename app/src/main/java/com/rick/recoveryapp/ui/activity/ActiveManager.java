package com.rick.recoveryapp.ui.activity;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.common.base.BaseFrameLayout;
import com.rick.recoveryapp.R;
import com.rick.recoveryapp.ui.activity.serial.SerialPort;
import com.rick.recoveryapp.databinding.ActivityManagerBinding;


public class ActiveManager extends BaseFrameLayout<ActivityManagerBinding> {

    public ActivityManagerBinding managerBinding;

    public ActiveManager(@NonNull Context context) {
        super(context);
    }

    public ActiveManager(@NonNull Context context, @NonNull AttributeSet attributes) {
        super(context, attributes);
    }

    public ActiveManager(@NonNull Context context, @NonNull AttributeSet attributes, int i) {
        super(context, attributes, i);
    }

    public void setManagerBinding(ActivityManagerBinding binding){
        this.managerBinding = binding;
    }

    @Override
    public void initView() {
    }

    public void initViewType(SerialPort.Type type) {//不同模式不同界面

        if (type == SerialPort.Type.ACTIVE) {
            managerBinding.activeTxtZhuansu.setCenterString("0");
            managerBinding.activeTxtResistance.setCenterString("1");
            managerBinding.activeTxtSpasm.setCenterString("0");

            managerBinding.stxActiveTitle.setBottomDividerLineVisibility(View.VISIBLE);
            managerBinding.stxPressTitle.setBottomDividerLineVisibility(View.GONE);
            managerBinding.stxIntelligenceTitle.setBottomDividerLineVisibility(View.GONE);
            managerBinding.llTimeControl.setVisibility(View.GONE);
            managerBinding.llRpmActive.setVisibility(View.VISIBLE);//运动rpm布局不需要
            managerBinding.llRpmIntelligence.setVisibility(View.GONE);//智能被动rpm布局也需要
            managerBinding.llSpasm.setVisibility(View.INVISIBLE);//痉挛等级
            managerBinding.llResistance.setVisibility(View.VISIBLE);//阻力等级

            managerBinding.activeTxtZhuansu.setCenterTextColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.activeTxtResistance.setCenterTextColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.activeTxtSpasm.setCenterTextColor(ContextCompat.getColor(mContext, R.color.progressLightGray));
            managerBinding.activeTxtLeft.setCenterTextColor(ContextCompat.getColor(mContext, R.color.Progress_bule));//左肢
            managerBinding.activeTxtRight.setCenterTextColor(ContextCompat.getColor(mContext, R.color.Progress_bule));//右肢
            managerBinding.stxLeftLimb.setCenterTextColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.stxRightLimb.setCenterTextColor(ContextCompat.getColor(mContext, R.color.Progress_bule));

            managerBinding.progressViewZhuansuActicve.setEndColor(ContextCompat.getColor(mContext, R.color.white));
            managerBinding.progressViewZhuansuActicve.setProgressTextColor(ContextCompat.getColor(mContext, R.color.white));
            managerBinding.progressViewZhuansuActicve.setStartColor(ContextCompat.getColor(mContext, R.color.white));
            managerBinding.progressViewZhuansuActicve.setTrackColor(ContextCompat.getColor(mContext, R.color.progressLightBlue));

            managerBinding.progressViewResistance.setEndColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewResistance.setProgressTextColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewResistance.setStartColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewResistance.setTrackColor(ContextCompat.getColor(mContext, R.color.progressLightBlue));

            managerBinding.progressViewSpasm.setEndColor(ContextCompat.getColor(mContext, R.color.white));
            managerBinding.progressViewSpasm.setProgressTextColor(ContextCompat.getColor(mContext, R.color.white));
            managerBinding.progressViewSpasm.setStartColor(ContextCompat.getColor(mContext, R.color.white));
            managerBinding.progressViewSpasm.setTrackColor(ContextCompat.getColor(mContext, R.color.progressLightGray));

            managerBinding.progressViewLeft.setEndColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewLeft.setProgressTextColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewLeft.setStartColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewLeft.setTrackColor(ContextCompat.getColor(mContext, R.color.progressLightBlue));

            managerBinding.progressViewRight.setEndColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewRight.setProgressTextColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewRight.setStartColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewRight.setTrackColor(ContextCompat.getColor(mContext, R.color.progressLightBlue));
        }

        if (type == SerialPort.Type.SUBJECT) {
            managerBinding.activeWaveShowView.resetCanavas();
            managerBinding.activeTxtZhuansu.setCenterString("5");
            managerBinding.activeTxtSpasm.setCenterString("0");

            managerBinding.stxActiveTitle.setBottomDividerLineVisibility(View.GONE);
            managerBinding.stxPressTitle.setBottomDividerLineVisibility(View.VISIBLE);
            managerBinding.stxIntelligenceTitle.setBottomDividerLineVisibility(View.GONE);
            managerBinding.llTimeControl.setVisibility(View.VISIBLE);
            managerBinding.llRpmActive.setVisibility(View.GONE);
            managerBinding.llRpmIntelligence.setVisibility(View.VISIBLE);
            managerBinding.llSpasm.setVisibility(View.VISIBLE);//痉挛等级
            managerBinding.llResistance.setVisibility(View.INVISIBLE);

            managerBinding.activeTxtZhuansu.setCenterTextColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.activeTxtResistance.setCenterTextColor(ContextCompat.getColor(mContext, R.color.white));
            managerBinding.activeTxtSpasm.setCenterTextColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.activeTxtLeft.setCenterTextColor(ContextCompat.getColor(mContext, R.color.white));//左肢
            managerBinding.activeTxtRight.setCenterTextColor(ContextCompat.getColor(mContext, R.color.white));//右肢
            managerBinding.stxLeftLimb.setCenterTextColor(ContextCompat.getColor(mContext, R.color.white));
            managerBinding.stxRightLimb.setCenterTextColor(ContextCompat.getColor(mContext, R.color.white));

            managerBinding.progressViewZhuansuActicve.setEndColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewZhuansuActicve.setProgressTextColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewZhuansuActicve.setStartColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewZhuansuActicve.setTrackColor(ContextCompat.getColor(mContext, R.color.progressLightBlue));

            managerBinding.progressViewResistance.setEndColor(ContextCompat.getColor(mContext, R.color.white));
            managerBinding.progressViewResistance.setProgressTextColor(ContextCompat.getColor(mContext, R.color.white));
            managerBinding.progressViewResistance.setStartColor(ContextCompat.getColor(mContext, R.color.white));
            managerBinding.progressViewResistance.setTrackColor(ContextCompat.getColor(mContext, R.color.progressLightGray));

            managerBinding.progressViewSpasm.setEndColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewSpasm.setProgressTextColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewSpasm.setStartColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewSpasm.setTrackColor(ContextCompat.getColor(mContext, R.color.progressLightBlue));

            managerBinding.progressViewLeft.setEndColor(ContextCompat.getColor(mContext, R.color.progressLightGray));
            managerBinding.progressViewLeft.setProgressTextColor(ContextCompat.getColor(mContext, R.color.progressLightGray));
            managerBinding.progressViewLeft.setStartColor(ContextCompat.getColor(mContext, R.color.progressLightGray));
            managerBinding.progressViewLeft.setTrackColor(ContextCompat.getColor(mContext, R.color.progressLightGray));

            managerBinding.progressViewRight.setEndColor(ContextCompat.getColor(mContext, R.color.progressLightGray));
            managerBinding.progressViewRight.setProgressTextColor(ContextCompat.getColor(mContext, R.color.progressLightGray));
            managerBinding.progressViewRight.setStartColor(ContextCompat.getColor(mContext, R.color.progressLightGray));
            managerBinding.progressViewRight.setTrackColor(ContextCompat.getColor(mContext, R.color.progressLightGray));
        }

        if (type == SerialPort.Type.INTELLIGENT) {

            managerBinding.stxActiveTitle.setBottomDividerLineVisibility(View.GONE);
            managerBinding.stxPressTitle.setBottomDividerLineVisibility(View.GONE);
            managerBinding.stxIntelligenceTitle.setBottomDividerLineVisibility(View.VISIBLE);
            managerBinding.llRpmActive.setVisibility(View.GONE);
            managerBinding.llRpmIntelligence.setVisibility(View.VISIBLE);
            managerBinding.llSpasm.setVisibility(View.VISIBLE);//痉挛等级
            managerBinding.llResistance.setVisibility(View.VISIBLE);

            managerBinding.activeTxtZhuansu.setCenterTextColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.activeTxtResistance.setCenterTextColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.activeTxtSpasm.setCenterTextColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.activeTxtLeft.setCenterTextColor(ContextCompat.getColor(mContext, R.color.Progress_bule));//左肢
            managerBinding.activeTxtRight.setCenterTextColor(ContextCompat.getColor(mContext, R.color.Progress_bule));//右肢
            managerBinding.stxLeftLimb.setCenterTextColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.stxRightLimb.setCenterTextColor(ContextCompat.getColor(mContext, R.color.Progress_bule));

            managerBinding.progressViewZhuansuActicve.setEndColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewZhuansuActicve.setProgressTextColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewZhuansuActicve.setStartColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewZhuansuActicve.setTrackColor(ContextCompat.getColor(mContext, R.color.progressLightBlue));

            managerBinding.progressViewResistance.setEndColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewResistance.setProgressTextColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewResistance.setStartColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewResistance.setTrackColor(ContextCompat.getColor(mContext, R.color.progressLightBlue));

            managerBinding.progressViewSpasm.setEndColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewSpasm.setProgressTextColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewSpasm.setStartColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewSpasm.setTrackColor(ContextCompat.getColor(mContext, R.color.progressLightBlue));

            managerBinding.progressViewLeft.setEndColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewLeft.setProgressTextColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewLeft.setStartColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewLeft.setTrackColor(ContextCompat.getColor(mContext, R.color.progressLightBlue));

            managerBinding.progressViewRight.setEndColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewRight.setProgressTextColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewRight.setStartColor(ContextCompat.getColor(mContext, R.color.Progress_bule));
            managerBinding.progressViewRight.setTrackColor(ContextCompat.getColor(mContext, R.color.progressLightBlue));
        }
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_manager;
    }

}
