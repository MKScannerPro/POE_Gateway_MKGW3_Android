package com.moko.mkgw3.activity.beacon;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkgw3.AppConstants;
import com.moko.mkgw3.R;
import com.moko.mkgw3.activity.DeviceDetailKgw3Activity;
import com.moko.mkgw3.base.BaseActivity;
import com.moko.mkgw3.databinding.ActivityBxpBCrInfoKgw3Binding;
import com.moko.mkgw3.dialog.AlertMessageDialog;
import com.moko.mkgw3.entity.MQTTConfigKgw3;
import com.moko.mkgw3.entity.MokoDeviceKgw3;
import com.moko.mkgw3.utils.SPUtiles;
import com.moko.mkgw3.utils.ToastUtils;
import com.moko.support.mkgw3.MQTTConstants;
import com.moko.support.mkgw3.MQTTSupport;
import com.moko.support.mkgw3.entity.BeaconInfo;
import com.moko.support.mkgw3.entity.MsgNotify;
import com.moko.support.mkgw3.event.DeviceOnlineEvent;
import com.moko.support.mkgw3.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class BXPBCRGW3Activity extends BaseActivity<ActivityBxpBCrInfoKgw3Binding> {
    private MokoDeviceKgw3 mMokoDeviceKgw3;
    private MQTTConfigKgw3 appMqttConfig;
    private String mAppTopic;
    private BeaconInfo mBeaconInfo;
    private Handler mHandler;

    @Override
    protected void onCreate() {
        mMokoDeviceKgw3 = (MokoDeviceKgw3) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfigKgw3.class);
        mAppTopic = TextUtils.isEmpty(appMqttConfig.topicPublish) ? mMokoDeviceKgw3.topicSubscribe : appMqttConfig.topicPublish;
        mHandler = new Handler(Looper.getMainLooper());

        mBeaconInfo = (BeaconInfo) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_BEACON_INFO);
        mBind.tvDeviceName.setText(mMokoDeviceKgw3.name);
        mBind.tvProductModel.setText(mBeaconInfo.product_model);
        mBind.tvManufacturer.setText(mBeaconInfo.company_name);
        mBind.tvDeviceFirmwareVersion.setText(mBeaconInfo.firmware_version);
        mBind.tvDeviceHardwareVersion.setText(mBeaconInfo.hardware_version);
        mBind.tvDeviceSoftwareVersion.setText(mBeaconInfo.software_version);
        mBind.tvDeviceMac.setText(mBeaconInfo.mac);
        mBind.tvBatteryVoltage.setText(String.format("%dmV", mBeaconInfo.battery_v));
        mBind.tvSinglePressCount.setText(String.valueOf(mBeaconInfo.single_alarm_num));
        mBind.tvDoublePressCount.setText(String.valueOf(mBeaconInfo.double_alarm_num));
        mBind.tvLongPressCount.setText(String.valueOf(mBeaconInfo.long_alarm_num));

        mBind.tvRemoteReminder.setOnClickListener(v -> gotoRemoteReminder());
        mBind.tvAlarmEventData.setOnClickListener(v -> gotoAlarmEventData());
        mBind.tvAccData.setOnClickListener(v -> gotoAccData());
        mBind.tvAdvParams.setOnClickListener(v -> gotoAdvParams());
        mBind.tvPowerOff.setOnClickListener(v -> showPowerOffDialog());
    }

    private void gotoRemoteReminder() {
        if (isWindowLocked()) return;
        Intent intent = new Intent(this, BXPBCRRemoteReminderGW3Activity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
        intent.putExtra(AppConstants.EXTRA_KEY_MAC, mBeaconInfo.mac);
        startActivity(intent);
    }

    private void gotoAlarmEventData() {
        if (isWindowLocked()) return;
        Intent intent = new Intent(this, BXPCRAlarmEventGW3Activity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
        intent.putExtra(AppConstants.EXTRA_KEY_MAC, mBeaconInfo.mac);
        startActivity(intent);
    }

    private void gotoAccData() {
        if (isWindowLocked()) return;
        Intent intent = new Intent(this, BXPAccGW3Activity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
        intent.putExtra(AppConstants.EXTRA_KEY_MAC, mBeaconInfo.mac);
        intent.putExtra(AppConstants.EXTRA_KEY_BEACON_TYPE, mBeaconInfo.type);
        startActivity(intent);
    }

    private void gotoAdvParams() {
        if (isWindowLocked()) return;
        Intent intent = new Intent(this, BXPBAdvParamsGW3Activity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
        intent.putExtra(AppConstants.EXTRA_KEY_BEACON_INFO, mBeaconInfo);
        startActivity(intent);
    }

    private void showPowerOffDialog() {
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setMessage("Please confirm again whether to power off BLE device");
        dialog.setOnAlertConfirmListener(() -> {
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Set up failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_B_CR_POWER_OFF;
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("mac", mBeaconInfo.mac);
            String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
            try {
                MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        });
        dialog.show(getSupportFragmentManager());
    }

    @Override
    protected ActivityBxpBCrInfoKgw3Binding getViewBinding() {
        return ActivityBxpBCrInfoKgw3Binding.inflate(getLayoutInflater());
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 200)
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
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_B_CR_STATUS) {
            EventBus.getDefault().cancelEventDelivery(event);
            runOnUiThread(() -> {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
                Type type = new TypeToken<MsgNotify<BeaconInfo>>() {
                }.getType();
                MsgNotify<BeaconInfo> result = new Gson().fromJson(message, type);
                if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac))
                    return;
                BeaconInfo bxpButtonInfo = result.data;
                if (bxpButtonInfo.result_code != 0) {
                    ToastUtils.showToast(this, "Setup failed");
                    return;
                }
                ToastUtils.showToast(this, "Setup succeed!");
                mBind.tvBatteryVoltage.setText(String.format("%dmV", bxpButtonInfo.battery_v));
                mBind.tvSinglePressCount.setText(String.valueOf(bxpButtonInfo.single_alarm_num));
                mBind.tvDoublePressCount.setText(String.valueOf(bxpButtonInfo.double_alarm_num));
                mBind.tvLongPressCount.setText(String.valueOf(bxpButtonInfo.long_alarm_num));
            });
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_B_CR_CLEAR_PRESS_EVENT) {
            runOnUiThread(() -> {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
                Type type = new TypeToken<MsgNotify<BeaconInfo>>() {
                }.getType();
                MsgNotify<BeaconInfo> result = new Gson().fromJson(message, type);
                if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac))
                    return;
                BeaconInfo bxpButtonInfo = result.data;
                if (bxpButtonInfo.result_code != 0) {
                    ToastUtils.showToast(this, "Setup failed");
                    return;
                }
                ToastUtils.showToast(this, "Setup succeed!");
                mHandler.postDelayed(() -> {
                    dismissLoadingProgressDialog();
                    ToastUtils.showToast(this, "Setup failed");
                }, 30 * 1000);
                showLoadingProgressDialog();
                getBXPButtonStatus();
            });
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_B_CR_DISMISS_ALARM) {
            runOnUiThread(() -> {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
                Type type = new TypeToken<MsgNotify<BeaconInfo>>() {
                }.getType();
                MsgNotify<BeaconInfo> result = new Gson().fromJson(message, type);
                if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac))
                    return;
                BeaconInfo bxpButtonInfo = result.data;
                if (bxpButtonInfo.result_code != 0) {
                    ToastUtils.showToast(this, "Setup failed");
                    return;
                }
                ToastUtils.showToast(this, "Setup succeed!");
                mHandler.postDelayed(() -> {
                    dismissLoadingProgressDialog();
                    ToastUtils.showToast(this, "Setup failed");
                }, 30 * 1000);
                showLoadingProgressDialog();
                getBXPButtonStatus();
            });
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_B_CR_POWER_OFF) {
            runOnUiThread(() -> {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
                Type type = new TypeToken<MsgNotify<BeaconInfo>>() {
                }.getType();
                MsgNotify<BeaconInfo> result = new Gson().fromJson(message, type);
                if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac))
                    return;
                BeaconInfo bxpButtonInfo = result.data;
                if (bxpButtonInfo.result_code != 0) {
                    ToastUtils.showToast(this, "Setup failed");
                    return;
                }
                ToastUtils.showToast(this, "Setup succeed!");
            });
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_DISCONNECT
                || msg_id == MQTTConstants.CONFIG_MSG_ID_BLE_DISCONNECT) {
            runOnUiThread(() -> {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
                Type type = new TypeToken<MsgNotify<JsonObject>>() {
                }.getType();
                MsgNotify<JsonObject> result = new Gson().fromJson(message, type);
                if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac))
                    return;
                ToastUtils.showToast(this, "Bluetooth disconnect");
                finish();
            });
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

    public void onDFU(View view) {
        if (isWindowLocked()) return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent intent = new Intent(this, BeaconDFUKgw3v2Activity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
        intent.putExtra(AppConstants.EXTRA_KEY_MAC, mBeaconInfo.mac);
        intent.putExtra(AppConstants.EXTRA_KEY_BEACON_TYPE, mBeaconInfo.type);
        startBeaconDFU.launch(intent);
    }

    private final ActivityResultLauncher<Intent> startBeaconDFU = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK && null != result.getData()) {
            int code = result.getData().getIntExtra("code", 1);
            if (code != 1) {
                ToastUtils.showToast(this, "Bluetooth disconnect");
                finish();
            }
        }
    });

    public void onReadBXPButtonStatus(View view) {
        if (isWindowLocked()) return;
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Setup failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        getBXPButtonStatus();
    }

    public void onClearPressEvent(View view) {
        if (isWindowLocked()) return;
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Setup failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        int type = (int) view.getTag();
        clearPressEvent(type);
    }


    public void onDismissAlarmStatus(View view) {
        if (isWindowLocked()) return;
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Setup failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        dismissAlarmStatus();
    }

    public void onDisconnect(View view) {
        if (isWindowLocked()) return;
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setMessage("Please confirm again whether to disconnect the gateway from BLE devices?");
        dialog.setOnAlertConfirmListener(() -> {
            if (!MQTTSupport.getInstance().isConnected()) {
                ToastUtils.showToast(this, R.string.network_error);
                return;
            }
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(BXPBCRGW3Activity.this, "Setup failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            disconnectDevice();
        });
        dialog.show(getSupportFragmentManager());
    }

    private void clearPressEvent(int type) {
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_B_CR_CLEAR_PRESS_EVENT;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", mBeaconInfo.mac);
        jsonObject.addProperty("type", type);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void disconnectDevice() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_DISCONNECT;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", mBeaconInfo.mac);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getBXPButtonStatus() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_B_CR_STATUS;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", mBeaconInfo.mac);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void dismissAlarmStatus() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_B_CR_DISMISS_ALARM;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mac", mBeaconInfo.mac);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        if (isWindowLocked()) return;
        backToDetail();
    }

    public void onBack(View view) {
        if (isWindowLocked()) return;
        backToDetail();
    }

    private void backToDetail() {
        Intent intent = new Intent(this, DeviceDetailKgw3Activity.class);
        startActivity(intent);
    }
}
