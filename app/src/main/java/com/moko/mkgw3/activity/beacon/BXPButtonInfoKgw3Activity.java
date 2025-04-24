package com.moko.mkgw3.activity.beacon;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkgw3.AppConstants;
import com.moko.mkgw3.R;
import com.moko.mkgw3.activity.DeviceDetailKgw3Activity;
import com.moko.mkgw3.base.BaseActivity;
import com.moko.mkgw3.databinding.ActivityBxpButtonInfoKgw3Binding;
import com.moko.mkgw3.db.MKgw3DBTools;
import com.moko.lib.scannerui.dialog.AlertMessageDialog;
import com.moko.mkgw3.dialog.LedBuzzerControlDialog;
import com.moko.mkgw3.entity.MQTTConfigKgw3;
import com.moko.mkgw3.entity.MokoDeviceKgw3;
import com.moko.mkgw3.utils.SPUtiles;
import com.moko.lib.scannerui.utils.ToastUtils;
import com.moko.support.mkgw3.MQTTConstants;
import com.moko.lib.mqtt.MQTTSupport;
import com.moko.support.mkgw3.entity.BeaconInfo;
import com.moko.lib.mqtt.entity.MsgNotify;
import com.moko.lib.mqtt.event.DeviceModifyNameEvent;
import com.moko.lib.mqtt.event.DeviceOnlineEvent;
import com.moko.lib.mqtt.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;

public class BXPButtonInfoKgw3Activity extends BaseActivity<ActivityBxpButtonInfoKgw3Binding> {
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
        String alarmStatusStr = "";
        if (mBeaconInfo.alarm_status == 0) {
            alarmStatusStr = "Not triggered";
        } else {
            StringBuilder modeStr = new StringBuilder();
            if ((mBeaconInfo.alarm_status & 0x01) == 0x01)
                modeStr.append("1&");
            if ((mBeaconInfo.alarm_status & 0x02) == 0x02)
                modeStr.append("2&");
            if ((mBeaconInfo.alarm_status & 0x04) == 0x04)
                modeStr.append("3&");
            if ((mBeaconInfo.alarm_status & 0x08) == 0x08)
                modeStr.append("4&");
            String mode = modeStr.substring(0, modeStr.length() - 1);
            alarmStatusStr = String.format("Mode %s triggered", mode);
        }
        mBind.tvAlarmStatus.setText(alarmStatusStr);
        mBind.tvLedControl.setOnClickListener(v -> showControl(0));
        mBind.tvBuzzerControl.setOnClickListener(v -> showControl(1));
    }

    private void showControl(int index) {
        LedBuzzerControlDialog dialog = new LedBuzzerControlDialog(index);
        dialog.setOnConfirmClickListener((duration, interval, from) -> {
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Set up failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            int msgId = from == 0 ? MQTTConstants.CONFIG_MSG_ID_BLE_BXP_BUTTON_LED : MQTTConstants.CONFIG_MSG_ID_BLE_BXP_BUTTON_BUZZER;
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("mac", mBeaconInfo.mac);
            jsonObject.addProperty(from == 0 ? "flash_time" : "ring_time", duration);
            jsonObject.addProperty(from == 0 ? "flash_interval" : "ring_interval", interval);
            String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
            try {
                MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        });
        dialog.showNow(getSupportFragmentManager(), "control");
    }

    @Override
    protected ActivityBxpButtonInfoKgw3Binding getViewBinding() {
        return ActivityBxpButtonInfoKgw3Binding.inflate(getLayoutInflater());
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
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_BUTTON_STATUS) {
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
                String alarmStatusStr = "";
                if (bxpButtonInfo.alarm_status == 0) {
                    alarmStatusStr = "Not triggered";
                } else {
                    StringBuilder modeStr = new StringBuilder();
                    if ((bxpButtonInfo.alarm_status & 0x01) == 0x01)
                        modeStr.append("1&");
                    if ((bxpButtonInfo.alarm_status & 0x02) == 0x02)
                        modeStr.append("2&");
                    if ((bxpButtonInfo.alarm_status & 0x04) == 0x04)
                        modeStr.append("3&");
                    if ((bxpButtonInfo.alarm_status & 0x08) == 0x08)
                        modeStr.append("4&");
                    String mode = modeStr.substring(0, modeStr.length() - 1);
                    alarmStatusStr = String.format("Mode %s triggered", mode);
                }
                mBind.tvAlarmStatus.setText(alarmStatusStr);
            });
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_BUTTON_DISMISS_ALARM) {
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
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_BUTTON_DISCONNECTED
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
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_BUTTON_LED || msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_BUTTON_BUZZER) {
            runOnUiThread(() -> {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
                Type type = new TypeToken<MsgNotify<BeaconInfo>>() {
                }.getType();
                MsgNotify<BeaconInfo> result = new Gson().fromJson(message, type);
                if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
                ToastUtils.showToast(this, result.data.result_code == 0 ? "Setup succeed!" : "Setup failed");
            });
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceModifyNameEvent(DeviceModifyNameEvent event) {
        // 修改了设备名称
        MokoDeviceKgw3 device = MKgw3DBTools.getInstance(BXPButtonInfoKgw3Activity.this).selectDevice(mMokoDeviceKgw3.mac);
        mMokoDeviceKgw3.name = device.name;
        mBind.tvDeviceName.setText(mMokoDeviceKgw3.name);
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
        Intent intent = new Intent(this, BeaconDFUKgw3Activity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
        intent.putExtra(AppConstants.EXTRA_KEY_MAC, mBeaconInfo.mac);
        startBeaconDFU.launch(intent);
    }

    private final ActivityResultLauncher<Intent> startBeaconDFU = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK && null != result.getData()) {
            int code = result.getData().getIntExtra("code", 0);
            if (code != 3) {
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
                ToastUtils.showToast(BXPButtonInfoKgw3Activity.this, "Setup failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            disconnectDevice();
        });
        dialog.show(getSupportFragmentManager());
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
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_B_D_STATUS;
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
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_BUTTON_DISMISS_ALARM;
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
