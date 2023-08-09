package com.moko.mkremotegw.dialog;

import android.content.Context;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.moko.mkremotegw.R;
import com.moko.mkremotegw.databinding.DialogPasswordRemoteBleBinding;

public class PasswordRemoteBleDialog extends MokoBaseDialog<DialogPasswordRemoteBleBinding> {
    public static final String TAG = PasswordRemoteBleDialog.class.getSimpleName();

    private final String FILTER_ASCII = "[ -~]*";

    @Override
    protected DialogPasswordRemoteBleBinding getViewBind(LayoutInflater inflater, ViewGroup container) {
        return DialogPasswordRemoteBleBinding.inflate(inflater, container, false);
    }

    @Override
    protected void onCreateView() {
        InputFilter filter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (!(source + "").matches(FILTER_ASCII)) {
                    return "";
                }

                return null;
            }
        };
        mBind.etPassword.setFilters(new InputFilter[]{new InputFilter.LengthFilter(16), filter});

        mBind.tvPasswordCancel.setOnClickListener(v -> {
            dismiss();
        });
        mBind.tvPasswordEnsure.setOnClickListener(v -> {
            dismiss();
            String password = mBind.etPassword.getText().toString();
            if (passwordClickListener != null)
                passwordClickListener.onEnsureClicked(password);
        });
        mBind.etPassword.postDelayed(() -> {
            //设置可获得焦点
            mBind.etPassword.setFocusable(true);
            mBind.etPassword.setFocusableInTouchMode(true);
            //请求获得焦点
            mBind.etPassword.requestFocus();
            //调用系统输入法
            InputMethodManager inputManager = (InputMethodManager) mBind.etPassword
                    .getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.showSoftInput(mBind.etPassword, 0);
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

    private PasswordClickListener passwordClickListener;

    public void setOnPasswordClicked(PasswordClickListener passwordClickListener) {
        this.passwordClickListener = passwordClickListener;
    }

    public interface PasswordClickListener {

        void onEnsureClicked(String password);
    }
}
