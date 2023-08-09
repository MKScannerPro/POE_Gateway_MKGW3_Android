package com.moko.mkremotegw.activity;


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
import com.moko.mkremotegw.AppConstants;
import com.moko.mkremotegw.R;
import com.moko.mkremotegw.base.BaseActivity;
import com.moko.mkremotegw.databinding.ActivityFilterOtherBinding;
import com.moko.mkremotegw.dialog.AlertMessageDialog;
import com.moko.mkremotegw.dialog.BottomDialog;
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

public class FilterOtherActivity extends BaseActivity<ActivityFilterOtherBinding> {

    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private String mAppTopic;

    public Handler mHandler;

    private List<JsonObject> filterOther;

    private ArrayList<String> mValues;
    private int mSelected;

    @Override
    protected void onCreate() {
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
        getFilterOther();
    }

    @Override
    protected ActivityFilterOtherBinding getViewBinding() {
        return ActivityFilterOtherBinding.inflate(getLayoutInflater());
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
        if (msg_id == MQTTConstants.READ_MSG_ID_FILTER_OTHER) {
            Type type = new TypeToken<MsgReadResult<JsonObject>>() {
            }.getType();
            MsgReadResult<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            mBind.cbOther.setChecked(result.data.get("switch_value").getAsInt() == 1);
            int relationship = result.data.get("relation").getAsInt();
            if (relationship < 1) {
                mValues = new ArrayList<>();
                mValues.add("A");
                mSelected = 0;
            } else if (relationship < 3) {
                mValues = new ArrayList<>();
                mValues.add("A & B");
                mValues.add("A | B");
                mSelected = relationship - 1;
            } else if (relationship < 6) {
                mValues = new ArrayList<>();
                mValues.add("A & B & C");
                mValues.add("(A & B) | C");
                mValues.add("A | B | C");
                mSelected = relationship - 3;
            }
            JsonArray ruleList = result.data.getAsJsonArray("rule");
            int number = ruleList.size();
            filterOther = new ArrayList<>();
            if (number > 0) {
                mBind.clOtherRelationship.setVisibility(View.VISIBLE);
                int index = 0;
                for (JsonElement jsonElement : ruleList) {
                    filterOther.add(jsonElement.getAsJsonObject());
                    View v = LayoutInflater.from(FilterOtherActivity.this).inflate(R.layout.item_other_filter_remote, mBind.llFilterCondition, false);
                    TextView tvCondition = v.findViewById(R.id.tv_condition);
                    EditText etDataType = v.findViewById(R.id.et_data_type);
                    EditText etMin = v.findViewById(R.id.et_min);
                    EditText etMax = v.findViewById(R.id.et_max);
                    EditText etRawData = v.findViewById(R.id.et_raw_data);
                    if (index == 0) {
                        tvCondition.setText("Condition A");
                    } else if (index == 1) {
                        tvCondition.setText("Condition B");
                    } else {
                        tvCondition.setText("Condition C");
                    }
                    String dataTypeStr = jsonElement.getAsJsonObject().get("type").getAsString();
                    int dataType = Integer.parseInt(dataTypeStr,16);
                    int start = jsonElement.getAsJsonObject().get("start").getAsInt();
                    int end = jsonElement.getAsJsonObject().get("end").getAsInt();
                    etDataType.setText(dataType == 0 ? "" : dataTypeStr);
                    etMin.setText(String.valueOf(start == 0 ? "" : start));
                    etMax.setText(String.valueOf(end == 0 ? "" : end));
                    etRawData.setText(jsonElement.getAsJsonObject().get("raw_data").getAsString());
                    mBind.llFilterCondition.addView(v);
                    index++;
                }
                mBind.tvOtherRelationship.setText(mValues.get(mSelected));
            } else {
                mBind.clOtherRelationship.setVisibility(View.GONE);
            }

        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_FILTER_OTHER) {
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

    private void getFilterOther() {
        int msgId = MQTTConstants.READ_MSG_ID_FILTER_OTHER;
        String message = assembleReadCommon(msgId, mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onSave(View view) {
        if (isWindowLocked())
            return;
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
        int count = mBind.llFilterCondition.getChildCount();
        if (count > 2) {
            ToastUtils.showToast(this, "You can set up to 3 filters!");
            return;
        }
        View v = LayoutInflater.from(this).inflate(R.layout.item_other_filter_remote, mBind.llFilterCondition, false);
        TextView tvCondition = v.findViewById(R.id.tv_condition);
        if (count == 0) {
            tvCondition.setText("Condition A");
        } else if (count == 1) {
            tvCondition.setText("Condition B");
        } else {
            tvCondition.setText("Condition C");
        }
        mBind.llFilterCondition.addView(v);
        mBind.clOtherRelationship.setVisibility(View.VISIBLE);
        mValues = new ArrayList<>();
        if (count == 0) {
            mValues.add("A");
            mSelected = 0;
        }
        if (count == 1) {
            mValues.add("A & B");
            mValues.add("A | B");
            mSelected = 1;
        }
        if (count == 2) {
            mValues.add("A & B & C");
            mValues.add("(A & B) | C");
            mValues.add("A | B | C");
            mSelected = 2;
        }
        mBind.tvOtherRelationship.setText(mValues.get(mSelected));
    }

    public void onDel(View view) {
        if (isWindowLocked())
            return;
        final int c = mBind.llFilterCondition.getChildCount();
        if (c == 0) {
            ToastUtils.showToast(this, "There are currently no filters to delete");
            return;
        }
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Warning");
        dialog.setMessage("Please confirm whether to delete it, if yes, the last option will be deleted!");
        dialog.setOnAlertConfirmListener(() -> {
            int count = mBind.llFilterCondition.getChildCount();
            if (count > 0) {
                mBind.llFilterCondition.removeViewAt(count - 1);
                mValues = new ArrayList<>();
                if (count == 1) {
                    mBind.clOtherRelationship.setVisibility(View.GONE);
                    return;
                }
                if (count == 2) {
                    mValues.add("A");
                    mSelected = 0;
                }
                if (count == 3) {
                    mValues.add("A & B");
                    mValues.add("A | B");
                    mSelected = 1;
                }
                mBind.tvOtherRelationship.setText(mValues.get(mSelected));
            }
        });
        dialog.show(getSupportFragmentManager());
    }


    private void saveParams() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_FILTER_OTHER;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("switch_value", mBind.cbOther.isChecked() ? 1 : 0);
        jsonObject.addProperty("relation", 0);
        if (filterOther.size() == 1) {
            jsonObject.addProperty("relation", 0);
        }
        if (filterOther.size() == 2) {
            jsonObject.addProperty("relation", mSelected + 1);
        }
        if (filterOther.size() == 3) {
            jsonObject.addProperty("relation", mSelected + 3);
        }
        JsonArray ruleList = new JsonArray();
        for (JsonObject object : filterOther) {
            ruleList.add(object);
        }
        jsonObject.add("rule", ruleList);
        String message = assembleWriteCommonData(msgId, mMokoDevice.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private boolean isValid() {
        final int count = mBind.llFilterCondition.getChildCount();
        if (count > 0) {
            // 发送设置的过滤RawData
            filterOther.clear();
            for (int i = 0; i < count; i++) {
                View v = mBind.llFilterCondition.getChildAt(i);
                EditText etDataType = v.findViewById(R.id.et_data_type);
                EditText etMin = v.findViewById(R.id.et_min);
                EditText etMax = v.findViewById(R.id.et_max);
                EditText etRawData = v.findViewById(R.id.et_raw_data);
                final String dataTypeStr = etDataType.getText().toString();
                final String minStr = etMin.getText().toString();
                final String maxStr = etMax.getText().toString();
                final String rawDataStr = etRawData.getText().toString();
                int dataType = 0;
                if (!TextUtils.isEmpty(dataTypeStr)) {
                    dataType = Integer.parseInt(dataTypeStr, 16);
                    if (dataType < 0 || dataType > 0xFF)
                        return false;
                }
                if (TextUtils.isEmpty(rawDataStr)) {
                    ToastUtils.showToast(this, "Para Error");
                    return false;
                }
                int length = rawDataStr.length();
                if (length % 2 != 0) {
                    ToastUtils.showToast(this, "Para Error");
                    return false;
                }
                int min = 0;
                if (!TextUtils.isEmpty(minStr)) {
                    min = Integer.parseInt(minStr);
                    if (min < 1 || min > 29) {
                        ToastUtils.showToast(this, "Range Error");
                        return false;
                    }
                }
                int max = 0;
                if (!TextUtils.isEmpty(maxStr)) {
                    max = Integer.parseInt(maxStr);
                    if (max < 1 || max > 29) {
                        ToastUtils.showToast(this, "Range Error");
                        return false;
                    }
                }
                if (min == 0 && max != 0) {
                    ToastUtils.showToast(this, "Para Error");
                    return false;
                }
                if (max < min) {
                    ToastUtils.showToast(this, "Para Error");
                    return false;
                }
                if (min > 0) {
                    int interval = max - min;
                    if (length != ((interval + 1) * 2)) {
                        ToastUtils.showToast(this, "Para Error");
                        return false;
                    }
                }
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("type", String.format("%02x", dataType));
                jsonObject.addProperty("start", min);
                jsonObject.addProperty("end", max);
                jsonObject.addProperty("raw_data", rawDataStr);
                filterOther.add(jsonObject);
            }
        } else {
            filterOther = new ArrayList<>();
        }
        return true;
    }

    public void onOtherRelationship(View view) {
        if (isWindowLocked())
            return;
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(mValues, mSelected);
        dialog.setListener(value -> {
            mSelected = value;
            mBind.tvOtherRelationship.setText(mValues.get(value));
        });
        dialog.show(getSupportFragmentManager());
    }
}
