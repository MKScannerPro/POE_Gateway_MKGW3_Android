package com.moko.mkgw3.activity.filter;


import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkgw3.AppConstants;
import com.moko.mkgw3.R;
import com.moko.mkgw3.base.BaseActivity;
import com.moko.mkgw3.databinding.ActivityFilterMkTofKgw3Binding;
import com.moko.lib.scannerui.dialog.AlertMessageDialog;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FilterMKTOFActivity extends BaseActivity<ActivityFilterMkTofKgw3Binding> {
    private MokoDeviceKgw3 mMokoDevice;
    private MQTTConfigKgw3 appMqttConfig;
    private String mAppTopic;
    public Handler mHandler;
    private List<String> filterCodeList;

    @Override
    protected void onCreate() {
        mMokoDevice = (MokoDeviceKgw3) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfigKgw3.class);
        mAppTopic = TextUtils.isEmpty(appMqttConfig.topicPublish) ? mMokoDevice.topicSubscribe : appMqttConfig.topicPublish;
        mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        showLoadingProgressDialog();
        getFilterTof();
    }

    @Override
    protected ActivityFilterMkTofKgw3Binding getViewBinding() {
        return ActivityFilterMkTofKgw3Binding.inflate(getLayoutInflater());
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
            XLog.e(e);
            return;
        }
        if (msg_id == MQTTConstants.READ_MSG_ID_FILTER_TOF) {
            Type type = new TypeToken<MsgReadResult<JsonObject>>() {
            }.getType();
            MsgReadResult<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac)) return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            mBind.cbMkTof.setChecked(result.data.get("switch_value").getAsInt() == 1);
            JsonArray codeList = result.data.getAsJsonArray("mfg_code");
            int number = codeList.size();
            filterCodeList = new ArrayList<>();
            if (number != 0) {
                int index = 1;
                for (JsonElement jsonElement : codeList) {
                    filterCodeList.add(jsonElement.getAsString());
                    String codeId = jsonElement.getAsString();
                    View v = LayoutInflater.from(FilterMKTOFActivity.this).inflate(R.layout.item_tof_filter_kgw3, mBind.llMfgCode, false);
                    TextView title = v.findViewById(R.id.tv_mfg_code_title);
                    EditText etCodeId = v.findViewById(R.id.et_mfg_code);
                    title.setText(String.format(Locale.getDefault(), "Code %d", index));
                    etCodeId.setText(codeId);
                    mBind.llMfgCode.addView(v);
                    index++;
                }
            }
        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_FILTER_TOF) {
            Type type = new TypeToken<MsgConfigResult<?>>() {
            }.getType();
            MsgConfigResult<?> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac)) return;
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
        super.offline(event, mMokoDevice.mac);
    }

    private void getFilterTof() {
        int msgId = MQTTConstants.READ_MSG_ID_FILTER_TOF;
        String message = assembleReadCommon(msgId, mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            XLog.e(e);
        }
    }

    public void onBack(View view) {
        finish();
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
        if (isWindowLocked()) return;
        int count = mBind.llMfgCode.getChildCount();
        if (count > 9) {
            ToastUtils.showToast(this, "You can set up to 10 filters!");
            return;
        }
        View v = LayoutInflater.from(this).inflate(R.layout.item_tof_filter_kgw3, mBind.llMfgCode, false);
        TextView title = v.findViewById(R.id.tv_mfg_code_title);
        title.setText(String.format(Locale.getDefault(), "Code %d", count + 1));
        mBind.llMfgCode.addView(v);
    }

    public void onDel(View view) {
        if (isWindowLocked()) return;
        final int c = mBind.llMfgCode.getChildCount();
        if (c == 0) {
            ToastUtils.showToast(this, "There are currently no filters to delete");
            return;
        }
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Warning");
        dialog.setMessage("Please confirm whether to delete it, if yes, the last option will be deleted!");
        dialog.setOnAlertConfirmListener(() -> {
            int count = mBind.llMfgCode.getChildCount();
            if (count > 0) {
                mBind.llMfgCode.removeViewAt(count - 1);
            }
        });
        dialog.show(getSupportFragmentManager());
    }

    private void saveParams() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_FILTER_TOF;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("switch_value", mBind.cbMkTof.isChecked() ? 1 : 0);
        JsonArray codeList = new JsonArray();
        for (String code : filterCodeList)
            codeList.add(code);
        jsonObject.add("mfg_code", codeList);
        String message = assembleWriteCommonData(msgId, mMokoDevice.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            XLog.e(e);
        }
    }

    private boolean isValid() {
        final int c = mBind.llMfgCode.getChildCount();
        if (c > 0) {
            // 发送设置的过滤RawData
            int count = mBind.llMfgCode.getChildCount();
            if (count == 0) {
                ToastUtils.showToast(this, "Para Error");
                return false;
            }
            filterCodeList.clear();
            for (int i = 0; i < count; i++) {
                View v = mBind.llMfgCode.getChildAt(i);
                EditText etCode = v.findViewById(R.id.et_mfg_code);
                final String code = etCode.getText().toString();
                if (TextUtils.isEmpty(code)) {
                    ToastUtils.showToast(this, "Para Error");
                    return false;
                }
                int length = code.length();
                if (length != 4) {
                    ToastUtils.showToast(this, "Para Error");
                    return false;
                }
                filterCodeList.add(code);
            }
        } else {
            filterCodeList = new ArrayList<>();
        }
        return true;
    }
}
