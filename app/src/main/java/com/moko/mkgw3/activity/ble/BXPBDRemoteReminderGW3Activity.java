package com.moko.mkgw3.activity.ble;

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
import com.moko.mkgw3.databinding.ActivityBxpBDRemoteReminderKgw3Binding;
import com.moko.mkgw3.entity.MQTTConfigKgw3;
import com.moko.mkgw3.entity.MokoDeviceKgw3;
import com.moko.mkgw3.utils.SPUtiles;
import com.moko.mkgw3.utils.ToastUtils;
import com.moko.support.mkgw3.MQTTConstants;
import com.moko.support.mkgw3.MQTTSupport;
import com.moko.support.mkgw3.entity.BeaconInfo;
import com.moko.support.mkgw3.entity.MsgNotify;
import com.moko.support.mkgw3.event.DeviceOnlineEvent;
import com.moko.support.mkgw3.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;

public class BXPBDRemoteReminderGW3Activity extends BaseActivity<ActivityBxpBDRemoteReminderKgw3Binding> {
    private MokoDeviceKgw3 mMokoDeviceKgw3;
    private MQTTConfigKgw3 appMqttConfig;
    private String mAppTopic;
    private String mMac;
    private Handler mHandler;

    @Override
    protected void onCreate() {
        mMokoDeviceKgw3 = (MokoDeviceKgw3) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfigKgw3.class);
        mAppTopic = TextUtils.isEmpty(appMqttConfig.topicPublish) ? mMokoDeviceKgw3.topicSubscribe : appMqttConfig.topicPublish;
        mHandler = new Handler(Looper.getMainLooper());

        mMac = getIntent().getStringExtra(AppConstants.EXTRA_KEY_MAC);
    }

    @Override
    protected ActivityBxpBDRemoteReminderKgw3Binding getViewBinding() {
        return ActivityBxpBDRemoteReminderKgw3Binding.inflate(getLayoutInflater());
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
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_BUTTON_LED || msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_BUTTON_BUZZER) {
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            Type type = new TypeToken<MsgNotify<BeaconInfo>>() {
            }.getType();
            MsgNotify<BeaconInfo> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            ToastUtils.showToast(this, result.data.result_code == 0 ? "Setup succeed!" : "Setup failed");
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

    public void onLedNotifyRemind(View view) {
        if (isWindowLocked()) return;
        if (isLEDValid()) {
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Setup failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            setLEDNotifyRemind();
        } else {
            ToastUtils.showToast(this, "Para Error");
        }
    }

    public void onBuzzerNotifyRemind(View view) {
        if (isWindowLocked()) return;
        if (isBuzzerValid()) {
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Setup failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            setBuzzerNotifyRemind();
        } else {
            ToastUtils.showToast(this, "Para Error");
        }
    }

    private void setLEDNotifyRemind() {
        String ledTimeStr = mBind.etBlinkingTime.getText().toString();
        String ledIntervalStr = mBind.etBlinkingInterval.getText().toString();
        int ledTime = Integer.parseInt(ledTimeStr);
        int ledInterval = Integer.parseInt(ledIntervalStr);
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_BUTTON_LED;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", mMac);
        jsonObject.addProperty("flash_time", ledTime);
        jsonObject.addProperty("flash_interval", ledInterval);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setBuzzerNotifyRemind() {
        String buzzerTimeStr = mBind.etRingingTime.getText().toString();
        String buzzerIntervalStr = mBind.etRingingInterval.getText().toString();
        int buzzerTime = Integer.parseInt(buzzerTimeStr);
        int buzzerInterval = Integer.parseInt(buzzerIntervalStr);
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_BUTTON_BUZZER;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", mMac);
        jsonObject.addProperty("ring_time", buzzerTime);
        jsonObject.addProperty("ring_interval", buzzerInterval);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private boolean isBuzzerValid() {
        String buzzerTimeStr = mBind.etRingingTime.getText().toString();
        String buzzerIntervalStr = mBind.etRingingInterval.getText().toString();
        if (TextUtils.isEmpty(buzzerTimeStr) || TextUtils.isEmpty(buzzerIntervalStr)) {
            return false;
        }
        int buzzerTime = Integer.parseInt(buzzerTimeStr);
        if (buzzerTime < 1 || buzzerTime > 6000)
            return false;
        int buzzerInterval = Integer.parseInt(buzzerIntervalStr);
        if (buzzerInterval < 0 || buzzerInterval > 100)
            return false;
        return true;
    }

    private boolean isLEDValid() {
        String ledTimeStr = mBind.etBlinkingTime.getText().toString();
        String ledIntervalStr = mBind.etBlinkingInterval.getText().toString();
        if (TextUtils.isEmpty(ledTimeStr) || TextUtils.isEmpty(ledIntervalStr)) {
            return false;
        }
        int ledTime = Integer.parseInt(ledTimeStr);
        if (ledTime < 1 || ledTime > 6000)
            return false;
        int ledInterval = Integer.parseInt(ledIntervalStr);
        if (ledInterval < 0 || ledInterval > 100)
            return false;
        return true;
    }

    public void onBack(View view) {
        if (isWindowLocked()) return;
        finish();
    }
}
