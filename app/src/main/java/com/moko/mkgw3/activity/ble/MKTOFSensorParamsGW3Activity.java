package com.moko.mkgw3.activity.ble;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkgw3.AppConstants;
import com.moko.mkgw3.base.BaseActivity;
import com.moko.mkgw3.databinding.ActivityMkTofSensorParamsKgw3Binding;
import com.moko.mkgw3.dialog.MKgw3BottomDialog;
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
import java.util.ArrayList;

public class MKTOFSensorParamsGW3Activity extends BaseActivity<ActivityMkTofSensorParamsKgw3Binding> {
    private MokoDeviceKgw3 mMokoDeviceKgw3;
    private MQTTConfigKgw3 appMqttConfig;
    private String mAppTopic;
    private String mMac;
    private Handler mHandler;
    private ArrayList<String> mValues;

    @Override
    protected ActivityMkTofSensorParamsKgw3Binding getViewBinding() {
        return ActivityMkTofSensorParamsKgw3Binding.inflate(getLayoutInflater());
    }

    @Override
    protected void onCreate() {
        mMokoDeviceKgw3 = (MokoDeviceKgw3) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfigKgw3.class);
        mAppTopic = TextUtils.isEmpty(appMqttConfig.topicPublish) ? mMokoDeviceKgw3.topicSubscribe : appMqttConfig.topicPublish;
        mValues = new ArrayList<>();
        mValues.add("Short distance");
        mValues.add("Long distance");

        mBind.tvMode.setOnClickListener(v -> openDialog(v));

        mMac = getIntent().getStringExtra(AppConstants.EXTRA_KEY_MAC);

        mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Setup failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        getSensorParams();
    }

    private void openDialog(View v) {
        int selected = (int) v.getTag();
        MKgw3BottomDialog dialog = new MKgw3BottomDialog();
        dialog.setDatas(mValues, selected);
        dialog.setListener(value -> {
            ((TextView) v).setText(mValues.get(value));
            v.setTag(value);
        });
        dialog.show(getSupportFragmentManager());
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
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_MK_TOF_SENSOR_PARAMS_WRITE) {
            Type type = new TypeToken<MsgNotify<BeaconInfo>>() {
            }.getType();
            MsgNotify<BeaconInfo> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            if (result.data.result_code != 0) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
                ToastUtils.showToast(this, "Setup failed");
                return;
            }
            setRangeMode();
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_MK_TOF_RANGE_MODE_WRITE) {
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            Type type = new TypeToken<MsgNotify<BeaconInfo>>() {
            }.getType();
            MsgNotify<BeaconInfo> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            ToastUtils.showToast(this, result.data.result_code == 0 ? "Setup succeed!" : "Setup failed");
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_MK_TOF_SENSOR_PARAMS_READ) {
            Type type = new TypeToken<MsgNotify<JsonObject>>() {
            }.getType();
            MsgNotify<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            int interval = result.data.get("interval").getAsInt();
            mBind.etSampleRate.setText(String.valueOf(interval));
            int count = result.data.get("count").getAsInt();
            mBind.etSampleCount.setText(String.valueOf(count));
            int time = result.data.get("time").getAsInt();
            mBind.etSampleTime.setText(String.valueOf(time));
            getRangeMode();
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_MK_TOF_RANGE_MODE_READ) {
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            Type type = new TypeToken<MsgNotify<JsonObject>>() {
            }.getType();
            MsgNotify<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            int delay = result.data.get("mode").getAsInt() - 1;
            mBind.tvMode.setTag(delay);
            mBind.tvMode.setText(mValues.get(delay));
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
            setSensorParams();
        } else {
            ToastUtils.showToast(this, "Para Error");
        }
    }

    private boolean isValid() {
        String sampleRateStr = mBind.etSampleRate.getText().toString();
        if (TextUtils.isEmpty(sampleRateStr)) {
            return false;
        }
        int sampleRate = Integer.parseInt(sampleRateStr);
        if (sampleRate < 1 || sampleRate > 86400)
            return false;
        String countStr = mBind.etSampleCount.getText().toString();
        if (TextUtils.isEmpty(countStr)) {
            return false;
        }
        int count = Integer.parseInt(countStr);
        if (count < 2 || count > 255)
            return false;
        String timeStr = mBind.etSampleTime.getText().toString();
        if (TextUtils.isEmpty(timeStr)) {
            return false;
        }
        int time = Integer.parseInt(timeStr);
        if (time < 8 || time > 140)
            return false;
        return true;
    }

    private void getSensorParams() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_MK_TOF_SENSOR_PARAMS_READ;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", mMac);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getRangeMode() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_MK_TOF_RANGE_MODE_READ;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", mMac);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setSensorParams() {
        int rate = Integer.parseInt(mBind.etSampleRate.getText().toString());
        int count = Integer.parseInt(mBind.etSampleCount.getText().toString());
        int time = Integer.parseInt(mBind.etSampleTime.getText().toString());
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_MK_TOF_SENSOR_PARAMS_WRITE;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", mMac);
        jsonObject.addProperty("interval", rate);
        jsonObject.addProperty("count", count);
        jsonObject.addProperty("time", time);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setRangeMode() {
        int mode = (int) mBind.tvMode.getTag() + 1;
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_MK_TOF_RANGE_MODE_WRITE;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", mMac);
        jsonObject.addProperty("mode", mode);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onBack(View view) {
        if (isWindowLocked()) return;
        finish();
    }
}
