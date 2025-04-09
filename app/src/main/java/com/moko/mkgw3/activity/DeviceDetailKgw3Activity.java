package com.moko.mkgw3.activity;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkgw3.AppConstants;
import com.moko.mkgw3.R;
import com.moko.mkgw3.activity.beacon.BXPBCRGW3Activity;
import com.moko.mkgw3.activity.beacon.BXPBDGW3Activity;
import com.moko.mkgw3.activity.beacon.BXPButtonInfoKgw3Activity;
import com.moko.mkgw3.activity.beacon.BXPCGW3Activity;
import com.moko.mkgw3.activity.beacon.BXPDGW3Activity;
import com.moko.mkgw3.activity.beacon.BXPSGW3Activity;
import com.moko.mkgw3.activity.beacon.BXPTGW3Activity;
import com.moko.mkgw3.activity.beacon.BleOtherInfoKgw3Activity;
import com.moko.mkgw3.activity.beacon.MKPIRGW3Activity;
import com.moko.mkgw3.activity.beacon.MKTOFGW3Activity;
import com.moko.mkgw3.adapter.ScanDeviceKgw3Adapter;
import com.moko.mkgw3.base.BaseActivity;
import com.moko.mkgw3.databinding.ActivityDetailKgw3Binding;
import com.moko.mkgw3.db.MKgw3DBTools;
import com.moko.mkgw3.entity.MQTTConfigKgw3;
import com.moko.mkgw3.entity.MokoDeviceKgw3;
import com.moko.mkgw3.utils.SPUtiles;
import com.moko.mkgw3.utils.ToastUtils;
import com.moko.support.mkgw3.MQTTConstants;
import com.moko.support.mkgw3.MQTTSupport;
import com.moko.support.mkgw3.entity.BeaconInfo;
import com.moko.support.mkgw3.entity.BleConnectedList;
import com.moko.support.mkgw3.entity.MsgConfigResult;
import com.moko.support.mkgw3.entity.MsgNotify;
import com.moko.support.mkgw3.entity.MsgReadResult;
import com.moko.support.mkgw3.entity.OtherDeviceInfo;
import com.moko.support.mkgw3.event.DeviceModifyNameEvent;
import com.moko.support.mkgw3.event.DeviceOnlineEvent;
import com.moko.support.mkgw3.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;

public class DeviceDetailKgw3Activity extends BaseActivity<ActivityDetailKgw3Binding> {
    public static final String TAG = DeviceDetailKgw3Activity.class.getSimpleName();

    private MokoDeviceKgw3 mMokoDeviceKgw3;
    private MQTTConfigKgw3 appMqttConfig;
    private String mAppTopic;

    private boolean mScanSwitch;
    private ScanDeviceKgw3Adapter mAdapter;
    private ArrayList<String> mScanDevices;
    private Handler mHandler;
    private BeaconInfo mConnectedBeaconInfo;
    private boolean isOnResume;

    @Override
    protected void onCreate() {
        mMokoDeviceKgw3 = (MokoDeviceKgw3) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfigKgw3.class);
        mAppTopic = TextUtils.isEmpty(appMqttConfig.topicPublish) ? mMokoDeviceKgw3.topicSubscribe : appMqttConfig.topicPublish;
        mHandler = new Handler(Looper.getMainLooper());

        mBind.tvDeviceName.setText(mMokoDeviceKgw3.name);
        mScanDevices = new ArrayList<>();
        mAdapter = new ScanDeviceKgw3Adapter();
        mAdapter.openLoadAnimation();
        mAdapter.replaceData(mScanDevices);
        mBind.rvDevices.setLayoutManager(new LinearLayoutManager(this));
        mBind.rvDevices.setAdapter(mAdapter);

        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        showLoadingProgressDialog();
        getScanConfig();
    }

    @Override
    protected ActivityDetailKgw3Binding getViewBinding() {
        return ActivityDetailKgw3Binding.inflate(getLayoutInflater());
    }

    private void changeView() {
        mBind.ivScanSwitch.setImageResource(mScanSwitch ? R.drawable.checkbox_open : R.drawable.checkbox_close);
        mBind.tvScanDeviceTotal.setVisibility(mScanSwitch ? View.VISIBLE : View.GONE);
        mBind.tvScanDeviceTotal.setText(getString(R.string.scan_device_total, mScanDevices.size()));
        mBind.tvManageDevices.setVisibility(mScanSwitch ? View.VISIBLE : View.GONE);
        mBind.rvDevices.setVisibility(mScanSwitch ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        XLog.i(TAG + "-->onNewIntent...");
        setIntent(intent);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTMessageArrivedEvent(MQTTMessageArrivedEvent event) {
        // 更新所有设备的网络状态
        final String topic = event.getTopic();
        final String message = event.getMessage();
        if (TextUtils.isEmpty(message))
            return;
        int msg_id;
        try {
            JsonObject object = new Gson().fromJson(message, JsonObject.class);
            JsonElement element = object.get("msg_id");
            msg_id = element.getAsInt();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        if (msg_id == MQTTConstants.READ_MSG_ID_SCAN_CONFIG) {
            Type type = new TypeToken<MsgReadResult<JsonObject>>() {
            }.getType();
            MsgReadResult<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            mScanSwitch = result.data.get("scan_switch").getAsInt() == 1;
            changeView();
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_SCAN_RESULT) {
            Type type = new TypeToken<MsgNotify<List<JsonObject>>>() {
            }.getType();
            MsgNotify<List<JsonObject>> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            for (JsonObject jsonObject : result.data) {
                mScanDevices.add(0, jsonObject.toString());
            }
            mBind.tvScanDeviceTotal.setText(getString(R.string.scan_device_total, mScanDevices.size()));
            mAdapter.replaceData(mScanDevices);
        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_SCAN_CONFIG) {
            Type type = new TypeToken<MsgConfigResult>() {
            }.getType();
            MsgConfigResult result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            if (result.result_code == 0) {
                ToastUtils.showToast(this, "Set up succeed");
            } else {
                ToastUtils.showToast(this, "Set up failed");
            }
        }
        if (msg_id == MQTTConstants.READ_MSG_ID_BLE_CONNECTED_LIST) {
            Type type = new TypeToken<MsgReadResult<BleConnectedList>>() {
            }.getType();
            MsgReadResult<BleConnectedList> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            if (result.data.ble_conn_list != null && !result.data.ble_conn_list.isEmpty()) {
                // 当前连接的设备有值
                BleConnectedList.BleDevice bleDevice = result.data.ble_conn_list.get(0);
                // 根据类型请求不同数据
                if (bleDevice.type > 0) {
                    mConnectedBeaconInfo = new BeaconInfo();
                    mConnectedBeaconInfo.type = bleDevice.type;
                    readConnectedBeaconInfo(bleDevice.mac, bleDevice.type);
                } else {
                    readConnectedOtherInfo(bleDevice.mac);
                }
            } else {
                Intent intent = new Intent(this, BleManagerKgw3Activity.class);
                intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
                startActivity(intent);
            }
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_B_D_INFO
                || msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_B_CR_INFO
                || msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_C_INFO
                || msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_D_INFO
                || msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_T_INFO
                || msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_S_INFO
                || msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_MK_PIR_INFO
                || msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_MK_TOF_INFO) {
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            Type type = new TypeToken<MsgNotify<BeaconInfo>>() {
            }.getType();
            MsgNotify<BeaconInfo> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            BeaconInfo beaconInfo = result.data;
            if (beaconInfo.result_code != 0) {
                ToastUtils.showToast(this, "Setup failed");
                return;
            }
            mConnectedBeaconInfo.mac = beaconInfo.mac;
            mConnectedBeaconInfo.product_model = beaconInfo.product_model;
            mConnectedBeaconInfo.company_name = beaconInfo.company_name;
            mConnectedBeaconInfo.hardware_version = beaconInfo.hardware_version;
            mConnectedBeaconInfo.software_version = beaconInfo.software_version;
            mConnectedBeaconInfo.firmware_version = beaconInfo.firmware_version;
            mConnectedBeaconInfo.sensor_status = beaconInfo.sensor_status;
            mConnectedBeaconInfo.axis_type = beaconInfo.axis_type;
            mConnectedBeaconInfo.th_type = beaconInfo.th_type;
            mConnectedBeaconInfo.light_type = beaconInfo.light_type;
            mConnectedBeaconInfo.pir_type = beaconInfo.pir_type;
            mConnectedBeaconInfo.tof_type = beaconInfo.tof_type;
            if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_B_CR_INFO) {
                readConnectedBeaconStatus(beaconInfo.mac, MQTTConstants.CONFIG_MSG_ID_BLE_BXP_B_CR_STATUS);
            } else if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_C_INFO) {
                readConnectedBeaconStatus(beaconInfo.mac, MQTTConstants.CONFIG_MSG_ID_BLE_BXP_C_STATUS);
            } else if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_D_INFO) {
                readConnectedBeaconStatus(beaconInfo.mac, MQTTConstants.CONFIG_MSG_ID_BLE_BXP_D_STATUS);
            } else if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_T_INFO) {
                readConnectedBeaconStatus(beaconInfo.mac, MQTTConstants.CONFIG_MSG_ID_BLE_BXP_T_STATUS);
            } else if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_S_INFO) {
                readConnectedBeaconStatus(beaconInfo.mac, MQTTConstants.CONFIG_MSG_ID_BLE_BXP_S_STATUS);
            } else if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_MK_PIR_INFO) {
                readConnectedBeaconStatus(beaconInfo.mac, MQTTConstants.CONFIG_MSG_ID_BLE_MK_PIR_STATUS);
            } else if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_MK_TOF_INFO) {
                readConnectedBeaconStatus(beaconInfo.mac, MQTTConstants.CONFIG_MSG_ID_BLE_MK_TOF_STATUS);
            } else {
                readConnectedBeaconStatus(beaconInfo.mac, MQTTConstants.CONFIG_MSG_ID_BLE_BXP_B_D_STATUS);
            }
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_B_D_STATUS
                || msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_B_CR_STATUS
                || msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_C_STATUS
                || msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_D_STATUS
                || msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_T_STATUS
                || msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_S_STATUS
                || msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_MK_PIR_STATUS
                || msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_MK_TOF_STATUS) {
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            Type type = new TypeToken<MsgNotify<BeaconInfo>>() {
            }.getType();
            MsgNotify<BeaconInfo> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            BeaconInfo beaconInfo = result.data;
            if (beaconInfo.result_code != 0) {
                ToastUtils.showToast(this, "Setup failed");
                return;
            }
            mConnectedBeaconInfo.battery_v = beaconInfo.battery_v;
            mConnectedBeaconInfo.battery_level = beaconInfo.battery_level;
            mConnectedBeaconInfo.single_alarm_num = beaconInfo.single_alarm_num;
            mConnectedBeaconInfo.double_alarm_num = beaconInfo.double_alarm_num;
            mConnectedBeaconInfo.long_alarm_num = beaconInfo.long_alarm_num;
            mConnectedBeaconInfo.alarm_status = beaconInfo.alarm_status;
            mConnectedBeaconInfo.run_time = beaconInfo.run_time;
            ToastUtils.showToast(this, "Setup succeed");
            Intent intent;
            if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_B_CR_STATUS) {
                intent = new Intent(this, BXPBCRGW3Activity.class);
            } else if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_C_STATUS) {
                intent = new Intent(this, BXPCGW3Activity.class);
            } else if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_D_STATUS) {
                intent = new Intent(this, BXPDGW3Activity.class);
            } else if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_T_STATUS) {
                intent = new Intent(this, BXPTGW3Activity.class);
            } else if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_S_STATUS) {
                intent = new Intent(this, BXPSGW3Activity.class);
            } else if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_MK_PIR_STATUS) {
                intent = new Intent(this, MKPIRGW3Activity.class);
            } else if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_MK_TOF_STATUS) {
                intent = new Intent(this, MKTOFGW3Activity.class);
            } else {
                intent = new Intent(this, BXPButtonInfoKgw3Activity.class);
                if (mMokoDeviceKgw3.deviceType == 1)
                    intent = new Intent(this, BXPBDGW3Activity.class);
            }
            intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
            intent.putExtra(AppConstants.EXTRA_KEY_BEACON_INFO, mConnectedBeaconInfo);
            startActivity(intent);
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_OTHER_INFO) {
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            Type type = new TypeToken<MsgNotify<OtherDeviceInfo>>() {
            }.getType();
            MsgNotify<OtherDeviceInfo> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            OtherDeviceInfo otherDeviceInfo = result.data;
            if (otherDeviceInfo.result_code != 0) {
                ToastUtils.showToast(this, "Setup failed");
                return;
            }
            ToastUtils.showToast(this, "Setup succeed");
            Intent intent = new Intent(this, BleOtherInfoKgw3Activity.class);
            intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
            intent.putExtra(AppConstants.EXTRA_KEY_OTHER_DEVICE_INFO, otherDeviceInfo);
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isOnResume = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isOnResume = false;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceModifyNameEvent(DeviceModifyNameEvent event) {
        // 修改了设备名称
        MokoDeviceKgw3 device = MKgw3DBTools.getInstance(DeviceDetailKgw3Activity.this).selectDevice(mMokoDeviceKgw3.mac);
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

    public void onBack(View view) {
        finish();
    }

    public void onDeviceSetting(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent intent = new Intent(this, DeviceSettingKgw3Activity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
        startActivity(intent);
    }

    public void onScannerOptionSetting(View view) {
        if (isWindowLocked())
            return;
        // 获取扫描过滤
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, ScannerUploadOptionKgw3Activity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
        startActivity(i);
    }

    public void onScanSwitch(View view) {
        if (isWindowLocked())
            return;
        // 切换扫描开关
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        mScanSwitch = !mScanSwitch;
        mBind.ivScanSwitch.setImageResource(mScanSwitch ? R.drawable.checkbox_open : R.drawable.checkbox_close);
        mBind.tvManageDevices.setVisibility(mScanSwitch ? View.VISIBLE : View.GONE);
        mBind.tvScanDeviceTotal.setVisibility(mScanSwitch ? View.VISIBLE : View.GONE);
        mBind.tvScanDeviceTotal.setText(getString(R.string.scan_device_total, 0));
        mBind.rvDevices.setVisibility(mScanSwitch ? View.VISIBLE : View.GONE);
        mScanDevices.clear();
        mAdapter.replaceData(mScanDevices);
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        setScanConfig();
    }

    public void onManageBleDevices(View view) {
        if (isWindowLocked()) return;
        // 设置扫描间隔
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        getBleConnectedList();
    }

    private void getBleConnectedList() {
        int msgId = MQTTConstants.READ_MSG_ID_BLE_CONNECTED_LIST;
        String message = assembleReadCommon(msgId, mMokoDeviceKgw3.mac);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getScanConfig() {
        int msgId = MQTTConstants.READ_MSG_ID_SCAN_CONFIG;
        String message = assembleReadCommon(msgId, mMokoDeviceKgw3.mac);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setScanConfig() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_SCAN_CONFIG;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("scan_switch", mScanSwitch ? 1 : 0);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    private void readConnectedOtherInfo(String mac) {
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Setup failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        getOtherInfo(mac);
    }

    private void getOtherInfo(String mac) {
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_OTHER_INFO;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", mac);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void readConnectedBeaconInfo(String mac, int type) {
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Setup failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        getConnectedBeaconInfo(mac, type);
    }

    private void getConnectedBeaconInfo(String mac, int type) {
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_B_D_INFO;
        if (type == 2)
            msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_B_CR_INFO;
        if (type == 3)
            msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_C_INFO;
        if (type == 4)
            msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_D_INFO;
        if (type == 5)
            msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_T_INFO;
        if (type == 6)
            msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_S_INFO;
        if (type == 7)
            msgId = MQTTConstants.CONFIG_MSG_ID_BLE_MK_PIR_INFO;
        if (type == 8)
            msgId = MQTTConstants.CONFIG_MSG_ID_BLE_MK_TOF_INFO;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", mac);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void readConnectedBeaconStatus(String mac, int msgId) {
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Setup failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        getConnectedBeaconStatus(mac, msgId);
    }

    private void getConnectedBeaconStatus(String mac, int msgId) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", mac);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
