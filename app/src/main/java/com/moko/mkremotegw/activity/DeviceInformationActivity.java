package com.moko.mkremotegw.activity;

import android.view.View;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mkremotegw.base.BaseActivity;
import com.moko.mkremotegw.databinding.ActivityDeviceInformationBinding;
import com.moko.support.remotegw.MokoSupport;
import com.moko.support.remotegw.OrderTaskAssembler;
import com.moko.support.remotegw.entity.OrderCHAR;
import com.moko.support.remotegw.entity.ParamsKeyEnum;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DeviceInformationActivity extends BaseActivity<ActivityDeviceInformationBinding> {

    @Override
    protected void onCreate() {
        showLoadingProgressDialog();
        mBind.tvDeviceName.postDelayed(() -> {
            List<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.getDeviceName());
            orderTasks.add(OrderTaskAssembler.getDeviceModel());
            orderTasks.add(OrderTaskAssembler.getManufacturer());
            orderTasks.add(OrderTaskAssembler.getFirmwareVersion());
            orderTasks.add(OrderTaskAssembler.getHardwareVersion());
            orderTasks.add(OrderTaskAssembler.getSoftwareVersion());
            orderTasks.add(OrderTaskAssembler.getWifiMac());
            orderTasks.add(OrderTaskAssembler.getBleMac());
            MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        }, 500);
    }


    @Override
    protected ActivityDeviceInformationBinding getViewBinding() {
        return ActivityDeviceInformationBinding.inflate(getLayoutInflater());
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
                case CHAR_MODEL_NUMBER:
                    mBind.tvProductModel.setText(new String(value));
                    break;
                case CHAR_MANUFACTURER_NAME:
                    mBind.tvManufacturer.setText(new String(value));
                    break;
                case CHAR_FIRMWARE_REVISION:
                    mBind.tvDeviceFirmwareVersion.setText(new String(value));
                    break;
                case CHAR_HARDWARE_REVISION:
                    mBind.tvDeviceHardwareVersion.setText(new String(value));
                    break;
                case CHAR_SOFTWARE_REVISION:
                    mBind.tvDeviceSoftwareVersion.setText(new String(value));
                    break;
                case CHAR_PARAMS:
                    if (value.length >= 4) {
                        int header = value[0] & 0xFF;// 0xED
                        int flag = value[1] & 0xFF;// read or write
                        int cmd = value[2] & 0xFF;
                        if (header == 0xED) {
                            ParamsKeyEnum configKeyEnum = ParamsKeyEnum.fromParamKey(cmd);
                            if (configKeyEnum == null) {
                                return;
                            }
                            int length = value[3] & 0xFF;
                            if (flag == 0x00) {
                                if (length == 0)
                                    return;
                                // read
                                switch (configKeyEnum) {
                                    case KEY_DEVICE_NAME:
                                        mBind.tvDeviceName.setText(new String(Arrays.copyOfRange(value, 4, 4 + length)));
                                        break;
                                    case KEY_WIFI_MAC:
                                        byte[] wifiMacBytes = Arrays.copyOfRange(value, 4, 4 + length);
                                        mBind.tvDeviceStaMac.setText(MokoUtils.bytesToHexString(wifiMacBytes).toUpperCase());
                                        break;
                                    case KEY_BLE_MAC:
                                        byte[] bleMacBytes = Arrays.copyOfRange(value, 4, 4 + length);
                                        mBind.tvDeviceBtMac.setText(MokoUtils.bytesToHexString(bleMacBytes).toUpperCase());
                                        break;

                                }
                            }
                        }
                    }
                    break;
            }
        }
    }

    public void onBack(View view) {
        finish();
    }
}
