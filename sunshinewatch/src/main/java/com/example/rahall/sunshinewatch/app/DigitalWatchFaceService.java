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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.PaintDrawable;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;


import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
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
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 layout modified from Android example Watchface - DigitalWatchFaceConfigListenerService.java
 */
public class DigitalWatchFaceService extends CanvasWatchFaceService {
    private static final String TAG = "DigitalWatchFaceService";

    private static final Typeface BOLD_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    /**
     * Update rate in milliseconds for normal (not ambient and not mute) mode. We update twice
     * a second to blink the colons.
     */
    private static final long NORMAL_UPDATE_RATE_MS = 500;

    /**
     * Update rate in milliseconds for mute mode. We update every minute, like in ambient mode.
     */
    private static final long MUTE_UPDATE_RATE_MS = TimeUnit.MINUTES.toMillis(1);

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine implements DataListener,
            ConnectionCallbacks, OnConnectionFailedListener {
        static final String COLON_STRING = ":";
        static final String COMMA_STRING = ", ";
        static final String SPACE_STRING = " ";
        static final String LINE_STRING = "_______";
        static final char DEGREES =  (char) 0x00B0;

        /** Alpha value for drawing time when in mute mode. */
        static final int MUTE_ALPHA = 100;

        /** Alpha value for drawing time when not in mute mode. */
        static final int NORMAL_ALPHA = 255;

        static final int MSG_UPDATE_TIME = 0;

        /** How often {@link #mUpdateTimeHandler} ticks in milliseconds. */
        long mInteractiveUpdateRateMs = NORMAL_UPDATE_RATE_MS;

        /** Handler to update the time periodically in interactive mode. */
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        if (Log.isLoggable(TAG, Log.VERBOSE)) {
                            Log.v(TAG, "updating time");
                        }
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs =
                                    mInteractiveUpdateRateMs - (timeMs % mInteractiveUpdateRateMs);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };



        /**
         * Handles time zone and locale changes.
         */
        final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                initFormats();
                invalidate();
            }
        };

        /**
         * Unregistering an unregistered receiver throws an exception. Keep track of the
         * registration state to prevent that.
         */
        boolean mRegisteredReceiver = false;

        Paint mBackgroundPaint;
        Paint mDatePaint;
        Paint mHourPaint;
        Paint mMinutePaint;
        Paint mAmPmPaint;
        Paint mLowTemp;
        Paint mHighTemp;

        Paint mColonPaint;
        float mColonWidth;
        Paint mCommaPaint;


        float mCommaWidth;
        Paint mSpacePaint;
        float mSpaceWidth;
        Paint mLinePaint;
        float mLineWidth;
        PaintDrawable mIconPaint;


        boolean mMute;

        Calendar mCalendar;
        Date mDate;
        SimpleDateFormat mDayOfWeekFormat;
        SimpleDateFormat mCalendarFormat;
        java.text.DateFormat mDateFormat;

        boolean mShouldDrawColons;
        float mXOffset;
        float mXOffsetLine2;
        float mXOffsetLine3;
        float mXOffsetFomIcon;
        float mYOffset;
        float mYOffset2;
        float mLineHeight;
        String mAmString;
        String mPmString;
        int mInteractiveBackgroundColor;
        int mInteractiveHourDigitsColor =
                DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_HOUR_DIGITS;
        int mInteractiveMinuteDigitsColor =
                DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_MINUTE_DIGITS;

        int mInteractiveCalendarDate;
        Bitmap mWeatherIcon;
        Bitmap mWeatherIconScaled;
        Bitmap mWeatherIconScaledAmbient;
        int  mWeatherId;
        String mTempHigh = "25"+ DEGREES;
        String mTempLow = "16"+ DEGREES;
        boolean mTimeType = false;

        GoogleApiClient mGoogleApiClient;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCreate");
            }
            super.onCreate(holder);
            Resources resources = DigitalWatchFaceService.this.getResources();
            mInteractiveBackgroundColor = resources.getColor(R.color.primary);
            mInteractiveCalendarDate = resources.getColor(R.color.digital_date);



            setWatchFaceStyle(new WatchFaceStyle.Builder(DigitalWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());
            mGoogleApiClient = new GoogleApiClient.Builder(DigitalWatchFaceService.this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Wearable.API)
                    .build();

            mGoogleApiClient.connect();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);
            mYOffset2 = resources.getDimension(R.dimen.digital_y2_offset);
            mLineHeight = resources.getDimension(R.dimen.digital_line_height);
            mAmString = resources.getString(R.string.digital_am);
            mPmString = resources.getString(R.string.digital_pm);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(mInteractiveBackgroundColor);
            mDatePaint = createTextPaint(resources.getColor(R.color.digital_date));
            mHourPaint = createTextPaint(mInteractiveHourDigitsColor, BOLD_TYPEFACE);
            mMinutePaint = createTextPaint(mInteractiveMinuteDigitsColor);
            mAmPmPaint = createTextPaint(mInteractiveMinuteDigitsColor);

            mColonPaint = createTextPaint(mInteractiveHourDigitsColor);
            mCommaPaint = createTextPaint(resources.getColor(R.color.digital_date));
            mSpacePaint = createTextPaint(resources.getColor(R.color.digital_date));
            mLinePaint = createTextPaint(resources.getColor(R.color.digital_date));

           mHighTemp = createTextPaint(mInteractiveHourDigitsColor);
           mLowTemp = createTextPaint(resources.getColor(R.color.digital_date));
            mCalendar = Calendar.getInstance();
            mDate = new Date();
            mWeatherIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_clear);
            mWeatherIconScaled = Bitmap.createScaledBitmap(mWeatherIcon,85,85,true);
            mWeatherIconScaledAmbient = DigitalWatchFaceUtil.createGrayScaleBackgroundBitmap(mWeatherIconScaled);
            DigitalWatchFaceUtil.requestNewWeatherData(mGoogleApiClient);

            initFormats();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int defaultInteractiveColor) {
            return createTextPaint(defaultInteractiveColor, NORMAL_TYPEFACE);
        }

        private Paint createTextPaint(int defaultInteractiveColor, Typeface typeface) {
            Paint paint = new Paint();
            paint.setColor(defaultInteractiveColor);
            paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onVisibilityChanged: " + visible);
            }
            super.onVisibilityChanged(visible);

            if (visible) {
                mGoogleApiClient.connect();

                registerReceiver();

                // Update time zone and date formats, in case they changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                initFormats();
            } else {
                unregisterReceiver();

            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void initFormats() {
            mDayOfWeekFormat = new SimpleDateFormat("EEE", Locale.getDefault());
            mCalendarFormat = new SimpleDateFormat("MMM dd yyyy");
            mDayOfWeekFormat.setCalendar(mCalendar);
            mCalendarFormat.setCalendar(mCalendar);
            mDateFormat = DateFormat.getDateFormat(DigitalWatchFaceService.this);
            mDateFormat.setCalendar(mCalendar);
        }

        private void registerReceiver() {
            if (mRegisteredReceiver) {
                return;
            }
            mRegisteredReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_LOCALE_CHANGED);
            DigitalWatchFaceService.this.registerReceiver(mReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredReceiver) {
                return;
            }
            mRegisteredReceiver = false;
            DigitalWatchFaceService.this.unregisterReceiver(mReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {

                    Log.d(TAG, "onApplyWindowInsets: " + (insets.isRound() ? "round" : "square"));

            }
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = DigitalWatchFaceService.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            mXOffsetLine2 = resources.getDimension(isRound
                    ? R.dimen.digital_x2_offset_round : R.dimen.digital_x2_offset);
            mXOffsetLine3 = resources.getDimension(isRound
                    ? R.dimen.digital_x3_offset_round : R.dimen.digital_x3_offset);
            mXOffsetFomIcon = resources.getDimension(isRound
                    ? R.dimen.digital_from_icon_offset_round : R.dimen.digital_from_icon_offset);
            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);
            float dateTextSize = resources.getDimension(isRound
                    ? R.dimen.digital_date_text_size_round : R.dimen.digital_date_text_size);
            float tempTextSize = resources.getDimension(isRound
                    ? R.dimen.digital_temp_text_size_round : R.dimen.digital_temp_text_size);
            float amPmSize = resources.getDimension(isRound
                    ? R.dimen.digital_am_pm_size_round : R.dimen.digital_am_pm_size);

            mDatePaint.setTextSize(dateTextSize);
            mLowTemp.setTextSize(tempTextSize);
            mHighTemp.setTextSize(tempTextSize);
            mHourPaint.setTextSize(textSize);
            mMinutePaint.setTextSize(textSize);
            mAmPmPaint.setTextSize(textSize);

            mColonPaint.setTextSize(textSize);
            mCommaPaint.setTextSize(dateTextSize);
            mSpacePaint.setTextSize(tempTextSize);

            mColonWidth = mColonPaint.measureText(COLON_STRING);
            mCommaWidth = mCommaPaint.measureText(COMMA_STRING);
            mSpaceWidth = mSpacePaint.measureText(SPACE_STRING);
            mLineWidth = mLinePaint.measureText(LINE_STRING);

        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);

            boolean burnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
            mHourPaint.setTypeface(burnInProtection ? NORMAL_TYPEFACE : BOLD_TYPEFACE);

            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onPropertiesChanged: burn-in protection = " + burnInProtection
                        + ", low-bit ambient = " + mLowBitAmbient);
            }
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onTimeTick: ambient = " + isInAmbientMode());
            }
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);
            }
            adjustPaintColorToCurrentMode(mBackgroundPaint,
                    DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_BACKGROUND, mInteractiveBackgroundColor);
            adjustPaintColorToCurrentMode(mHourPaint, mInteractiveHourDigitsColor,
                    DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_HOUR_DIGITS);
            adjustPaintColorToCurrentMode(mMinutePaint,  DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_MINUTE_DIGITS,
                    mInteractiveMinuteDigitsColor);
            adjustPaintColorToCurrentMode(mAmPmPaint,  DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_MINUTE_DIGITS,
                    mInteractiveMinuteDigitsColor);
            adjustPaintColorToCurrentMode(mDatePaint, DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_MINUTE_DIGITS,
                    mInteractiveCalendarDate);
            adjustPaintColorToCurrentMode(mLinePaint,DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_MINUTE_DIGITS,
                    mInteractiveCalendarDate);
            adjustPaintColorToCurrentMode(mHighTemp,DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_MINUTE_DIGITS,
                    mInteractiveMinuteDigitsColor);
            adjustPaintColorToCurrentMode(mLowTemp,DigitalWatchFaceUtil.COLOR_VALUE_DEFAULT_AND_AMBIENT_MINUTE_DIGITS,
                    mInteractiveCalendarDate);




            if (mLowBitAmbient) {
                boolean antiAlias = !inAmbientMode;
                mDatePaint.setAntiAlias(antiAlias);
                mHourPaint.setAntiAlias(antiAlias);
                mMinutePaint.setAntiAlias(antiAlias);
                mAmPmPaint.setAntiAlias(antiAlias);

                mColonPaint.setAntiAlias(antiAlias);
                mCommaPaint.setAntiAlias(antiAlias);
                mLinePaint.setAntiAlias(antiAlias);
                mHighTemp.setAntiAlias(antiAlias);
                mLowTemp.setAntiAlias(antiAlias);
            }
            invalidate();

            // Whether the timer should be running depends on whether we're in ambient mode (as well
            // as whether we're visible), so we may need to start or stop the timer.
            updateTimer();
        }

        private void adjustPaintColorToCurrentMode(Paint paint, int ambientColor ,
                                                   int interactiveColor) {
            paint.setColor(isInAmbientMode() ? ambientColor : interactiveColor);
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onInterruptionFilterChanged: " + interruptionFilter);
            }
            super.onInterruptionFilterChanged(interruptionFilter);

            boolean inMuteMode = interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE;
            // We only need to update once a minute in mute mode.
            setInteractiveUpdateRateMs(inMuteMode ? MUTE_UPDATE_RATE_MS : NORMAL_UPDATE_RATE_MS);

            if (mMute != inMuteMode) {
                mMute = inMuteMode;
                int alpha = inMuteMode ? MUTE_ALPHA : NORMAL_ALPHA;
                mDatePaint.setAlpha(alpha);
                mHourPaint.setAlpha(alpha);
                mLowTemp.setAlpha(alpha);
                mHighTemp.setAlpha(alpha);
                mMinutePaint.setAlpha(alpha);
                mAmPmPaint.setAlpha(alpha);
                mColonPaint.setAlpha(alpha);
                mCommaPaint.setAlpha(alpha);
                mLowTemp.setAlpha(alpha);
                mHighTemp.setAlpha(alpha);
                invalidate();
            }
        }

        public void setInteractiveUpdateRateMs(long updateRateMs) {
            if (updateRateMs == mInteractiveUpdateRateMs) {
                return;
            }
            mInteractiveUpdateRateMs = updateRateMs;

            // Stop and restart the timer so the new update rate takes effect immediately.
            if (shouldTimerBeRunning()) {
                updateTimer();
            }
        }









        private String formatTwoDigitNumber(int hour) {
            return String.format("%02d", hour);
        }

        private String getAmPmString(int amPm) {
            return amPm == Calendar.AM ? mAmString : mPmString;
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);
            mDate.setTime(now);
            boolean is24Hour = DateFormat.is24HourFormat(DigitalWatchFaceService.this);

            // Show colons for the first half of each second so the colons blink on when the time
            // updates.
            mShouldDrawColons = (System.currentTimeMillis() % 1000) < 500;

            // Draw the background.
            canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);

            // Draw the hours.
            float x;
            if (mTimeType) {
                x = mXOffset;
            } else {
                x = mXOffset - 50;
            }

            String hourString;
            if (mTimeType) {

                hourString = formatTwoDigitNumber(mCalendar.get(Calendar.HOUR_OF_DAY));
            } else {
                int hour = mCalendar.get(Calendar.HOUR);
                if (hour == 0) {
                    hour = 12;
                }
                hourString = String.valueOf(hour);
            }

            Log.d("onDraw ", "Time Type = " + String.valueOf(mTimeType));

            canvas.drawText(hourString, x, mYOffset, mHourPaint);
            x += mHourPaint.measureText(hourString);

            // In ambient and mute modes, always draw the first colon. Otherwise, draw the
            // first colon for the first half of each second.
            if (isInAmbientMode() || mMute || mShouldDrawColons) {
                canvas.drawText(COLON_STRING, x, mYOffset, mColonPaint);
            }
            x += mColonWidth;

            // Draw the minutes.
            String minuteString = formatTwoDigitNumber(mCalendar.get(Calendar.MINUTE));
            canvas.drawText(minuteString, x, mYOffset, mMinutePaint);
            x += mMinutePaint.measureText(minuteString);
            if(!mTimeType) {
                canvas.drawText(getAmPmString(
                        mCalendar.get(Calendar.AM_PM)), x+10, mYOffset, mAmPmPaint);
            }


            // In unmuted interactive mode, draw a second blinking colon followed by the seconds.
            // Otherwise, if we're in 12-hour mode, draw AM/PM


            // Only render the day of week and date if there is no peek card, so they do not bleed
            // into each other in ambient mode.
            if (getPeekCardPosition().isEmpty()) {
                // Day of week
                String dayString =  mDayOfWeekFormat.format(mDate);
                dayString = dayString.toUpperCase();
                canvas.drawText(
                        dayString,
                        mXOffsetLine2, mYOffset + mLineHeight , mDatePaint);
                x = mXOffsetLine2 + mDatePaint.measureText(dayString);
                canvas.drawText(COMMA_STRING,x,mYOffset + mLineHeight,mCommaPaint);
                x+= mCommaWidth;

                // Date
                String calendarString = mCalendarFormat.format(mDate);
                calendarString = calendarString.toUpperCase() ;
                canvas.drawText(
                        calendarString,
                        x, mYOffset + mLineHeight , mDatePaint);
                x = mXOffsetLine3;
                canvas.drawText(LINE_STRING
                        , x, mYOffset + 2*mLineHeight, mDatePaint);
                // Weather Image
                //High and Low temps
                x=mXOffsetLine2;
                if(isInAmbientMode()) {
                    canvas.drawBitmap(mWeatherIconScaledAmbient,x,mYOffset + 2*mLineHeight +mYOffset2, new Paint());
                } else {
                    canvas.drawBitmap(mWeatherIconScaled,x,mYOffset + 2*mLineHeight + mYOffset2,new Paint());
                }
                String highTemp = mTempHigh;
                canvas.drawText(highTemp,x + mXOffsetFomIcon ,mYOffset + 4*mLineHeight, mHighTemp);
                canvas.drawText(SPACE_STRING,x + mXOffsetFomIcon + mHighTemp.measureText(highTemp),mYOffset + 4*mLineHeight,mSpacePaint);
                String lowTemp = mTempLow;
                canvas.drawText(lowTemp,x + mXOffsetFomIcon + mSpaceWidth + mHighTemp.measureText(highTemp),mYOffset + 4*mLineHeight, mLowTemp);

            }

        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "updateTimer");
            }
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }










        @Override
        public void onConnected(@Nullable Bundle connectionHint) {

                Log.d(TAG, "onConnected: " + connectionHint);

            Wearable.DataApi.addListener(mGoogleApiClient, this);
        }

        @Override
        public void onConnectionSuspended(int cause) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnectionSuspended: " + cause);
            }
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult result) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnectionFailed: " + result);
            }
        }

        @Override // DataApi.DataListener
        public void onDataChanged(DataEventBuffer dataEvents) {
            Log.d(TAG, "Got to onDataChanged");

            for (DataEvent dataEvent : dataEvents) {
                if (dataEvent.getType() != DataEvent.TYPE_CHANGED) {
                    continue;
                }
                String path = dataEvent.getDataItem().getUri().getPath();
                if ("/sunshine/TimeType".equals(path)) {
                    updateTimeType(DigitalWatchFaceUtil.extractTimeType(dataEvents));
                }
                if ("/sunshine/ActivityData".equals(path)) {
                    Log.d(TAG, "Got to onDataChanged 2");
                    DataItem dataItem = dataEvent.getDataItem();
                    Log.d(TAG, "Got to onDataChanged 3" + dataItem.getUri().getPath());


                    DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                    DataMap config = dataMapItem.getDataMap();
                    ArrayList<String> weatherData = new ArrayList<>();
                    weatherData = config.getStringArrayList("WeatherData");

                    String weatherString = weatherData.get(0);

                    Log.d(TAG, "Got to onDataChanged 4 " + weatherString);
                    String[] wParts = weatherString.split("\\|");
                    Log.d(TAG, "Got to onDataChanged 5 " + wParts[0]);

                    mWeatherId = Integer.parseInt(wParts[0]);
                    mWeatherIcon = BitmapFactory.decodeResource(
                            getResources(),
                            DigitalWatchFaceUtil.getIconResourceForWeatherCondition(mWeatherId));
                    mWeatherIconScaled = Bitmap.createScaledBitmap(mWeatherIcon, 85, 85, true);
                    mWeatherIconScaledAmbient = DigitalWatchFaceUtil.createGrayScaleBackgroundBitmap(mWeatherIconScaled);

                    mTempHigh = wParts[1];

                    mTempLow = wParts[2];
                    Log.d(TAG, "Got to onDataChanged 5 " + mWeatherId + mTempHigh + mTempLow);
                    invalidate();
                }

            }
        }

        private void updateTimeType(final boolean timeType) {
            mTimeType = timeType;
            invalidate();
        }









    }
}
