package com.moko.support.mkgw3.entity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author: jun.liu
 * @date: 2023/8/31 10:03
 * @des:
 */
public class BxpCInfo implements Parcelable {
    public String mac;
    public int result_code;
    public String result_msg;
    public String product_model;
    public String company_name;
    public String hardware_version;
    public String software_version;
    public String firmware_version;
    public int battery_level;
    public int sensor_state;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mac);
        dest.writeInt(this.result_code);
        dest.writeString(this.result_msg);
        dest.writeString(this.product_model);
        dest.writeString(this.company_name);
        dest.writeString(this.hardware_version);
        dest.writeString(this.software_version);
        dest.writeString(this.firmware_version);
        dest.writeInt(this.battery_level);
        dest.writeInt(this.sensor_state);
    }

    public void readFromParcel(Parcel source) {
        this.mac = source.readString();
        this.result_code = source.readInt();
        this.result_msg = source.readString();
        this.product_model = source.readString();
        this.company_name = source.readString();
        this.hardware_version = source.readString();
        this.software_version = source.readString();
        this.firmware_version = source.readString();
        this.battery_level = source.readInt();
        this.sensor_state = source.readInt();
    }

    public BxpCInfo() {
    }

    protected BxpCInfo(Parcel in) {
        this.mac = in.readString();
        this.result_code = in.readInt();
        this.result_msg = in.readString();
        this.product_model = in.readString();
        this.company_name = in.readString();
        this.hardware_version = in.readString();
        this.software_version = in.readString();
        this.firmware_version = in.readString();
        this.battery_level = in.readInt();
        this.sensor_state = in.readInt();
    }

    public static final Parcelable.Creator<BxpCInfo> CREATOR = new Parcelable.Creator<BxpCInfo>() {
        @Override
        public BxpCInfo createFromParcel(Parcel source) {
            return new BxpCInfo(source);
        }

        @Override
        public BxpCInfo[] newArray(int size) {
            return new BxpCInfo[size];
        }
    };
}
