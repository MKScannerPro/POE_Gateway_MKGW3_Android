package com.moko.mkremotegw.activity;


import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.RadioGroup;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkremotegw.AppConstants;
import com.moko.mkremotegw.R;
import com.moko.mkremotegw.adapter.MQTTFragmentAdapter;
import com.moko.mkremotegw.base.BaseActivity;
import com.moko.mkremotegw.databinding.ActivityMqttDeviceModifyRemoteBinding;
import com.moko.mkremotegw.dialog.AlertMessageDialog;
import com.moko.mkremotegw.entity.MQTTConfig;
import com.moko.mkremotegw.entity.MokoDevice;
import com.moko.mkremotegw.fragment.GeneralDeviceFragment;
import com.moko.mkremotegw.fragment.LWTFragment;
import com.moko.mkremotegw.fragment.SSLDeviceUrlFragment;
import com.moko.mkremotegw.fragment.UserDeviceFragment;
import com.moko.mkremotegw.utils.FileUtils;
import com.moko.mkremotegw.utils.SPUtiles;
import com.moko.mkremotegw.utils.ToastUtils;
import com.moko.mkremotegw.utils.Utils;
import com.moko.support.remotegw.MQTTConstants;
import com.moko.support.remotegw.MQTTSupport;
import com.moko.support.remotegw.entity.MsgConfigResult;
import com.moko.support.remotegw.entity.MsgNotify;
import com.moko.support.remotegw.entity.MsgReadResult;
import com.moko.support.remotegw.event.DeviceOnlineEvent;
import com.moko.support.remotegw.event.MQTTMessageArrivedEvent;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

public class ModifyMQTTSettingsActivity extends BaseActivity<ActivityMqttDeviceModifyRemoteBinding> implements RadioGroup.OnCheckedChangeListener {
    public static String TAG = ModifyMQTTSettingsActivity.class.getSimpleName();
    private final String FILTER_ASCII = "[ -~]*";


    private GeneralDeviceFragment generalFragment;
    private UserDeviceFragment userFragment;
    private SSLDeviceUrlFragment sslFragment;
    private LWTFragment lwtFragment;
    private MQTTFragmentAdapter adapter;
    private ArrayList<Fragment> fragments;

    private MQTTConfig mqttDeviceConfig;

    private MokoDevice mMokoDevice;
    private MQTTConfig appMqttConfig;
    private String mAppTopic;

    public Handler mHandler;

    public InputFilter filter;

    private String expertFilePath;
    private boolean isFileError;
    private boolean mIsConfigFinish;

    @Override
    protected void onCreate() {
        mqttDeviceConfig = new MQTTConfig();
        filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }

            return null;
        };
        mBind.etMqttHost.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        mBind.etMqttClientId.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        mBind.etMqttSubscribeTopic.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), filter});
        mBind.etMqttPublishTopic.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128), filter});
        createFragment();
        initData();
        adapter = new MQTTFragmentAdapter(this);
        adapter.setFragmentList(fragments);
        mBind.vpMqtt.setAdapter(adapter);
        mBind.vpMqtt.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    mBind.rbGeneral.setChecked(true);
                } else if (position == 1) {
                    mBind.rbUser.setChecked(true);
                } else if (position == 2) {
                    mBind.rbSsl.setChecked(true);
                } else if (position == 3) {
                    mBind.rbLwt.setChecked(true);
                }
            }
        });
        mBind.vpMqtt.setOffscreenPageLimit(4);
        mBind.rgMqtt.setOnCheckedChangeListener(this);
        expertFilePath = RemoteMainActivity.PATH_LOGCAT + File.separator + "export" + File.separator + "Settings for Device.xlsx";
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
        mBind.etMqttHost.postDelayed(() -> getMqttSettings(), 1000);
    }


    @Override
    protected ActivityMqttDeviceModifyRemoteBinding getViewBinding() {
        return ActivityMqttDeviceModifyRemoteBinding.inflate(getLayoutInflater());
    }

    private void createFragment() {
        fragments = new ArrayList<>();
        generalFragment = GeneralDeviceFragment.newInstance();
        userFragment = UserDeviceFragment.newInstance();
        sslFragment = SSLDeviceUrlFragment.newInstance();
        lwtFragment = LWTFragment.newInstance();
        fragments.add(generalFragment);
        fragments.add(userFragment);
        fragments.add(sslFragment);
        fragments.add(lwtFragment);
    }

    private void initData() {
        mBind.etMqttHost.setText(mqttDeviceConfig.host);
        mBind.etMqttPort.setText(mqttDeviceConfig.port);
        mBind.etMqttClientId.setText(mqttDeviceConfig.clientId);
        mBind.etMqttSubscribeTopic.setText(mqttDeviceConfig.topicSubscribe);
        mBind.etMqttPublishTopic.setText(mqttDeviceConfig.topicPublish);
        generalFragment.setCleanSession(mqttDeviceConfig.cleanSession);
        generalFragment.setQos(mqttDeviceConfig.qos);
        generalFragment.setKeepAlive(mqttDeviceConfig.keepAlive);
        userFragment.setUserName(mqttDeviceConfig.username);
        userFragment.setPassword(mqttDeviceConfig.password);
        sslFragment.setCAUrl(mqttDeviceConfig.caPath);
        sslFragment.setClientKeyUrl(mqttDeviceConfig.clientKeyPath);
        sslFragment.setClientCertUrl(mqttDeviceConfig.clientCertPath);
        sslFragment.setConnectMode(mqttDeviceConfig.connectMode);
        lwtFragment.setLwtEnable(mqttDeviceConfig.lwtEnable);
        lwtFragment.setLwtRetain(mqttDeviceConfig.lwtRetain);
        lwtFragment.setQos(mqttDeviceConfig.lwtQos);
        lwtFragment.setTopic(mqttDeviceConfig.lwtTopic);
        lwtFragment.setPayload(mqttDeviceConfig.lwtPayload);
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
        if (msg_id == MQTTConstants.READ_MSG_ID_DEVICE_STATUS) {
            Type type = new TypeToken<MsgNotify<JsonObject>>() {
            }.getType();
            MsgNotify<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            int status = result.data.get("status").getAsInt();
            if (status == 1) {
                ToastUtils.showToast(this, "Device is OTA, please wait");
                return;
            }
            mHandler.postDelayed(() -> {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "Set up failed");
            }, 30 * 1000);
            showLoadingProgressDialog();
            setMqttSettings();
        }
        if (msg_id == MQTTConstants.READ_MSG_ID_MQTT_SETTINGS) {
            Type type = new TypeToken<MsgReadResult<JsonObject>>() {
            }.getType();
            MsgReadResult<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            sslFragment.setConnectMode(result.data.get("security_type").getAsInt());
            mBind.etMqttHost.setText(result.data.get("host").getAsString());
            mBind.etMqttPort.setText(String.valueOf(result.data.get("port").getAsInt()));
            mBind.etMqttClientId.setText(result.data.get("client_id").getAsString());
            userFragment.setUserName(result.data.get("username").getAsString());
            userFragment.setPassword(result.data.get("passwd").getAsString());
            mBind.etMqttSubscribeTopic.setText(result.data.get("sub_topic").getAsString());
            mBind.etMqttPublishTopic.setText(result.data.get("pub_topic").getAsString());
            generalFragment.setQos(result.data.get("qos").getAsInt());
            generalFragment.setCleanSession(result.data.get("clean_session").getAsInt() == 1);
            generalFragment.setKeepAlive(result.data.get("keepalive").getAsInt());
            lwtFragment.setLwtEnable(result.data.get("lwt_en").getAsInt() == 1);
            lwtFragment.setQos(result.data.get("lwt_qos").getAsInt());
            lwtFragment.setLwtRetain(result.data.get("lwt_retain").getAsInt() == 1);
            lwtFragment.setTopic(result.data.get("lwt_topic").getAsString());
            lwtFragment.setPayload(result.data.get("lwt_payload").getAsString());
        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_MQTT_SETTINGS) {
            Type type = new TypeToken<MsgConfigResult>() {
            }.getType();
            MsgConfigResult result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            if (result.result_code == 0) {
                ToastUtils.showToast(this, "Set up succeed");
                mIsConfigFinish = true;
                mqttDeviceConfig.connectMode = sslFragment.getConnectMode();
                mqttDeviceConfig.host = mBind.etMqttHost.getText().toString();
                mqttDeviceConfig.port = mBind.etMqttPort.getText().toString();
                mqttDeviceConfig.clientId = mBind.etMqttClientId.getText().toString();
                mqttDeviceConfig.username = userFragment.getUsername();
                mqttDeviceConfig.password = userFragment.getPassword();
                mqttDeviceConfig.topicSubscribe = mBind.etMqttSubscribeTopic.getText().toString();
                mqttDeviceConfig.topicPublish = mBind.etMqttPublishTopic.getText().toString();
                mqttDeviceConfig.qos = generalFragment.getQos();
                mqttDeviceConfig.cleanSession = generalFragment.isCleanSession();
                mqttDeviceConfig.keepAlive = generalFragment.getKeepAlive();
                mqttDeviceConfig.lwtEnable = lwtFragment.getLwtEnable();
                mqttDeviceConfig.lwtQos = lwtFragment.getQos();
                mqttDeviceConfig.lwtRetain = lwtFragment.getLwtRetain();
                mqttDeviceConfig.lwtTopic = lwtFragment.getTopic();
                mqttDeviceConfig.lwtPayload = lwtFragment.getPayload();
                if (sslFragment.getConnectMode() < 2)
                    return;
                String caFileUrl = sslFragment.getCAUrl();
                String certFileUrl = sslFragment.getClientCertUrl();
                String keyFileUrl = sslFragment.getClientKeyUrl();
                // 若证书类型是CA certificate file且CA证书为空，不发送证书更新指令
                if (sslFragment.getConnectMode() == 2
                        && TextUtils.isEmpty(caFileUrl))
                    return;
                // 若证书类型是Self signed certificates且所有证书都为空，不发送证书更新指令
                if (sslFragment.getConnectMode() == 3
                        && TextUtils.isEmpty(caFileUrl)
                        && TextUtils.isEmpty(certFileUrl)
                        && TextUtils.isEmpty(keyFileUrl))
                    return;
                XLog.i("升级Mqtt证书");
                mHandler.postDelayed(() -> {
                    dismissLoadingProgressDialog();
                    finish();
                }, 50 * 1000);
                showLoadingProgressDialog();
                setMqttCertFile();
            } else {
                ToastUtils.showToast(this, "Set up failed");
            }
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_MQTT_CERT_RESULT) {
            Type type = new TypeToken<MsgNotify<JsonObject>>() {
            }.getType();
            MsgNotify<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDevice.mac.equalsIgnoreCase(result.device_info.mac))
                return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            int resultCode = result.data.get("result_code").getAsInt();
            if (resultCode == 1) {
                ToastUtils.showToast(this, R.string.update_success);
            } else {
                ToastUtils.showToast(this, R.string.update_failed);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceOnlineEvent(DeviceOnlineEvent event) {
        super.offline(event, mMokoDevice.mac);
    }

    public void onBack(View view) {
        back();
    }

    @Override
    public void onBackPressed() {
        back();
    }

    private void back() {
        if (mIsConfigFinish) {
            Intent intent = new Intent();
            intent.putExtra(AppConstants.EXTRA_KEY_MQTT_CONFIG_DEVICE, mqttDeviceConfig);
            setResult(RESULT_OK, intent);
        }
        finish();
    }

    public void onSave(View view) {
        if (isWindowLocked()) return;
        if (isParaError()) return;
        saveParams();
    }


    public void onSelectCertificate(View view) {
        if (isWindowLocked())
            return;
        sslFragment.selectCertificate();
    }


    private void saveParams() {
        if (!MQTTSupport.getInstance().isConnected()) {
            ToastUtils.showToast(this, R.string.network_error);
            return;
        }
        XLog.i("查询设备当前状态");
        mHandler.postDelayed(() -> {
            dismissLoadingProgressDialog();
            ToastUtils.showToast(this, "Set up failed");
        }, 50 * 1000);
        showLoadingProgressDialog();
        getDeviceStatus();
    }

    private void getDeviceStatus() {
        int msgId = MQTTConstants.READ_MSG_ID_DEVICE_STATUS;
        String message = assembleReadCommon(msgId, mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setMqttSettings() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_MQTT_SETTINGS;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("security_type", sslFragment.getConnectMode());
        jsonObject.addProperty("host", mBind.etMqttHost.getText().toString());
        jsonObject.addProperty("port", Integer.parseInt(mBind.etMqttPort.getText().toString()));
        jsonObject.addProperty("client_id", mBind.etMqttClientId.getText().toString());
        jsonObject.addProperty("username", userFragment.getUsername());
        jsonObject.addProperty("passwd", userFragment.getPassword());
        jsonObject.addProperty("sub_topic", mBind.etMqttSubscribeTopic.getText().toString());
        jsonObject.addProperty("pub_topic", mBind.etMqttPublishTopic.getText().toString());
        jsonObject.addProperty("qos", generalFragment.getQos());
        jsonObject.addProperty("clean_session", generalFragment.isCleanSession() ? 1 : 0);
        jsonObject.addProperty("keepalive", generalFragment.getKeepAlive());
        jsonObject.addProperty("lwt_en", lwtFragment.getLwtEnable() ? 1 : 0);
        jsonObject.addProperty("lwt_qos", lwtFragment.getQos());
        jsonObject.addProperty("lwt_retain", lwtFragment.getLwtRetain() ? 1 : 0);
        jsonObject.addProperty("lwt_topic", lwtFragment.getTopic());
        jsonObject.addProperty("lwt_payload", lwtFragment.getPayload());
        String message = assembleWriteCommonData(msgId, mMokoDevice.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setMqttCertFile() {
        String caFileUrl = sslFragment.getCAUrl();
        String certFileUrl = sslFragment.getClientCertUrl();
        String keyFileUrl = sslFragment.getClientKeyUrl();
        int msgId = MQTTConstants.CONFIG_MSG_ID_MQTT_CERT_FILE;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("ca_url", caFileUrl);
        jsonObject.addProperty("client_cert_url", certFileUrl);
        jsonObject.addProperty("client_key_url", keyFileUrl);
        String message = assembleWriteCommonData(msgId, mMokoDevice.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private boolean isParaError() {
        String host = mBind.etMqttHost.getText().toString().trim();
        String port = mBind.etMqttPort.getText().toString().trim();
        String clientId = mBind.etMqttClientId.getText().toString().trim();
        String topicSubscribe = mBind.etMqttSubscribeTopic.getText().toString().trim();
        String topicPublish = mBind.etMqttPublishTopic.getText().toString().trim();

        if (TextUtils.isEmpty(host)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_host));
            return true;
        }
        if (TextUtils.isEmpty(port)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_port_empty));
            return true;
        }
        if (Integer.parseInt(port) < 1 || Integer.parseInt(port) > 65535) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_port));
            return true;
        }
        if (TextUtils.isEmpty(clientId)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_client_id_empty));
            return true;
        }
        if (TextUtils.isEmpty(topicSubscribe)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_topic_subscribe));
            return true;
        }
        if (TextUtils.isEmpty(topicPublish)) {
            ToastUtils.showToast(this, getString(R.string.mqtt_verify_topic_publish));
            return true;
        }
        if (topicPublish.equals(topicSubscribe)) {
            ToastUtils.showToast(this, "Subscribed and published topic can't be same !");
            return true;
        }
        if (!generalFragment.isValid() || !lwtFragment.isValid())
            return true;
        return false;
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
        if (checkedId == R.id.rb_general) {
            mBind.vpMqtt.setCurrentItem(0);
        } else if (checkedId == R.id.rb_user) {
            mBind.vpMqtt.setCurrentItem(1);
        } else if (checkedId == R.id.rb_ssl) {
            mBind.vpMqtt.setCurrentItem(2);
        } else if (checkedId == R.id.rb_lwt) {
            mBind.vpMqtt.setCurrentItem(3);
        }
    }

    private void getMqttSettings() {
        int msgId = MQTTConstants.READ_MSG_ID_MQTT_SETTINGS;
        String message = assembleReadCommon(msgId, mMokoDevice.mac);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onExportSettings(View view) {
        if (isWindowLocked()) return;
        if (isParaError()) return;
        mqttDeviceConfig.host = mBind.etMqttHost.getText().toString().replaceAll(" ", "");
        mqttDeviceConfig.port = mBind.etMqttPort.getText().toString();
        mqttDeviceConfig.clientId = mBind.etMqttClientId.getText().toString().replaceAll(" ", "");
        mqttDeviceConfig.topicSubscribe = mBind.etMqttSubscribeTopic.getText().toString();
        mqttDeviceConfig.topicPublish = mBind.etMqttPublishTopic.getText().toString();
        mqttDeviceConfig.cleanSession = generalFragment.isCleanSession();
        mqttDeviceConfig.qos = generalFragment.getQos();
        mqttDeviceConfig.keepAlive = generalFragment.getKeepAlive();
        mqttDeviceConfig.username = userFragment.getUsername();
        mqttDeviceConfig.password = userFragment.getPassword();
        mqttDeviceConfig.connectMode = sslFragment.getConnectMode();
        mqttDeviceConfig.caPath = sslFragment.getCAUrl();
        mqttDeviceConfig.clientKeyPath = sslFragment.getClientKeyUrl();
        mqttDeviceConfig.clientCertPath = sslFragment.getClientCertUrl();
        mqttDeviceConfig.lwtEnable = lwtFragment.getLwtEnable();
        mqttDeviceConfig.lwtRetain = lwtFragment.getLwtRetain();
        mqttDeviceConfig.lwtQos = lwtFragment.getQos();
        mqttDeviceConfig.lwtTopic = lwtFragment.getTopic();
        mqttDeviceConfig.lwtPayload = lwtFragment.getPayload();
        showLoadingProgressDialog();
        final File expertFile = new File(expertFilePath);
        try {
            if (!expertFile.getParentFile().exists()) {
                expertFile.getParentFile().mkdirs();
            }
            if (!expertFile.exists()) {
                expertFile.delete();
                expertFile.createNewFile();
            }
            new Thread(() -> {
                XSSFWorkbook xssfWorkbook = new XSSFWorkbook();
                XSSFSheet sheet = xssfWorkbook.createSheet();
                XSSFRow row0 = sheet.createRow(0);
                row0.createCell(0).setCellValue("Config_Item");
                row0.createCell(1).setCellValue("Config_value");
                row0.createCell(2).setCellValue("Remark");

                XSSFRow row1 = sheet.createRow(1);
                row1.createCell(0).setCellValue("Host");
                if (!TextUtils.isEmpty(mqttDeviceConfig.host))
                    row1.createCell(1).setCellValue(String.format("value:%s", mqttDeviceConfig.host));
                row1.createCell(2).setCellValue("1-64 characters");

                XSSFRow row2 = sheet.createRow(2);
                row2.createCell(0).setCellValue("Port");
                if (!TextUtils.isEmpty(mqttDeviceConfig.port))
                    row2.createCell(1).setCellValue(String.format("value:%s", mqttDeviceConfig.port));
                row2.createCell(2).setCellValue("Range: 1-65535");

                XSSFRow row3 = sheet.createRow(3);
                row3.createCell(0).setCellValue("Client id");
                if (!TextUtils.isEmpty(mqttDeviceConfig.clientId))
                    row3.createCell(1).setCellValue(String.format("value:%s", mqttDeviceConfig.clientId));
                row3.createCell(2).setCellValue("1-64 characters");

                XSSFRow row4 = sheet.createRow(4);
                row4.createCell(0).setCellValue("Subscribe Topic");
                if (!TextUtils.isEmpty(mqttDeviceConfig.topicSubscribe))
                    row4.createCell(1).setCellValue(String.format("value:%s", mqttDeviceConfig.topicSubscribe));
//                else
//                    row4.createCell(1).setCellValue("");
                row4.createCell(2).setCellValue("1-128 characters");

                XSSFRow row5 = sheet.createRow(5);
                row5.createCell(0).setCellValue("Publish Topic");
                if (!TextUtils.isEmpty(mqttDeviceConfig.topicPublish))
                    row5.createCell(1).setCellValue(String.format("value:%s", mqttDeviceConfig.topicPublish));
//                else
//                    row5.createCell(1).setCellValue("");
                row5.createCell(2).setCellValue("1-128 characters");

                XSSFRow row6 = sheet.createRow(6);
                row6.createCell(0).setCellValue("Clean Session");
                row6.createCell(1).setCellValue(String.format("value:%s", mqttDeviceConfig.cleanSession ? "1" : "0"));
                row6.createCell(2).setCellValue("Range: 0/1 0:NO 1:YES");

                XSSFRow row7 = sheet.createRow(7);
                row7.createCell(0).setCellValue("Qos");
                row7.createCell(1).setCellValue(String.format("value:%d", mqttDeviceConfig.qos));
                row7.createCell(2).setCellValue("Range: 0/1/2 0:qos0 1:qos1 2:qos2");

                XSSFRow row8 = sheet.createRow(8);
                row8.createCell(0).setCellValue("Keep Alive");
                row8.createCell(1).setCellValue(String.format("value:%d", mqttDeviceConfig.keepAlive));
                row8.createCell(2).setCellValue("Range: 10-120, unit: second");

                XSSFRow row9 = sheet.createRow(9);
                row9.createCell(0).setCellValue("MQTT Username");
                if (!TextUtils.isEmpty(mqttDeviceConfig.username))
                    row9.createCell(1).setCellValue(String.format("value:%s", mqttDeviceConfig.username));
//                else
//                    row9.createCell(1).setCellValue("");
                row9.createCell(2).setCellValue("0-256 characters");

                XSSFRow row10 = sheet.createRow(10);
                row10.createCell(0).setCellValue("MQTT Password");
                if (!TextUtils.isEmpty(mqttDeviceConfig.password))
                    row10.createCell(1).setCellValue(String.format("value:%s", mqttDeviceConfig.password));
//                else
//                    row10.createCell(1).setCellValue("");
                row10.createCell(2).setCellValue("0-256 characters");

                XSSFRow row11 = sheet.createRow(11);
                row11.createCell(0).setCellValue("SSL/TLS");
                XSSFRow row12 = sheet.createRow(12);
                row12.createCell(0).setCellValue("Certificate type");
                if (mqttDeviceConfig.connectMode > 0) {
                    row11.createCell(1).setCellValue("value:1");
                    row12.createCell(1).setCellValue(String.format("value:%d", mqttDeviceConfig.connectMode));
                } else {
                    row11.createCell(1).setCellValue(String.format("value:%d", mqttDeviceConfig.connectMode));
                    row12.createCell(1).setCellValue("value:1");
                }
                row11.createCell(2).setCellValue("Range: 0/1 0:Disable SSL (TCP mode) 1:Enable SSL");
                row12.createCell(2).setCellValue("Valid when SSL is enabled, range: 1/2/3 1: CA certificate file 2: CA certificate file 3: Self signed certificates");

                XSSFRow row13 = sheet.createRow(13);
                row13.createCell(0).setCellValue("LWT");
                row13.createCell(1).setCellValue(mqttDeviceConfig.lwtEnable ? "value:1" : "value:0");
                row13.createCell(2).setCellValue("Range: 0/1 0:Disable 1:Enable");

                XSSFRow row14 = sheet.createRow(14);
                row14.createCell(0).setCellValue("LWT Retain");
                row14.createCell(1).setCellValue(mqttDeviceConfig.lwtRetain ? "value:1" : "value:0");
                row14.createCell(2).setCellValue("Range: 0/1 0:NO 1:YES");

                XSSFRow row15 = sheet.createRow(15);
                row15.createCell(0).setCellValue("LWT Qos");
                row15.createCell(1).setCellValue(String.format("value:%d", mqttDeviceConfig.lwtQos));
                row15.createCell(2).setCellValue("Range: 0/1/2 0:qos0 1:qos1 2:qos2");

                XSSFRow row16 = sheet.createRow(16);
                row16.createCell(0).setCellValue("LWT Topic");
                if (!TextUtils.isEmpty(mqttDeviceConfig.lwtTopic))
                    row16.createCell(1).setCellValue(String.format("value:%s", mqttDeviceConfig.lwtTopic));
//                else
//                    row16.createCell(1).setCellValue("");
                row16.createCell(2).setCellValue("1-128 characters (When LWT is enabled) ");

                XSSFRow row17 = sheet.createRow(17);
                row17.createCell(0).setCellValue("LWT Payload");
                if (!TextUtils.isEmpty(mqttDeviceConfig.lwtPayload))
                    row17.createCell(1).setCellValue(String.format("value:%s", mqttDeviceConfig.lwtPayload));
//                else
//                    row17.createCell(1).setCellValue("");
                row17.createCell(2).setCellValue("1-128 characters (When LWT is enabled) ");

                Uri uri = Uri.fromFile(expertFile);
                try {
                    OutputStream outputStream = getContentResolver().openOutputStream(uri);
                    xssfWorkbook.write(outputStream);
                } catch (Exception e) {
                    e.printStackTrace();
                    isFileError = true;
                }
                runOnUiThread(() -> {
                    dismissLoadingProgressDialog();
                    if (isFileError) {
                        isFileError = false;
                        ToastUtils.showToast(ModifyMQTTSettingsActivity.this, "Export error!");
                        return;
                    }
                    ToastUtils.showToast(ModifyMQTTSettingsActivity.this, "Export success!");
                    Utils.sendEmail(ModifyMQTTSettingsActivity.this, "", "", "Settings for Device", "Choose Email Client", expertFile);

                });
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            ToastUtils.showToast(this, "Export error!");
        }
    }

    public void onImportSettings(View view) {
        if (isWindowLocked())
            return;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "select file first!"), AppConstants.REQUEST_CODE_OPEN_DEVICE_SETTINGS_FILE);
        } catch (ActivityNotFoundException ex) {
            ToastUtils.showToast(this, "install file manager app");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppConstants.REQUEST_CODE_OPEN_DEVICE_SETTINGS_FILE) {
            if (resultCode == RESULT_OK) {
                //得到uri，后面就是将uri转化成file的过程。
                Uri uri = data.getData();
                String paramFilePath = FileUtils.getPath(this, uri);
                if (TextUtils.isEmpty(paramFilePath)) {
                    return;
                }
                if (!paramFilePath.endsWith(".xlsx")) {
                    ToastUtils.showToast(this, "Please select the correct file!");
                    return;
                }
                final File paramFile = new File(paramFilePath);
                if (paramFile.exists()) {
                    showLoadingProgressDialog();
                    new Thread(() -> {
                        try {
                            Workbook workbook = WorkbookFactory.create(paramFile);
                            Sheet sheet = workbook.getSheetAt(0);
                            int rows = sheet.getPhysicalNumberOfRows();
                            int columns = sheet.getRow(0).getPhysicalNumberOfCells();
                            // 从第二行开始
                            if (rows < 18 || columns < 3) {
                                runOnUiThread(() -> {
                                    dismissLoadingProgressDialog();
                                    ToastUtils.showToast(ModifyMQTTSettingsActivity.this, "Please select the correct file!");
                                });
                                return;
                            }
                            Cell hostCell = sheet.getRow(1).getCell(1);
                            if (hostCell != null)
                                mqttDeviceConfig.host = hostCell.getStringCellValue().replaceAll("value:", "");
                            Cell postCell = sheet.getRow(2).getCell(1);
                            if (postCell != null)
                                mqttDeviceConfig.port = postCell.getStringCellValue().replaceAll("value:", "");
                            Cell clientCell = sheet.getRow(3).getCell(1);
                            if (clientCell != null)
                                mqttDeviceConfig.clientId = clientCell.getStringCellValue().replaceAll("value:", "");
                            Cell topicSubscribeCell = sheet.getRow(4).getCell(1);
                            if (topicSubscribeCell != null) {
                                mqttDeviceConfig.topicSubscribe = topicSubscribeCell.getStringCellValue().replaceAll("value:", "");
                            }
                            Cell topicPublishCell = sheet.getRow(5).getCell(1);
                            if (topicPublishCell != null) {
                                mqttDeviceConfig.topicPublish = topicPublishCell.getStringCellValue().replaceAll("value:", "");
                            }
                            Cell cleanSessionCell = sheet.getRow(6).getCell(1);
                            if (cleanSessionCell != null)
                                mqttDeviceConfig.cleanSession = "1".equals(cleanSessionCell.getStringCellValue().replaceAll("value:", ""));
                            Cell qosCell = sheet.getRow(7).getCell(1);
                            if (qosCell != null)
                                mqttDeviceConfig.qos = Integer.parseInt(qosCell.getStringCellValue().replaceAll("value:", ""));
                            Cell keepAliveCell = sheet.getRow(8).getCell(1);
                            if (keepAliveCell != null)
                                mqttDeviceConfig.keepAlive = Integer.parseInt(keepAliveCell.getStringCellValue().replaceAll("value:", ""));
                            Cell usernameCell = sheet.getRow(9).getCell(1);
                            if (usernameCell != null) {
                                mqttDeviceConfig.username = usernameCell.getStringCellValue().replaceAll("value:", "");
                            }
                            Cell passwordCell = sheet.getRow(10).getCell(1);
                            if (passwordCell != null) {
                                mqttDeviceConfig.password = passwordCell.getStringCellValue().replaceAll("value:", "");
                            }
                            Cell connectModeCell = sheet.getRow(11).getCell(1);
                            if (connectModeCell != null) {
                                // 0/1
                                mqttDeviceConfig.connectMode = Integer.parseInt(connectModeCell.getStringCellValue().replaceAll("value:", ""));
                                if (mqttDeviceConfig.connectMode > 0) {
                                    Cell cell = sheet.getRow(12).getCell(1);
                                    if (cell != null)
                                        // 1/2/3
                                        mqttDeviceConfig.connectMode = Integer.parseInt(cell.getStringCellValue().replaceAll("value:", ""));
                                }
                            }
                            Cell lwtEnableCell = sheet.getRow(13).getCell(1);
                            if (lwtEnableCell != null)
                                mqttDeviceConfig.lwtEnable = "1".equals(lwtEnableCell.getStringCellValue().replaceAll("value:", ""));
                            Cell lwtRetainCell = sheet.getRow(14).getCell(1);
                            if (lwtRetainCell != null)
                                mqttDeviceConfig.lwtRetain = "1".equals(lwtRetainCell.getStringCellValue().replaceAll("value:", ""));
                            Cell lwtQosCell = sheet.getRow(15).getCell(1);
                            if (lwtQosCell != null)
                                mqttDeviceConfig.lwtQos = Integer.parseInt(lwtQosCell.getStringCellValue().replaceAll("value:", ""));
                            Cell topicCell = sheet.getRow(16).getCell(1);
                            if (topicCell != null) {
                                mqttDeviceConfig.lwtTopic = topicCell.getStringCellValue().replaceAll("value:", "");
                            }
                            Cell payloadCell = sheet.getRow(17).getCell(1);
                            if (payloadCell != null) {
                                mqttDeviceConfig.lwtPayload = payloadCell.getStringCellValue().replaceAll("value:", "");
                            }
                            runOnUiThread(() -> {
                                dismissLoadingProgressDialog();
                                if (isFileError) {
                                    ToastUtils.showToast(ModifyMQTTSettingsActivity.this, "Import failed!");
                                    return;
                                }
                                ToastUtils.showToast(ModifyMQTTSettingsActivity.this, "Import success!");
                                initData();
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            isFileError = true;
                        }
                    }).start();
                } else {
                    ToastUtils.showToast(this, "File is not exists!");
                }
            }
        }
    }

    public void onClearConfig(View view) {
        if (isWindowLocked()) return;
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setMessage("Please confirm whether to delete all configurations in this page?");
        dialog.setConfirm("YES");
        dialog.setCancel("NO");
        dialog.setOnAlertConfirmListener(() -> {
            mqttDeviceConfig = new MQTTConfig();
            mqttDeviceConfig.keepAlive = -1;
            sslFragment.setCAUrl("");
            sslFragment.setClientCertUrl("");
            sslFragment.setClientKeyUrl("");
            initData();
        });
        dialog.show(getSupportFragmentManager());
    }
}
