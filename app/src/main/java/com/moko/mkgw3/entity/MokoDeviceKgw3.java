package com.moko.mkgw3.entity;


import java.io.Serializable;

public class MokoDeviceKgw3 implements Serializable {

    public int id;
    public String name;
    public String mac;
    public String mqttInfo;
    public int lwtEnable;
    public String lwtTopic;
    public String topicPublish;
    public String topicSubscribe;
    public boolean isOnline;
    public int deviceType;
    public int wifiRssi;
    public int networkType;
    public boolean isSelected;
}
