package com.moko.mkgw3.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.moko.mkgw3.R;
import com.moko.support.mkgw3.entity.LightData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author: jun.liu
 * @date: 2023/9/1 11:26
 * @des:
 */
public class LightDataAdapter extends BaseQuickAdapter<LightData, BaseViewHolder> {
    private final SimpleDateFormat sdf;

    public LightDataAdapter() {
        super(R.layout.item_light_data);
        sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
    }

    @Override
    protected void convert(BaseViewHolder helper, LightData item) {
        helper.setText(R.id.tvTime, sdf.format(new Date(item.timestamp * 1000)));
        String state = item.state == 1 ? "Ambient light \n detected" : "Ambient light NOT \n detected";
        helper.setText(R.id.tvType, state);
    }
}
