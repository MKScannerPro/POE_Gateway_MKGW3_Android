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
import com.moko.mkgw3.databinding.ActivityBxpSAdvParamsKgw3Binding;
import com.moko.mkgw3.databinding.LayoutAdvParamsBinding;
import com.moko.lib.scannerui.dialog.BottomDialog;
import com.moko.mkgw3.entity.AdvChannelS;
import com.moko.mkgw3.entity.AdvChannelSInfo;
import com.moko.mkgw3.entity.MQTTConfigKgw3;
import com.moko.mkgw3.entity.MokoDeviceKgw3;
import com.moko.mkgw3.entity.TxPowerEnum;
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
import java.util.Locale;

public class BXPSAdvParamsGW3Activity extends BaseActivity<ActivityBxpSAdvParamsKgw3Binding> {
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
        mTxPowerArray.add("3 dBm");
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
    protected ActivityBxpSAdvParamsKgw3Binding getViewBinding() {
        return ActivityBxpSAdvParamsKgw3Binding.inflate(getLayoutInflater());
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
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_S_ADV_PARAMS_READ) {
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            Type type = new TypeToken<MsgNotify<AdvChannelSInfo>>() {
            }.getType();
            MsgNotify<AdvChannelSInfo> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            if (result.data.result_code != 0) {
                ToastUtils.showToast(this, "Setup failed");
                return;
            }
            if (result.data.adv_param.isEmpty()) return;
            for (AdvChannelS advChannel : result.data.adv_param) {
                if (advChannel.channel == 0 && advChannel.enable == 1) {
                    mBind.tvSlot1Title.setText("Slot 1");
                    mBind.tvSlot1Config.setVisibility(View.VISIBLE);
                    mBind.tvSlot1Config.setTag(advChannel.channel_type);
                    if (advChannel.channel_type == 0) {
                        mBind.llSlot1NormalAdv.setVisibility(View.VISIBLE);
                        if (advChannel.normal_adv.adv_type == 0x00)
                            mBind.tvSlot1NormalAdvType.append("UID");
                        else if (advChannel.normal_adv.adv_type == 0x10)
                            mBind.tvSlot1NormalAdvType.setText("URL");
                        else if (advChannel.normal_adv.adv_type == 0x20)
                            mBind.tvSlot1NormalAdvType.setText("TLM");
                        else if (advChannel.normal_adv.adv_type == 0x50)
                            mBind.tvSlot1NormalAdvType.setText("iBeacon");
                        else if (advChannel.normal_adv.adv_type == 0x70)
                            mBind.tvSlot1NormalAdvType.setText("TH Info");
                        else if (advChannel.normal_adv.adv_type == 0x80)
                            mBind.tvSlot1NormalAdvType.setText("Sensor info");
                        else if (advChannel.normal_adv.adv_type == 0x90)
                            mBind.tvSlot1NormalAdvType.setText("No Data");
                        mBind.layoutSlot1Normal.etAdvInterval.setText(String.valueOf(advChannel.normal_adv.adv_interval / 20));
                        mBind.layoutSlot1Normal.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", advChannel.normal_adv.tx_power));
                        mBind.layoutSlot1Normal.tvTxPower.setTag(advChannel.normal_adv.tx_power);
                    } else if (advChannel.channel_type == 1) {
                        mBind.llSlot1AfterAdv.setVisibility(View.VISIBLE);
                        if (advChannel.trigger_after_adv.adv_type == 0x00)
                            mBind.tvSlot1AfterAdvType.append("UID");
                        else if (advChannel.trigger_after_adv.adv_type == 0x10)
                            mBind.tvSlot1AfterAdvType.setText("URL");
                        else if (advChannel.trigger_after_adv.adv_type == 0x20)
                            mBind.tvSlot1AfterAdvType.setText("TLM");
                        else if (advChannel.trigger_after_adv.adv_type == 0x50)
                            mBind.tvSlot1AfterAdvType.setText("iBeacon");
                        else if (advChannel.trigger_after_adv.adv_type == 0x70)
                            mBind.tvSlot1AfterAdvType.setText("TH Info");
                        else if (advChannel.trigger_after_adv.adv_type == 0x80)
                            mBind.tvSlot1AfterAdvType.setText("Sensor info");
                        else if (advChannel.trigger_after_adv.adv_type == 0x90)
                            mBind.tvSlot1AfterAdvType.setText("No Data");
                        mBind.layoutSlot1AfterAdv.etAdvInterval.setText(String.valueOf(advChannel.trigger_after_adv.adv_interval / 20));
                        mBind.layoutSlot1AfterAdv.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", advChannel.trigger_after_adv.tx_power));
                        mBind.layoutSlot1AfterAdv.tvTxPower.setTag(advChannel.trigger_after_adv.tx_power);
                    } else if (advChannel.channel_type == 2) {
                        mBind.llSlot1BeforeAndAfterAdv.setVisibility(View.VISIBLE);
                        if (advChannel.trigger_before_adv.adv_type == 0x00)
                            mBind.tvSlot1BeforeAdvType.append("UID");
                        else if (advChannel.trigger_before_adv.adv_type == 0x10)
                            mBind.tvSlot1BeforeAdvType.setText("URL");
                        else if (advChannel.trigger_before_adv.adv_type == 0x20)
                            mBind.tvSlot1BeforeAdvType.setText("TLM");
                        else if (advChannel.trigger_before_adv.adv_type == 0x50)
                            mBind.tvSlot1BeforeAdvType.setText("iBeacon");
                        else if (advChannel.trigger_before_adv.adv_type == 0x70)
                            mBind.tvSlot1BeforeAdvType.setText("TH Info");
                        else if (advChannel.trigger_before_adv.adv_type == 0x80)
                            mBind.tvSlot1BeforeAdvType.setText("Sensor info");
                        else if (advChannel.trigger_before_adv.adv_type == 0x90)
                            mBind.tvSlot1BeforeAdvType.setText("No Data");
                        if (advChannel.trigger_after_adv.adv_type == 0x00)
                            mBind.tvSlot1TriggerAdvType.append("UID");
                        else if (advChannel.trigger_after_adv.adv_type == 0x10)
                            mBind.tvSlot1TriggerAdvType.setText("URL");
                        else if (advChannel.trigger_after_adv.adv_type == 0x20)
                            mBind.tvSlot1TriggerAdvType.setText("TLM");
                        else if (advChannel.trigger_after_adv.adv_type == 0x50)
                            mBind.tvSlot1TriggerAdvType.setText("iBeacon");
                        else if (advChannel.trigger_after_adv.adv_type == 0x70)
                            mBind.tvSlot1TriggerAdvType.setText("TH Info");
                        else if (advChannel.trigger_after_adv.adv_type == 0x80)
                            mBind.tvSlot1TriggerAdvType.setText("Sensor info");
                        else if (advChannel.trigger_after_adv.adv_type == 0x90)
                            mBind.tvSlot1TriggerAdvType.setText("No Data");
                        mBind.layoutSlot1BeforeAdv.etAdvInterval.setText(String.valueOf(advChannel.trigger_before_adv.adv_interval / 20));
                        mBind.layoutSlot1BeforeAdv.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", advChannel.trigger_before_adv.tx_power));
                        mBind.layoutSlot1BeforeAdv.tvTxPower.setTag(advChannel.trigger_before_adv.tx_power);
                        mBind.layoutSlot1TriggerAdv.etAdvInterval.setText(String.valueOf(advChannel.trigger_after_adv.adv_interval / 20));
                        mBind.layoutSlot1TriggerAdv.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", advChannel.trigger_after_adv.tx_power));
                        mBind.layoutSlot1TriggerAdv.tvTxPower.setTag(advChannel.trigger_after_adv.tx_power);
                    }
                }
                if (advChannel.channel == 1 && advChannel.enable == 1) {
                    mBind.tvSlot2Title.setText("Slot 2");
                    mBind.tvSlot2Config.setVisibility(View.VISIBLE);
                    mBind.tvSlot2Config.setTag(advChannel.channel_type);
                    if (advChannel.channel_type == 0) {
                        mBind.llSlot2NormalAdv.setVisibility(View.VISIBLE);
                        if (advChannel.normal_adv.adv_type == 0x00)
                            mBind.tvSlot2NormalAdvType.append("UID");
                        else if (advChannel.normal_adv.adv_type == 0x10)
                            mBind.tvSlot2NormalAdvType.setText("URL");
                        else if (advChannel.normal_adv.adv_type == 0x20)
                            mBind.tvSlot2NormalAdvType.setText("TLM");
                        else if (advChannel.normal_adv.adv_type == 0x50)
                            mBind.tvSlot2NormalAdvType.setText("iBeacon");
                        else if (advChannel.normal_adv.adv_type == 0x70)
                            mBind.tvSlot2NormalAdvType.setText("TH Info");
                        else if (advChannel.normal_adv.adv_type == 0x80)
                            mBind.tvSlot2NormalAdvType.setText("Sensor info");
                        else if (advChannel.normal_adv.adv_type == 0x90)
                            mBind.tvSlot2NormalAdvType.setText("No Data");
                        mBind.layoutSlot2Normal.etAdvInterval.setText(String.valueOf(advChannel.normal_adv.adv_interval / 20));
                        mBind.layoutSlot2Normal.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", advChannel.normal_adv.tx_power));
                        mBind.layoutSlot2Normal.tvTxPower.setTag(advChannel.normal_adv.tx_power);
                    } else if (advChannel.channel_type == 1) {
                        mBind.llSlot2AfterAdv.setVisibility(View.VISIBLE);
                        if (advChannel.trigger_after_adv.adv_type == 0x00)
                            mBind.tvSlot2AfterAdvType.append("UID");
                        else if (advChannel.trigger_after_adv.adv_type == 0x10)
                            mBind.tvSlot2AfterAdvType.setText("URL");
                        else if (advChannel.trigger_after_adv.adv_type == 0x20)
                            mBind.tvSlot2AfterAdvType.setText("TLM");
                        else if (advChannel.trigger_after_adv.adv_type == 0x50)
                            mBind.tvSlot2AfterAdvType.setText("iBeacon");
                        else if (advChannel.trigger_after_adv.adv_type == 0x70)
                            mBind.tvSlot2AfterAdvType.setText("TH Info");
                        else if (advChannel.trigger_after_adv.adv_type == 0x80)
                            mBind.tvSlot2AfterAdvType.setText("Sensor info");
                        else if (advChannel.trigger_after_adv.adv_type == 0x90)
                            mBind.tvSlot2AfterAdvType.setText("No Data");
                        mBind.layoutSlot2AfterAdv.etAdvInterval.setText(String.valueOf(advChannel.trigger_after_adv.adv_interval / 20));
                        mBind.layoutSlot2AfterAdv.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", advChannel.trigger_after_adv.tx_power));
                        mBind.layoutSlot2AfterAdv.tvTxPower.setTag(advChannel.trigger_after_adv.tx_power);
                    } else if (advChannel.channel_type == 2) {
                        mBind.llSlot2BeforeAndAfterAdv.setVisibility(View.VISIBLE);
                        if (advChannel.trigger_before_adv.adv_type == 0x00)
                            mBind.tvSlot2BeforeAdvType.append("UID");
                        else if (advChannel.trigger_before_adv.adv_type == 0x10)
                            mBind.tvSlot2BeforeAdvType.setText("URL");
                        else if (advChannel.trigger_before_adv.adv_type == 0x20)
                            mBind.tvSlot2BeforeAdvType.setText("TLM");
                        else if (advChannel.trigger_before_adv.adv_type == 0x50)
                            mBind.tvSlot2BeforeAdvType.setText("iBeacon");
                        else if (advChannel.trigger_before_adv.adv_type == 0x70)
                            mBind.tvSlot2BeforeAdvType.setText("TH Info");
                        else if (advChannel.trigger_before_adv.adv_type == 0x80)
                            mBind.tvSlot2BeforeAdvType.setText("Sensor info");
                        else if (advChannel.trigger_before_adv.adv_type == 0x90)
                            mBind.tvSlot2BeforeAdvType.setText("No Data");
                        if (advChannel.trigger_after_adv.adv_type == 0x00)
                            mBind.tvSlot2TriggerAdvType.append("UID");
                        else if (advChannel.trigger_after_adv.adv_type == 0x10)
                            mBind.tvSlot2TriggerAdvType.setText("URL");
                        else if (advChannel.trigger_after_adv.adv_type == 0x20)
                            mBind.tvSlot2TriggerAdvType.setText("TLM");
                        else if (advChannel.trigger_after_adv.adv_type == 0x50)
                            mBind.tvSlot2TriggerAdvType.setText("iBeacon");
                        else if (advChannel.trigger_after_adv.adv_type == 0x70)
                            mBind.tvSlot2TriggerAdvType.setText("TH Info");
                        else if (advChannel.trigger_after_adv.adv_type == 0x80)
                            mBind.tvSlot2TriggerAdvType.setText("Sensor info");
                        else if (advChannel.trigger_after_adv.adv_type == 0x90)
                            mBind.tvSlot2TriggerAdvType.setText("No Data");
                        mBind.layoutSlot2BeforeAdv.etAdvInterval.setText(String.valueOf(advChannel.trigger_before_adv.adv_interval / 20));
                        mBind.layoutSlot2BeforeAdv.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", advChannel.trigger_before_adv.tx_power));
                        mBind.layoutSlot2BeforeAdv.tvTxPower.setTag(advChannel.trigger_before_adv.tx_power);
                        mBind.layoutSlot2TriggerAdv.etAdvInterval.setText(String.valueOf(advChannel.trigger_after_adv.adv_interval / 20));
                        mBind.layoutSlot2TriggerAdv.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", advChannel.trigger_after_adv.tx_power));
                        mBind.layoutSlot2TriggerAdv.tvTxPower.setTag(advChannel.trigger_after_adv.tx_power);
                    }
                }
                if (advChannel.channel == 2 && advChannel.enable == 1) {
                    mBind.tvSlot3Title.setText("Slot 3");
                    mBind.tvSlot3Config.setVisibility(View.VISIBLE);
                    mBind.tvSlot3Config.setTag(advChannel.channel_type);
                    if (advChannel.channel_type == 0) {
                        mBind.llSlot3NormalAdv.setVisibility(View.VISIBLE);
                        if (advChannel.normal_adv.adv_type == 0x00)
                            mBind.tvSlot3NormalAdvType.append("UID");
                        else if (advChannel.normal_adv.adv_type == 0x10)
                            mBind.tvSlot3NormalAdvType.setText("URL");
                        else if (advChannel.normal_adv.adv_type == 0x20)
                            mBind.tvSlot3NormalAdvType.setText("TLM");
                        else if (advChannel.normal_adv.adv_type == 0x50)
                            mBind.tvSlot3NormalAdvType.setText("iBeacon");
                        else if (advChannel.normal_adv.adv_type == 0x70)
                            mBind.tvSlot3NormalAdvType.setText("TH Info");
                        else if (advChannel.normal_adv.adv_type == 0x80)
                            mBind.tvSlot3NormalAdvType.setText("Sensor info");
                        else if (advChannel.normal_adv.adv_type == 0x90)
                            mBind.tvSlot3NormalAdvType.setText("No Data");
                        mBind.layoutSlot3Normal.etAdvInterval.setText(String.valueOf(advChannel.normal_adv.adv_interval / 20));
                        mBind.layoutSlot3Normal.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", advChannel.normal_adv.tx_power));
                        mBind.layoutSlot3Normal.tvTxPower.setTag(advChannel.normal_adv.tx_power);
                    } else if (advChannel.channel_type == 1) {
                        mBind.llSlot3AfterAdv.setVisibility(View.VISIBLE);
                        if (advChannel.trigger_after_adv.adv_type == 0x00)
                            mBind.tvSlot3AfterAdvType.append("UID");
                        else if (advChannel.trigger_after_adv.adv_type == 0x10)
                            mBind.tvSlot3AfterAdvType.setText("URL");
                        else if (advChannel.trigger_after_adv.adv_type == 0x20)
                            mBind.tvSlot3AfterAdvType.setText("TLM");
                        else if (advChannel.trigger_after_adv.adv_type == 0x50)
                            mBind.tvSlot3AfterAdvType.setText("iBeacon");
                        else if (advChannel.trigger_after_adv.adv_type == 0x70)
                            mBind.tvSlot3AfterAdvType.setText("TH Info");
                        else if (advChannel.trigger_after_adv.adv_type == 0x80)
                            mBind.tvSlot3AfterAdvType.setText("Sensor info");
                        else if (advChannel.trigger_after_adv.adv_type == 0x90)
                            mBind.tvSlot3AfterAdvType.setText("No Data");
                        mBind.layoutSlot3AfterAdv.etAdvInterval.setText(String.valueOf(advChannel.trigger_after_adv.adv_interval / 20));
                        mBind.layoutSlot3AfterAdv.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", advChannel.trigger_after_adv.tx_power));
                        mBind.layoutSlot3AfterAdv.tvTxPower.setTag(advChannel.trigger_after_adv.tx_power);
                    } else if (advChannel.channel_type == 2) {
                        mBind.llSlot3BeforeAndAfterAdv.setVisibility(View.VISIBLE);
                        if (advChannel.trigger_before_adv.adv_type == 0x00)
                            mBind.tvSlot3BeforeAdvType.append("UID");
                        else if (advChannel.trigger_before_adv.adv_type == 0x10)
                            mBind.tvSlot3BeforeAdvType.setText("URL");
                        else if (advChannel.trigger_before_adv.adv_type == 0x20)
                            mBind.tvSlot3BeforeAdvType.setText("TLM");
                        else if (advChannel.trigger_before_adv.adv_type == 0x50)
                            mBind.tvSlot3BeforeAdvType.setText("iBeacon");
                        else if (advChannel.trigger_before_adv.adv_type == 0x70)
                            mBind.tvSlot3BeforeAdvType.setText("TH Info");
                        else if (advChannel.trigger_before_adv.adv_type == 0x80)
                            mBind.tvSlot3BeforeAdvType.setText("Sensor info");
                        else if (advChannel.trigger_before_adv.adv_type == 0x90)
                            mBind.tvSlot3BeforeAdvType.setText("No Data");
                        if (advChannel.trigger_after_adv.adv_type == 0x00)
                            mBind.tvSlot3TriggerAdvType.append("UID");
                        else if (advChannel.trigger_after_adv.adv_type == 0x10)
                            mBind.tvSlot3TriggerAdvType.setText("URL");
                        else if (advChannel.trigger_after_adv.adv_type == 0x20)
                            mBind.tvSlot3TriggerAdvType.setText("TLM");
                        else if (advChannel.trigger_after_adv.adv_type == 0x50)
                            mBind.tvSlot3TriggerAdvType.setText("iBeacon");
                        else if (advChannel.trigger_after_adv.adv_type == 0x70)
                            mBind.tvSlot3TriggerAdvType.setText("TH Info");
                        else if (advChannel.trigger_after_adv.adv_type == 0x80)
                            mBind.tvSlot3TriggerAdvType.setText("Sensor info");
                        else if (advChannel.trigger_after_adv.adv_type == 0x90)
                            mBind.tvSlot3TriggerAdvType.setText("No Data");
                        mBind.layoutSlot3BeforeAdv.etAdvInterval.setText(String.valueOf(advChannel.trigger_before_adv.adv_interval / 20));
                        mBind.layoutSlot3BeforeAdv.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", advChannel.trigger_before_adv.tx_power));
                        mBind.layoutSlot3BeforeAdv.tvTxPower.setTag(advChannel.trigger_before_adv.tx_power);
                        mBind.layoutSlot3TriggerAdv.etAdvInterval.setText(String.valueOf(advChannel.trigger_after_adv.adv_interval / 20));
                        mBind.layoutSlot3TriggerAdv.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", advChannel.trigger_after_adv.tx_power));
                        mBind.layoutSlot3TriggerAdv.tvTxPower.setTag(advChannel.trigger_after_adv.tx_power);
                    }
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
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_S_ADV_PARAMS_WRITE) {
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
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_S_ADV_PARAMS_READ;;
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


    public void onSlot1Config(View view) {
        if (isWindowLocked()) return;
        if (isSlot1ParamsValid(view, 0)) {
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Setup failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            setAdvParamsConfig(view, 0);
        } else {
            ToastUtils.showToast(this, "Para Error");
        }
    }

    public void onSlot2Config(View view) {
        if (isWindowLocked()) return;
        if (isSlot1ParamsValid(view, 1)) {
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Setup failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            setAdvParamsConfig(view, 1);
        } else {
            ToastUtils.showToast(this, "Para Error");
        }
    }

    public void onSlot3Config(View view) {
        if (isWindowLocked()) return;
        if (isSlot1ParamsValid(view, 2)) {
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Setup failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            setAdvParamsConfig(view, 2);
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
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(mTxPowerArray, selected);
        dialog.setListener(value -> {
            ((TextView) view).setText(mTxPowerArray.get(value));
            int txPowerValue = TxPowerEnum.fromOrdinal(value).getTxPower();
            view.setTag(txPowerValue);
        });
        dialog.show(getSupportFragmentManager());
    }

    private void setAdvParamsConfig(View view, int channel) {
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_S_ADV_PARAMS_WRITE;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", mMac);
        jsonObject.addProperty("channel", channel);
        int channelType = (int) view.getTag();
        if (channelType == 0) {
            String advIntervalStr = mBind.layoutSlot1Normal.etAdvInterval.getText().toString();
            int interval = Integer.parseInt(advIntervalStr) * 20;
            int txPower = (int) mBind.layoutSlot1Normal.tvTxPower.getTag();
            JsonObject object = new JsonObject();
            object.addProperty("adv_interval", interval);
            object.addProperty("tx_power", txPower);
            jsonObject.add("normal_adv", object);
        }
        if (channelType == 1) {
            String advIntervalStr = mBind.layoutSlot1AfterAdv.etAdvInterval.getText().toString();
            int interval = Integer.parseInt(advIntervalStr) * 20;
            int txPower = (int) mBind.layoutSlot1AfterAdv.tvTxPower.getTag();
            JsonObject object = new JsonObject();
            object.addProperty("adv_interval", interval);
            object.addProperty("tx_power", txPower);
            jsonObject.add("trigger_after_adv", object);
        }
        if (channelType == 2) {
            String advIntervalStr = mBind.layoutSlot1BeforeAdv.etAdvInterval.getText().toString();
            int interval = Integer.parseInt(advIntervalStr) * 20;
            int txPower = (int) mBind.layoutSlot1BeforeAdv.tvTxPower.getTag();
            JsonObject object = new JsonObject();
            object.addProperty("adv_interval", interval);
            object.addProperty("tx_power", txPower);
            jsonObject.add("trigger_before_adv", object);

            String advIntervalTriggerStr = mBind.layoutSlot1TriggerAdv.etAdvInterval.getText().toString();
            int intervalTrigger = Integer.parseInt(advIntervalTriggerStr) * 20;
            int txPowerTrigger = (int) mBind.layoutSlot1BeforeAdv.tvTxPower.getTag();
            JsonObject objectTrigger = new JsonObject();
            objectTrigger.addProperty("adv_interval", intervalTrigger);
            objectTrigger.addProperty("tx_power", txPowerTrigger);
            jsonObject.add("trigger_after_adv", objectTrigger);
        }
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private boolean isSlot1ParamsValid(View view, int channel) {
        int channelType = (int) view.getTag();
        ArrayList<LayoutAdvParamsBinding> layoutBindings = new ArrayList<>();
        if (channel == 0) {
            if (channelType == 0)
                layoutBindings.add(mBind.layoutSlot1Normal);
            if (channelType == 1)
                layoutBindings.add(mBind.layoutSlot1AfterAdv);
            if (channelType == 2) {
                layoutBindings.add(mBind.layoutSlot1BeforeAdv);
                layoutBindings.add(mBind.layoutSlot1TriggerAdv);
            }

        }
        if (channel == 1) {
            if (channelType == 0)
                layoutBindings.add(mBind.layoutSlot2Normal);
            if (channelType == 1)
                layoutBindings.add(mBind.layoutSlot2AfterAdv);
            if (channelType == 2) {
                layoutBindings.add(mBind.layoutSlot2BeforeAdv);
                layoutBindings.add(mBind.layoutSlot2TriggerAdv);
            }

        }
        if (channel == 2) {
            if (channelType == 0)
                layoutBindings.add(mBind.layoutSlot3Normal);
            if (channelType == 1)
                layoutBindings.add(mBind.layoutSlot3AfterAdv);
            if (channelType == 2) {
                layoutBindings.add(mBind.layoutSlot3BeforeAdv);
                layoutBindings.add(mBind.layoutSlot3TriggerAdv);
            }

        }
        for (LayoutAdvParamsBinding binding : layoutBindings) {
            String advIntervalStr = binding.etAdvInterval.getText().toString();
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
