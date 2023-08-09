package com.moko.mkremotegw.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.RadioGroup;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.moko.mkremotegw.AppConstants;
import com.moko.mkremotegw.R;
import com.moko.mkremotegw.adapter.MQTTFragmentAdapter;
import com.moko.mkremotegw.base.BaseActivity;
import com.moko.mkremotegw.databinding.ActivityMqttAppRemoteBinding;
import com.moko.mkremotegw.dialog.AlertMessageDialog;
import com.moko.mkremotegw.entity.MQTTConfig;
import com.moko.mkremotegw.fragment.GeneralFragment;
import com.moko.mkremotegw.fragment.SSLFragment;
import com.moko.mkremotegw.fragment.UserFragment;
import com.moko.mkremotegw.utils.FileUtils;
import com.moko.mkremotegw.utils.SPUtiles;
import com.moko.mkremotegw.utils.ToastUtils;
import com.moko.mkremotegw.utils.Utils;
import com.moko.support.remotegw.MQTTSupport;
import com.moko.support.remotegw.event.MQTTConnectionCompleteEvent;
import com.moko.support.remotegw.event.MQTTConnectionFailureEvent;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.UUID;

import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

public class SetAppMQTTActivity extends BaseActivity<ActivityMqttAppRemoteBinding> implements RadioGroup.OnCheckedChangeListener {
    private final String FILTER_ASCII = "[ -~]*";

    private GeneralFragment generalFragment;
    private UserFragment userFragment;
    private SSLFragment sslFragment;
    private MQTTFragmentAdapter adapter;
    private ArrayList<Fragment> fragments;

    private MQTTConfig mqttConfig;

    private String expertFilePath;
    private boolean isFileError;

    @Override
    protected void onCreate() {
        String MQTTConfigStr = SPUtiles.getStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, "");
        if (TextUtils.isEmpty(MQTTConfigStr)) {
            UUID uuid = UUID.randomUUID();
            String clintIdStr = String.format("MK_%s", uuid.toString().substring(0, 8).toUpperCase());
            mqttConfig = new MQTTConfig();
            mqttConfig.host = "47.104.81.55";
            mqttConfig.port = "1883";
            mqttConfig.clientId = clintIdStr;
        } else {
            Gson gson = new Gson();
            mqttConfig = gson.fromJson(MQTTConfigStr, MQTTConfig.class);
        }
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
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
                }
            }
        });
        mBind.vpMqtt.setOffscreenPageLimit(3);
        mBind.rgMqtt.setOnCheckedChangeListener(this);
        expertFilePath = RemoteMainActivity.PATH_LOGCAT + File.separator + "export" + File.separator + "Settings for APP.xlsx";
    }

    @Override
    protected ActivityMqttAppRemoteBinding getViewBinding() {
        return ActivityMqttAppRemoteBinding.inflate(getLayoutInflater());
    }


    private void createFragment() {
        fragments = new ArrayList<>();
        generalFragment = GeneralFragment.newInstance();
        userFragment = UserFragment.newInstance();
        sslFragment = SSLFragment.newInstance();
        fragments.add(generalFragment);
        fragments.add(userFragment);
        fragments.add(sslFragment);
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 10)
    public void onMQTTConnectionCompleteEvent(MQTTConnectionCompleteEvent event) {
        EventBus.getDefault().cancelEventDelivery(event);
        String mqttConfigStr = new Gson().toJson(mqttConfig, MQTTConfig.class);
        ToastUtils.showToast(SetAppMQTTActivity.this, getString(R.string.success));
        dismissLoadingProgressDialog();
        Intent intent = new Intent();
        intent.putExtra(AppConstants.EXTRA_KEY_MQTT_CONFIG_APP, mqttConfigStr);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTConnectionFailureEvent(MQTTConnectionFailureEvent event) {
        ToastUtils.showToast(SetAppMQTTActivity.this, getString(R.string.mqtt_connect_failed));
        dismissLoadingProgressDialog();
        finish();
    }

    private void initData() {
        mBind.etMqttHost.setText(mqttConfig.host);
        mBind.etMqttPort.setText(mqttConfig.port);
        mBind.etMqttClientId.setText(mqttConfig.clientId);
        mBind.etMqttSubscribeTopic.setText(mqttConfig.topicSubscribe);
        mBind.etMqttPublishTopic.setText(mqttConfig.topicPublish);
        generalFragment.setCleanSession(mqttConfig.cleanSession);
        generalFragment.setQos(mqttConfig.qos);
        generalFragment.setKeepAlive(mqttConfig.keepAlive);
        userFragment.setUserName(mqttConfig.username);
        userFragment.setPassword(mqttConfig.password);
        sslFragment.setConnectMode(mqttConfig.connectMode);
        sslFragment.setCAPath(mqttConfig.caPath);
        sslFragment.setClientKeyPath(mqttConfig.clientKeyPath);
        sslFragment.setClientCertPath(mqttConfig.clientCertPath);
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
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setMessage("Please confirm whether to save the modified parameters?");
        dialog.setConfirm("YES");
        dialog.setCancel("NO");
        dialog.setOnAlertConfirmListener(() -> {
            onSave(null);
        });
        dialog.setOnAlertCancelListener(() -> {
            finish();
        });
        dialog.show(getSupportFragmentManager());
    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        if (checkedId == R.id.rb_general) {
            mBind.vpMqtt.setCurrentItem(0);
        } else if (checkedId == R.id.rb_user) {
            mBind.vpMqtt.setCurrentItem(1);
        } else if (checkedId == R.id.rb_ssl) {
            mBind.vpMqtt.setCurrentItem(2);
        }
    }

    public void onSave(View view) {
        if (isWindowLocked()) return;
        if (isParaError()) return;
        String mqttConfigStr = new Gson().toJson(mqttConfig, MQTTConfig.class);
        SPUtiles.setStringValue(this, AppConstants.SP_KEY_MQTT_CONFIG_APP, mqttConfigStr);
        MQTTSupport.getInstance().disconnectMqtt();
        showLoadingProgressDialog();
        mBind.etMqttHost.postDelayed(() -> {
            try {
                MQTTSupport.getInstance().connectMqtt(mqttConfigStr);
            } catch (FileNotFoundException e) {
                dismissLoadingProgressDialog();
                ToastUtils.showToast(this, "The SSL certificates path is invalid, please select a valid file path and save it.");
                // 读取stacktrace信息
                final Writer result = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(result);
                e.printStackTrace(printWriter);
                StringBuffer errorReport = new StringBuffer();
                errorReport.append(result.toString());
                XLog.e(errorReport.toString());
            }
        }, 2000);
    }

    private boolean isParaError() {
        String host = mBind.etMqttHost.getText().toString().replaceAll(" ", "");
        String port = mBind.etMqttPort.getText().toString();
        String clientId = mBind.etMqttClientId.getText().toString().replaceAll(" ", "");
        String subscribeTopic = mBind.etMqttSubscribeTopic.getText().toString().replaceAll(" ", "");
        String publishTopic = mBind.etMqttPublishTopic.getText().toString().replaceAll(" ", "");

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
        if (!generalFragment.isValid() || !sslFragment.isValid())
            return true;
        mqttConfig.host = host;
        mqttConfig.port = port;
        mqttConfig.clientId = clientId;
        mqttConfig.cleanSession = generalFragment.isCleanSession();
        mqttConfig.qos = generalFragment.getQos();
        mqttConfig.keepAlive = generalFragment.getKeepAlive();
        mqttConfig.topicSubscribe = subscribeTopic;
        mqttConfig.topicPublish = publishTopic;
        mqttConfig.username = userFragment.getUsername();
        mqttConfig.password = userFragment.getPassword();
        mqttConfig.connectMode = sslFragment.getConnectMode();
        mqttConfig.caPath = sslFragment.getCaPath();
        mqttConfig.clientKeyPath = sslFragment.getClientKeyPath();
        mqttConfig.clientCertPath = sslFragment.getClientCertPath();

        if (!mqttConfig.topicPublish.isEmpty() && !mqttConfig.topicSubscribe.isEmpty()
                && mqttConfig.topicPublish.equals(mqttConfig.topicSubscribe)) {
            ToastUtils.showToast(this, "Subscribed and published topic can't be same !");
            return true;
        }
        return false;
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
        mqttConfig.host = mBind.etMqttHost.getText().toString().replaceAll(" ", "");
        mqttConfig.port = mBind.etMqttPort.getText().toString();
        mqttConfig.clientId = mBind.etMqttClientId.getText().toString().replaceAll(" ", "");
        mqttConfig.topicSubscribe = mBind.etMqttSubscribeTopic.getText().toString().replaceAll(" ", "");
        mqttConfig.topicPublish = mBind.etMqttPublishTopic.getText().toString().replaceAll(" ", "");
        mqttConfig.cleanSession = generalFragment.isCleanSession();
        mqttConfig.qos = generalFragment.getQos();
        mqttConfig.keepAlive = generalFragment.getKeepAlive();
        mqttConfig.username = userFragment.getUsername();
        mqttConfig.password = userFragment.getPassword();
        mqttConfig.connectMode = sslFragment.getConnectMode();
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
                if (!TextUtils.isEmpty(mqttConfig.host))
                    row1.createCell(1).setCellValue(String.format("value:%s", mqttConfig.host));
                row1.createCell(2).setCellValue("1-64 characters");

                XSSFRow row2 = sheet.createRow(2);
                row2.createCell(0).setCellValue("Port");
                if (!TextUtils.isEmpty(mqttConfig.port))
                    row2.createCell(1).setCellValue(String.format("value:%s", mqttConfig.port));
                row2.createCell(2).setCellValue("Range: 1-65535");

                XSSFRow row3 = sheet.createRow(3);
                row3.createCell(0).setCellValue("Client id");
                if (!TextUtils.isEmpty(mqttConfig.clientId))
                    row3.createCell(1).setCellValue(String.format("value:%s", mqttConfig.clientId));
                row3.createCell(2).setCellValue("1-64 characters");

                XSSFRow row4 = sheet.createRow(4);
                row4.createCell(0).setCellValue("Subscribe Topic");
                if (!TextUtils.isEmpty(mqttConfig.topicSubscribe))
                    row4.createCell(1).setCellValue(String.format("value:%s", mqttConfig.topicSubscribe));
//                else
//                    row4.createCell(1).setCellValue("");
                row4.createCell(2).setCellValue("0-128 characters");

                XSSFRow row5 = sheet.createRow(5);
                row5.createCell(0).setCellValue("Publish Topic");
                if (!TextUtils.isEmpty(mqttConfig.topicPublish))
                    row5.createCell(1).setCellValue(String.format("value:%s", mqttConfig.topicPublish));
//                else
//                    row5.createCell(1).setCellValue("");
                row5.createCell(2).setCellValue("0-128 characters");

                XSSFRow row6 = sheet.createRow(6);
                row6.createCell(0).setCellValue("Clean Session");
                row6.createCell(1).setCellValue(String.format("value:%s", mqttConfig.cleanSession ? "1" : "0"));
                row6.createCell(2).setCellValue("Range: 0/1 0:NO 1:YES");

                XSSFRow row7 = sheet.createRow(7);
                row7.createCell(0).setCellValue("Qos");
                row7.createCell(1).setCellValue(String.format("value:%d", mqttConfig.qos));
                row7.createCell(2).setCellValue("Range: 0/1/2 0:qos0 1:qos1 2:qos2");

                XSSFRow row8 = sheet.createRow(8);
                row8.createCell(0).setCellValue("Keep Alive");
                row8.createCell(1).setCellValue(String.format("value:%d", mqttConfig.keepAlive));
                row8.createCell(2).setCellValue("Range: 10-120, unit: second");

                XSSFRow row9 = sheet.createRow(9);
                row9.createCell(0).setCellValue("MQTT Username");
                if (!TextUtils.isEmpty(mqttConfig.username))
                    row9.createCell(1).setCellValue(String.format("value:%s", mqttConfig.username));
//                else
//                    row9.createCell(1).setCellValue("");
                row9.createCell(2).setCellValue("0-256 characters");

                XSSFRow row10 = sheet.createRow(10);
                row10.createCell(0).setCellValue("MQTT Password");
                if (!TextUtils.isEmpty(mqttConfig.password))
                    row10.createCell(1).setCellValue(String.format("value:%s", mqttConfig.password));
//                else
//                    row10.createCell(1).setCellValue("");
                row10.createCell(2).setCellValue("0-256 characters");

                XSSFRow row11 = sheet.createRow(11);
                row11.createCell(0).setCellValue("SSL/TLS");
                XSSFRow row12 = sheet.createRow(12);
                row12.createCell(0).setCellValue("Certificate type");
                if (mqttConfig.connectMode > 0) {
                    row11.createCell(1).setCellValue("value:1");
                    row12.createCell(1).setCellValue(String.format("value:%d", mqttConfig.connectMode));
                } else {
                    row11.createCell(1).setCellValue(String.format("value:%d", mqttConfig.connectMode));
                    row12.createCell(1).setCellValue("value:1");
                }
                row11.createCell(2).setCellValue("Range: 0/1 0:Disable SSL (TCP mode) 1:Enable SSL");
                row12.createCell(2).setCellValue("Valid when SSL is enabled, range: 1/2/3 1: CA certificate file 2: CA certificate file 3: Self signed certificates");

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
                        ToastUtils.showToast(SetAppMQTTActivity.this, "Export error!");
                        return;
                    }
                    ToastUtils.showToast(SetAppMQTTActivity.this, "Export success!");
                    Utils.sendEmail(SetAppMQTTActivity.this, "", "", "Settings for APP", "Choose Email Client", expertFile);

                });
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            ToastUtils.showToast(this, "Export error!");
        }
    }

    public void onImportSettings(View view) {
        if (isWindowLocked()) return;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "select file first!"), AppConstants.REQUEST_CODE_OPEN_APP_SETTINGS_FILE);
        } catch (ActivityNotFoundException ex) {
            ToastUtils.showToast(this, "install file manager app");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppConstants.REQUEST_CODE_OPEN_APP_SETTINGS_FILE) {
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
                            if (rows < 13 || columns < 3) {
                                runOnUiThread(() -> {
                                    dismissLoadingProgressDialog();
                                    ToastUtils.showToast(SetAppMQTTActivity.this, "Please select the correct file!");
                                });
                                return;
                            }
                            Cell hostCell = sheet.getRow(1).getCell(1);
                            if (hostCell != null)
                                mqttConfig.host = hostCell.getStringCellValue().replaceAll("value:", "");
                            Cell postCell = sheet.getRow(2).getCell(1);
                            if (postCell != null)
                                mqttConfig.port = postCell.getStringCellValue().replaceAll("value:", "");
                            Cell clientCell = sheet.getRow(3).getCell(1);
                            if (clientCell != null)
                                mqttConfig.clientId = clientCell.getStringCellValue().replaceAll("value:", "");
                            Cell topicSubscribeCell = sheet.getRow(4).getCell(1);
                            if (topicSubscribeCell != null) {
                                mqttConfig.topicSubscribe = topicSubscribeCell.getStringCellValue().replaceAll("value:", "");
                            }
                            Cell topicPublishCell = sheet.getRow(5).getCell(1);
                            if (topicPublishCell != null) {
                                mqttConfig.topicPublish = topicPublishCell.getStringCellValue().replaceAll("value:", "");
                            }
                            Cell cleanSessionCell = sheet.getRow(6).getCell(1);
                            if (cleanSessionCell != null)
                                mqttConfig.cleanSession = "1".equals(cleanSessionCell.getStringCellValue().replaceAll("value:", ""));
                            Cell qosCell = sheet.getRow(7).getCell(1);
                            if (qosCell != null)
                                mqttConfig.qos = Integer.parseInt(qosCell.getStringCellValue().replaceAll("value:", ""));
                            Cell keepAliveCell = sheet.getRow(8).getCell(1);
                            if (keepAliveCell != null)
                                mqttConfig.keepAlive = Integer.parseInt(keepAliveCell.getStringCellValue().replaceAll("value:", ""));
                            Cell usernameCell = sheet.getRow(9).getCell(1);
                            if (usernameCell != null) {
                                mqttConfig.username = usernameCell.getStringCellValue().replaceAll("value:", "");
                            }
                            Cell passwordCell = sheet.getRow(10).getCell(1);
                            if (passwordCell != null) {
                                mqttConfig.password = passwordCell.getStringCellValue().replaceAll("value:", "");
                            }
                            Cell connectModeCell = sheet.getRow(11).getCell(1);
                            if (connectModeCell != null) {
                                // 0/1
                                mqttConfig.connectMode = Integer.parseInt(connectModeCell.getStringCellValue().replaceAll("value:", ""));
                                if (mqttConfig.connectMode > 0) {
                                    Cell cell = sheet.getRow(12).getCell(1);
                                    if (cell != null)
                                        // 1/2/3
                                        mqttConfig.connectMode = Integer.parseInt(cell.getStringCellValue().replaceAll("value:", ""));
                                }
                            }
                            runOnUiThread(() -> {
                                dismissLoadingProgressDialog();
                                if (isFileError) {
                                    ToastUtils.showToast(SetAppMQTTActivity.this, "Import failed!");
                                    return;
                                }
                                ToastUtils.showToast(SetAppMQTTActivity.this, "Import success!");
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
            mqttConfig = new MQTTConfig();
            mqttConfig.keepAlive = -1;
            initData();
        });
        dialog.show(getSupportFragmentManager());
    }
}
