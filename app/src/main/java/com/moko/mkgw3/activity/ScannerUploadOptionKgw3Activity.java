package com.moko.mkgw3.activity;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.SeekBar;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkgw3.AppConstants;
import com.moko.mkgw3.R;
import com.moko.mkgw3.activity.filter.DuplicateDataFilterKgw3Activity;
import com.moko.mkgw3.activity.upload.UploadDataOptionKgw3Activity;
import com.moko.mkgw3.activity.filter.FilterAdvNameKgw3Activity;
import com.moko.mkgw3.activity.filter.FilterMacAddressKgw3Activity;
import com.moko.mkgw3.activity.filter.FilterRawDataSwitchKgw3Activity;
import com.moko.mkgw3.activity.upload.UploadDataIntervalKgw3Activity;
import com.moko.mkgw3.base.BaseActivity;
import com.moko.mkgw3.databinding.ActivityScannerUploadOptionKgw3Binding;
import com.moko.mkgw3.dialog.MKgw3BottomDialog;
import com.moko.mkgw3.entity.MQTTConfigKgw3;
import com.moko.mkgw3.entity.MokoDeviceKgw3;
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
import java.util.ArrayList;
import java.util.Arrays;

public class ScannerUploadOptionKgw3Activity extends BaseActivity<ActivityScannerUploadOptionKgw3Binding> implements SeekBar.OnSeekBarChangeListener {
    private MokoDeviceKgw3 mMokoDeviceKgw3;
    private MQTTConfigKgw3 appMqttConfig;
    private String mAppTopic;
    public Handler mHandler;
    private ArrayList<String> mRelationshipValues;
    private ArrayList<String> mDuplicateDataValues;
    private int mRelationshipSelected;
    private int mDuplicateDataSelected;
    private final String[] phyArr = {"1M PHY(V4.2)", "1M PHY(V5.0)", "1M PHY(V4.2) & 1M PHY(V5.0)", "Coded PHY(V5.0)"};
    private int phySelected;

    @Override
    protected void onCreate() {
        mMokoDeviceKgw3 = (MokoDeviceKgw3) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfigKgw3.class);
        mAppTopic = TextUtils.isEmpty(appMqttConfig.topicPublish) ? mMokoDeviceKgw3.topicSubscribe : appMqttConfig.topicPublish;
        mHandler = new Handler(Looper.getMainLooper());

        mBind.tvName.setText(mMokoDeviceKgw3.name);
        mBind.sbRssiFilter.setOnSeekBarChangeListener(this);
        mRelationshipValues = new ArrayList<>();
        mRelationshipValues.add("Null");
        mRelationshipValues.add("Only MAC");
        mRelationshipValues.add("Only ADV Name");
        mRelationshipValues.add("Only RAW DATA");
        mRelationshipValues.add("ADV name&Raw data");
        mRelationshipValues.add("MAC&ADV name&Raw data");
        mRelationshipValues.add("ADV name | Raw data");
        mRelationshipValues.add("ADV NAME & MAC");
        mDuplicateDataValues = new ArrayList<>();
        mDuplicateDataValues.add("Disable");
        mDuplicateDataValues.add("MAC");
        mDuplicateDataValues.add("MAC+DATA TYPE");
        mDuplicateDataValues.add("MAC+RAW DATA");
        mBind.clDuplicateDataFilter.setVisibility(mMokoDeviceKgw3.deviceType != 0 ? View.VISIBLE : View.GONE);
        mBind.rlDuplicateDataFilter.setVisibility(mMokoDeviceKgw3.deviceType != 0 ? View.GONE : View.VISIBLE);
        mBind.tvUploadDataInterval.setVisibility(mMokoDeviceKgw3.deviceType != 0 ? View.VISIBLE : View.GONE);
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        showLoadingProgressDialog();
        getFilterRSSI();
        mBind.tvFilterPhy.setOnClickListener(v -> onFilterPhyClick());
    }

    @Override
    protected ActivityScannerUploadOptionKgw3Binding getViewBinding() {
        return ActivityScannerUploadOptionKgw3Binding.inflate(getLayoutInflater());
    }

    private void onFilterPhyClick() {
        if (isWindowLocked()) return;
        MKgw3BottomDialog dialog = new MKgw3BottomDialog();
        dialog.setDatas(new ArrayList<>(Arrays.asList(phyArr)), phySelected);
        dialog.setListener(value -> {
            phySelected = value;
            mBind.tvFilterPhy.setText(phyArr[value]);
        });
        dialog.show(getSupportFragmentManager());
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
        if (msg_id == MQTTConstants.READ_MSG_ID_FILTER_RSSI) {
            Type type = new TypeToken<MsgReadResult<JsonObject>>() {
            }.getType();
            MsgReadResult<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            final int rssi = result.data.get("rssi").getAsInt();
            int progress = rssi + 127;
            mBind.sbRssiFilter.setProgress(progress);
            mBind.tvRssiFilterValue.setText(String.format("%ddBm", rssi));
            mBind.tvRssiFilterTips.setText(getString(R.string.rssi_filter, rssi));
            getFilterRelationship();
        }
        if (msg_id == MQTTConstants.READ_MSG_ID_FILTER_RELATIONSHIP) {
            Type type = new TypeToken<MsgReadResult<JsonObject>>() {
            }.getType();
            MsgReadResult<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            final int relation = result.data.get("relation").getAsInt();
            mRelationshipSelected = relation;
            mBind.tvFilterRelationship.setText(mRelationshipValues.get(relation));
            getFilterPhy();
        }

        if (msg_id == MQTTConstants.READ_MSG_ID_FILTER_PHY) {
            Type type = new TypeToken<MsgReadResult<JsonObject>>() {
            }.getType();
            MsgReadResult<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            phySelected = result.data.get("phy_filter").getAsInt();
            mBind.tvFilterPhy.setText(phyArr[phySelected]);
            if (mMokoDeviceKgw3.deviceType != 0) {
                getDuplicateDataFilter();
                return;
            }
            mHandler.removeMessages(0);
            dismissLoadingProgressDialog();
        }
        if (msg_id == MQTTConstants.READ_MSG_ID_DUPLICATE_DATA_FILTER) {
            Type type = new TypeToken<MsgReadResult<JsonObject>>() {
            }.getType();
            MsgReadResult<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            mDuplicateDataSelected = result.data.get("rule").getAsInt();
            mBind.tvDuplicateDataFilter.setText(mDuplicateDataValues.get(mDuplicateDataSelected));
        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_FILTER_RSSI) {
            Type type = new TypeToken<MsgConfigResult<?>>() {
            }.getType();
            MsgConfigResult<?> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            if (result.result_code != 0) return;
            setFilterRelationship();
        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_FILTER_RELATIONSHIP) {
            Type type = new TypeToken<MsgConfigResult<?>>() {
            }.getType();
            MsgConfigResult<?> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            if (result.result_code != 0) return;
            setFilterPhy();
        }

        if (msg_id == MQTTConstants.CONFIG_MSG_ID_FILTER_PHY) {
            Type type = new TypeToken<MsgConfigResult<?>>() {
            }.getType();
            MsgConfigResult<?> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            if (mMokoDeviceKgw3.deviceType != 0) {
                setDuplicateDataFilter();
                return;
            }
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            if (result.result_code == 0) {
                ToastUtils.showToast(this, "Set up succeed");
            } else {
                ToastUtils.showToast(this, "Set up failed");
            }
        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_DUPLICATE_DATA_FILTER) {
            Type type = new TypeToken<MsgConfigResult>() {
            }.getType();
            MsgConfigResult result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac))
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
        super.offline(event, mMokoDeviceKgw3.mac);
    }

    public void onBack(View view) {
        finish();
    }

    private void getFilterRSSI() {
        int msgId = MQTTConstants.READ_MSG_ID_FILTER_RSSI;
        String message = assembleReadCommon(msgId, mMokoDeviceKgw3.mac);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setFilterRSSI() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_FILTER_RSSI;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("rssi", mBind.sbRssiFilter.getProgress() - 127);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getFilterRelationship() {
        int msgId = MQTTConstants.READ_MSG_ID_FILTER_RELATIONSHIP;
        String message = assembleReadCommon(msgId, mMokoDeviceKgw3.mac);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setFilterRelationship() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_FILTER_RELATIONSHIP;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("relation", mRelationshipSelected);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getFilterPhy() {
        int msgId = MQTTConstants.READ_MSG_ID_FILTER_PHY;
        String message = assembleReadCommon(msgId, mMokoDeviceKgw3.mac);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setFilterPhy() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_FILTER_PHY;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("phy_filter", phySelected);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getDuplicateDataFilter() {
        int msgId = MQTTConstants.READ_MSG_ID_DUPLICATE_DATA_FILTER;
        String message = assembleReadCommon(msgId, mMokoDeviceKgw3.mac);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setDuplicateDataFilter() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_DUPLICATE_DATA_FILTER;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("rule", mDuplicateDataSelected);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    public void onFilterRelationship(View view) {
        if (isWindowLocked())
            return;
        MKgw3BottomDialog dialog = new MKgw3BottomDialog();
        dialog.setDatas(mRelationshipValues, mRelationshipSelected);
        dialog.setListener(value -> {
            mRelationshipSelected = value;
            mBind.tvFilterRelationship.setText(mRelationshipValues.get(value));
        });
        dialog.show(getSupportFragmentManager());
    }

    public void onFilterDuplicateData(View view) {
        if (isWindowLocked())
            return;
        MKgw3BottomDialog dialog = new MKgw3BottomDialog();
        dialog.setDatas(mDuplicateDataValues, mDuplicateDataSelected);
        dialog.setListener(value -> {
            mDuplicateDataSelected = value;
            mBind.tvDuplicateDataFilter.setText(mDuplicateDataValues.get(value));
        });
        dialog.show(getSupportFragmentManager());
    }


    public void onDuplicateDataFilter(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, DuplicateDataFilterKgw3Activity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
        startActivity(i);
    }

    public void onUploadDataOption(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, UploadDataOptionKgw3Activity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
        startActivity(i);
    }

    public void onUploadDataInterval(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, UploadDataIntervalKgw3Activity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
        startActivity(i);
    }

    public void onFilterByMac(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, FilterMacAddressKgw3Activity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
        startActivity(i);
    }

    public void onFilterByName(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, FilterAdvNameKgw3Activity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
        startActivity(i);
    }

    public void onFilterByRawData(View view) {
        if (isWindowLocked())
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        Intent i = new Intent(this, FilterRawDataSwitchKgw3Activity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
        startActivity(i);
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
        setFilterRSSI();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppConstants.REQUEST_CODE_FILTER_CONDITION) {
            showLoadingProgressDialog();
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                finish();
            }, 30 * 1000);
            getFilterRSSI();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int rssi = progress - 127;
        mBind.tvRssiFilterValue.setText(String.format("%ddBm", rssi));
        mBind.tvRssiFilterTips.setText(getString(R.string.rssi_filter, rssi));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
