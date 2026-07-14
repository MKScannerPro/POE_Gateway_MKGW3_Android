package com.moko.mkgw3.activity;

import android.annotation.SuppressLint;
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
import com.moko.mkgw3.databinding.ActivityAdvSettingsKgw3Binding;
import com.moko.mkgw3.dialog.MKgw3BottomDialog;
import com.moko.mkgw3.utils.ToastUtils;
import com.moko.support.mkgw3.MokoSupport;
import com.moko.support.mkgw3.OrderTaskAssembler;
import com.moko.support.mkgw3.entity.OrderCHAR;
import com.moko.support.mkgw3.entity.ParamsKeyEnum;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author: jun.liu
 * @date: 2023/7/3 11:24
 * @des:
 */
public class AdvSettingsMkgw3Activity extends BaseActivity<ActivityAdvSettingsKgw3Binding> implements SeekBar.OnSeekBarChangeListener {
    private final String[] txPowerArr = {"-24dBm", "-21dBm", "-18dBm", "-15dBm", "-12dBm", "-9dBm", "-6dBm", "-3dBm", "0dBm", "3dBm", "6dBm",
            "9dBm", "12dBm", "15dBm", "18dBm", "21dBm"};
    private int mSelected;
    private boolean saveParamsError;

    @Override
    protected ActivityAdvSettingsKgw3Binding getViewBinding() {
        return ActivityAdvSettingsKgw3Binding.inflate(getLayoutInflater());
    }

    @Override
    protected void onCreate() {
        showLoadingProgressDialog();
        List<OrderTask> orderTasks = new ArrayList<>(8);
        orderTasks.add(OrderTaskAssembler.getIBeaconEnable());
        orderTasks.add(OrderTaskAssembler.getIBeaconMajor());
        orderTasks.add(OrderTaskAssembler.getIBeaconMinor());
        orderTasks.add(OrderTaskAssembler.getIBeaconUUid());
        orderTasks.add(OrderTaskAssembler.getIBeaconAdInterval());
        orderTasks.add(OrderTaskAssembler.getIBeaconTxPower());
        orderTasks.add(OrderTaskAssembler.getIBeaconRssi1M());
        orderTasks.add(OrderTaskAssembler.getIBeaconConnectable());
        MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[0]));
        mBind.tvTxPowerVal.setOnClickListener(v -> {
            if (isWindowLocked()) return;
            MKgw3BottomDialog dialog = new MKgw3BottomDialog();
            dialog.setDatas(Arrays.asList(txPowerArr), mSelected);
            dialog.setListener(value -> {
                mSelected = value;
                mBind.tvTxPowerVal.setText(txPowerArr[value]);
            });
            dialog.show(getSupportFragmentManager());
        });
        mBind.cbIBeacon.setOnCheckedChangeListener((buttonView, isChecked) -> mBind.layoutAdvertise.setVisibility(isChecked ? View.VISIBLE : View.GONE));
        mBind.sbRssiFilter.setOnSeekBarChangeListener(this);
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
            byte[] value = response.responseValue;
            if (orderCHAR == OrderCHAR.CHAR_PARAMS) {
                if (value.length >= 4) {
                    int header = value[0] & 0xFF;// 0xED or 0xEE
                    int flag = value[1] & 0xFF;// read or write
                    int cmd = value[2] & 0xFF;
                    if (header == 0xED) {
                        ParamsKeyEnum configKeyEnum = ParamsKeyEnum.fromParamKey(cmd);
                        if (configKeyEnum == null) return;
                        int length = value[3] & 0xFF;
                        if (flag == 0x01) {
                            // write
                            int result = value[4] & 0xFF;
                            switch (configKeyEnum) {
                                case KEY_I_BEACON_SWITCH:
                                    if (!mBind.cbIBeacon.isChecked()) {
                                        if (result == 1) {
                                            ToastUtils.showToast(this, "Setup succeed！");
                                        } else {
                                            ToastUtils.showToast(this, "Setup failed！");
                                        }
                                    }
                                case KEY_I_BEACON_MAJOR:
                                case KEY_I_BEACON_MINOR:
                                case KEY_I_BEACON_UUID:
                                case KEY_I_BEACON_AD_INTERVAL:
                                case KEY_I_BEACON_RSSI1M:
                                case KEY_I_BEACON_CONNECTABLE:
                                    saveParamsError = result != 1;
                                    break;

                                case KEY_I_BEACON_TX_POWER:
                                    saveParamsError = result != 1;
                                    if (!saveParamsError) {
                                        ToastUtils.showToast(this, "Setup succeed！");
                                    } else {
                                        ToastUtils.showToast(this, "Setup failed！");
                                    }
                                    break;
                            }
                        }
                        if (flag == 0x00) {
                            if (length == 0) return;
                            // read
                            switch (configKeyEnum) {
                                case KEY_I_BEACON_SWITCH:
                                    if (length == 1) {
                                        int enable = value[4] & 0xff;
                                        mBind.cbIBeacon.setChecked(enable == 1);
                                        mBind.layoutAdvertise.setVisibility(enable == 1 ? View.VISIBLE : View.GONE);
                                    }
                                    break;

                                case KEY_I_BEACON_MAJOR:
                                    if (length == 2) {
                                        int major = MokoUtils.toInt(Arrays.copyOfRange(value, 4, value.length));
                                        mBind.etMajor.setText(String.valueOf(major));
                                        mBind.etMajor.setSelection(mBind.etMajor.getText().length());
                                    }
                                    break;

                                case KEY_I_BEACON_MINOR:
                                    if (length == 2) {
                                        int minor = MokoUtils.toInt(Arrays.copyOfRange(value, 4, value.length));
                                        mBind.etMinor.setText(String.valueOf(minor));
                                        mBind.etMinor.setSelection(mBind.etMinor.getText().length());
                                    }
                                    break;

                                case KEY_I_BEACON_UUID:
                                    if (length == 16) {
                                        String uuid = MokoUtils.bytesToHexString(Arrays.copyOfRange(value, 4, value.length));
                                        mBind.etUUid.setText(uuid);
                                        mBind.etUUid.setSelection(mBind.etUUid.getText().length());
                                    }
                                    break;

                                case KEY_I_BEACON_AD_INTERVAL:
                                    if (length == 1) {
                                        int interval = value[4] & 0xff;
                                        mBind.etAdInterval.setText(String.valueOf(interval));
                                        mBind.etAdInterval.setSelection(mBind.etAdInterval.getText().length());
                                    }
                                    break;

                                case KEY_I_BEACON_TX_POWER:
                                    if (length == 1) {
                                        mSelected = value[4] & 0xff;
                                        mBind.tvTxPowerVal.setText(txPowerArr[mSelected]);
                                    }
                                    break;

                                case KEY_I_BEACON_RSSI1M:
                                    int progress = value[4] + 100;
                                    mBind.sbRssiFilter.setProgress(progress);
                                    break;
                            }
                        }
                    }
                }
            }
        }
    }

    public void onSave(View view) {
        if (isWindowLocked()) return;
        if (!mBind.cbIBeacon.isChecked()) {
            showLoadingProgressDialog();
            MokoSupport.getInstance().sendOrder(OrderTaskAssembler.setIBeaconEnable(0));
        } else {
            if (isValid()) {
                showLoadingProgressDialog();
                saveParamsError = false;
                List<OrderTask> orderTasks = new ArrayList<>(8);
                int major = Integer.parseInt(mBind.etMajor.getText().toString());
                int minor = Integer.parseInt(mBind.etMinor.getText().toString());
                String uuid = mBind.etUUid.getText().toString();
                int interval = Integer.parseInt(mBind.etAdInterval.getText().toString());
                orderTasks.add(OrderTaskAssembler.setIBeaconEnable(1));
                orderTasks.add(OrderTaskAssembler.setIBeaconMajor(major));
                orderTasks.add(OrderTaskAssembler.setIBeaconMinor(minor));
                orderTasks.add(OrderTaskAssembler.setIBeaconUuid(uuid));
                orderTasks.add(OrderTaskAssembler.setIBeaconAdInterval(interval));
                orderTasks.add(OrderTaskAssembler.setIBeaconRssi1M(mBind.sbRssiFilter.getProgress() - 100));
                orderTasks.add(OrderTaskAssembler.setIBeaconConnectable(mBind.cbConnectable.isChecked() ? 0 : 1));
                orderTasks.add(OrderTaskAssembler.setIBeaconTxPower(mSelected));
                MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[0]));
            } else {
                ToastUtils.showToast(this, "Para Error");
            }
        }
    }

    private boolean isValid() {
        if (TextUtils.isEmpty(mBind.etMajor.getText())) return false;
        int major = Integer.parseInt(mBind.etMajor.getText().toString());
        if (major > 65535) return false;
        if (TextUtils.isEmpty(mBind.etMinor.getText())) return false;
        int minor = Integer.parseInt(mBind.etMinor.getText().toString());
        if (minor > 65535) return false;
        if (TextUtils.isEmpty(mBind.etUUid.getText()) || mBind.etUUid.getText().length() != 32)
            return false;
        if (TextUtils.isEmpty(mBind.etAdInterval.getText())) return false;
        int interval = Integer.parseInt(mBind.etAdInterval.getText().toString());
        return interval >= 1 && interval <= 100;
    }

    public void onBack(View view) {
        finish();
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int rssi = progress - 100;
        mBind.tvRssiFilterValue.setText(String.format("%ddBm", rssi));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
