package com.moko.support.remotegw.event;

public class DeviceOnlineEvent {

    private String mac;
    private boolean online;

    public DeviceOnlineEvent(String mac, boolean online) {
        this.mac = mac;
        this.online = online;
    }

    public boolean isOnline() {
        return online;
    }

    public String getMac() {
        return mac;
    }
}
