package com.onsite.onsitefaulttracker.activity.previous;

import android.content.Intent;
import android.os.Bundle;

import com.onsite.onsitefaulttracker.activity.BaseActivity;
import com.onsite.onsitefaulttracker.activity.record.RecordActivity;
import com.onsite.onsitefaulttracker.activity.submit.SubmitActivity;
import com.onsite.onsitefaulttracker.model.Record;

/**
 * Created by hihi on 6/25/2016.
 *
 * The previous records activity,
 * displays a list of previously created records and their details to the user,
 * the user can then resume creating or submitting a record, or delete a record
 */
public class PreviousRecordsActivity extends BaseActivity implements PreviousRecordsFragment.Listener {

    /**
     * create and return an instance of PreviousRecordsActivity.
     *
     * @return
     */
    @Override
    protected PreviousRecordsFragment getDefaultFragment() {
        return PreviousRecordsFragment.createInstance();
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
        return BaseActivity.ActionBarConfig.Back;
    }

    /**
     * Action when user shooses to record a record
     *
     * @param record
     */
    @Override
    public void onRecordRecord(final Record record) {
        Intent recordIntent = new Intent();
        recordIntent.setClass(this, RecordActivity.class);
        startActivity(recordIntent);
    }

    /**
     * Action when the user chooses to upload a record,
     * open the submit record screen
     *
     * @param record
     */
    @Override
    public void onUploadRecord(final Record record) {
        Intent submitRecordIntent = new Intent();
        submitRecordIntent.setClass(this, SubmitActivity.class);
        submitRecordIntent.putExtra(SubmitActivity.EXTRA_RECORD_ID, record.recordId);

        startActivity(submitRecordIntent);
    }

}
