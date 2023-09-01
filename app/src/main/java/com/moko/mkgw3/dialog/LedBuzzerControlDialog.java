package com.moko.mkgw3.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.moko.mkgw3.R;
import com.moko.mkgw3.utils.ToastUtils;

/**
 * @author: jun.liu
 * @date: 2023/8/30 18:17
 * @des:
 */
public class LedBuzzerControlDialog extends DialogFragment {
    private final int from;
    private EditText etDuration;
    private EditText etInterval;
    private OnConfirmClickListener listener;

    public LedBuzzerControlDialog(int from) {
        this.from = from;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (null != getDialog() && null != getDialog().getWindow())
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        View layoutView = inflater.inflate(R.layout.dialog_led_buzzer_control, container, false);
        initViews(layoutView);
        return layoutView;
    }

    @SuppressLint("SetTextI18n")
    private void initViews(@NonNull View layoutView) {
        TextView tvDuration = layoutView.findViewById(R.id.tvDuration);
        TextView tvInterval = layoutView.findViewById(R.id.tvInterval);
        TextView tvSure = layoutView.findViewById(R.id.tvSure);
        TextView tvCancel = layoutView.findViewById(R.id.tvCancel);
        etDuration = layoutView.findViewById(R.id.etDuration);
        etInterval = layoutView.findViewById(R.id.etInterval);
        if (from == 0) {
            //led
            tvDuration.setText("Blinking duration");
            tvInterval.setText("Blinking interval");
        } else {
            tvDuration.setText("Ring duration");
            tvInterval.setText("Ring interval");
        }
        tvCancel.setOnClickListener(v -> dismiss());
        tvSure.setOnClickListener(v -> {
            if (TextUtils.isEmpty(etDuration.getText()) || TextUtils.isEmpty(etInterval.getText())) {
                ToastUtils.showToast(requireContext(), "Para Error");
                return;
            }
            int duration = Integer.parseInt(etDuration.getText().toString());
            int interval = Integer.parseInt(etInterval.getText().toString());
            if (duration < 1 || duration > 6000 || interval > 10000) {
                ToastUtils.showToast(requireContext(), "Para Error");
                return;
            }
            if (null != listener) listener.onConfirm(duration, interval, from);
            dismiss();
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initWindow();
    }

    private void initWindow() {
        if (null != getDialog()) {
            Window window = getDialog().getWindow();
            if (null != window) {
                window.getAttributes().width = getResources().getDisplayMetrics().widthPixels-dip2px(requireContext());;
                window.getAttributes().height = WindowManager.LayoutParams.WRAP_CONTENT;
                window.getAttributes().gravity = Gravity.CENTER;
                window.setBackgroundDrawableResource(R.drawable.shape_radius_solid_ffffff);
            }
        }
    }

    private int dip2px(Context context) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (60 * scale + 0.5f);
    }

    public interface OnConfirmClickListener {
        void onConfirm(int duration, int interval, int from);
    }

    public void setOnConfirmClickListener(OnConfirmClickListener listener) {
        this.listener = listener;
    }
}
