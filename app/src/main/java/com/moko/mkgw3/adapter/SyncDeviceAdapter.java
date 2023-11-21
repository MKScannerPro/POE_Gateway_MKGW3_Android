package com.moko.mkgw3.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.moko.mkgw3.R;
import com.moko.mkgw3.entity.MokoDeviceKgw3;


public class SyncDeviceAdapter extends BaseQuickAdapter<MokoDeviceKgw3, BaseViewHolder> {

    public SyncDeviceAdapter() {
        super(R.layout.item_sync_device);
    }

    @Override
    protected void convert(BaseViewHolder helper, MokoDeviceKgw3 item) {
        helper.setText(R.id.tv_name, item.name);
        helper.setText(R.id.tv_mac, item.mac);
        helper.setImageResource(R.id.iv_select, item.isSelected ? R.drawable.ic_selected : R.drawable.ic_unselected);
    }
}
