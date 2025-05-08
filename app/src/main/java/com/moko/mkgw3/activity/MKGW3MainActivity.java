package com.moko.mkgw3.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.HttpHeaders;
import com.lzy.okgo.model.Response;
import com.lzy.okgo.request.base.Request;
import com.moko.lib.mqtt.MQTTSupport;
import com.moko.lib.mqtt.entity.MsgNotify;
import com.moko.lib.mqtt.event.DeviceDeletedEvent;
import com.moko.lib.mqtt.event.DeviceModifyNameEvent;
import com.moko.lib.mqtt.event.DeviceOnlineEvent;
import com.moko.lib.mqtt.event.MQTTConnectionCompleteEvent;
import com.moko.lib.mqtt.event.MQTTConnectionFailureEvent;
import com.moko.lib.mqtt.event.MQTTConnectionLostEvent;
import com.moko.lib.mqtt.event.MQTTMessageArrivedEvent;
import com.moko.lib.mqtt.event.MQTTUnSubscribeFailureEvent;
import com.moko.lib.mqtt.event.MQTTUnSubscribeSuccessEvent;
import com.moko.lib.scanneriot.IoTDMConstants;
import com.moko.lib.scanneriot.Urls;
import com.moko.lib.scanneriot.activity.SyncDeviceActivity;
import com.moko.lib.scanneriot.dialog.LoginDialog;
import com.moko.lib.scanneriot.entity.CommonResp;
import com.moko.lib.scanneriot.entity.SyncDevice;
import com.moko.lib.scanneriot.utils.IoTDMSPUtils;
import com.moko.lib.scannerui.dialog.AlertMessageDialog;
import com.moko.lib.scannerui.utils.ToastUtils;
import com.moko.mkgw3.AppConstants;
import com.moko.mkgw3.BuildConfig;
import com.moko.mkgw3.R;
import com.moko.mkgw3.activity.add.DeviceScannerKgw3Activity;
import com.moko.mkgw3.activity.modify.ModifySettingsKgw3Activity;
import com.moko.mkgw3.adapter.DeviceKgw3Adapter;
import com.moko.mkgw3.base.BaseActivity;
import com.moko.mkgw3.databinding.ActivityMainMkgw3Binding;
import com.moko.mkgw3.db.MKgw3DBTools;
import com.moko.mkgw3.entity.LoginEntity;
import com.moko.mkgw3.entity.MQTTConfigKgw3;
import com.moko.mkgw3.entity.MokoDeviceKgw3;
import com.moko.mkgw3.utils.SPUtiles;
import com.moko.mkgw3.utils.Utils;
import com.moko.support.mkgw3.MQTTConstants;
import com.moko.support.mkgw3.MokoSupport;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import okhttp3.RequestBody;

public class MKGW3MainActivity extends BaseActivity<ActivityMainMkgw3Binding> implements BaseQuickAdapter.OnItemClickListener, BaseQuickAdapter.OnItemLongClickListener {
    private ArrayList<MokoDeviceKgw3> devices;
    private DeviceKgw3Adapter adapter;
    public Handler mHandler;
    public String mAppMqttConfigStr;
    private MQTTConfigKgw3 mAppMqttConfig;
    public static String PATH_LOGCAT;

    @Override
    protected void onCreate() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            // 优先保存到SD卡中
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                PATH_LOGCAT = getExternalFilesDir(null).getAbsolutePath() + File.separator + (BuildConfig.IS_LIBRARY ? "MKScannerPro" : "MKGW3");
            } else {
                PATH_LOGCAT = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + (BuildConfig.IS_LIBRARY ? "MKScannerPro" : "MKGW3");
            }
        } else {
            // 如果SD卡不存在，就保存到本应用的目录下
            PATH_LOGCAT = getFilesDir().getAbsolutePath() + File.separator + (BuildConfig.IS_LIBRARY ? "MKScannerPro" : "MKGW3");
        }
        if (!BuildConfig.IS_LIBRARY) {
            StringBuffer buffer = new StringBuffer();
            // 记录机型
            buffer.append("机型：");
            buffer.append(android.os.Build.MODEL);
            buffer.append("=====");
            // 记录版本号
            buffer.append("手机系统版本：");
            buffer.append(android.os.Build.VERSION.RELEASE);
            buffer.append("=====");
            // 记录APP版本
            buffer.append("APP版本：");
            buffer.append(Utils.getVersionInfo(this));
            XLog.d(buffer.toString());
        }
        MokoSupport.getInstance().init(getApplicationContext());
        MQTTSupport.getInstance().init(getApplicationContext());
        devices = MKgw3DBTools.getInstance(this).selectAllDevice();
        adapter = new DeviceKgw3Adapter();
        adapter.openLoadAnimation();
        adapter.replaceData(devices);
        adapter.setOnItemClickListener(this);
        adapter.setOnItemLongClickListener(this);
        mBind.rvDeviceList.setLayoutManager(new LinearLayoutManager(this));
        mBind.rvDeviceList.setAdapter(adapter);
        if (devices.isEmpty()) {
            mBind.rlEmpty.setVisibility(View.VISIBLE);
            mBind.rvDeviceList.setVisibility(View.GONE);
        } else {
            mBind.rvDeviceList.setVisibility(View.VISIBLE);
            mBind.rlEmpty.setVisibility(View.GONE);
        }
        mHandler = new Handler(Looper.getMainLooper());
        mAppMqttConfigStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        if (!TextUtils.isEmpty(mAppMqttConfigStr)) {
            mAppMqttConfig = new Gson().fromJson(mAppMqttConfigStr, MQTTConfigKgw3.class);
            mBind.tvTitle.setText(getString(R.string.mqtt_connecting));
        }
        try {
            MQTTSupport.getInstance().connectMqtt(mAppMqttConfigStr);
        } catch (FileNotFoundException e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ToastUtils.showToast(this, "Please select your SSL certificates again, otherwise the APP can't use normally.");
                startActivityForResult(new Intent(this, SetAppMQTTKgw3Activity.class), AppConstants.REQUEST_CODE_MQTT_CONFIG_APP);
            }
            // 读取stacktrace信息
            final Writer result = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(result);
            e.printStackTrace(printWriter);
            StringBuffer errorReport = new StringBuffer();
            errorReport.append(result.toString());
            XLog.e(errorReport.toString());
        }
        if (!BuildConfig.IS_LIBRARY) {
            mBind.tvTitle.setOnClickListener(v -> {
                if (isWindowLocked()) return;
                // 关于
                startActivity(new Intent(this, AboutActivity.class));
            });
        }
    }

    @Override
    protected ActivityMainMkgw3Binding getViewBinding() {
        return ActivityMainMkgw3Binding.inflate(getLayoutInflater());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTConnectionCompleteEvent(MQTTConnectionCompleteEvent event) {
        mBind.tvTitle.setText("MKScannerPro");
        // 订阅所有设备的Topic
        subscribeAllDevices();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTConnectionLostEvent(MQTTConnectionLostEvent event) {
        mBind.tvTitle.setText(getString(R.string.mqtt_connecting));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTConnectionFailureEvent(MQTTConnectionFailureEvent event) {
        mBind.tvTitle.setText(getString(R.string.mqtt_connect_failed));
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 100)
    public void onMQTTMessageArrivedEvent(MQTTMessageArrivedEvent event) {
        runOnUiThread(() -> {
            // 更新所有设备的网络状态
            updateDeviceNetworkStatus(event);
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTUnSubscribeSuccessEvent(MQTTUnSubscribeSuccessEvent event) {
        dismissLoadingProgressDialog();
        mHandler.removeMessages(0);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTUnSubscribeFailureEvent(MQTTUnSubscribeFailureEvent event) {
        dismissLoadingProgressDialog();
        mHandler.removeMessages(0);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceModifyNameEvent(DeviceModifyNameEvent event) {
        // 修改了设备名称
        if (!devices.isEmpty()) {
            for (MokoDeviceKgw3 device : devices) {
                if (device.mac.equals(event.getMac())) {
                    device.name = MKgw3DBTools.getInstance(this).selectDevice(device.mac).name;
                    break;
                }
            }
        }
        adapter.replaceData(devices);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceDeletedEvent(DeviceDeletedEvent event) {
        // 删除了设备
        int id = event.getId();
        if (id > 0 && mHandler.hasMessages(id)) {
            mHandler.removeMessages(id);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        XLog.i("onNewIntent...");
        setIntent(intent);
        if (getIntent().getExtras() != null) {
            String from = getIntent().getStringExtra(AppConstants.EXTRA_KEY_FROM_ACTIVITY);
            String mac = getIntent().getStringExtra(AppConstants.EXTRA_KEY_MAC);
            if (ModifyNameKgw3Activity.TAG.equals(from)
                    || DeviceSettingKgw3Activity.TAG.equals(from)) {
                devices.clear();
                devices.addAll(MKgw3DBTools.getInstance(this).selectAllDevice());
                if (!TextUtils.isEmpty(mac)) {
                    for (final MokoDeviceKgw3 device : devices) {
                        if (mac.equals(device.mac)) {
                            device.isOnline = true;
                            if (mHandler.hasMessages(device.id)) {
                                mHandler.removeMessages(device.id);
                            }
                            Message message = Message.obtain(mHandler, () -> {
                                device.isOnline = false;
                                XLog.i(device.mac + "离线");
                                adapter.replaceData(devices);
                            });
                            message.what = device.id;
                            mHandler.sendMessageDelayed(message, 60 * 1000);
                            break;
                        }
                    }
                }
                adapter.replaceData(devices);
                if (!devices.isEmpty()) {
                    mBind.rvDeviceList.setVisibility(View.VISIBLE);
                    mBind.rlEmpty.setVisibility(View.GONE);
                } else {
                    mBind.rvDeviceList.setVisibility(View.GONE);
                    mBind.rlEmpty.setVisibility(View.VISIBLE);
                }
            }
            if (ModifySettingsKgw3Activity.TAG.equals(from)) {
                if (!TextUtils.isEmpty(mac)) {
                    MokoDeviceKgw3 mokoDeviceKgw3 = MKgw3DBTools.getInstance(this).selectDevice(mac);
                    for (final MokoDeviceKgw3 device : devices) {
                        if (mac.equals(device.mac)) {
                            if (TextUtils.isEmpty(mAppMqttConfig.topicSubscribe)) {
                                try {
                                    if (!device.topicPublish.equals(mokoDeviceKgw3.topicPublish)) {
                                        // 取消订阅旧主题
                                        MQTTSupport.getInstance().unSubscribe(device.topicPublish);
                                        // 订阅新主题
                                        MQTTSupport.getInstance().subscribe(mokoDeviceKgw3.topicPublish, mAppMqttConfig.qos);
                                    }
                                    if (device.lwtEnable == 1
                                            && !TextUtils.isEmpty(device.lwtTopic)
                                            && !device.lwtTopic.equals(mokoDeviceKgw3.topicPublish)) {
                                        // 取消订阅旧遗愿主题
                                        MQTTSupport.getInstance().unSubscribe(device.lwtTopic);
                                        // 订阅新遗愿主题
                                        MQTTSupport.getInstance().subscribe(mokoDeviceKgw3.lwtTopic, mAppMqttConfig.qos);

                                    }
                                } catch (MqttException e) {
                                    e.printStackTrace();
                                }
                            }
                            device.mqttInfo = mokoDeviceKgw3.mqttInfo;
                            device.topicPublish = mokoDeviceKgw3.topicPublish;
                            device.topicSubscribe = mokoDeviceKgw3.topicSubscribe;
                            device.lwtEnable = mokoDeviceKgw3.lwtEnable;
                            device.lwtTopic = mokoDeviceKgw3.lwtTopic;
                            device.networkType = mokoDeviceKgw3.networkType;
                            break;
                        }
                    }
                }
                adapter.replaceData(devices);
            }
        }
    }

    public void setAppMQTTConfig(View view) {
        if (isWindowLocked()) return;
        startActivityForResult(new Intent(this, SetAppMQTTKgw3Activity.class), AppConstants.REQUEST_CODE_MQTT_CONFIG_APP);
    }

    public void mainAddDevices(View view) {
        if (isWindowLocked()) return;
        if (TextUtils.isEmpty(mAppMqttConfigStr)) {
            startActivityForResult(new Intent(this, SetAppMQTTKgw3Activity.class), AppConstants.REQUEST_CODE_MQTT_CONFIG_APP);
            return;
        }
        if (Utils.isNetworkAvailable(this)) {
            MQTTConfigKgw3 MQTTAppConfig = new Gson().fromJson(mAppMqttConfigStr, MQTTConfigKgw3.class);
            if (TextUtils.isEmpty(MQTTAppConfig.host)) {
                startActivityForResult(new Intent(this, SetAppMQTTKgw3Activity.class), AppConstants.REQUEST_CODE_MQTT_CONFIG_APP);
                return;
            }
            startActivity(new Intent(this, DeviceScannerKgw3Activity.class));
        } else {
            String ssid = Utils.getWifiSSID(this);
            ToastUtils.showToast(this, String.format("SSID:%s, the network cannot available,please check", ssid));
            XLog.i(String.format("SSID:%s, the network cannot available,please check", ssid));
        }
    }

    public void mainSyncDevices(View view) {
        if (isWindowLocked()) return;
        if (devices.isEmpty()) {
            ToastUtils.showToast(this, "Add devices first");
            return;
        }
        // 登录
        String account = IoTDMSPUtils.getStringValue(this, IoTDMConstants.EXTRA_KEY_LOGIN_ACCOUNT, "");
        String password = IoTDMSPUtils.getStringValue(this, IoTDMConstants.EXTRA_KEY_LOGIN_PASSWORD, "");
        int env = IoTDMSPUtils.getIntValue(this, IoTDMConstants.EXTRA_KEY_LOGIN_ENV, 0);
        if (TextUtils.isEmpty(account) || TextUtils.isEmpty(password)) {
            LoginDialog dialog = new LoginDialog();
            dialog.setOnLoginClicked(this::login);
            dialog.show(getSupportFragmentManager());
            return;
        }
        login(account, password, env);
    }

    private void login(String account, String password, int envValue) {
        LoginEntity entity = new LoginEntity();
        entity.username = account;
        entity.password = password;
        entity.source = 1;
        if (envValue == 0)
            Urls.setCloudEnv(getApplicationContext());
        else
            Urls.setTestEnv(getApplicationContext());
        RequestBody body = RequestBody.create(Urls.JSON, new Gson().toJson(entity));
        OkGo.<String>post(Urls.loginApi(getApplicationContext()))
                .upRequestBody(body)
                .execute(new StringCallback() {

                    @Override
                    public void onStart(Request<String, ? extends Request> request) {
                        showLoadingProgressDialog();
                    }

                    @Override
                    public void onSuccess(Response<String> response) {
                        Type type = new TypeToken<CommonResp<JsonObject>>() {
                        }.getType();
                        CommonResp<JsonObject> commonResp = new Gson().fromJson(response.body(), type);
                        if (commonResp.code != 200) {
                            ToastUtils.showToast(MKGW3MainActivity.this, commonResp.msg);
                            LoginDialog dialog = new LoginDialog();
                            dialog.setOnLoginClicked((account1, password1, env) -> login(account1, password1, env));
                            dialog.show(getSupportFragmentManager());
                            return;
                        }
                        // add header
                        String accessToken = commonResp.data.get("access_token").getAsString();
                        HttpHeaders headers = new HttpHeaders();
                        headers.put("Authorization", accessToken);
                        OkGo.getInstance().addCommonHeaders(headers);

                        IoTDMSPUtils.setStringValue(MKGW3MainActivity.this, IoTDMConstants.EXTRA_KEY_LOGIN_ACCOUNT, account);
                        IoTDMSPUtils.setStringValue(MKGW3MainActivity.this, IoTDMConstants.EXTRA_KEY_LOGIN_PASSWORD, password);
                        IoTDMSPUtils.setIntValue(MKGW3MainActivity.this, IoTDMConstants.EXTRA_KEY_LOGIN_ENV, envValue);
                        Intent intent = new Intent(MKGW3MainActivity.this, SyncDeviceActivity.class);
                        ArrayList<SyncDevice> syncDevices = new ArrayList<>();
                        for (MokoDeviceKgw3 device : devices) {
                            SyncDevice syncDevice = new SyncDevice();
                            syncDevice.mac = device.mac;
                            syncDevice.macName = device.name;
                            syncDevice.publishTopic = device.topicPublish;
                            syncDevice.subscribeTopic = device.topicSubscribe;
                            syncDevice.lastWill = device.lwtTopic;
                            syncDevice.model = "70";
                            syncDevices.add(syncDevice);
                        }
                        intent.putExtra(IoTDMConstants.EXTRA_KEY_SYNC_DEVICES, syncDevices);
                        startActivity(intent);
                    }

                    @Override
                    public void onError(Response<String> response) {
                        ToastUtils.showToast(MKGW3MainActivity.this, R.string.request_error);
                        LoginDialog dialog = new LoginDialog();
                        dialog.setOnLoginClicked((account12, password12, env) -> login(account12, password12, env));
                        dialog.show(getSupportFragmentManager());
                    }

                    @Override
                    public void onFinish() {
                        dismissLoadingProgressDialog();
                    }
                });
    }

    @Override
    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
        MokoDeviceKgw3 mokoDeviceKgw3 = (MokoDeviceKgw3) adapter.getItem(position);
        if (mokoDeviceKgw3 == null)
            return;
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        if (!mokoDeviceKgw3.isOnline) {
            ToastUtils.showToast(this, R.string.device_offline);
            return;
        }
        Intent i = new Intent(MKGW3MainActivity.this, DeviceDetailKgw3Activity.class);
        i.putExtra(AppConstants.EXTRA_KEY_DEVICE, mokoDeviceKgw3);
        startActivity(i);
    }

    @Override
    public boolean onItemLongClick(BaseQuickAdapter adapter, View view, int position) {
        MokoDeviceKgw3 mokoDeviceKgw3 = (MokoDeviceKgw3) adapter.getItem(position);
        if (mokoDeviceKgw3 == null) return true;
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Remove Device");
        dialog.setMessage("Please confirm again whether to \n remove the device");
        dialog.setOnAlertConfirmListener(() -> {
            if (!MQTTSupport.getInstance().isConnected()) {
                ToastUtils.showToast(MKGW3MainActivity.this, R.string.network_error);
                return;
            }
            showLoadingProgressDialog();
            mHandler.postDelayed(this::dismissLoadingProgressDialog, 5000);
            // 取消订阅
            if (TextUtils.isEmpty(mAppMqttConfig.topicSubscribe)) {
                try {
                    MQTTSupport.getInstance().unSubscribe(mokoDeviceKgw3.topicPublish);
                    if (mokoDeviceKgw3.lwtEnable == 1
                            && !TextUtils.isEmpty(mokoDeviceKgw3.lwtTopic)
                            && !mokoDeviceKgw3.lwtTopic.equals(mokoDeviceKgw3.topicPublish))
                        MQTTSupport.getInstance().unSubscribe(mokoDeviceKgw3.lwtTopic);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
            XLog.i(String.format("删除设备:%s", mokoDeviceKgw3.name));
            MKgw3DBTools.getInstance(MKGW3MainActivity.this).deleteDevice(mokoDeviceKgw3);
            EventBus.getDefault().post(new DeviceDeletedEvent(mokoDeviceKgw3.id));
            devices.remove(mokoDeviceKgw3);
            adapter.replaceData(devices);
            if (devices.isEmpty()) {
                mBind.rlEmpty.setVisibility(View.VISIBLE);
                mBind.rvDeviceList.setVisibility(View.GONE);
            }
        });
        dialog.show(getSupportFragmentManager());
        return true;
    }

    private void subscribeAllDevices() {
        if (!TextUtils.isEmpty(mAppMqttConfig.topicSubscribe)) {
            try {
                MQTTSupport.getInstance().subscribe(mAppMqttConfig.topicSubscribe, mAppMqttConfig.qos);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            if (devices.isEmpty())
                return;
            for (MokoDeviceKgw3 device : devices) {
                try {
                    // 订阅设备发布主题
                    if (TextUtils.isEmpty(mAppMqttConfig.topicSubscribe))
                        MQTTSupport.getInstance().subscribe(device.topicPublish, mAppMqttConfig.qos);
                    // 订阅遗愿主题
                    if (device.lwtEnable == 1
                            && !TextUtils.isEmpty(device.lwtTopic)
                            && !device.lwtTopic.equals(device.topicPublish))
                        MQTTSupport.getInstance().subscribe(device.lwtTopic, mAppMqttConfig.qos);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void updateDeviceNetworkStatus(MQTTMessageArrivedEvent event) {
        if (devices.isEmpty()) return;
        final String topic = event.getTopic();
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
        // 收到任何信息都认为在线，除了遗愿信息
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_BLE_SCAN_RESULT && isDurationVoid()) return;
        Type type = new TypeToken<MsgNotify<Object>>() {
        }.getType();
        MsgNotify<Object> msgNotify = new Gson().fromJson(message, type);
        final String mac = msgNotify.device_info.mac;
        for (final MokoDeviceKgw3 device : devices) {
            if (device.mac.equals(mac)) {
                if ((msg_id == MQTTConstants.NOTIFY_MSG_ID_OFFLINE
                        || msg_id == MQTTConstants.NOTIFY_MSG_ID_BUTTON_RESET) && device.isOnline) {
                    // 收到遗愿信息或者按键重置，设备离线
                    device.isOnline = false;
                    if (mHandler.hasMessages(device.id)) {
                        mHandler.removeMessages(device.id);
                    }
                    XLog.i(device.mac + "离线");
                    adapter.replaceData(devices);
                    EventBus.getDefault().post(new DeviceOnlineEvent(mac, false));
                    break;
                }
                if (msg_id == MQTTConstants.NOTIFY_MSG_ID_NETWORKING_STATUS) {
                    Type netType = new TypeToken<MsgNotify<JsonObject>>() {
                    }.getType();
                    MsgNotify<JsonObject> netMsgNotify = new Gson().fromJson(message, netType);
                    device.wifiRssi = netMsgNotify.data.get("wifi_rssi").getAsInt();
                    device.networkType = netMsgNotify.data.get("net_interface").getAsInt();
                }
                device.isOnline = true;
                if (mHandler.hasMessages(device.id)) {
                    mHandler.removeMessages(device.id);
                }
                Message offline = Message.obtain(mHandler, () -> {
                    device.isOnline = false;
                    XLog.i(device.mac + "离线");
                    adapter.replaceData(devices);
                    EventBus.getDefault().post(new DeviceOnlineEvent(mac, false));
                });
                offline.what = device.id;
                mHandler.sendMessageDelayed(offline, 62 * 1000);
                adapter.replaceData(devices);
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK)
            return;
        if (requestCode == AppConstants.REQUEST_CODE_MQTT_CONFIG_APP) {
            mAppMqttConfigStr = data.getStringExtra(AppConstants.EXTRA_KEY_MQTT_CONFIG_APP);
            mAppMqttConfig = new Gson().fromJson(mAppMqttConfigStr, MQTTConfigKgw3.class);
            mBind.tvTitle.setText("MKScannerPro");
            // 订阅所有设备的Topic
            subscribeAllDevices();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MQTTSupport.getInstance().disconnectMqtt();
        if (devices != null && !devices.isEmpty()) {
            for (final MokoDeviceKgw3 device : devices) {
                if (mHandler.hasMessages(device.id)) {
                    mHandler.removeMessages(device.id);
                }
            }
        }
    }

    // 记录上次收到信息的时间,屏蔽无效事件
    protected long mLastMessageTime = 0;

    public boolean isDurationVoid() {
        long current = SystemClock.elapsedRealtime();
        if (current - mLastMessageTime > 500) {
            mLastMessageTime = current;
            return false;
        } else {
            return true;
        }
    }

    public void onBack(View view) {
        if (isWindowLocked()) return;
        back();
    }

    @Override
    public void onBackPressed() {
        if (isWindowLocked()) return;
        back();
    }

    private void back() {
        if (BuildConfig.IS_LIBRARY) {
            finish();
        } else {
            AlertMessageDialog dialog = new AlertMessageDialog();
            dialog.setMessage(R.string.main_exit_tips);
            dialog.setOnAlertConfirmListener(() -> finish());
            dialog.show(getSupportFragmentManager());
        }
    }
}
