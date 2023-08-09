package com.moko.support.remotegw.callback;

import com.moko.support.remotegw.entity.DeviceInfo;

public interface MokoScanDeviceCallback {
    void onStartScan();

    void onScanDevice(DeviceInfo device);

    void onStopScan();
}
