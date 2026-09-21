package com.moko.mkgw3.base;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.DisplayCutout;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;

import com.elvishew.xlog.XLog;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.moko.lib.mqtt.entity.MsgConfigReq;
import com.moko.lib.mqtt.entity.MsgDeviceInfo;
import com.moko.lib.mqtt.entity.MsgReadReq;
import com.moko.lib.mqtt.event.DeviceOnlineEvent;
import com.moko.lib.scannerui.dialog.LoadingDialog;
import com.moko.lib.scannerui.dialog.LoadingMessageDialog;
import com.moko.mkgw3.activity.GuideActivity;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

public abstract class BaseActivity<VM extends ViewBinding> extends FragmentActivity {
    protected VM mBind;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // targetSdk 35+ / Android 16：预测性返回不会再走 Activity.onBackPressed()，
        // 在此统一接管系统返回，并继续回调子类已有的 onBackPressed() 实现。
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                BaseActivity.this.onBackPressed();
            }
        });
        if (savedInstanceState != null) {
            Intent intent = new Intent(this, GuideActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }
        // 设置全屏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
            // 透明导航栏
            getWindow().setNavigationBarColor(Color.TRANSPARENT);

            // Android P及以上支持刘海屏
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(params);

            // 设置WindowInsets监听
            getWindow().getDecorView().setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                private int lastOrientation = -1;

                @Override
                public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                    DisplayCutout cutout = insets.getDisplayCutout();
                    if (cutout != null) {
                        // 获取当前方向
                        int currentOrientation = getResources().getConfiguration().orientation;

                        // 只有当方向改变时才重新设置padding
                        if (currentOrientation != lastOrientation) {
                            lastOrientation = currentOrientation;

                            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                                // 横屏：只考虑左右安全区域
                                v.setPadding(cutout.getSafeInsetLeft(), 0,
                                        cutout.getSafeInsetRight(), 0);
                            } else {
                                int bottomInset = 0;
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                                }
                                // 竖屏：使用全部安全区域
                                v.setPadding(cutout.getSafeInsetLeft(), cutout.getSafeInsetTop(),
                                        cutout.getSafeInsetRight(), cutout.getSafeInsetBottom() + bottomInset);
                            }

                            // 请求重新布局
                            v.requestLayout();
                        }
                    } else {
                        int bottomInset = 0;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                        }
                        // 没有刘海屏时重置padding
                        v.setPadding(0, 0, 0, bottomInset);
                        lastOrientation = -1;
                    }

                    return insets;
                }
            });
        }
        mBind = getViewBinding();
        setContentView(mBind.getRoot());
        onCreate();
        EventBus.getDefault().register(this);
    }

    /**
     * 覆盖 ComponentActivity 默认实现（会再次进 OnBackPressedDispatcher，可能死循环）。
     * 未重写的页面默认 finish；已重写 onBackPressed 的子类仍走各自逻辑。
     */
    @Override
    public void onBackPressed() {
        finish();
    }

    protected void onCreate() {
    }

    protected abstract VM getViewBinding();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (EventBus.getDefault().isRegistered(this))
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
