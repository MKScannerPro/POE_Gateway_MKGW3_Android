package com.moko.support.mkgw3.event;

public class MQTTPublishSuccessEvent {
    private String topic;
    private int msgId;

    public MQTTPublishSuccessEvent(String topic, int msgId) {
        this.topic = topic;
        this.msgId = msgId;
    }

    public String getTopic() {
        return topic;
    }

    public int getMsgId() {
        return msgId;
    }
}
