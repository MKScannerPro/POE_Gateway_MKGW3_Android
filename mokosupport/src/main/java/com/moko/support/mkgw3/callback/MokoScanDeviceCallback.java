package com.moko.support.mkgw3.callback;

import com.moko.support.mkgw3.entity.DeviceInfo;

public interface MokoScanDeviceCallback {
    void onStartScan();

    void onScanDevice(DeviceInfo device);

    void onStopScan();
}
