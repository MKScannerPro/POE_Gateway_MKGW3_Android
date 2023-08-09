package com.moko.support.mkgw3.event;

public class MQTTUnSubscribeFailureEvent {
    private String topic;

    public MQTTUnSubscribeFailureEvent(String topic) {
        this.topic = topic;
    }

    public String getTopic() {
        return topic;
    }
}
