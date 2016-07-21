package com.example.rahall.sunshinewatch.app;

import android.app.Activity;
import android.os.Bundle;

import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;


import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

public class DigitalWatchFaceConfigActivity extends Activity {

    private GoogleApiClient mGoogleApiClient;
    private CheckBox mTimeType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_digital_watch_face_config);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(mConnectionCallbacks)
                .build();
        mTimeType = (CheckBox) findViewById(R.id.time_type);
        mTimeType.setOnCheckedChangeListener(mCheckedChangeListener);

    }
    private CompoundButton.OnCheckedChangeListener mCheckedChangeListener =
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    Log.d("Check","Got check changed");
                    DigitalWatchFaceUtil.putTimeType(mGoogleApiClient, isChecked);
                }
            };

    private GoogleApiClient.ConnectionCallbacks mConnectionCallbacks =
            new GoogleApiClient.ConnectionCallbacks() {
                @Override
                public void onConnected(Bundle bundle) {
                    initializeTimeType();
                }

                @Override
                public void onConnectionSuspended(int i) {
                }
            };
    private void initializeTimeType() {
        DigitalWatchFaceUtil.fetchTimeType(mGoogleApiClient,
                new DigitalWatchFaceUtil.FetchTimeTypeCallback() {
                    @Override
                    public void onTimeTypeFetched(boolean continuousSweep) {

                        mTimeType.setChecked(continuousSweep);
                        mTimeType.setEnabled(true);
                    }
                });
    }
    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        mGoogleApiClient.disconnect();
        super.onPause();
    }
}
