package com.moko.mkgw3.activity.beacon;

import android.annotation.SuppressLint;
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
import com.moko.mkgw3.databinding.ActivityBxpCInfoKgw3Binding;
import com.moko.mkgw3.db.MKgw3DBTools;
import com.moko.mkgw3.dialog.AlertMessageDialog;
import com.moko.mkgw3.entity.MQTTConfigKgw3;
import com.moko.mkgw3.entity.MokoDeviceKgw3;
import com.moko.mkgw3.utils.SPUtiles;
import com.moko.mkgw3.utils.ToastUtils;
import com.moko.support.mkgw3.MQTTConstants;
import com.moko.support.mkgw3.MQTTSupport;
import com.moko.support.mkgw3.entity.BeaconInfo;
import com.moko.support.mkgw3.entity.MsgNotify;
import com.moko.support.mkgw3.event.DeviceModifyNameEvent;
import com.moko.support.mkgw3.event.DeviceOnlineEvent;
import com.moko.support.mkgw3.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;

/**
 * @author: jun.liu
 * @date: 2023/8/30 20:29
 * @des:
 */
public class BXPCGW3Activity extends BaseActivity<ActivityBxpCInfoKgw3Binding> {
    private MokoDeviceKgw3 mMokoDeviceKgw3;
    private MQTTConfigKgw3 appMqttConfig;
    private String mAppTopic;
    private BeaconInfo mBeaconInfo;
    private Handler mHandler;

    @Override
    protected ActivityBxpCInfoKgw3Binding getViewBinding() {
        return ActivityBxpCInfoKgw3Binding.inflate(getLayoutInflater());
    }

    @SuppressLint("SetTextI18n")
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
        mBind.tvDeviceMac.setText(mBeaconInfo.mac.toUpperCase());
        mBind.tvBatteryVoltage.setText(mBeaconInfo.battery_level + "%");
        int status = mBeaconInfo.sensor_status;
        int accStatus = (status & 0x01) == 1 ? 1 : 0;
        int thStatus = (status >> 1 & 0x01) == 1 ? 1 : 0;
        int lightStatus = (status >> 2 & 0x01) == 1 ? 1 : 0;
        StringBuilder builder = new StringBuilder();
        builder.append(accStatus == 1 ? "ACC" : "").append(thStatus == 1 ? "&TH" : "").append(lightStatus == 1 ? "&Light" : "");
        String state = builder.toString();
        if (state.startsWith("&")) state = state.substring(1);
        mBind.tvSensorState.setText(state);
//        if (thStatus == 0 && lightStatus == 0) {
//            mBind.tvHistorySensor.setVisibility(View.GONE);
//        } else {
//            mBind.tvHistorySensor.setVisibility(View.VISIBLE);
//        }
//        if (thStatus == 0 && lightStatus == 0 && accStatus == 0) {
//            mBind.tvRealtimeSensor.setVisibility(View.GONE);
//        } else {
//            mBind.tvRealtimeSensor.setVisibility(View.VISIBLE);
//        }
//        mBind.tvAcc.setVisibility(accStatus == 1 ? View.VISIBLE : View.GONE);
//        mBind.tvThHistory.setVisibility(thStatus == 1 ? View.VISIBLE : View.GONE);
//        mBind.tvThRealTime.setVisibility(thStatus == 1 ? View.VISIBLE : View.GONE);
//        mBind.tvLightHistory.setVisibility(lightStatus == 1 ? View.VISIBLE : View.GONE);
//        mBind.tvLightRealTime.setVisibility(lightStatus == 1 ? View.VISIBLE : View.GONE);
//        mBind.tvReadBattery.setOnClickListener(v -> readBattery());
        mBind.tvPowerOff.setOnClickListener(v -> powerOff());
        mBind.tvThRealtime.setOnClickListener(v -> startActivity(THDataActivity.class, null));
        mBind.tvThHistory.setOnClickListener(v -> startActivity(THDataActivity.class, "history"));
        mBind.tvAccData.setOnClickListener(v -> gotoAccData());
        mBind.tvThSampleRate.setOnClickListener(v -> gotoTHSampleRate());
        mBind.tvAdvParams.setOnClickListener(v -> gotoAdvParams());
    }

    private void startActivity(Class<?> clazz, String flag) {
        Intent intent = new Intent(this, clazz);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
        intent.putExtra(AppConstants.EXTRA_KEY_MAC, mBeaconInfo.mac);
        intent.putExtra("flag", flag);
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

    private void gotoTHSampleRate() {
        if (isWindowLocked()) return;
        Intent intent = new Intent(this, BXPCTHSampleRateGW3Activity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
        intent.putExtra(AppConstants.EXTRA_KEY_MAC, mBeaconInfo.mac);
        startActivity(intent);
    }

    private void gotoAdvParams() {
        if (isWindowLocked()) return;
        Intent intent = new Intent(this, BXPAdvParamsGW3Activity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
        intent.putExtra(AppConstants.EXTRA_KEY_MAC, mBeaconInfo.mac);
        startActivity(intent);
    }

    //关机
    private void powerOff() {
        if (isWindowLocked()) return;
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Warning！");
        dialog.setMessage("Are you sure to turn off the Beacon?Please make sure the Beacon has a button to turn on!");
        dialog.setConfirm("OK");
        dialog.setOnAlertConfirmListener(() -> {
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Setup failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_C_POWER_OFF;
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTMessageArrivedEvent(MQTTMessageArrivedEvent event) {
        // 更新所有设备的网络状态
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
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_C_POWER_OFF) {
            //关机结果通知
            Type type = new TypeToken<MsgNotify<JsonObject>>() {
            }.getType();
            MsgNotify<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            int code = result.data.get("result_code").getAsInt();
            ToastUtils.showToast(this, code == 0 ? "Setup succeed！" : "setup failed");
            if (code == 0) {
                EventBus.getDefault().unregister(this);
                Intent intent = new Intent(this, DeviceDetailKgw3Activity.class);
                startActivity(intent);
                finish();
            }
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_DISCONNECT
                || msg_id == MQTTConstants.CONFIG_MSG_ID_BLE_DISCONNECT) {
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            Type type = new TypeToken<MsgNotify<JsonObject>>() {
            }.getType();
            MsgNotify<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            ToastUtils.showToast(this, "Bluetooth disconnect");
            finish();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceModifyNameEvent(DeviceModifyNameEvent event) {
        // 修改了设备名称
        MokoDeviceKgw3 device = MKgw3DBTools.getInstance(getApplicationContext()).selectDevice(mMokoDeviceKgw3.mac);
        mMokoDeviceKgw3.name = device.name;
        mBind.tvDeviceName.setText(mMokoDeviceKgw3.name);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceOnlineEvent(DeviceOnlineEvent event) {
        String mac = event.getMac();
        if (!mMokoDeviceKgw3.mac.equals(mac)) return;
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
            int code = result.getData().getIntExtra("code", 0);
            if (code != 3) {
                ToastUtils.showToast(this, "Bluetooth disconnect");
                finish();
            }
        }
    });

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
                ToastUtils.showToast(this, "Setup failed");
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
