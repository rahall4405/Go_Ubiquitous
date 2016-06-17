package com.example.android.sunshine.app.sync;

import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class WearableUpdateService extends WearableListenerService {
    public static final String NEW_WEATHERDATA = "/sunshine/NewData";
    public WearableUpdateService() {
    }
    @Override
    public void onMessageReceived( MessageEvent messageEvent )
    {
        super.onMessageReceived( messageEvent );

        Log.d("WearableUpdateService", "Message: " + messageEvent.getPath());

        if ( messageEvent.getPath().equals( NEW_WEATHERDATA ) )
        {
            SunshineSyncAdapter.syncImmediately(this);
        }
    }

}
