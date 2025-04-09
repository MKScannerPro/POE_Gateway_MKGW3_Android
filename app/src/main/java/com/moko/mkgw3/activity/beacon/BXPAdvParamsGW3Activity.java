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
import com.moko.mkgw3.AppConstants;
import com.moko.mkgw3.base.BaseActivity;
import com.moko.mkgw3.databinding.ActivityBxpCAdvParamsKgw3Binding;
import com.moko.mkgw3.databinding.LayoutSlotBinding;
import com.moko.mkgw3.dialog.MKgw3BottomDialog;
import com.moko.mkgw3.entity.AdvParamInfo;
import com.moko.mkgw3.entity.MQTTConfigKgw3;
import com.moko.mkgw3.entity.MokoDeviceKgw3;
import com.moko.mkgw3.entity.SlotAdvParams;
import com.moko.mkgw3.entity.TxPowerEnum;
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
import java.util.Locale;

public class BXPAdvParamsGW3Activity extends BaseActivity<ActivityBxpCAdvParamsKgw3Binding> {
    private MokoDeviceKgw3 mMokoDeviceKgw3;
    private MQTTConfigKgw3 appMqttConfig;
    private String mAppTopic;
    private String mMac;
    private Handler mHandler;
    private ArrayList<String> mTxPowerArray;
    private int mBeaconType;

    @Override
    protected void onCreate() {
        mMokoDeviceKgw3 = (MokoDeviceKgw3) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfigKgw3.class);
        mAppTopic = TextUtils.isEmpty(appMqttConfig.topicPublish) ? mMokoDeviceKgw3.topicSubscribe : appMqttConfig.topicPublish;
        mMac = getIntent().getStringExtra(AppConstants.EXTRA_KEY_MAC);
        mBeaconType = getIntent().getIntExtra(AppConstants.EXTRA_KEY_BEACON_TYPE, 1);
        mTxPowerArray = new ArrayList<>();
        mTxPowerArray.add("-40 dBm");
        mTxPowerArray.add("-20 dBm");
        mTxPowerArray.add("-16 dBm");
        mTxPowerArray.add("-12 dBm");
        mTxPowerArray.add("-8 dBm");
        mTxPowerArray.add("-4 dBm");
        mTxPowerArray.add("0 dBm");
        mTxPowerArray.add("3 dBm");
        mTxPowerArray.add("4 dBm");
        mBind.layoutSlot1.tvSlot.setText("Slot 1:");
        mBind.layoutSlot2.tvSlot.setText("Slot 2:");
        mBind.layoutSlot3.tvSlot.setText("Slot 3:");
        mBind.layoutSlot4.tvSlot.setText("Slot 4:");
        mBind.layoutSlot5.tvSlot.setText("Slot 5:");
        mBind.layoutSlot6.tvSlot.setText("Slot 6:");
        mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Setup failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        getAdvParams();
    }

    @Override
    protected ActivityBxpCAdvParamsKgw3Binding getViewBinding() {
        return ActivityBxpCAdvParamsKgw3Binding.inflate(getLayoutInflater());
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
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_C_ADV_PARAMS_READ
                || msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_D_ADV_PARAMS_READ
                || msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_T_ADV_PARAMS_READ) {
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            Type type = new TypeToken<MsgNotify<AdvParamInfo>>() {
            }.getType();
            MsgNotify<AdvParamInfo> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            if (result.data.result_code != 0) {
                ToastUtils.showToast(this, "Setup failed");
                return;
            }
            if (result.data.adv_param.isEmpty()) return;
            for (SlotAdvParams slotAdvParams : result.data.adv_param) {
                String channelType = "No data";
                if (slotAdvParams.channel_type == 0x00)
                    channelType = "UID";
                else if (slotAdvParams.channel_type == 0x10)
                    channelType = "URL";
                else if (slotAdvParams.channel_type == 0x20)
                    channelType = "TLM";
                else if (slotAdvParams.channel_type == 0x30)
                    channelType = "EID";
                else if (slotAdvParams.channel_type == 0x40)
                    channelType = "Device info";
                else if (slotAdvParams.channel_type == 0x50)
                    channelType = "iBeacon";
                else if (slotAdvParams.channel_type == 0x60)
                    channelType = "ACC";
                else if (slotAdvParams.channel_type == 0x70)
                    channelType = "TH";
                else if (slotAdvParams.channel_type == 0x80)
                    channelType = "Tag";
                String triggerType = "OFF";
                if (slotAdvParams.trigger_type == 1)
                    triggerType = "Temperature";
                else if (slotAdvParams.trigger_type == 2)
                    triggerType = "Humidity";
                else if (slotAdvParams.trigger_type == 3)
                    triggerType = "Double press button";
                else if (slotAdvParams.trigger_type == 4)
                    triggerType = "Triple press button";
                else if (slotAdvParams.trigger_type == 5)
                    triggerType = "Device moves";
                else if (slotAdvParams.trigger_type == 6)
                    triggerType = "Light";

                if (slotAdvParams.channel == 0) {
                    mBind.layoutSlot1.tvSlot.append(channelType);
                    mBind.layoutSlot1.tvSlotConfig.setVisibility(slotAdvParams.channel_type == 0xFF ? View.GONE : View.VISIBLE);
                    mBind.layoutSlot1.llSlotAdv.setVisibility(slotAdvParams.channel_type == 0xFF ? View.GONE : View.VISIBLE);
                    mBind.layoutSlot1.tvSlotConfig.setTag(slotAdvParams.channel);
                    mBind.layoutSlot1.tvTriggerType.append(triggerType);
                    mBind.layoutSlot1.layoutAdvParams.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", slotAdvParams.tx_power));
                    mBind.layoutSlot1.layoutAdvParams.tvTxPower.setTag(slotAdvParams.tx_power);
                    mBind.layoutSlot1.layoutAdvParams.etAdvInterval.setText(String.valueOf(slotAdvParams.adv_interval / 100));
                } else if (slotAdvParams.channel == 1) {
                    mBind.layoutSlot2.tvSlot.append(channelType);
                    mBind.layoutSlot2.tvSlotConfig.setVisibility(slotAdvParams.channel_type == 0xFF ? View.GONE : View.VISIBLE);
                    mBind.layoutSlot2.llSlotAdv.setVisibility(slotAdvParams.channel_type == 0xFF ? View.GONE : View.VISIBLE);
                    mBind.layoutSlot2.tvSlotConfig.setTag(slotAdvParams.channel);
                    mBind.layoutSlot2.tvTriggerType.append(triggerType);
                    mBind.layoutSlot2.layoutAdvParams.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", slotAdvParams.tx_power));
                    mBind.layoutSlot2.layoutAdvParams.tvTxPower.setTag(slotAdvParams.tx_power);
                    mBind.layoutSlot2.layoutAdvParams.etAdvInterval.setText(String.valueOf(slotAdvParams.adv_interval / 100));
                } else if (slotAdvParams.channel == 2) {
                    mBind.layoutSlot3.tvSlot.append(channelType);
                    mBind.layoutSlot3.llSlotAdv.setVisibility(slotAdvParams.channel_type == 0xFF ? View.GONE : View.VISIBLE);
                    mBind.layoutSlot3.tvSlotConfig.setVisibility(slotAdvParams.channel_type == 0xFF ? View.GONE : View.VISIBLE);
                    mBind.layoutSlot3.tvSlotConfig.setTag(slotAdvParams.channel);
                    mBind.layoutSlot3.tvTriggerType.append(triggerType);
                    mBind.layoutSlot3.layoutAdvParams.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", slotAdvParams.tx_power));
                    mBind.layoutSlot3.layoutAdvParams.tvTxPower.setTag(slotAdvParams.tx_power);
                    mBind.layoutSlot3.layoutAdvParams.etAdvInterval.setText(String.valueOf(slotAdvParams.adv_interval / 100));
                } else if (slotAdvParams.channel == 3) {
                    mBind.layoutSlot4.tvSlot.append(channelType);
                    mBind.layoutSlot4.tvSlotConfig.setVisibility(slotAdvParams.channel_type == 0xFF ? View.GONE : View.VISIBLE);
                    mBind.layoutSlot4.llSlotAdv.setVisibility(slotAdvParams.channel_type == 0xFF ? View.GONE : View.VISIBLE);
                    mBind.layoutSlot4.tvSlotConfig.setTag(slotAdvParams.channel);
                    mBind.layoutSlot4.tvTriggerType.append(triggerType);
                    mBind.layoutSlot4.layoutAdvParams.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", slotAdvParams.tx_power));
                    mBind.layoutSlot4.layoutAdvParams.tvTxPower.setTag(slotAdvParams.tx_power);
                    mBind.layoutSlot4.layoutAdvParams.etAdvInterval.setText(String.valueOf(slotAdvParams.adv_interval / 100));
                } else if (slotAdvParams.channel == 4) {
                    mBind.layoutSlot5.tvSlot.append(channelType);
                    mBind.layoutSlot5.tvSlotConfig.setVisibility(slotAdvParams.channel_type == 0xFF ? View.GONE : View.VISIBLE);
                    mBind.layoutSlot5.llSlotAdv.setVisibility(slotAdvParams.channel_type == 0xFF ? View.GONE : View.VISIBLE);
                    mBind.layoutSlot5.tvSlotConfig.setTag(slotAdvParams.channel);
                    mBind.layoutSlot5.tvTriggerType.append(triggerType);
                    mBind.layoutSlot5.layoutAdvParams.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", slotAdvParams.tx_power));
                    mBind.layoutSlot5.layoutAdvParams.tvTxPower.setTag(slotAdvParams.tx_power);
                    mBind.layoutSlot5.layoutAdvParams.etAdvInterval.setText(String.valueOf(slotAdvParams.adv_interval / 100));
                } else if (slotAdvParams.channel == 5) {
                    mBind.layoutSlot6.tvSlot.append(channelType);
                    mBind.layoutSlot6.tvSlotConfig.setVisibility(slotAdvParams.channel_type == 0xFF ? View.GONE : View.VISIBLE);
                    mBind.layoutSlot6.llSlotAdv.setVisibility(slotAdvParams.channel_type == 0xFF ? View.GONE : View.VISIBLE);
                    mBind.layoutSlot6.tvSlotConfig.setTag(slotAdvParams.channel);
                    mBind.layoutSlot6.tvTriggerType.append(triggerType);
                    mBind.layoutSlot6.layoutAdvParams.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", slotAdvParams.tx_power));
                    mBind.layoutSlot6.layoutAdvParams.tvTxPower.setTag(slotAdvParams.tx_power);
                    mBind.layoutSlot6.layoutAdvParams.etAdvInterval.setText(String.valueOf(slotAdvParams.adv_interval / 100));
                }

            }
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
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_C_ADV_PARAMS_WRITE
                || msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_D_ADV_PARAMS_WRITE
                || msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_T_ADV_PARAMS_WRITE) {
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
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_C_ADV_PARAMS_READ;
        if (mBeaconType == 4)
            msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_D_ADV_PARAMS_READ;
        if (mBeaconType == 5)
            msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_T_ADV_PARAMS_READ;
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


    public void onSlotConfig(View view) {
        if (isWindowLocked()) return;
        int channel = (int) view.getTag();
        if (isSlotParamsValid(channel)) {
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Setup failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            setAdvParamsConfig(channel);
        } else {
            ToastUtils.showToast(this, "Para Error");
        }
    }

    public void onTxPower(View view) {
        if (isWindowLocked()) return;
        int txPower = (int) view.getTag();
        TxPowerEnum txPowerEnum = TxPowerEnum.fromTxPower(txPower);
        if (txPowerEnum == null) return;
        int selected = txPowerEnum.ordinal();
        MKgw3BottomDialog dialog = new MKgw3BottomDialog();
        dialog.setDatas(mTxPowerArray, selected);
        dialog.setListener(value -> {
            ((TextView) view).setText(mTxPowerArray.get(value));
            int txPowerValue = TxPowerEnum.fromOrdinal(value).getTxPower();
            view.setTag(txPowerValue);
        });
        dialog.show(getSupportFragmentManager());
    }

    private void setAdvParamsConfig(int channel) {
        LayoutSlotBinding layoutSlotBinding = null;
        if (channel == 0)
            layoutSlotBinding = mBind.layoutSlot1;
        if (channel == 1)
            layoutSlotBinding = mBind.layoutSlot2;
        if (channel == 2)
            layoutSlotBinding = mBind.layoutSlot3;
        if (channel == 3)
            layoutSlotBinding = mBind.layoutSlot4;
        if (channel == 4)
            layoutSlotBinding = mBind.layoutSlot5;
        if (channel == 5)
            layoutSlotBinding = mBind.layoutSlot6;
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_C_ADV_PARAMS_WRITE;
        if (mBeaconType == 4)
            msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_D_ADV_PARAMS_WRITE;
        if (mBeaconType == 5)
            msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_T_ADV_PARAMS_WRITE;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", mMac);
        jsonObject.addProperty("channel", channel);
        assert layoutSlotBinding != null;
        String advIntervalStr = layoutSlotBinding.layoutAdvParams.etAdvInterval.getText().toString();
        int interval = Integer.parseInt(advIntervalStr);
        int txPower = (int) layoutSlotBinding.layoutAdvParams.tvTxPower.getTag();
        jsonObject.addProperty("adv_interval", interval * 100);
        jsonObject.addProperty("tx_power", txPower);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private boolean isSlotParamsValid(int channel) {
        ArrayList<LayoutSlotBinding> layoutBindings = new ArrayList<>();
        if (channel == 0)
            layoutBindings.add(mBind.layoutSlot1);
        if (channel == 1)
            layoutBindings.add(mBind.layoutSlot2);
        if (channel == 2)
            layoutBindings.add(mBind.layoutSlot3);
        if (channel == 3)
            layoutBindings.add(mBind.layoutSlot4);
        if (channel == 4)
            layoutBindings.add(mBind.layoutSlot5);
        if (channel == 5)
            layoutBindings.add(mBind.layoutSlot6);

        for (LayoutSlotBinding binding : layoutBindings) {
            String advIntervalStr = binding.layoutAdvParams.etAdvInterval.getText().toString();
            if (TextUtils.isEmpty(advIntervalStr)) {
                return false;
            }
            int interval = Integer.parseInt(advIntervalStr);
            if (interval < 1 || interval > 100)
                return false;
        }
        return true;
    }

    public void onBack(View view) {
        if (isWindowLocked()) return;
        finish();
    }

}
