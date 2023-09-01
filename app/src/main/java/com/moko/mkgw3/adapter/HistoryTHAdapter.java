package com.moko.mkgw3.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.moko.mkgw3.R;
import com.moko.support.mkgw3.entity.HistoryTHData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author: jun.liu
 * @date: 2023/9/1 9:49
 * @des:
 */
public class HistoryTHAdapter extends BaseQuickAdapter<HistoryTHData, BaseViewHolder> {
    private final SimpleDateFormat sdf;

    public HistoryTHAdapter() {
        super(R.layout.item_th_data);
        sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
    }

    @Override
    protected void convert(BaseViewHolder helper, HistoryTHData item) {
        helper.setText(R.id.tvTime, sdf.format(new Date(item.timestamp * 1000)).replace(" ", "\n"));
        helper.setText(R.id.tvTemperature, item.temperature);
        helper.setText(R.id.tvHumidity, item.humidity);
    }
}
