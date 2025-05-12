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
import com.moko.mkgw3.databinding.ActivityBxpBDAdvParamsKgw3Binding;
import com.moko.mkgw3.databinding.LayoutAdvParamsBinding;
import com.moko.lib.scannerui.dialog.BottomDialog;
import com.moko.mkgw3.entity.AdvChannel;
import com.moko.mkgw3.entity.AdvChannelInfo;
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

public class BXPBAdvParamsGW3Activity extends BaseActivity<ActivityBxpBDAdvParamsKgw3Binding> {
    private MokoDeviceKgw3 mMokoDeviceKgw3;
    private MQTTConfigKgw3 appMqttConfig;
    private String mAppTopic;
    private BeaconInfo mBeaconInfo;
    private Handler mHandler;
    private ArrayList<String> mTxPowerArray;

    private boolean mIsShowAdvType;

    @Override
    protected void onCreate() {
        mMokoDeviceKgw3 = (MokoDeviceKgw3) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfigKgw3.class);
        mAppTopic = TextUtils.isEmpty(appMqttConfig.topicPublish) ? mMokoDeviceKgw3.topicSubscribe : appMqttConfig.topicPublish;
        mBeaconInfo = (BeaconInfo) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_BEACON_INFO);
        mIsShowAdvType = mBeaconInfo.type == 1 && mBeaconInfo.software_version.startsWith("V2");
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
    protected ActivityBxpBDAdvParamsKgw3Binding getViewBinding() {
        return ActivityBxpBDAdvParamsKgw3Binding.inflate(getLayoutInflater());
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
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_B_D_ADV_PARAMS_READ
                || msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_B_CR_ADV_PARAMS_READ) {
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            Type type = new TypeToken<MsgNotify<AdvChannelInfo>>() {
            }.getType();
            MsgNotify<AdvChannelInfo> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            if (result.data.result_code != 0) {
                ToastUtils.showToast(this, "Setup failed");
                return;
            }
            if (result.data.adv_param.isEmpty()) return;
            for (AdvChannel advChannel : result.data.adv_param) {
                if (advChannel.channel == 0 && advChannel.enable == 1) {
                    mBind.tvSinglePressConfig.setVisibility(View.VISIBLE);
                    mBind.tvSinglePressConfig.setTag(advChannel.channel_type);
                    mBind.tvSinglePressAdvType.setText("Alarm");
                    if (mIsShowAdvType) {
                        if (advChannel.adv_type == 1)
                            mBind.tvSinglePressAdvType.setText("UID");
                        else if (advChannel.adv_type == 2)
                            mBind.tvSinglePressAdvType.setText("iBeacon");
                    }
                    if (advChannel.channel_type == 0) {
                        mBind.llSinglePressNormalAdv.setVisibility(View.VISIBLE);
                        mBind.layoutSingleNormal.etAdvInterval.setText(String.valueOf(advChannel.normal_adv.adv_interval / 20));
                        mBind.layoutSingleNormal.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", advChannel.normal_adv.tx_power));
                        mBind.layoutSingleNormal.tvTxPower.setTag(advChannel.normal_adv.tx_power);
                    } else if (advChannel.channel_type == 1) {
                        mBind.llSinglePressAfterAdv.setVisibility(View.VISIBLE);
                        mBind.layoutSingleAfterAdv.etAdvInterval.setText(String.valueOf(advChannel.trigger_after_adv.adv_interval / 20));
                        mBind.layoutSingleAfterAdv.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", advChannel.trigger_after_adv.tx_power));
                        mBind.layoutSingleAfterAdv.tvTxPower.setTag(advChannel.trigger_after_adv.tx_power);
                    } else if (advChannel.channel_type == 2) {
                        mBind.llSinglePressBeforeAndAfterAdv.setVisibility(View.VISIBLE);
                        mBind.layoutSingleBeforeAdv.etAdvInterval.setText(String.valueOf(advChannel.trigger_before_adv.adv_interval / 20));
                        mBind.layoutSingleBeforeAdv.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", advChannel.trigger_before_adv.tx_power));
                        mBind.layoutSingleBeforeAdv.tvTxPower.setTag(advChannel.trigger_before_adv.tx_power);
                        mBind.layoutSingleTriggerAdv.etAdvInterval.setText(String.valueOf(advChannel.trigger_after_adv.adv_interval / 20));
                        mBind.layoutSingleTriggerAdv.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", advChannel.trigger_after_adv.tx_power));
                        mBind.layoutSingleTriggerAdv.tvTxPower.setTag(advChannel.trigger_after_adv.tx_power);
                    }
                }
                if (advChannel.channel == 1 && advChannel.enable == 1) {
                    mBind.tvDoublePressConfig.setVisibility(View.VISIBLE);
                    mBind.tvDoublePressConfig.setTag(advChannel.channel_type);
                    mBind.tvDoublePressAdvType.setText("Alarm");
                    if (mIsShowAdvType) {
                        if (advChannel.adv_type == 1)
                            mBind.tvDoublePressAdvType.setText("UID");
                        else if (advChannel.adv_type == 2)
                            mBind.tvDoublePressAdvType.setText("iBeacon");
                    }
                    if (advChannel.channel_type == 0) {
                        mBind.llDoublePressNormalAdv.setVisibility(View.VISIBLE);
                        mBind.layoutDoubleNormal.etAdvInterval.setText(String.valueOf(advChannel.normal_adv.adv_interval / 20));
                        mBind.layoutDoubleNormal.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", advChannel.normal_adv.tx_power));
                        mBind.layoutDoubleNormal.tvTxPower.setTag(advChannel.normal_adv.tx_power);
                    } else if (advChannel.channel_type == 1) {
                        mBind.llDoublePressAfterAdv.setVisibility(View.VISIBLE);
                        mBind.layoutDoubleAfterAdv.etAdvInterval.setText(String.valueOf(advChannel.trigger_after_adv.adv_interval / 20));
                        mBind.layoutDoubleAfterAdv.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", advChannel.trigger_after_adv.tx_power));
                        mBind.layoutDoubleAfterAdv.tvTxPower.setTag(advChannel.trigger_after_adv.tx_power);
                    } else if (advChannel.channel_type == 2) {
                        mBind.llDoublePressBeforeAndAfterAdv.setVisibility(View.VISIBLE);
                        mBind.layoutDoubleBeforeAdv.etAdvInterval.setText(String.valueOf(advChannel.trigger_before_adv.adv_interval / 20));
                        mBind.layoutDoubleBeforeAdv.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", advChannel.trigger_before_adv.tx_power));
                        mBind.layoutDoubleBeforeAdv.tvTxPower.setTag(advChannel.trigger_before_adv.tx_power);
                        mBind.layoutDoubleTriggerAdv.etAdvInterval.setText(String.valueOf(advChannel.trigger_after_adv.adv_interval / 20));
                        mBind.layoutDoubleTriggerAdv.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", advChannel.trigger_after_adv.tx_power));
                        mBind.layoutDoubleTriggerAdv.tvTxPower.setTag(advChannel.trigger_after_adv.tx_power);
                    }
                }
                if (advChannel.channel == 2 && advChannel.enable == 1) {
                    mBind.tvLongPressConfig.setVisibility(View.VISIBLE);
                    mBind.tvLongPressConfig.setTag(advChannel.channel_type);
                    mBind.tvLongPressAdvType.setText("Alarm");
                    if (mIsShowAdvType) {
                        if (advChannel.adv_type == 1)
                            mBind.tvLongPressAdvType.setText("UID");
                        else if (advChannel.adv_type == 2)
                            mBind.tvLongPressAdvType.setText("iBeacon");
                    }
                    if (advChannel.channel_type == 0) {
                        mBind.llLongPressNormalAdv.setVisibility(View.VISIBLE);
                        mBind.layoutLongNormal.etAdvInterval.setText(String.valueOf(advChannel.normal_adv.adv_interval / 20));
                        mBind.layoutLongNormal.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", advChannel.normal_adv.tx_power));
                        mBind.layoutLongNormal.tvTxPower.setTag(advChannel.normal_adv.tx_power);
                    } else if (advChannel.channel_type == 1) {
                        mBind.llLongPressAfterAdv.setVisibility(View.VISIBLE);
                        mBind.layoutLongAfterAdv.etAdvInterval.setText(String.valueOf(advChannel.trigger_after_adv.adv_interval / 20));
                        mBind.layoutLongAfterAdv.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", advChannel.trigger_after_adv.tx_power));
                        mBind.layoutLongAfterAdv.tvTxPower.setTag(advChannel.trigger_after_adv.tx_power);
                    } else if (advChannel.channel_type == 2) {
                        mBind.llLongPressBeforeAndAfterAdv.setVisibility(View.VISIBLE);
                        mBind.layoutLongBeforeAdv.etAdvInterval.setText(String.valueOf(advChannel.trigger_before_adv.adv_interval / 20));
                        mBind.layoutLongBeforeAdv.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", advChannel.trigger_before_adv.tx_power));
                        mBind.layoutLongBeforeAdv.tvTxPower.setTag(advChannel.trigger_before_adv.tx_power);
                        mBind.layoutLongTriggerAdv.etAdvInterval.setText(String.valueOf(advChannel.trigger_after_adv.adv_interval / 20));
                        mBind.layoutLongTriggerAdv.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", advChannel.trigger_after_adv.tx_power));
                        mBind.layoutLongTriggerAdv.tvTxPower.setTag(advChannel.trigger_after_adv.tx_power);
                    }
                }
                if (advChannel.channel == 3 && advChannel.enable == 1) {
                    mBind.tvAbnormalInactivityConfig.setVisibility(View.VISIBLE);
                    mBind.tvAbnormalInactivityConfig.setTag(advChannel.channel_type);
                    mBind.tvAbnormalInactivityAdvType.setText("Alarm");
                    if (mIsShowAdvType) {
                        if (advChannel.adv_type == 1)
                            mBind.tvAbnormalInactivityAdvType.setText("UID");
                        else if (advChannel.adv_type == 2)
                            mBind.tvAbnormalInactivityAdvType.setText("iBeacon");
                    }
                    if (advChannel.channel_type == 0) {
                        mBind.llAbnormalInactivityNormalAdv.setVisibility(View.VISIBLE);
                        mBind.layoutAbnormalInactivityNormal.etAdvInterval.setText(String.valueOf(advChannel.normal_adv.adv_interval / 20));
                        mBind.layoutAbnormalInactivityNormal.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", advChannel.normal_adv.tx_power));
                        mBind.layoutAbnormalInactivityNormal.tvTxPower.setTag(advChannel.normal_adv.tx_power);
                    } else if (advChannel.channel_type == 1) {
                        mBind.llAbnormalInactivityAfterAdv.setVisibility(View.VISIBLE);
                        mBind.layoutAbnormalInactivityAfterAdv.etAdvInterval.setText(String.valueOf(advChannel.trigger_after_adv.adv_interval / 20));
                        mBind.layoutAbnormalInactivityAfterAdv.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", advChannel.trigger_after_adv.tx_power));
                        mBind.layoutAbnormalInactivityAfterAdv.tvTxPower.setTag(advChannel.trigger_after_adv.tx_power);
                    } else if (advChannel.channel_type == 2) {
                        mBind.llAbnormalInactivityBeforeAndAfterAdv.setVisibility(View.VISIBLE);
                        mBind.layoutAbnormalInactivityBeforeAdv.etAdvInterval.setText(String.valueOf(advChannel.trigger_before_adv.adv_interval / 20));
                        mBind.layoutAbnormalInactivityBeforeAdv.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", advChannel.trigger_before_adv.tx_power));
                        mBind.layoutAbnormalInactivityBeforeAdv.tvTxPower.setTag(advChannel.trigger_before_adv.tx_power);
                        mBind.layoutAbnormalInactivityTriggerAdv.etAdvInterval.setText(String.valueOf(advChannel.trigger_after_adv.adv_interval / 20));
                        mBind.layoutAbnormalInactivityTriggerAdv.tvTxPower.setText(String.format(Locale.getDefault(), "%d dBm", advChannel.trigger_after_adv.tx_power));
                        mBind.layoutAbnormalInactivityTriggerAdv.tvTxPower.setTag(advChannel.trigger_after_adv.tx_power);
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
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_B_D_ADV_PARAMS_WRITE
                || msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_B_CR_ADV_PARAMS_WRITE) {
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
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_B_D_ADV_PARAMS_READ;
        if (mBeaconInfo.type == 2)
            msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_B_CR_ADV_PARAMS_READ;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", mBeaconInfo.mac);
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


    public void onSinglePressConfig(View view) {
        if (isWindowLocked()) return;
        if (isSinglePressParamsValid(view, 0)) {
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

    public void onDoublePressConfig(View view) {
        if (isWindowLocked()) return;
        if (isSinglePressParamsValid(view, 1)) {
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

    public void onLongPressConfig(View view) {
        if (isWindowLocked()) return;
        if (isSinglePressParamsValid(view, 2)) {
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

    public void onAbnormalInactivityConfig(View view) {
        if (isWindowLocked()) return;
        if (isSinglePressParamsValid(view, 3)) {
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Setup failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            setAdvParamsConfig(view, 3);
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
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_B_D_ADV_PARAMS_WRITE;
        if (mBeaconInfo.type == 2)
            msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_B_CR_ADV_PARAMS_WRITE;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", mBeaconInfo.mac);
        jsonObject.addProperty("channel", channel);
        LayoutAdvParamsBinding normalBinding = null;
        if (channel == 0)
            normalBinding = mBind.layoutSingleNormal;
        if (channel == 1)
            normalBinding = mBind.layoutDoubleNormal;
        if (channel == 2)
            normalBinding = mBind.layoutLongNormal;
        if (channel == 3)
            normalBinding = mBind.layoutAbnormalInactivityNormal;
        int channelType = (int) view.getTag();
        if (channelType == 0 && normalBinding != null) {
            String advIntervalStr = normalBinding.etAdvInterval.getText().toString();
            int interval = Integer.parseInt(advIntervalStr) * 20;
            int txPower = (int) normalBinding.tvTxPower.getTag();
            JsonObject object = new JsonObject();
            object.addProperty("adv_interval", interval);
            object.addProperty("tx_power", txPower);
            jsonObject.add("normal_adv", object);
        }
        LayoutAdvParamsBinding afterAdvBinding = null;
        if (channel == 0)
            afterAdvBinding = mBind.layoutSingleAfterAdv;
        if (channel == 1)
            afterAdvBinding = mBind.layoutDoubleAfterAdv;
        if (channel == 2)
            afterAdvBinding = mBind.layoutLongAfterAdv;
        if (channel == 3)
            afterAdvBinding = mBind.layoutAbnormalInactivityAfterAdv;
        if (channelType == 1 && afterAdvBinding != null) {
            String advIntervalStr = afterAdvBinding.etAdvInterval.getText().toString();
            int interval = Integer.parseInt(advIntervalStr) * 20;
            int txPower = (int) afterAdvBinding.tvTxPower.getTag();
            JsonObject object = new JsonObject();
            object.addProperty("adv_interval", interval);
            object.addProperty("tx_power", txPower);
            jsonObject.add("trigger_after_adv", object);
        }
        LayoutAdvParamsBinding beforeAdvBinding = null;
        if (channel == 0)
            beforeAdvBinding = mBind.layoutSingleBeforeAdv;
        if (channel == 1)
            beforeAdvBinding = mBind.layoutDoubleBeforeAdv;
        if (channel == 2)
            beforeAdvBinding = mBind.layoutLongBeforeAdv;
        if (channel == 3)
            beforeAdvBinding = mBind.layoutAbnormalInactivityBeforeAdv;
        LayoutAdvParamsBinding triggerAdvBinding = null;
        if (channel == 0)
            triggerAdvBinding = mBind.layoutSingleTriggerAdv;
        if (channel == 1)
            triggerAdvBinding = mBind.layoutDoubleTriggerAdv;
        if (channel == 2)
            triggerAdvBinding = mBind.layoutLongTriggerAdv;
        if (channel == 3)
            triggerAdvBinding = mBind.layoutAbnormalInactivityTriggerAdv;
        if (channelType == 2 && beforeAdvBinding != null && triggerAdvBinding != null) {
            String advIntervalStr = beforeAdvBinding.etAdvInterval.getText().toString();
            int interval = Integer.parseInt(advIntervalStr) * 20;
            int txPower = (int) beforeAdvBinding.tvTxPower.getTag();
            JsonObject object = new JsonObject();
            object.addProperty("adv_interval", interval);
            object.addProperty("tx_power", txPower);
            jsonObject.add("trigger_before_adv", object);

            String advIntervalTriggerStr = triggerAdvBinding.etAdvInterval.getText().toString();
            int intervalTrigger = Integer.parseInt(advIntervalTriggerStr) * 20;
            int txPowerTrigger = (int) triggerAdvBinding.tvTxPower.getTag();
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

    private boolean isSinglePressParamsValid(View view, int channel) {
        int channelType = (int) view.getTag();
        ArrayList<LayoutAdvParamsBinding> layoutBindings = new ArrayList<>();
        if (channel == 0) {
            if (channelType == 0)
                layoutBindings.add(mBind.layoutSingleNormal);
            if (channelType == 1)
                layoutBindings.add(mBind.layoutSingleAfterAdv);
            if (channelType == 2) {
                layoutBindings.add(mBind.layoutSingleBeforeAdv);
                layoutBindings.add(mBind.layoutSingleTriggerAdv);
            }

        }
        if (channel == 1) {
            if (channelType == 0)
                layoutBindings.add(mBind.layoutDoubleNormal);
            if (channelType == 1)
                layoutBindings.add(mBind.layoutDoubleAfterAdv);
            if (channelType == 2) {
                layoutBindings.add(mBind.layoutDoubleBeforeAdv);
                layoutBindings.add(mBind.layoutDoubleTriggerAdv);
            }

        }
        if (channel == 2) {
            if (channelType == 0)
                layoutBindings.add(mBind.layoutLongNormal);
            if (channelType == 1)
                layoutBindings.add(mBind.layoutLongAfterAdv);
            if (channelType == 2) {
                layoutBindings.add(mBind.layoutLongBeforeAdv);
                layoutBindings.add(mBind.layoutLongTriggerAdv);
            }

        }
        if (channel == 3) {
            if (channelType == 0)
                layoutBindings.add(mBind.layoutAbnormalInactivityNormal);
            if (channelType == 1)
                layoutBindings.add(mBind.layoutAbnormalInactivityAfterAdv);
            if (channelType == 2) {
                layoutBindings.add(mBind.layoutAbnormalInactivityBeforeAdv);
                layoutBindings.add(mBind.layoutAbnormalInactivityTriggerAdv);
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
