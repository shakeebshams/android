/*
 * Copyright (c) 2015 IRCCloud, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.robobunny;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import androidx.appcompat.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.irccloud.android.R;

public class SeekBarPreference extends Preference implements OnSeekBarChangeListener {

    private final String TAG = getClass().getName();

    private static final String ANDROIDNS = "http://schemas.android.com/apk/res/android";
    private static final String APPLICATIONNS = "http://robobunny.com";
    private static final int DEFAULT_VALUE = 50;

    private int mMaxValue = 100;
    private int mMinValue = 0;
    private int mInterval = 1;
    private int mCurrentValue;
    private String mUnitsLeft = "";
    private String mUnitsRight = "";
    private AppCompatSeekBar mSeekBar;

    private TextView mStatusText;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPreference(context, attrs);
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initPreference(context, attrs);
    }

    private void initPreference(Context context, AttributeSet attrs) {
        setValuesFromXml(attrs);
        mSeekBar = new AppCompatSeekBar(context, attrs);
        mSeekBar.setMax(mMaxValue - mMinValue);
        mSeekBar.setOnSeekBarChangeListener(this);

        setWidgetLayoutResource(R.layout.seek_bar_preference);
    }

    private void setValuesFromXml(AttributeSet attrs) {
        mMaxValue = attrs.getAttributeIntValue(ANDROIDNS, "max", 100);
        mMinValue = attrs.getAttributeIntValue(APPLICATIONNS, "min", 0);

        mUnitsLeft = getAttributeStringValue(attrs, APPLICATIONNS, "unitsLeft", "");
        String units = getAttributeStringValue(attrs, APPLICATIONNS, "units", "");
        mUnitsRight = getAttributeStringValue(attrs, APPLICATIONNS, "unitsRight", units);

        try {
            String newInterval = attrs.getAttributeValue(APPLICATIONNS, "interval");
            if (newInterval != null)
                mInterval = Integer.parseInt(newInterval);
        } catch (Exception e) {
            Log.e(TAG, "Invalid interval value", e);
        }

    }

    private String getAttributeStringValue(AttributeSet attrs, String namespace, String name, String defaultValue) {
        String value = attrs.getAttributeValue(namespace, name);
        if (value == null)
            value = defaultValue;

        return value;
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);

        // The basic preference layout puts the widget frame to the right of the title and summary,
        // so we need to change it a bit - the seekbar should be under them.
        LinearLayout layout = (LinearLayout) view;
        layout.setOrientation(LinearLayout.VERTICAL);

        return view;
    }

    @Override
    public void onBindView(View view)
    {
        super.onBindView(view);
        if(view != null)
        {
            mSeekBar = view.findViewById(R.id.seekBarPrefSeekBar);
            mSeekBar.setMax(mMaxValue - mMinValue);
            mSeekBar.setOnSeekBarChangeListener(this);
        }

        updateView(view);
    }

    /**
     * Update a SeekBarPreference view with our current state
     *
     * @param view
     */
    protected void updateView(View view) {

        try {
            mStatusText = view.findViewById(R.id.seekBarPrefValue);

            mStatusText.setText(String.valueOf(mCurrentValue));
            mStatusText.setMinimumWidth(30);

            mSeekBar.setProgress(mCurrentValue - mMinValue);

            TextView unitsRight = view.findViewById(R.id.seekBarPrefUnitsRight);
            unitsRight.setText(mUnitsRight);

            TextView unitsLeft = view.findViewById(R.id.seekBarPrefUnitsLeft);
            unitsLeft.setText(mUnitsLeft);

        } catch (Exception e) {
            Log.e(TAG, "Error updating seek bar preference", e);
        }

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int newValue = progress + mMinValue;

        if (newValue > mMaxValue)
            newValue = mMaxValue;
        else if (newValue < mMinValue)
            newValue = mMinValue;
        else if (mInterval != 1 && newValue % mInterval != 0)
            newValue = Math.round(((float) newValue) / mInterval) * mInterval;

        // change rejected, revert to the previous value
        if (!callChangeListener(newValue)) {
            seekBar.setProgress(mCurrentValue - mMinValue);
            return;
        }

        // change accepted, store it
        mCurrentValue = newValue;
        mStatusText.setText(String.valueOf(newValue));
        persistInt(newValue);

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        notifyChanged();
    }


    @Override
    protected Object onGetDefaultValue(TypedArray ta, int index) {

        int defaultValue = ta.getInt(index, DEFAULT_VALUE);
        return defaultValue;

    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {

        if (restoreValue) {
            mCurrentValue = getPersistedInt(mCurrentValue);
        } else {
            int temp = 0;
            try {
                temp = (Integer) defaultValue;
            } catch (Exception ex) {
                Log.e(TAG, "Invalid default value: " + defaultValue.toString());
            }

            persistInt(temp);
            mCurrentValue = temp;
        }

    }

    /**
     * make sure that the seekbar is disabled if the preference is disabled
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mSeekBar.setEnabled(enabled);
    }

    @Override
    public void onDependencyChanged(Preference dependency, boolean disableDependent) {
        super.onDependencyChanged(dependency, disableDependent);

        //Disable movement of seek bar when dependency is false
        if (mSeekBar != null) {
            mSeekBar.setEnabled(!disableDependent);
        }
    }
}