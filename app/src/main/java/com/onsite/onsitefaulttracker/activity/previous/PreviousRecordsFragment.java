package com.onsite.onsitefaulttracker.activity.previous;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.onsite.onsitefaulttracker.R;
import com.onsite.onsitefaulttracker.activity.BaseFragment;
import com.onsite.onsitefaulttracker.activity.home.HomeFragment;
import com.onsite.onsitefaulttracker.model.Record;
import com.onsite.onsitefaulttracker.util.RecordUtil;
import com.onsite.onsitefaulttracker.util.ThreadUtil;

import java.util.ArrayList;

/**
 * Created by hihi on 6/25/2016.
 *
 * The previous records fragment,
 * displays a list of previously created records and their details to the user,
 * the user can then resume creating or submitting a record, or delete a record
 */
public class PreviousRecordsFragment extends BaseFragment implements PreviousRecordsAdapter.RecordItemListener {

    // The TAG name for this fragment
    private static final String TAG = PreviousRecordsFragment.class.getSimpleName();

    // List View which will display the previously created records
    private ListView mPreviousRecordsList;

    // The adapter for previous records list
    private PreviousRecordsAdapter mPreviousRecordsAdapter;

    // List of records which are in storage
    private ArrayList<Record> mRecords;

    // Listener for communicating with the parent activity
    private Listener mListener;

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
            mPreviousRecordsList = (ListView)view.findViewById(R.id.previous_records_list);
        }
        return view;
    }

    /**
     * Action on resume, load the record list
     */
    public void onResume() {
        super.onResume();
        populatePreviousRecordsList();
    }

    /**
     * Action on attach,  set the passed in context as the listener
     *
     * @param context
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof Listener) {
            mListener = (Listener)context;
        }
    }

    /**
     * Action on detach from parent activity, nullify the listener as its no longer valid.
     */
    @Override
    public void onDetach() {
        super.onDetach();

        mListener = null;
    }

    /**
     * populate the list of Previous Records
     */
    private void populatePreviousRecordsList() {
        mRecords = RecordUtil.sharedInstance().getAllSavedRecords();
        mPreviousRecordsAdapter = new PreviousRecordsAdapter(mRecords, getActivity());
        mPreviousRecordsAdapter.setRecordItemListener(this);
        mPreviousRecordsList.setAdapter(mPreviousRecordsAdapter);
    }

    /**
     * Delete the record from storage
     *
     * @param record
     */
    private void deleteRecord(final Record record) {
        RecordUtil.sharedInstance().deleteRecord(record);
        ThreadUtil.executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                populatePreviousRecordsList();
            }
        });
    }

    /**
     * Confirm that the user wants to delete a record
     *
     * @param record
     */
    private void confirmDeleteRecord(final Record record) {
        boolean recordFinalized = record.uploadedSizeKB >= record.totalSizeKB;
        String deleteMessage = recordFinalized ? getString(R.string.delete_previous_record_message) :
                getString(R.string.delete_previous_record_not_finalized_message);

        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.delete_previous_record_title))
                .setMessage(deleteMessage)
                .setPositiveButton(getString(R.string.delete_previous_record_delete), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteRecord(record);
                    }
                })
                .setNegativeButton(getString(R.string.delete_previous_record_cancel), null)
                .show();
    }

    /**
     * Show the more options dialog
     */
    private void showMoreOptionsDialog(final Record record) {
        RelativeLayout relativeLayout = new RelativeLayout(getActivity());
        ViewGroup moreOptionsLayout = (ViewGroup)getActivity().getLayoutInflater().inflate(R.layout.more_dialog_layout, relativeLayout);
        final AlertDialog moreOptionsAlert = new AlertDialog.Builder(getActivity())
                .setView(moreOptionsLayout)
                .show();

        final TextView nameTextView = (TextView)moreOptionsLayout.findViewById(R.id.record_name_text_view);

        final Button submitButton = (Button)moreOptionsLayout.findViewById(R.id.submit_record_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onUploadButtonClicked(record);
            }
        });

        final Button continueButton = (Button)moreOptionsLayout.findViewById(R.id.continue_recording_button);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRecordButtonClicked(record);
            }
        });

        final Button deleteButton = (Button)moreOptionsLayout.findViewById(R.id.delete_record_button);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moreOptionsAlert.dismiss();
                confirmDeleteRecord(record);
            }
        });
        nameTextView.setText(record.recordName);

    }

    /**
     * action when user clicks on the record button on an item
     *
     * @param recordItem
     */
    @Override
    public void onRecordButtonClicked(final Record recordItem) {
        RecordUtil.sharedInstance().setCurrentRecord(recordItem);
        if (mListener != null) {
            mListener.onRecordRecord(recordItem);
        }
    }

    /**
     * action when the user clicks on the upload button on an item
     *
     * @param recordItem
     */
    @Override
    public void onUploadButtonClicked(final Record recordItem) {
        if (mListener != null) {
            mListener.onUploadRecord(recordItem);
        }
    }

    /**
     * action when the user clicks on the delete button of a finalized item
     *
     * @param recordItem
     */
    @Override
    public void onDeleteButtonClicked(final Record recordItem) {
        confirmDeleteRecord(recordItem);
    }

    /**
     * action when the user clicks on the more button on an item
     *
     * @param recordItem
     */
    @Override
    public void onMoreButtonClicked(final Record recordItem) {
        showMoreOptionsDialog(recordItem);
    }

    /**
     * instantiate and return an instance of this fragment
     *
     * @return
     */
    public static PreviousRecordsFragment createInstance() {
        return new PreviousRecordsFragment();
    }

    /**
     * Returns the display title for this fragment
     *
     * @return
     */
    @Override
    protected String getDisplayTitle() {
        return getString(R.string.previous_records);
    }

    /**
     * Returns the layout resource for this fragment
     *
     * @return
     */
    @Override
    protected int getLayoutResourceId() {
        return R.layout.fragment_previous_records;
    }

    /**
     * Listener for communicating user actions to the parent activity
     */
    public interface Listener {
        void onRecordRecord(final Record record);
        void onUploadRecord(final Record record);
    }
}
