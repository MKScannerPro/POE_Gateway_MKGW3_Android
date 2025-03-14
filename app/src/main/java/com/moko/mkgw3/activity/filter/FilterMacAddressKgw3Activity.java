package com.moko.mkgw3.activity.filter;


import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkgw3.AppConstants;
import com.moko.mkgw3.R;
import com.moko.mkgw3.base.BaseActivity;
import com.moko.mkgw3.databinding.ActivityFilterMacAddressKgw3Binding;
import com.moko.mkgw3.dialog.AlertMessageDialog;
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
import java.util.List;

public class FilterMacAddressKgw3Activity extends BaseActivity<ActivityFilterMacAddressKgw3Binding> {

    private MokoDeviceKgw3 mMokoDeviceKgw3;
    private MQTTConfigKgw3 appMqttConfig;
    private String mAppTopic;

    public Handler mHandler;

    private List<String> filterMacAddress;

    @Override
    protected void onCreate() {
        mMokoDeviceKgw3 = (MokoDeviceKgw3) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfigKgw3.class);
        mAppTopic = TextUtils.isEmpty(appMqttConfig.topicPublish) ? mMokoDeviceKgw3.topicSubscribe : appMqttConfig.topicPublish;
        mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        showLoadingProgressDialog();
        getFilterMacAddress();
    }

    @Override
    protected ActivityFilterMacAddressKgw3Binding getViewBinding() {
        return ActivityFilterMacAddressKgw3Binding.inflate(getLayoutInflater());
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
        if (msg_id == MQTTConstants.READ_MSG_ID_FILTER_MAC_ADDRESS) {
            Type type = new TypeToken<MsgReadResult<JsonObject>>() {
            }.getType();
            MsgReadResult<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            mBind.cbPreciseMatch.setChecked(result.data.get("precise").getAsInt() == 1);
            mBind.cbReverseFilter.setChecked(result.data.get("reverse").getAsInt() == 1);
            JsonArray macList = result.data.getAsJsonArray("mac");
            int number = macList.size();
            filterMacAddress = new ArrayList<>();
            if (number != 0) {
                int index = 1;
                for (JsonElement jsonElement : macList) {
                    filterMacAddress.add(jsonElement.getAsString());
                    String macAddress = jsonElement.getAsString();
                    View v = LayoutInflater.from(this).inflate(R.layout.item_mac_address_filter, mBind.llMacAddress, false);
                    TextView title = v.findViewById(R.id.tv_mac_address_title);
                    EditText etMacAddress = v.findViewById(R.id.et_mac_address);
                    title.setText(String.format("MAC %d", index));
                    etMacAddress.setText(macAddress);
                    mBind.llMacAddress.addView(v);
                    index++;
                }
            }
        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_FILTER_MAC_ADDRESS) {
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

    public void back(View view) {
        finish();
    }

    private void getFilterMacAddress() {
        int msgId = MQTTConstants.READ_MSG_ID_FILTER_MAC_ADDRESS;
        String message = assembleReadCommon(msgId, mMokoDeviceKgw3.mac);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onSave(View view) {
        if (isWindowLocked()) return;
        if (isValid()) {
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Set up failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            saveParams();
        }
    }

    public void onAdd(View view) {
        if (isWindowLocked())
            return;
        int count = mBind.llMacAddress.getChildCount();
        if (count > 9) {
            ToastUtils.showToast(this, "You can set up to 10 filters!");
            return;
        }
        View v = LayoutInflater.from(this).inflate(R.layout.item_mac_address_filter, mBind.llMacAddress, false);
        TextView title = v.findViewById(R.id.tv_mac_address_title);
        title.setText(String.format("MAC %d", count + 1));
        mBind.llMacAddress.addView(v);
    }

    public void onDel(View view) {
        if (isWindowLocked())
            return;
        final int c = mBind.llMacAddress.getChildCount();
        if (c == 0) {
            ToastUtils.showToast(this, "There are currently no filters to delete");
            return;
        }
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Warning");
        dialog.setMessage("Please confirm whether to delete it, if yes, the last option will be deleted!");
        dialog.setOnAlertConfirmListener(() -> {
            int count = mBind.llMacAddress.getChildCount();
            if (count > 0) {
                mBind.llMacAddress.removeViewAt(count - 1);
            }
        });
        dialog.show(getSupportFragmentManager());
    }


    private void saveParams() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_FILTER_MAC_ADDRESS;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("precise", mBind.cbPreciseMatch.isChecked() ? 1 : 0);
        jsonObject.addProperty("reverse", mBind.cbReverseFilter.isChecked() ? 1 : 0);
        JsonArray macList = new JsonArray();
        for (String mac : filterMacAddress)
            macList.add(mac);
        jsonObject.add("mac", macList);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private boolean isValid() {
        final int c = mBind.llMacAddress.getChildCount();
        if (c > 0) {
            // 发送设置的过滤RawData
            int count = mBind.llMacAddress.getChildCount();
            if (count == 0) {
                ToastUtils.showToast(this, "Para Error");
                return false;
            }
            filterMacAddress.clear();
            for (int i = 0; i < count; i++) {
                View v = mBind.llMacAddress.getChildAt(i);
                EditText etMacAddress = v.findViewById(R.id.et_mac_address);
                final String macAddress = etMacAddress.getText().toString();
                if (TextUtils.isEmpty(macAddress)) {
                    ToastUtils.showToast(this, "Para Error");
                    return false;
                }
                int length = macAddress.length();
                if (length % 2 != 0) {
                    ToastUtils.showToast(this, "Para Error");
                    return false;
                }
                filterMacAddress.add(macAddress);
            }
        } else {
            filterMacAddress = new ArrayList<>();
        }
        return true;
    }
}
