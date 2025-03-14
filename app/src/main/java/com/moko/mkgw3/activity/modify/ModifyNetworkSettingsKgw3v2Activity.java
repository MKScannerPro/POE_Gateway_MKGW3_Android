package com.moko.mkgw3.activity.modify;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.moko.mkgw3.AppConstants;
import com.moko.mkgw3.R;
import com.moko.mkgw3.base.BaseActivity;
import com.moko.mkgw3.databinding.ActivityModifyNetworkSettingsKgw3v2Binding;
import com.moko.mkgw3.databinding.LayoutDhcpInfoBinding;
import com.moko.mkgw3.dialog.MKgw3BottomDialog;
import com.moko.mkgw3.entity.MQTTConfigKgw3;
import com.moko.mkgw3.entity.MokoDeviceKgw3;
import com.moko.mkgw3.utils.SPUtiles;
import com.moko.mkgw3.utils.ToastUtils;
import com.moko.support.mkgw3.MQTTConstants;
import com.moko.support.mkgw3.MQTTSupport;
import com.moko.support.mkgw3.entity.MsgConfigResult;
import com.moko.support.mkgw3.entity.MsgNotify;
import com.moko.support.mkgw3.entity.MsgReadResult;
import com.moko.support.mkgw3.event.DeviceOnlineEvent;
import com.moko.support.mkgw3.event.MQTTMessageArrivedEvent;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModifyNetworkSettingsKgw3v2Activity extends BaseActivity<ActivityModifyNetworkSettingsKgw3v2Binding> {
    private final String FILTER_ASCII = "[ -~]*";
    private ArrayList<String> mSecurityValues;
    private int mSecuritySelected;
    private ArrayList<String> mEAPTypeValues;
    private int mEAPTypeSelected;
    private MokoDeviceKgw3 mMokoDeviceKgw3;
    private MQTTConfigKgw3 appMqttConfig;
    private String mAppTopic;
    public Handler mHandler;
    private final String[] networkTypeValues = {"ETH", "WIFI", "ETH>WIFI", "WIFI>ETH"};
    private int selectedNetworkType;
    private Pattern pattern;
    private String wifiIp;
    private String wifiMask;
    private String wifiGateway;
    private String wifiDns;
    private String ethernetIp;
    private String ethernetMask;
    private String ethernetGateway;
    private String ethernetDns;
    private boolean wifiDhcpEnable;
    private boolean ethernetDhcpEnable;

    @Override
    protected void onCreate() {
        String IP_REGEX = "^((25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}(25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))$";
        pattern = Pattern.compile(IP_REGEX);
        mBind.cbVerifyServer.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mSecuritySelected != 0 && mEAPTypeSelected != 2)
                mBind.clCa.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
        mSecurityValues = new ArrayList<>();
        mSecurityValues.add("Personal");
        mSecurityValues.add("Enterprise");
        mEAPTypeValues = new ArrayList<>();
        mEAPTypeValues.add("PEAP-MSCHAPV2");
        mEAPTypeValues.add("TTLS-MSCHAPV2");
        mEAPTypeValues.add("TLS");
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }
            return null;
        };
        mBind.etUsername.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32), filter});
        mBind.etPassword.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        mBind.etEapPassword.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        mBind.etSsid.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32), filter});
        mBind.etDomainId.setFilters(new InputFilter[]{new InputFilter.LengthFilter(64), filter});
        mBind.etCaFileUrl.setFilters(new InputFilter[]{new InputFilter.LengthFilter(256), filter});
        mBind.etCertFileUrl.setFilters(new InputFilter[]{new InputFilter.LengthFilter(256), filter});
        mBind.etKeyFileUrl.setFilters(new InputFilter[]{new InputFilter.LengthFilter(256), filter});

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
        getNetworkType();
        mBind.tvNetworkType.setOnClickListener(v -> onNetworkTypeClick());
        mBind.wifiDhcp.imgDhcp.setOnClickListener(v -> {
            wifiDhcpEnable = !wifiDhcpEnable;
            setDhcpEnable(mBind.wifiDhcp, wifiDhcpEnable);
        });
        mBind.ethDhcp.imgDhcp.setOnClickListener(v -> {
            ethernetDhcpEnable = !ethernetDhcpEnable;
            setDhcpEnable(mBind.ethDhcp, ethernetDhcpEnable);
        });
    }

    @Override
    protected ActivityModifyNetworkSettingsKgw3v2Binding getViewBinding() {
        return ActivityModifyNetworkSettingsKgw3v2Binding.inflate(getLayoutInflater());
    }

    private void onNetworkTypeClick() {
        if (isWindowLocked()) return;
        MKgw3BottomDialog dialog = new MKgw3BottomDialog();
        dialog.setDatas(new ArrayList<>(Arrays.asList(networkTypeValues)), selectedNetworkType);
        dialog.setListener(value -> {
            selectedNetworkType = value;
            mBind.tvNetworkType.setText(networkTypeValues[value]);
            if (selectedNetworkType == 0) {
                mBind.tvEthernet.setVisibility(View.GONE);
                mBind.ethDhcp.llDHCPParent.setVisibility(View.VISIBLE);
                mBind.tvWifi.setVisibility(View.GONE);
                mBind.wifiDhcp.llDHCPParent.setVisibility(View.GONE);
                mBind.layoutWifi.setVisibility(View.GONE);
            } else if (selectedNetworkType == 1) {
                mBind.tvEthernet.setVisibility(View.GONE);
                mBind.ethDhcp.llDHCPParent.setVisibility(View.GONE);
                mBind.tvWifi.setVisibility(View.GONE);
                mBind.wifiDhcp.llDHCPParent.setVisibility(View.VISIBLE);
                mBind.layoutWifi.setVisibility(View.VISIBLE);
            } else {
                mBind.tvEthernet.setVisibility(View.VISIBLE);
                mBind.ethDhcp.llDHCPParent.setVisibility(View.VISIBLE);
                mBind.tvWifi.setVisibility(View.VISIBLE);
                mBind.wifiDhcp.llDHCPParent.setVisibility(View.VISIBLE);
                mBind.layoutWifi.setVisibility(View.VISIBLE);
            }
            //更新开关状态
            setDhcpEnable(mBind.ethDhcp, value == 0 ? ethernetDhcpEnable : wifiDhcpEnable);
            setIpInfo(mBind.ethDhcp);
        });
        dialog.show(getSupportFragmentManager());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTMessageArrivedEvent(MQTTMessageArrivedEvent event) {
        // 更新所有设备的网络状态
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
        if (msg_id == MQTTConstants.READ_MSG_ID_NETWORK_TYPE) {
            Type type = new TypeToken<MsgReadResult<JsonObject>>() {
            }.getType();
            MsgReadResult<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            selectedNetworkType = result.data.get("net_interface").getAsInt();
            mBind.tvNetworkType.setText(networkTypeValues[selectedNetworkType]);
            if (selectedNetworkType == 0) {
                mBind.tvEthernet.setVisibility(View.GONE);
                mBind.ethDhcp.llDHCPParent.setVisibility(View.VISIBLE);
                mBind.tvWifi.setVisibility(View.GONE);
                mBind.wifiDhcp.llDHCPParent.setVisibility(View.GONE);
                mBind.layoutWifi.setVisibility(View.GONE);
            } else if (selectedNetworkType == 1) {
                mBind.tvEthernet.setVisibility(View.GONE);
                mBind.ethDhcp.llDHCPParent.setVisibility(View.GONE);
                mBind.tvWifi.setVisibility(View.GONE);
                mBind.wifiDhcp.llDHCPParent.setVisibility(View.VISIBLE);
                mBind.layoutWifi.setVisibility(View.VISIBLE);
            } else {
                mBind.tvEthernet.setVisibility(View.VISIBLE);
                mBind.ethDhcp.llDHCPParent.setVisibility(View.VISIBLE);
                mBind.tvWifi.setVisibility(View.VISIBLE);
                mBind.wifiDhcp.llDHCPParent.setVisibility(View.VISIBLE);
                mBind.layoutWifi.setVisibility(View.VISIBLE);
            }
            getWifiSettings();
        }
        if (msg_id == MQTTConstants.READ_MSG_ID_WIFI_SETTINGS) {
            Type type = new TypeToken<MsgReadResult<JsonObject>>() {
            }.getType();
            MsgReadResult<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            mSecuritySelected = result.data.get("security_type").getAsInt();
            mBind.etSsid.setText(result.data.get("ssid").getAsString());
            mBind.etPassword.setText(result.data.get("passwd").getAsString());
            mBind.etDomainId.setText(result.data.get("eap_id").getAsString());
            mBind.tvSecurity.setText(mSecurityValues.get(mSecuritySelected));
            mBind.clEapType.setVisibility(mSecuritySelected != 0 ? View.VISIBLE : View.GONE);
            mBind.clPassword.setVisibility(mSecuritySelected != 0 ? View.GONE : View.VISIBLE);

            mEAPTypeSelected = result.data.get("eap_type").getAsInt();
            mBind.tvEapType.setText(mEAPTypeValues.get(mEAPTypeSelected));
            if (mSecuritySelected != 0) {
                mBind.clUsername.setVisibility(mEAPTypeSelected == 2 ? View.GONE : View.VISIBLE);
                mBind.etUsername.setText(result.data.get("eap_username").getAsString());
                mBind.clEapPassword.setVisibility(mEAPTypeSelected == 2 ? View.GONE : View.VISIBLE);
                mBind.etEapPassword.setText(result.data.get("eap_passwd").getAsString());
                mBind.cbVerifyServer.setVisibility(mEAPTypeSelected == 2 ? View.INVISIBLE : View.VISIBLE);
                mBind.cbVerifyServer.setChecked(result.data.get("eap_verify_server").getAsInt() == 1);
                if (mEAPTypeSelected != 2) {
                    mBind.clCa.setVisibility(mBind.cbVerifyServer.isChecked() ? View.VISIBLE : View.GONE);
                } else {
                    mBind.clCa.setVisibility(View.VISIBLE);
                }
                mBind.clDomainId.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
                mBind.clCert.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
                mBind.clKey.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
            }
            getWifiIpInfo();
        }
        if (msg_id == MQTTConstants.READ_MSG_ID_WIFI_PARAMS) {
            Type type = new TypeToken<MsgReadResult<JsonObject>>() {
            }.getType();
            MsgReadResult<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            wifiDhcpEnable = result.data.get("dhcp_en").getAsInt() == 1;
            wifiIp = result.data.get("ip").getAsString();
            wifiMask = result.data.get("netmask").getAsString();
            wifiGateway = result.data.get("gw").getAsString();
            wifiDns = result.data.get("dns").getAsString();
            if (selectedNetworkType != 0) {
                setDhcpEnable(mBind.wifiDhcp, wifiDhcpEnable);
                setIpInfo(mBind.wifiDhcp);
            }
            getEthernetIpInfo();
        }
        if (msg_id == MQTTConstants.READ_MSG_ID_ETHERNET_PARAMS) {
            Type type = new TypeToken<MsgReadResult<JsonObject>>() {
            }.getType();
            MsgReadResult<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            ethernetDhcpEnable = result.data.get("dhcp_en").getAsInt() == 1;
            ethernetIp = result.data.get("ip").getAsString();
            ethernetMask = result.data.get("netmask").getAsString();
            ethernetGateway = result.data.get("gw").getAsString();
            ethernetDns = result.data.get("dns").getAsString();
            if (selectedNetworkType != 1) {
                setDhcpEnable(mBind.ethDhcp, ethernetDhcpEnable);
                setIpInfo(mBind.ethDhcp);
            }
        }
        if (msg_id == MQTTConstants.READ_MSG_ID_DEVICE_STATUS) {
            Type type = new TypeToken<MsgNotify<JsonObject>>() {
            }.getType();
            MsgNotify<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
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
            setNetworkType();
        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_NETWORK_TYPE) {
            Type type = new TypeToken<MsgConfigResult>() {
            }.getType();
            MsgConfigResult result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            if (result.result_code != 0) return;
            if (selectedNetworkType != 1)
                setEthNetworkSettings();
            else
                setWifiDHCPParams();
        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_ETHERNET_PARAMS) {
            Type type = new TypeToken<MsgConfigResult>() {
            }.getType();
            MsgConfigResult result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            if (selectedNetworkType == 0) {
                dismissLoadingProgressDialog();
                mHandler.removeMessages(0);
                if (result.result_code == 0) {
                    ToastUtils.showToast(this, "Set up succeed");
                } else {
                    ToastUtils.showToast(this, "Set up failed");
                }
            } else {
                if (result.result_code != 0) return;
                setWifiDHCPParams();
            }
        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_WIFI_PARAMS) {
            Type type = new TypeToken<MsgConfigResult>() {
            }.getType();
            MsgConfigResult result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            if (result.result_code != 0) return;
            setWifiSettings();
        }
        if (msg_id == MQTTConstants.CONFIG_MSG_ID_WIFI_SETTINGS) {
            Type type = new TypeToken<MsgConfigResult>() {
            }.getType();
            MsgConfigResult result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac)) return;
            dismissLoadingProgressDialog();
            mHandler.removeMessages(0);
            if (result.result_code == 0) {
                ToastUtils.showToast(this, "Set up succeed");
                if (mSecuritySelected == 0) return;
                String caFileUrl = mBind.etCaFileUrl.getText().toString();
                String certFileUrl = mBind.etCertFileUrl.getText().toString();
                String keyFileUrl = mBind.etKeyFileUrl.getText().toString();
                // 若EAP类型不是TLS且CA证书为空，不发送证书更新指令
                if (mEAPTypeSelected != 2
                        && TextUtils.isEmpty(caFileUrl))
                    return;
                // 若EAP类型是TLS且所有证书都为空，不发送证书更新指令
                if (mEAPTypeSelected == 2
                        && TextUtils.isEmpty(caFileUrl)
                        && TextUtils.isEmpty(certFileUrl)
                        && TextUtils.isEmpty(keyFileUrl))
                    return;
                XLog.i("升级Wifi证书");
                mHandler.postDelayed(() -> {
                    dismissLoadingProgressDialog();
                    finish();
                }, 50 * 1000);
                showLoadingProgressDialog();
                setWifiCertFile();
            } else {
                ToastUtils.showToast(this, "Set up failed");
            }
        }
        if (msg_id == MQTTConstants.NOTIFY_MSG_ID_WIFI_CERT_RESULT) {
            Type type = new TypeToken<MsgNotify<JsonObject>>() {
            }.getType();
            MsgNotify<JsonObject> result = new Gson().fromJson(message, type);
            if (!mMokoDeviceKgw3.mac.equalsIgnoreCase(result.device_info.mac))
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

    private void setDhcpEnable(LayoutDhcpInfoBinding dhcpInfoBinding, boolean enable) {
        dhcpInfoBinding.imgDhcp.setImageResource(enable ? R.drawable.checkbox_open : R.drawable.checkbox_close);
        dhcpInfoBinding.clIp.setVisibility(enable ? View.GONE : View.VISIBLE);
    }

    private void setIpInfo(LayoutDhcpInfoBinding dhcpInfoBinding) {
        if (dhcpInfoBinding == mBind.wifiDhcp) {
            dhcpInfoBinding.etIp.setText(wifiIp);
            dhcpInfoBinding.etMask.setText(wifiMask);
            dhcpInfoBinding.etGateway.setText(wifiGateway);
            dhcpInfoBinding.etDns.setText(wifiDns);
        } else {
            dhcpInfoBinding.etIp.setText(ethernetIp);
            dhcpInfoBinding.etMask.setText(ethernetMask);
            dhcpInfoBinding.etGateway.setText(ethernetGateway);
            dhcpInfoBinding.etDns.setText(ethernetDns);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceOnlineEvent(DeviceOnlineEvent event) {
        super.offline(event, mMokoDeviceKgw3.mac);
    }

    public void onBack(View view) {
        back();
    }

    private void back() {
        Intent intent = new Intent();
        intent.putExtra("type", selectedNetworkType);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        back();
    }

    private void setNetworkType() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_NETWORK_TYPE;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("net_interface", selectedNetworkType);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setWifiDHCPParams() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_WIFI_PARAMS;
        JsonObject jsonObject = new JsonObject();
        int enable = wifiDhcpEnable ? 1 : 0;
        jsonObject.addProperty("dhcp_en", enable);
        jsonObject.addProperty("ip", mBind.wifiDhcp.etIp.getText().toString());
        jsonObject.addProperty("netmask", mBind.wifiDhcp.etMask.getText().toString());
        jsonObject.addProperty("gw", mBind.wifiDhcp.etGateway.getText().toString());
        jsonObject.addProperty("dns", mBind.wifiDhcp.etDns.getText().toString());
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setEthNetworkSettings() {
        int msgId = MQTTConstants.CONFIG_MSG_ID_ETHERNET_PARAMS;
        JsonObject jsonObject = new JsonObject();
        int enable = ethernetDhcpEnable ? 1 : 0;
        jsonObject.addProperty("dhcp_en", enable);
        jsonObject.addProperty("ip", mBind.ethDhcp.etIp.getText().toString());
        jsonObject.addProperty("netmask", mBind.ethDhcp.etMask.getText().toString());
        jsonObject.addProperty("gw", mBind.ethDhcp.etGateway.getText().toString());
        jsonObject.addProperty("dns", mBind.ethDhcp.etDns.getText().toString());
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setWifiSettings() {
        String ssid = mBind.etSsid.getText().toString();
        String username = mBind.etUsername.getText().toString();
        String password = mBind.etPassword.getText().toString();
        String eapPassword = mBind.etEapPassword.getText().toString();
        String domainId = mBind.etDomainId.getText().toString();
        int msgId = MQTTConstants.CONFIG_MSG_ID_WIFI_SETTINGS;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("security_type", mSecuritySelected);
        jsonObject.addProperty("ssid", ssid);
        jsonObject.addProperty("passwd", mSecuritySelected == 0 ? password : "");
        jsonObject.addProperty("eap_type", mEAPTypeSelected);
        jsonObject.addProperty("eap_id", mEAPTypeSelected == 2 ? domainId : "");
        jsonObject.addProperty("eap_username", mSecuritySelected != 0 ? username : "");
        jsonObject.addProperty("eap_passwd", mSecuritySelected != 0 ? eapPassword : "");
        jsonObject.addProperty("eap_verify_server", mBind.cbVerifyServer.isChecked() ? 1 : 0);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setWifiCertFile() {
        String caFileUrl = mBind.etCaFileUrl.getText().toString();
        String certFileUrl = mBind.etCertFileUrl.getText().toString();
        String keyFileUrl = mBind.etKeyFileUrl.getText().toString();
        int msgId = MQTTConstants.CONFIG_MSG_ID_WIFI_CERT_FILE;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("ca_url", caFileUrl);
        jsonObject.addProperty("client_cert_url", certFileUrl);
        jsonObject.addProperty("client_key_url", keyFileUrl);
        String message = assembleWriteCommonData(msgId, mMokoDeviceKgw3.mac, jsonObject);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getWifiSettings() {
        int msgId = MQTTConstants.READ_MSG_ID_WIFI_SETTINGS;
        String message = assembleReadCommon(msgId, mMokoDeviceKgw3.mac);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getWifiIpInfo() {
        int msgId = MQTTConstants.READ_MSG_ID_WIFI_PARAMS;
        String message = assembleReadCommon(msgId, mMokoDeviceKgw3.mac);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getEthernetIpInfo() {
        int msgId = MQTTConstants.READ_MSG_ID_ETHERNET_PARAMS;
        String message = assembleReadCommon(msgId, mMokoDeviceKgw3.mac);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void getNetworkType() {
        int msgId = MQTTConstants.READ_MSG_ID_NETWORK_TYPE;
        String message = assembleReadCommon(msgId, mMokoDeviceKgw3.mac);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void onSelectSecurity(View view) {
        if (isWindowLocked()) return;
        MKgw3BottomDialog dialog = new MKgw3BottomDialog();
        dialog.setDatas(mSecurityValues, mSecuritySelected);
        dialog.setListener(value -> {
            mSecuritySelected = value;
            mBind.tvSecurity.setText(mSecurityValues.get(value));
            mBind.clEapType.setVisibility(mSecuritySelected != 0 ? View.VISIBLE : View.GONE);
            mBind.clPassword.setVisibility(mSecuritySelected != 0 ? View.GONE : View.VISIBLE);
            if (mSecuritySelected == 0) {
                mBind.clCa.setVisibility(View.GONE);
                mBind.clUsername.setVisibility(View.GONE);
                mBind.clEapPassword.setVisibility(View.GONE);
                mBind.cbVerifyServer.setVisibility(View.GONE);
                mBind.clDomainId.setVisibility(View.GONE);
                mBind.clCert.setVisibility(View.GONE);
                mBind.clKey.setVisibility(View.GONE);
            } else {
                if (mEAPTypeSelected != 2) {
                    mBind.clCa.setVisibility(mBind.cbVerifyServer.isChecked() ? View.VISIBLE : View.GONE);
                } else {
                    mBind.clCa.setVisibility(View.VISIBLE);
                }
                mBind.clUsername.setVisibility(mEAPTypeSelected == 2 ? View.GONE : View.VISIBLE);
                mBind.clEapPassword.setVisibility(mEAPTypeSelected == 2 ? View.GONE : View.VISIBLE);
                mBind.cbVerifyServer.setVisibility(mEAPTypeSelected == 2 ? View.INVISIBLE : View.VISIBLE);
                mBind.clDomainId.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
                mBind.clCert.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
                mBind.clKey.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
            }
        });
        dialog.show(getSupportFragmentManager());
    }

    public void onSelectEAPType(View view) {
        if (isWindowLocked()) return;
        MKgw3BottomDialog dialog = new MKgw3BottomDialog();
        dialog.setDatas(mEAPTypeValues, mEAPTypeSelected);
        dialog.setListener(value -> {
            mEAPTypeSelected = value;
            mBind.tvEapType.setText(mEAPTypeValues.get(value));
            mBind.clUsername.setVisibility(mEAPTypeSelected == 2 ? View.GONE : View.VISIBLE);
            mBind.clEapPassword.setVisibility(mEAPTypeSelected == 2 ? View.GONE : View.VISIBLE);
            mBind.cbVerifyServer.setVisibility(mEAPTypeSelected == 2 ? View.INVISIBLE : View.VISIBLE);
            mBind.clDomainId.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
            if (mEAPTypeSelected != 2) {
                mBind.clCa.setVisibility(mBind.cbVerifyServer.isChecked() ? View.VISIBLE : View.GONE);
            } else {
                mBind.clCa.setVisibility(View.VISIBLE);
            }
            mBind.clCert.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
            mBind.clKey.setVisibility(mEAPTypeSelected == 2 ? View.VISIBLE : View.GONE);
        });
        dialog.show(getSupportFragmentManager());
    }

    public void onSave(View view) {
        if (isWindowLocked()) return;
        if (!isParaError()) {
            saveParams();
        } else {
            ToastUtils.showToast(this, "Para Error");
        }
    }

    private boolean isParaError() {
        if (selectedNetworkType > 0) {
            String ssid = mBind.etSsid.getText().toString();
            if (TextUtils.isEmpty(ssid)) return true;
        }
        if (selectedNetworkType != 1) {
            if (!ethernetDhcpEnable) {
                //检查ip地址是否合法
                String ip = mBind.ethDhcp.etIp.getText().toString();
                String mask = mBind.ethDhcp.etMask.getText().toString();
                String gateway = mBind.ethDhcp.etGateway.getText().toString();
                String dns = mBind.ethDhcp.etDns.getText().toString();
                Matcher matcherIp = pattern.matcher(ip);
                Matcher matcherMask = pattern.matcher(mask);
                Matcher matcherGateway = pattern.matcher(gateway);
                Matcher matcherDns = pattern.matcher(dns);
                if (!matcherIp.matches()
                        || !matcherMask.matches()
                        || !matcherGateway.matches()
                        || !matcherDns.matches())
                    return true;
                ethernetIp = ip;
                ethernetMask = mask;
                ethernetGateway = gateway;
                ethernetDns = dns;
            }
        }
        if (selectedNetworkType != 0) {
            if (!wifiDhcpEnable) {
                //检查ip地址是否合法
                String ip = mBind.wifiDhcp.etIp.getText().toString();
                String mask = mBind.wifiDhcp.etMask.getText().toString();
                String gateway = mBind.wifiDhcp.etGateway.getText().toString();
                String dns = mBind.wifiDhcp.etDns.getText().toString();
                Matcher matcherIp = pattern.matcher(ip);
                Matcher matcherMask = pattern.matcher(mask);
                Matcher matcherGateway = pattern.matcher(gateway);
                Matcher matcherDns = pattern.matcher(dns);
                if (!matcherIp.matches()
                        || !matcherMask.matches()
                        || !matcherGateway.matches()
                        || !matcherDns.matches())
                    return true;
                wifiIp = ip;
                wifiMask = mask;
                wifiGateway = gateway;
                wifiDns = dns;
            }
        }
        return false;
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
        String message = assembleReadCommon(msgId, mMokoDeviceKgw3.mac);
        try {
            MQTTSupport.getInstance().publish(mAppTopic, message, msgId, appMqttConfig.qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
