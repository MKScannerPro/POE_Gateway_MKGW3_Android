package com.moko.mkgw3.fragment;

import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.moko.mkgw3.base.BaseActivity;
import com.moko.mkgw3.databinding.FragmentUserAppKgw3Binding;

public class UserKgw3Fragment extends Fragment {
    private final String FILTER_ASCII = "[ -~]*";
    private static final String TAG = UserKgw3Fragment.class.getSimpleName();
    private FragmentUserAppKgw3Binding mBind;


    private BaseActivity activity;
    private String username;
    private String password;

    public UserKgw3Fragment() {
    }

    public static UserKgw3Fragment newInstance() {
        UserKgw3Fragment fragment = new UserKgw3Fragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView: ");
        mBind = FragmentUserAppKgw3Binding.inflate(inflater, container, false);
        activity = (BaseActivity) getActivity();
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }

            return null;
        };
        mBind.etMqttUsername.setFilters(new InputFilter[]{new InputFilter.LengthFilter(256), filter});
        mBind.etMqttPassword.setFilters(new InputFilter[]{new InputFilter.LengthFilter(256), filter});
        mBind.etMqttUsername.setText(username);
        mBind.etMqttPassword.setText(password);
        return mBind.getRoot();
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume: ");
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause: ");
        super.onPause();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: ");
        super.onDestroy();
    }

    public void setUserName(String username) {
        this.username = username;
        if (mBind == null)
            return;
        mBind.etMqttUsername.setText(username);
    }

    public void setPassword(String password) {
        this.password = password;
        if (mBind == null)
            return;
        mBind.etMqttPassword.setText(password);
    }

    public String getUsername() {
        String username = mBind.etMqttUsername.getText().toString();
        return username;
    }

    public String getPassword() {
        String password = mBind.etMqttPassword.getText().toString();
        return password;
    }
}
