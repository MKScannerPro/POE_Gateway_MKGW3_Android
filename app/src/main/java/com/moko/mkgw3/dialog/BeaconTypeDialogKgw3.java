package com.moko.mkgw3.dialog;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.moko.mkgw3.AppConstants;
import com.moko.mkgw3.R;
import com.moko.mkgw3.databinding.DialogBeaconTypeBinding;
import com.moko.mkgw3.utils.SPUtiles;

public class BeaconTypeDialogKgw3 extends MokoBaseDialog<DialogBeaconTypeBinding> {
    public static final String TAG = BeaconTypeDialogKgw3.class.getSimpleName();
    private int mSelected;

    @Override
    protected DialogBeaconTypeBinding getViewBind(LayoutInflater inflater, ViewGroup container) {
        return DialogBeaconTypeBinding.inflate(inflater, container, false);
    }

    @Override
    protected void onCreateView() {
        mBind.tvCancel.setOnClickListener(v -> {
            dismiss();
        });
        mBind.tvEnsure.setOnClickListener(v -> {
            dismiss();
            SPUtiles.setIntValue(getContext(), AppConstants.SP_BEACON_TYPE, mSelected);
            if (beaconTypeListener != null)
                beaconTypeListener.onEnsureClicked(mSelected);
        });
        mSelected = SPUtiles.getIntValue(getContext(), AppConstants.SP_BEACON_TYPE, 1);
        if (mSelected == 1)
            mBind.rbBXPBD.setChecked(true);
        else if (mSelected == 2)
            mBind.rbBXPBCR.setChecked(true);
        else if (mSelected == 3)
            mBind.rbBXPC.setChecked(true);
        else if (mSelected == 4)
            mBind.rbBXPD.setChecked(true);
        else if (mSelected == 5)
            mBind.rbBXPT.setChecked(true);
        else if (mSelected == 6)
            mBind.rbBXPS.setChecked(true);
        else if (mSelected == 7)
            mBind.rbMKPIR.setChecked(true);
        else if (mSelected == 8)
            mBind.rbMKTOF.setChecked(true);
        else if (mSelected == 0)
            mBind.rbOther.setChecked(true);
        mBind.rgBeaconType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbBXPBD)
                mSelected = 1;
            else if (checkedId == R.id.rbBXPBCR)
                mSelected = 2;
            else if (checkedId == R.id.rbBXPC)
                mSelected = 3;
            else if (checkedId == R.id.rbBXPD)
                mSelected = 4;
            else if (checkedId == R.id.rbBXPT)
                mSelected = 5;
            else if (checkedId == R.id.rbBXPS)
                mSelected = 6;
            else if (checkedId == R.id.rbMKPIR)
                mSelected = 7;
            else if (checkedId == R.id.rbMKTOF)
                mSelected = 8;
            else if (checkedId == R.id.rbOther)
                mSelected = 0;
        });

    }

    @Override
    public int getDialogStyle() {
        return R.style.CenterDialog;
    }

    @Override
    public int getGravity() {
        return Gravity.CENTER;
    }

    @Override
    public String getFragmentTag() {
        return TAG;
    }

    @Override
    public float getDimAmount() {
        return 0.7f;
    }

    @Override
    public boolean getCancelOutside() {
        return false;
    }

    @Override
    public boolean getCancellable() {
        return true;
    }

    private OnBeaconTypeListener beaconTypeListener;

    public void setBeaconTypeListener(OnBeaconTypeListener beaconTypeListener) {
        this.beaconTypeListener = beaconTypeListener;
    }

    public interface OnBeaconTypeListener {

        void onEnsureClicked(int type);
    }
}
