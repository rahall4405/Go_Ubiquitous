package com.example.rahall.sunshinewatch.app;

import android.content.Context;
import android.support.wearable.view.WearableListView;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * Created by rahall4405 on 7/19/16.
 */
public class SunshineListItemLayout extends LinearLayout implements WearableListView.OnCenterProximityListener {

    private ImageView mImage;
    private static final float NO_ALPHA = 1f, PARTIAL_ALPHA = 0.40f;
    private static final float NO_X_TRANSLATION = 0f, X_TRANSLATION = 20f;
    private float imageSize;

    public SunshineListItemLayout(Context context) {
        super(context);
    }
    public SunshineListItemLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SunshineListItemLayout(Context context, AttributeSet attrs,
                                  int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
//        mImage = (ImageView) findViewById(R.id.wImage);

    }

    @Override
    public void onCenterPosition(boolean b) {
        if (b) {
            animate().alpha(NO_ALPHA).translationX(X_TRANSLATION).start();
        }
    }

    @Override
    public void onNonCenterPosition(boolean b) {
        if (b) {
            animate().alpha(PARTIAL_ALPHA).translationX(NO_X_TRANSLATION).start();
        }

    }
}
