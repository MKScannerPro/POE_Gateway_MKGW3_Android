package com.moko.mkremotegw.entity;

import com.chad.library.adapter.base.entity.MultiItemEntity;

public class BleOtherChar implements MultiItemEntity {
    public int type;
    public String mac;
    public String serviceUUID;
    public String characteristicUUID;
    public int characteristicProperties;
    // -1表示特征不支持通知
    // 0表示特征通知关闭
    // 1表示特征通知打开
    public int characteristicNotifyStatus;
    public String characteristicPayload;

    @Override
    public int getItemType() {
        return type;
    }
}
