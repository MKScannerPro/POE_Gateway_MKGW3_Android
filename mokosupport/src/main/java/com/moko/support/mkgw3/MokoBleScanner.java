package com.moko.support.mkgw3;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.text.TextUtils;

import com.elvishew.xlog.XLog;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.support.mkgw3.callback.MokoScanDeviceCallback;
import com.moko.support.mkgw3.entity.DeviceInfo;
import com.moko.support.mkgw3.entity.OrderServices;

import java.util.ArrayList;
import java.util.List;

import androidx.core.content.ContextCompat;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

public final class MokoBleScanner {

    private MokoLeScanHandler mMokoLeScanHandler;
    private MokoScanDeviceCallback mMokoScanDeviceCallback;

    private Context mContext;

    public MokoBleScanner(Context context) {
        mContext = context;
    }

    public void startScanDevice(MokoScanDeviceCallback callback) {
        mMokoScanDeviceCallback = callback;
        XLog.i("Start scan");
        final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        List<ScanFilter> scanFilterList = new ArrayList<>();
        ScanFilter.Builder configBuilder = new ScanFilter.Builder();
        configBuilder.setServiceData(new ParcelUuid(OrderServices.SERVICE_ADV.getUuid()), null);
        scanFilterList.add(configBuilder.build());
//        ScanFilter.Builder iBeaconBuilder = new ScanFilter.Builder();
//        iBeaconBuilder.setManufacturerData(0x004C, null);
//        scanFilterList.add(iBeaconBuilder.build());
//        List<ScanFilter> scanFilterList = Collections.singletonList(new ScanFilter.Builder().build());
        mMokoLeScanHandler = new MokoLeScanHandler(callback);
        scanner.startScan(scanFilterList, settings, mMokoLeScanHandler);
        callback.onStartScan();
    }

    public void stopScanDevice() {
        if (mMokoLeScanHandler != null && mMokoScanDeviceCallback != null) {
            XLog.i("End scan");
            final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
            scanner.stopScan(mMokoLeScanHandler);
            mMokoScanDeviceCallback.onStopScan();
            mMokoLeScanHandler = null;
            mMokoScanDeviceCallback = null;
        }
    }

    public static class MokoLeScanHandler extends ScanCallback {

        private MokoScanDeviceCallback callback;

        public MokoLeScanHandler(MokoScanDeviceCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (result != null) {
                BluetoothDevice device = result.getDevice();
                byte[] scanRecord = result.getScanRecord().getBytes();
                String name = result.getScanRecord().getDeviceName();
                int rssi = result.getRssi();
                if (TextUtils.isEmpty(name) || scanRecord.length == 0 || rssi == 127) {
                    return;
                }
                DeviceInfo deviceInfo = new DeviceInfo();
                deviceInfo.name = name;
                deviceInfo.rssi = rssi;
                deviceInfo.mac = device.getAddress();
                String scanRecordStr = MokoUtils.bytesToHexString(scanRecord);
                deviceInfo.scanRecord = scanRecordStr;
                deviceInfo.scanResult = result;
                callback.onScanDevice(deviceInfo);
            }
        }
    }
}
