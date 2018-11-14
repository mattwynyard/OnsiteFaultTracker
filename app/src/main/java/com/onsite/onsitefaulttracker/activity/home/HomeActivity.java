package com.onsite.onsitefaulttracker.activity.home;

import android.content.Intent;
import android.os.Bundle;

import com.onsite.onsitefaulttracker.activity.BaseActivity;
import com.onsite.onsitefaulttracker.activity.BaseFragment;
import com.onsite.onsitefaulttracker.activity.previous.PreviousRecordsActivity;
import com.onsite.onsitefaulttracker.activity.record.RecordActivity;
import com.onsite.onsitefaulttracker.activity.submit.SubmitActivity;

/**
 * The Home Activity is the activity for the Home screen is where the user
 * can select to make a new record,  view previous records or continue making
 * a previous record. The user can also access the settings screen from the
 * settings button in the action bar.
 */
public class HomeActivity extends BaseActivity implements HomeFragment.Listener {

    /**
     * create and return an instance of HomeFragment.
     *
     * @return
     */
    @Override
    protected BaseFragment getDefaultFragment() {
        return HomeFragment.createInstance();
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
     * Return the Settings only action bar config for this activity
     *
     * @return
     */
    @Override
    protected BaseActivity.ActionBarConfig getDefaultActionBarConfig() {
        return ActionBarConfig.Settings;
    }

    /**
     * Action when user has selected to create a new record.
     * Open the Record screen with a new record.
     */
    @Override
    public void onNewRecord() {
        Intent recordIntent = new Intent();
        recordIntent.setClass(this, RecordActivity.class);
        startActivity(recordIntent);
    }

    /**
     * Action when user clicks on previous records
     */
    @Override
    public void onPreviousRecords() {
        Intent previousRecordsIntent = new Intent();
        previousRecordsIntent.setClass(this, PreviousRecordsActivity.class);
        startActivity(previousRecordsIntent);
    }

    /**
     * Action when user clicks on submit records
     */
    @Override
    public void onSubmitRecord(final String recordId) {
        Intent submitRecordIntent = new Intent();
        submitRecordIntent.setClass(this, SubmitActivity.class);
        submitRecordIntent.putExtra(SubmitActivity.EXTRA_RECORD_ID, recordId);

        startActivity(submitRecordIntent);
    }

    /**
     * Action when user clicks to open the settings screen
     */
    @Override
    public void onOpenSettings() {
        onSettingsClicked();
    }

}
