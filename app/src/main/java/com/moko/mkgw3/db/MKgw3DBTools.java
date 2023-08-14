package com.moko.mkgw3.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.moko.mkgw3.entity.MokoDeviceKgw3;

import java.util.ArrayList;

public class MKgw3DBTools {
    private MKgw3DBOpenHelper myMKgw3DBOpenHelper;
    private SQLiteDatabase db;
    private static MKgw3DBTools MKgw3DbTools;

    public static MKgw3DBTools getInstance(Context context) {
        if (MKgw3DbTools == null) {
            MKgw3DbTools = new MKgw3DBTools(context);
            return MKgw3DbTools;
        }
        return MKgw3DbTools;
    }

    public MKgw3DBTools(Context context) {
        myMKgw3DBOpenHelper = new MKgw3DBOpenHelper(context);
        db = myMKgw3DBOpenHelper.getWritableDatabase();
    }

    public long insertDevice(MokoDeviceKgw3 mokoDeviceKgw3) {
        ContentValues cv = new ContentValues();
        cv.put(MKgw3DBConstants.DEVICE_FIELD_NAME, mokoDeviceKgw3.name);
        cv.put(MKgw3DBConstants.DEVICE_FIELD_MAC, mokoDeviceKgw3.mac);
        cv.put(MKgw3DBConstants.DEVICE_FIELD_MQTT_INFO, mokoDeviceKgw3.mqttInfo);
        cv.put(MKgw3DBConstants.DEVICE_FIELD_DEVICE_TYPE, mokoDeviceKgw3.deviceType);
        cv.put(MKgw3DBConstants.DEVICE_FIELD_LWT_ENABLE, mokoDeviceKgw3.lwtEnable);
        cv.put(MKgw3DBConstants.DEVICE_FIELD_LWT_TOPIC, mokoDeviceKgw3.lwtTopic);
        cv.put(MKgw3DBConstants.DEVICE_FIELD_TOPIC_PUBLISH, mokoDeviceKgw3.topicPublish);
        cv.put(MKgw3DBConstants.DEVICE_FIELD_TOPIC_SUBSCRIBE, mokoDeviceKgw3.topicSubscribe);
        cv.put(MKgw3DBConstants.DEVICE_NETWORK_TYPE, mokoDeviceKgw3.networkType);
        long row = db.insert(MKgw3DBConstants.TABLE_NAME_DEVICE, null, cv);
        return row;
    }

    @SuppressLint("Range")
    public ArrayList<MokoDeviceKgw3> selectAllDevice() {
        Cursor cursor = db.query(MKgw3DBConstants.TABLE_NAME_DEVICE, null, null, null,
                null, null, MKgw3DBConstants.DEVICE_FIELD_ID + " DESC");
        ArrayList<MokoDeviceKgw3> mokoDeviceKgw3s = new ArrayList<>();
        while (cursor.moveToNext()) {
            MokoDeviceKgw3 mokoDeviceKgw3 = new MokoDeviceKgw3();
            mokoDeviceKgw3.id = cursor.getInt(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_ID));
            mokoDeviceKgw3.name = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_NAME));
            mokoDeviceKgw3.mac = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_MAC));
            mokoDeviceKgw3.mqttInfo = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_MQTT_INFO));
            mokoDeviceKgw3.deviceType = cursor.getInt(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_DEVICE_TYPE));
            mokoDeviceKgw3.lwtEnable = cursor.getInt(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_LWT_ENABLE));
            mokoDeviceKgw3.lwtTopic = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_LWT_TOPIC));
            mokoDeviceKgw3.topicPublish = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_TOPIC_PUBLISH));
            mokoDeviceKgw3.topicSubscribe = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_TOPIC_SUBSCRIBE));
            mokoDeviceKgw3.networkType = cursor.getInt(cursor.getColumnIndex(MKgw3DBConstants.DEVICE_NETWORK_TYPE));
            mokoDeviceKgw3s.add(mokoDeviceKgw3);
        }
        return mokoDeviceKgw3s;
    }

    @SuppressLint("Range")
    public MokoDeviceKgw3 selectDevice(String mac) {
        Cursor cursor = db.query(MKgw3DBConstants.TABLE_NAME_DEVICE, null, MKgw3DBConstants.DEVICE_FIELD_MAC + " = ?", new String[]{mac}, null, null, null);
        MokoDeviceKgw3 mokoDeviceKgw3 = null;
        while (cursor.moveToFirst()) {
            mokoDeviceKgw3 = new MokoDeviceKgw3();
            mokoDeviceKgw3.id = cursor.getInt(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_ID));
            mokoDeviceKgw3.name = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_NAME));
            mokoDeviceKgw3.mac = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_MAC));
            mokoDeviceKgw3.mqttInfo = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_MQTT_INFO));
            mokoDeviceKgw3.deviceType = cursor.getInt(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_DEVICE_TYPE));
            mokoDeviceKgw3.lwtEnable = cursor.getInt(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_LWT_ENABLE));
            mokoDeviceKgw3.lwtTopic = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_LWT_TOPIC));
            mokoDeviceKgw3.topicPublish = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_TOPIC_PUBLISH));
            mokoDeviceKgw3.topicSubscribe = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_TOPIC_SUBSCRIBE));
            mokoDeviceKgw3.networkType = cursor.getInt(cursor.getColumnIndex(MKgw3DBConstants.DEVICE_NETWORK_TYPE));
            break;
        }
        return mokoDeviceKgw3;
    }

    @SuppressLint("Range")
    public MokoDeviceKgw3 selectDeviceByMac(String mac) {
        Cursor cursor = db.query(MKgw3DBConstants.TABLE_NAME_DEVICE, null, MKgw3DBConstants.DEVICE_FIELD_MAC + " = ?", new String[]{mac}, null, null, null);
        MokoDeviceKgw3 mokoDeviceKgw3 = null;
        while (cursor.moveToFirst()) {
            mokoDeviceKgw3 = new MokoDeviceKgw3();
            mokoDeviceKgw3.id = cursor.getInt(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_ID));
            mokoDeviceKgw3.name = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_NAME));
            mokoDeviceKgw3.mac = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_MAC));
            mokoDeviceKgw3.mqttInfo = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_MQTT_INFO));
            mokoDeviceKgw3.deviceType = cursor.getInt(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_DEVICE_TYPE));
            mokoDeviceKgw3.lwtEnable = cursor.getInt(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_LWT_ENABLE));
            mokoDeviceKgw3.lwtTopic = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_LWT_TOPIC));
            mokoDeviceKgw3.topicPublish = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_TOPIC_PUBLISH));
            mokoDeviceKgw3.topicSubscribe = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_TOPIC_SUBSCRIBE));
            mokoDeviceKgw3.networkType = cursor.getInt(cursor.getColumnIndex(MKgw3DBConstants.DEVICE_NETWORK_TYPE));
            break;
        }
        return mokoDeviceKgw3;
    }


    public void updateDevice(MokoDeviceKgw3 mokoDeviceKgw3) {
        String where = MKgw3DBConstants.DEVICE_FIELD_MAC + " = ?";
        String[] whereValue = {mokoDeviceKgw3.mac};
        ContentValues cv = new ContentValues();
        cv.put(MKgw3DBConstants.DEVICE_FIELD_NAME, mokoDeviceKgw3.name);
        cv.put(MKgw3DBConstants.DEVICE_FIELD_MAC, mokoDeviceKgw3.mac);
        cv.put(MKgw3DBConstants.DEVICE_FIELD_MQTT_INFO, mokoDeviceKgw3.mqttInfo);
        cv.put(MKgw3DBConstants.DEVICE_FIELD_LWT_ENABLE, mokoDeviceKgw3.lwtEnable);
        cv.put(MKgw3DBConstants.DEVICE_FIELD_LWT_TOPIC, mokoDeviceKgw3.lwtTopic);
        cv.put(MKgw3DBConstants.DEVICE_FIELD_TOPIC_PUBLISH, mokoDeviceKgw3.topicPublish);
        cv.put(MKgw3DBConstants.DEVICE_FIELD_TOPIC_SUBSCRIBE, mokoDeviceKgw3.topicSubscribe);
        cv.put(MKgw3DBConstants.DEVICE_FIELD_DEVICE_TYPE, mokoDeviceKgw3.deviceType);
        cv.put(MKgw3DBConstants.DEVICE_NETWORK_TYPE, mokoDeviceKgw3.networkType);
        db.update(MKgw3DBConstants.TABLE_NAME_DEVICE, cv, where, whereValue);
    }

    public void deleteAllData() {
        db.delete(MKgw3DBConstants.TABLE_NAME_DEVICE, null, null);
    }

    public void deleteDevice(MokoDeviceKgw3 device) {
        String where = MKgw3DBConstants.DEVICE_FIELD_MAC + " = ?";
        String[] whereValue = {device.mac + ""};
        db.delete(MKgw3DBConstants.TABLE_NAME_DEVICE, where, whereValue);
    }

    // drop table;
    public void dropTable(String tablename) {
        db.execSQL("DROP TABLE IF EXISTS " + tablename);
    }

    // close database;
    public void close(String databaseName) {
        db.close();
    }

}
