package com.moko.support.remotegw.entity;


import java.io.Serializable;

public enum ParamsKeyEnum implements Serializable {
    // DEVICE
    KEY_REBOOT(0x01),
    KEY_EXIT_CONFIG_MODE(0x02),
    KEY_PASSWORD(0x03),
    KEY_RESET_PARAMS_TYPE(0x04),
    KEY_DEVICE_NAME(0x05),
    KEY_PRODUCT_MODEL(0x06),
    KEY_HARDWARE_VERSION(0x07),
    KEY_MANUFACTURER(0x08),
    KEY_BLE_MAC(0x09),
    KEY_WIFI_MAC(0x0A),
    // ====TEST
    KEY_PRODUCT_TEST_MODE(0x0B),
    KEY_PRODUCT_TEST_BUTTON_STATE(0x0C),
    KEY_PRODUCT_TEST_DEVICE_STATE(0x0D),
    KEY_RESET_PARAMS(0x0E),
    // ====
    KEY_INDICATOR_SWITCH(0x0F),
    KEY_NTP_ENABLE(0x10),
    KEY_NTP_URL(0x11),
    KEY_NTP_TIME_ZONE(0x12),
    // MQTT
    KEY_MQTT_HOST(0x20),
    KEY_MQTT_PORT(0x21),
    KEY_MQTT_CLIENT_ID(0x22),
    KEY_MQTT_CLEAN_SESSION(0x25),
    KEY_MQTT_KEEP_ALIVE(0x26),
    KEY_MQTT_QOS(0x27),
    KEY_MQTT_SUBSCRIBE_TOPIC(0x28),
    KEY_MQTT_PUBLISH_TOPIC(0x29),
    KEY_MQTT_LWT_ENABLE(0x2A),
    KEY_MQTT_LWT_QOS(0x2B),
    KEY_MQTT_LWT_RETAIN(0x2C),
    KEY_MQTT_LWT_TOPIC(0x2D),
    KEY_MQTT_LWT_PAYLOAD(0x2E),
    KEY_MQTT_CONNECT_MODE(0x2F),
    // WIFI
    KEY_WIFI_SECURITY_TYPE(0x40),
    KEY_WIFI_SSID(0x41),
    KEY_WIFI_PASSWORD(0x42),
    KEY_WIFI_EAP_TYPE(0x43),
    KEY_WIFI_EAP_USERNAME(0x44),
    KEY_WIFI_EAP_PASSWORD(0x45),
    KEY_WIFI_EAP_DOMAIN_ID(0x46),
    KEY_WIFI_EAP_VERIFY_SERVICE_ENABLE(0x47),
    KEY_NETWORK_DHCP(0x4B),
    KEY_NETWORK_IP_INFO(0x4C),
    // OTHER
    KEY_FILTER_RSSI(0x60),
    KEY_FILTER_RELATIONSHIP(0x61),
    KEY_FILTER_MAC_PRECISE(0x62),
    KEY_FILTER_MAC_REVERSE(0x63),
    KEY_FILTER_MAC_RULES(0x64),
    KEY_FILTER_NAME_PRECISE(0x65),
    KEY_FILTER_NAME_REVERSE(0x66),
    ;

    private int paramsKey;

    ParamsKeyEnum(int paramsKey) {
        this.paramsKey = paramsKey;
    }


    public int getParamsKey() {
        return paramsKey;
    }

    public static ParamsKeyEnum fromParamKey(int paramsKey) {
        for (ParamsKeyEnum paramsKeyEnum : ParamsKeyEnum.values()) {
            if (paramsKeyEnum.getParamsKey() == paramsKey) {
                return paramsKeyEnum;
            }
        }
        return null;
    }
}
