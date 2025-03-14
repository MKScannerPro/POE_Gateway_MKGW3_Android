package com.moko.mkgw3.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.moko.mkgw3.R;
import com.moko.mkgw3.entity.PIRSensorData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class PIRSensorDataAdapter extends BaseQuickAdapter<PIRSensorData, BaseViewHolder> {
    private final SimpleDateFormat sdf;

    public PIRSensorDataAdapter() {
        super(R.layout.item_sensor_data);
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }

    @Override
    protected void convert(BaseViewHolder helper, PIRSensorData item) {
        helper.setText(R.id.tvTimestamp, sdf.format(new Date(item.timestamp)));
        String sensor = String.format("%s/%s",
                item.hall_status == 1 ? "Door open" : "Door close",
                item.pir_status == 1 ? "occupied" : "not occupied");
        helper.setText(R.id.tvSensorData, sensor);
    }
}
