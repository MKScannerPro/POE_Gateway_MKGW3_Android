package com.moko.mkgw3.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.moko.mkgw3.R;
import com.moko.support.mkgw3.entity.DeviceInfo;

public class DeviceInfoKgw3Adapter extends BaseQuickAdapter<DeviceInfo, BaseViewHolder> {
    public DeviceInfoKgw3Adapter() {
        super(R.layout.item_devices_kgw3);
    }

    @Override
    protected void convert(BaseViewHolder helper, DeviceInfo item) {
        helper.setText(R.id.tv_device_name, item.name);
        helper.setText(R.id.tv_device_rssi, String.valueOf(item.rssi));
    }
}
