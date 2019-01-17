package com.onsite.onsitefaulttracker.dropbox;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.os.AsyncTask;
import android.widget.Toast;

import com.dropbox.client2.session.TokenPair;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.http.HttpRequestor;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;

import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.UploadBuilder;
import com.dropbox.core.v2.files.UploadSessionStartUploader;
import com.dropbox.core.v2.files.UploadUploader;
import com.dropbox.core.v2.files.WriteMode;
import com.dropbox.core.v2.users.FullAccount;
import com.dropbox.core.android.Auth;
import com.onsite.onsitefaulttracker.OnSiteConstants;
import com.onsite.onsitefaulttracker.dropboxapi.DropboxCallback;
import com.onsite.onsitefaulttracker.model.Record;
import com.onsite.onsitefaulttracker.util.ThreadUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class DropboxClient {

    // The tag name for this class
    private static final String TAG = DropboxClient.class.getSimpleName();

    // The parent activity which this dropbox client has been initialized with
    private Activity mParentActivity;

    // The dropbox client listener, communicates events with the parent activity
    private DropboxClient.DropboxClientListener mDropboxClientListener;

    // The access token for the dropbox session once initialized
    private String mDropBoxAccessToken;

    // In the class declaration section:
    private DropboxAPI<AndroidAuthSession> mDBApi;

    private static final String ACCESS_TOKEN = "Ctnea--u6xAAAAAAAAAAHSNKi9T3phbEOvs6p-A0JH6aT_eKPAQRGCeeK1BhrHk7";

    private DbxClientV2 mDbxClient;

    private boolean mConnected = false; // connected to dropbox

    // And later in some initialization function:
    //AppKeyPair mAppKeys = new AppKeyPair(OnSiteConstants.DROPBOX_APP_KEY, OnSiteConstants.DROPBOX_SECRET_KEY);
    //AndroidAuthSession mSession;

    /**
     * Interface for communicating with the parent fragment/activity
     */
    public interface DropboxClientListener {
        void onDropboxAuthenticated(FullAccount result, DbxClientV2 client);
        void onDropboxFailed();
    }


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
    public void authenticateDropBoxUser() throws DbxException {

        //Auth.startOAuth2Authentication(mParentActivity, OnSiteConstants.DROPBOX_APP_KEY);
        //String accessToken = Auth.getOAuth2Token();
        String accessToken = "Ctnea--u6xAAAAAAAAAAHSNKi9T3phbEOvs6p-A0JH6aT_eKPAQRGCeeK1BhrHk7";
        if (accessToken != null) {
            new DropBoxCallback(this, accessToken, new DropBoxCallback.Callback() {
                @Override
                public void onLoginComplete(FullAccount result, DbxClientV2 client) {
                    mDbxClient = client;
                    mConnected = true;
                    Log.e(TAG, "Success.", null);
                    mDropboxClientListener.onDropboxAuthenticated(result, client);
                }
                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Failed to login.", e);
                }
            }).execute();
        }
    }

    public void confirmOrCreateFolder(final DbxClientV2 client, final Record record, final UploadCallback callback) {
        ThreadUtil.executeOnNewThread(new Runnable() {
            @Override
            public void run() {
                try {
                    client.files().createFolderV2("/" + record.recordFolderName);
                    ThreadUtil.executeOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccess(null);
                        }
                    });
                } catch (DbxException e) { //folder already exists
                    //callback.onFailure(e.getMessage());
                    ThreadUtil.executeOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFolderExists();
                        }
                    });
                }
            }
        });
    }

    public void uploadBatchFile(final Record record, final ArrayList<File> recordFiles, final UploadCallback Callback) {
        final File nextFileToUpload = recordFiles.get(0);
        if (nextFileToUpload == null) {
            // TODO: react accordingly
            ThreadUtil.executeOnMainThread(new Runnable() {
                @Override
                public void run() {
                    Callback.onFailure("No files to upload");
                }
            });
            return;
        }
        ThreadUtil.executeOnNewThread(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    /**
     * Upload the next file for the record to dropbox
     *
     * @param record
     */
    public void uploadNextFile(final Record record, final ArrayList<File> recordFiles, final UploadCallback Callback) {
        //final int nextFileIndex = record.fileUploadCount;
        //final File nextFileToUpload = nextFileIndex < recordFiles.length ? recordFiles[nextFileIndex] : null;
        final File nextFileToUpload = recordFiles.get(0);
        if (nextFileToUpload == null) {
            // TODO: react accordingly
            ThreadUtil.executeOnMainThread(new Runnable() {
                @Override
                public void run() {
                    Callback.onFailure("No files to upload");
                }
            });
            return;
        }

        ThreadUtil.executeOnNewThread(new Runnable() {
            @Override
            public void run() {
                try {
                    FileInputStream finStream = new FileInputStream(nextFileToUpload);
                    Log.i(TAG, "Bytes to upload: " +
                            String.valueOf(nextFileToUpload.length()/1024));
                    FileMetadata response = mDbxClient.files().uploadBuilder("/" +
                            record.recordFolderName + "/" + nextFileToUpload.getName())
                            .withMode(WriteMode.OVERWRITE)
                            .uploadAndFinish(finStream);

                    UploadSessionStartUploader uploader = mDbxClient.files().uploadSessionStart();
                    //String sessionId = uploader.;

                    //mDbxClient.files().
                    //UploadSessionStartUploader session = mDbxClient.files().uploadSessionStart();
                    //session.getBody().write(first_chunk_of_upload)

                    if (response != null && !TextUtils.isEmpty(response.getRev())) {
                        Log.i(TAG, "File Uploaded");
                        record.fileUploadCount++;
                        record.uploadedSizeKB += (nextFileToUpload.length()/1024);
                        ThreadUtil.executeOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                Callback.onSuccess(null);
                            }
                        });
                    }
                } catch (IOException ioEx) {
                    Log.e(TAG, "Error creating input stream: " + ioEx.getLocalizedMessage());
                    ThreadUtil.executeOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            Callback.onFailure("Failed to open file stream");
                        }
                    });
                } catch (DbxException dpEx) {
                    // If file already exists exception
                    if (dpEx.toString().contains("Conflict")) {
                        // File already exists in dropbox,  update record then goto the next
                        Log.i(TAG, "File already exists");
                        record.fileUploadCount++;
                        record.uploadedSizeKB += (nextFileToUpload.length()/1024);
                        ThreadUtil.executeOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                Callback.onSuccess(null);
                            }
                        });
                        return;
                    }

                    // Some other error uploading file
                    Log.e(TAG, "Dropbox exception trying to upload file: " + dpEx.getLocalizedMessage());
                    ThreadUtil.executeOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            Callback.onFailure("Failed to upload file");
                        }
                    });
                }
            }
        });
    }
}
