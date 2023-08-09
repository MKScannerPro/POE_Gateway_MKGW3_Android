package com.moko.mkremotegw.activity;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkremotegw.AppConstants;
import com.moko.mkremotegw.R;
import com.moko.mkremotegw.adapter.BleCharacteristicsAdapter;
import com.moko.mkremotegw.base.BaseActivity;
import com.moko.mkremotegw.databinding.ActivityOtherInfoBinding;
import com.moko.mkremotegw.db.DBTools;
import com.moko.mkremotegw.dialog.AlertMessageDialog;
import com.moko.mkremotegw.dialog.CharWriteDialog;
import com.moko.mkremotegw.entity.BleOtherChar;
import com.moko.mkremotegw.entity.MQTTConfig;
import com.moko.mkremotegw.entity.MokoDevice;
import com.moko.mkremotegw.utils.SPUtiles;
import com.moko.mkremotegw.utils.ToastUtils;
import com.moko.support.remotegw.MQTTConstants;
import com.moko.support.remotegw.MQTTSupport;
import com.moko.support.remotegw.entity.BleCharResponse;
import com.moko.support.remotegw.entity.BleCharacteristic;
import com.moko.support.remotegw.entity.BleService;
import com.moko.support.remotegw.entity.MsgNotify;
import com.moko.support.remotegw.entity.OtherDeviceInfo;
import com.moko.support.remotegw.event.DeviceModifyNameEvent;
import com.moko.support.remotegw.event.DeviceOnlineEvent;
import com.moko.support.remotegw.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;
import java.util.ArrayList;

import androidx.recyclerview.widget.LinearLayoutManager;

public class BleOtherInfoActivity extends BaseActivity<ActivityOtherInfoBinding> implements BaseQuickAdapter.OnItemChildClickListener {

    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private String mAppTopic;

    private OtherDeviceInfo mOtherDeviceInfo;
    private Handler mHandler;
    private ArrayList<BleOtherChar> mBleOtherChars;
    private BleCharacteristicsAdapter mAdapter;

    @Override
    protected void onCreate() {
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mAppTopic = TextUtils.isEmpty(appMqttConfig.topicPublish) ? mMokoDevice.topicSubscribe : appMqttConfig.topicPublish;
        mHandler = new Handler(Looper.getMainLooper());

        mOtherDeviceInfo = (OtherDeviceInfo) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_OTHER_DEVICE_INFO);
        mBind.tvDeviceName.setText(mMokoDevice.name);
        mBleOtherChars = new ArrayList<>();
        String mac = mOtherDeviceInfo.mac;
        for (int i = 0; i < mOtherDeviceInfo.service_array.size(); i++) {
            BleService bleService = mOtherDeviceInfo.service_array.get(i);
            // Service
            BleOtherChar bleOtherService = new BleOtherChar();
            bleOtherService.mac = mac;
            bleOtherService.type = 0;
            bleOtherService.serviceUUID = bleService.service_uuid;
            mBleOtherChars.add(bleOtherService);
            for (int j = 0; j < bleService.char_array.size(); j++) {
                BleCharacteristic characteristic = bleService.char_array.get(j);
                // Service
                BleOtherChar bleOtherChar = new BleOtherChar();
                bleOtherChar.mac = mac;
                bleOtherChar.type = 1;
                bleOtherChar.serviceUUID = bleService.service_uuid;
                bleOtherChar.characteristicUUID = characteristic.char_uuid;
                bleOtherChar.characteristicProperties = characteristic.properties;
                bleOtherChar.characteristicNotifyStatus = characteristic.notify_status;
                mBleOtherChars.add(bleOtherChar);
            }
        }
        mAdapter = new BleCharacteristicsAdapter(mBleOtherChars);
        mAdapter.openLoadAnimation();
        mAdapter.setOnItemChildClickListener(this);
        mBind.rvCharacteristics.setLayoutManager(new LinearLayoutManager(this));
        mBind.rvCharacteristics.setAdapter(mAdapter);
    }

    @Override
    protected ActivityOtherInfoBinding getViewBinding() {
        return ActivityOtherInfoBinding.inflate(getLayoutInflater());
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
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_OTHER_CHANGE_NOTIFY_ENABLE) {
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            Type type = new TypeToken<MsgNotify<BleCharResponse>>() {
            }.getType();
            MsgNotify<BleCharResponse> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            BleCharResponse charResponse = result.data;
            if (charResponse.result_code != 0) {
                ToastUtils.showToast(this, "Setup failed");
                return;
            }
            // 更新item
            for (BleOtherChar bleOtherChar : mBleOtherChars) {
                if (bleOtherChar.type == 1
                        && charResponse.service_uuid.equalsIgnoreCase(bleOtherChar.serviceUUID)
                        && charResponse.char_uuid.equalsIgnoreCase(bleOtherChar.characteristicUUID)) {
                    bleOtherChar.characteristicNotifyStatus = bleOtherChar.characteristicNotifyStatus == 1 ? 0 : 1;
                    break;
                }
            }
            mAdapter.replaceData(mBleOtherChars);
            ToastUtils.showToast(this, "Setup succeed!");
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_OTHER_READ_CHAR_VALUE) {
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            Type type = new TypeToken<MsgNotify<BleCharResponse>>() {
            }.getType();
            MsgNotify<BleCharResponse> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            BleCharResponse charResponse = result.data;
            if (charResponse.result_code != 0) {
                ToastUtils.showToast(this, "Setup failed");
                return;
            }
            // 更新item
            for (BleOtherChar bleOtherChar : mBleOtherChars) {
                if (bleOtherChar.type == 1
                        && charResponse.service_uuid.equalsIgnoreCase(bleOtherChar.serviceUUID)
                        && charResponse.char_uuid.equalsIgnoreCase(bleOtherChar.characteristicUUID)) {
                    bleOtherChar.characteristicPayload = charResponse.payload;
                    break;
                }
            }
            mAdapter.replaceData(mBleOtherChars);
            ToastUtils.showToast(this, "Setup succeed!");
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_OTHER_NOTIFY_CHAR_VALUE) {
            Type type = new TypeToken<MsgNotify<BleCharResponse>>() {
            }.getType();
            MsgNotify<BleCharResponse> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            BleCharResponse charResponse = result.data;
            // 更新item
            for (BleOtherChar bleOtherChar : mBleOtherChars) {
                if (bleOtherChar.type == 1
                        && charResponse.service_uuid.equalsIgnoreCase(bleOtherChar.serviceUUID)
                        && charResponse.char_uuid.equalsIgnoreCase(bleOtherChar.characteristicUUID)) {
                    bleOtherChar.characteristicPayload = charResponse.payload;
                    break;
                }
            }
            mAdapter.replaceData(mBleOtherChars);
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_OTHER_WRITE_CHAR_VALUE) {
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            Type type = new TypeToken<MsgNotify<BleCharResponse>>() {
            }.getType();
            MsgNotify<BleCharResponse> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            BleCharResponse charResponse = result.data;
            if (charResponse.result_code != 0) {
                ToastUtils.showToast(this, "Setup failed");
                return;
            }
            ToastUtils.showToast(this, "Setup succeed!");
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_OTHER_DISCONNECTED
                || msg_id == MQTTConstants.CONFIG_MSG_ID_BLE_DISCONNECT) {
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            Type type = new TypeToken<MsgNotify<JsonObject>>() {
            }.getType();
            MsgNotify<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            ToastUtils.showToast(this, "Bluetooth disconnect");
            finish();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceModifyNameEvent(DeviceModifyNameEvent event) {
        // 修改了设备名称
        MokoDevice device = DBTools.getInstance(BleOtherInfoActivity.this).selectDevice(mMokoDevice.mac);
        mMokoDevice.name = device.name;
        mBind.tvDeviceName.setText(mMokoDevice.name);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceOnlineEvent(DeviceOnlineEvent event) {
        String mac = event.getMac();
        if (!mMokoDevice.mac.equals(mac))
            return;
        boolean online = event.isOnline();
        if (!online) {
            ToastUtils.showToast(this, "device is off-line");
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (isWindowLocked()) return;
        backToDetail();
    }

    public void onBack(View view) {
        if (isWindowLocked()) return;
        backToDetail();
    }

    private void backToDetail() {
        Intent intent = new Intent(this, DeviceDetailActivity.class);
        startActivity(intent);
    }

    public void onDisconnect(View view) {
        if (isWindowLocked()) return;
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setMessage("Please confirm again whether to disconnect the gateway from BLE devices?");
        dialog.setOnAlertConfirmListener(() -> {
            if (!MQTTSupport.getInstance().isConnected()) {
                ToastUtils.showToast(this, R.string.network_error);
                return;
            }
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(BleOtherInfoActivity.this, "Setup failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            disconnectDevice();
        });
        dialog.show(getSupportFragmentManager());
    }

    private void disconnectDevice() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_DISCONNECT;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", mOtherDeviceInfo.mac);
        String message = assembleWriteCommonData(msgId, mMokoDevice.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
        if (isWindowLocked()) return;
        BleOtherChar bleOtherChar = (BleOtherChar) adapter.getItem(position);
        if (bleOtherChar == null) return;
        if (view.getId() == R.id.iv_notify) {
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Setup failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            changeNotifyEnable(bleOtherChar);
        }
        if (view.getId() == R.id.iv_read) {
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Setup failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            readCharValue(bleOtherChar);
        }
        if (view.getId() == R.id.iv_write) {
            openWriteCharValueDialog(bleOtherChar);
        }
    }

    private void openWriteCharValueDialog(BleOtherChar bleOtherChar) {
        CharWriteDialog dialog = new CharWriteDialog();
        dialog.setOnCharWriteClicked(payload -> {
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Setup failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            openWriteCharValueDialog(bleOtherChar, payload);
        });
        dialog.show(getSupportFragmentManager());
    }

    private void openWriteCharValueDialog(BleOtherChar bleOtherChar, String payload) {
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_OTHER_WRITE_CHAR_VALUE;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", bleOtherChar.mac);
        jsonObject.addProperty("service_uuid", bleOtherChar.serviceUUID);
        jsonObject.addProperty("char_uuid", bleOtherChar.characteristicUUID);
        jsonObject.addProperty("payload", payload);
        String message = assembleWriteCommonData(msgId, mMokoDevice.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void readCharValue(BleOtherChar bleOtherChar) {
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_OTHER_READ_CHAR_VALUE;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", bleOtherChar.mac);
        jsonObject.addProperty("service_uuid", bleOtherChar.serviceUUID);
        jsonObject.addProperty("char_uuid", bleOtherChar.characteristicUUID);
        String message = assembleWriteCommonData(msgId, mMokoDevice.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void changeNotifyEnable(BleOtherChar bleOtherChar) {
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_OTHER_CHANGE_NOTIFY_ENABLE;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", bleOtherChar.mac);
        jsonObject.addProperty("service_uuid", bleOtherChar.serviceUUID);
        jsonObject.addProperty("char_uuid", bleOtherChar.characteristicUUID);
        jsonObject.addProperty("switch_value", bleOtherChar.characteristicNotifyStatus == 1 ? 0 : 1);
        String message = assembleWriteCommonData(msgId, mMokoDevice.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
