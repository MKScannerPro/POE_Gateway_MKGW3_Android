package com.moko.mkgw3.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.moko.mkgw3.R;
import com.moko.support.mkgw3.entity.AccData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author: jun.liu
 * @date: 2023/9/1 17:50
 * @des:
 */
public class AccDataAdapter extends BaseQuickAdapter<AccData, BaseViewHolder> {
    private final SimpleDateFormat sdf;

    public AccDataAdapter() {
        super(R.layout.item_acc_data);
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }

    @Override
    protected void convert(BaseViewHolder helper, AccData item) {
        helper.setText(R.id.tvTimestamp, sdf.format(new Date(item.timeStamp * 1000)));
        helper.setText(R.id.tvAxisData, "X-axis: " + item.x_axis_data +
                ", Y-axis: " + item.y_axis_data + ", Z-axis: " + item.z_axis_data);
    }
}
