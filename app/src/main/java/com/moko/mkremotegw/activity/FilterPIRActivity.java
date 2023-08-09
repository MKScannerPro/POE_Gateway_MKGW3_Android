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
import com.moko.mkremotegw.databinding.ActivityFilterPirBinding;
import com.moko.mkremotegw.dialog.BottomDialog;
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
import java.util.ArrayList;

public class FilterPIRActivity extends BaseActivity<ActivityFilterPirBinding> {

    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private String mAppTopic;

    public Handler mHandler;

    private ArrayList<String> mDelayRespStatusValues;
    private int mDelayRespStatusSelected;
    private ArrayList<String> mDoorStatusValues;
    private int mDoorStatusSelected;
    private ArrayList<String> mSensorSensitivityValues;
    private int mSensorSensitivitySelected;
    private ArrayList<String> mDetectionStatusValues;
    private int mDetectionStatusSelected;

    @Override
    protected void onCreate() {
        mDelayRespStatusValues = new ArrayList<>();
        mDelayRespStatusValues.add("low delay");
        mDelayRespStatusValues.add("medium delay");
        mDelayRespStatusValues.add("high delay");
        mDelayRespStatusValues.add("all");
        mDoorStatusValues = new ArrayList<>();
        mDoorStatusValues.add("close");
        mDoorStatusValues.add("open");
        mDoorStatusValues.add("all");
        mSensorSensitivityValues = new ArrayList<>();
        mSensorSensitivityValues.add("low");
        mSensorSensitivityValues.add("medium");
        mSensorSensitivityValues.add("high");
        mSensorSensitivityValues.add("all");
        mDetectionStatusValues = new ArrayList<>();
        mDetectionStatusValues.add("no motion detected");
        mDetectionStatusValues.add("motion detected");
        mDetectionStatusValues.add("all");
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
        getFilterPIR();
    }

    @Override
    protected ActivityFilterPirBinding getViewBinding() {
        return ActivityFilterPirBinding.inflate(getLayoutInflater());
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
        if (msg_id == MQTTConstants.READ_MSG_ID_FILTER_PIR) {
            Type type = new TypeToken<MsgReadResult<JsonObject>>() {
            }.getType();
            MsgReadResult<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            mBind.cbPir.setChecked(result.data.get("switch_value").getAsInt() == 1);
            mDelayRespStatusSelected = result.data.get("delay_response_status").getAsInt();
            mBind.tvDelayRespStatus.setText(mDelayRespStatusValues.get(mDelayRespStatusSelected));
            mDoorStatusSelected = result.data.get("door_status").getAsInt();
            mBind.tvDoorStatus.setText(mDoorStatusValues.get(mDoorStatusSelected));
            mSensorSensitivitySelected = result.data.get("sensor_sensitivity").getAsInt();
            mBind.tvSensorSensitivity.setText(mSensorSensitivityValues.get(mSensorSensitivitySelected));
            mDetectionStatusSelected = result.data.get("sensor_detection_status").getAsInt();
            mBind.tvDetectionStatus.setText(mDetectionStatusValues.get(mDetectionStatusSelected));
            mBind.etPirMajorMin.setText(String.valueOf(result.data.get("min_major").getAsInt()));
            mBind.etPirMajorMax.setText(String.valueOf(result.data.get("max_major").getAsInt()));
            mBind.etPirMinorMin.setText(String.valueOf(result.data.get("min_minor").getAsInt()));
            mBind.etPirMinorMax.setText(String.valueOf(result.data.get("max_minor").getAsInt()));
        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_FILTER_PIR) {
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

    private void getFilterPIR() {
        int msgId = MQTTConstants.READ_MSG_ID_FILTER_PIR;
        String message = assembleReadCommon(msgId, mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onBack(View view) {
        finish();
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
        String majorMinStr = mBind.etPirMajorMin.getText().toString();
        String majorMaxStr = mBind.etPirMajorMax.getText().toString();
        int majorMin = 0;
        int majorMax = 65535;
        if (!TextUtils.isEmpty(majorMinStr))
            majorMin = Integer.parseInt(majorMinStr);
        if (!TextUtils.isEmpty(majorMaxStr))
            majorMax = Integer.parseInt(majorMaxStr);
        String minorMinStr = mBind.etPirMinorMin.getText().toString();
        String minorMaxStr = mBind.etPirMinorMax.getText().toString();
        int minorMin = 0;
        int minorMax = 65535;
        if (!TextUtils.isEmpty(minorMinStr))
            minorMin = Integer.parseInt(minorMinStr);
        if (!TextUtils.isEmpty(minorMaxStr))
            minorMax = Integer.parseInt(minorMaxStr);
        int msgId = MQTTConstants.CONFIG_MSG_ID_FILTER_PIR;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("switch_value", mBind.cbPir.isChecked() ? 1 : 0);
        jsonObject.addProperty("delay_response_status", mDelayRespStatusSelected);
        jsonObject.addProperty("door_status", mDoorStatusSelected);
        jsonObject.addProperty("sensor_sensitivity", mSensorSensitivitySelected);
        jsonObject.addProperty("sensor_detection_status", mDetectionStatusSelected);
        jsonObject.addProperty("min_major", majorMin);
        jsonObject.addProperty("max_major", majorMax);
        jsonObject.addProperty("min_minor", minorMin);
        jsonObject.addProperty("max_minor", minorMax);
        String message = assembleWriteCommonData(msgId, mMokoDevice.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private boolean isValid() {
        final String majorMin = mBind.etPirMajorMin.getText().toString();
        final String majorMax = mBind.etPirMajorMax.getText().toString();
        final String minorMin = mBind.etPirMinorMin.getText().toString();
        final String minorMax = mBind.etPirMinorMax.getText().toString();
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

    public void onDelayRespStatus(View view) {
        if (isWindowLocked()) return;
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(mDelayRespStatusValues, mDelayRespStatusSelected);
        dialog.setListener(value -> {
            mDelayRespStatusSelected = value;
            mBind.tvDelayRespStatus.setText(mDelayRespStatusValues.get(value));
        });
        dialog.show(getSupportFragmentManager());
    }

    public void onDoorStatus(View view) {
        if (isWindowLocked()) return;
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(mDoorStatusValues, mDoorStatusSelected);
        dialog.setListener(value -> {
            mDoorStatusSelected = value;
            mBind.tvDoorStatus.setText(mDoorStatusValues.get(value));
        });
        dialog.show(getSupportFragmentManager());
    }

    public void onSensorSensitivity(View view) {
        if (isWindowLocked()) return;
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(mSensorSensitivityValues, mSensorSensitivitySelected);
        dialog.setListener(value -> {
            mSensorSensitivitySelected = value;
            mBind.tvSensorSensitivity.setText(mSensorSensitivityValues.get(value));
        });
        dialog.show(getSupportFragmentManager());
    }

    public void onDetectionStatus(View view) {
        if (isWindowLocked()) return;
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(mDetectionStatusValues, mDetectionStatusSelected);
        dialog.setListener(value -> {
            mDetectionStatusSelected = value;
            mBind.tvDetectionStatus.setText(mDetectionStatusValues.get(value));
        });
        dialog.show(getSupportFragmentManager());
    }
}
