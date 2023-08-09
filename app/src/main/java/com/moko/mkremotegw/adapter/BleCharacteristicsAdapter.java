package com.moko.mkremotegw.adapter;

import android.text.TextUtils;

import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.moko.mkremotegw.R;
import com.moko.mkremotegw.entity.BleOtherChar;

import java.util.List;

public class BleCharacteristicsAdapter extends BaseMultiItemQuickAdapter<BleOtherChar, BaseViewHolder> {
    public BleCharacteristicsAdapter(List<BleOtherChar> data) {
        super(data);
        addItemType(0, R.layout.item_service);
        addItemType(1, R.layout.item_characteristics);
    }

    @Override
    protected void convert(BaseViewHolder helper, BleOtherChar item) {
        switch (helper.getItemViewType()) {
            case 0:
                helper.setText(R.id.tv_service_uuid,
                        String.format("Service UUID:0x%s", item.serviceUUID.toUpperCase()));
                break;
            case 1:
                helper.setText(R.id.tv_characteristics_uuid,
                        String.format("Characteristics UUID:0x%s", item.characteristicUUID.toUpperCase()));
                boolean readFlag = (item.characteristicProperties & 0x02) == 0x02;
                boolean writeNoRespFlag = (item.characteristicProperties & 0x04) == 0x04;
                boolean writeFlag = (item.characteristicProperties & 0x08) == 0x08;
                boolean notifyFlag = (item.characteristicProperties & 0x10) == 0x10;
                helper.setGone(R.id.iv_read, readFlag);
                helper.setGone(R.id.iv_write, writeNoRespFlag | writeFlag);
                helper.setGone(R.id.iv_notify, notifyFlag);
                helper.setImageResource(R.id.iv_notify, item.characteristicNotifyStatus == 1 ? R
                        .drawable.ic_notify_open : R.drawable.ic_notify_close);
                StringBuilder properties = new StringBuilder("");
                String propertiesStr;
                if (notifyFlag)
                    properties.append("NOTIFY,");
                if (readFlag)
                    properties.append("READ,");
                if (writeNoRespFlag)
                    properties.append("WRITE NO RESPONSE,");
                if (writeFlag)
                    properties.append("WRITE,");
                if (properties.indexOf(",") > 0) {
                    propertiesStr = properties.substring(0, properties.lastIndexOf(","));
                } else {
                    propertiesStr = properties.toString();
                }
                helper.setText(R.id.tv_characteristics_properties,
                        String.format("Properties:%s", propertiesStr));
                if (TextUtils.isEmpty(item.characteristicPayload)) {
                    helper.setText(R.id.tv_characteristics_value, "Value:");
                } else {
                    helper.setText(R.id.tv_characteristics_value,
                            String.format("Value:0x%s", item.characteristicPayload));
                }
                if (readFlag)
                    helper.addOnClickListener(R.id.iv_read);
                if (writeNoRespFlag | writeFlag)
                    helper.addOnClickListener(R.id.iv_write);
                if (notifyFlag)
                    helper.addOnClickListener(R.id.iv_notify);
                break;
        }
    }
}
