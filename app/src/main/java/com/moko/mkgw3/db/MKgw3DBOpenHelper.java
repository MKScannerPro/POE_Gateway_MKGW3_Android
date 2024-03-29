package com.moko.mkgw3.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.elvishew.xlog.XLog;

public class MKgw3DBOpenHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "MKGW3";
    // 数据库版本号
    private static final int DB_VERSION = 1;

    private Context mContext;

    public MKgw3DBOpenHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_DEVICE);
        Log.i("MKGW3", "创建数据库");
    }

    /**
     * 升级时数据库时调用
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion > oldVersion) {
            XLog.i("数据库升级");
            XLog.i("旧版本:" + oldVersion + ";新版本:" + newVersion);
        }
    }

    /**
     * 删除数据库
     *
     * @param context
     * @return
     */
    public boolean deleteDatabase(Context context) {
        return context.deleteDatabase(DB_NAME);
    }

    // 设备表
    private static final String CREATE_TABLE_DEVICE = "CREATE TABLE "
            + MKgw3DBConstants.TABLE_NAME_DEVICE
            // id
            + " (" + MKgw3DBConstants.DEVICE_FIELD_ID
            + " INTEGER primary key autoincrement, "
            // 名字
            + MKgw3DBConstants.DEVICE_FIELD_NAME + " TEXT,"
            // MAC
            + MKgw3DBConstants.DEVICE_FIELD_MAC + " TEXT,"
            // mqtt信息
            + MKgw3DBConstants.DEVICE_FIELD_MQTT_INFO + " TEXT,"
            // 遗愿开关
            + MKgw3DBConstants.DEVICE_FIELD_LWT_ENABLE + " TEXT,"
            // 遗愿主题
            + MKgw3DBConstants.DEVICE_FIELD_LWT_TOPIC + " TEXT,"
            // 发布主题
            + MKgw3DBConstants.DEVICE_FIELD_TOPIC_PUBLISH + " TEXT,"
            // 订阅主题
            + MKgw3DBConstants.DEVICE_FIELD_TOPIC_SUBSCRIBE + " TEXT,"
            // 设备类型
            + MKgw3DBConstants.DEVICE_FIELD_DEVICE_TYPE + " INTEGER,"
            + MKgw3DBConstants.DEVICE_NETWORK_TYPE + " INTEGER);";

}
