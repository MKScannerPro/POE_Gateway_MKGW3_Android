package com.moko.mkgw3.activity;

import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.SeekBar;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mkgw3.base.BaseActivity;
import com.moko.mkgw3.databinding.ActivityScanAndUploadKgw3Binding;
import com.moko.mkgw3.databinding.ActivityScannerFilterKgw3Binding;
import com.moko.mkgw3.utils.ToastUtils;
import com.moko.support.mkgw3.MokoSupport;
import com.moko.support.mkgw3.OrderTaskAssembler;
import com.moko.support.mkgw3.entity.OrderCHAR;
import com.moko.support.mkgw3.entity.ParamsKeyEnum;
import com.moko.support.mkgw3.entity.ParamsLongKeyEnum;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScanAndUploadKgw3Activity extends BaseActivity<ActivityScanAndUploadKgw3Binding> implements SeekBar.OnSeekBarChangeListener {

    private final String FILTER_ASCII = "[ -~]*";

    private boolean mSavedParamsError;

    private ArrayList<String> filterMacAddress;
    private ArrayList<String> filterAdvName;

    @Override
    protected void onCreate() {
        filterMacAddress = new ArrayList<>();
        filterAdvName = new ArrayList<>();
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }

            return null;
        };
        mBind.sbRssiFilter.setOnSeekBarChangeListener(this);
        mBind.etAdvName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20), filter});
        showLoadingProgressDialog();
        mBind.tvTitle.postDelayed(() -> {
            List<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.getFilterRSSI());
            orderTasks.add(OrderTaskAssembler.getFilterMacRules());
            orderTasks.add(OrderTaskAssembler.getFilterNameRules());
            orderTasks.add(OrderTaskAssembler.getReportInterval());
            MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        }, 500);
    }

    @Override
    protected ActivityScanAndUploadKgw3Binding getViewBinding() {
        return ActivityScanAndUploadKgw3Binding.inflate(getLayoutInflater());
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 100)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        String action = event.getAction();
        if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
            runOnUiThread(() -> {
                dismissLoadingProgressDialog();
                finish();
            });
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        final String action = event.getAction();
        if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
            dismissLoadingProgressDialog();
        }
        if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
            OrderTaskResponse response = event.getResponse();
            OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
            int responseType = response.responseType;
            byte[] value = response.responseValue;
            switch (orderCHAR) {
                case CHAR_PARAMS:
                    if (value.length >= 4) {
                        int header = value[0] & 0xFF;// 0xED or 0xEE
                        int flag = value[1] & 0xFF;// read or write
                        int cmd = value[2] & 0xFF;
                        if (header == 0xEE) {
                            ParamsLongKeyEnum configKeyEnum = ParamsLongKeyEnum.fromParamKey(cmd);
                            if (configKeyEnum == null) {
                                return;
                            }
                            if (flag == 0x01) {
                                // write
                                int result = value[4] & 0xFF;
                                switch (configKeyEnum) {
                                    case KEY_FILTER_NAME_RULES:
                                        if (result != 1) {
                                            mSavedParamsError = true;
                                        }
                                        break;
                                }
                            }
                            if (flag == 0x00) {
                                int length = MokoUtils.toInt(Arrays.copyOfRange(value, 3, 5));
                                // read
                                switch (configKeyEnum) {
                                    case KEY_FILTER_NAME_RULES:
                                        if (length > 0) {
                                            filterAdvName.clear();
                                            byte[] advNameBytes = Arrays.copyOfRange(value, 5, 5 + length);
                                            for (int i = 0, l = advNameBytes.length; i < l; ) {
                                                int advNameLength = advNameBytes[i] & 0xFF;
                                                i++;
                                                filterAdvName.add(new String(Arrays.copyOfRange(advNameBytes, i, i + advNameLength)));
                                                i += advNameLength;
                                            }
                                            mBind.etAdvName.setText(filterAdvName.get(0));
                                        }
                                        break;
                                }
                            }
                        }
                        if (header == 0xED) {
                            ParamsKeyEnum configKeyEnum = ParamsKeyEnum.fromParamKey(cmd);
                            if (configKeyEnum == null) {
                                return;
                            }
                            int length = value[3] & 0xFF;
                            if (flag == 0x01) {
                                // write
                                int result = value[4] & 0xFF;
                                switch (configKeyEnum) {
                                    case KEY_FILTER_MAC_RULES:
                                    case KEY_FILTER_RELATIONSHIP:
                                    case KEY_REPORT_INTERVAL:
                                        if (result != 1) {
                                            mSavedParamsError = true;
                                        }
                                        break;
                                    case KEY_FILTER_RSSI:
                                        if (result != 1) {
                                            mSavedParamsError = true;
                                        }
                                        if (mSavedParamsError) {
                                            ToastUtils.showToast(this, "Setup failed！");
                                        } else {
                                            ToastUtils.showToast(this, "Setup succeed！");
                                        }
                                        break;
                                }
                            }
                            if (flag == 0x00) {
                                if (length == 0)
                                    return;
                                // read
                                switch (configKeyEnum) {
                                    case KEY_REPORT_INTERVAL:
                                        int interval = MokoUtils.toInt(Arrays.copyOfRange(value, 4, 4 + length));
                                        mBind.etReportInterval.setText(String.valueOf(interval));
                                        break;
                                    case KEY_FILTER_RSSI:
                                        int progress = value[4] + 127;
                                        mBind.sbRssiFilter.setProgress(progress);
                                        break;
                                    case KEY_FILTER_MAC_RULES:
                                        filterMacAddress.clear();
                                        byte[] macBytes = Arrays.copyOfRange(value, 4, 4 + length);
                                        for (int i = 0, l = macBytes.length; i < l; ) {
                                            int macLength = macBytes[i] & 0xFF;
                                            i++;
                                            filterMacAddress.add(MokoUtils.bytesToHexString(Arrays.copyOfRange(macBytes, i, i + macLength)));
                                            i += macLength;
                                        }
                                        mBind.etMacAddress.setText(filterMacAddress.get(0));
                                        break;

                                }
                            }
                        }
                    }
                    break;
            }
        }
    }

    public void onSave(View view) {
        if (isWindowLocked()) return;
        if (isParaError()) {
            ToastUtils.showToast(this, "Para Error");
            return;
        }
        int rssi = mBind.sbRssiFilter.getProgress() - 127;
        String intervalStr = mBind.etReportInterval.getText().toString();
        int interval = Integer.parseInt(intervalStr);
        showLoadingProgressDialog();
        List<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.setFilterNameRules(filterAdvName));
        orderTasks.add(OrderTaskAssembler.setFilterMacRules(filterMacAddress));
        orderTasks.add(OrderTaskAssembler.setFilterRelationship(7));
        orderTasks.add(OrderTaskAssembler.setReportInterval(interval));
        orderTasks.add(OrderTaskAssembler.setFilterRSSI(rssi));
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    private boolean isParaError() {
        String filerMacStr = mBind.etMacAddress.getText().toString();
        int length = filerMacStr.length();
        if (!TextUtils.isEmpty(filerMacStr)) {
            if (length % 2 != 0) {
                return true;
            }
            filterMacAddress.clear();
            filterMacAddress.add(filerMacStr);
        } else {
            filterMacAddress.clear();
        }
        String filerNameStr = mBind.etAdvName.getText().toString();
        if (!TextUtils.isEmpty(filerNameStr)) {
            filterAdvName.clear();
            filterAdvName.add(filerNameStr);
        } else {
            filterAdvName.clear();
        }
        String intervalStr = mBind.etReportInterval.getText().toString();
        if (TextUtils.isEmpty(intervalStr)) {
            return true;
        }
        int interval = Integer.parseInt(intervalStr);
        if (interval > 86400) {
            return true;
        }
        return false;
    }

    public void onBack(View view) {
        finish();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int rssi = progress - 127;
        mBind.tvRssiFilterValue.setText(String.format("%ddBm", rssi));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
