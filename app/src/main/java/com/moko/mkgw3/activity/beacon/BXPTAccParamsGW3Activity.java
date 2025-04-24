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
import com.moko.mkgw3.databinding.ActivityBxpDAccParamsBinding;
import com.moko.lib.scannerui.dialog.BottomDialog;
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
import java.util.ArrayList;

public class BXPTAccParamsGW3Activity extends BaseActivity<ActivityBxpDAccParamsBinding> {
    private MokoDeviceKgw3 mMokoDeviceKgw3;
    private MQTTConfigKgw3 appMqttConfig;
    private String mAppTopic;
    private String mMac;
    private String mBeaconType;
    private Handler mHandler;
    private ArrayList<String> mFullScaleArray;
    private ArrayList<String> mSampleRateArray;

    @Override
    protected void onCreate() {
        mMokoDeviceKgw3 = (MokoDeviceKgw3) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfigKgw3.class);
        mAppTopic = TextUtils.isEmpty(appMqttConfig.topicPublish) ? mMokoDeviceKgw3.topicSubscribe : appMqttConfig.topicPublish;
        mFullScaleArray = new ArrayList<>();
        mFullScaleArray.add("±2g");
        mFullScaleArray.add("±4g");
        mFullScaleArray.add("±8g");
        mFullScaleArray.add("±16g");
        mSampleRateArray = new ArrayList<>();
        mSampleRateArray.add("1HZ");
        mSampleRateArray.add("10HZ");
        mSampleRateArray.add("25HZ");
        mSampleRateArray.add("50HZ");
        mSampleRateArray.add("100HZ");

        mBind.tvSampleRate.setOnClickListener(v -> openSampleRateDialog(v));
        mBind.tvFullScale.setOnClickListener(v -> openFullScaleDialog(v));

        mMac = getIntent().getStringExtra(AppConstants.EXTRA_KEY_MAC);
        mBeaconType = getIntent().getStringExtra(AppConstants.EXTRA_KEY_BEACON_TYPE);

        mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Setup failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        getAccParams();
    }

    private void openSampleRateDialog(View v) {
        int selected = (int) v.getTag();
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(mSampleRateArray, selected);
        dialog.setListener(value -> {
            mBind.tvSampleRate.setText(mSampleRateArray.get(value));
            v.setTag(value);
        });
        dialog.show(getSupportFragmentManager());
    }

    private void openFullScaleDialog(View v) {
        int selected = (int) v.getTag();
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(mFullScaleArray, selected);
        dialog.setListener(value -> {
            mBind.tvFullScale.setText(mFullScaleArray.get(value));
            v.setTag(value);
        });
        dialog.show(getSupportFragmentManager());
    }

    @Override
    protected ActivityBxpDAccParamsBinding getViewBinding() {
        return ActivityBxpDAccParamsBinding.inflate(getLayoutInflater());
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
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_T_ACC_PARAMS_WRITE) {
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            Type type = new TypeToken<MsgNotify<BeaconInfo>>() {
            }.getType();
            MsgNotify<BeaconInfo> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            ToastUtils.showToast(this, result.data.result_code == 0 ? "Setup succeed!" : "Setup failed");
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_T_ACC_PARAMS_READ) {
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            Type type = new TypeToken<MsgNotify<JsonObject>>() {
            }.getType();
            MsgNotify<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            int sampleRate = result.data.get("sampling_rate").getAsInt();
            mBind.tvSampleRate.setTag(sampleRate);
            mBind.tvSampleRate.setText(mSampleRateArray.get(sampleRate));
            int fullScale = result.data.get("full_scale").getAsInt();
            mBind.tvFullScale.setTag(fullScale);
            mBind.tvFullScale.setText(mFullScaleArray.get(fullScale));
            int sensitivity = result.data.get("sensitivity").getAsInt();
            mBind.etSensitivity.setText(String.valueOf(sensitivity));
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
            setAccParams();
        } else {
            ToastUtils.showToast(this, "Para Error");
        }
    }

    private void getAccParams() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_T_ACC_PARAMS_READ;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", mMac);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setAccParams() {
        String sensitivityStr = mBind.etSensitivity.getText().toString();
        int sensitivity = Integer.parseInt(sensitivityStr);
        int sampleRate = (int) mBind.tvSampleRate.getTag();
        int fullScale = (int) mBind.tvFullScale.getTag();
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_T_ACC_PARAMS_WRITE;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", mMac);
        jsonObject.addProperty("full_scale", fullScale);
        jsonObject.addProperty("sampling_rate", sampleRate);
        jsonObject.addProperty("sensitivity", sensitivity);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private boolean isValid() {
        String sensitivityStr = mBind.etSensitivity.getText().toString();
        if (TextUtils.isEmpty(sensitivityStr)) {
            return false;
        }
        int sensitivity = Integer.parseInt(sensitivityStr);
        if (sensitivity < 1 || sensitivity > 255)
            return false;
        return true;
    }

    public void onBack(View view) {
        if (isWindowLocked()) return;
        finish();
    }
}
