package com.moko.mkgw3.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.moko.mkgw3.R;
import com.moko.mkgw3.entity.TOFSensorData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class TOFSensorDataAdapter extends BaseQuickAdapter<TOFSensorData, BaseViewHolder> {
    private final SimpleDateFormat sdf;

    public TOFSensorDataAdapter() {
        super(R.layout.item_sensor_data);
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }

    @Override
    protected void convert(BaseViewHolder helper, TOFSensorData item) {
        helper.setText(R.id.tvTimestamp, sdf.format(new Date(item.timestamp)));
        String sensor = String.format(Locale.getDefault(), "%dmm", item.distance);
        helper.setText(R.id.tvSensorData, sensor);
    }
}
