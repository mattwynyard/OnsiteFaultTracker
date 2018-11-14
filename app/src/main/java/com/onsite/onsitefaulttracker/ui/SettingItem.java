package com.onsite.onsitefaulttracker.ui;

import android.media.Image;
import android.os.ParcelUuid;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.onsite.onsitefaulttracker.R;
import com.onsite.onsitefaulttracker.activity.BaseFragment;
import com.onsite.onsitefaulttracker.activity.previous.PreviousRecordsFragment;
import com.onsite.onsitefaulttracker.util.SettingsUtil;

import org.w3c.dom.Text;

/**
 * Created by hihi on 8/19/2017.
 *
 * Setting Item, contains the views for each setting item
 * and callbacks for communicating with the parent fragment
 */
public class SettingItem {

    // The tag name for this item
    private static final String TAG = SettingItem.class.getSimpleName();

    /**
     * Possible Setting Item types
     */
    public enum SettingItemType {
        SettingItemTypeFrequency,
        SettingItemTypeImageSize,
        SettingItemTypeRecordingHours,
        SettingItemTypeExposure,
        SettingItemTypeFocusDistance,
        SettingItemTypeFrameDuration
    }

    // The setting type for this item
    private SettingItemType mSettingItemType;

    // The parent fragment
    private BaseFragment mParentFragment;

    // The layout for this setting
    private RelativeLayout mSettingLayout;

    // The title text view for this setting
    private TextView mTitleView;

    // The value text view for this setting
    private TextView mValueView;

    // The seek bar for this setting
    private SeekBar mSeekBar;

    // The Plus button to increase available storage
    private FloatingActionButton mPlusButton;

    // The Minus button to decrease available storage
    private FloatingActionButton mMinusButton;

    // The layout id of this setting item
    private int mSettingLayoutId;

    // Listener for communicating with the parent
    private Listener mListener;

    /**
     * Initialize this setting item.
     *
     */
    public SettingItem(final BaseFragment parentFragment,
                       final View view,
            final SettingItemType settingType) {
        mParentFragment = parentFragment;
        mSettingItemType = settingType;
        mSettingLayoutId = getResourceIdForSettingType();

        mSettingLayout = (RelativeLayout)view.findViewById(mSettingLayoutId);
        mPlusButton = (FloatingActionButton)mSettingLayout.findViewById(R.id.setting_item_plus_button);
        mMinusButton = (FloatingActionButton)mSettingLayout.findViewById(R.id.setting_item_minus_button);
        mTitleView = (TextView)mSettingLayout.findViewById(R.id.setting_item_title);
        mValueView = (TextView)mSettingLayout.findViewById(R.id.setting_item_value);
        mSeekBar = (SeekBar)mSettingLayout.findViewById(R.id.setting_item_seek_bar);

        mPlusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onPlusTapped(mSettingItemType);
                }
            }
        });
        mMinusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onMinusTapped(mSettingItemType);
                }
            }
        });
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Ignore any changes that arn't from the user
                if (!fromUser) {
                    return;
                }
                if (mListener != null) {
                    mListener.onSeekBarPositionUpdated(mSettingItemType, progress);
                }


                /*
                if (seekBar == mFrequencySeekBar) {
                    // The frequency value has been changed
                    mFrequencyMilliseconds = roundValueToClosest(FREQUENCY_MILLIS_MIN_VALUE + Math.round((FREQUENCY_MILLIS_MAX_VALUE - FREQUENCY_MILLIS_MIN_VALUE) * valuePercentage), 500);
                    updateFrequencyTextView(mFrequencyMilliseconds);
                    SettingsUtil.sharedInstance().setPictureFrequency(mFrequencyMilliseconds);
                } else if (seekBar == mImageSizeSeekBar) {
                    // The image size value has been changed
                    mImageSize = roundValueToClosest(IMAGE_SIZE_MIN_VALUE + Math.round((IMAGE_SIZE_MAX_VALUE - IMAGE_SIZE_MIN_VALUE)*valuePercentage), 10);
                    updateImageSizeTextView(mImageSize);
                    SettingsUtil.sharedInstance().setImageSize(mImageSize);
                } else if (seekBar == mRecordingHoursSeekBar) {
                    // The recording hours value has been changed
                    mRecordingHours = roundValueToClosest(Math.round((RECORD_HOURS_MIN + (RECORD_HOURS_MAX - RECORD_HOURS_MIN)*valuePercentage)*100), 50)/100.0f;
                    updateRecordingHoursTextView(mRecordingHours);
                    SettingsUtil.sharedInstance().setRecordingHours(mRecordingHours);
                } else if (seekBar == mExposureSeekBar) {
                    // The exposure value has changed
                    SettingsUtil.sharedInstance().setCurrentExposureFromPercentage(progress);
                    updateExposureTextView();
                }
                */
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        setTitleText();
    }

    /**
     *
     * @return
     */
    private int getResourceIdForSettingType() {
        switch (mSettingItemType) {
            case SettingItemTypeFrequency:
                return R.id.frequency_setting_layout;
            case SettingItemTypeImageSize:
                return R.id.image_size_setting_layout;
            case SettingItemTypeRecordingHours:
                return R.id.recording_hours_setting_layout;
            case SettingItemTypeExposure:
                return R.id.exposure_setting_layout;
            case SettingItemTypeFocusDistance:
                return R.id.focus_distance_setting_layout;
            case SettingItemTypeFrameDuration:
                return R.id.frame_duration_setting_layout;
        }
        return 0;
    }

    /**
     *
     * @param listener
     */
    public void setListener(Listener listener) {
        mListener = listener;
    }

    /**
     * Set the position of the seek bar
     *
     * @param position
     */
    public void setSeekPosition(int position) {
        mSeekBar.setProgress(position);
    }

    /**
     *
     */
    private void setTitleText() {
        String title = "";
        switch (mSettingItemType) {
            case SettingItemTypeFrequency:
                title = mParentFragment.getString(R.string.setting_frequency_title);
                break;
            case SettingItemTypeImageSize:
                title = mParentFragment.getString(R.string.setting_image_size_title);
                break;
            case SettingItemTypeRecordingHours:
                title = mParentFragment.getString(R.string.setting_recording_hours_title);
                break;
            case SettingItemTypeExposure:
                title = mParentFragment.getString(R.string.setting_exposure_title);
                break;
            case SettingItemTypeFocusDistance:
                title = mParentFragment.getString(R.string.setting_focus_distance_title);
                break;
            case SettingItemTypeFrameDuration:
                title = mParentFragment.getString(R.string.setting_frame_duration_title);
                break;
        }
        mTitleView.setText(title);
    }

    /**
     *
     * @param value
     */
    public void setValue(final float value) {
        String valueString = "";
        switch (mSettingItemType) {
            case SettingItemTypeFrequency:
                valueString = mParentFragment.getString(R.string.setting_frequency_value, value);
                break;
            case SettingItemTypeImageSize:
                valueString = mParentFragment.getString(R.string.setting_image_size_value, value);
                break;
            case SettingItemTypeRecordingHours:
                valueString = mParentFragment.getString(R.string.setting_recording_hours_value, value);
                break;
            case SettingItemTypeExposure:
                valueString = mParentFragment.getString(R.string.setting_exposure_value, value);
                break;
            case SettingItemTypeFocusDistance:
                valueString = mParentFragment.getString(R.string.setting_focus_distance_value, value);
                break;
            case SettingItemTypeFrameDuration:
                valueString = mParentFragment.getString(R.string.setting_frame_duration_value, value);
                break;
        }
        mValueView.setText(valueString);
    }

    /**
     *
     *
     * @param value
     */
    public void setValue(final long value) {
        String valueString = "";
        switch (mSettingItemType) {
            case SettingItemTypeFrequency:
                valueString = mParentFragment.getString(R.string.setting_frequency_value, value);
                break;
            case SettingItemTypeImageSize:
                valueString = mParentFragment.getString(R.string.setting_image_size_value, value);
                break;
            case SettingItemTypeRecordingHours:
                valueString = mParentFragment.getString(R.string.setting_recording_hours_value, value);
                break;
            case SettingItemTypeExposure:
                valueString = mParentFragment.getString(R.string.setting_exposure_value, value);
                break;
            case SettingItemTypeFocusDistance:
                valueString = mParentFragment.getString(R.string.setting_focus_distance_value, value);
                break;
            case SettingItemTypeFrameDuration:
                valueString = mParentFragment.getString(R.string.setting_frame_duration_value, value);
                break;
        }
        mValueView.setText(valueString);
    }

    /**
     * Listener for communicating with parent fragment
     */
    public interface Listener {
        void onPlusTapped(SettingItemType settingItemType);
        void onMinusTapped(SettingItemType settingItemType);
        void onSeekBarPositionUpdated(SettingItemType settingItemType, int newValue);
    }

}
