package com.onsite.onsitefaulttracker.activity.record;

import android.os.Bundle;

import com.onsite.onsitefaulttracker.activity.BaseActivity;
import com.onsite.onsitefaulttracker.activity.BaseFragment;

/**
 * Created by hihi on 6/12/2016.
 *
 * Record Activity is the activity which performs the bulk of the work for this app.
 * It takes a photo every set time interval and saves them to storage with the date stamp
 * filename
 */
public class RecordActivity extends BaseActivity {

    // The Tag name for this activity
    private static final String TAG = RecordActivity.class.getSimpleName();

    /**
     * create and return an instance of RecordFragment.
     *
     * @return
     */
    @Override
    protected BaseFragment getDefaultFragment() {
        return RecordFragment.createInstance();
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
     * Return the hidden option for the action bar as this displays fullscreen
     *
     * @return
     */
    @Override
    protected BaseActivity.ActionBarConfig getDefaultActionBarConfig() {
        return ActionBarConfig.Hidden;
    }

}
