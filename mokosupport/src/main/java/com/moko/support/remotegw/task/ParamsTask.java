package com.moko.support.remotegw.task;

import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.support.remotegw.MokoSupport;
import com.moko.support.remotegw.entity.OrderCHAR;
import com.moko.support.remotegw.entity.ParamsKeyEnum;
import com.moko.support.remotegw.entity.ParamsLongKeyEnum;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;

import androidx.annotation.IntRange;

public class ParamsTask extends OrderTask {
    public byte[] data;

    public ParamsTask() {
        super(OrderCHAR.CHAR_PARAMS, OrderTask.RESPONSE_TYPE_WRITE);
    }

    @Override
    public byte[] assemble() {
        return data;
    }

    public void setData(ParamsKeyEnum key) {
        switch (key) {
            case KEY_PASSWORD:
            case KEY_RESET_PARAMS_TYPE:
            case KEY_DEVICE_NAME:
            case KEY_PRODUCT_MODEL:
            case KEY_HARDWARE_VERSION:
            case KEY_MANUFACTURER:
            case KEY_BLE_MAC:
            case KEY_WIFI_MAC:
                // ====TEST
            case KEY_PRODUCT_TEST_BUTTON_STATE:
            case KEY_PRODUCT_TEST_DEVICE_STATE:
                // ====
            case KEY_INDICATOR_SWITCH:
            case KEY_NTP_ENABLE:
            case KEY_NTP_URL:
            case KEY_NTP_TIME_ZONE:
                // MQTT
            case KEY_MQTT_HOST:
            case KEY_MQTT_PORT:
            case KEY_MQTT_CLIENT_ID:
            case KEY_MQTT_CLEAN_SESSION:
            case KEY_MQTT_KEEP_ALIVE:
            case KEY_MQTT_QOS:
            case KEY_MQTT_SUBSCRIBE_TOPIC:
            case KEY_MQTT_PUBLISH_TOPIC:
            case KEY_MQTT_LWT_ENABLE:
            case KEY_MQTT_LWT_QOS:
            case KEY_MQTT_LWT_RETAIN:
            case KEY_MQTT_LWT_TOPIC:
            case KEY_MQTT_LWT_PAYLOAD:
            case KEY_MQTT_CONNECT_MODE:
                // WIFI
            case KEY_WIFI_SECURITY_TYPE:
            case KEY_WIFI_SSID:
            case KEY_WIFI_PASSWORD:
            case KEY_WIFI_EAP_TYPE:
            case KEY_WIFI_EAP_USERNAME:
            case KEY_WIFI_EAP_PASSWORD:
            case KEY_WIFI_EAP_DOMAIN_ID:
            case KEY_WIFI_EAP_VERIFY_SERVICE_ENABLE:
            case KEY_NETWORK_DHCP:
            case KEY_NETWORK_IP_INFO:
                // OTHER
            case KEY_FILTER_RSSI:
            case KEY_FILTER_RELATIONSHIP:
            case KEY_FILTER_MAC_PRECISE:
            case KEY_FILTER_MAC_REVERSE:
            case KEY_FILTER_MAC_RULES:
            case KEY_FILTER_NAME_PRECISE:
            case KEY_FILTER_NAME_REVERSE:
                createGetConfigData(key.getParamsKey());
                break;
        }
    }

    public void setData(ParamsLongKeyEnum key) {
        switch (key) {
            case KEY_MQTT_USERNAME:
            case KEY_MQTT_PASSWORD:
            case KEY_FILTER_NAME_RULES:
                createGetLongConfigData(key.getParamsKey());
                break;
        }
    }

    private void createGetLongConfigData(int paramsKey) {
        data = new byte[]{
                (byte) 0xEE,
                (byte) 0x00,
                (byte) paramsKey,
                (byte) 0x00
        };
        response.responseValue = data;
    }

    private void createGetConfigData(int configKey) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x00,
                (byte) configKey,
                (byte) 0x00
        };
        response.responseValue = data;
    }

    public void reboot() {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_REBOOT.getParamsKey(),
                (byte) 0x00
        };
        response.responseValue = data;
    }

    public void exitConfigMode() {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_EXIT_CONFIG_MODE.getParamsKey(),
                (byte) 0x00,
        };
        response.responseValue = data;
    }

    public void changePassword(String password) {
        byte[] dataBytes = password.getBytes();
        int length = dataBytes.length;
        this.data = new byte[4 + length];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_PASSWORD.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < length; i++) {
            data[i + 4] = dataBytes[i];
        }
        response.responseValue = data;
    }

    public void resetParamsType(@IntRange(from = 0, to = 2) int type) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_RESET_PARAMS_TYPE.getParamsKey(),
                (byte) 0x01,
                (byte) type
        };
        response.responseValue = data;
    }

    public void setDeviceName(String deviceName) {
        byte[] dataBytes = deviceName.getBytes();
        int length = dataBytes.length;
        this.data = new byte[4 + length];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_DEVICE_NAME.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < length; i++) {
            data[i + 4] = dataBytes[i];
        }
        response.responseValue = data;
    }

    public void setProductModel(String productModel) {
        byte[] dataBytes = productModel.getBytes();
        int length = dataBytes.length;
        this.data = new byte[4 + length];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_PRODUCT_MODEL.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < length; i++) {
            data[i + 4] = dataBytes[i];
        }
        response.responseValue = data;
    }

    public void setHardwareVersion(String hardwareVersion) {
        byte[] dataBytes = hardwareVersion.getBytes();
        int length = dataBytes.length;
        this.data = new byte[4 + length];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_HARDWARE_VERSION.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < length; i++) {
            data[i + 4] = dataBytes[i];
        }
        response.responseValue = data;
    }

    public void setManufacturer(String manufacturer) {
        byte[] dataBytes = manufacturer.getBytes();
        int length = dataBytes.length;
        this.data = new byte[4 + length];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_MANUFACTURER.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < length; i++) {
            data[i + 4] = dataBytes[i];
        }
        response.responseValue = data;
    }

    public void setProductTestMode() {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_PRODUCT_TEST_MODE.getParamsKey(),
                (byte) 0x00
        };
        response.responseValue = data;
    }

    public void setProductTestDeviceState(@IntRange(from = 0, to = 2) int state) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_PRODUCT_TEST_DEVICE_STATE.getParamsKey(),
                (byte) 0x01,
                (byte) state,
        };
        response.responseValue = data;
    }

    public void resetParams() {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_RESET_PARAMS.getParamsKey(),
                (byte) 0x00
        };
        response.responseValue = data;
    }

    public void setIndicatorSwitch(@IntRange(from = 0, to = 15) int enable) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_INDICATOR_SWITCH.getParamsKey(),
                (byte) 0x01,
                (byte) enable,
        };
        response.responseValue = data;
    }

    public void setNtpEnable(@IntRange(from = 0, to = 1) int enable) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_NTP_ENABLE.getParamsKey(),
                (byte) 0x01,
                (byte) enable,
        };
        response.responseValue = data;
    }


    public void setNtpUrl(String url) {
        byte[] dataBytes = url.getBytes();
        int length = dataBytes.length;
        this.data = new byte[4 + length];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_NTP_URL.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < length; i++) {
            data[i + 4] = dataBytes[i];
        }
        response.responseValue = data;
    }

    public void setTimezone(@IntRange(from = -24, to = 28) int timezone) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_NTP_TIME_ZONE.getParamsKey(),
                (byte) 0x01,
                (byte) timezone,
        };
        response.responseValue = data;
    }

    public void setMqttHost(String host) {
        byte[] dataBytes = host.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_MQTT_HOST.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
        response.responseValue = data;
    }

    public void setMqttPort(@IntRange(from = 1, to = 65535) int port) {
        byte[] dataBytes = MokoUtils.toByteArray(port, 2);
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_MQTT_PORT.getParamsKey(),
                (byte) 0x02,
                dataBytes[0],
                dataBytes[1]
        };
        response.responseValue = data;
    }

    public void setMqttClientId(String clientId) {
        byte[] dataBytes = clientId.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_MQTT_CLIENT_ID.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
        response.responseValue = data;
    }

    public void setMqttCleanSession(@IntRange(from = 0, to = 1) int enable) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_MQTT_CLEAN_SESSION.getParamsKey(),
                (byte) 0x01,
                (byte) enable
        };
        response.responseValue = data;
    }

    public void setMqttKeepAlive(@IntRange(from = 10, to = 120) int keepAlive) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_MQTT_KEEP_ALIVE.getParamsKey(),
                (byte) 0x01,
                (byte) keepAlive
        };
        response.responseValue = data;
    }

    public void setMqttQos(@IntRange(from = 0, to = 2) int qos) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_MQTT_QOS.getParamsKey(),
                (byte) 0x01,
                (byte) qos
        };
        response.responseValue = data;
    }

    public void setMqttSubscribeTopic(String topic) {
        byte[] dataBytes = topic.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_MQTT_SUBSCRIBE_TOPIC.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
        response.responseValue = data;
    }

    public void setMqttPublishTopic(String topic) {
        byte[] dataBytes = topic.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_MQTT_PUBLISH_TOPIC.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
        response.responseValue = data;
    }

    public void setMqttLwtEnable(@IntRange(from = 0, to = 1) int enable) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_MQTT_LWT_ENABLE.getParamsKey(),
                (byte) 0x01,
                (byte) enable
        };
        response.responseValue = data;
    }

    public void setMqttLwtQos(@IntRange(from = 0, to = 2) int qos) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_MQTT_LWT_QOS.getParamsKey(),
                (byte) 0x01,
                (byte) qos
        };
        response.responseValue = data;
    }

    public void setMqttLwtRetain(@IntRange(from = 0, to = 1) int retain) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_MQTT_LWT_RETAIN.getParamsKey(),
                (byte) 0x01,
                (byte) retain
        };
        response.responseValue = data;
    }

    public void setMqttLwtTopic(String topic) {
        byte[] dataBytes = topic.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_MQTT_LWT_TOPIC.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
        response.responseValue = data;
    }

    public void setMqttLwtPayload(String payload) {
        byte[] dataBytes = payload.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_MQTT_LWT_PAYLOAD.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
        response.responseValue = data;
    }

    public void setMqttConnectMode(@IntRange(from = 0, to = 3) int mode) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_MQTT_CONNECT_MODE.getParamsKey(),
                (byte) 0x01,
                (byte) mode
        };
        response.responseValue = data;
    }

    public void setWifiSecurityType(@IntRange(from = 0, to = 1) int type) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_WIFI_SECURITY_TYPE.getParamsKey(),
                (byte) 0x01,
                (byte) type
        };
        response.responseValue = data;
    }


    public void setWifiSSID(String SSID) {
        byte[] dataBytes = SSID.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_WIFI_SSID.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
        response.responseValue = data;
    }

    public void setWifiPassword(String password) {
        byte[] dataBytes = password.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_WIFI_PASSWORD.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
        response.responseValue = data;
    }

    public void setWifiEapType(@IntRange(from = 0, to = 2) int type) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_WIFI_EAP_TYPE.getParamsKey(),
                (byte) 0x01,
                (byte) type
        };
        response.responseValue = data;
    }

    public void setWifiEapUsername(String username) {
        byte[] dataBytes = username.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_WIFI_EAP_USERNAME.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
        response.responseValue = data;
    }

    public void setWifiEapPassword(String password) {
        byte[] dataBytes = password.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_WIFI_EAP_PASSWORD.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
        response.responseValue = data;
    }

    public void setWifiEapDomainId(String domainId) {
        byte[] dataBytes = domainId.getBytes();
        int length = dataBytes.length;
        data = new byte[length + 4];
        data[0] = (byte) 0xED;
        data[1] = (byte) 0x01;
        data[2] = (byte) ParamsKeyEnum.KEY_WIFI_EAP_DOMAIN_ID.getParamsKey();
        data[3] = (byte) length;
        for (int i = 0; i < dataBytes.length; i++) {
            data[i + 4] = dataBytes[i];
        }
        response.responseValue = data;
    }

    public void setWifiEapVerifyServiceEnable(@IntRange(from = 0, to = 1) int enable) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_WIFI_EAP_VERIFY_SERVICE_ENABLE.getParamsKey(),
                (byte) 0x01,
                (byte) enable
        };
        response.responseValue = data;
    }

    public void setNetworkDHCP(@IntRange(from = 0, to = 1) int enable) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_NETWORK_DHCP.getParamsKey(),
                (byte) 0x01,
                (byte) enable
        };
        response.responseValue = data;
    }

    public void setNetworkIPInfo(String ip, String sbNetworkMask, String gateway, String dns) {
        byte[] ipBytes = MokoUtils.hex2bytes(ip);
        byte[] sbNetworkMaskBytes = MokoUtils.hex2bytes(sbNetworkMask);
        byte[] gatewayBytes = MokoUtils.hex2bytes(gateway);
        byte[] dnsBytes = MokoUtils.hex2bytes(dns);
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_NETWORK_IP_INFO.getParamsKey(),
                (byte) 0x10,
                (byte) ipBytes[0],
                (byte) ipBytes[1],
                (byte) ipBytes[2],
                (byte) ipBytes[3],
                (byte) sbNetworkMaskBytes[0],
                (byte) sbNetworkMaskBytes[1],
                (byte) sbNetworkMaskBytes[2],
                (byte) sbNetworkMaskBytes[3],
                (byte) gatewayBytes[0],
                (byte) gatewayBytes[1],
                (byte) gatewayBytes[2],
                (byte) gatewayBytes[3],
                (byte) dnsBytes[0],
                (byte) dnsBytes[1],
                (byte) dnsBytes[2],
                (byte) dnsBytes[3],
        };
        response.responseValue = data;
    }

    public void setFilterRSSI(@IntRange(from = -127, to = 0) int rssi) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_FILTER_RSSI.getParamsKey(),
                (byte) 0x01,
                (byte) rssi
        };
        response.responseValue = data;
    }

    public void setFilterRelationship(@IntRange(from = 0, to = 7) int relationship) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_FILTER_RELATIONSHIP.getParamsKey(),
                (byte) 0x01,
                (byte) relationship
        };
        response.responseValue = data;
    }

    public void setFilterMacPrecise(@IntRange(from = 0, to = 1) int enable) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_FILTER_MAC_PRECISE.getParamsKey(),
                (byte) 0x01,
                (byte) enable
        };
        response.responseValue = data;
    }

    public void setFilterMacReverse(@IntRange(from = 0, to = 1) int enable) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_FILTER_MAC_REVERSE.getParamsKey(),
                (byte) 0x01,
                (byte) enable
        };
        response.responseValue = data;
    }

    public void setFilterMacRules(ArrayList<String> filterMacRules) {
        if (filterMacRules == null || filterMacRules.size() == 0) {
            data = new byte[]{
                    (byte) 0xED,
                    (byte) 0x01,
                    (byte) ParamsKeyEnum.KEY_FILTER_MAC_RULES.getParamsKey(),
                    (byte) 0x00
            };
        } else {
            int length = 0;
            for (String mac : filterMacRules) {
                length += 1;
                length += mac.length() / 2;
            }
            data = new byte[4 + length];
            data[0] = (byte) 0xED;
            data[1] = (byte) 0x01;
            data[2] = (byte) ParamsKeyEnum.KEY_FILTER_MAC_RULES.getParamsKey();
            data[3] = (byte) length;
            int index = 0;
            for (int i = 0, size = filterMacRules.size(); i < size; i++) {
                String mac = filterMacRules.get(i);
                byte[] macBytes = MokoUtils.hex2bytes(mac);
                int l = macBytes.length;
                data[4 + index] = (byte) l;
                index++;
                for (int j = 0; j < l; j++, index++) {
                    data[4 + index] = macBytes[j];
                }
            }
        }
        response.responseValue = data;
    }

    public void setFilterNamePrecise(@IntRange(from = 0, to = 1) int enable) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_FILTER_NAME_PRECISE.getParamsKey(),
                (byte) 0x01,
                (byte) enable
        };
        response.responseValue = data;
    }

    public void setFilterNameReverse(@IntRange(from = 0, to = 1) int enable) {
        data = new byte[]{
                (byte) 0xED,
                (byte) 0x01,
                (byte) ParamsKeyEnum.KEY_FILTER_NAME_REVERSE.getParamsKey(),
                (byte) 0x01,
                (byte) enable
        };
        response.responseValue = data;
    }

    public void setFile(ParamsLongKeyEnum key, File file) throws Exception {
        FileInputStream inputSteam = new FileInputStream(file);
        dataBytes = new byte[(int) file.length()];
        inputSteam.read(dataBytes);
        dataLength = dataBytes.length;
        if (dataLength % DATA_LENGTH_MAX > 0) {
            packetCount = dataLength / DATA_LENGTH_MAX + 1;
        } else {
            packetCount = dataLength / DATA_LENGTH_MAX;
        }
        remainPack = packetCount - 1;
        packetIndex = 0;
        delayTime = DEFAULT_DELAY_TIME + 500 * packetCount;
        if (packetCount > 1) {
            data = new byte[DATA_LENGTH_MAX + 6];
            data[0] = (byte) 0xEE;
            data[1] = (byte) 0x01;
            data[2] = (byte) key.getParamsKey();
            data[3] = (byte) packetCount;
            data[4] = (byte) packetIndex;
            data[5] = (byte) DATA_LENGTH_MAX;
            for (int i = 0; i < DATA_LENGTH_MAX; i++, dataOrigin++) {
                data[i + 6] = dataBytes[dataOrigin];
            }
        } else {
            data = new byte[dataLength + 6];
            data[0] = (byte) 0xEE;
            data[1] = (byte) 0x01;
            data[2] = (byte) key.getParamsKey();
            data[3] = (byte) packetCount;
            data[4] = (byte) packetIndex;
            data[5] = (byte) dataLength;
            for (int i = 0; i < dataLength; i++) {
                data[i + 6] = dataBytes[i];
            }
        }
    }


    public void setFilterNameRules(ArrayList<String> filterNameRules) {
        int length = 0;
        for (String name : filterNameRules) {
            length += 1;
            length += name.length();
        }
        dataBytes = new byte[length];
        int index = 0;
        for (int i = 0, size = filterNameRules.size(); i < size; i++) {
            String name = filterNameRules.get(i);
            byte[] nameBytes = name.getBytes();
            int l = nameBytes.length;
            dataBytes[index] = (byte) l;
            index++;
            for (int j = 0; j < l; j++, index++) {
                dataBytes[index] = nameBytes[j];
            }
        }
        dataLength = dataBytes.length;
        if (dataLength != 0) {
            if (dataLength % DATA_LENGTH_MAX > 0) {
                packetCount = dataLength / DATA_LENGTH_MAX + 1;
            } else {
                packetCount = dataLength / DATA_LENGTH_MAX;
            }
        } else {
            packetCount = 1;
        }
        remainPack = packetCount - 1;
        packetIndex = 0;
        delayTime = DEFAULT_DELAY_TIME + 500 * packetCount;
        if (packetCount > 1) {
            data = new byte[DATA_LENGTH_MAX + 6];
            data[0] = (byte) 0xEE;
            data[1] = (byte) 0x01;
            data[2] = (byte) ParamsLongKeyEnum.KEY_FILTER_NAME_RULES.getParamsKey();
            data[3] = (byte) packetCount;
            data[4] = (byte) packetIndex;
            data[5] = (byte) DATA_LENGTH_MAX;
            for (int i = 0; i < DATA_LENGTH_MAX; i++, dataOrigin++) {
                data[i + 6] = dataBytes[dataOrigin];
            }
        } else {
            data = new byte[dataLength + 6];
            data[0] = (byte) 0xEE;
            data[1] = (byte) 0x01;
            data[2] = (byte) ParamsLongKeyEnum.KEY_FILTER_NAME_RULES.getParamsKey();
            data[3] = (byte) packetCount;
            data[4] = (byte) packetIndex;
            data[5] = (byte) dataLength;
            for (int i = 0; i < dataLength; i++) {
                data[i + 6] = dataBytes[i];
            }
        }
    }

    public void setLongChar(ParamsLongKeyEnum key, String character) {
        dataBytes = character.getBytes();
        dataLength = dataBytes.length;
        if (dataLength != 0) {
            if (dataLength % DATA_LENGTH_MAX > 0) {
                packetCount = dataLength / DATA_LENGTH_MAX + 1;
            } else {
                packetCount = dataLength / DATA_LENGTH_MAX;
            }
        } else {
            packetCount = 1;
        }
        remainPack = packetCount - 1;
        packetIndex = 0;
        delayTime = DEFAULT_DELAY_TIME + 500 * packetCount;
        if (packetCount > 1) {
            data = new byte[DATA_LENGTH_MAX + 6];
            data[0] = (byte) 0xEE;
            data[1] = (byte) 0x01;
            data[2] = (byte) key.getParamsKey();
            data[3] = (byte) packetCount;
            data[4] = (byte) packetIndex;
            data[5] = (byte) DATA_LENGTH_MAX;
            for (int i = 0; i < DATA_LENGTH_MAX; i++, dataOrigin++) {
                data[i + 6] = dataBytes[dataOrigin];
            }
        } else {
            data = new byte[dataLength + 6];
            data[0] = (byte) 0xEE;
            data[1] = (byte) 0x01;
            data[2] = (byte) key.getParamsKey();
            data[3] = (byte) 0x01;
            data[4] = (byte) packetIndex;
            data[5] = (byte) dataLength;
            for (int i = 0; i < dataLength; i++) {
                data[i + 6] = dataBytes[i];
            }
        }
    }

    private int packetCount;
    private int packetIndex;
    private int remainPack;
    private int dataLength;
    private int dataOrigin;
    private byte[] dataBytes;
    private String dataBytesStr = "";
    private static final int DATA_LENGTH_MAX = 238;

    @Override
    public boolean parseValue(byte[] value) {
        final int header = value[0] & 0xFF;
        final int flag = value[1] & 0xFF;
        if (header == 0xED)
            return true;
        if (flag == 0x01) {
            final int cmd = value[2] & 0xFF;
            final int result = value[4] & 0xFF;
            if (result == 1) {
                remainPack--;
                packetIndex++;
                if (remainPack >= 0) {
                    assembleRemainData(cmd);
                    return false;
                }
                return true;
            }
        } else {
            final int cmd = value[2] & 0xFF;
            final int packetCount = value[3] & 0xFF;
            final int indexPack = value[4] & 0xFF;
            final int length = value[5] & 0xFF;
            if (indexPack < (packetCount - 1)) {
                byte[] remainBytes = Arrays.copyOfRange(value, 6, 6 + length);
                dataBytesStr += MokoUtils.bytesToHexString(remainBytes);
            } else {
                if (length == 0){
                    data = new byte[5];
                    data[0] = (byte) 0xEE;
                    data[1] = (byte) 0x00;
                    data[2] = (byte) cmd;
                    data[3] = 0;
                    data[4] = 0;
                    response.responseValue = data;
                    orderStatus = ORDER_STATUS_SUCCESS;
                    MokoSupport.getInstance().pollTask();
                    MokoSupport.getInstance().executeTask();
                    MokoSupport.getInstance().orderResult(response);
                    return false;
                }
                byte[] remainBytes = Arrays.copyOfRange(value, 6, 6 + length);
                dataBytesStr += MokoUtils.bytesToHexString(remainBytes);
                dataBytes = MokoUtils.hex2bytes(dataBytesStr);
                dataLength = dataBytes.length;
                byte[] dataLengthBytes = MokoUtils.toByteArray(dataLength, 2);
                data = new byte[dataLength + 5];
                data[0] = (byte) 0xEE;
                data[1] = (byte) 0x00;
                data[2] = (byte) cmd;
                data[3] = dataLengthBytes[0];
                data[4] = dataLengthBytes[1];
                for (int i = 0; i < dataLength; i++) {
                    data[i + 5] = dataBytes[i];
                }
                response.responseValue = data;
                orderStatus = ORDER_STATUS_SUCCESS;
                MokoSupport.getInstance().pollTask();
                MokoSupport.getInstance().executeTask();
                MokoSupport.getInstance().orderResult(response);
                dataBytesStr = "";
            }
        }
        return false;
    }

    private void assembleRemainData(int cmd) {
        int length = dataLength - dataOrigin;
        if (length > DATA_LENGTH_MAX) {
            data = new byte[DATA_LENGTH_MAX + 6];
            data[0] = (byte) 0xEE;
            data[1] = (byte) 0x01;
            data[2] = (byte) cmd;
            data[3] = (byte) packetCount;
            data[4] = (byte) packetIndex;
            data[5] = (byte) DATA_LENGTH_MAX;
            for (int i = 0; i < DATA_LENGTH_MAX; i++, dataOrigin++) {
                data[i + 6] = dataBytes[dataOrigin];
            }
        } else {
            data = new byte[length + 6];
            data[0] = (byte) 0xEE;
            data[1] = (byte) 0x01;
            data[2] = (byte) cmd;
            data[3] = (byte) packetCount;
            data[4] = (byte) packetIndex;
            data[5] = (byte) length;
            for (int i = 0; i < length; i++, dataOrigin++) {
                data[i + 6] = dataBytes[dataOrigin];
            }
        }
        MokoSupport.getInstance().sendDirectOrder(this);
    }
}
