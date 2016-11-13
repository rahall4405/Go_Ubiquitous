package com.example.android.sunshine.app.sync;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import static com.example.android.sunshine.app.sync.SunshineSyncAdapter.syncImmediately;

/**
 * Created by rahall4405 on 11/13/16.
 */

public class SunshineWearableListener extends WearableListenerService{
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if(messageEvent.getPath().equals("/sunshine/NewData")) {
            syncImmediately(getApplicationContext());
        }else {
            super.onMessageReceived( messageEvent );
        }
    }
}
