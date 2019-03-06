package com.onsite.onsitefaulttracker.activity.submit;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.Toast;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
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

    // Thumbnails have been uploaded to dropbox
    boolean mUploaded;

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

    // size of full size photos
    private long totalBytesOriginal = 0;

    // size of the thumbnails
    private long totalBytesResize = 0;

    private long totalBytesCompressed = 0;

    // resized photos uploaded
    private long totalBytesUploaded = 0;

    // path to zip file to upload
    private String outPath;

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
            //mRecord = RecordUtil.sharedInstance().getRecordWithId(mRecordId);
            mRecord = RecordUtil.sharedInstance().getCurrentRecord();
            mRecordFiles = RecordUtil.sharedInstance().getRecordFiles(mRecord.recordId);
            splitRecordFiles();
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



        mRecord.recordSizeKB = totalBytesOriginal / 1024;
        mRecord.totalSizeKB = (totalBytesResize + totalBytesOriginal) /1024;
        RecordUtil.sharedInstance().saveRecord(mRecord);
        RecordUtil.sharedInstance().saveCurrentRecord();

        //mRecord.recordSizeKB = mOriginalFiles.length;

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
        final Bitmap uploadingBitmap = mOriginalFiles[mDisplayImageIndex].getName().contains(".jpg") ?
                BitmapFactory.decodeFile(mOriginalFiles[mDisplayImageIndex].getAbsolutePath()) : null;
        mCurrentImageView.setImageBitmap(uploadingBitmap);
    }

    /**
     * Checks if phone is connected to wifi before attempting upload
     * @return - boolean true if connected false if not connected
     */
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
            new AlertDialog.Builder(getActivity())
                    .setTitle("Wifi Not Connected")
                    .setMessage("Phone must connected to wifi before uploading. " +
                            "Check internet connection or enable wifi in phone settings")
                    .setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getActivity().finish();
                            onBackClicked();
                        }
                    })
                    .show();
        }
    }
    /**
     * Start uploading the record to dropbox
     */
    private void startUploadingRecord() {
        mSubmittingProgressBar.setVisibility(View.VISIBLE);
        mPercentageTextView.setVisibility(View.VISIBLE);
        mSubmitButton.setEnabled(false);
        mSubmitButton.setText("Uploading");
        mUploading = true;


        mDropboxClient.confirmOrCreateFolder(mDbxClientV2, mRecord, new UploadCallback() {
            @Override
            public void onSuccess(Object object) {
                if (mRecordFiles != null && mRecordFiles.length > 0) {
                    uploadPhotos();
                }
            }
            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Error creating folder on dropbox: " + error);

                new AlertDialog.Builder(getActivity())
                        .setTitle("Folder already exists")
                        .setMessage(error)
                        .setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                uploadPhotos();
                            }
                        })
                        .setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mUploading = false;
                                mUploaded = false;
                                mSubmitting = false;
                                getActivity().finish();
                                onBackClicked();
                            }
                        })
                        .show();
            }
        });
    }
    /**
     *
     */
    private void uploadPhotos() {
        if (!mResumed) {
            // Don't attempt to upload a file if the fragment is paused.
            return;
        }
        //handles null pointer if user navigates away from submit page. Will re-split large photo
        //array. Filenames could potentially be stored in shared preferences to be more efficient
        outPath = RecordUtil.sharedInstance().getBaseFolder().getAbsolutePath()
                + "/onsite_record_" + mRecord.recordFolderName + "_R.zip";

        mUploadFiles = new ArrayList<String>(); //holds filepaths of resized photos.
        mUploadFiles.add(outPath);
        start = System.currentTimeMillis();
        mDropboxClient.uploadChunkedFile(mRecord, mUploadFiles, new UploadCallback() {
            @Override
            public void onSuccess(Object object) {
                // If the fragment is not active just return
                if (!mResumed) {
                    return;
                }
                mUploading = true;
                mUploaded = false;
                end = System.currentTimeMillis();
                uploadMetaData();
            }
            @Override
            public void onFailure(String errorMessage) {
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
     *
     */
    private void uploadMetaData() {
        mRecord.fileUploadCount = mResizeFiles.length;
        mRecord.uploadedSizeKB = totalBytesUploaded / 1024;
        mRecord.uploadTime = (end - start) / 1000;
        //RecordUtil.sharedInstance().saveRecord(mRecord);
        RecordUtil.sharedInstance().saveCurrentRecord();

        mDropboxClient.uploadNextFile(mRecord, mRecordFiles[0], new UploadCallback() {
            @Override
            public void onSuccess(Object object) {
                mRecord.fileUploadCount++;
                //RecordUtil.sharedInstance().saveRecord(mRecord);
                RecordUtil.sharedInstance().saveCurrentRecord();
                onUploadComplete();
            }
            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Metadata upload failed");
                //RecordUtil.sharedInstance().saveRecord(mRecord);
                RecordUtil.sharedInstance().saveCurrentRecord();
                mUploaded = false;
                mUploading = false;
            }
        });
    }
    /**
     *
     */
    private void onUploadComplete() {
        mUploaded = true;
        mUploading = false;
        mSubmitting = false;
        mPercentageTextView.setVisibility(View.INVISIBLE);
        mRecordSubmittedTextView.setVisibility(View.VISIBLE);
        mRecordSubmittedTextView.setText("UPLOAD COMPLETE");
        mSubmittingProgressBar.setVisibility(View.INVISIBLE);
        mSubmitButton.setVisibility(View.INVISIBLE);

    }
    /**
     * Action when submitting a record has completed
     */
    private void onSubmissionComplete() {

        mSubmitting = false;
        mCurrentImageView.setVisibility(View.INVISIBLE);
        mPercentageTextView.setVisibility(View.INVISIBLE);
        mRecordSubmittedTextView.setVisibility(View.VISIBLE);
        mSubmittingProgressBar.setVisibility(View.INVISIBLE);
        //mSubmitButton.setVisibility(View.INVISIBLE);
        mSubmitButton.setEnabled(true);
        mSubmitButton.setText("UPLOAD");
        mRecord.fileCompressedCount = mOriginalFiles.length;
        mRecord.totalUploadSizeKB = mRecord.totalSizeKB - mRecord.recordSizeKB;
        RecordUtil.sharedInstance().saveRecord(mRecord);
        RecordUtil.sharedInstance().updateRecordCount();
        //RecordUtil.sharedInstance().saveCurrentRecord();
    }
    /**
     * Action when the user clicks on submit, initiate photo compression and zip files
     *
     */
    private void onSubmitClicked() {

        if (mSubmitButton.getText() == "UPLOAD") {
            Log.i(TAG, "Upload clicked");
            onUploadClicked();
        } else {
            mSubmitting = true;
            //splitRecordFiles();
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
                    Log.i(TAG, "END ZIP");
                    ThreadUtil.executeOnMainThread(new Runnable() {
                        @Override
                        public void run() {
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
                                final long remainingKB = (mRecord.totalSizeKB - mRecordFiles[0].length())
                                        - ((totalBytesCompressed + totalBytesResize) / 1024);
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

    /**
     * Splits orginal array of photos into two arrays. One containing full size photos
     * (mOriginalFiles) and the other (mResizeFiles) containing thumbnails. Ignores .rec file
     */
    private void splitRecordFiles() {
        try {
            mOriginalFiles = new File[(mRecordFiles.length / 2)];
            mResizeFiles = new File[(mRecordFiles.length / 2)];
            for (int i = 1, j = 0; i < mRecordFiles.length; i += 2, j++) {
                mOriginalFiles[j] = new File(mRecordFiles[i].getAbsolutePath());
                totalBytesOriginal += mOriginalFiles[j].length();
                mResizeFiles[j] = new File(mRecordFiles[i + 1].getAbsolutePath());
                totalBytesResize += mResizeFiles[j].length();
            }
        } catch (Exception e){
            Log.e(TAG, "Corrupted record file");
            new AlertDialog.Builder(getActivity())
                    .setTitle("Error")
                    .setMessage("Corrupted record file: splitRecordFiles failed")
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
    }
//        RecordUtil.sharedInstance().saveRecord(mRecord);

        //mOriginalFiles[mRecordFiles.length] = new File(mRecordFiles[0].getAbsolutePath());

    /**
     * Intialises a new Compressor object at set files to compress and path to save zip file to
     *
     * @param files - array of files to compress
     * @param path - file path to save zipped file to
     * @return - new Compressor object
     */
    public Compressor compress(File[] files, String path) {
        Compressor c = new Compressor(files, path);
        return c;
    }

//--------------------------- OVERRIDE METHODS ----------------------------------//
    /**
     *
     * @param buffer
     */

    @Override
    public void dataRead(long buffer, int caller) {
        //Log.i(TAG, "Buffer: " + buffer);
        if (caller == 0) {
            totalBytesCompressed += buffer;
            //Log.i("Bytes: ", String.valueOf(totalBytesOriginal));
        } else {
            totalBytesResize += buffer;
            //Log.i("Bytes resize: ", String.valueOf(totalBytesResize));
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
        Toast.makeText(getActivity(), "Succesfully connected to dropbox!",
                Toast.LENGTH_LONG).show();
        FullAccount account = result;
        mDbxClientV2 = client;
        startUploadingRecord();
    }

    /**
     * Called when dropbox fails / has an error initializing
     */
    @Override
    public void onDropboxFailed(Exception e) {
        Log.i(TAG, "Drop box failed to authenticate");
        mSubmitting = false;
        mUploading = false;
        mUploaded = false;
//        Toast.makeText(getActivity(), "ERROR: Failed to connect to dropbox!",
//                Toast.LENGTH_LONG).show();
        new AlertDialog.Builder(getActivity())
                .setTitle("Dropbox Login Failed")
                .setMessage(e.getMessage())
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        getActivity().onBackPressed();
                    }
                })
                .setPositiveButton(getString(android.R.string.ok), null)
                .show();
    }

    /**
     * Override and handle on back action return true if consumed the event
     * (if the parent activity should not close as is its normal coarse of action) otherwise
     * false
     *
     * @return true if activity should handle the back action, otherwise false
     */
    public boolean onBackClicked() {
        if (mSubmitting || mUploading) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.submitting_back_dialog_title))
                    .setMessage(getString(R.string.submitting_back_dialog_message))
                    .setPositiveButton(getString(R.string.submitting_back_dialog_exit), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mSubmitting = false;
                            mUploading = false;
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
