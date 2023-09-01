package com.moko.mkgw3.activity;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkgw3.AppConstants;
import com.moko.mkgw3.R;
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
import com.moko.support.mkgw3.entity.BXPButtonInfo;
import com.moko.support.mkgw3.entity.BleConnectedList;
import com.moko.support.mkgw3.entity.BxpCInfo;
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

public class DeviceDetailKgw3Activity extends BaseActivity<ActivityDetailKgw3Binding> {
    public static final String TAG = DeviceDetailKgw3Activity.class.getSimpleName();

    private MokoDeviceKgw3 mMokoDeviceKgw3;
    private MQTTConfigKgw3 appMqttConfig;
    private String mAppTopic;

    private boolean mScanSwitch;
    private ScanDeviceKgw3Adapter mAdapter;
    private ArrayList<String> mScanDevices;
    private Handler mHandler;
    private BXPButtonInfo mConnectedBXPButtonInfo;
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
                if (bleDevice.type == 1 || bleDevice.type == 2) {
                    if (bleDevice.type == 1) mConnectedBXPButtonInfo = new BXPButtonInfo();
                    readBXPButtonInfo(bleDevice.mac, bleDevice.type);
                } else if (bleDevice.type == 0) {
                    readOtherInfo(bleDevice.mac);
                }
            } else {
                Intent intent = new Intent(this, SelectBeaconTypeActivity.class);
                intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
                startActivity(intent);
            }
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_BUTTON_INFO) {
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            Type type = new TypeToken<MsgNotify<BXPButtonInfo>>() {
            }.getType();
            MsgNotify<BXPButtonInfo> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            BXPButtonInfo bxpButtonInfo = result.data;
            if (bxpButtonInfo.result_code != 0) {
                ToastUtils.showToast(this, "Setup failed");
                return;
            }
            mConnectedBXPButtonInfo.mac = bxpButtonInfo.mac;
            mConnectedBXPButtonInfo.product_model = bxpButtonInfo.product_model;
            mConnectedBXPButtonInfo.company_name = bxpButtonInfo.company_name;
            mConnectedBXPButtonInfo.hardware_version = bxpButtonInfo.hardware_version;
            mConnectedBXPButtonInfo.software_version = bxpButtonInfo.software_version;
            mConnectedBXPButtonInfo.firmware_version = bxpButtonInfo.firmware_version;
            readBXPButtonStatus(bxpButtonInfo.mac);
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_C_INFO) {
            if (!isOnResume) return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            Type type = new TypeToken<MsgNotify<BxpCInfo>>() {
            }.getType();
            MsgNotify<BxpCInfo> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            BxpCInfo bxpCInfo = result.data;
            if (bxpCInfo.result_code != 0) {
                ToastUtils.showToast(this, "Setup failed");
                return;
            }
            Intent intent = new Intent(this, BxpCInfoActivity.class);
            intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
            intent.putExtra(AppConstants.EXTRA_KEY_BXP_BUTTON_INFO, bxpCInfo);
            startActivity(intent);
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_BUTTON_STATUS) {
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            Type type = new TypeToken<MsgNotify<BXPButtonInfo>>() {
            }.getType();
            MsgNotify<BXPButtonInfo> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            BXPButtonInfo bxpButtonInfo = result.data;
            if (bxpButtonInfo.result_code != 0) {
                ToastUtils.showToast(this, "Setup failed");
                return;
            }
            mConnectedBXPButtonInfo.battery_v = bxpButtonInfo.battery_v;
            mConnectedBXPButtonInfo.single_alarm_num = bxpButtonInfo.single_alarm_num;
            mConnectedBXPButtonInfo.double_alarm_num = bxpButtonInfo.double_alarm_num;
            mConnectedBXPButtonInfo.long_alarm_num = bxpButtonInfo.long_alarm_num;
            mConnectedBXPButtonInfo.alarm_status = bxpButtonInfo.alarm_status;
            ToastUtils.showToast(this, "Setup succeed");
            Intent intent = new Intent(this, BXPButtonInfoKgw3Activity.class);
            intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
            intent.putExtra(AppConstants.EXTRA_KEY_BXP_BUTTON_INFO, mConnectedBXPButtonInfo);
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


    private void readOtherInfo(String mac) {
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

    public void readBXPButtonInfo(String mac, int type) {
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Setup failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        getBXPButtonInfo(mac, type);
    }

    private void getBXPButtonInfo(String mac, int type) {
        int msgId = type == 1 ? MQTTConstants.CONFIG_MSG_ID_BLE_BXP_BUTTON_INFO : MQTTConstants.CONFIG_MSG_ID_BLE_BXP_C_INFO;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", mac);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void readBXPButtonStatus(String mac) {
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Setup failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        getBXPButtonStatus(mac);
    }

    private void getBXPButtonStatus(String mac) {
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_BUTTON_STATUS;
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
