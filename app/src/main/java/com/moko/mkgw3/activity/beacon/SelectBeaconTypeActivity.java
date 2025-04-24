package com.moko.mkgw3.activity.beacon;

import android.content.Intent;
import android.view.View;

import com.moko.mkgw3.AppConstants;
import com.moko.mkgw3.activity.BleManagerKgw3Activity;
import com.moko.mkgw3.base.BaseActivity;
import com.moko.mkgw3.databinding.ActivitySelectBeaconTypeBinding;
import com.moko.mkgw3.entity.MokoDeviceKgw3;
import com.moko.lib.mqtt.event.MQTTConnectionCompleteEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * @author: jun.liu
 * @date: 2023/8/30 15:28
 * @des:
 */
public class SelectBeaconTypeActivity extends BaseActivity<ActivitySelectBeaconTypeBinding> {
    private MokoDeviceKgw3 mMokoDeviceKgw3;

    @Override
    protected ActivitySelectBeaconTypeBinding getViewBinding() {
        return ActivitySelectBeaconTypeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onCreate() {
        mMokoDeviceKgw3 = (MokoDeviceKgw3) getIntent().getSerializableExtra(AppConstants.EXTRA_KEY_DEVICE);
        mBind.tvBxpB.setOnClickListener(v -> onItemClick(0));
        mBind.tvBxpC.setOnClickListener(v -> onItemClick(1));
        mBind.tvOther.setOnClickListener(v -> onItemClick(2));
    }

    private void onItemClick(int from) {
        Intent intent = new Intent(this, BleManagerKgw3Activity.class);
        intent.putExtra("from", from);
        intent.putExtra(AppConstants.EXTRA_KEY_DEVICE, mMokoDeviceKgw3);
        startActivity(intent);
    }

    public void back(View view){
        finish();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTConnectionCompleteEvent(MQTTConnectionCompleteEvent event) {
    }
}
