package com.moko.mkgw3.activity;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkgw3.AppConstants;
import com.moko.mkgw3.adapter.BleDeviceKgw3Adapter;
import com.moko.mkgw3.base.BaseActivity;
import com.moko.mkgw3.databinding.ActivityBleDevicesKgw3Binding;
import com.moko.mkgw3.db.MKgw3DBTools;
import com.moko.mkgw3.dialog.PasswordBleDialogKgw3;
import com.moko.mkgw3.dialog.ScanFilterDialog;
import com.moko.mkgw3.entity.MQTTConfigKgw3;
import com.moko.mkgw3.entity.MokoDeviceKgw3;
import com.moko.mkgw3.utils.SPUtiles;
import com.moko.mkgw3.utils.ToastUtils;
import com.moko.support.mkgw3.MQTTConstants;
import com.moko.support.mkgw3.MQTTSupport;
import com.moko.support.mkgw3.MokoSupport;
import com.moko.support.mkgw3.entity.BXPButtonInfo;
import com.moko.support.mkgw3.entity.BleDevice;
import com.moko.support.mkgw3.entity.BxpCInfo;
import com.moko.support.mkgw3.entity.MsgNotify;
import com.moko.support.mkgw3.entity.OtherDeviceInfo;
import com.moko.support.mkgw3.event.DeviceModifyNameEvent;
import com.moko.support.mkgw3.event.DeviceOnlineEvent;
import com.moko.support.mkgw3.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import androidx.recyclerview.widget.LinearLayoutManager;

public class BleManagerKgw3Activity extends BaseActivity<ActivityBleDevicesKgw3Binding> implements BaseQuickAdapter.OnItemChildClickListener {
    private MokoDeviceKgw3 mMokoDeviceKgw3;
    private MQTTConfigKgw3 appMqttConfig;
    private String mAppTopic;

    private BleDeviceKgw3Adapter mAdapter;
    private ArrayList<BleDevice> mBleDevices;
    private ConcurrentHashMap<String, BleDevice> mBleDevicesMap;
    private Handler mHandler;
    private int mIndex;
    private int from;

    @Override
    protected void onCreate() {
        mMokoDeviceKgw3 = (MokoDeviceKgw3) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        from = getIntent().getIntExtra("from", 0);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfigKgw3.class);
        mAppTopic = TextUtils.isEmpty(appMqttConfig.topicPublish) ? mMokoDeviceKgw3.topicSubscribe : appMqttConfig.topicPublish;
        mHandler = new Handler(Looper.getMainLooper());

        mBind.tvDeviceName.setText(mMokoDeviceKgw3.name);
        mBleDevices = new ArrayList<>();
        mBleDevicesMap = new ConcurrentHashMap<>();
        mAdapter = new BleDeviceKgw3Adapter();
        mAdapter.openLoadAnimation();
        mAdapter.replaceData(mBleDevices);
        mAdapter.setOnItemChildClickListener(this);
        mBind.rvDevices.setLayoutManager(new LinearLayoutManager(this));
        mBind.rvDevices.setAdapter(mAdapter);
        refreshList();
    }

    private void refreshList() {
        new Thread(() -> {
            while (refreshFlag) {
                runOnUiThread(() -> {
                    mBind.tvCount.setText(String.format("Count:%d", mBleDevices.size()));
                    mAdapter.replaceData(mBleDevices);
                });
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                updateDevices();
            }
        }).start();
    }

    @Override
    protected ActivityBleDevicesKgw3Binding getViewBinding() {
        return ActivityBleDevicesKgw3Binding.inflate(getLayoutInflater());
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 100)
    public void onMQTTMessageArrivedEvent(MQTTMessageArrivedEvent event) {
        // 更新所有设备的网络状态
        final String message = event.getMessage();
        if (TextUtils.isEmpty(message)) return;
        int msg_id;
        try {
            JsonObject object = new Gson().fromJson(message, JsonObject.class);
            JsonElement element = object.get("msg_id");
            msg_id = element.getAsInt();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_SCAN_RESULT) {
            EventBus.getDefault().cancelEventDelivery(event);
            runOnUiThread(() -> {
                Type type = new TypeToken<MsgNotify<List<BleDevice>>>() {
                }.getType();
                MsgNotify<List<BleDevice>> result = new Gson().fromJson(message, type);
                if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
                List<BleDevice> bleDevices = result.data;

                for (BleDevice device : bleDevices) {
                    if (device.rssi < filterRssi) continue;
                    if (from == 0 && device.type_code != 7) continue;
                    if (from == 1 && device.type_code != 4 && device.type_code != 6) continue;
                    if (from == 2 && (device.type_code == 7 || device.type_code == 4 || device.type_code == 6))
                        continue;
                    if (!mBleDevicesMap.containsKey(device.mac)) {
                        device.index = mIndex++;
                        mBleDevicesMap.put(device.mac, device);
                    } else {
                        BleDevice existDevice = mBleDevicesMap.get(device.mac);
                        existDevice.rssi = device.rssi;
                        existDevice.adv_name = device.adv_name;
                        existDevice.type_code = device.type_code;
                        existDevice.connectable = device.connectable;
                    }
                }
            });
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_BUTTON_CONNECT_RESULT) {
            runOnUiThread(() -> {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
                Type type = new TypeToken<MsgNotify<BXPButtonInfo>>() {
                }.getType();
                MsgNotify<BXPButtonInfo> result = new Gson().fromJson(message, type);
                if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac))
                    return;
                BXPButtonInfo bxpButtonInfo = result.data;
                if (bxpButtonInfo.result_code != 0) {
                    ToastUtils.showToast(this, bxpButtonInfo.result_msg);
                    return;
                }
                Intent intent = new Intent(this, BXPButtonInfoKgw3Activity.class);
                intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
                intent.putExtra(AppConstants.EXTRA_KEY_BXP_BUTTON_INFO, bxpButtonInfo);
                startActivity(intent);
            });
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_C_CONNECT_RESULT) {
            runOnUiThread(() -> {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
                Type type = new TypeToken<MsgNotify<BxpCInfo>>() {
                }.getType();
                MsgNotify<BxpCInfo> result = new Gson().fromJson(message, type);
                if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
                BxpCInfo bxpInfo = result.data;
                if (bxpInfo.result_code != 0) {
                    ToastUtils.showToast(this, bxpInfo.result_msg);
                    return;
                }
                Intent intent = new Intent(this, BxpCInfoActivity.class);
                intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
                intent.putExtra(AppConstants.EXTRA_KEY_BXP_BUTTON_INFO, bxpInfo);
                startActivity(intent);
            });
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_OTHER_CONNECT_RESULT) {
            runOnUiThread(() -> {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
                Type type = new TypeToken<MsgNotify<OtherDeviceInfo>>() {
                }.getType();
                MsgNotify<OtherDeviceInfo> result = new Gson().fromJson(message, type);
                if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
                OtherDeviceInfo otherDeviceInfo = result.data;
                if (otherDeviceInfo.result_code != 0) {
                    ToastUtils.showToast(this, otherDeviceInfo.result_msg);
                    return;
                }
                Intent intent = new Intent(this, BleOtherInfoKgw3Activity.class);
                intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
                intent.putExtra(AppConstants.EXTRA_KEY_OTHER_DEVICE_INFO, otherDeviceInfo);
                startActivity(intent);
            });
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceModifyNameEvent(DeviceModifyNameEvent event) {
        // 修改了设备名称
        MokoDeviceKgw3 device = MKgw3DBTools.getInstance(BleManagerKgw3Activity.this).selectDevice(mMokoDeviceKgw3.mac);
        mMokoDeviceKgw3.name = device.name;
        mBind.tvDeviceName.setText(mMokoDeviceKgw3.name);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceOnlineEvent(DeviceOnlineEvent event) {
        String mac = event.getMac();
        if (!mMokoDeviceKgw3.mac.equals(mac))
            return;
        boolean online = event.isOnline();
        if (!online) {
            ToastUtils.showToast(this, "device is off-line");
            finish();
        }
    }

    private boolean refreshFlag = true;
    public String filterName;
    public String filterMac;
    public int filterRssi = -127;

    private void updateDevices() {
        mBleDevices.clear();
        if (!TextUtils.isEmpty(filterName)
                || !TextUtils.isEmpty(filterMac)
                || filterRssi != -127) {
            ArrayList<BleDevice> bleDevices = new ArrayList<>(mBleDevicesMap.values());
            Iterator<BleDevice> iterator = bleDevices.iterator();
            while (iterator.hasNext()) {
                BleDevice bleDevice = iterator.next();
                if (bleDevice.rssi > filterRssi) {
                    if (TextUtils.isEmpty(filterName) && TextUtils.isEmpty(filterMac)) {
                        continue;
                    } else {
                        if (!TextUtils.isEmpty(filterMac) && TextUtils.isEmpty(bleDevice.mac)) {
                            iterator.remove();
                        } else if (!TextUtils.isEmpty(filterMac) && bleDevice.mac.toLowerCase().replaceAll(":", "").contains(filterMac.toLowerCase())) {
                            continue;
                        } else if (!TextUtils.isEmpty(filterName) && TextUtils.isEmpty(bleDevice.adv_name)) {
                            iterator.remove();
                        } else if (!TextUtils.isEmpty(filterName) && bleDevice.adv_name.toLowerCase().contains(filterName.toLowerCase())) {
                            continue;
                        } else {
                            iterator.remove();
                        }
                    }
                } else {
                    iterator.remove();
                }
            }
            mBleDevices.addAll(bleDevices);
        } else {
            mBleDevices.addAll(mBleDevicesMap.values());
        }
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
        Collections.sort(mBleDevices, (lhs, rhs) -> {
            if (lhs.index > rhs.index) {
                return 1;
            } else if (lhs.index < rhs.index) {
                return -1;
            }
            return 0;
        });
    }

    public void onFilter(View view) {
        if (isWindowLocked())
            return;
        ScanFilterDialog scanFilterDialog = new ScanFilterDialog();
        scanFilterDialog.setFilterName(filterName);
        scanFilterDialog.setFilterMac(filterMac);
        scanFilterDialog.setFilterRssi(filterRssi);
        scanFilterDialog.setOnScanFilterListener((filterName, filterMac, filterRssi) -> {
            BleManagerKgw3Activity.this.filterName = filterName;
            BleManagerKgw3Activity.this.filterMac = filterMac;
            BleManagerKgw3Activity.this.filterRssi = filterRssi;
            if (!TextUtils.isEmpty(filterName)
                    || !TextUtils.isEmpty(filterMac)
                    || filterRssi != -127) {
                mBind.rlFilter.setVisibility(View.VISIBLE);
                mBind.tvEditFilter.setVisibility(View.GONE);
                StringBuilder stringBuilder = new StringBuilder();
                if (!TextUtils.isEmpty(filterName)) {
                    stringBuilder.append(filterName);
                    stringBuilder.append(";");
                }
                if (!TextUtils.isEmpty(filterMac)) {
                    stringBuilder.append(filterMac);
                    stringBuilder.append(";");
                }
                if (filterRssi != -127) {
                    stringBuilder.append(String.format("%sdBm", filterRssi + ""));
                    stringBuilder.append(";");
                }
                mBind.tvFilter.setText(stringBuilder.toString());
            } else {
                mBind.rlFilter.setVisibility(View.GONE);
                mBind.tvEditFilter.setVisibility(View.VISIBLE);
            }
            mBleDevicesMap.clear();
            mIndex = 0;
        });
        scanFilterDialog.show(getSupportFragmentManager());
    }

    public void onFilterDelete(View view) {
        if (isWindowLocked())
            return;
        mBind.rlFilter.setVisibility(View.GONE);
        mBind.tvEditFilter.setVisibility(View.VISIBLE);
        filterName = "";
        filterMac = "";
        filterRssi = -127;
        mBleDevicesMap.clear();
        mIndex = 0;
    }

    @Override
    public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
        if (isWindowLocked()) return;
        BleDevice bleDevice = (BleDevice) adapter.getItem(position);
        if (bleDevice == null) return;
        if (from == 0 || from == 1) {
            // BXP-Button
            // show password
            final PasswordBleDialogKgw3 dialog = new PasswordBleDialogKgw3();
            dialog.setOnPasswordClicked(password -> {
                if (!MokoSupport.getInstance().isBluetoothOpen()) {
                    MokoSupport.getInstance().enableBluetooth();
                    return;
                }
                XLog.i(password);
                mHandler.postDelayed(() -> {
                    dismissLoadingProgressDialog();
                    ToastUtils.showToast(BleManagerKgw3Activity.this, "Setup failed");
                }, 50 * 1000);
                showLoadingProgressDialog();
                getBleDeviceInfo(bleDevice, password);
            });
            dialog.show(getSupportFragmentManager());
        } else {
            // Other
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Setup failed");
            }, 50 * 1000);
            showLoadingProgressDialog();
            getBleDeviceInfo(bleDevice);
        }
    }

    private void getBleDeviceInfo(BleDevice bleDevice, String password) {
        int msgId = from == 0 ? MQTTConstants.CONFIG_MSG_ID_BLE_BXP_BUTTON_CONNECT : MQTTConstants.CONFIG_MSG_ID_BLE_BXP_C_CONNECT;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", bleDevice.mac);
        jsonObject.addProperty("passwd", password);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getBleDeviceInfo(BleDevice bleDevice) {
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_OTHER_CONNECT;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", bleDevice.mac);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        if (isWindowLocked()) return;
        back();
    }

    public void onBack(View view) {
        if (isWindowLocked()) return;
        back();
    }

    private void back() {
        if (refreshFlag)
            refreshFlag = false;
        finish();
    }
}
