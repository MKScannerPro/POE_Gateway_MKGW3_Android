package com.moko.support.remotegw.entity;


import java.io.Serializable;

public enum ParamsLongKeyEnum implements Serializable {

    KEY_MQTT_USERNAME(0x23),
    KEY_MQTT_PASSWORD(0x24),
    KEY_MQTT_CA(0x30),
    KEY_MQTT_CLIENT_CERT(0x31),
    KEY_MQTT_CLIENT_KEY(0x32),
    KEY_WIFI_CA(0x48),
    KEY_WIFI_CLIENT_CERT(0x49),
    KEY_WIFI_CLIENT_KEY(0x4A),
    KEY_FILTER_NAME_RULES(0x67),
    ;

    private int paramsKey;

    ParamsLongKeyEnum(int paramsKey) {
        this.paramsKey = paramsKey;
    }


    public int getParamsKey() {
        return paramsKey;
    }

    public static ParamsLongKeyEnum fromParamKey(int paramsKey) {
        for (ParamsLongKeyEnum paramsKeyEnum : ParamsLongKeyEnum.values()) {
            if (paramsKeyEnum.getParamsKey() == paramsKey) {
                return paramsKeyEnum;
            }
        }
        return null;
    }
}
