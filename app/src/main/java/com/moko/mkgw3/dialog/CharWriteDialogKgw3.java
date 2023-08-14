package com.moko.mkgw3.dialog;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.moko.mkgw3.R;
import com.moko.mkgw3.databinding.DialogCharWriteKgw3Binding;

public class CharWriteDialogKgw3 extends MokoBaseDialog<DialogCharWriteKgw3Binding> {
    public static final String TAG = CharWriteDialogKgw3.class.getSimpleName();

    @Override
    protected DialogCharWriteKgw3Binding getViewBind(LayoutInflater inflater, ViewGroup container) {
        return DialogCharWriteKgw3Binding.inflate(inflater, container, false);
    }

    @Override
    protected void onCreateView() {
        mBind.tvCancel.setOnClickListener(v -> {
            dismiss();
        });
        mBind.tvEnsure.setOnClickListener(v -> {
            dismiss();
            String password = mBind.etPayload.getText().toString();
            if (charWriteClickListener != null)
                charWriteClickListener.onEnsureClicked(password);
        });
        mBind.etPayload.postDelayed(() -> {
            //设置可获得焦点
            mBind.etPayload.setFocusable(true);
            mBind.etPayload.setFocusableInTouchMode(true);
            //请求获得焦点
            mBind.etPayload.requestFocus();
            //调用系统输入法
            InputMethodManager inputManager = (InputMethodManager) mBind.etPayload
                    .getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.showSoftInput(mBind.etPayload, 0);
        }, 200);
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

    private CharWriteClickListener charWriteClickListener;

    public void setOnCharWriteClicked(CharWriteClickListener charWriteClickListener) {
        this.charWriteClickListener = charWriteClickListener;
    }

    public interface CharWriteClickListener {

        void onEnsureClicked(String payload);
    }
}
