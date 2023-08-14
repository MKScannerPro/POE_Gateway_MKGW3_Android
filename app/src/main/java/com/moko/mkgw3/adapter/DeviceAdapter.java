package com.moko.mkgw3.adapter;

import androidx.core.content.ContextCompat;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.moko.mkgw3.R;
import com.moko.mkgw3.entity.MokoDevice;

public class DeviceAdapter extends BaseQuickAdapter<MokoDevice, BaseViewHolder> {

    public DeviceAdapter() {
        super(R.layout.device_item_kgw3);
    }

    @Override
    protected void convert(BaseViewHolder helper, MokoDevice item) {
        helper.setText(R.id.tv_device_name, item.name);
        helper.setText(R.id.tv_device_mac, item.mac.toUpperCase());
        if (!item.isOnline) {
            helper.setText(R.id.tv_device_status, mContext.getString(R.string.device_state_offline));
            helper.setTextColor(R.id.tv_device_status, ContextCompat.getColor(mContext, R.color.grey_b3b3b3));
            helper.setImageResource(R.id.iv_net_status, item.networkType == 1 ? R.drawable.ic_net_offline : R.drawable.ethernet_offline);
        } else {
            helper.setText(R.id.tv_device_status, mContext.getString(R.string.device_state_online));
            helper.setTextColor(R.id.tv_device_status, ContextCompat.getColor(mContext, R.color.blue_0188cc));
            if (item.networkType == 1) {
                if (item.wifiRssi >= -50) {
                    helper.setImageResource(R.id.iv_net_status, R.drawable.ic_net_good);
                } else if (item.wifiRssi >= -65) {
                    helper.setImageResource(R.id.iv_net_status, R.drawable.ic_net_medium);
                } else {
                    helper.setImageResource(R.id.iv_net_status, R.drawable.ic_net_poor);
                }
            } else {
                helper.setImageResource(R.id.iv_net_status, R.drawable.ethernet_online);
            }
        }
    }
}
