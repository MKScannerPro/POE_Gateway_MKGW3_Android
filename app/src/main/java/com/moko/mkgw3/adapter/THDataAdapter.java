package com.moko.mkgw3.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.moko.mkgw3.R;
import com.moko.support.mkgw3.entity.THData;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * @author: jun.liu
 * @date: 2023/9/1 9:49
 * @des:
 */
public class THDataAdapter extends BaseQuickAdapter<THData, BaseViewHolder> {
    private final SimpleDateFormat sdf;
    private final String flag;
    private final String FLAG_TYPE = "history";

    public THDataAdapter(String flag) {
        super(R.layout.item_th_data);
        this.flag = flag;
        sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
    }

    @Override
    protected void convert(BaseViewHolder helper, THData item) {
        helper.setText(R.id.tvTime, sdf.format(new Date(item.timestamp * (FLAG_TYPE.equals(flag) ? 1000 : 1))).replace(" ", "\n"));
        helper.setText(R.id.tvTemperature, item.temperature);
        helper.setText(R.id.tvHumidity, item.humidity);
    }
}
