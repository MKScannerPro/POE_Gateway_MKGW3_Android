package com.moko.mkremotegw.activity;


import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkremotegw.AppConstants;
import com.moko.mkremotegw.base.BaseActivity;
import com.moko.mkremotegw.databinding.ActivityFilterIbeaconBinding;
import com.moko.mkremotegw.entity.MQTTConfig;
import com.moko.mkremotegw.entity.MokoDevice;
import com.moko.mkremotegw.utils.SPUtiles;
import com.moko.mkremotegw.utils.ToastUtils;
import com.moko.support.remotegw.MQTTConstants;
import com.moko.support.remotegw.MQTTSupport;
import com.moko.support.remotegw.entity.MsgConfigResult;
import com.moko.support.remotegw.entity.MsgReadResult;
import com.moko.support.remotegw.event.DeviceOnlineEvent;
import com.moko.support.remotegw.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;

public class FilterIBeaconActivity extends BaseActivity<ActivityFilterIbeaconBinding> {

    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private String mAppTopic;

    public Handler mHandler;

    @Override
    protected void onCreate() {
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mAppTopic = TextUtils.isEmpty(appMqttConfig.topicPublish) ? mMokoDevice.topicSubscribe : appMqttConfig.topicPublish;
        mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        showLoadingProgressDialog();
        getFilterIBeacon();
    }

    @Override
    protected ActivityFilterIbeaconBinding getViewBinding() {
        return ActivityFilterIbeaconBinding.inflate(getLayoutInflater());
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
        if (msg_id == MQTTConstants.READ_MSG_ID_FILTER_IBEACON) {
            Type type = new TypeToken<MsgReadResult<JsonObject>>() {
            }.getType();
            MsgReadResult<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            mBind.cbIbeacon.setChecked(result.data.get("switch_value").getAsInt() == 1);
            mBind.etIbeaconUuid.setText(result.data.get("uuid").getAsString());
            mBind.etIbeaconMajorMin.setText(String.valueOf(result.data.get("min_major").getAsInt()));
            mBind.etIbeaconMajorMax.setText(String.valueOf(result.data.get("max_major").getAsInt()));
            mBind.etIbeaconMinorMin.setText(String.valueOf(result.data.get("min_minor").getAsInt()));
            mBind.etIbeaconMinorMax.setText(String.valueOf(result.data.get("max_minor").getAsInt()));
        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_FILTER_IBEACON) {
            Type type = new TypeToken<MsgConfigResult>() {
            }.getType();
            MsgConfigResult result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            if (result.result_code == 0) {
                ToastUtils.showToast(this, "Set up succeed");
            } else {
                ToastUtils.showToast(this, "Set up failed");
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceOnlineEvent(DeviceOnlineEvent event) {
        super.offline(event, mMokoDevice.mac);
    }

    public void onBack(View view) {
        finish();
    }

    private void getFilterIBeacon() {
        int msgId = MQTTConstants.READ_MSG_ID_FILTER_IBEACON;
        String message = assembleReadCommon(msgId, mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onSave(View view) {
        if (isWindowLocked()) return;
        if (isValid()) {
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Set up failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            saveParams();
        }
    }


    private void saveParams() {
        String majorMinStr = mBind.etIbeaconMajorMin.getText().toString();
        String majorMaxStr = mBind.etIbeaconMajorMax.getText().toString();
        int majorMin = 0;
        int majorMax = 65535;
        if (!TextUtils.isEmpty(majorMinStr))
            majorMin = Integer.parseInt(majorMinStr);
        if (!TextUtils.isEmpty(majorMaxStr))
            majorMax = Integer.parseInt(majorMaxStr);
        String minorMinStr = mBind.etIbeaconMinorMin.getText().toString();
        String minorMaxStr = mBind.etIbeaconMinorMax.getText().toString();
        int minorMin = 0;
        int minorMax = 65535;
        if (!TextUtils.isEmpty(minorMinStr))
            minorMin = Integer.parseInt(minorMinStr);
        if (!TextUtils.isEmpty(minorMaxStr))
            minorMax = Integer.parseInt(minorMaxStr);
        int msgId = MQTTConstants.CONFIG_MSG_ID_FILTER_IBEACON;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("switch_value", mBind.cbIbeacon.isChecked() ? 1 : 0);
        jsonObject.addProperty("min_major", majorMin);
        jsonObject.addProperty("max_major", majorMax);
        jsonObject.addProperty("min_minor", minorMin);
        jsonObject.addProperty("max_minor", minorMax);
        jsonObject.addProperty("uuid", mBind.etIbeaconUuid.getText().toString());
        String message = assembleWriteCommonData(msgId, mMokoDevice.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private boolean isValid() {
        final String uuid = mBind.etIbeaconUuid.getText().toString();
        final String majorMin = mBind.etIbeaconMajorMin.getText().toString();
        final String majorMax = mBind.etIbeaconMajorMax.getText().toString();
        final String minorMin = mBind.etIbeaconMinorMin.getText().toString();
        final String minorMax = mBind.etIbeaconMinorMax.getText().toString();
        if (!TextUtils.isEmpty(uuid)) {
            int length = uuid.length();
            if (length % 2 != 0) {
                ToastUtils.showToast(this, "UUID Error");
                return false;
            }
        }
        if (!TextUtils.isEmpty(majorMin) && !TextUtils.isEmpty(majorMax)) {
            if (Integer.parseInt(majorMin) > 65535) {
                ToastUtils.showToast(this, "Major Error");
                return false;
            }
            if (Integer.parseInt(majorMax) > 65535) {
                ToastUtils.showToast(this, "Major Error");
                return false;
            }
            if (Integer.parseInt(majorMax) < Integer.parseInt(majorMin)) {
                ToastUtils.showToast(this, "Major Error");
                return false;
            }
        } else if (!TextUtils.isEmpty(majorMin) && TextUtils.isEmpty(majorMax)) {
            ToastUtils.showToast(this, "Major Error");
            return false;
        } else if (TextUtils.isEmpty(majorMin) && !TextUtils.isEmpty(majorMax)) {
            ToastUtils.showToast(this, "Major Error");
            return false;
        }
        if (!TextUtils.isEmpty(minorMin) && !TextUtils.isEmpty(minorMax)) {
            if (Integer.parseInt(minorMin) > 65535) {
                ToastUtils.showToast(this, "Minor Error");
                return false;
            }
            if (Integer.parseInt(minorMax) > 65535) {
                ToastUtils.showToast(this, "Minor Error");
                return false;
            }
            if (Integer.parseInt(minorMax) < Integer.parseInt(minorMin)) {
                ToastUtils.showToast(this, "Minor Error");
                return false;
            }
        } else if (!TextUtils.isEmpty(minorMin) && TextUtils.isEmpty(minorMax)) {
            ToastUtils.showToast(this, "Minor Error");
            return false;
        } else if (TextUtils.isEmpty(minorMin) && !TextUtils.isEmpty(minorMax)) {
            ToastUtils.showToast(this, "Minor Error");
            return false;
        }
        return true;
    }
}
