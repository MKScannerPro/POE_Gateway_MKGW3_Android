package com.moko.mkgw3.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.moko.mkgw3.entity.MokoDevice;

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

    public long insertDevice(MokoDevice mokoDevice) {
        ContentValues cv = new ContentValues();
        cv.put(MKgw3DBConstants.DEVICE_FIELD_NAME, mokoDevice.name);
        cv.put(MKgw3DBConstants.DEVICE_FIELD_MAC, mokoDevice.mac);
        cv.put(MKgw3DBConstants.DEVICE_FIELD_MQTT_INFO, mokoDevice.mqttInfo);
        cv.put(MKgw3DBConstants.DEVICE_FIELD_DEVICE_TYPE, mokoDevice.deviceType);
        cv.put(MKgw3DBConstants.DEVICE_FIELD_LWT_ENABLE, mokoDevice.lwtEnable);
        cv.put(MKgw3DBConstants.DEVICE_FIELD_LWT_TOPIC, mokoDevice.lwtTopic);
        cv.put(MKgw3DBConstants.DEVICE_FIELD_TOPIC_PUBLISH, mokoDevice.topicPublish);
        cv.put(MKgw3DBConstants.DEVICE_FIELD_TOPIC_SUBSCRIBE, mokoDevice.topicSubscribe);
        cv.put(MKgw3DBConstants.DEVICE_NETWORK_TYPE, mokoDevice.networkType);
        long row = db.insert(MKgw3DBConstants.TABLE_NAME_DEVICE, null, cv);
        return row;
    }

    @SuppressLint("Range")
    public ArrayList<MokoDevice> selectAllDevice() {
        Cursor cursor = db.query(MKgw3DBConstants.TABLE_NAME_DEVICE, null, null, null,
                null, null, MKgw3DBConstants.DEVICE_FIELD_ID + " DESC");
        ArrayList<MokoDevice> mokoDevices = new ArrayList<>();
        while (cursor.moveToNext()) {
            MokoDevice mokoDevice = new MokoDevice();
            mokoDevice.id = cursor.getInt(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_ID));
            mokoDevice.name = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_NAME));
            mokoDevice.mac = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_MAC));
            mokoDevice.mqttInfo = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_MQTT_INFO));
            mokoDevice.deviceType = cursor.getInt(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_DEVICE_TYPE));
            mokoDevice.lwtEnable = cursor.getInt(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_LWT_ENABLE));
            mokoDevice.lwtTopic = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_LWT_TOPIC));
            mokoDevice.topicPublish = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_TOPIC_PUBLISH));
            mokoDevice.topicSubscribe = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_TOPIC_SUBSCRIBE));
            mokoDevice.networkType = cursor.getInt(cursor.getColumnIndex(MKgw3DBConstants.DEVICE_NETWORK_TYPE));
            mokoDevices.add(mokoDevice);
        }
        return mokoDevices;
    }

    @SuppressLint("Range")
    public MokoDevice selectDevice(String mac) {
        Cursor cursor = db.query(MKgw3DBConstants.TABLE_NAME_DEVICE, null, MKgw3DBConstants.DEVICE_FIELD_MAC + " = ?", new String[]{mac}, null, null, null);
        MokoDevice mokoDevice = null;
        while (cursor.moveToFirst()) {
            mokoDevice = new MokoDevice();
            mokoDevice.id = cursor.getInt(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_ID));
            mokoDevice.name = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_NAME));
            mokoDevice.mac = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_MAC));
            mokoDevice.mqttInfo = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_MQTT_INFO));
            mokoDevice.deviceType = cursor.getInt(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_DEVICE_TYPE));
            mokoDevice.lwtEnable = cursor.getInt(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_LWT_ENABLE));
            mokoDevice.lwtTopic = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_LWT_TOPIC));
            mokoDevice.topicPublish = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_TOPIC_PUBLISH));
            mokoDevice.topicSubscribe = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_TOPIC_SUBSCRIBE));
            mokoDevice.networkType = cursor.getInt(cursor.getColumnIndex(MKgw3DBConstants.DEVICE_NETWORK_TYPE));
            break;
        }
        return mokoDevice;
    }

    @SuppressLint("Range")
    public MokoDevice selectDeviceByMac(String mac) {
        Cursor cursor = db.query(MKgw3DBConstants.TABLE_NAME_DEVICE, null, MKgw3DBConstants.DEVICE_FIELD_MAC + " = ?", new String[]{mac}, null, null, null);
        MokoDevice mokoDevice = null;
        while (cursor.moveToFirst()) {
            mokoDevice = new MokoDevice();
            mokoDevice.id = cursor.getInt(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_ID));
            mokoDevice.name = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_NAME));
            mokoDevice.mac = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_MAC));
            mokoDevice.mqttInfo = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_MQTT_INFO));
            mokoDevice.deviceType = cursor.getInt(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_DEVICE_TYPE));
            mokoDevice.lwtEnable = cursor.getInt(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_LWT_ENABLE));
            mokoDevice.lwtTopic = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_LWT_TOPIC));
            mokoDevice.topicPublish = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_TOPIC_PUBLISH));
            mokoDevice.topicSubscribe = cursor.getString(cursor
                    .getColumnIndex(MKgw3DBConstants.DEVICE_FIELD_TOPIC_SUBSCRIBE));
            mokoDevice.networkType = cursor.getInt(cursor.getColumnIndex(MKgw3DBConstants.DEVICE_NETWORK_TYPE));
            break;
        }
        return mokoDevice;
    }


    public void updateDevice(MokoDevice mokoDevice) {
        String where = MKgw3DBConstants.DEVICE_FIELD_MAC + " = ?";
        String[] whereValue = {mokoDevice.mac};
        ContentValues cv = new ContentValues();
        cv.put(MKgw3DBConstants.DEVICE_FIELD_NAME, mokoDevice.name);
        cv.put(MKgw3DBConstants.DEVICE_FIELD_MAC, mokoDevice.mac);
        cv.put(MKgw3DBConstants.DEVICE_FIELD_MQTT_INFO, mokoDevice.mqttInfo);
        cv.put(MKgw3DBConstants.DEVICE_FIELD_LWT_ENABLE, mokoDevice.lwtEnable);
        cv.put(MKgw3DBConstants.DEVICE_FIELD_LWT_TOPIC, mokoDevice.lwtTopic);
        cv.put(MKgw3DBConstants.DEVICE_FIELD_TOPIC_PUBLISH, mokoDevice.topicPublish);
        cv.put(MKgw3DBConstants.DEVICE_FIELD_TOPIC_SUBSCRIBE, mokoDevice.topicSubscribe);
        cv.put(MKgw3DBConstants.DEVICE_FIELD_DEVICE_TYPE, mokoDevice.deviceType);
        cv.put(MKgw3DBConstants.DEVICE_NETWORK_TYPE, mokoDevice.networkType);
        db.update(MKgw3DBConstants.TABLE_NAME_DEVICE, cv, where, whereValue);
    }

    public void deleteAllData() {
        db.delete(MKgw3DBConstants.TABLE_NAME_DEVICE, null, null);
    }

    public void deleteDevice(MokoDevice device) {
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
