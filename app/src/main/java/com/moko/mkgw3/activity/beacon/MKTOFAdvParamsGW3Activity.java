package com.moko.mkgw3.activity.beacon;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.lib.mqtt.MQTTSupport;
import com.moko.lib.mqtt.entity.MsgNotify;
import com.moko.lib.mqtt.event.DeviceOnlineEvent;
import com.moko.lib.mqtt.event.MQTTMessageArrivedEvent;
import com.moko.lib.scannerui.dialog.BottomDialog;
import com.moko.lib.scannerui.utils.ToastUtils;
import com.moko.mkgw3.AppConstants;
import com.moko.mkgw3.base.BaseActivity;
import com.moko.mkgw3.databinding.ActivityMkTofAdvParamsKgw3Binding;
import com.moko.mkgw3.entity.MQTTConfigKgw3;
import com.moko.mkgw3.entity.MokoDeviceKgw3;
import com.moko.mkgw3.utils.SPUtiles;
import com.moko.support.mkgw3.MQTTConstants;
import com.moko.support.mkgw3.entity.BeaconInfo;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class MKTOFAdvParamsGW3Activity extends BaseActivity<ActivityMkTofAdvParamsKgw3Binding> {
    private MokoDeviceKgw3 mMokoDeviceKgw3;
    private MQTTConfigKgw3 appMqttConfig;
    private String mAppTopic;
    private String mMac;
    private Handler mHandler;
    private ArrayList<String> mTxPowerArray;

    @Override
    protected void onCreate() {
        mMokoDeviceKgw3 = (MokoDeviceKgw3) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfigKgw3.class);
        mAppTopic = TextUtils.isEmpty(appMqttConfig.topicPublish) ? mMokoDeviceKgw3.topicSubscribe : appMqttConfig.topicPublish;
        mMac = getIntent().getStringExtra(AppConstants.EXTRA_KEY_MAC);
        mTxPowerArray = new ArrayList<>();
        mTxPowerArray.add("-40 dBm");
        mTxPowerArray.add("-20 dBm");
        mTxPowerArray.add("-16 dBm");
        mTxPowerArray.add("-12 dBm");
        mTxPowerArray.add("-8 dBm");
        mTxPowerArray.add("-4 dBm");
        mTxPowerArray.add("0 dBm");
        mTxPowerArray.add("4 dBm");
        mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Setup failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        getAdvParams();
    }

    @Override
    protected ActivityMkTofAdvParamsKgw3Binding getViewBinding() {
        return ActivityMkTofAdvParamsKgw3Binding.inflate(getLayoutInflater());
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
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_MK_TOF_ADV_PARAMS_READ) {
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            Type type = new TypeToken<MsgNotify<JsonObject>>() {
            }.getType();
            MsgNotify<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            int result_code = result.data.get("result_code").getAsInt();
            if (result_code != 0) {
                ToastUtils.showToast(this, "Setup failed");
                return;
            }
            int txPower = result.data.get("tx_power").getAsInt();
            int advInterval = result.data.get("adv_interval").getAsInt();
            mBind.tvTxPower.setText(mTxPowerArray.get(7 - txPower));
            mBind.tvTxPower.setTag(7 - txPower);
            mBind.etAdvInterval.setText(String.valueOf(advInterval));
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
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_MK_TOF_ADV_PARAMS_WRITE) {
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            Type type = new TypeToken<MsgNotify<BeaconInfo>>() {
            }.getType();
            MsgNotify<BeaconInfo> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            ToastUtils.showToast(this, result.data.result_code == 0 ? "Setup succeed!" : "Setup failed");
        }
    }

    private void getAdvParams() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_MK_TOF_ADV_PARAMS_READ;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", mMac);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
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
        if (isSlotParamsValid()) {
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Setup failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            setAdvParamsConfig();
        } else {
            ToastUtils.showToast(this, "Para Error");
        }
    }

    public void onTxPower(View view) {
        if (isWindowLocked()) return;
        int txPower = (int) view.getTag();
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(mTxPowerArray, txPower);
        dialog.setListener(value -> {
            ((TextView) view).setText(mTxPowerArray.get(value));
            view.setTag(value);
        });
        dialog.show(getSupportFragmentManager());
    }

    private void setAdvParamsConfig() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_MK_TOF_ADV_PARAMS_WRITE;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", mMac);
        String advIntervalStr = mBind.etAdvInterval.getText().toString();
        int interval = Integer.parseInt(advIntervalStr);
        int txPower = (int) mBind.tvTxPower.getTag();
        jsonObject.addProperty("adv_interval", interval);
        jsonObject.addProperty("tx_power", txPower + 7);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private boolean isSlotParamsValid() {
        String advIntervalStr = mBind.etAdvInterval.getText().toString();
        if (TextUtils.isEmpty(advIntervalStr)) {
            return false;
        }
        int interval = Integer.parseInt(advIntervalStr);
        if (interval < 1 || interval > 86400)
            return false;
        return true;
    }

    public void onBack(View view) {
        if (isWindowLocked()) return;
        finish();
    }

}
