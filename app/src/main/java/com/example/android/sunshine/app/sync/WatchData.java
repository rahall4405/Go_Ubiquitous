package com.example.android.sunshine.app.sync;

/**
 * Created by rahall4405 on 7/16/16.
 */
public class WatchData {
    private double high;
    private double low;
    private int weatherId;

    public double getHigh() {
        return high;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public int getWeatherId() {
        return weatherId;
    }

    public void setWeatherId(int weatherId) {
        this.weatherId = weatherId;
    }
}
