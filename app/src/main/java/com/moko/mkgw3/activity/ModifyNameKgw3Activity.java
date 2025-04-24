package com.moko.mkgw3.activity;

import android.content.Context;
import android.content.Intent;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.moko.mkgw3.AppConstants;
import com.moko.mkgw3.R;
import com.moko.mkgw3.base.BaseActivity;
import com.moko.mkgw3.databinding.ActivityModifyDeviceNameKgw3Binding;
import com.moko.mkgw3.db.MKgw3DBTools;
import com.moko.mkgw3.entity.MokoDeviceKgw3;
import com.moko.lib.scannerui.utils.ToastUtils;
import com.moko.lib.mqtt.event.MQTTConnectionCompleteEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


public class ModifyNameKgw3Activity extends BaseActivity<ActivityModifyDeviceNameKgw3Binding> {
    private final String FILTER_ASCII = "[ -~]*";
    public static String TAG = ModifyNameKgw3Activity.class.getSimpleName();

    private MokoDeviceKgw3 device;

    @Override
    protected void onCreate() {
        device = (MokoDeviceKgw3) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            if (!(source + "").matches(FILTER_ASCII)) {
                return "";
            }

            return null;
        };
        mBind.etNickName.setText(device.name);
        mBind.etNickName.setSelection(mBind.etNickName.getText().toString().length());
        mBind.etNickName.setFilters(new InputFilter[]{filter, new InputFilter.LengthFilter(20)});
        mBind.etNickName.postDelayed(() -> {
            InputMethodManager inputManager = (InputMethodManager) mBind.etNickName.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.showSoftInput(mBind.etNickName, 0);
        }, 300);
    }

    @Override
    protected ActivityModifyDeviceNameKgw3Binding getViewBinding() {
        return ActivityModifyDeviceNameKgw3Binding.inflate(getLayoutInflater());
    }


    public void modifyDone(View view) {
        String name = mBind.etNickName.getText().toString();
        if (TextUtils.isEmpty(name)) {
            ToastUtils.showToast(this, R.string.modify_device_name_empty);
            return;
        }
        device.name = name;
        MKgw3DBTools.getInstance(this).updateDevice(device);
        // 跳转首页，刷新数据
        Intent intent = new Intent(this, MKGW3MainActivity.class);
        intent.putExtra(AppConstants.EXTRA_KEY_FROM_ACTIVITY, TAG);
        intent.putExtra(AppConstants.EXTRA_KEY_MAC, device.mac);
        startActivity(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTConnectionCompleteEvent(MQTTConnectionCompleteEvent event) {
    }
}
