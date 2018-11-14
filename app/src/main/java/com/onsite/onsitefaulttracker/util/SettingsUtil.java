package com.onsite.onsitefaulttracker.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by hihi on 6/8/2016.
 *
 * Settings Util,
 * The single access point for the application settings.
 * All application settings can be set and retrieved here.
 * Stores settings in persistent storage.
 */
public class SettingsUtil {

    // The tag name for this utility class
    private static final String TAG = SettingsUtil.class.getSimpleName();

    // The name for the settings shared preferences
    private static final String SETTINGS_PREFERENCES = "settings_preferences";

    // The preference key for the picture frequency setting for storing persistently
    private static final String KEY_PICTURE_FREQUENCY = "picture_frequency";

    // The preference key for the desired image size
    private static final String KEY_IMAGE_SIZE = "image_size";

    // The preferences key for the usual recording hours
    private static final String KEY_RECORDING_HOURS = "recording_hours";

    // The preferences key for the exposure value
    private static final String KEY_EXPOSURE = "exposure";

    // The preference key for focus distance
    private static final String KEY_FOCUS_DISTANCE = "focus_distance";

    // The preference key for frame duration percentage
    private static final String KEY_FRAME_DURATION_PERCENTAGE = "frame_duration";

    // The id of the current camera
    private static final String KEY_CAMERA_ID = "camera_id";

    // The default value for picture frequency
    private static final long DEFAULT_PICTURE_FREQUENCY = 1000;

    // The default desired image size in KB
    private static final long DEFAULT_IMAGE_SIZE = 200;

    // The default expected record hours per recording
    private static final float DEFAULT_RECORD_HOURS = 6.0f;

    // The default exposure value
    private static final long DEFAULT_EXPOSURE = 2000000;

    // The default frame duration
    private static final long DEFAULT_FRAME_DURATION_PERCENTAGE = 0;

    // The default focus distance
    private static final float DEFAULT_FOCUS_DISTANCE = 1.5f;

    // The minimum exposure value
    private static final long MIN_EXPOSURE = 1500000;

    // The maximum exposure value
    private static final long MAX_EXPOSURE = 12000000;

    // The minimum focus distance value
    public static final float MIN_FOCUS_DISTANCE = 0.5f;

    // The maximum focus distance value
    public static final float MAX_FOCUS_DISTANCE = 6.0f;

    // Static instance of the settings utilities that is initialized
    // once and used throughout the application.
    private static SettingsUtil sSettingsUtil;

    // The desired size in KB of each image
    private long mImageSize;

    // The frequency to take pictures at in milliseconds
    private long mPictureFrequencyMillis;

    // The usual recording time in hours
    private float mUsualRecordingHours;

    // The camera exposure while making a record
    private long mExposure;

    // The current frame duration
    private long mFrameDuration;

    // The current focus distance setting
    private float mFocusDistance;

    // The camera id of this device
    private String mCameraId;

    // The application context
    private Context mContext;

    // Is currently recording
    private boolean mCurrentlyRecording;

    /**
     * Initialize SettingsUtil by passing in the appilcation context
     *
     * @param context The application context
     */
    public static void initialize(final Context context) {
        sSettingsUtil = new SettingsUtil(context);
    }

    /**
     * Return the shared instance of SettingsUtil
     *
     * @return The shared instance of SettingsUtil
     */
    public static SettingsUtil sharedInstance() {
        if (sSettingsUtil != null) {
            return sSettingsUtil;
        } else {
            throw new RuntimeException("SettingsUtil must be initialized in the Application class");
        }
    }

    /**
     * Constructor for SettingsUtil,
     * To be instantiated internally via initialize
     *
     * @param context The application context
     */
    private SettingsUtil(final Context context) {
        mContext = context;
        loadStoredSettings();
    }

    /**
     * Load the previously stored settings or defaults if none
     * have been set yet.
     */
    private void loadStoredSettings() {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(SETTINGS_PREFERENCES, Context.MODE_PRIVATE);
        mImageSize = sharedPreferences.getLong(KEY_IMAGE_SIZE, DEFAULT_IMAGE_SIZE);
        mPictureFrequencyMillis = sharedPreferences.getLong(KEY_PICTURE_FREQUENCY, DEFAULT_PICTURE_FREQUENCY);
        mUsualRecordingHours = sharedPreferences.getFloat(KEY_RECORDING_HOURS, DEFAULT_RECORD_HOURS);
        mExposure = sharedPreferences.getLong(KEY_EXPOSURE, DEFAULT_EXPOSURE);
        mCameraId = sharedPreferences.getString(KEY_CAMERA_ID, "");
        mFrameDuration = sharedPreferences.getLong(KEY_FRAME_DURATION_PERCENTAGE, DEFAULT_FRAME_DURATION_PERCENTAGE);
        mFocusDistance = sharedPreferences.getFloat(KEY_FOCUS_DISTANCE, DEFAULT_FOCUS_DISTANCE);
    }

    /**
     * resets all settings to their original state
     */
    public void resetSettings() {
        setImageSize(DEFAULT_IMAGE_SIZE);
        setPictureFrequency(DEFAULT_PICTURE_FREQUENCY);
        setRecordingHours(DEFAULT_RECORD_HOURS);
        setExposure(DEFAULT_EXPOSURE);
        setFrameDurationPercentage(DEFAULT_FRAME_DURATION_PERCENTAGE);
        setFocusDistance(DEFAULT_FOCUS_DISTANCE);
    }

    /**
     * Set the desired frame duration percentage
     *
     * @param frameDuration
     */
    public void setFrameDurationPercentage(final long frameDuration) {
        mFrameDuration = frameDuration;
        SharedPreferences preferences = mContext.getSharedPreferences(SETTINGS_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(KEY_FRAME_DURATION_PERCENTAGE, mFrameDuration);
        editor.apply();
    }

    /**
     * Get the current frame duration percentage
     *
     * @return
     */
    public long getFrameDurationPercentage() {
        return mFrameDuration;
    }

    /**
     * Set the desired focus distance in meters
     *
     * @param focusDistance
     */
    public void setFocusDistance(final float focusDistance) {
        mFocusDistance = focusDistance;
        SharedPreferences preferences = mContext.getSharedPreferences(SETTINGS_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putFloat(KEY_FOCUS_DISTANCE, mFocusDistance);
        editor.apply();
    }

    public float getFocusDistance() {
        return mFocusDistance;
    }

    /**
     * Set the desired exposure
     *
     * @param exposure The desired exposure for the recording camera
     */
    public void setExposure(final long exposure) {
        mExposure = exposure;
        SharedPreferences preferences = mContext.getSharedPreferences(SETTINGS_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(KEY_EXPOSURE, mExposure);
        editor.apply();
    }

    /**
     * get the desired Exposure
     *
     * @return desired exposure
     */
    public long getExposure() {
        return mExposure;
    }

    /**
     * Return the current exposure value as a percentage between 0 and 100
     * @return current exposure value as a percentage between 0 and 100
     */
    public int getCurrentExposureAsPercentage() {
        return (int)Math.round((double)(mExposure - MIN_EXPOSURE) / (double)(MAX_EXPOSURE - MIN_EXPOSURE) * 100.00);
    }

    /**
     * Sets the exposure value given a percentage value from 0 - 100
     * @param percentage value from 0 - 100 to set the exposure value as
     */
    public void setCurrentExposureFromPercentage(final int percentage) {
        setExposure(MIN_EXPOSURE + Math.round((percentage / 100.0) * (MAX_EXPOSURE - MIN_EXPOSURE)));
    }

    /**
     * Set the desired image size of each photo in KB
     *
     * @param imageSize The desired image size of each photo in KB
     */
    public void setImageSize(final long imageSize) {
        mImageSize = imageSize;
        SharedPreferences preferences = mContext.getSharedPreferences(SETTINGS_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(KEY_IMAGE_SIZE, mImageSize);
        editor.apply();
    }

    /**
     * get the desired image size in KB
     *
     * @return desired image size in KB
     */
    public long getImageSize() {
        return mImageSize;
    }

    /**
     * Set the picture frequency in milliseconds
     *
     * @param pictureFrequency The picture frequency in milliseconds
     */
    public void setPictureFrequency(final long pictureFrequency) {
        mPictureFrequencyMillis = pictureFrequency;
        SharedPreferences preferences = mContext.getSharedPreferences(SETTINGS_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(KEY_PICTURE_FREQUENCY, mPictureFrequencyMillis);
        editor.apply();
    }

    /**
     * Get the picture frequency in milliseconds
     *
     * @return The frequence that each photo should be taken in milliseconds
     */
    public long getPictureFrequency() {
        return mPictureFrequencyMillis;
    }

    /**
     * Set the expected/average recording hours
     *
     * @param recordingHours The expected/average recording hours of each session
     */
    public void setRecordingHours(final float recordingHours) {
        mUsualRecordingHours = recordingHours;
        SharedPreferences preferences = mContext.getSharedPreferences(SETTINGS_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putFloat(KEY_RECORDING_HOURS, mUsualRecordingHours);
        editor.apply();
    }

    /**
     * Get the usual recording hours setting for each work day
     *
     * @return The usual recording hours of each work day
     */
    public float getRecordingHours() {
        return mUsualRecordingHours;
    }

    /**
     * Set the camera id for this device
     *
     * @param cameraId The camera id to set this device to
     */
    public void setCameraId(final String cameraId) {
        mCameraId = cameraId;
        SharedPreferences preferences = mContext.getSharedPreferences(SETTINGS_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_CAMERA_ID, mCameraId);
        editor.apply();
    }

    /**
     * Get the camera Id
     *
     * @return the camera id of this device
     */
    public String getCameraId() {
        return mCameraId;
    }
}
