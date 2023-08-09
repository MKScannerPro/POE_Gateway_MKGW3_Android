package com.moko.mkremotegw.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.RadioGroup;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mkremotegw.AppConstants;
import com.moko.mkremotegw.R;
import com.moko.mkremotegw.adapter.MQTTFragmentAdapter;
import com.moko.mkremotegw.base.BaseActivity;
import com.moko.mkremotegw.databinding.ActivityMqttDeviceRemoteBinding;
import com.moko.mkremotegw.dialog.AlertMessageDialog;
import com.moko.mkremotegw.entity.MQTTConfig;
import com.moko.mkremotegw.fragment.GeneralDeviceFragment;
import com.moko.mkremotegw.fragment.LWTFragment;
import com.moko.mkremotegw.fragment.SSLDeviceFragment;
import com.moko.mkremotegw.fragment.UserDeviceFragment;
import com.moko.mkremotegw.utils.FileUtils;
import com.moko.mkremotegw.utils.ToastUtils;
import com.moko.mkremotegw.utils.Utils;
import com.moko.support.remotegw.MokoSupport;
import com.moko.support.remotegw.OrderTaskAssembler;
import com.moko.support.remotegw.entity.OrderCHAR;
import com.moko.support.remotegw.entity.ParamsKeyEnum;
import com.moko.support.remotegw.entity.ParamsLongKeyEnum;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

public class MqttSettingsActivity extends BaseActivity<ActivityMqttDeviceRemoteBinding> implements RadioGroup.OnCheckedChangeListener {
    private final String FILTER_ASCII = "[ -~]*";

    private GeneralDeviceFragment generalFragment;
    private UserDeviceFragment userFragment;
    private SSLDeviceFragment sslFragment;
    private LWTFragment lwtFragment;
    private MQTTFragmentAdapter adapter;
    private ArrayList<Fragment> fragments;

    private MQTTConfig mqttDeviceConfig;

    private boolean mSavedParamsError;
    private boolean mIsSaved;

    private InputFilter filter;

    private String expertFilePath;
    private boolean isFileError;
    private String mStaMac;
    private String mDeviceName;


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
        showLoadingProgressDialog();
        mBind.title.postDelayed(() -> {
            ArrayList<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.getDeviceName());
            orderTasks.add(OrderTaskAssembler.getWifiMac());
            orderTasks.add(OrderTaskAssembler.getMQTTConnectMode());
            orderTasks.add(OrderTaskAssembler.getMQTTHost());
            orderTasks.add(OrderTaskAssembler.getMQTTPort());
            orderTasks.add(OrderTaskAssembler.getMQTTCleanSession());
            orderTasks.add(OrderTaskAssembler.getMQTTKeepAlive());
            orderTasks.add(OrderTaskAssembler.getMQTTQos());
            orderTasks.add(OrderTaskAssembler.getMQTTClientId());
            orderTasks.add(OrderTaskAssembler.getMQTTSubscribeTopic());
            orderTasks.add(OrderTaskAssembler.getMQTTPublishTopic());
            orderTasks.add(OrderTaskAssembler.getMQTTUsername());
            orderTasks.add(OrderTaskAssembler.getMQTTPassword());
            orderTasks.add(OrderTaskAssembler.getMQTTLwtEnable());
            orderTasks.add(OrderTaskAssembler.getMQTTLwtRetain());
            orderTasks.add(OrderTaskAssembler.getMQTTLwtQos());
            orderTasks.add(OrderTaskAssembler.getMQTTLwtTopic());
            orderTasks.add(OrderTaskAssembler.getMQTTLwtPayload());
            MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        }, 500);

    }

    @Override
    protected ActivityMqttDeviceRemoteBinding getViewBinding() {
        return ActivityMqttDeviceRemoteBinding.inflate(getLayoutInflater());
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 100)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        String action = event.getAction();
        if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
            runOnUiThread(() -> {
                dismissLoadingProgressDialog();
                finish();
            });
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        final String action = event.getAction();
        if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
            dismissLoadingProgressDialog();
        }
        if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
            OrderTaskResponse response = event.getResponse();
            OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
            int responseType = response.responseType;
            byte[] value = response.responseValue;
            switch (orderCHAR) {
                case CHAR_PARAMS:
                    if (value.length >= 4) {
                        int header = value[0] & 0xFF;// 0xED
                        int flag = value[1] & 0xFF;// read or write
                        int cmd = value[2] & 0xFF;
                        if (header == 0xEE) {
                            ParamsLongKeyEnum configKeyEnum = ParamsLongKeyEnum.fromParamKey(cmd);
                            if (configKeyEnum == null) {
                                return;
                            }
                            if (flag == 0x01) {
                                // write
                                int result = value[4] & 0xFF;
                                switch (configKeyEnum) {
                                    case KEY_MQTT_USERNAME:
                                    case KEY_MQTT_PASSWORD:
                                    case KEY_MQTT_CLIENT_KEY:
                                    case KEY_MQTT_CLIENT_CERT:
                                        if (result != 1) {
                                            mSavedParamsError = true;
                                        }
                                        break;
                                    case KEY_MQTT_CA:
                                        if (mSavedParamsError) {
                                            ToastUtils.showToast(this, "Setup failed！");
                                        } else {
                                            mIsSaved = true;
                                            ToastUtils.showToast(this, "Setup succeed！");
                                        }
                                        break;
                                }
                            }
                            if (flag == 0x00) {
                                int length = MokoUtils.toInt(Arrays.copyOfRange(value, 3, 5));
                                // read
                                switch (configKeyEnum) {
                                    case KEY_MQTT_USERNAME:
                                        mqttDeviceConfig.username = new String(Arrays.copyOfRange(value, 5, 5 + length));
                                        userFragment.setUserName(mqttDeviceConfig.username);
                                        break;
                                    case KEY_MQTT_PASSWORD:
                                        mqttDeviceConfig.password = new String(Arrays.copyOfRange(value, 5, 5 + length));
                                        userFragment.setPassword(mqttDeviceConfig.password);
                                        break;
                                }
                            }
                        }
                        if (header == 0xED) {
                            ParamsKeyEnum configKeyEnum = ParamsKeyEnum.fromParamKey(cmd);
                            if (configKeyEnum == null) {
                                return;
                            }
                            int length = value[3] & 0xFF;
                            if (flag == 0x01) {
                                // write
                                int result = value[4] & 0xFF;
                                switch (configKeyEnum) {
                                    case KEY_MQTT_HOST:
                                    case KEY_MQTT_PORT:
                                    case KEY_MQTT_CLIENT_ID:
                                    case KEY_MQTT_SUBSCRIBE_TOPIC:
                                    case KEY_MQTT_PUBLISH_TOPIC:
                                    case KEY_MQTT_CLEAN_SESSION:
                                    case KEY_MQTT_QOS:
                                    case KEY_MQTT_KEEP_ALIVE:
                                    case KEY_MQTT_LWT_ENABLE:
                                    case KEY_MQTT_LWT_RETAIN:
                                    case KEY_MQTT_LWT_QOS:
                                    case KEY_MQTT_LWT_TOPIC:
                                    case KEY_MQTT_LWT_PAYLOAD:
                                        if (result != 1) {
                                            mSavedParamsError = true;
                                        }
                                        break;
                                    case KEY_MQTT_CONNECT_MODE:
                                        if (result != 1) {
                                            mSavedParamsError = true;
                                        }
                                        if (mSavedParamsError) {
                                            ToastUtils.showToast(this, "Setup failed！");
                                        } else {
                                            mIsSaved = true;
                                            ToastUtils.showToast(this, "Setup succeed！");
                                        }
                                        break;
                                }
                            }
                            if (flag == 0x00) {
                                if (length == 0)
                                    return;
                                // read
                                switch (configKeyEnum) {
                                    case KEY_MQTT_CONNECT_MODE:
                                        mqttDeviceConfig.connectMode = value[4];
                                        sslFragment.setConnectMode(mqttDeviceConfig.connectMode);
                                        break;
                                    case KEY_MQTT_HOST:
                                        mqttDeviceConfig.host = new String(Arrays.copyOfRange(value, 4, 4 + length));
                                        mBind.etMqttHost.setText(mqttDeviceConfig.host);
                                        break;
                                    case KEY_MQTT_PORT:
                                        mqttDeviceConfig.port = String.valueOf(MokoUtils.toInt(Arrays.copyOfRange(value, 4, 4 + length)));
                                        mBind.etMqttPort.setText(mqttDeviceConfig.port);
                                        break;
                                    case KEY_MQTT_CLEAN_SESSION:
                                        mqttDeviceConfig.cleanSession = value[4] == 1;
                                        generalFragment.setCleanSession(mqttDeviceConfig.cleanSession);
                                        break;
                                    case KEY_MQTT_KEEP_ALIVE:
                                        mqttDeviceConfig.keepAlive = value[4] & 0xFF;
                                        generalFragment.setKeepAlive(mqttDeviceConfig.keepAlive);
                                        break;
                                    case KEY_MQTT_QOS:
                                        mqttDeviceConfig.qos = value[4] & 0xFF;
                                        generalFragment.setQos(mqttDeviceConfig.qos);
                                        break;
                                    case KEY_MQTT_CLIENT_ID:
                                        mqttDeviceConfig.clientId = new String(Arrays.copyOfRange(value, 4, 4 + length));
                                        mBind.etMqttClientId.setText(mqttDeviceConfig.clientId);
                                        break;
                                    case KEY_MQTT_SUBSCRIBE_TOPIC:
                                        mqttDeviceConfig.topicSubscribe = new String(Arrays.copyOfRange(value, 4, 4 + length));
                                        mBind.etMqttSubscribeTopic.setText(mqttDeviceConfig.topicSubscribe);
                                        break;
                                    case KEY_MQTT_PUBLISH_TOPIC:
                                        mqttDeviceConfig.topicPublish = new String(Arrays.copyOfRange(value, 4, 4 + length));
                                        mBind.etMqttPublishTopic.setText(mqttDeviceConfig.topicPublish);
                                        break;
                                    case KEY_MQTT_LWT_ENABLE:
                                        mqttDeviceConfig.lwtEnable = value[4] == 1;
                                        lwtFragment.setLwtEnable(mqttDeviceConfig.lwtEnable);
                                        break;
                                    case KEY_MQTT_LWT_QOS:
                                        mqttDeviceConfig.qos = value[4];
                                        lwtFragment.setQos(mqttDeviceConfig.qos);
                                        break;
                                    case KEY_MQTT_LWT_RETAIN:
                                        mqttDeviceConfig.lwtRetain = value[4] == 1;
                                        lwtFragment.setLwtRetain(mqttDeviceConfig.lwtRetain);
                                        break;
                                    case KEY_MQTT_LWT_TOPIC:
                                        mqttDeviceConfig.lwtTopic = new String(Arrays.copyOfRange(value, 4, 4 + length));
                                        lwtFragment.setTopic(mqttDeviceConfig.lwtTopic);
                                        break;
                                    case KEY_MQTT_LWT_PAYLOAD:
                                        mqttDeviceConfig.lwtPayload = new String(Arrays.copyOfRange(value, 4, 4 + length));
                                        lwtFragment.setPayload(mqttDeviceConfig.lwtPayload);
                                        break;
                                    case KEY_DEVICE_NAME:
                                        String name = new String(Arrays.copyOfRange(value, 4, 4 + length));
                                        mqttDeviceConfig.deviceName = name;
                                        mDeviceName = name;
                                        break;
                                    case KEY_WIFI_MAC:
                                        String mac = MokoUtils.bytesToHexString(Arrays.copyOfRange(value, 4, 4 + length));
                                        mqttDeviceConfig.staMac = mac;
                                        mStaMac = mac;
                                        break;
                                }
                            }
                        }
                    }
                    break;
            }
        }
    }

    private void createFragment() {
        fragments = new ArrayList<>();
        generalFragment = GeneralDeviceFragment.newInstance();
        userFragment = UserDeviceFragment.newInstance();
        sslFragment = SSLDeviceFragment.newInstance();
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
        sslFragment.setCAPath(mqttDeviceConfig.caPath);
        sslFragment.setClientKeyPath(mqttDeviceConfig.clientKeyPath);
        sslFragment.setClientCertPath(mqttDeviceConfig.clientCertPath);
        sslFragment.setConnectMode(mqttDeviceConfig.connectMode);
        lwtFragment.setLwtEnable(mqttDeviceConfig.lwtEnable);
        lwtFragment.setLwtRetain(mqttDeviceConfig.lwtRetain);
        lwtFragment.setQos(mqttDeviceConfig.lwtQos);
        lwtFragment.setTopic(mqttDeviceConfig.lwtTopic);
        lwtFragment.setPayload(mqttDeviceConfig.lwtPayload);
    }

    public void onBack(View view) {
        back();
    }

    @Override
    public void onBackPressed() {
        back();
    }

    private void back() {
        if (mIsSaved) {
            Intent intent = new Intent();
            intent.putExtra(AppConstants.EXTRA_KEY_MQTT_CONFIG_DEVICE, mqttDeviceConfig);
            setResult(RESULT_OK, intent);
        }
        finish();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
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

    public void onSave(View view) {
        if (isWindowLocked()) return;
        if (isParaError()) return;
        setMQTTDeviceConfig();
    }

    private boolean isParaError() {
        String host = mBind.etMqttHost.getText().toString().replaceAll(" ", "");
        String port = mBind.etMqttPort.getText().toString();
        String clientId = mBind.etMqttClientId.getText().toString().replaceAll(" ", "");
        String topicSubscribe = mBind.etMqttSubscribeTopic.getText().toString();
        String topicPublish = mBind.etMqttPublishTopic.getText().toString();

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
        if (!generalFragment.isValid() || !sslFragment.isValid() || !lwtFragment.isValid())
            return true;
        mqttDeviceConfig.host = host;
        mqttDeviceConfig.port = port;
        mqttDeviceConfig.clientId = clientId;
        mqttDeviceConfig.cleanSession = generalFragment.isCleanSession();
        mqttDeviceConfig.qos = generalFragment.getQos();
        mqttDeviceConfig.keepAlive = generalFragment.getKeepAlive();
        mqttDeviceConfig.topicSubscribe = topicSubscribe;
        mqttDeviceConfig.topicPublish = topicPublish;
        mqttDeviceConfig.username = userFragment.getUsername();
        mqttDeviceConfig.password = userFragment.getPassword();
        mqttDeviceConfig.connectMode = sslFragment.getConnectMode();
        mqttDeviceConfig.caPath = sslFragment.getCaPath();
        mqttDeviceConfig.clientKeyPath = sslFragment.getClientKeyPath();
        mqttDeviceConfig.clientCertPath = sslFragment.getClientCertPath();
        mqttDeviceConfig.lwtEnable = lwtFragment.getLwtEnable();
        if (mqttDeviceConfig.lwtEnable) {
            mqttDeviceConfig.lwtQos = lwtFragment.getQos();
            mqttDeviceConfig.lwtRetain = lwtFragment.getLwtRetain();
            mqttDeviceConfig.lwtTopic = lwtFragment.getTopic();
            mqttDeviceConfig.lwtPayload = lwtFragment.getPayload();
        }
        return false;
    }

    private void setMQTTDeviceConfig() {
        try {
            showLoadingProgressDialog();
            ArrayList<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.setMqttHost(mqttDeviceConfig.host));
            orderTasks.add(OrderTaskAssembler.setMqttPort(Integer.parseInt(mqttDeviceConfig.port)));
            orderTasks.add(OrderTaskAssembler.setMqttClientId(mqttDeviceConfig.clientId));
            orderTasks.add(OrderTaskAssembler.setMqttCleanSession(mqttDeviceConfig.cleanSession ? 1 : 0));
            orderTasks.add(OrderTaskAssembler.setMqttQos(mqttDeviceConfig.qos));
            orderTasks.add(OrderTaskAssembler.setMqttKeepAlive(mqttDeviceConfig.keepAlive));
            orderTasks.add(OrderTaskAssembler.setMqttPublishTopic(mqttDeviceConfig.topicPublish));
            orderTasks.add(OrderTaskAssembler.setMqttSubscribeTopic(mqttDeviceConfig.topicSubscribe));
            orderTasks.add(OrderTaskAssembler.setMqttLwtEnable(mqttDeviceConfig.lwtEnable ? 1 : 0));
            if (mqttDeviceConfig.lwtEnable) {
                orderTasks.add(OrderTaskAssembler.setMqttLwtQos(mqttDeviceConfig.lwtQos));
                orderTasks.add(OrderTaskAssembler.setMqttLwtRetain(mqttDeviceConfig.lwtRetain ? 1 : 0));
                orderTasks.add(OrderTaskAssembler.setMqttLwtTopic(mqttDeviceConfig.lwtTopic));
                orderTasks.add(OrderTaskAssembler.setMqttLwtPayload(mqttDeviceConfig.lwtPayload));
            }
            orderTasks.add(OrderTaskAssembler.setMqttUserName(mqttDeviceConfig.username));
            orderTasks.add(OrderTaskAssembler.setMqttPassword(mqttDeviceConfig.password));
            orderTasks.add(OrderTaskAssembler.setMqttConnectMode(mqttDeviceConfig.connectMode));
            if (mqttDeviceConfig.connectMode == 2) {
                File file = new File(mqttDeviceConfig.caPath);
                orderTasks.add(OrderTaskAssembler.setCA(file));
            } else if (mqttDeviceConfig.connectMode == 3) {
                File clientKeyFile = new File(mqttDeviceConfig.clientKeyPath);
                orderTasks.add(OrderTaskAssembler.setClientKey(clientKeyFile));
                File clientCertFile = new File(mqttDeviceConfig.clientCertPath);
                orderTasks.add(OrderTaskAssembler.setClientCert(clientCertFile));
                File caFile = new File(mqttDeviceConfig.caPath);
                orderTasks.add(OrderTaskAssembler.setCA(caFile));
            }
            MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        } catch (Exception e) {
            ToastUtils.showToast(this, "File is missing");
        }
    }

    public void selectCertificate(View view) {
        if (isWindowLocked())
            return;
        sslFragment.selectCertificate();
    }

    public void selectCAFile(View view) {
        if (isWindowLocked())
            return;
        sslFragment.selectCAFile();
    }

    public void selectKeyFile(View view) {
        if (isWindowLocked())
            return;
        sslFragment.selectKeyFile();
    }

    public void selectCertFile(View view) {
        if (isWindowLocked())
            return;
        sslFragment.selectCertFile();
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
        mqttDeviceConfig.caPath = sslFragment.getCaPath();
        mqttDeviceConfig.clientKeyPath = sslFragment.getClientKeyPath();
        mqttDeviceConfig.clientCertPath = sslFragment.getClientCertPath();
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
                        ToastUtils.showToast(MqttSettingsActivity.this, "Export error!");
                        return;
                    }
                    ToastUtils.showToast(MqttSettingsActivity.this, "Export success!");
                    Utils.sendEmail(MqttSettingsActivity.this, "", "", "Settings for Device", "Choose Email Client", expertFile);

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
                                    ToastUtils.showToast(MqttSettingsActivity.this, "Please select the correct file!");
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
                                    ToastUtils.showToast(MqttSettingsActivity.this, "Import failed!");
                                    return;
                                }
                                ToastUtils.showToast(MqttSettingsActivity.this, "Import success!");
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
            mqttDeviceConfig.deviceName = mDeviceName;
            mqttDeviceConfig.staMac = mStaMac;
            initData();
        });
        dialog.show(getSupportFragmentManager());
    }
}
