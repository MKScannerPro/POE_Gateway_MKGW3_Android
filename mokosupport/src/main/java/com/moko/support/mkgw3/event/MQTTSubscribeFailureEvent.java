package com.moko.support.mkgw3.event;

public class MQTTSubscribeFailureEvent {
    private String topic;

    public MQTTSubscribeFailureEvent(String topic) {
        this.topic = topic;
    }

    public String getTopic() {
        return topic;
    }
}
