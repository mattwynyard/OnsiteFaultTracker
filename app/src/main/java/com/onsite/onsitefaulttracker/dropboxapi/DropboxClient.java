package com.onsite.onsitefaulttracker.dropboxapi;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;
import com.onsite.onsitefaulttracker.OnSiteConstants;
import com.onsite.onsitefaulttracker.model.Record;
import com.onsite.onsitefaulttracker.util.ThreadUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by hihi on 6/27/2016.
 *
 * Dropbox clients,  handles all interaction with drop box and submission of
 * records to dropbox.
 */
public class DropboxClient {

    // The tag name for this class
    private static final String TAG = DropboxClient.class.getSimpleName();

    // The parent activity which this dropbox client has been initialized with
    private Activity mParentActivity;

    // The dropbox client listener, communicates events with the parent activity
    private DropboxClientListener mDropboxClientListener;

    // The access token for the dropbox session once initialized
    private String mDropBoxAccessToken;

    // In the class declaration section:
    private DropboxAPI<AndroidAuthSession> mDBApi;

    // And later in some initialization function:
    AppKeyPair mAppKeys = new AppKeyPair(OnSiteConstants.DROPBOX_APP_KEY, OnSiteConstants.DROPBOX_SECRET_KEY);
    AndroidAuthSession mSession;


    /**
     * Constructor, takes in the parent activity
     *
     * @param parentActivity
     */
    public DropboxClient(final Activity parentActivity) {
        mParentActivity = parentActivity;
    }

    /**
     * Updates the current parent activity
     *
     * @param parentActivity
     */
    public void updateActivity(final Activity parentActivity) {
        mParentActivity = parentActivity;
    }

    /**
     * Sets the dropbox client listener
     *
     * @param dropboxClientListener
     */
    public void setDropboxClientListener(final DropboxClientListener dropboxClientListener) {
        mDropboxClientListener = dropboxClientListener;
    }

    /**
     * Call to present the dropbox authentication dialog to the user
     */
    public void authenticateDropBoxUser() {
        if (mDBApi == null) {
            mSession = new AndroidAuthSession(mAppKeys);
            mDBApi =  new DropboxAPI<AndroidAuthSession>(mSession);
            mDBApi.getSession().startOAuth2Authentication(mParentActivity);
        }
    }

    /**
     * To be called when resuming from dropbox authentication
     */
    public void onDropboxActivityResumed() {
        if (mDBApi != null && mDBApi.getSession().authenticationSuccessful()) {
            try {
                // Required to complete auth, sets the access token on the session
                mDBApi.getSession().finishAuthentication();

                mDropBoxAccessToken = mDBApi.getSession().getOAuth2AccessToken();
                initializeOnsiteDropbox(new DropboxCallback() {
                    @Override
                    public void onSuccess(Object object) {
                        if (mDropboxClientListener != null) {
                            mDropboxClientListener.onDropboxAuthenticated();
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        if (mDropboxClientListener != null) {
                            mDropboxClientListener.onDropboxFailed();
                        }
                    }
                });
            } catch (IllegalStateException e) {
                Log.i("DbAuthLog", "Error authenticating", e);
            }
        }
    }

    /**
     * Verifies that the OnSite dropbox directory exists, if not creates it
     */
    private void initializeOnsiteDropbox(final DropboxCallback dropboxCallback) {
        ThreadUtil.executeOnNewThread(new Runnable() {
            @Override
            public void run() {
                try {
                    DropboxAPI.Entry existingEntry = mDBApi.metadata("/OnSiteApp", 10000, null, false, null);
                    Log.i(TAG, "initializeOnsiteDropbox,  called metadata");
                    ThreadUtil.executeOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            dropboxCallback.onSuccess(null);
                        }
                    });
                } catch (DropboxException dbEx) {
                    Log.i(TAG, "initializeOnsiteDropbox,  " + dbEx.getLocalizedMessage());

                    // Folder not found,  create the folder
                    createOnSiteAppFolder(dropboxCallback);
                }
            }
        });
    }

    /**
     * create the onSite application folder for uploading projects to
     *
     * @param dropboxCallback
     */
    private void createOnSiteAppFolder(final DropboxCallback dropboxCallback) {
        try {
            mDBApi.createFolder("/OnSiteApp");
            ThreadUtil.executeOnMainThread(new Runnable() {
                @Override
                public void run() {
                    dropboxCallback.onSuccess(null);
                }
            });
        } catch (final DropboxException dbEx) {
            Log.e(TAG, "error creating dropbox OnSite folder, " + dbEx.getLocalizedMessage());
            ThreadUtil.executeOnMainThread(new Runnable() {
                @Override
                public void run() {
                    dropboxCallback.onFailure(dbEx.getLocalizedMessage());
                }
            });
        }
    }

    /**
     * check if a record folder exists and if not create it
     *
     * @param dropboxCallback
     */
    public void confirmOrCreateRecord(final Record record, final DropboxCallback dropboxCallback) {
        ThreadUtil.executeOnNewThread(new Runnable() {
            @Override
            public void run() {
                try {
                    DropboxAPI.Entry existingEntry = mDBApi.metadata("/OnSiteApp/" + record.recordFolderName, 10000, null, false, null);
                    ThreadUtil.executeOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            dropboxCallback.onSuccess(null);
                        }
                    });
                } catch (DropboxException dbEx) {
                    createRecordFolder(record, dropboxCallback);
                }
            }
        });
    }

    /**
     * Create the folder on dropbox where the record will be stored
     *
     * @param record
     * @param dropboxCallback
     */
    private void createRecordFolder(final Record record, final DropboxCallback dropboxCallback) {
        try {
            mDBApi.createFolder("/OnSiteApp/" + record.recordFolderName);
            ThreadUtil.executeOnMainThread(new Runnable() {
                @Override
                public void run() {
                    dropboxCallback.onSuccess(null);
                }
            });
        } catch (final DropboxException dbEx) {
            Log.e(TAG, "error creating dropbox OnSite folder, " + dbEx.getLocalizedMessage());
            ThreadUtil.executeOnMainThread(new Runnable() {
                @Override
                public void run() {
                    dropboxCallback.onFailure(dbEx.getLocalizedMessage());
                }
            });
        }
    }

    /**
     * Upload the next file for the record to dropbox
     *
     * @param record
     */
    public void uploadNextFile(final Record record, final File[] recordFiles, final DropboxCallback dropboxCallback) {
        final int nextFileIndex = record.fileUploadCount;
        final File nextFileToUpload = nextFileIndex < recordFiles.length ? recordFiles[nextFileIndex] : null;
        if (nextFileToUpload == null) {
            // TODO: react accordingly
            ThreadUtil.executeOnMainThread(new Runnable() {
                @Override
                public void run() {
                    dropboxCallback.onFailure("No more files to upload");
                }
            });
            return;
        }

        ThreadUtil.executeOnNewThread(new Runnable() {
            @Override
            public void run() {
                try {
                    FileInputStream finStream = new FileInputStream(nextFileToUpload);

                    DropboxAPI.Entry response = mDBApi.putFile("/OnSiteApp/" + record.recordFolderName + "/" + nextFileToUpload.getName(),
                            finStream, nextFileToUpload.length(), null, false, null);


                    if (response != null && !TextUtils.isEmpty(response.rev)) {
                        Log.i(TAG, "File Uploaded");
                        record.fileUploadCount++;
                        record.uploadedSizeKB += (nextFileToUpload.length()/1024);
                        ThreadUtil.executeOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                dropboxCallback.onSuccess(null);
                            }
                        });
                    }
                } catch (IOException ioEx) {
                    Log.e(TAG, "Error creating input stream: " + ioEx.getLocalizedMessage());
                    ThreadUtil.executeOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            dropboxCallback.onFailure("Failed to open file stream");
                        }
                    });
                } catch (DropboxException dpEx) {
                    // If file already exists exception
                    if (dpEx.toString().contains("Conflict")) {
                        // File already exists in dropbox,  update record then goto the next
                        record.fileUploadCount++;
                        record.uploadedSizeKB += (nextFileToUpload.length()/1024);
                        ThreadUtil.executeOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                dropboxCallback.onSuccess(null);
                            }
                        });
                        return;
                    }

                    // Some other error uploading file
                    Log.e(TAG, "Dropbox exception trying to upload file: " + dpEx.getLocalizedMessage());
                    ThreadUtil.executeOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            dropboxCallback.onFailure("Failed to upload file");
                        }
                    });
                }
            }
        });
    }


    /**
     * Interface for communicating with the parent fragment/activity
     */
    public interface DropboxClientListener {
        void onDropboxAuthenticated();
        void onDropboxFailed();
    }

}
