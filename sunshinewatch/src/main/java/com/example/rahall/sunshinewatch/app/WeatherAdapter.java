package com.example.rahall.sunshinewatch.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.wearable.view.WearableListView;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by rahall4405 on 7/17/16.
 */
public class WeatherAdapter extends WearableListView.Adapter {
    private ArrayList<String> mItems;
    private final LayoutInflater mInflater;
    private static final String TODAY = "Today";
    private static final String TOMORROW = "Tomorrow";
    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;



    public WeatherAdapter(Context context, ArrayList<String> items) {
        mInflater = LayoutInflater.from(context);
        mItems = items;


    }

    @Override
    public WearableListView.ViewHolder onCreateViewHolder(
            ViewGroup viewGroup, int i) {
        return new ItemViewHolder(mInflater.inflate(R.layout.list_item, null));
    }

    @Override
    public void onBindViewHolder(WearableListView.ViewHolder viewHolder,
                                 int position) {
        ItemViewHolder itemViewHolder = (ItemViewHolder) viewHolder;
        String[] wParts = mItems.get(position).split("\\|");
        int weatherId = Integer.parseInt(wParts[0]);
        int weatherIcon = DigitalWatchFaceUtil.getIconResourceForWeatherCondition(weatherId);
        itemViewHolder.mImageView.setImageResource(weatherIcon);
        itemViewHolder.mHighTextView.setText(wParts[1]);
        itemViewHolder.mLowTextView.setText(wParts[2]);
        if (position == 0) {
            itemViewHolder.mDayTextView.setText(TODAY);
        } else if (position == 1) {
            itemViewHolder.mDayTextView.setText(TOMORROW);
        } else {

            long dayInMillis = System.currentTimeMillis() + position*DAY_IN_MILLIS;
            Calendar calendar = Calendar.getInstance();;
            Date date = new Date();

            calendar.setTimeInMillis(dayInMillis);
            date.setTime(dayInMillis);
            //SimpleDateFormat calendarFormat = new SimpleDateFormat("MMM dd yyyy");
            SimpleDateFormat calendarFormat = new SimpleDateFormat("MMM dd");
            SimpleDateFormat dayOfWeekFormat = new SimpleDateFormat("EEE", Locale.getDefault());
            String dayString =  dayOfWeekFormat.format(date);
            dayString = dayString.toUpperCase();
            String calendarString = calendarFormat.format(date);

            itemViewHolder.mDayTextView.setText(dayString + ", " + calendarString);



        }


    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    private static class ItemViewHolder extends WearableListView.ViewHolder {
        private ImageView mImageView;
        private TextView mDayTextView;
        private TextView mHighTextView;
        private TextView mLowTextView;

        public ItemViewHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView)
                    itemView.findViewById(R.id.wImage);
            mDayTextView = (TextView) itemView.findViewById(R.id.dayOfWeek);
            mHighTextView = (TextView) itemView.findViewById(R.id.highTemp);
            mLowTextView = (TextView) itemView.findViewById(R.id.lowTemp);

        }
    }

}
