package com.moko.support.remotegw;

import com.moko.ble.lib.task.OrderTask;
import com.moko.support.remotegw.entity.ParamsKeyEnum;
import com.moko.support.remotegw.entity.ParamsLongKeyEnum;
import com.moko.support.remotegw.task.GetFirmwareRevisionTask;
import com.moko.support.remotegw.task.GetHardwareRevisionTask;
import com.moko.support.remotegw.task.GetManufacturerNameTask;
import com.moko.support.remotegw.task.GetModelNumberTask;
import com.moko.support.remotegw.task.GetSoftwareRevisionTask;
import com.moko.support.remotegw.task.ParamsTask;
import com.moko.support.remotegw.task.SetPasswordTask;

import java.io.File;
import java.util.ArrayList;

import androidx.annotation.IntRange;

public class OrderTaskAssembler {
    ///////////////////////////////////////////////////////////////////////////
    // READ
    ///////////////////////////////////////////////////////////////////////////
    public static OrderTask getManufacturer() {
        GetManufacturerNameTask getManufacturerTask = new GetManufacturerNameTask();
        return getManufacturerTask;
    }

    public static OrderTask getDeviceModel() {
        GetModelNumberTask getDeviceModelTask = new GetModelNumberTask();
        return getDeviceModelTask;
    }

    public static OrderTask getHardwareVersion() {
        GetHardwareRevisionTask getHardwareVersionTask = new GetHardwareRevisionTask();
        return getHardwareVersionTask;
    }

    public static OrderTask getFirmwareVersion() {
        GetFirmwareRevisionTask getFirmwareVersionTask = new GetFirmwareRevisionTask();
        return getFirmwareVersionTask;
    }

    public static OrderTask getSoftwareVersion() {
        GetSoftwareRevisionTask getSoftwareVersionTask = new GetSoftwareRevisionTask();
        return getSoftwareVersionTask;
    }

    public static OrderTask getDeviceName() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_DEVICE_NAME);
        return task;
    }
//
//    public static OrderTask getProductModel() {
//        ParamsTask task = new ParamsTask();
//        task.setData(ParamsKeyEnum.KEY_PRODUCT_MODEL);
//        return task;
//    }

//    public static OrderTask getHardwareVersion() {
//        ParamsTask task = new ParamsTask();
//        task.setData(ParamsKeyEnum.KEY_HARDWARE_VERSION);
//        return task;
//    }
//
//    public static OrderTask getManufacturer() {
//        ParamsTask task = new ParamsTask();
//        task.setData(ParamsKeyEnum.KEY_MANUFACTURER);
//        return task;
//    }

    public static OrderTask getBleMac() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_BLE_MAC);
        return task;
    }

    public static OrderTask getWifiMac() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_WIFI_MAC);
        return task;
    }

    public static OrderTask getResetParamsType() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_RESET_PARAMS_TYPE);
        return task;
    }

    public static OrderTask getProductTestButtonState() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_PRODUCT_TEST_BUTTON_STATE);
        return task;
    }

    public static OrderTask getProductTestDeviceState() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_PRODUCT_TEST_DEVICE_STATE);
        return task;
    }

    public static OrderTask getIndicatorSwitch() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_INDICATOR_SWITCH);
        return task;
    }

    public static OrderTask getNtpEnable() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_NTP_ENABLE);
        return task;
    }

    public static OrderTask getNtpUrl() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_NTP_URL);
        return task;
    }

    public static OrderTask getTimezone() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_NTP_TIME_ZONE);
        return task;
    }

    public static OrderTask getMQTTHost() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_HOST);
        return task;
    }

    public static OrderTask getMQTTPort() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_PORT);
        return task;
    }

    public static OrderTask getMQTTClientId() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_CLIENT_ID);
        return task;
    }

    public static OrderTask getMQTTCleanSession() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_CLEAN_SESSION);
        return task;
    }

    public static OrderTask getMQTTKeepAlive() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_KEEP_ALIVE);
        return task;
    }

    public static OrderTask getMQTTQos() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_QOS);
        return task;
    }

    public static OrderTask getMQTTSubscribeTopic() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_SUBSCRIBE_TOPIC);
        return task;
    }

    public static OrderTask getMQTTPublishTopic() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_PUBLISH_TOPIC);
        return task;
    }

    public static OrderTask getMQTTLwtEnable() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_LWT_ENABLE);
        return task;
    }

    public static OrderTask getMQTTLwtQos() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_LWT_QOS);
        return task;
    }

    public static OrderTask getMQTTLwtRetain() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_LWT_RETAIN);
        return task;
    }

    public static OrderTask getMQTTLwtTopic() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_LWT_TOPIC);
        return task;
    }

    public static OrderTask getMQTTLwtPayload() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_LWT_PAYLOAD);
        return task;
    }


    public static OrderTask getMQTTConnectMode() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_MQTT_CONNECT_MODE);
        return task;
    }

    public static OrderTask getMQTTUsername() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsLongKeyEnum.KEY_MQTT_USERNAME);
        return task;
    }

    public static OrderTask getMQTTPassword() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsLongKeyEnum.KEY_MQTT_PASSWORD);
        return task;
    }

    public static OrderTask getWifiSecurityType() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_WIFI_SECURITY_TYPE);
        return task;
    }

    public static OrderTask getWifiSSID() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_WIFI_SSID);
        return task;
    }

    public static OrderTask getWifiPassword() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_WIFI_PASSWORD);
        return task;
    }

    public static OrderTask getWifiEapType() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_WIFI_EAP_TYPE);
        return task;
    }

    public static OrderTask getWifiEapUsername() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_WIFI_EAP_USERNAME);
        return task;
    }

    public static OrderTask getWifiEapPassword() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_WIFI_EAP_PASSWORD);
        return task;
    }

    public static OrderTask getWifiEapDomainId() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_WIFI_EAP_DOMAIN_ID);
        return task;
    }

    public static OrderTask getWifiEapVerifyServiceEnable() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_WIFI_EAP_VERIFY_SERVICE_ENABLE);
        return task;
    }

    public static OrderTask getNetworkDHCP() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_NETWORK_DHCP);
        return task;
    }

    public static OrderTask getNetworkIPInfo() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_NETWORK_IP_INFO);
        return task;
    }

    public static OrderTask getFilterRSSI() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_FILTER_RSSI);
        return task;
    }

    public static OrderTask getFilterRelationship() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_FILTER_RELATIONSHIP);
        return task;
    }

    public static OrderTask getFilterMacPrecise() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_FILTER_MAC_PRECISE);
        return task;
    }

    public static OrderTask getFilterMacReverse() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_FILTER_MAC_REVERSE);
        return task;
    }

    public static OrderTask getFilterMacRules() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_FILTER_MAC_RULES);
        return task;
    }

    public static OrderTask getFilterNamePrecise() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_FILTER_NAME_PRECISE);
        return task;
    }

    public static OrderTask getFilterNameReverse() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsKeyEnum.KEY_FILTER_NAME_REVERSE);
        return task;
    }

    public static OrderTask getFilterNameRules() {
        ParamsTask task = new ParamsTask();
        task.setData(ParamsLongKeyEnum.KEY_FILTER_NAME_RULES);
        return task;
    }


    ///////////////////////////////////////////////////////////////////////////
    // WRITE
    ///////////////////////////////////////////////////////////////////////////

    public static OrderTask reboot() {
        ParamsTask task = new ParamsTask();
        task.reboot();
        return task;
    }

    public static OrderTask exitConfigMode() {
        ParamsTask task = new ParamsTask();
        task.exitConfigMode();
        return task;
    }

    public static OrderTask setPassword(String password) {
        SetPasswordTask task = new SetPasswordTask();
        task.setData(password);
        return task;
    }

    public static OrderTask changePassword(String password) {
        ParamsTask task = new ParamsTask();
        task.changePassword(password);
        return task;
    }

    public static OrderTask resetParamsType(@IntRange(from = 0, to = 2) int type) {
        ParamsTask task = new ParamsTask();
        task.resetParamsType(type);
        return task;
    }

    public static OrderTask setDeviceName(String deviceName) {
        ParamsTask task = new ParamsTask();
        task.setDeviceName(deviceName);
        return task;
    }

    public static OrderTask setProductModel(String productModel) {
        ParamsTask task = new ParamsTask();
        task.setProductModel(productModel);
        return task;
    }

    public static OrderTask setHardwareVersion(String hardwareVersion) {
        ParamsTask task = new ParamsTask();
        task.setHardwareVersion(hardwareVersion);
        return task;
    }

    public static OrderTask setManufacturer(String manufacturer) {
        ParamsTask task = new ParamsTask();
        task.setManufacturer(manufacturer);
        return task;
    }

    public static OrderTask setProductTestMode() {
        ParamsTask task = new ParamsTask();
        task.setProductTestMode();
        return task;
    }

    public static OrderTask setProductTestDeviceState(@IntRange(from = 0, to = 2) int state) {
        ParamsTask task = new ParamsTask();
        task.setProductTestDeviceState(state);
        return task;
    }

    public static OrderTask resetParams() {
        ParamsTask task = new ParamsTask();
        task.resetParams();
        return task;
    }

    public static OrderTask setIndicatorSwitch(@IntRange(from = 0, to = 15) int enable) {
        ParamsTask task = new ParamsTask();
        task.setIndicatorSwitch(enable);
        return task;
    }

    public static OrderTask setNtpEnable(@IntRange(from = 0, to = 1) int enable) {
        ParamsTask task = new ParamsTask();
        task.setNtpEnable(enable);
        return task;
    }

    public static OrderTask setNtpUrl(String url) {
        ParamsTask task = new ParamsTask();
        task.setNtpUrl(url);
        return task;
    }

    public static OrderTask setTimezone(@IntRange(from = -24, to = 28) int timezone) {
        ParamsTask task = new ParamsTask();
        task.setTimezone(timezone);
        return task;
    }

    public static OrderTask setMqttHost(String host) {
        ParamsTask task = new ParamsTask();
        task.setMqttHost(host);
        return task;
    }

    public static OrderTask setMqttPort(@IntRange(from = 1, to = 65535) int port) {
        ParamsTask task = new ParamsTask();
        task.setMqttPort(port);
        return task;
    }

    public static OrderTask setMqttClientId(String clientId) {
        ParamsTask task = new ParamsTask();
        task.setMqttClientId(clientId);
        return task;
    }

    public static OrderTask setMqttCleanSession(@IntRange(from = 0, to = 1) int enable) {
        ParamsTask task = new ParamsTask();
        task.setMqttCleanSession(enable);
        return task;
    }

    public static OrderTask setMqttKeepAlive(@IntRange(from = 10, to = 120) int keepAlive) {
        ParamsTask task = new ParamsTask();
        task.setMqttKeepAlive(keepAlive);
        return task;
    }

    public static OrderTask setMqttQos(@IntRange(from = 0, to = 2) int qos) {
        ParamsTask task = new ParamsTask();
        task.setMqttQos(qos);
        return task;
    }

    public static OrderTask setMqttSubscribeTopic(String topic) {
        ParamsTask task = new ParamsTask();
        task.setMqttSubscribeTopic(topic);
        return task;
    }

    public static OrderTask setMqttPublishTopic(String topic) {
        ParamsTask task = new ParamsTask();
        task.setMqttPublishTopic(topic);
        return task;
    }

    public static OrderTask setMqttLwtEnable(@IntRange(from = 0, to = 1) int enable) {
        ParamsTask task = new ParamsTask();
        task.setMqttLwtEnable(enable);
        return task;
    }

    public static OrderTask setMqttLwtQos(@IntRange(from = 0, to = 2) int qos) {
        ParamsTask task = new ParamsTask();
        task.setMqttLwtQos(qos);
        return task;
    }

    public static OrderTask setMqttLwtRetain(@IntRange(from = 0, to = 1) int retain) {
        ParamsTask task = new ParamsTask();
        task.setMqttLwtRetain(retain);
        return task;
    }

    public static OrderTask setMqttLwtTopic(String topic) {
        ParamsTask task = new ParamsTask();
        task.setMqttLwtTopic(topic);
        return task;
    }

    public static OrderTask setMqttLwtPayload(String payload) {
        ParamsTask task = new ParamsTask();
        task.setMqttLwtPayload(payload);
        return task;
    }


    public static OrderTask setMqttConnectMode(@IntRange(from = 0, to = 3) int mode) {
        ParamsTask task = new ParamsTask();
        task.setMqttConnectMode(mode);
        return task;
    }

    public static OrderTask setWifiSecurityType(@IntRange(from = 0, to = 1) int type) {
        ParamsTask task = new ParamsTask();
        task.setWifiSecurityType(type);
        return task;
    }


    public static OrderTask setWifiSSID(String SSID) {
        ParamsTask task = new ParamsTask();
        task.setWifiSSID(SSID);
        return task;
    }

    public static OrderTask setWifiPassword(String password) {
        ParamsTask task = new ParamsTask();
        task.setWifiPassword(password);
        return task;
    }

    public static OrderTask setWifiEapType(@IntRange(from = 0, to = 2) int type) {
        ParamsTask task = new ParamsTask();
        task.setWifiEapType(type);
        return task;
    }

    public static OrderTask setWifiEapUsername(String username) {
        ParamsTask task = new ParamsTask();
        task.setWifiEapUsername(username);
        return task;
    }

    public static OrderTask setWifiEapPassword(String password) {
        ParamsTask task = new ParamsTask();
        task.setWifiEapPassword(password);
        return task;
    }

    public static OrderTask setWifiEapDomainId(String domainId) {
        ParamsTask task = new ParamsTask();
        task.setWifiEapDomainId(domainId);
        return task;
    }

    public static OrderTask setWifiEapVerifyServiceEnable(@IntRange(from = 0, to = 1) int enable) {
        ParamsTask task = new ParamsTask();
        task.setWifiEapVerifyServiceEnable(enable);
        return task;
    }

    public static OrderTask setNetworkDHCP(@IntRange(from = 0, to = 1) int enable) {
        ParamsTask task = new ParamsTask();
        task.setNetworkDHCP(enable);
        return task;
    }

    public static OrderTask setNetworkIPInfo(String ip, String sbNetworkMask, String gateway, String dns) {
        ParamsTask task = new ParamsTask();
        task.setNetworkIPInfo(ip, sbNetworkMask, gateway, dns);
        return task;
    }

    public static OrderTask setFilterRSSI(@IntRange(from = -127, to = 0) int rssi) {
        ParamsTask task = new ParamsTask();
        task.setFilterRSSI(rssi);
        return task;
    }

    public static OrderTask setFilterRelationship(@IntRange(from = 0, to = 7) int relationship) {
        ParamsTask task = new ParamsTask();
        task.setFilterRelationship(relationship);
        return task;
    }

    public static OrderTask setFilterMacPrecise(@IntRange(from = 0, to = 1) int enable) {
        ParamsTask task = new ParamsTask();
        task.setFilterMacPrecise(enable);
        return task;
    }

    public static OrderTask setFilterMacReverse(@IntRange(from = 0, to = 1) int enable) {
        ParamsTask task = new ParamsTask();
        task.setFilterMacReverse(enable);
        return task;
    }

    public static OrderTask setFilterMacRules(ArrayList<String> filterMacRules) {
        ParamsTask task = new ParamsTask();
        task.setFilterMacRules(filterMacRules);
        return task;
    }

    public static OrderTask setFilterNamePrecise(@IntRange(from = 0, to = 1) int enable) {
        ParamsTask task = new ParamsTask();
        task.setFilterNamePrecise(enable);
        return task;
    }

    public static OrderTask setFilterNameReverse(@IntRange(from = 0, to = 1) int enable) {
        ParamsTask task = new ParamsTask();
        task.setFilterNameReverse(enable);
        return task;
    }

    public static OrderTask setMqttUserName(String username) {
        ParamsTask task = new ParamsTask();
        task.setLongChar(ParamsLongKeyEnum.KEY_MQTT_USERNAME, username);
        return task;
    }

    public static OrderTask setMqttPassword(String password) {
        ParamsTask task = new ParamsTask();
        task.setLongChar(ParamsLongKeyEnum.KEY_MQTT_PASSWORD, password);
        return task;
    }

    public static OrderTask setFilterNameRules(ArrayList<String> filterOtherRules) {
        ParamsTask task = new ParamsTask();
        task.setFilterNameRules(filterOtherRules);
        return task;
    }

    public static OrderTask setCA(File file) throws Exception {
        ParamsTask task = new ParamsTask();
        task.setFile(ParamsLongKeyEnum.KEY_MQTT_CA, file);
        return task;
    }

    public static OrderTask setClientCert(File file) throws Exception {
        ParamsTask task = new ParamsTask();
        task.setFile(ParamsLongKeyEnum.KEY_MQTT_CLIENT_CERT, file);
        return task;
    }

    public static OrderTask setClientKey(File file) throws Exception {
        ParamsTask task = new ParamsTask();
        task.setFile(ParamsLongKeyEnum.KEY_MQTT_CLIENT_KEY, file);
        return task;
    }

    public static OrderTask setWifiCA(File file) throws Exception {
        ParamsTask task = new ParamsTask();
        task.setFile(ParamsLongKeyEnum.KEY_WIFI_CA, file);
        return task;
    }

    public static OrderTask setWifiClientCert(File file) throws Exception {
        ParamsTask task = new ParamsTask();
        task.setFile(ParamsLongKeyEnum.KEY_WIFI_CLIENT_CERT, file);
        return task;
    }

    public static OrderTask setWifiClientKey(File file) throws Exception {
        ParamsTask task = new ParamsTask();
        task.setFile(ParamsLongKeyEnum.KEY_WIFI_CLIENT_KEY, file);
        return task;
    }
}
