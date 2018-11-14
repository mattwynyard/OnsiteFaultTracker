package com.onsite.onsitefaulttracker.activity.submit;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.onsite.onsitefaulttracker.R;
import com.onsite.onsitefaulttracker.activity.BaseFragment;
import com.onsite.onsitefaulttracker.dropboxapi.DropboxCallback;
import com.onsite.onsitefaulttracker.dropboxapi.DropboxClient;
import com.onsite.onsitefaulttracker.model.Record;
import com.onsite.onsitefaulttracker.util.CalculationUtil;
import com.onsite.onsitefaulttracker.util.RecordUtil;
import com.onsite.onsitefaulttracker.util.ThreadUtil;

import java.io.File;
import java.text.SimpleDateFormat;

/**
 * Created by hihi on 6/25/2016.
 *
 * The Submit Fragment, The default fragment of submit Activity.
 * Here the user can submit their record and upload it to drop box
 */
public class SubmitFragmentDeprecated extends BaseFragment implements DropboxClient.DropboxClientListener {

    // The tag name for this fragment
    private static final String TAG = SubmitFragmentDeprecated.class.getSimpleName();

    // Id of the record to submit
    public static final String ARG_RECORD_ID = "record_id";

    // The name text view
    private TextView mNameTextView;

    // The date text view
    private TextView mDateTextView;

    // The total size of the record
    private TextView mTotalSizeTextView;

    // The remaining size
    private TextView mRemainingTextView;

    // The percentage text view
    private TextView mPercentageTextView;

    // The text view that says Record Submitted, initially hidden, displayed on
    // completion
    private TextView mRecordSubmittedTextView;

    // Image view which displays the currently uploading image
    private ImageView mCurrentImageView;

    // The progress indicator which shows the user that the record is being submitted
    private ProgressBar mSubmittingProgressBar;

    // The submit button
    private Button mSubmitButton;

    // The id of the record that is being submitted
    private String mRecordId;

    // The dropbox client which handles interaction with dropbox
    private DropboxClient mDropboxClient;

    // Record that is to be submitted
    private Record mRecord;

    // Array of record files to be uploaded
    private File[] mRecordFiles;

    // Is this fragment currently resumed?
    boolean mResumed;

    // Is it currently submitting the record
    boolean mSubmitting;

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
            Bundle args = getArguments();
            if (args != null) {
                mRecordId = args.getString(ARG_RECORD_ID);
            }

            mNameTextView = (TextView) view.findViewById(R.id.record_name_text_view);
            mDateTextView = (TextView) view.findViewById(R.id.record_creation_date_text_view);
            mTotalSizeTextView = (TextView) view.findViewById(R.id.total_size_text_view);
            mRemainingTextView = (TextView) view.findViewById(R.id.remaining_size_text_view);
            mSubmitButton = (Button) view.findViewById(R.id.submit_record_button);
            mSubmitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSubmitClicked();
                }
            });
            mPercentageTextView = (TextView) view.findViewById(R.id.uploaded_percentage_text_view);
            mCurrentImageView = (ImageView) view.findViewById(R.id.current_image_id);
            mSubmittingProgressBar = (ProgressBar) view.findViewById(R.id.submitting_progress_bar);
            mRecordSubmittedTextView = (TextView) view.findViewById(R.id.record_submitted_text_view);

            mRecord = RecordUtil.sharedInstance().getRecordWithId(mRecordId);

            updateUIValues();
        }
        return view;
    }

    /**
     * Action on resume
     */
    public void onResume() {
        super.onResume();

        if (mDropboxClient == null) {
            mDropboxClient = new DropboxClient(getActivity());
        } else {
            mDropboxClient.updateActivity(getActivity());
        }
        mDropboxClient.setDropboxClientListener(this);

        mDropboxClient.onDropboxActivityResumed();

    }

    /**
     * Action on attached
     */
    public void onAttach(Context context) {
        super.onAttach(context);

        mResumed = true;
    }

    /**
     * Action on detach
     */
    public void onDetach() {
        super.onDetach();
        mResumed = false;
        mSubmitting = false;
    }

    /**
     * Update the ui with the record details
     */
    private void updateUIValues() {
        mNameTextView.setText(mRecord.recordName);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMMM yyyy, h:mm a");
        mDateTextView.setText(String.format(getString(R.string.submit_created_date), simpleDateFormat.format(mRecord.creationDate)));
        mTotalSizeTextView.setText(String.format(getString(R.string.submit_total_size), CalculationUtil.sharedInstance().getDisplayValueFromKB(mRecord.totalSizeKB)));
        final long remainingKB = mRecord.totalSizeKB - mRecord.uploadedSizeKB;
        mRemainingTextView.setText(String.format(getString(R.string.submit_remaining_size), CalculationUtil.sharedInstance().getDisplayValueFromKB(Math.max(remainingKB, 0))));
        float percentage = ((float)mRecord.uploadedSizeKB / (float)mRecord.totalSizeKB) * 100.0f;
        percentage = Math.min(100.0f, percentage);
        mPercentageTextView.setText(String.format("%.0f%%", percentage));

        // If the submission is already complete show that
        if (mRecord.fileUploadCount >= mRecord.photoCount) {
            onSubmissionComplete();
        }
    }

    /**
     * Upload the next file from the record files.
     */
    private void uploadNextFile() {
        if (!mResumed) {
            // Don't attempt to upload a file if the fragment is paused.
            return;
        }
        // If all files have already been uploaded dont upload another one
        if (mRecord.fileUploadCount >= mRecordFiles.length) {
            mSubmitting = false;

            return;
        }

        if (mCurrentImageView.getVisibility() != View.VISIBLE) {
            mCurrentImageView.setVisibility(View.VISIBLE);
        }
        final Bitmap uploadingBitmap = mRecordFiles[mRecord.fileUploadCount].getName().contains(".jpg") ?
                BitmapFactory.decodeFile(mRecordFiles[mRecord.fileUploadCount].getAbsolutePath()) : null;
        mCurrentImageView.setImageBitmap(uploadingBitmap);

        mDropboxClient.uploadNextFile(mRecord, mRecordFiles, new DropboxCallback() {
            @Override
            public void onSuccess(Object object) {
                ThreadUtil.executeOnNewThread(new Runnable() {
                    @Override
                    public void run() {
                        RecordUtil.sharedInstance().saveRecord(mRecord);
                    }
                });

                // If the fragment is not active just return
                if (!mResumed) {
                    return;
                }

                updateUIValues();

                mCurrentImageView.setImageBitmap(null);
                if (uploadingBitmap != null) {
                    uploadingBitmap.recycle();
                }

                if (mRecord.fileUploadCount < mRecordFiles.length) {
                    uploadNextFile();
                } else {
                    mSubmitting = false;
                    onSubmissionComplete();
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                mSubmitting = false;
                // If the fragment is not active just return
                if (!mResumed) {
                    return;
                }

                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.record_submit_failed_title))
                        .setMessage(getString(R.string.record_submit_failed_message))
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                getActivity().onBackPressed();
                            }
                        })
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                getActivity().onBackPressed();
                            }
                        })
                        .setPositiveButton(getString(android.R.string.ok), null)
                        .show();
            }
        });
    }

    /**
     * Action when submitting a record has completed
     */
    private void onSubmissionComplete() {
        mPercentageTextView.setVisibility(View.INVISIBLE);
        mRecordSubmittedTextView.setVisibility(View.VISIBLE);
        mSubmittingProgressBar.setVisibility(View.INVISIBLE);
        mSubmitButton.setVisibility(View.INVISIBLE);
    }

    /**
     * Start uploading the record to dropbox
     */
    private void startUploadingRecord() {
        mSubmittingProgressBar.setVisibility(View.VISIBLE);
        mPercentageTextView.setVisibility(View.VISIBLE);
        mSubmitButton.setEnabled(false);
        mSubmitButton.setText(getString(R.string.submitting));
        mSubmitting = true;

        mDropboxClient.confirmOrCreateRecord(mRecord, new DropboxCallback() {
            @Override
            public void onSuccess(Object object) {
                Log.i(TAG, "Record is created on dropbox, starting image uploading");
                mRecordFiles = RecordUtil.sharedInstance().getRecordFiles(mRecord.recordId);

                if (mRecordFiles != null && mRecordFiles.length > 0) {
                    uploadNextFile();
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Error creating record on dropbox: " + errorMessage);
            }
        });
    }

    /**
     * Action when the user clicks on submit,  initiate the dropbox api
     * and start uploading the record
     */
    private void onSubmitClicked() {
        /*
        mRecordFiles = RecordUtil.sharedInstance().getRecordFiles(mRecord.recordId);
        String fileNames[] = new String[mRecordFiles.length];
        for (int i = 0; i < mRecordFiles.length; i++) {
            fileNames[i] = mRecordFiles[i].getAbsolutePath();
        }
        Log.i(TAG, "START ZIP");
        String outPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/tmpfile.zip";
        Compressor compressor = new Compressor(fileNames, outPath);
        compressor.zip();
        Log.i(TAG, "END ZIP");
*/
        mDropboxClient.authenticateDropBoxUser();
    }

    /**
     * Override and handle on back action return true if consumed the event
     * (if the parent activity should not close as is its normal coarse of action) otherwise
     * false
     *
     * @return true if activity should handle the back action, otherwise false
     */
    public boolean onBackClicked() {
        if (mSubmitting) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.submitting_back_dialog_title))
                    .setMessage(getString(R.string.submitting_back_dialog_message))
                    .setPositiveButton(getString(R.string.submitting_back_dialog_exit), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mSubmitting = false;
                            getActivity().onBackPressed();
                        }
                    })
                    .setNegativeButton(getString(R.string.submitting_back_dialog_cancel), null)
                    .show();

            return true;
        } else {
            return false;
        }
    }


    /**
     * Called when dropbox has been authenticated,
     * start uploading the record
     */
    @Override
    public void onDropboxAuthenticated() {
        startUploadingRecord();
    }

    /**
     * Called when dropbox fails / has an error initializing
     */
    @Override
    public void onDropboxFailed() {

    }

    /**
     * instantiate and return an instance of this fragment
     *
     * @return
     */
    public static SubmitFragmentDeprecated createInstance(final String recordId) {
        final SubmitFragmentDeprecated submitFragmentDeprecated = new SubmitFragmentDeprecated();

        Bundle args = new Bundle();
        args.putString(ARG_RECORD_ID, recordId);
        submitFragmentDeprecated.setArguments(args);

        return submitFragmentDeprecated;
    }

    /**
     * Returns the display title for this fragment
     *
     * @return
     */
    @Override
    protected String getDisplayTitle() {
        return getString(R.string.submit_record);
    }

    /**
     * Returns the layout resource for this fragment
     *
     * @return
     */
    @Override
    protected int getLayoutResourceId() {
        return R.layout.fragment_submit_deprecated;
    }

}
