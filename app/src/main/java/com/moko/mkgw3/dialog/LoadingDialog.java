package com.moko.mkgw3.dialog;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.moko.mkgw3.R;
import com.moko.mkgw3.databinding.DialogLoadingBinding;
import com.moko.mkgw3.view.ProgressDrawable;

import androidx.core.content.ContextCompat;

public class LoadingDialog extends MokoBaseDialog<DialogLoadingBinding> {
    public static final String TAG = LoadingDialog.class.getSimpleName();

    @Override
    protected DialogLoadingBinding getViewBind(LayoutInflater inflater, ViewGroup container) {
        return DialogLoadingBinding.inflate(inflater, container, false);
    }

    @Override
    protected void onCreateView() {
        ProgressDrawable progressDrawable = new ProgressDrawable();
        progressDrawable.setColor(ContextCompat.getColor(getContext(), R.color.black_333333));
        mBind.ivLoading.setImageDrawable(progressDrawable);
        progressDrawable.start();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ((ProgressDrawable) mBind.ivLoading.getDrawable()).stop();
    }
}