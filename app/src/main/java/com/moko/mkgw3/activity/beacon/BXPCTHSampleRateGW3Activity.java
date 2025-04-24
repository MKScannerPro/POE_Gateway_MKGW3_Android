package com.moko.mkgw3.activity.beacon;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkgw3.AppConstants;
import com.moko.mkgw3.base.BaseActivity;
import com.moko.mkgw3.databinding.ActivityBxpCThSampleRateKgw3Binding;
import com.moko.mkgw3.entity.MQTTConfigKgw3;
import com.moko.mkgw3.entity.MokoDeviceKgw3;
import com.moko.mkgw3.utils.SPUtiles;
import com.moko.lib.scannerui.utils.ToastUtils;
import com.moko.support.mkgw3.MQTTConstants;
import com.moko.lib.mqtt.MQTTSupport;
import com.moko.support.mkgw3.entity.BeaconInfo;
import com.moko.lib.mqtt.entity.MsgNotify;
import com.moko.lib.mqtt.event.DeviceOnlineEvent;
import com.moko.lib.mqtt.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;

public class BXPCTHSampleRateGW3Activity extends BaseActivity<ActivityBxpCThSampleRateKgw3Binding> {
    private MokoDeviceKgw3 mMokoDeviceKgw3;
    private MQTTConfigKgw3 appMqttConfig;
    private String mAppTopic;
    private String mMac;
    private Handler mHandler;
    private int mBeaconType;

    @Override
    protected void onCreate() {
        mMokoDeviceKgw3 = (MokoDeviceKgw3) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfigKgw3.class);
        mAppTopic = TextUtils.isEmpty(appMqttConfig.topicPublish) ? mMokoDeviceKgw3.topicSubscribe : appMqttConfig.topicPublish;
        mHandler = new Handler(Looper.getMainLooper());
        mMac = getIntent().getStringExtra(AppConstants.EXTRA_KEY_MAC);
        mBeaconType = getIntent().getIntExtra(AppConstants.EXTRA_KEY_BEACON_TYPE, 1);
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Setup failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        getTHSampleRate();
    }

    @Override
    protected ActivityBxpCThSampleRateKgw3Binding getViewBinding() {
        return ActivityBxpCThSampleRateKgw3Binding.inflate(getLayoutInflater());
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
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_C_TH_SAMPLE_RATE_WRITE
                || msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_S_TH_SAMPLE_RATE_WRITE) {
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            Type type = new TypeToken<MsgNotify<BeaconInfo>>() {
            }.getType();
            MsgNotify<BeaconInfo> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            ToastUtils.showToast(this, result.data.result_code == 0 ? "Setup succeed!" : "Setup failed");
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_C_TH_SAMPLE_RATE_READ
                || msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_S_TH_SAMPLE_RATE_READ) {
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            Type type = new TypeToken<MsgNotify<JsonObject>>() {
            }.getType();
            MsgNotify<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            int sampleRate = result.data.get("sampling_rate").getAsInt();
            mBind.etSampleRate.setText(String.valueOf(sampleRate));
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_DISCONNECT) {
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            Type type = new TypeToken<MsgNotify<JsonObject>>() {
            }.getType();
            MsgNotify<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            finish();
        }
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

    public void onSave(View view) {
        if (isWindowLocked()) return;
        if (isValid()) {
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Setup failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            setSampleRate();
        } else {
            ToastUtils.showToast(this, "Para Error");
        }
    }

    private void getTHSampleRate() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_C_TH_SAMPLE_RATE_READ;
        if (mBeaconType == 6)
            msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_S_TH_SAMPLE_RATE_READ;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", mMac);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setSampleRate() {
        String sampleRateStr = mBind.etSampleRate.getText().toString();
        int sampleRate = Integer.parseInt(sampleRateStr);
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_C_TH_SAMPLE_RATE_WRITE;
        if (mBeaconType == 6)
            msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_S_TH_SAMPLE_RATE_WRITE;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", mMac);
        jsonObject.addProperty("sampling_rate", sampleRate);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private boolean isValid() {
        String sampleRateStr = mBind.etSampleRate.getText().toString();
        if (TextUtils.isEmpty(sampleRateStr)) {
            return false;
        }
        int sampleRate = Integer.parseInt(sampleRateStr);
        if (sampleRate < 1 || sampleRate > 65535)
            return false;
        return true;
    }

    public void onBack(View view) {
        if (isWindowLocked()) return;
        finish();
    }
}
