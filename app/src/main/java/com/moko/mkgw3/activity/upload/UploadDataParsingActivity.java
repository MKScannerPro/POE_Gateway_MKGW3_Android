package com.moko.mkgw3.activity.upload;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.lib.mqtt.MQTTSupport;
import com.moko.lib.mqtt.entity.MsgConfigResult;
import com.moko.lib.mqtt.entity.MsgReadResult;
import com.moko.lib.mqtt.event.DeviceOnlineEvent;
import com.moko.lib.mqtt.event.MQTTMessageArrivedEvent;
import com.moko.lib.scannerui.utils.ToastUtils;
import com.moko.mkgw3.AppConstants;
import com.moko.mkgw3.R;
import com.moko.mkgw3.base.BaseActivity;
import com.moko.mkgw3.databinding.ActivityUploadDataParsingKgw3Binding;
import com.moko.mkgw3.entity.MQTTConfigKgw3;
import com.moko.mkgw3.entity.MokoDeviceKgw3;
import com.moko.mkgw3.entity.MokoDeviceKgw3;
import com.moko.mkgw3.utils.SPUtiles;
import com.moko.support.mkgw3.MQTTConstants;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;

public class UploadDataParsingActivity extends BaseActivity<ActivityUploadDataParsingKgw3Binding> {

    private MokoDeviceKgw3 mMokoDevice;
    private MQTTConfigKgw3 appMqttConfig;
    private String mAppTopic;

    public Handler mHandler;

    @Override
    protected void onCreate() {
        mMokoDevice = (MokoDeviceKgw3) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfigKgw3.class);
        mAppTopic = TextUtils.isEmpty(appMqttConfig.topicPublish) ? mMokoDevice.topicSubscribe : appMqttConfig.topicPublish;
        mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            UploadDataParsingActivity.this.finish();
        }, 30 * 1000);
        showLoadingProgressDialog();
        getDataParsing();
    }

    @Override
    protected ActivityUploadDataParsingKgw3Binding getViewBinding() {
        return ActivityUploadDataParsingKgw3Binding.inflate(getLayoutInflater());
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
        if (msg_id == MQTTConstants.READ_MSG_ID_DATA_PARSING) {
            Type type = new TypeToken<MsgReadResult<JsonObject>>() {
            }.getType();
            MsgReadResult<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);

            mBind.cbIBeacon.setChecked(result.data.get("iBeacon_switch").getAsInt() == 1);
            mBind.cbEddystoneUID.setChecked(result.data.get("eddystone_uid_switch").getAsInt() == 1);
            mBind.cbEddystoneTLM.setChecked(result.data.get("eddystone_tlm_switch").getAsInt() == 1);
            mBind.cbEddystoneURL.setChecked(result.data.get("eddystone_url_switch").getAsInt() == 1);
            mBind.cbBXPACC.setChecked(result.data.get("bxp_acc_switch").getAsInt() == 1);
            mBind.cbBXPTH.setChecked(result.data.get("bxp_th_switch").getAsInt() == 1);
            mBind.cbBXPDeviceInfo.setChecked(result.data.get("bxp_devinfo_switch").getAsInt() == 1);
            mBind.cbBXPButton.setChecked(result.data.get("bxp_button_switch").getAsInt() == 1);
            mBind.cbBXPTag.setChecked(result.data.get("bxp_tag&sensor_switch").getAsInt() == 1);
            mBind.cbPIR.setChecked(result.data.get("mk_pir_switch").getAsInt() == 1);
            mBind.cbMKTof.setChecked(result.data.get("mk_tof_switch").getAsInt() == 1);
            mBind.cbNano.setChecked(result.data.get("nanobeacon_info_switch").getAsInt() == 1);
        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_DATA_PARSING) {
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


    private void setFilterBXPButton() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_DATA_PARSING;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("iBeacon_switch", mBind.cbIBeacon.isChecked() ? 1 : 0);
        jsonObject.addProperty("eddystone_uid_switch", mBind.cbEddystoneUID.isChecked() ? 1 : 0);
        jsonObject.addProperty("eddystone_tlm_switch", mBind.cbEddystoneTLM.isChecked() ? 1 : 0);
        jsonObject.addProperty("eddystone_url_switch",  mBind.cbEddystoneURL.isChecked() ? 1 : 0);
        jsonObject.addProperty("bxp_acc_switch", mBind.cbBXPACC.isChecked() ? 1 : 0);
        jsonObject.addProperty("bxp_th_switch", mBind.cbBXPTH.isChecked() ? 1 : 0);
        jsonObject.addProperty("bxp_devinfo_switch", mBind.cbBXPDeviceInfo.isChecked() ? 1 : 0);
        jsonObject.addProperty("bxp_button_switch", mBind.cbBXPButton.isChecked() ? 1 : 0);
        jsonObject.addProperty("bxp_tag&sensor_switch", mBind.cbBXPTag.isChecked() ? 1 : 0);
        jsonObject.addProperty("mk_pir_switch", mBind.cbPIR.isChecked() ? 1 : 0);
        jsonObject.addProperty("mk_tof_switch", mBind.cbMKTof.isChecked() ? 1 : 0);
        jsonObject.addProperty("nanobeacon_info_switch", mBind.cbNano.isChecked() ? 1 : 0);
        String message = assembleWriteCommonData(msgId, mMokoDevice.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getDataParsing() {
        int msgId = MQTTConstants.READ_MSG_ID_DATA_PARSING;
        String message = assembleReadCommon(msgId, mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onSave(View view) {
        if (isWindowLocked()) return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        setFilterBXPButton();
    }
}
