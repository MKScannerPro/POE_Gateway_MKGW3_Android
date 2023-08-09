package com.moko.mkremotegw.adapter;

import android.text.TextUtils;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.moko.mkremotegw.R;
import com.moko.support.remotegw.entity.BleDevice;

import java.util.Locale;

public class BleDeviceAdapter extends BaseQuickAdapter<BleDevice, BaseViewHolder> {
    public BleDeviceAdapter() {
        super(R.layout.item_ble_device);
    }

    @Override
    protected void convert(BaseViewHolder helper, BleDevice item) {
        helper.setText(R.id.tv_name, TextUtils.isEmpty(item.adv_name) ? "N/A" : item.adv_name);
        helper.setText(R.id.tv_mac, item.mac);
        helper.setText(R.id.tv_rssi, String.format(Locale.getDefault(), "%ddBm", item.rssi));
        helper.setGone(R.id.tv_connect,item.connectable == 1);
        helper.addOnClickListener(R.id.tv_connect);
    }
}
