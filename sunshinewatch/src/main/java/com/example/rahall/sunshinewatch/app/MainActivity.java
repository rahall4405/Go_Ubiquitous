package com.example.rahall.sunshinewatch.app;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WatchViewStub;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;


import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi.DataListener;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;

public class MainActivity extends WearableActivity implements DataListener,
        ConnectionCallbacks, OnConnectionFailedListener {

    GoogleApiClient mGoogleApiClient;
    private static final String TAG = "MainActcivty";
    private static final String PATH_ACTIVITY_DATA = "/sunshine/ActivityData";
    ArrayList<String> mWeatherData;
    WatchViewStub mStub;
    WearableListView mWearableListView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"Got to onCreate");
        setContentView(R.layout.activity_main);


        mWearableListView =
                (WearableListView) findViewById(R.id.wearable_List);

        /*stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
            }
        });*/
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        Log.d(TAG,"Finished onCreate");
    }
    @Override
    protected  void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
        sendMessage();
        //DigitalWatchFaceUtil.requestNewWeatherData(mGoogleApiClient);
    }
    @Override
    protected  void onPause() {
        super.onPause();
        if ((mGoogleApiClient != null) && mGoogleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);

        }

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected: " + "");
        DigitalWatchFaceUtil.requestNewWeatherData(mGoogleApiClient);
        Wearable.DataApi.addListener(mGoogleApiClient, this);


    }

    @Override
    public void onConnectionSuspended(int cause) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnectionSuspended: " + cause);
        }

    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "Got to onDataChanged activity");
        for (DataEvent dataEvent : dataEvents) {
            if (dataEvent.getType() != DataEvent.TYPE_CHANGED) {
                continue;
            }

            String path = dataEvent.getDataItem().getUri().getPath();
            if ("/sunshine/ActivityData".equals(path)) {
                DataItem dataItem = dataEvent.getDataItem();
                DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                DataMap config = dataMapItem.getDataMap();
                mWeatherData = new ArrayList<>();
                mWeatherData = config.getStringArrayList("WeatherData");

                mWearableListView.setAdapter(new WeatherAdapter(this, mWeatherData));
            }

        }

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnectionFailed: " + result);
        }
    }


private void sendMessage() {
    new Thread( new Runnable() {

        @Override
        public void run() {
            NodeApi.GetConnectedNodesResult nodes =
                    Wearable.NodeApi.getConnectedNodes( mGoogleApiClient ).await();
            for(Node node : nodes.getNodes()) {

                Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), DigitalWatchFaceUtil.NEW_WEATHERDATA, null)
                        .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                            @Override
                            public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                                Log.d("Message", "New weatherData:" + sendMessageResult.getStatus());

                            }
                        });
            }
        }
    }).start();
}

}
