package com.moko.mkgw3.activity;

import android.content.Intent;
import android.net.Uri;
import android.view.View;

import com.moko.ble.lib.utils.MokoUtils;
import com.moko.mkgw3.R;
import com.moko.mkgw3.base.BaseActivity;
import com.moko.mkgw3.databinding.ActivityAboutBinding;
import com.moko.lib.scannerui.utils.ToastUtils;
import com.moko.mkgw3.utils.Utils;
import com.moko.lib.mqtt.event.MQTTConnectionCompleteEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.Calendar;

public class AboutActivity extends BaseActivity<ActivityAboutBinding> {


    @Override
    protected void onCreate() {
        mBind.tvSoftVersion.setText(getString(R.string.version_info, Utils.getVersionInfo(this)));
    }

    @Override
    protected ActivityAboutBinding getViewBinding() {
        return ActivityAboutBinding.inflate(getLayoutInflater());
    }

    public void openURL(View view) {
        Uri uri = Uri.parse("https://" + getString(R.string.company_website));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    public void back(View view) {
        finish();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMQTTConnectionCompleteEvent(MQTTConnectionCompleteEvent event) {
    }

    public void onFeedbackLog(View view) {
        if (isWindowLocked())
            return;
        File trackerLog = new File(MKGW3MainActivity.PATH_LOGCAT + File.separator + "MKGW3.txt");
        File trackerLogBak = new File(MKGW3MainActivity.PATH_LOGCAT + File.separator + "MKGW3.txt.bak");
        File trackerCrashLog = new File(MKGW3MainActivity.PATH_LOGCAT + File.separator + "crash_log.txt");
        if (!trackerLog.exists() || !trackerLog.canRead()) {
            ToastUtils.showToast(this, "File is not exists!");
            return;
        }
        String address = "feedback@mokotechnology.com";
        StringBuilder mailContent = new StringBuilder("MKGW3_");
        Calendar calendar = Calendar.getInstance();
        String date = MokoUtils.calendar2strDate(calendar, "yyyyMMdd");
        mailContent.append(date);
        String title = mailContent.toString();
        if ((!trackerLogBak.exists() || !trackerLogBak.canRead())
                && (!trackerCrashLog.exists() || !trackerCrashLog.canRead())) {
            Utils.sendEmail(this, address, "", title, "Choose Email Client", trackerLog);
        } else if (!trackerCrashLog.exists() || !trackerCrashLog.canRead()) {
            Utils.sendEmail(this, address, "", title, "Choose Email Client", trackerLog, trackerLogBak);
        } else if (!trackerLogBak.exists() || !trackerLogBak.canRead()) {
            Utils.sendEmail(this, address, "", title, "Choose Email Client", trackerLog, trackerCrashLog);
        } else {
            Utils.sendEmail(this, address, "", title, "Choose Email Client", trackerLog, trackerLogBak, trackerCrashLog);
        }
    }
}
