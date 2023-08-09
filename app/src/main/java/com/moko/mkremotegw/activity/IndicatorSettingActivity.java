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
import com.moko.mkremotegw.R;
import com.moko.mkremotegw.base.BaseActivity;
import com.moko.mkremotegw.databinding.ActivityIndicatorSettingBinding;
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

public class IndicatorSettingActivity extends BaseActivity<ActivityIndicatorSettingBinding> {

    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private String mAppTopic;

    private int bleBroadcastEnable;
    private int bleConnectedEnable;
    private int serverConnectingEnable;
    private int serverConnectedEnable;

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
            IndicatorSettingActivity.this.finish();
        }, 30 * 1000);
        showLoadingProgressDialog();
        getIndicatorStatus();
    }

    @Override
    protected ActivityIndicatorSettingBinding getViewBinding() {
        return ActivityIndicatorSettingBinding.inflate(getLayoutInflater());
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
        if (msg_id == MQTTConstants.READ_MSG_ID_INDICATOR_STATUS) {
            Type type = new TypeToken<MsgReadResult<JsonObject>>() {
            }.getType();
            MsgReadResult<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            bleBroadcastEnable = result.data.get("ble_adv_led").getAsInt();
            bleConnectedEnable = result.data.get("ble_connected_led").getAsInt();
            serverConnectingEnable = result.data.get("server_connecting_led").getAsInt();
            serverConnectedEnable = result.data.get("server_connected_led").getAsInt();
            mBind.cbBleConnected.setChecked(bleConnectedEnable == 1);
            mBind.cbBleBroadcast.setChecked(bleBroadcastEnable == 1);
            mBind.cbServerConnecting.setChecked(serverConnectingEnable == 1);
            mBind.cbServerConnected.setChecked(serverConnectedEnable == 1);
        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_INDICATOR_STATUS) {
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


    private void setIndicatorStatus() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_INDICATOR_STATUS;
        bleBroadcastEnable = mBind.cbBleBroadcast.isChecked() ? 1 : 0;
        bleConnectedEnable = mBind.cbBleConnected.isChecked() ? 1 : 0;
        serverConnectingEnable = mBind.cbServerConnecting.isChecked() ? 1 : 0;
        serverConnectedEnable = mBind.cbServerConnected.isChecked() ? 1 : 0;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("ble_adv_led", bleBroadcastEnable);
        jsonObject.addProperty("ble_connected_led", bleConnectedEnable);
        jsonObject.addProperty("server_connecting_led", serverConnectingEnable);
        jsonObject.addProperty("server_connected_led", serverConnectedEnable);
        String message = assembleWriteCommonData(msgId, mMokoDevice.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getIndicatorStatus() {
        int msgId = MQTTConstants.READ_MSG_ID_INDICATOR_STATUS;
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
        setIndicatorStatus();
    }
}
