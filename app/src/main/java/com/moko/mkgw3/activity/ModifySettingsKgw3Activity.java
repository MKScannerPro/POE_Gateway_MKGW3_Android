package com.moko.mkgw3.activity;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkgw3.AppConstants;
import com.moko.mkgw3.R;
import com.moko.mkgw3.base.BaseActivity;
import com.moko.mkgw3.databinding.ActivityModifySettingsKgw3Binding;
import com.moko.mkgw3.db.MKgw3DBTools;
import com.moko.mkgw3.dialog.AlertMessageDialog;
import com.moko.mkgw3.entity.MQTTConfig;
import com.moko.mkgw3.entity.MokoDevice;
import com.moko.mkgw3.utils.SPUtiles;
import com.moko.mkgw3.utils.ToastUtils;
import com.moko.support.mkgw3.MQTTConstants;
import com.moko.support.mkgw3.MQTTSupport;
import com.moko.support.mkgw3.entity.MsgConfigResult;
import com.moko.support.mkgw3.entity.MsgReadResult;
import com.moko.support.mkgw3.event.DeviceOnlineEvent;
import com.moko.support.mkgw3.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;

public class ModifySettingsKgw3Activity extends BaseActivity<ActivityModifySettingsKgw3Binding> {
    public static String TAG = ModifySettingsKgw3Activity.class.getSimpleName();
    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private String mAppTopic;
    public Handler mHandler;
    private MQTTConfig mqttDeviceConfig;

    @Override
    protected void onCreate() {
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mAppTopic = TextUtils.isEmpty(appMqttConfig.topicPublish) ? mMokoDevice.topicSubscribe : appMqttConfig.topicPublish;
        mqttDeviceConfig = new MQTTConfig();
        mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        showLoadingProgressDialog();
        mBind.tvName.postDelayed(this::getMqttSettings, 1000);
    }

    @Override
    protected ActivityModifySettingsKgw3Binding getViewBinding() {
        return ActivityModifySettingsKgw3Binding.inflate(getLayoutInflater());
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
        if (msg_id == MQTTConstants.READ_MSG_ID_MQTT_SETTINGS) {
            Type type = new TypeToken<MsgReadResult<JsonObject>>() {
            }.getType();
            MsgReadResult<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            mqttDeviceConfig.host = result.data.get("host").getAsString();
            mqttDeviceConfig.port = String.valueOf(result.data.get("port").getAsInt());
            mqttDeviceConfig.clientId = result.data.get("client_id").getAsString();
            mqttDeviceConfig.username = result.data.get("username").getAsString();
            mqttDeviceConfig.password = result.data.get("passwd").getAsString();
            mqttDeviceConfig.topicSubscribe = result.data.get("sub_topic").getAsString();
            mqttDeviceConfig.topicPublish = result.data.get("pub_topic").getAsString();
            mqttDeviceConfig.qos = result.data.get("qos").getAsInt();
            mqttDeviceConfig.cleanSession = result.data.get("clean_session").getAsInt() == 1;
            mqttDeviceConfig.connectMode = result.data.get("security_type").getAsInt();
            mqttDeviceConfig.keepAlive = result.data.get("keepalive").getAsInt();
            mqttDeviceConfig.lwtEnable = result.data.get("lwt_en").getAsInt() == 1;
            mqttDeviceConfig.lwtQos = result.data.get("lwt_qos").getAsInt();
            mqttDeviceConfig.lwtRetain = result.data.get("lwt_retain").getAsInt() == 1;
            mqttDeviceConfig.lwtTopic = result.data.get("lwt_topic").getAsString();
            mqttDeviceConfig.lwtPayload = result.data.get("lwt_payload").getAsString();
        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_REBOOT) {
            Type type = new TypeToken<MsgConfigResult>() {
            }.getType();
            MsgConfigResult result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            if (result.result_code == 0) {
                mMokoDevice.lwtEnable = mqttDeviceConfig.lwtEnable ? 1 : 0;
                mMokoDevice.lwtTopic = mqttDeviceConfig.lwtTopic;
                mMokoDevice.topicPublish = mqttDeviceConfig.topicPublish;
                mMokoDevice.topicSubscribe = mqttDeviceConfig.topicSubscribe;
                MQTTConfig mqttConfig = new Gson().fromJson(mMokoDevice.mqttInfo, MQTTConfig.class);
                mqttConfig.host = mqttDeviceConfig.host;
                mqttConfig.port = mqttDeviceConfig.port;
                mqttConfig.clientId = mqttDeviceConfig.clientId;
                mqttConfig.username = mqttDeviceConfig.username;
                mqttConfig.password = mqttDeviceConfig.password;
                mqttConfig.topicSubscribe = mqttDeviceConfig.topicSubscribe;
                mqttConfig.topicPublish = mqttDeviceConfig.topicPublish;
                mqttConfig.qos = mqttDeviceConfig.qos;
                mqttConfig.cleanSession = mqttDeviceConfig.cleanSession;
                mqttConfig.connectMode = mqttDeviceConfig.connectMode;
                mqttConfig.keepAlive = mqttDeviceConfig.keepAlive;
                mqttConfig.lwtEnable = mqttDeviceConfig.lwtEnable;
                mqttConfig.lwtQos = mqttDeviceConfig.lwtQos;
                mqttConfig.lwtRetain = mqttDeviceConfig.lwtRetain;
                mqttConfig.lwtTopic = mqttDeviceConfig.lwtTopic;
                mqttConfig.lwtPayload = mqttDeviceConfig.lwtPayload;
                mMokoDevice.mqttInfo = new Gson().toJson(mqttConfig, MQTTConfig.class);
                MKgw3DBTools.getInstance(this).updateDevice(mMokoDevice);
                mBind.tvName.postDelayed(() -> {
                    dismissLoadingProgressDialog();
                    mHandler.removeMessages(0);
                    ToastUtils.showToast(this, "Set up succeed");
                    // 跳转首页，刷新数据
                    Intent intent = new Intent(this, MKGW3MainActivity.class);
                    intent.putExtra(AppConstants.EXTRA_KEY_FROM_ACTIVITY, TAG);
                    intent.putExtra(AppConstants.EXTRA_KEY_MAC, mMokoDevice.mac);
                    startActivity(intent);
                }, 1000);
            } else {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
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

    public void onWifiSettings(View view) {
        if (isWindowLocked()) return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, ModifyNetworkSettingsKgw3Activity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        startActivity(i);
    }

    public void onMqttSettings(View view) {
        if (isWindowLocked()) return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, ModifyMQTTSettingsKgw3Activity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDevice);
        launcher.launch(i);
    }

    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> onMQTTSettingsResult(result));

    private void onMQTTSettingsResult(ActivityResult result) {
        if (result.getResultCode() == RESULT_OK) {
            mqttDeviceConfig = (MQTTConfig) result.getData().getSerializableExtra(AppConstants.EXTRA_KEY_MQTT_CONFIG_DEVICE);
        }
    }

    private void getMqttSettings() {
        int msgId = MQTTConstants.READ_MSG_ID_MQTT_SETTINGS;
        String message = assembleReadCommon(msgId, mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onConnect(View view) {
        if (isWindowLocked()) return;
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setMessage("If confirm, device will reboot and use new settings to reconnect");
        dialog.setOnAlertConfirmListener(() -> {
            if (!MQTTSupport.getInstance().isConnected()) {
                ToastUtils.showToast(this, R.string.network_error);
                return;
            }
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Set up failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            rebootDevice();
        });
        dialog.show(getSupportFragmentManager());
    }

    private void rebootDevice() {
        XLog.i("重启设备");
        int msgId = MQTTConstants.CONFIG_MSG_ID_REBOOT;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("reset", 0);
        String message = assembleWriteCommonData(msgId, mMokoDevice.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
