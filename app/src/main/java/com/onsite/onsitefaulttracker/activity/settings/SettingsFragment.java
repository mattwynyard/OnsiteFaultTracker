package com.onsite.onsitefaulttracker.activity.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.onsite.onsitefaulttracker.R;
import com.onsite.onsitefaulttracker.activity.BaseFragment;
import com.onsite.onsitefaulttracker.ui.SettingItem;
import com.onsite.onsitefaulttracker.util.CalculationUtil;
import com.onsite.onsitefaulttracker.util.SettingsUtil;
import com.onsite.onsitefaulttracker.util.ThreadUtil;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by hihi on 6/7/2016.
 *
 * SettingsFragment is the default Fragment for the Settings Activity,
 * On the Settings screen the user can change settings related
 * to how the app creates a record such as:
 *
 */
public class SettingsFragment extends BaseFragment implements SettingItem.Listener {

    // The tag name for this fragment
    private static final String TAG = SettingsFragment.class.getSimpleName();

    // Minimum milliseconds for photo taking frequency
    private static final long FREQUENCY_MILLIS_MIN_VALUE = 500;

    // Maximum milliseconds for photo taking frequency
    private static final long FREQUENCY_MILLIS_MAX_VALUE = 6000;

    // The amount that frequency increments/decrements by per step increment/decrement
    private static final long FREQUENCY_STEP_AMOUNT = 500;

    // Minimum size in KB of desired photo size
    private static final long IMAGE_SIZE_MIN_VALUE = 50;

    // Maximum size in KB of desired  photo size
    private static final long IMAGE_SIZE_MAX_VALUE = 1000;

    // The amount that image size increments/decrements by per step increment/decrement
    private static final long IMAGE_SIZE_STEP_AMOUNT = 10;

    // Minimum value for record hours
    private static final float RECORD_HOURS_MIN = 0.5f;

    // Maximum value for record hours
    private static final float RECORD_HOURS_MAX = 10.0f;

    // The amount that recording hours increments/decrements by per step increment/decrement
    private static final float RECORD_HOURS_STEP_AMOUNT = 0.5f;

    // The amount that the focus distance increments/decrements by per step
    private static final float FOCUS_DISTANCE_STEP_AMOUNT = 0.1f;

    // The camera Id edit text view
    private TextView mCameraIdTextView;

    // The camera id of this device
    private String mCameraId;

    // frequency setting item
    private SettingItem mFrequencyItem;

    // image size setting item
    private SettingItem mImageSizeItem;

    // recording hours setting item
    private SettingItem mRecordingHoursItem;

    // exposure setting item
    private SettingItem mExposureItem;

    // focus distance setting item
    private SettingItem mFocusDistanceItem;

    // frame duration setting item
    private SettingItem mFrameDurationItem;

    // The actual value for the photo capture frequency milliseconds
    private long mFrequencyMilliseconds;

    // The actual value for the image size in KB
    private long mImageSize;

    // The actual value for the usual recording hours
    private float mRecordingHours;

    // The current focus distance value
    private float mFocusDistance;

    // The current frame duration percentage value
    private long mFrameDuration;

    // The text view that displays the required storage space for a days work in KB
    private TextView mRequiredStorageTextView;

    // The text view that displays the storage space available
    private TextView mAvailableStorageTextView;

    // The button to restore settings to their default values
    private Button mRestoreDefaultsButton;

    // Has the user confirmed they want to exit?
    private boolean mExitConfirmed;

    /**
     * On create view, Override this in each extending fragment to implement initialization for that
     * fragment.
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view != null) {
            mFrequencyItem = new SettingItem(this,
                    view,
                    SettingItem.SettingItemType.SettingItemTypeFrequency);
            mFrequencyItem.setListener(this);
            mImageSizeItem = new SettingItem(this,
                    view,
                    SettingItem.SettingItemType.SettingItemTypeImageSize);
            mImageSizeItem.setListener(this);
            mRecordingHoursItem = new SettingItem(this,
                    view,
                    SettingItem.SettingItemType.SettingItemTypeRecordingHours);
            mRecordingHoursItem.setListener(this);
            mExposureItem = new SettingItem(this,
                    view,
                    SettingItem.SettingItemType.SettingItemTypeExposure);
            mExposureItem.setListener(this);
            mFocusDistanceItem = new SettingItem(this,
                    view,
                    SettingItem.SettingItemType.SettingItemTypeFocusDistance);
            mFocusDistanceItem.setListener(this);
            mFrameDurationItem = new SettingItem(this,
                    view,
                    SettingItem.SettingItemType.SettingItemTypeFrameDuration);
            mFrameDurationItem.setListener(this);

            mCameraIdTextView = (TextView)view.findViewById(R.id.camera_id_edit_text);
            mCameraIdTextView.setClickable(true);
            mCameraIdTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showChangeCameraIdTextView();
                }
            });

            mRequiredStorageTextView = (TextView)view.findViewById(R.id.required_storage_text_view);
            mAvailableStorageTextView = (TextView)view.findViewById(R.id.available_storage_text_view);

            mRestoreDefaultsButton = (Button)view.findViewById(R.id.restore_defaults_button);
            mRestoreDefaultsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onRestoreDefaultsClicked();
                }
            });

            setInitialValues();
        }
        return view;
    }

    /**
     * instantiate and return an instance of this fragment
     *
     * @return
     */
    public static SettingsFragment createInstance() {
        return new SettingsFragment();
    }

    /**
     * Returns the display title for this fragment
     *
     * @return
     */
    @Override
    protected String getDisplayTitle() {
        return getString(R.string.settings_title);
    }

    /**
     * Returns the layout resource for this fragment
     *
     * @return
     */
    @Override
    protected int getLayoutResourceId() {
        return R.layout.fragment_settings;
    }

    /**
     * Sets the initial values of the sliders and the value text views
     */
    private void setInitialValues() {
        mCameraId = SettingsUtil.sharedInstance().getCameraId();
        mFrequencyMilliseconds = SettingsUtil.sharedInstance().getPictureFrequency();
        mImageSize = SettingsUtil.sharedInstance().getImageSize();
        mRecordingHours = SettingsUtil.sharedInstance().getRecordingHours();
        mFrameDuration = SettingsUtil.sharedInstance().getFrameDurationPercentage();
        mFocusDistance = SettingsUtil.sharedInstance().getFocusDistance();

        // Set the seek bar positions
        updateFrequencySeekPosition();
        updateImageSizeSeekPosition();
        updateRecordingHoursSeekPosition();
        updateExposureSeekPosition();
        updateFrameDurationSeekPosition();
        updateFocusDistanceSeekPosition();

        // Update the value text views
        if (!TextUtils.isEmpty(mCameraId)) {
            mCameraIdTextView.setText(mCameraId);
        }
        updateFrequencyTextView(mFrequencyMilliseconds);
        updateImageSizeTextView(mImageSize);
        updateRecordingHoursTextView(mRecordingHours);
        updateExposureTextView();
        updateFrameDurationTextView(mFrameDuration);
        updateFocusDistanceTextView(mFocusDistance);
        updateRequiredStorageTextView();
        updateAvailableStorageTextView();
    }

    /**
     * Action when user clicks on the restore defaults button,
     * Show a confirmation dialog then if the user accepts reset
     * settings to the default settings
     */
    private void onRestoreDefaultsClicked() {
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.restore_settings_dialog_title))
                .setMessage(getString(R.string.restore_settings_dialog_message))
                .setPositiveButton(getString(R.string.restore_settings_dialog_confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SettingsUtil.sharedInstance().resetSettings();
                        setInitialValues();
                    }
                })
                .setNeutralButton(getString(R.string.restore_settings_dialog_cancel), null)
                .show();
    }

    /**
     * Sets the position of the seek bar to match the focus distance value
     */
    private void updateFocusDistanceSeekPosition() {
        mFocusDistanceItem.setSeekPosition(Math.round(((mFocusDistance - SettingsUtil.MIN_FOCUS_DISTANCE) / (SettingsUtil.MAX_FOCUS_DISTANCE - SettingsUtil.MIN_FOCUS_DISTANCE)) * 100));
    }

    /**
     * Sets the frame duration seek bar to match the frame duration value
     */
    private void updateFrameDurationSeekPosition() {
        mFrameDurationItem.setSeekPosition((int) mFrameDuration);
    }

    /**
     * Sets the position of the seek bar to match the exposure value
     */
    private void updateExposureSeekPosition() {
        mExposureItem.setSeekPosition(SettingsUtil.sharedInstance().getCurrentExposureAsPercentage());
    }

    /**
     * Sets the position of the seek bar to match the frequency milliseconds value
     */
    private void updateFrequencySeekPosition() {
        mFrequencyItem.setSeekPosition(Math.round(((float) (mFrequencyMilliseconds - FREQUENCY_MILLIS_MIN_VALUE) / (FREQUENCY_MILLIS_MAX_VALUE - FREQUENCY_MILLIS_MIN_VALUE)) * 100));
    }

    /**
     * Sets the position of the seek bar to match the image size value
     */
    private void updateImageSizeSeekPosition() {
        mImageSizeItem.setSeekPosition(Math.round(((float) (mImageSize - IMAGE_SIZE_MIN_VALUE) / (IMAGE_SIZE_MAX_VALUE - IMAGE_SIZE_MIN_VALUE)) * 100));
    }

    /**
     * Sets the position of the seek bar to match the recording hours value
     */
    private void updateRecordingHoursSeekPosition() {
        mRecordingHoursItem.setSeekPosition(Math.round(((mRecordingHours - RECORD_HOURS_MIN) / (RECORD_HOURS_MAX - RECORD_HOURS_MIN)) * 100));
    }

    /**
     * Update the display value fo the focus distance setting value
     *
     * @param focusDistance
     */
    private void updateFocusDistanceTextView(final float focusDistance) {
        mFocusDistanceItem.setValue(focusDistance);
    }

    /**
     * Update the display value of the frame duration
     *
     * @param frameDuration
     */
    private void updateFrameDurationTextView(final long frameDuration) {
        mFrameDurationItem.setValue((int)frameDuration);
    }

    /**
     * Update the display value of the frequency setting value
     *
     * @param frequencyMilliseconds  The frequency which pictures are to be snapped at
     */
    private void updateFrequencyTextView(final long frequencyMilliseconds) {
        mFrequencyItem.setValue((frequencyMilliseconds / 1000.0f));
    }

    /**
     * Update the display value of the image size setting value
     *
     * @param imageSizeKb  The desired image size in KB
     */
    private void updateImageSizeTextView(final long imageSizeKb) {
        mImageSizeItem.setValue(imageSizeKb);
    }

    /**
     * Update the display value of the recording hours setting value
     *
     * @param recordingHours  The expected recording hours
     */
    private void updateRecordingHoursTextView(final float recordingHours) {
        mRecordingHoursItem.setValue(recordingHours);
    }

    /**
     * Update the required storage space display
     */
    private void updateRequiredStorageTextView() {
        mRequiredStorageTextView.setText(String.format(getString(R.string.required_storage_text),
                CalculationUtil.sharedInstance().getDisplayValueFromKB(CalculationUtil.sharedInstance().calculateNeededSpaceKB())));
    }

    /**
     * Update the available storage space display
     */
    private void updateAvailableStorageTextView() {
        mAvailableStorageTextView.setText(String.format(getString(R.string.available_storage_text),
                CalculationUtil.sharedInstance().getDisplayValueFromKB(CalculationUtil.sharedInstance().getAvailableStorageSpaceKB())));
        updateAvailableStorageColor();
    }

    /**
     * Update the exposure value display
     */
    private void updateExposureTextView() {
        mExposureItem.setValue(SettingsUtil.sharedInstance().getCurrentExposureAsPercentage());
    }

    /**
     * Update the available storage space text color
     * - Black: plenty of space available
     * - Yellow: just enough space available
     * - Red: not enough space available
     */
    private void updateAvailableStorageColor() {
        mAvailableStorageTextView.setTextColor(CalculationUtil.sharedInstance().getAvailableStorageConditionColor());
    }

    /**
     * Rounds the frequency milliseconds value to the closest 500 millis
     *
     * @param valueToRound
     * @return
     */
    private long roundValueToClosest(final long valueToRound, long toClosest) {
        float val = valueToRound / (float)toClosest;
        int iVal = (int)val;
        if (val - iVal < 0.5) {
            return iVal * toClosest;
        } else {
            return  (iVal * toClosest) + toClosest;
        }
    }

    /**
     * Show the change camera id dialog
     */
    private void showChangeCameraIdTextView() {
        final RelativeLayout changeCameraIdLayout = new RelativeLayout(getActivity());

        final EditText changeCameraIdInput = new EditText(getActivity());
        changeCameraIdInput.setHint(R.string.camera_id_hint);
        RelativeLayout.LayoutParams changeCameraIdParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        changeCameraIdParams.leftMargin = getResources().getDimensionPixelSize(R.dimen.new_record_name_text_margin);
        changeCameraIdParams.rightMargin = getResources().getDimensionPixelSize(R.dimen.new_record_name_text_margin);
        changeCameraIdInput.setLayoutParams(changeCameraIdParams);
        changeCameraIdInput.setSingleLine();
        changeCameraIdInput.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        changeCameraIdInput.setText(SettingsUtil.sharedInstance().getCameraId());
        changeCameraIdLayout.addView(changeCameraIdInput);

        final AlertDialog d = new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.set_camera_id_title))
                .setMessage(getString(R.string.set_camera_id_message))
                .setView(changeCameraIdLayout)
                .setPositiveButton(getString(android.R.string.ok), null)
                .setNegativeButton(getString(android.R.string.cancel), null)
                .create();

        // Set action on button clicks,  This is so the default button click action
        d.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button positiveButton = d.getButton(AlertDialog.BUTTON_POSITIVE);
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!TextUtils.isEmpty(changeCameraIdInput.getText().toString())) {
                            SettingsUtil.sharedInstance().setCameraId(changeCameraIdInput.getText().toString());
                            mCameraIdTextView.setText(changeCameraIdInput.getText().toString());
                            d.dismiss();
                        } else {
                            showCameraIdMustBeEntered();
                        }
                    }
                });
            }
        });
        d.show();

        // Show the keyboard as the name dialog pops up
        ThreadUtil.executeOnMainThreadDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager keyboard = (InputMethodManager)
                        getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.showSoftInput(changeCameraIdInput, 0);
            }
        }, 300);
    }

    /**
     * Show a dialog notifying the user that they must enter a name for the record
     */
    private void showCameraIdMustBeEntered() {
        new AlertDialog.Builder(getActivity())
            .setTitle(getString(R.string.must_set_camera_id_title))
            .setMessage(getString(R.string.must_set_camera_id_message))
            .setPositiveButton(getString(android.R.string.ok), null)
            .show();
    }


    // ************************************************************
    //  Seek Bar delegate methods
    // ************************************************************
    /**
     * Action when a user taps on the plus button of an item
     *
     * @param settingItemType
     */
    public void onPlusTapped(SettingItem.SettingItemType settingItemType) {
        switch (settingItemType) {
            case SettingItemTypeFrequency:
                mFrequencyMilliseconds += FREQUENCY_STEP_AMOUNT;
                if (mFrequencyMilliseconds > FREQUENCY_MILLIS_MAX_VALUE) {
                    mFrequencyMilliseconds = FREQUENCY_MILLIS_MAX_VALUE;
                }
                updateFrequencyTextView(mFrequencyMilliseconds);
                SettingsUtil.sharedInstance().setPictureFrequency(mFrequencyMilliseconds);
                updateFrequencySeekPosition();
                updateRequiredStorageTextView();
                updateAvailableStorageColor();
                break;

            case SettingItemTypeImageSize:
                mImageSize += IMAGE_SIZE_STEP_AMOUNT;
                if (mImageSize > IMAGE_SIZE_MAX_VALUE) {
                    mImageSize = IMAGE_SIZE_MAX_VALUE;
                }
                updateImageSizeTextView(mImageSize);
                SettingsUtil.sharedInstance().setImageSize(mImageSize);
                updateImageSizeSeekPosition();
                updateRequiredStorageTextView();
                updateAvailableStorageColor();
                break;

            case SettingItemTypeRecordingHours:
                mRecordingHours += RECORD_HOURS_STEP_AMOUNT;
                if (mRecordingHours > RECORD_HOURS_MAX) {
                    mRecordingHours = RECORD_HOURS_MAX;
                }
                updateRecordingHoursTextView(mRecordingHours);
                SettingsUtil.sharedInstance().setRecordingHours(mRecordingHours);
                updateRecordingHoursSeekPosition();
                updateRequiredStorageTextView();
                updateAvailableStorageColor();
                break;

            case SettingItemTypeExposure:
                int exposurePercent = SettingsUtil.sharedInstance().getCurrentExposureAsPercentage();
                exposurePercent++;
                if (exposurePercent > 100) {
                    exposurePercent = 100;
                }

                SettingsUtil.sharedInstance().setCurrentExposureFromPercentage(exposurePercent);
                updateExposureSeekPosition();
                updateExposureTextView();
                break;

            case SettingItemTypeFocusDistance:
                mFocusDistance += FOCUS_DISTANCE_STEP_AMOUNT;
                if (mFocusDistance > SettingsUtil.MAX_FOCUS_DISTANCE) {
                    mFocusDistance = SettingsUtil.MAX_FOCUS_DISTANCE;
                }
                updateFocusDistanceTextView(mFocusDistance);
                SettingsUtil.sharedInstance().setFocusDistance(mFocusDistance);
                updateFocusDistanceSeekPosition();
                updateRequiredStorageTextView();
                updateAvailableStorageColor();
                break;

            case SettingItemTypeFrameDuration:
                mFrameDuration++;
                if (mFrameDuration > 100) {
                    mFrameDuration = 100;
                }

                SettingsUtil.sharedInstance().setFrameDurationPercentage(mFrameDuration);
                updateFrameDurationTextView(mFrameDuration);
                updateFrameDurationSeekPosition();
                break;
        }
    }

    /**
     * Action when a user taps on the minus button of an item
     *
     * @param settingItemType
     */
    public void onMinusTapped(SettingItem.SettingItemType settingItemType) {
        switch (settingItemType) {
            case SettingItemTypeFrequency:
                mFrequencyMilliseconds -= FREQUENCY_STEP_AMOUNT;
                if (mFrequencyMilliseconds < FREQUENCY_MILLIS_MIN_VALUE) {
                    mFrequencyMilliseconds = FREQUENCY_MILLIS_MIN_VALUE;
                }
                updateFrequencyTextView(mFrequencyMilliseconds);
                SettingsUtil.sharedInstance().setPictureFrequency(mFrequencyMilliseconds);
                updateFrequencySeekPosition();
                updateRequiredStorageTextView();
                updateAvailableStorageColor();
                break;

            case SettingItemTypeImageSize:
                mImageSize -= IMAGE_SIZE_STEP_AMOUNT;
                if (mImageSize < IMAGE_SIZE_MIN_VALUE) {
                    mImageSize = IMAGE_SIZE_MIN_VALUE;
                }
                updateImageSizeTextView(mImageSize);
                SettingsUtil.sharedInstance().setImageSize(mImageSize);
                updateImageSizeSeekPosition();
                updateRequiredStorageTextView();
                updateAvailableStorageColor();
                break;

            case SettingItemTypeRecordingHours:
                mRecordingHours -= RECORD_HOURS_STEP_AMOUNT;
                if (mRecordingHours < RECORD_HOURS_MIN) {
                    mRecordingHours = RECORD_HOURS_MIN;
                }
                updateRecordingHoursTextView(mRecordingHours);
                SettingsUtil.sharedInstance().setRecordingHours(mRecordingHours);
                updateRecordingHoursSeekPosition();
                updateRequiredStorageTextView();
                updateAvailableStorageColor();
                break;

            case SettingItemTypeExposure:
                int exposurePercent = SettingsUtil.sharedInstance().getCurrentExposureAsPercentage();
                exposurePercent--;
                if (exposurePercent < 0) {
                    exposurePercent = 0;
                }

                SettingsUtil.sharedInstance().setCurrentExposureFromPercentage(exposurePercent);
                updateExposureSeekPosition();
                updateExposureTextView();
                break;

            case SettingItemTypeFocusDistance:
                mFocusDistance -= FOCUS_DISTANCE_STEP_AMOUNT;
                if (mFocusDistance < SettingsUtil.MIN_FOCUS_DISTANCE) {
                    mFocusDistance = SettingsUtil.MIN_FOCUS_DISTANCE;
                }
                updateFocusDistanceTextView(mFocusDistance);
                SettingsUtil.sharedInstance().setFocusDistance(mFocusDistance);
                updateFocusDistanceSeekPosition();
                updateRequiredStorageTextView();
                updateAvailableStorageColor();
                break;

            case SettingItemTypeFrameDuration:
                mFrameDuration--;
                if (mFrameDuration < 0) {
                    mFrameDuration = 0;
                }

                SettingsUtil.sharedInstance().setFrameDurationPercentage(mFrameDuration);
                updateFrameDurationTextView(mFrameDuration);
                updateFrameDurationSeekPosition();
                break;

        }
    }

    /**
     * Action when an items seek bar is updated
     *
     * @param settingItemType
     * @param newValue
     */
    public void onSeekBarPositionUpdated(SettingItem.SettingItemType settingItemType, int newValue) {
        float valuePercentage = (float)newValue / 100.0f;

        switch (settingItemType) {
            case SettingItemTypeFrequency:
                // The frequency value has been changed
                mFrequencyMilliseconds = roundValueToClosest(FREQUENCY_MILLIS_MIN_VALUE + Math.round((FREQUENCY_MILLIS_MAX_VALUE - FREQUENCY_MILLIS_MIN_VALUE) * valuePercentage), 500);
                updateFrequencyTextView(mFrequencyMilliseconds);
                SettingsUtil.sharedInstance().setPictureFrequency(mFrequencyMilliseconds);
                break;

            case SettingItemTypeImageSize:
                // The image size value has been changed
                mImageSize = roundValueToClosest(IMAGE_SIZE_MIN_VALUE + Math.round((IMAGE_SIZE_MAX_VALUE - IMAGE_SIZE_MIN_VALUE)*valuePercentage), 10);
                updateImageSizeTextView(mImageSize);
                SettingsUtil.sharedInstance().setImageSize(mImageSize);
                break;

            case SettingItemTypeRecordingHours:
                // The recording hours value has been changed
                mRecordingHours = roundValueToClosest(Math.round((RECORD_HOURS_MIN + (RECORD_HOURS_MAX - RECORD_HOURS_MIN)*valuePercentage)*100), 50)/100.0f;
                updateRecordingHoursTextView(mRecordingHours);
                SettingsUtil.sharedInstance().setRecordingHours(mRecordingHours);
                break;

            case SettingItemTypeExposure:
                // The exposure value has changed
                SettingsUtil.sharedInstance().setCurrentExposureFromPercentage(newValue);
                updateExposureTextView();
                break;

            case SettingItemTypeFocusDistance:
                // The focus distance value has changed
                mFocusDistance = roundValueToClosest(Math.round((RECORD_HOURS_MIN + (RECORD_HOURS_MAX - RECORD_HOURS_MIN)*valuePercentage)*100), 50)/100.0f;
                updateFocusDistanceTextView(mFocusDistance);
                SettingsUtil.sharedInstance().setFocusDistance(mFocusDistance);
                break;

            case SettingItemTypeFrameDuration:
                // The frame duration value has changed
                mFrameDuration = newValue;
                updateFrameDurationTextView(mFrameDuration);
                SettingsUtil.sharedInstance().setFrameDurationPercentage(newValue);
                break;
        }
        updateRequiredStorageTextView();
        updateAvailableStorageColor();
    }


    /**
     * Action when the value of the seek bar has been changed
     *
     * @param seekBar   The seek bar that the event was sent from
     * @param progress  the progress value that the seek bar has updated up to
     * @param fromUser  true if the progress was changed via a user action
     */
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    /**
     * Action when the user starts move the value on the seek bar
     *
     * @param seekBar  The seek bar that this event occurred on
     */
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    /**
     * Action when the user has stopped moving the value on the seek bar
     *
     * @param seekBar  The seek bar that this event occurred on
     */
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    /**
     * Override and handle on back action return true if consumed the event
     * (if the parent activity should not close as is its normal coarse of action) otherwise
     * false
     *
     * @return true if activity should handle the back action, otherwise false
     */
    public boolean onBackClicked() {
        if (CalculationUtil.sharedInstance().getAvailableStorageConditionColor() ==
                ContextCompat.getColor(getActivity(), R.color.no_storage) &&
                !mExitConfirmed) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.not_enough_storage_title))
                    .setMessage(getString(R.string.not_enough_storage_message))
                    .setPositiveButton(getString(R.string.continue_anyway), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mExitConfirmed = true;
                            // The user wants to continue with the selected settings event though
                            // there is not enough space to perform a days work, tell the
                            // activity to close
                            getActivity().onBackPressed();
                        }
                    })
                    .setNeutralButton(getString(R.string.cancel), null)
                    .show();
            return true;
        } else {
            return false;
        }
    }

}
