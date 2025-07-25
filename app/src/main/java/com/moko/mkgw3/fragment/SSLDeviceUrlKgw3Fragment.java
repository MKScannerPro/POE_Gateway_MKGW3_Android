package com.moko.mkgw3.fragment;

import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.moko.mkgw3.activity.modify.ModifyMQTTSettingsKgw3Activity;
import com.moko.mkgw3.databinding.FragmentSslDeviceUrlKgw3Binding;
import com.moko.lib.scannerui.dialog.BottomDialog;

import java.util.ArrayList;

public class SSLDeviceUrlKgw3Fragment extends Fragment {

    private static final String TAG = SSLDeviceUrlKgw3Fragment.class.getSimpleName();
    private FragmentSslDeviceUrlKgw3Binding mBind;


    private ModifyMQTTSettingsKgw3Activity activity;

    private int mConnectMode = 0;

    private String caUrl;
    private String clientKeyUrl;
    private String clientCertUrl;

    private ArrayList<String> values;
    private int selected;

    public SSLDeviceUrlKgw3Fragment() {
    }

    public static SSLDeviceUrlKgw3Fragment newInstance() {
        SSLDeviceUrlKgw3Fragment fragment = new SSLDeviceUrlKgw3Fragment();
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
        mBind = FragmentSslDeviceUrlKgw3Binding.inflate(inflater, container, false);
        activity = (ModifyMQTTSettingsKgw3Activity) getActivity();
        mBind.clCertificate.setVisibility(mConnectMode > 0 ? View.VISIBLE : View.GONE);
        mBind.cbSsl.setChecked(mConnectMode > 0);
        mBind.cbSsl.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                mConnectMode = 0;
            } else {
                mConnectMode = selected + 1;
            }
            mBind.clCertificate.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
        values = new ArrayList<>();
        values.add("CA signed server certificate");
        values.add("CA certificate file");
        values.add("Self signed certificates");
        mBind.etCaUrl.setFilters(new InputFilter[]{new InputFilter.LengthFilter(256), activity.filter});
        mBind.etClientKeyUrl.setFilters(new InputFilter[]{new InputFilter.LengthFilter(256), activity.filter});
        mBind.etClientCertUrl.setFilters(new InputFilter[]{new InputFilter.LengthFilter(256), activity.filter});
        if (mConnectMode > 0) {
            selected = mConnectMode - 1;
            mBind.etCaUrl.setText(caUrl);
            mBind.etClientKeyUrl.setText(clientKeyUrl);
            mBind.etClientCertUrl.setText(clientCertUrl);
            mBind.tvCertification.setText(values.get(selected));
        }
        if (selected == 0) {
            mBind.llCa.setVisibility(View.GONE);
            mBind.llClientKey.setVisibility(View.GONE);
            mBind.llClientCert.setVisibility(View.GONE);
        } else if (selected == 1) {
            mBind.llCa.setVisibility(View.VISIBLE);
            mBind.llClientKey.setVisibility(View.GONE);
            mBind.llClientCert.setVisibility(View.GONE);
        } else if (selected == 2) {
            mBind.llCa.setVisibility(View.VISIBLE);
            mBind.llClientKey.setVisibility(View.VISIBLE);
            mBind.llClientCert.setVisibility(View.VISIBLE);
        }
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

    public void setConnectMode(int connectMode) {
        this.mConnectMode = connectMode;
        if (mBind == null)
            return;
        mBind.clCertificate.setVisibility(mConnectMode > 0 ? View.VISIBLE : View.GONE);
        if (mConnectMode > 0) {
            selected = mConnectMode - 1;
            mBind.etCaUrl.setText(caUrl);
            mBind.etClientKeyUrl.setText(clientKeyUrl);
            mBind.etClientCertUrl.setText(clientCertUrl);
            mBind.tvCertification.setText(values.get(selected));
        }
        mBind.cbSsl.setChecked(mConnectMode > 0);
        if (selected == 0) {
            mBind.llCa.setVisibility(View.GONE);
            mBind.llClientKey.setVisibility(View.GONE);
            mBind.llClientCert.setVisibility(View.GONE);
        } else if (selected == 1) {
            mBind.llCa.setVisibility(View.VISIBLE);
            mBind.llClientKey.setVisibility(View.GONE);
            mBind.llClientCert.setVisibility(View.GONE);
        } else if (selected == 2) {
            mBind.llCa.setVisibility(View.VISIBLE);
            mBind.llClientKey.setVisibility(View.VISIBLE);
            mBind.llClientCert.setVisibility(View.VISIBLE);
        }
    }

    public void setCAUrl(String caUrl) {
        this.caUrl = caUrl;
        if (mBind == null)
            return;
        mBind.etCaUrl.setText(caUrl);
    }

    public void setClientKeyUrl(String clientKeyUrl) {
        this.clientKeyUrl = clientKeyUrl;
        if (mBind == null)
            return;
        mBind.etClientKeyUrl.setText(clientKeyUrl);
    }

    public void setClientCertUrl(String clientCertUrl) {
        this.clientCertUrl = clientCertUrl;
        if (mBind == null)
            return;
        mBind.etClientCertUrl.setText(clientCertUrl);
    }

    public void selectCertificate() {
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(values, selected);
        dialog.setListener(value -> {
            selected = value;
            mBind.tvCertification.setText(values.get(selected));
            if (selected == 0) {
                mConnectMode = 1;
                mBind.llCa.setVisibility(View.GONE);
                mBind.llClientKey.setVisibility(View.GONE);
                mBind.llClientCert.setVisibility(View.GONE);
            } else if (selected == 1) {
                mConnectMode = 2;
                mBind.llCa.setVisibility(View.VISIBLE);
                mBind.llClientKey.setVisibility(View.GONE);
                mBind.llClientCert.setVisibility(View.GONE);
            } else if (selected == 2) {
                mConnectMode = 3;
                mBind.llCa.setVisibility(View.VISIBLE);
                mBind.llClientKey.setVisibility(View.VISIBLE);
                mBind.llClientCert.setVisibility(View.VISIBLE);
            }
        });
        dialog.show(activity.getSupportFragmentManager());
    }

    public int getConnectMode() {
        return mConnectMode;
    }

    public String getCAUrl() {
        return mBind.etCaUrl.getText().toString();
    }

    public String getClientCertUrl() {
        return mBind.etClientCertUrl.getText().toString();
    }

    public String getClientKeyUrl() {
        return mBind.etClientKeyUrl.getText().toString();
    }
}
