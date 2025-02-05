package com.moko.mkgw3.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.moko.mkgw3.R;
import com.moko.mkgw3.entity.WifiInfo;

public class NearbyWifiKgw3Adapter extends BaseQuickAdapter<WifiInfo, BaseViewHolder> {
    public NearbyWifiKgw3Adapter() {
        super(R.layout.item_wifi_ssid_kgw3);
    }

    @Override
    protected void convert(BaseViewHolder helper, WifiInfo item) {
        helper.setText(R.id.tv_ssid, item.ssid);
        helper.setText(R.id.tv_bssid, item.bssid);
        helper.setText(R.id.tv_rssi, String.valueOf(item.rssi));
    }
}
