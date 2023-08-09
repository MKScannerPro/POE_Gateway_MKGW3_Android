package com.moko.mkremotegw.base;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.SystemClock;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.moko.mkremotegw.activity.GuideActivity;
import com.moko.mkremotegw.dialog.LoadingDialog;
import com.moko.mkremotegw.dialog.LoadingMessageDialog;
import com.moko.support.remotegw.entity.MsgConfigReq;
import com.moko.support.remotegw.entity.MsgDeviceInfo;
import com.moko.support.remotegw.entity.MsgReadReq;
import com.moko.support.remotegw.event.DeviceOnlineEvent;

import org.greenrobot.eventbus.EventBus;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

public abstract class BaseActivity<VM extends ViewBinding> extends FragmentActivity {
    protected VM mBind;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            Intent intent = new Intent(this, GuideActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }
        mBind = getViewBinding();
        setContentView(mBind.getRoot());
        onCreate();
        EventBus.getDefault().register(this);
    }

    protected void onCreate() {
    }

    protected abstract VM getViewBinding();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        XLog.i("onConfigurationChanged...");
        finish();
    }

    // 记录上次页面控件点击时间,屏蔽无效点击事件
    protected long mLastOnClickTime = 0;

    public boolean isWindowLocked() {
        long current = SystemClock.elapsedRealtime();
        if (current - mLastOnClickTime > 500) {
            mLastOnClickTime = current;
            return false;
        } else {
            return true;
        }
    }

    private LoadingDialog mLoadingDialog;

    public void showLoadingProgressDialog() {
        mLoadingDialog = new LoadingDialog();
        mLoadingDialog.show(getSupportFragmentManager());

    }

    public void dismissLoadingProgressDialog() {
        if (mLoadingDialog != null)
            mLoadingDialog.dismissAllowingStateLoss();
    }

    public LoadingMessageDialog mLoadingMessageDialog;

    public void showLoadingMessageDialog(String message) {
        mLoadingMessageDialog = new LoadingMessageDialog();
        mLoadingMessageDialog.setMessage(message);
        mLoadingMessageDialog.show(getSupportFragmentManager());
    }

    public void showLoadingMessageDialog(String message, boolean isAutoDismiss) {
        mLoadingMessageDialog = new LoadingMessageDialog();
        mLoadingMessageDialog.setMessage(message);
        mLoadingMessageDialog.setAutoDismiss(isAutoDismiss);
        mLoadingMessageDialog.show(getSupportFragmentManager());
    }

    public void dismissLoadingMessageDialog() {
        if (mLoadingMessageDialog != null)
            mLoadingMessageDialog.dismissAllowingStateLoss();
    }

    public boolean isWriteStoragePermissionOpen() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isLocationPermissionOpen() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public void offline(DeviceOnlineEvent event, String deviceMac) {
        String mac = event.getMac();
        if (!deviceMac.equals(mac))
            return;
        boolean online = event.isOnline();
        if (!online)
            finish();
    }


    public String assembleReadCommon(int msgId, String mac) {
        MsgReadReq readReq = new MsgReadReq();
        MsgDeviceInfo deviceInfo = new MsgDeviceInfo();
        deviceInfo.mac = mac;
        readReq.device_info = deviceInfo;
        readReq.msg_id = msgId;
        String message = new Gson().toJson(readReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }

    public String assembleWriteCommonData(int msgId, String mac, JsonObject jsonObject) {
        MsgConfigReq<JsonObject> configReq = new MsgConfigReq();
        MsgDeviceInfo deviceInfo = new MsgDeviceInfo();
        deviceInfo.mac = mac;
        configReq.device_info = deviceInfo;
        configReq.msg_id = msgId;
        configReq.data = jsonObject;
        String message = new Gson().toJson(configReq);
        XLog.e("app_to_device--->" + message);
        return message;
    }
}
