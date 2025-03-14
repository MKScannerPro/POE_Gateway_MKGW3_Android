package com.moko.mkgw3.activity.add;

import android.content.Intent;
import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mkgw3.adapter.NearbyWifiKgw3Adapter;
import com.moko.mkgw3.base.BaseActivity;
import com.moko.mkgw3.databinding.ActivityNearbyWifiKgw3Binding;
import com.moko.mkgw3.entity.WifiInfo;
import com.moko.mkgw3.utils.ToastUtils;
import com.moko.support.mkgw3.MokoSupport;
import com.moko.support.mkgw3.OrderTaskAssembler;
import com.moko.support.mkgw3.entity.OrderCHAR;
import com.moko.support.mkgw3.entity.ParamsKeyEnum;
import com.moko.support.mkgw3.entity.ParamsLongKeyEnum;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;

import androidx.recyclerview.widget.LinearLayoutManager;

public class NearbyWifiKgw3Activity extends BaseActivity<ActivityNearbyWifiKgw3Binding> implements BaseQuickAdapter.OnItemClickListener {
    private boolean mSavedParamsError;
    private NearbyWifiKgw3Adapter mAdapter;
    private ArrayList<WifiInfo> mWifiInfoList;

    @Override
    protected void onCreate() {
        mWifiInfoList = new ArrayList<>();
        mAdapter = new NearbyWifiKgw3Adapter();
        mAdapter.openLoadAnimation();
        mAdapter.replaceData(mWifiInfoList);
        mAdapter.setOnItemClickListener(this);
        mBind.rvSsid.setLayoutManager(new LinearLayoutManager(this));
        mBind.rvSsid.setAdapter(mAdapter);
        showLoadingProgressDialog();
        mBind.ivRefresh.postDelayed(() -> {
            MokoSupport.getInstance().sendOrder(OrderTaskAssembler.getNearbyWifi());
        }, 500);
        mBind.ivRefresh.setOnClickListener(v -> {
            showLoadingProgressDialog();
            MokoSupport.getInstance().sendOrder(OrderTaskAssembler.getNearbyWifi());
        });
    }

    @Override
    protected ActivityNearbyWifiKgw3Binding getViewBinding() {
        return ActivityNearbyWifiKgw3Binding.inflate(getLayoutInflater());
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 200)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        String action = event.getAction();
        if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
            runOnUiThread(() -> {
                dismissLoadingProgressDialog();
                finish();
            });
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        final String action = event.getAction();
        if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
            dismissLoadingProgressDialog();
        }
        if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
            OrderTaskResponse response = event.getResponse();
            OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
            int responseType = response.responseType;
            byte[] value = response.responseValue;
            if (orderCHAR == OrderCHAR.CHAR_PARAMS) {
                if (value.length >= 4) {
                    int header = value[0] & 0xFF;// 0xED
                    int flag = value[1] & 0xFF;// read or write
                    int cmd = value[2] & 0xFF;
                    if (header == 0xED) {
                        ParamsKeyEnum configKeyEnum = ParamsKeyEnum.fromParamKey(cmd);
                        if (configKeyEnum == null) return;
                        int length = value[3] & 0xFF;
                        if (flag == 0x01) {
                            // write
                            int result = value[4] & 0xFF;
                            if (configKeyEnum == ParamsKeyEnum.KEY_WIFI_SEARCH) {
                                if (result != 1) mSavedParamsError = true;
                                if (mSavedParamsError) {
                                    ToastUtils.showToast(this, "Setup failed！");
                                } else {
                                    ToastUtils.showToast(this, "Setup succeed！");
                                }
                            }
                        }

                    }
                }
            }
        }
        if (MokoConstants.ACTION_CURRENT_DATA.equals(action)) {
            OrderTaskResponse response = event.getResponse();
            OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
            byte[] value = response.responseValue;
            if (orderCHAR == OrderCHAR.CHAR_PARAMS) {
                if (value.length >= 5) {
                    int header = value[0] & 0xFF;// 0xED
                    int flag = value[1] & 0xFF;// read or write
                    int cmd = value[2] & 0xFF;
                    if (header == 0xEE && flag == 0x02) {
                        ParamsLongKeyEnum configKeyEnum = ParamsLongKeyEnum.fromParamKey(cmd);
                        if (configKeyEnum == ParamsLongKeyEnum.KEY_WIFI_SEARCH_RESULT) {
                            int length = MokoUtils.toInt(Arrays.copyOfRange(value, 3, 5));
                            byte[] tlvResult = Arrays.copyOfRange(value, 5, 5 + length);
                            String bssid = "";
                            String ssid = "";
                            for (int i = 0; i < length; ) {
                                int t = tlvResult[i++];
                                int l = tlvResult[i++];
                                if (t == 0x00) {
                                    bssid = new String(Arrays.copyOfRange(tlvResult, i, i + l));
                                }
                                if (t == 0x01) {
                                    ssid = new String(Arrays.copyOfRange(tlvResult, i, i + l));
                                }
                                if (t == 0x02) {
                                    int rssi = tlvResult[i];
                                    WifiInfo info = new WifiInfo();
                                    info.bssid = bssid;
                                    info.ssid = ssid;
                                    info.rssi = rssi;
                                    mWifiInfoList.add(info);
                                }
                                i += l;
                            }
                            mAdapter.replaceData(mWifiInfoList);
                        }
                    }
                }
            }
        }
    }

    public void onBack(View view) {
        if (isWindowLocked()) return;
        finish();
    }

    @Override
    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
        WifiInfo wifiInfo = (WifiInfo) adapter.getItem(position);
        Intent intent = new Intent();
        assert wifiInfo != null;
        intent.putExtra("ssid", wifiInfo.ssid);
        setResult(RESULT_OK, intent);
    }
}
