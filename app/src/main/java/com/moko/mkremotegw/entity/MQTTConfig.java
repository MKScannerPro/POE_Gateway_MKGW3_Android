package com.moko.mkremotegw.entity;


import java.io.Serializable;

public class MQTTConfig implements Serializable {
    public String host = "";
    public String port = "";
    public boolean cleanSession = true;
    public int connectMode;
    public int qos = 0;
    public int keepAlive = 60;
    public String clientId = "";
    public String username = "";
    public String password = "";
    public String caPath = "";
    public String clientKeyPath = "";
    public String clientCertPath = "";
    public String topicSubscribe = "";
    public String topicPublish = "";
    public boolean lwtEnable = true;
    public boolean lwtRetain;
    public int lwtQos = 1;
    public String lwtTopic = "";
    public String lwtPayload = "";
    public String deviceName;
    public String staMac;
}
