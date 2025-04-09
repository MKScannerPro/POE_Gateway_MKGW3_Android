package com.moko.mkgw3.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.moko.mkgw3.R;
import com.moko.mkgw3.entity.AlarmEventData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class AlarmEventDataAdapter extends BaseQuickAdapter<AlarmEventData, BaseViewHolder> {
    private final SimpleDateFormat sdf;

    public AlarmEventDataAdapter() {
        super(R.layout.item_alarm_event);
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }

    @Override
    protected void convert(BaseViewHolder helper, AlarmEventData item) {
        helper.setText(R.id.tvTimestamp, sdf.format(new Date(item.timestamp)));
        String mode;
        if (item.type == 0) {
            mode = "Single press mode";
        } else if (item.type == 1) {
            mode = "Double press mode";
        } else {
            mode = "Long press mode";
        }
        helper.setText(R.id.tvTriggerEventData, mode);
    }
}
