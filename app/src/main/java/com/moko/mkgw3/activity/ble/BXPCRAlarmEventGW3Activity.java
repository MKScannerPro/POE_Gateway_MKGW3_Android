package com.moko.mkgw3.activity.ble;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkgw3.AppConstants;
import com.moko.mkgw3.R;
import com.moko.mkgw3.activity.MKGW3MainActivity;
import com.moko.mkgw3.adapter.AlarmEventDataAdapter;
import com.moko.mkgw3.base.BaseActivity;
import com.moko.mkgw3.databinding.ActivityAlarmEventDataKgw3Binding;
import com.moko.mkgw3.entity.AlarmEventData;
import com.moko.mkgw3.entity.MQTTConfigKgw3;
import com.moko.mkgw3.entity.MokoDeviceKgw3;
import com.moko.mkgw3.utils.SPUtiles;
import com.moko.mkgw3.utils.ToastUtils;
import com.moko.mkgw3.utils.Utils;
import com.moko.support.mkgw3.MQTTConstants;
import com.moko.support.mkgw3.MQTTSupport;
import com.moko.support.mkgw3.entity.AccData;
import com.moko.support.mkgw3.entity.MsgNotify;
import com.moko.support.mkgw3.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BXPCRAlarmEventGW3Activity extends BaseActivity<ActivityAlarmEventDataKgw3Binding> {
    private boolean isSync;
    private MokoDeviceKgw3 mMokoDeviceKgw3;
    private MQTTConfigKgw3 appMqttConfig;
    private String mAppTopic;
    private Handler mHandler;
    private String mac;
    private Animation animation;
    private final List<AlarmEventData> dataList = new ArrayList<>();
    private AlarmEventDataAdapter adapter;
    private final StringBuilder exportStr = new StringBuilder();
    private final String title = "alarm_event_data";
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private int mSyncCount;

    @Override
    protected ActivityAlarmEventDataKgw3Binding getViewBinding() {
        return ActivityAlarmEventDataKgw3Binding.inflate(getLayoutInflater());
    }

    @Override
    protected void onCreate() {
        mMokoDeviceKgw3 = (MokoDeviceKgw3) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        mac = getIntent().getStringExtra(AppConstants.EXTRA_KEY_MAC);
        String mqttConfigAppStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        appMqttConfig = new Gson().fromJson(mqttConfigAppStr, MQTTConfigKgw3.class);
        mAppTopic = TextUtils.isEmpty(appMqttConfig.topicPublish) ? mMokoDeviceKgw3.topicSubscribe : appMqttConfig.topicPublish;
        mHandler = new Handler(Looper.getMainLooper());
        animation = AnimationUtils.loadAnimation(this, R.anim.rotate_refresh);
        adapter = new AlarmEventDataAdapter();
        mBind.rvList.setAdapter(adapter);
        mBind.tvExport.setEnabled(false);
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
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_B_CR_ALARM_EVENT_ENABLE) {
            Type type = new TypeToken<MsgNotify<JsonObject>>() {
            }.getType();
            MsgNotify<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            int code = result.data.get("result_code").getAsInt();
            if (code == 0) {
                mSyncCount++;
            }
            if (mSyncCount == 3) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
                ToastUtils.showToast(this, "Setup succeed！");
                if (code == 0) {
                    if (!isSync) {
                        isSync = true;
                        dataList.clear();
                        adapter.replaceData(dataList);
                        mBind.ivSync.startAnimation(animation);
                        mBind.tvSync.setText("Stop");
                    } else {
                        isSync = false;
                        mBind.ivSync.clearAnimation();
                        mBind.tvSync.setText("Sync");
                    }
                }
            }
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_BXP_B_CR_ALARM_EVENT_DATA) {
            //历史温湿度数据
            Type type = new TypeToken<MsgNotify<AlarmEventData>>() {
            }.getType();
            MsgNotify<AlarmEventData> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            AlarmEventData data = result.data;
            dataList.add(0, data);
            adapter.replaceData(dataList);
            mBind.tvExport.setEnabled(true);
            String mode;
            if (data.alarmType == 0)
                mode = "Single press mode";
            else if (data.alarmType == 1)
                mode = "Double press mode";
            else
                mode = "Long press mode";
            exportStr.insert(0, "\n" + sdf.format(data.timeStamp) + "\t" + mode);
        }
    }

    public void onSync(View view) {
        if (isWindowLocked()) return;
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 30 * 1000);
        showLoadingProgressDialog();
        changeNotifyStatus(!isSync ? 1 : 0);
    }

    public void onExport(View view) {
        if (isWindowLocked()) return;
        showLoadingProgressDialog();
        writeTrackedFile("");
        mBind.tvExport.postDelayed(() -> {
            dismissLoadingProgressDialog();
            final String log = exportStr.toString();
            if (!TextUtils.isEmpty(log)) {
                writeTrackedFile(log);
                File file = getTrackedFile();
                // 发送邮件
                String address = "Development@mokotechnology.com";
                Utils.sendEmail(this, address, title, title, "Choose Email Client", file);
            }
        }, 500);
    }

    private void writeTrackedFile(String thLog) {
        File file = new File(MKGW3MainActivity.PATH_LOGCAT + File.separator + "alarmEventData.txt");
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(thLog);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getTrackedFile() {
        File file = new File(MKGW3MainActivity.PATH_LOGCAT + File.separator + "alarmEventData.txt");
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    private void changeNotifyStatus(int status) {
        mSyncCount = 0;
        int msgId = MQTTConstants.CONFIG_MSG_ID_BLE_BXP_B_CR_ALARM_EVENT_ENABLE;
        JsonObject jsonObjectSingle = new JsonObject();
        jsonObjectSingle.addProperty("mac", mac);
        jsonObjectSingle.addProperty("type", 0);
        jsonObjectSingle.addProperty("switch_value", status);
        String messageSingle = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObjectSingle);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, messageSingle, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
        JsonObject jsonObjectDouble = new JsonObject();
        jsonObjectDouble.addProperty("mac", mac);
        jsonObjectDouble.addProperty("type", 1);
        jsonObjectDouble.addProperty("switch_value", status);
        String messageDouble = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObjectDouble);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, messageDouble, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
        JsonObject jsonObjectLong = new JsonObject();
        jsonObjectLong.addProperty("mac", mac);
        jsonObjectLong.addProperty("type", 2);
        jsonObjectLong.addProperty("switch_value", status);
        String messageLong = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObjectLong);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, messageLong, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onBack(View view) {
        back();
    }

    @Override
    public void onBackPressed() {
        back();
    }

    private void back() {
        EventBus.getDefault().unregister(this);
        if (isSync) changeNotifyStatus(0);
        finish();
    }
}
