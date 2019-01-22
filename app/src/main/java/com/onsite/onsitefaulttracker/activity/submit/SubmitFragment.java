package com.onsite.onsitefaulttracker.activity.submit;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.users.FullAccount;
import com.onsite.onsitefaulttracker.R;
import com.onsite.onsitefaulttracker.activity.BaseFragment;
import com.onsite.onsitefaulttracker.dropbox.UploadCallback;
import com.onsite.onsitefaulttracker.model.Record;
import com.onsite.onsitefaulttracker.util.CalculationUtil;
import com.onsite.onsitefaulttracker.util.Compressor;
import com.onsite.onsitefaulttracker.util.RecordUtil;
import com.onsite.onsitefaulttracker.util.ThreadUtil;

import com.onsite.onsitefaulttracker.dropbox.DropboxClient;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import static android.support.v4.content.ContextCompat.getSystemService;

/**
 * Created by hihi on 6/25/2016.
 *
 * The Submit Fragment, The default fragment of submit Activity.
 * Here the user can submit their record and upload it to drop box
 */
public class SubmitFragment extends BaseFragment implements DropboxClient.DropboxClientListener, Compressor.CompressorListener {

    // The tag name for this fragment
    private static final String TAG = SubmitFragment.class.getSimpleName();

    // Id of the record to submit
    public static final String ARG_RECORD_ID = "record_id";

    // The format of output record names when converted from a date
    private static final String OUT_RECORD_DATE_FORMAT = "yy_MM_dd";

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

    // Record that is to be submitted
    private Record mRecord;

//    // Array of record files to be uploaded deprecated
//    private File[] mRecordFiles;

    // Array of all record files
    private File[] mRecordFiles;

    // Array of thumbnail record files to be uploaded
    private File[] mResizeFiles;

    // Array of thumbnail record files to be uploaded
    private File[] mOriginalFiles;

//    // Array of files to upload
//    private File[] mUploadFiles;

// Array of files to upload
    ArrayList<String> mUploadFiles;

    // Is this fragment currently resumed?
    boolean mResumed;

    // Is it currently zipping the record
    boolean mSubmitting;

    // Is it currently uploading the record
    boolean mUploading;

    // Index of the next image to display
    int mDisplayImageIndex;

    // The dropbox client which handles interaction with dropbox
    private DropboxClient mDropboxClient;

    private Compressor mCompressor;

    // Instance of dropbox remote client
    private DbxClientV2 mDbxClientV2;

    // The dropbox user account
    private FullAccount account;

    private Context mContext;

    //start end time for file upload;
    private long start, end;

    //cursor to keep track of files that have been compressed
    private int offset = 0;

   private int totalBytesOriginal = 0;

    private int totalBytesResize = 0;

    private long totalBytesUploaded = 0;




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
            mDisplayImageIndex = 0;
            mNameTextView = view.findViewById(R.id.record_name_text_view);
            mDateTextView = view.findViewById(R.id.record_creation_date_text_view);
            mTotalSizeTextView = view.findViewById(R.id.total_size_text_view);
            mRemainingTextView = view.findViewById(R.id.remaining_size_text_view);
            mSubmitButton = view.findViewById(R.id.submit_record_button);
            mSubmitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSubmitClicked();
                }
            });
            mPercentageTextView = view.findViewById(R.id.uploaded_percentage_text_view);
            mCurrentImageView = view.findViewById(R.id.current_image_id);
            mSubmittingProgressBar = view.findViewById(R.id.submitting_progress_bar);
            mRecordSubmittedTextView = view.findViewById(R.id.record_submitted_text_view);

            mRecord = RecordUtil.sharedInstance().getRecordWithId(mRecordId);

            updateUIValues();
        }
        return view;
    }

    /**
     * Action on attached
     */
    public void onAttach(Context context) {
        mContext = context;
        super.onAttach(context);
        mResumed = true;
    }
    /**
     * Action on resume
     */
    public void onResume() {
        Log.i(TAG, "Submit resumed");
        super.onResume();

        if (mDropboxClient == null) {
            mDropboxClient = new DropboxClient(getActivity());
        } else {
            mDropboxClient.updateActivity(getActivity());
        }
        mDropboxClient.setDropboxClientListener(this);
    }
    /**
     * Action on detach
     */
    public void onDetach() {
        super.onDetach();
        mResumed = false;
        mSubmitting = false;
        mUploading = false;
    }

    /**
     * Update the ui with the record details
     */
    private void updateUIValues() {
        if (mRecord.fileCompressedCount == 0) {
            mSubmitButton.setVisibility(View.VISIBLE);
            mSubmitButton.setText("ZIP RECORD");
        } else {
            mSubmitButton.setText("UPLOAD");
        }

        mNameTextView.setText(mRecord.recordName);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMMM yyyy, h:mm a");
        mDateTextView.setText(String.format(getString(R.string.submit_created_date), simpleDateFormat.format(mRecord.creationDate)));
        mTotalSizeTextView.setText(String.format(getString(R.string.submit_total_size), CalculationUtil.sharedInstance().getDisplayValueFromKB(mRecord.totalSizeKB)));
        //final long remainingKB = mRecord.totalSizeKB - mRecord.uploadedSizeKB;
        //final long remainingKB = mRecord.totalSizeKB - (totalBytesOriginal / 1024);
        //final long uploadedKB = mRecord.totalSizeKB - (totalBytesUploaded / 1024);
        //mRemainingTextView.setText(String.format(getString(R.string.submit_remaining_size), CalculationUtil.sharedInstance().getDisplayValueFromKB(Math.max(remainingKB, 0))));
        float percentage = ((float)mRecord.uploadedSizeKB / (float)mRecord.totalSizeKB) * 100.0f;
        percentage = Math.min(100.0f, percentage);
        mPercentageTextView.setText(String.format("%.0f%%", percentage));

        // If the submission is already complete show that
//        if (mRecord.fileCompressedCount >= mRecord.photoCount) {
//            onSubmissionComplete();
//        }
        if (mRecord.fileUploadCount >= mRecord.photoCount) {
            onUploadComplete();
        }
    }

    /**
     * Display the next image from the record files.
     */
    private void displayNextImage() {
        if (!mResumed) {
            // Don't attempt to display an image if the fragment is paused.
            return;
        }
        if (mCurrentImageView.getVisibility() != View.VISIBLE) {
            mCurrentImageView.setVisibility(View.VISIBLE);
        }
        mDisplayImageIndex++;
        if (mDisplayImageIndex >= mRecordFiles.length)
        {
            mDisplayImageIndex = 0;
            return;
        }

        final Bitmap uploadingBitmap = mRecordFiles[mDisplayImageIndex].getName().contains(".jpg") ?
                BitmapFactory.decodeFile(mRecordFiles[mDisplayImageIndex].getAbsolutePath()) : null;
        mCurrentImageView.setImageBitmap(uploadingBitmap);
    }

    private boolean checkWifiConnected() {
        WifiManager wifiMgr = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiMgr.isWifiEnabled()) { // Wi-Fi adapter is ON
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();

            if( wifiInfo.getNetworkId() == -1 ){
                return false; // Not connected to an access point
            }
            return true; // Connected to an access point
        }
        else {
            return false; // Wi-Fi adapter is OFF
        }
    }

    private void uploadNextFile() {
        if (!mResumed) {
            // Don't attempt to upload a file if the fragment is paused.
            return;
        }
        //TODO temp hack array needs to be changed to stored property
        if (mResizeFiles == null) { //handles null pointer if user does not upload after compress
            //should be changed to a stored property
            splitRecordFiles();
        }
        final String dateString = mRecord.recordFolderName;
        final String outPath = RecordUtil.sharedInstance().getBaseFolder().getAbsolutePath()
                + "/onsite_record_" + mRecord.recordFolderName + "_R.zip";
        start = System.currentTimeMillis();
        mDropboxClient.uploadChunkedFile(mRecord, mUploadFiles, new UploadCallback() {
            @Override
            public void onSuccess(Object object) {
                end = System.currentTimeMillis();
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
                //updateUIValues();
                mUploading = false;
                onUploadComplete();
            }
            @Override
            public void onFailure(String errorMessage) {
                mUploading = false;
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
            @Override
            public void onFolderExists() {

            }
        });
        ThreadUtil.executeOnNewThread(new Runnable() {
            @Override
            public void run() {
                while (mUploading) {
                    ThreadUtil.executeOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            final long remainingKB = mRecord.totalUploadSizeKB - ((totalBytesUploaded) / 1024);
                            mRemainingTextView.setText(String.format(getString(R.string.submit_remaining_size),
                                    CalculationUtil.sharedInstance().getDisplayValueFromKB(Math.max(remainingKB, 0))));
                            float percentage = ((float)totalBytesUploaded / (float)mRecord.totalUploadSizeKB) * 100.0f;
                            percentage = Math.min(100.0f, percentage);
                            mPercentageTextView.setText(String.format("%.0f%%", percentage));

                        }
                    });
                    try {
                        Thread.sleep(100);
                    } catch (Exception ex) {
                    }
                }
                ThreadUtil.executeOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        mCurrentImageView.setVisibility(View.INVISIBLE);
                    }
                });
            }
        });
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
        mRecordFiles = RecordUtil.sharedInstance().getRecordFiles(mRecord.recordId);
        mDropboxClient.confirmOrCreateFolder(mDbxClientV2, mRecord, new UploadCallback() {
            @Override
            public void onSuccess(Object object) {
                if (mRecordFiles != null && mRecordFiles.length > 0) {
                    uploadNextFile();
                }
            }
            @Override
            public void onFolderExists() {
                Log.e(TAG, "Folder already exists");
                uploadNextFile();
            }
            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Error creating folder on dropbox: " + errorMessage);
            }
        });

    }

    private void onUploadClicked() {

        if (checkWifiConnected()) {
            Log.i(TAG, "Wifi connected");
            mUploading = true;
            mRecordSubmittedTextView.setVisibility(View.INVISIBLE);
            mSubmittingProgressBar.setVisibility(View.VISIBLE);
            mPercentageTextView.setVisibility(View.VISIBLE);
            mTotalSizeTextView.setText(String.format(getString(R.string.submit_total_size),
                    CalculationUtil.sharedInstance().getDisplayValueFromKB(mRecord.totalUploadSizeKB)));
            mRemainingTextView.setText(String.format(getString(R.string.submit_remaining_size),
                    CalculationUtil.sharedInstance().getDisplayValueFromKB(Math.max(mRecord.totalUploadSizeKB, 0))));
            float percentage = ((float)totalBytesUploaded / (float)mRecord.totalUploadSizeKB) * 100.0f;
            percentage = Math.min(100.0f, percentage);
            mPercentageTextView.setText(String.format("%.0f%%", percentage));
            if (mDropboxClient == null) {
                mDropboxClient = new DropboxClient(getActivity());
            } else {
                mDropboxClient.updateActivity(getActivity());
            }
            mDropboxClient.setDropboxClientListener(this);
            try {
                mDropboxClient.authenticateDropBoxUser();
            } catch (DbxException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Wifi not connected");
            //onUploadComplete();
        }
    }

    private void onUploadComplete() {
        Log.i(TAG, "Upload time: " + ((end - start) / 1000) + "s");

        //mPercentageTextView.setText(String.format("%.0f%%", 100));
        mPercentageTextView.setVisibility(View.INVISIBLE);
        mRecordSubmittedTextView.setVisibility(View.VISIBLE);
        mRecordSubmittedTextView.setText("UPLOAD COMPLETE");
        mSubmittingProgressBar.setVisibility(View.INVISIBLE);
        mSubmitButton.setVisibility(View.INVISIBLE);
        mRecord.fileUploadCount = mRecord.photoCount;
        mRecord.uploadedSizeKB = totalBytesUploaded / 1024;
        mRecord.uploadTime = (end - start) / 1000;
        RecordUtil.sharedInstance().saveRecord(mRecord);
    }

    /**
     * Action when submitting a record has completed
     */
    private void onSubmissionComplete() {

        Log.i(TAG, "Original Bytes uploaded: " + String.valueOf(totalBytesOriginal));
        Log.i(TAG, "Resize Bytes uploaded: " + String.valueOf(totalBytesResize));

        mPercentageTextView.setVisibility(View.INVISIBLE);
        mRecordSubmittedTextView.setVisibility(View.VISIBLE);
        mSubmittingProgressBar.setVisibility(View.INVISIBLE);
        //mSubmitButton.setVisibility(View.INVISIBLE);
        mSubmitButton.setEnabled(true);
        mSubmitButton.setText("UPLOAD");
        mRecord.fileCompressedCount = mRecord.photoCount;
        RecordUtil.sharedInstance().saveRecord(mRecord);
    }

    private void splitRecordFiles() {
        mOriginalFiles = new File[(mRecordFiles.length / 2) + 1];
        mResizeFiles = new File[(mRecordFiles.length / 2) + 1];
        mOriginalFiles[0] = new File(mRecordFiles[0].getAbsolutePath());
        mResizeFiles[0] = new File(mRecordFiles[0].getAbsolutePath());
        //splits original array into two arrays
        for (int i = 1, j = 1; i < mRecordFiles.length; i += 2, j++) {
            mOriginalFiles[j] = new File(mRecordFiles[i].getAbsolutePath());
            mResizeFiles[j] = new File(mRecordFiles[i + 1].getAbsolutePath());
        }
        //mOriginalFiles[mRecordFiles.length] = new File(mRecordFiles[0].getAbsolutePath());

    }
    /**
     * Action when the user clicks on submit, initiate photo compression and zip files
     *
     */
    private void onSubmitClicked() {
        mSubmitting = true;
        if (mSubmitButton.getText() == "UPLOAD") {
            Log.i(TAG, "Upload clicked");
            onUploadClicked();
        } else {
            mRecordFiles = RecordUtil.sharedInstance().getRecordFiles(mRecord.recordId);
            splitRecordFiles();
            SimpleDateFormat dateFormat = new SimpleDateFormat(OUT_RECORD_DATE_FORMAT);
            final String dateString = dateFormat.format(mRecord.creationDate);
            final String outOriginalPath = RecordUtil.sharedInstance().getBaseFolder()
                    .getAbsolutePath() + "/onsite_record_" + mRecord.recordFolderName;
            final String outResizePath = RecordUtil.sharedInstance().getBaseFolder()
                    .getAbsolutePath() + "/onsite_record_" + mRecord.recordFolderName + "_R";

            mSubmittingProgressBar.setVisibility(View.VISIBLE);
            mSubmitButton.setEnabled(false);
            final Compressor cOriginal = compress(mOriginalFiles, outOriginalPath);
            cOriginal.setCompressorListener(this);
            final Compressor cResize = compress(mResizeFiles, outResizePath);
            cResize.setCompressorListener(this);
            ThreadUtil.executeOnNewThread(new Runnable( ) {
                @Override
                public void run() {
                    Log.i(TAG, "START ZIP");
                    cOriginal.zip(0);
                    cResize.zip(1);
                    //TODO TEMP Hack need to add .rec file
                    mUploadFiles = new ArrayList<String>();
                    mUploadFiles.add(outResizePath + ".zip");
                    Log.i(TAG, "END ZIP");
                    ThreadUtil.executeOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            mSubmitting = false;
                            mRecord.totalUploadSizeKB = totalBytesResize / 1024;
                            RecordUtil.sharedInstance().saveRecord(mRecord);
                            onSubmissionComplete();
                        }
                    });
                }
            });
            ThreadUtil.executeOnNewThread(new Runnable() {
                @Override
                public void run() {
                    while (mSubmitting) {
                        ThreadUtil.executeOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                displayNextImage();
                                final long remainingKB = mRecord.totalSizeKB - ((totalBytesOriginal
                                        + totalBytesResize) / 1024);
                                mRemainingTextView.setText(String.format(getString(R.string.submit_remaining_size),
                                        CalculationUtil.sharedInstance().getDisplayValueFromKB(Math.max(remainingKB, 0))));
                            }
                        });
                        try {
                            Thread.sleep(100);
                        } catch (Exception ex) {
                        }
                    }
                }
            });
        }
    }

    public Compressor compress(File[] files, String path) {
        Compressor c = new Compressor(files, path);
        return c;
    }

    public void listDbxFolders(DbxClientV2 client) {
        final DbxClientV2 mDbxClientV2 = client;
        ListFolderResult folders = null;
        ThreadUtil.executeOnNewThread(new Runnable() {
            @Override
            public void run() {
                try {
                    ListFolderResult result = mDbxClientV2.files().listFolder("");
                    while (true) {
                        for (Metadata metadata : result.getEntries()) {
                            System.out.println(metadata.getPathLower());
                        }
                        if (!result.getHasMore()) {
                            break;
                        }
                        result = mDbxClientV2.files().listFolderContinue(result.getCursor());
                    }
                } catch (DbxException e) {
                    e.printStackTrace();
                }
            }
        });
    }

//--------------------------- OVERRIDE METHODS ----------------------------------//
    /**
     *
     * @param buffer
     */

    @Override
    public void dataRead(int buffer, int caller) {
        //Log.i(TAG, "Buffer: " + buffer);
        if (caller == 0) {
            totalBytesOriginal += buffer;
        } else {
            totalBytesResize += buffer;
        }
    }

    @Override
    public void uploadProgress(long buffer) {
        Log.i("Bytes upload before: ", String.valueOf(totalBytesUploaded));
        totalBytesUploaded = buffer;
        Log.i("Bytes upload after: ", String.valueOf(totalBytesUploaded));
    }

    /**
     * Called when dropbox has been authenticated,
     * start uploading the record
     */
    @Override
    public void onDropboxAuthenticated(FullAccount result, DbxClientV2 client) {
        Log.i(TAG, "Drop box authenticated");
        FullAccount account = result;
        mDbxClientV2 = client;
        startUploadingRecord();
    }

    /**
     * Called when dropbox fails / has an error initializing
     */
    @Override
    public void onDropboxFailed() {

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
     * instantiate and return an instance of this fragment
     *
     * @return
     */
    public static SubmitFragment createInstance(final String recordId) {
        final SubmitFragment submitFragment = new SubmitFragment();

        Bundle args = new Bundle();
        args.putString(ARG_RECORD_ID, recordId);
        submitFragment.setArguments(args);

        return submitFragment;
    }

    /**
     * Returns the display title for this fragment
     *
     * @return
     */
    @Override
    protected String getDisplayTitle() {
        return getString(R.string.zip_record);
    }

    /**
     * Returns the layout resource for this fragment
     *
     * @return
     */
    @Override
    protected int getLayoutResourceId() {
        return R.layout.fragment_submit;
    }

}
