package com.moko.mkgw3.activity.filter;


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
import com.moko.mkgw3.base.BaseActivity;
import com.moko.mkgw3.databinding.ActivityFilterRawDataSwitchKgw3Binding;
import com.moko.mkgw3.entity.MQTTConfigKgw3;
import com.moko.mkgw3.entity.MokoDeviceKgw3;
import com.moko.mkgw3.utils.SPUtiles;
import com.moko.lib.scannerui.utils.ToastUtils;
import com.moko.support.mkgw3.MQTTConstants;
import com.moko.lib.mqtt.MQTTSupport;
import com.moko.lib.mqtt.entity.MsgConfigResult;
import com.moko.lib.mqtt.entity.MsgReadResult;
import com.moko.lib.mqtt.event.DeviceOnlineEvent;
import com.moko.lib.mqtt.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;

public class FilterRawDataSwitchKgw3Activity extends BaseActivity<ActivityFilterRawDataSwitchKgw3Binding> {

    private MokoDeviceKgw3 mMokoDeviceKgw3;
    private MQTTConfigKgw3 appMqttConfig;
    private String mAppTopic;

    public Handler mHandler;

    private boolean isBXPDeviceInfoOpen;
    private boolean isBXPAccOpen;
    private boolean isBXPTHOpen;

    @Override
    protected void onCreate() {
        mMokoDeviceKgw3 = (MokoDeviceKgw3) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfigKgw3.class);
        mAppTopic = TextUtils.isEmpty(appMqttConfig.topicPublish) ? mMokoDeviceKgw3.topicSubscribe : appMqttConfig.topicPublish;

        mBind.rlFilterByMKTOF.setVisibility(mMokoDeviceKgw3.deviceType != 0 ? View.VISIBLE : View.GONE);
        mBind.tvFilterByBxpTagTitle.setText(mMokoDeviceKgw3.deviceType != 0 ? "BXP - Tag/Sensor" : "BXP - Tag");

        mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        showLoadingProgressDialog();
        getFilterRawDataSwitch();
    }

    @Override
    protected ActivityFilterRawDataSwitchKgw3Binding getViewBinding() {
        return ActivityFilterRawDataSwitchKgw3Binding.inflate(getLayoutInflater());
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
        if (msg_id == MQTTConstants.READ_MSG_ID_FILTER_RAW_DATA_SWITCH) {
            Type type = new TypeToken<MsgReadResult<JsonObject>>() {
            }.getType();
            MsgReadResult<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            mBind.tvFilterByIbeacon.setText(result.data.get("ibeacon").getAsInt() == 1 ? "ON" : "OFF");
            mBind.tvFilterByUid.setText(result.data.get("eddystone_uid").getAsInt() == 1 ? "ON" : "OFF");
            mBind.tvFilterByUrl.setText(result.data.get("eddystone_url").getAsInt() == 1 ? "ON" : "OFF");
            mBind.tvFilterByTlm.setText(result.data.get("eddystone_tlm").getAsInt() == 1 ? "ON" : "OFF");
            isBXPDeviceInfoOpen = result.data.get("bxp_devinfo").getAsInt() == 1;
            isBXPAccOpen = result.data.get("bxp_acc").getAsInt() == 1;
            isBXPTHOpen = result.data.get("bxp_th").getAsInt() == 1;
            mBind.ivFilterByBxpInfo.setImageResource(isBXPDeviceInfoOpen ? R.drawable.ic_checkbox_open : R.drawable.ic_checkbox_close);
            mBind.ivFilterByBxpAcc.setImageResource(isBXPAccOpen ? R.drawable.ic_checkbox_open : R.drawable.ic_checkbox_close);
            mBind.ivFilterByBxpTh.setImageResource(isBXPTHOpen ? R.drawable.ic_checkbox_open : R.drawable.ic_checkbox_close);
            mBind.tvFilterByBxpButton.setText(result.data.get("bxp_button").getAsInt() == 1 ? "ON" : "OFF");
            mBind.tvFilterByPir.setText(result.data.get("pir").getAsInt() == 1 ? "ON" : "OFF");
            mBind.tvFilterByOther.setText(result.data.get("other").getAsInt() == 1 ? "ON" : "OFF");
            mBind.tvFilterByBxpTag.setText(result.data.get("bxp_tag").getAsInt() == 1 ? "ON" : "OFF");
            if (mMokoDeviceKgw3.deviceType != 0) {
                mBind.tvFilterByMKTOF.setText(result.data.get("mk_tof").getAsInt() == 1 ? "ON" : "OFF");
            }
        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_FILTER_BXP_DEVICE_INFO
                || msg_id == MQTTConstants.CONFIG_MSG_ID_FILTER_BXP_ACC
                || msg_id == MQTTConstants.CONFIG_MSG_ID_FILTER_BXP_TH) {
            Type type = new TypeToken<MsgConfigResult>() {
            }.getType();
            MsgConfigResult result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            if (result.result_code == 0) {
                getFilterRawDataSwitch();
                ToastUtils.showToast(this, "Set up succeed");
            } else {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
                ToastUtils.showToast(this, "Set up failed");
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceOnlineEvent(DeviceOnlineEvent event) {
        super.offline(event, mMokoDeviceKgw3.mac);
    }

    private void getFilterRawDataSwitch() {
        int msgId = MQTTConstants.READ_MSG_ID_FILTER_RAW_DATA_SWITCH;
        String message = assembleReadCommon(msgId, mMokoDeviceKgw3.mac);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onBack(View view) {
        finish();
    }

    public void onFilterByBXPDeviceInfo(View view) {
        if (isWindowLocked())
            return;
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        setBXPDevice();
    }

    public void onFilterByBXPAcc(View view) {
        if (isWindowLocked())
            return;
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        setBXPAcc();
    }

    public void onFilterByBXPTH(View view) {
        if (isWindowLocked())
            return;
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        setBXPTH();
    }

    private void setBXPDevice() {
        isBXPDeviceInfoOpen = !isBXPDeviceInfoOpen;
        int msgId = MQTTConstants.CONFIG_MSG_ID_FILTER_BXP_DEVICE_INFO;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("switch_value", isBXPDeviceInfoOpen ? 1 : 0);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setBXPAcc() {
        isBXPAccOpen = !isBXPAccOpen;
        int msgId = MQTTConstants.CONFIG_MSG_ID_FILTER_BXP_ACC;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("switch_value", isBXPAccOpen ? 1 : 0);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setBXPTH() {
        isBXPTHOpen = !isBXPTHOpen;
        int msgId = MQTTConstants.CONFIG_MSG_ID_FILTER_BXP_TH;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("switch_value", isBXPTHOpen ? 1 : 0);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onFilterByIBeacon(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, FilterIBeaconKgw3Activity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
        startFilterDetail.launch(i);
    }

    public void onFilterByUid(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, FilterUIDKgw3Activity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
        startFilterDetail.launch(i);
    }

    public void onFilterByUrl(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, FilterUrlKgw3Activity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
        startFilterDetail.launch(i);
    }

    public void onFilterByTlm(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, FilterTLMKgw3Activity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
        startFilterDetail.launch(i);
    }

    public void onFilterByBXPButton(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, FilterBXPButtonKgw3Activity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
        startFilterDetail.launch(i);
    }

    public void onFilterByBXPTag(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, FilterBXPTagKgw3Activity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
        startFilterDetail.launch(i);
    }

    public void onFilterByPIRPresence(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, FilterPIRKgw3Activity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
        startFilterDetail.launch(i);
    }

    public void onFilterByMKTOF(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, FilterMKTOFActivity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
        startFilterDetail.launch(i);
    }


    public void onFilterByOther(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, FilterOtherKgw3Activity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
        startFilterDetail.launch(i);
    }

    private final ActivityResultLauncher<Intent> startFilterDetail = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        showLoadingProgressDialog();
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        getFilterRawDataSwitch();
    });
}
