package com.moko.mkgw3.activity.beacon;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkgw3.AppConstants;
import com.moko.mkgw3.R;
import com.moko.mkgw3.base.BaseActivity;
import com.moko.mkgw3.databinding.ActivityBeaconDfuKgw3Binding;
import com.moko.mkgw3.entity.BleInfo;
import com.moko.mkgw3.entity.MQTTConfigKgw3;
import com.moko.mkgw3.entity.MokoDeviceKgw3;
import com.moko.mkgw3.utils.SPUtiles;
import com.moko.lib.scannerui.utils.ToastUtils;
import com.moko.support.mkgw3.MQTTConstants;
import com.moko.lib.mqtt.MQTTSupport;
import com.moko.lib.mqtt.entity.MsgConfigResult;
import com.moko.lib.mqtt.entity.MsgNotify;
import com.moko.lib.mqtt.event.DeviceOnlineEvent;
import com.moko.lib.mqtt.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class BeaconDFUKgw3v2Activity extends BaseActivity<ActivityBeaconDfuKgw3Binding> {
    private final String FILTER_ASCII = "[ -~]*";

    private MokoDeviceKgw3 mMokoDeviceKgw3;
    private MQTTConfigKgw3 appMqttConfig;
    private String mAppTopic;

    public Handler mHandler;
    private String mBeaconMac;

    private int mBeaconType;

    @Override
    protected void onCreate() {
        InputFilter inputFilter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }
            return null;
        };
        mBind.etFirmwareFileUrl.setFilters(new InputFilter[]{new InputFilter.LengthFilter(256), inputFilter});
        mBind.etInitDataFileUrl.setFilters(new InputFilter[]{new InputFilter.LengthFilter(256), inputFilter});
        mMokoDeviceKgw3 = (MokoDeviceKgw3) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        mBeaconType = getIntent().getIntExtra(AppConstants.EXTRA_KEY_BEACON_TYPE, 1);
        if (mBeaconType == 5 || mBeaconType == 6)
            // BXP-T、BXP-S
            mBind.rlInitDataFileUrl.setVisibility(View.GONE);
        mBeaconMac = getIntent().getStringExtra(AppConstants.EXTRA_KEY_MAC);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfigKgw3.class);
        mAppTopic = TextUtils.isEmpty(appMqttConfig.topicPublish) ? mMokoDeviceKgw3.topicSubscribe : appMqttConfig.topicPublish;
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected ActivityBeaconDfuKgw3Binding getViewBinding() {
        return ActivityBeaconDfuKgw3Binding.inflate(getLayoutInflater());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTMessageArrivedEvent(MQTTMessageArrivedEvent event) {
        // 更新所有设备的网络状态
        final String topic = event.getTopic();
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
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_DFU_PERCENT_BATCH) {
            Type type = new TypeToken<MsgNotify<JsonObject>>() {
            }.getType();
            MsgNotify<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            int percent = result.data.get("percent").getAsInt();
            if (isFinishing()) return;
            if (mLoadingMessageDialog != null && mLoadingMessageDialog.isResumed())
                mLoadingMessageDialog.setMessage(String.format("Beacon DFU process: %d%%", percent));
            else
                showLoadingMessageDialog(String.format("Beacon DFU process: %d%%", percent), false);

        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_DFU_RESULT_BATCH) {
            Type type = new TypeToken<MsgNotify<JsonObject>>() {
            }.getType();
            MsgNotify<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            int status = result.data.get("status").getAsInt();
            if (status > 0) {
                dismissLoadingMessageDialog();
                ToastUtils.showToast(this,
                        String.format("Beacon DFU %s!", status == 1 ? "successfully" : "failed"));
                Intent intent = new Intent();
                intent.putExtra("code", status);
                setResult(RESULT_OK, intent);
                finish();
            }
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_DFU_FAILED_BATCH) {
            Type type = new TypeToken<MsgNotify<JsonObject>>() {
            }.getType();
            MsgNotify<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            dismissLoadingMessageDialog();
            int resultCode = result.data.get("multi_dfu_result_code").getAsInt();
            ToastUtils.showToast(this, "Beacon DFU failed!");
            Intent intent = new Intent();
            intent.putExtra("code", resultCode);
            setResult(RESULT_OK, intent);
            finish();
        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_BLE_DFU_BATCH) {
            Type type = new TypeToken<MsgConfigResult>() {
            }.getType();
            MsgConfigResult result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            showLoadingMessageDialog("Beacon DFU process: 0%", false);
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_DISCONNECT) {
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            Type type = new TypeToken<MsgNotify<JsonObject>>() {
            }.getType();
            MsgNotify<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            finish();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceOnlineEvent(DeviceOnlineEvent event) {
        super.offline(event, mMokoDeviceKgw3.mac);
    }

    public void onBack(View view) {
        finish();
    }

    public void onStartUpdate(View view) {
        if (isWindowLocked()) return;
        String firmwareFileUrlStr = mBind.etFirmwareFileUrl.getText().toString();
        String initDataFileUrlStr = mBind.etInitDataFileUrl.getText().toString();
        if (TextUtils.isEmpty(firmwareFileUrlStr)
                || (mBeaconType != 5 && mBeaconType != 6 && TextUtils.isEmpty(initDataFileUrlStr))) {
            ToastUtils.showToast(this, "File URL error");
            return;
        }
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        XLog.i("升级固件");
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 50 * 1000);
        showLoadingProgressDialog();
        setDFU(firmwareFileUrlStr, initDataFileUrlStr);
    }


    private void setDFU(String firmwareFileUrlStr, String initDataFileUrlStr) {
        List<BleInfo> bleList = new ArrayList<>();
        BleInfo bleInfo = new BleInfo();
        bleInfo.mac = mBeaconMac;
        bleInfo.passwd = "";
        if (mBeaconType == 7 || mBeaconType == 8)
            bleInfo.passwd = "MOKOMOKO";
        bleList.add(bleInfo);
        JsonElement element = new Gson().toJsonTree(bleList);
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_DFU_BATCH;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("beacon_type", mBeaconType);
        jsonObject.addProperty("firmware_url", firmwareFileUrlStr);
        jsonObject.addProperty("init_data_url", initDataFileUrlStr);
        jsonObject.add("ble_dev", element);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
