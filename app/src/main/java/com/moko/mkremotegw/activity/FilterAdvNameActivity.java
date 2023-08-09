package com.moko.mkremotegw.activity;


import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.Spanned;
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
import com.moko.mkremotegw.AppConstants;
import com.moko.mkremotegw.R;
import com.moko.mkremotegw.base.BaseActivity;
import com.moko.mkremotegw.databinding.ActivityFilterAdvNameBinding;
import com.moko.mkremotegw.dialog.AlertMessageDialog;
import com.moko.mkremotegw.entity.MQTTConfig;
import com.moko.mkremotegw.entity.MokoDevice;
import com.moko.mkremotegw.utils.SPUtiles;
import com.moko.mkremotegw.utils.ToastUtils;
import com.moko.support.remotegw.MQTTConstants;
import com.moko.support.remotegw.MQTTSupport;
import com.moko.support.remotegw.entity.MsgConfigResult;
import com.moko.support.remotegw.entity.MsgReadResult;
import com.moko.support.remotegw.event.DeviceOnlineEvent;
import com.moko.support.remotegw.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FilterAdvNameActivity extends BaseActivity<ActivityFilterAdvNameBinding> {

    private final String FILTER_ASCII = "[ -~]*";

    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private String mAppTopic;

    public Handler mHandler;

    private List<String> filterAdvName;
    private InputFilter filter;

    @Override
    protected void onCreate() {
        filter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (!(source + "").matches(FILTER_ASCII)) {
                    return "";
                }

                return null;
            }
        };
        mMokoDevice = (MokoDevice) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfig.class);
        mAppTopic = TextUtils.isEmpty(appMqttConfig.topicPublish) ? mMokoDevice.topicSubscribe : appMqttConfig.topicPublish;
        mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            finish();
        }, 30 * 1000);
        showLoadingProgressDialog();
        getFilterAdvName();
    }

    @Override
    protected ActivityFilterAdvNameBinding getViewBinding() {
        return ActivityFilterAdvNameBinding.inflate(getLayoutInflater());
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
        if (msg_id == MQTTConstants.READ_MSG_ID_FILTER_ADV_NAME) {
            Type type = new TypeToken<MsgReadResult<JsonObject>>() {
            }.getType();
            MsgReadResult<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            mBind.cbPreciseMatch.setChecked(result.data.get("precise").getAsInt() == 1);
            mBind.cbReverseFilter.setChecked(result.data.get("reverse").getAsInt() == 1);
            JsonArray macList = result.data.getAsJsonArray("name");
            int number = macList.size();
            filterAdvName = new ArrayList<>();
            if (number != 0) {
                int index = 1;
                for (JsonElement jsonElement : macList) {
                    filterAdvName.add(jsonElement.getAsString());
                    String advName = jsonElement.getAsString();
                    View v = LayoutInflater.from(FilterAdvNameActivity.this).inflate(R.layout.item_adv_name_filter, mBind.llDavName, false);
                    TextView title = v.findViewById(R.id.tv_adv_name_title);
                    EditText etAdvName = v.findViewById(R.id.et_adv_name);
                    etAdvName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20), filter});
                    title.setText(String.format("ADV Name%d", index));
                    etAdvName.setText(advName);
                    mBind.llDavName.addView(v);
                    index++;
                }
            }
        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_FILTER_ADV_NAME) {
            Type type = new TypeToken<MsgConfigResult>() {
            }.getType();
            MsgConfigResult result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac))
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
        super.offline(event, mMokoDevice.mac);
    }

    public void back(View view) {
        finish();
    }

    private void getFilterAdvName() {
        int msgId = MQTTConstants.READ_MSG_ID_FILTER_ADV_NAME;
        String message = assembleReadCommon(msgId, mMokoDevice.mac);
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
        int count = mBind.llDavName.getChildCount();
        if (count > 9) {
            ToastUtils.showToast(this, "You can set up to 10 filters!");
            return;
        }
        View v = LayoutInflater.from(this).inflate(R.layout.item_adv_name_filter, mBind.llDavName, false);
        TextView title = v.findViewById(R.id.tv_adv_name_title);
        title.setText(String.format("ADV Name%d", count + 1));
        EditText etAdvName = v.findViewById(R.id.et_adv_name);
        etAdvName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20), filter});
        mBind.llDavName.addView(v);
    }

    public void onDel(View view) {
        if (isWindowLocked())
            return;
        final int c = mBind.llDavName.getChildCount();
        if (c == 0) {
            ToastUtils.showToast(this, "There are currently no filters to delete");
            return;
        }
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Warning");
        dialog.setMessage("Please confirm whether to delete it, if yes, the last option will be deleted!");
        dialog.setOnAlertConfirmListener(() -> {
            int count = mBind.llDavName.getChildCount();
            if (count > 0) {
                mBind.llDavName.removeViewAt(count - 1);
            }
        });
        dialog.show(getSupportFragmentManager());
    }


    private void saveParams() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_FILTER_ADV_NAME;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("precise", mBind.cbPreciseMatch.isChecked() ? 1 : 0);
        jsonObject.addProperty("reverse", mBind.cbReverseFilter.isChecked() ? 1 : 0);
        JsonArray macList = new JsonArray();
        for (String mac : filterAdvName)
            macList.add(mac);
        jsonObject.add("name", macList);
        String message = assembleWriteCommonData(msgId, mMokoDevice.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private boolean isValid() {
        final int c = mBind.llDavName.getChildCount();
        if (c > 0) {
            // 发送设置的过滤RawData
            int count = mBind.llDavName.getChildCount();
            if (count == 0) {
                ToastUtils.showToast(this, "Para Error");
                return false;
            }
            filterAdvName.clear();
            for (int i = 0; i < count; i++) {
                View v = mBind.llDavName.getChildAt(i);
                EditText etAdvName = v.findViewById(R.id.et_adv_name);
                final String advName = etAdvName.getText().toString();
                if (TextUtils.isEmpty(advName)) {
                    ToastUtils.showToast(this, "Para Error");
                    return false;
                }
                filterAdvName.add(advName);
            }
        } else {
            filterAdvName = new ArrayList<>();
        }
        return true;
    }
}
