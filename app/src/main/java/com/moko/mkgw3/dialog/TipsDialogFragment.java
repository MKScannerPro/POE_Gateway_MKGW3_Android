package com.moko.mkgw3.dialog;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.moko.mkgw3.R;

/**
 * @author: jun.liu
 * @date: 2023/10/16 17:32
 * @des:
 */
public class TipsDialogFragment extends DialogFragment {
    private String content;
    private TextView tvTips;
    public TipsDialogFragment(){}

    public TipsDialogFragment(String content){
        this.content = content;
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (null != getDialog() && null != getDialog().getWindow())
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        View layoutView = inflater.inflate(R.layout.dialog_tips_gw3, container, false);
        initViews(layoutView);
        return layoutView;
    }

    private void initViews(View layoutView) {
        tvTips = layoutView.findViewById(R.id.tvContent);
        tvTips.setText(content);
        setCancelable(false);
    }

    public void updateContent(String content){
        tvTips.setText(content);
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
                window.getAttributes().width = getResources().getDisplayMetrics().widthPixels - dip2px(requireContext());
                window.getAttributes().height = WindowManager.LayoutParams.WRAP_CONTENT;
                window.getAttributes().gravity = Gravity.CENTER;
                window.setBackgroundDrawableResource(R.drawable.shape_radius_solid_black);
            }
        }
    }

    private int dip2px(Context context) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (120 * scale + 0.5f);
    }
}
