/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.rahall.sunshinewatch.app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

//  adapted from android example wearable

public final class DigitalWatchFaceUtil {


    private static final String TAG = "DigitalWatchFaceUtil";




    /**
     * The path for weather request message
     */
    /**
     * The path for the {@link DataItem} containing {@link DigitalWatchFaceService} configuration.
     */

    public static final String NEW_WEATHERDATA = "/sunshine/NewData";
    public static final String PATH_TIME_TYPE = "/sunshine/TimeType";
    public static final String KEY_TIME_TYPE = "timetype";
    public static final String KEY_BACKGROUND = "background";
    public static final String KEY_BACKGROUND_COLOR = "BACKGROUND_COLOR";
    /**
     * The path for the {@link DataItem} containing {@link DigitalWatchFaceService} configuration.
     */
    public static final String PATH_WITH_FEATURE = "/watch_face_config/Digital";

    /**
     * Name of the default interactive mode background color and the ambient mode background color.
     */
    public static final String COLOR_AMBIENT_BACKGROUND = "Black";
    public static final int COLOR_VALUE_DEFAULT_AND_AMBIENT_BACKGROUND =
            parseColor(COLOR_AMBIENT_BACKGROUND);

    /**
     * Name of the default interactive mode hour digits color and the ambient mode hour digits
     * color.
     */
    public static final String COLOR_NAME_DEFAULT_AND_AMBIENT_HOUR_DIGITS = "White";
    public static final int COLOR_VALUE_DEFAULT_AND_AMBIENT_HOUR_DIGITS =
            parseColor(COLOR_NAME_DEFAULT_AND_AMBIENT_HOUR_DIGITS);

    /**
     * Name of the default interactive mode minute digits color and the ambient mode minute digits
     * color.
     */
    public static final String COLOR_NAME_DEFAULT_AND_AMBIENT_MINUTE_DIGITS = "White";
    public static final int COLOR_VALUE_DEFAULT_AND_AMBIENT_MINUTE_DIGITS =
            parseColor(COLOR_NAME_DEFAULT_AND_AMBIENT_MINUTE_DIGITS);






    private static int parseColor(String colorName) {
        return Color.parseColor(colorName.toLowerCase());
    }











// this is a modified from android wearable example
    public static Bitmap createGrayScaleBackgroundBitmap(Bitmap b) {
        Bitmap mGrayBackgroundBitmap = Bitmap.createBitmap(
                b.getWidth(),
                b.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mGrayBackgroundBitmap);
        Paint grayPaint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
        grayPaint.setColorFilter(filter);

            canvas.drawBitmap(mGrayBackgroundBitmap, 0, 0, grayPaint);
        return mGrayBackgroundBitmap;
    }

    public static int getIconResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.ic_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.ic_rain;
        } else if (weatherId == 511) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.ic_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.ic_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.ic_storm;
        } else if (weatherId == 800) {
            return R.drawable.ic_clear;
        } else if (weatherId == 801) {
            return R.drawable.ic_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.ic_cloudy;
        }
        return R.mipmap.ic_launcher;
    }

    public static void requestNewWeatherData(GoogleApiClient mGoogleApiClient) {
        Wearable.MessageApi.sendMessage(mGoogleApiClient, "", DigitalWatchFaceUtil.NEW_WEATHERDATA, null)
                .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        Log.d("Message", "New weatherData:" + sendMessageResult.getStatus());
                    }
                });

    }
    public static void putTimeType(GoogleApiClient googleApiClient, boolean timeType) {
        Log.d("putTimeType","Got here timeType = " + timeType);
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH_TIME_TYPE).setUrgent();
        putDataMapRequest.getDataMap().putBoolean(KEY_TIME_TYPE, timeType);
        putDataMapRequest.getDataMap().putLong("timestamp",System.currentTimeMillis());
        PutDataRequest putDataReq = putDataMapRequest.asPutDataRequest();
        Log.d("putTimeType","Got here2");
        Wearable.DataApi.putDataItem(googleApiClient, putDataReq).setResultCallback(new ResultCallbacks<DataApi.DataItemResult>() {
            @Override
            public void onSuccess(@NonNull DataApi.DataItemResult dataItemResult) {
                Log.d("Config change","Data sent successfully");
            }

            @Override
            public void onFailure(@NonNull Status status) {
                Log.d("Config change ","Data send failure");
            }
        });
    }

    public static boolean extractTimeType(DataEventBuffer dataEvents) {
        final List<DataEvent> events = FreezableUtils
                .freezeIterable(dataEvents);

        for (DataEvent event : events) {
            if (event.getType() != DataEvent.TYPE_CHANGED) {
                continue;
            }

            DataItem dataItem = event.getDataItem();
            if (!dataItem.getUri().getPath().equals(DigitalWatchFaceUtil.PATH_TIME_TYPE)) {
                continue;
            }

            return extractTimeType(dataItem);
        }

        return false;
    }

    private static boolean extractTimeType(DataItem dataItem) {
        if(dataItem == null) {
            return false;
        }

        DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
        DataMap config = dataMapItem.getDataMap();
        return config.getBoolean(KEY_TIME_TYPE, true);
    }
    public static void fetchTimeType(final GoogleApiClient client,
                                            final FetchTimeTypeCallback callback) {
        Wearable.NodeApi.getLocalNode(client).setResultCallback(
                new ResultCallback<NodeApi.GetLocalNodeResult>() {
                    @Override
                    public void onResult(NodeApi.GetLocalNodeResult localNodeResult) {
                        String localNode = localNodeResult.getNode().getId();
                        fetchTimeTypeDataItem(client, localNode, callback);
                    }
                }
        );
    }

    public interface FetchTimeTypeCallback {
        void onTimeTypeFetched(boolean TimeType);
    }
    private static void fetchTimeTypeDataItem(GoogleApiClient client, String localNode,
                                                     final FetchTimeTypeCallback callback) {
        Uri uri = new Uri.Builder()
                .scheme("wear")
                .path(PATH_TIME_TYPE)
                .authority(localNode)
                .build();

        Wearable.DataApi.getDataItem(client, uri)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        boolean TimeType = extractTimeType(dataItemResult.getDataItem());
                        callback.onTimeTypeFetched(TimeType);
                    }
                });
    }


    public static void putConfigDataItem(GoogleApiClient googleApiClient, int backgroundColor) {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH_WITH_FEATURE);
        putDataMapRequest.getDataMap().putInt(KEY_BACKGROUND, backgroundColor);
        putDataMapRequest.getDataMap().putLong("timestamp",System.currentTimeMillis());
        PutDataRequest putDataReq = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(googleApiClient, putDataReq).setResultCallback(new ResultCallbacks<DataApi.DataItemResult>() {
            @Override
            public void onSuccess(@NonNull DataApi.DataItemResult dataItemResult) {
                Log.d("Config change","Data sent successfully");
            }

            @Override
            public void onFailure(@NonNull Status status) {
                Log.d("Config change ","Data send failure");
            }
        });
    }




}




