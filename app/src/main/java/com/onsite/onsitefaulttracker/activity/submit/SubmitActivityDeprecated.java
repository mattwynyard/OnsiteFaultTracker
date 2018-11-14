package com.onsite.onsitefaulttracker.activity.submit;

import android.os.Bundle;

import com.onsite.onsitefaulttracker.activity.BaseActivity;

/**
 * Created by hihi on 6/25/2016.
 *
 * Submit Activity,  the activity which allows the user to submit and upload records
 * to DropBox
 */
public class SubmitActivityDeprecated extends BaseActivity {

    // The tag name for this activity
    private static final String TAG = SubmitActivityDeprecated.class.getSimpleName();

    // The record id key for passing in the id of the record that is to be submitted
    public static final String EXTRA_RECORD_ID = "record_id";

    /**
     * create and return an instance of SubmitActivityDeprecated.
     *
     * @return
     */
    @Override
    protected SubmitFragmentDeprecated getDefaultFragment() {
        final String recordId = getIntent().getStringExtra(EXTRA_RECORD_ID);

        return SubmitFragmentDeprecated.createInstance(recordId);
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
     * Return the back button action bar config for this activity
     *
     * @return
     */
    @Override
    protected BaseActivity.ActionBarConfig getDefaultActionBarConfig() {
        return ActionBarConfig.Back;
    }


}
