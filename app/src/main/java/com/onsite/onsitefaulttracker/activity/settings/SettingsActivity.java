package com.onsite.onsitefaulttracker.activity.settings;

import android.os.Bundle;

import com.onsite.onsitefaulttracker.activity.BaseActivity;
import com.onsite.onsitefaulttracker.activity.BaseFragment;

/**
 * Created by hihi on 6/7/2016.
 *
 * SettingsActivity is the Activity for the Settings Screen,
 * On the Settings screen the user can change settings related
 * to how the app creates a record such as:
 *
 *
 */
public class SettingsActivity extends BaseActivity {

    // The Tag name for this activity/
    private static final String TAG = SettingsActivity.class.getSimpleName();

    /**
     * create and return an instance of SettingsFragment.
     *
     * @return
     */
    @Override
    protected BaseFragment getDefaultFragment() {
        return SettingsFragment.createInstance();
    }

    /**
     * Sets up the activity and default fragment.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Return the Back only action bar config for this activity
     *
     * @return
     */
    @Override
    protected BaseActivity.ActionBarConfig getDefaultActionBarConfig() {
        return ActionBarConfig.Back;
    }

}
