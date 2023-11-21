package com.moko.mkgw3.dialog;

import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.moko.mkgw3.AppConstants;
import com.moko.mkgw3.R;
import com.moko.mkgw3.databinding.DialogLoginBinding;
import com.moko.mkgw3.utils.SPUtiles;
import com.moko.mkgw3.utils.ToastUtils;

public class LoginDialog extends MokoBaseDialog<DialogLoginBinding> {
    public static final String TAG = LoginDialog.class.getSimpleName();


    @Override
    protected DialogLoginBinding getViewBind(LayoutInflater inflater, ViewGroup container) {
        return DialogLoginBinding.inflate(inflater, container, false);
    }

    @Override
    protected void onCreateView() {
        String acc = SPUtiles.getStringValue(getContext(), AppConstants.EXTRA_KEY_LOGIN_ACCOUNT, "");
        String pwd = SPUtiles.getStringValue(getContext(), AppConstants.EXTRA_KEY_LOGIN_PASSWORD, "");
        int env = SPUtiles.getIntValue(getContext(), AppConstants.EXTRA_KEY_LOGIN_ENV, 0);
        mBind.etAccount.setText(acc);
        mBind.etPassword.setText(pwd);
        if (env == 0)
            mBind.rbEnvCloud.setChecked(true);
        else
            mBind.rbEnvTest.setChecked(true);
        mBind.tvCancel.setOnClickListener(v -> {
            dismiss();
        });
        mBind.tvConfirm.setOnClickListener(v -> {
            String account = mBind.etAccount.getText().toString();
            String password = mBind.etPassword.getText().toString();
            if (TextUtils.isEmpty(account) || TextUtils.isEmpty(password)) {
                ToastUtils.showToast(getContext(), "Cannot be empty!");
                return;
            }
            int env_confirm = 0;
            if (mBind.rbEnvCloud.isChecked())
                env_confirm = 0;
            if (mBind.rbEnvTest.isChecked())
                env_confirm = 1;
            dismiss();
            if (loginClickListener != null)
                loginClickListener.onConfirm(account, password, env_confirm);
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

    private LoginClickListener loginClickListener;

    public void setOnLoginClicked(LoginClickListener loginClickListener) {
        this.loginClickListener = loginClickListener;
    }

    public interface LoginClickListener {

        void onConfirm(String account, String password, int env);
    }
}
