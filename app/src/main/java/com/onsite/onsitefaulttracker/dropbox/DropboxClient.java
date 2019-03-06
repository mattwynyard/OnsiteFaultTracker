package com.onsite.onsitefaulttracker.dropbox;

import android.app.Activity;

import android.text.TextUtils;
import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.NetworkIOException;
import com.dropbox.core.RetryException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;

import com.dropbox.core.v2.files.CommitInfo;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.UploadSessionCursor;
import com.dropbox.core.v2.files.UploadSessionFinishArg;
import com.dropbox.core.v2.files.UploadSessionFinishBatchLaunch;
import com.dropbox.core.v2.files.WriteMode;
import com.dropbox.core.v2.users.FullAccount;
import com.onsite.onsitefaulttracker.model.Record;
import com.onsite.onsitefaulttracker.util.ThreadUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    private static final long CHUNKED_UPLOAD_CHUNK_SIZE =  100 * (1024 * 1024);// 1L << 20; // 10MiB  = 10 * 2^20
    private static final int CHUNKED_UPLOAD_MAX_ATTEMPTS = 5;

    // And later in some initialization function:
    //AppKeyPair mAppKeys = new AppKeyPair(OnSiteConstants.DROPBOX_APP_KEY, OnSiteConstants.DROPBOX_SECRET_KEY);
    //AndroidAuthSession mSession;

    /**
     * Interface for communicating with the parent fragment/activity
     */
    public interface DropboxClientListener {
        void onDropboxAuthenticated(FullAccount result, DbxClientV2 client);
        void onDropboxFailed(Exception e);
        void uploadProgress(long buffer);
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

        String accessToken = "Ctnea--u6xAAAAAAAAAAHSNKi9T3phbEOvs6p-A0JH6aT_eKPAQRGCeeK1BhrHk7";
        if (accessToken != null) {
            new DropBoxCallback(this, accessToken, new DropBoxCallback.Callback() {
                @Override
                public void onLoginComplete(FullAccount result, DbxClientV2 client) {
                    mDbxClient = client;
                    mConnected = true;
                    mDropboxClientListener.onDropboxAuthenticated(result, client);
                }
                @Override
                public void onError(Exception e) {
                    mDropboxClientListener.onDropboxFailed(e);
                }
            }).execute();
        }
    }

    public void confirmOrCreateFolder(final DbxClientV2 client, final Record record, final UploadCallback callback) {
        ThreadUtil.executeOnNewThread(new Runnable() {
            @Override
            public void run() {
                try {

                    client.files().createFolderV2("/" + record.recordName);
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
                            callback.onFailure("Files have " +
                                    "already uploaded. Click OK to re-upload photos or CANCEL to " +
                                    "abort upload");
                        }
                    });
                }
            }
        });
    }

    private void printProgress(long uploaded, long size) {
        System.out.printf("Uploaded %12d / %12d bytes (%5.2f%%)\n", uploaded, size, 100 * (uploaded / (double) size));
    }

    public void uploadChunkedFile(final Record record, final ArrayList<String> _files, final UploadCallback Callback) {
        final File f = new File(_files.get(0));
        final long _size = f.length();
        System.err.println("File size: " + _size);

        if (_size < CHUNKED_UPLOAD_CHUNK_SIZE) {
            System.err.println("File too small, use upload() instead.");
            uploadNextFile(record, f, Callback);
            return;
        }
        ThreadUtil.executeOnNewThread(new Runnable() {
            @Override
            public void run() {
                long uploaded = 0L;
                String sessionId = null;
                long offset = 0L;
                boolean close = false;
                DbxException thrown = null;
                UploadSessionCursor cursor = null;
                mDropboxClientListener.uploadProgress(offset);
                for (int i = 0; i < CHUNKED_UPLOAD_MAX_ATTEMPTS; ++i) {
                    if (i > 0) {
                        System.out.printf("Retrying chunked upload (%d / %d attempts)\n",
                                i + 1, CHUNKED_UPLOAD_MAX_ATTEMPTS);
                    }

                    try {
                        InputStream in = new FileInputStream(f);

                        if (sessionId == null) {
                            sessionId = mDbxClient.files().uploadSessionStart(close)
                                    .uploadAndFinish(in, CHUNKED_UPLOAD_CHUNK_SIZE).getSessionId();
                            offset += CHUNKED_UPLOAD_CHUNK_SIZE;
                            mDropboxClientListener.uploadProgress(offset);
                        }
                        cursor = new UploadSessionCursor(sessionId, offset);

                        while ((_size - offset) > CHUNKED_UPLOAD_CHUNK_SIZE) {

                            mDbxClient.files().uploadSessionAppendV2(cursor)
                                    .uploadAndFinish(in, CHUNKED_UPLOAD_CHUNK_SIZE);
                            offset += CHUNKED_UPLOAD_CHUNK_SIZE;
                            cursor = new UploadSessionCursor(sessionId, offset);
                            mDropboxClientListener.uploadProgress(offset);

                        }
                        long remaining = _size - offset;
                        offset += remaining;
                        mDropboxClientListener.uploadProgress(offset);
                        CommitInfo commitInfo = CommitInfo.newBuilder("/" +
                                record.recordName + "/" + f.getName())
                                .withMode(WriteMode.OVERWRITE)
                                .withClientModified(new Date(f.lastModified()))
                                .build();
                        FileMetadata metadata = mDbxClient.files().uploadSessionFinish(cursor, commitInfo)
                                .uploadAndFinish(in, remaining);
                        mDropboxClientListener.uploadProgress(offset);
                        System.out.println(metadata.toStringMultiline());
                        ThreadUtil.executeOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                Callback.onSuccess(null);
                            }
                        });
                    } catch (RetryException ex) {
                        thrown = ex;
                        // RetryExceptions are never automatically retried by the client for uploads. Must
                        // catch this exception even if DbxRequestConfig.getMaxRetries() > 0.
                        try {
                            Thread.sleep(ex.getBackoffMillis());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    } catch (NetworkIOException ex) {
                        thrown = ex;
                        // network issue with Dropbox (maybe a timeout?) try again
                        continue;
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (DbxException e) {
                        Log.e(TAG, "DBx exception: " + e.getLocalizedMessage());
                        ThreadUtil.executeOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                Callback.onFailure("Failed to commit batch files");
                            }
                        });
                    } catch (IOException ioEx) {
                        Log.e(TAG, "Error creating input stream: " + ioEx.getLocalizedMessage());
                        ThreadUtil.executeOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                Callback.onFailure("Failed to open file stream");
                            }
                        });
                    }
                }
                System.err.println("Maxed out upload attempts to Dropbox. Most recent error: "
                        + thrown.getMessage());
                ThreadUtil.executeOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        Callback.onFailure("Maxed out upload attempts to Dropbox.");
                    }
                });
            }
        });
    }
    /**
     * Upload the next file for the record to dropbox
     *
     * @param record
     */
    public void uploadNextFile(final Record record, final File _file, final UploadCallback Callback) {
        //final int nextFileIndex = record.fileUploadCount;
        //final File nextFileToUpload = nextFileIndex < recordFiles.length ? recordFiles[nextFileIndex] : null;
        final File nextFileToUpload = _file;
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
                            record.recordName + "/" + nextFileToUpload.getName())
                            .withMode(WriteMode.OVERWRITE)
                            .uploadAndFinish(finStream);

                    if (response != null && !TextUtils.isEmpty(response.getRev())) {
                        Log.i(TAG, "File Uploaded");
                        mDropboxClientListener.uploadProgress(nextFileToUpload.length());
                        //record.fileUploadCount++;
                        //record.uploadedSizeKB += (nextFileToUpload.length()/1024);
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
                        //record.fileUploadCount++;
                        //record.uploadedSizeKB += (nextFileToUpload.length()/1024);
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


    /********* DEPRECIATED - uploads files individually but too slow ***********/
    public void uploadBatchFile(final Record record, final ArrayList<String> recordFiles,
                                final UploadCallback Callback) {

        ThreadUtil.executeOnNewThread(new Runnable() {
            @Override
            public void run() {

                List<UploadSessionFinishArg> entries = new ArrayList<>();
                String sessionId = null;
                long offset = 0;
                boolean close = false;
                UploadSessionCursor cursor = null;
                for (int i = 0; i < recordFiles.size(); i++) {
                    try {
                        File f = new File(recordFiles.get(i));
                        InputStream in = new FileInputStream(f);
                        sessionId = mDbxClient.files().uploadSessionStart(true)
                                .uploadAndFinish(in).getSessionId();
                        offset = f.length();
                        cursor = new UploadSessionCursor(sessionId, offset);
                        CommitInfo commitInfo = CommitInfo.newBuilder("/" +
                                record.recordFolderName + "/" + f.getName())
                                .withMode(WriteMode.OVERWRITE)
                                .withClientModified(new Date(f.lastModified()))
                                .build();
                        //System.out.println(commitInfo.toStringMultiline());
                        UploadSessionFinishArg arg = new UploadSessionFinishArg(cursor, commitInfo);
                        //System.out.println(arg.toStringMultiline());
                        entries.add(arg);
                    } catch (IOException ioEx) {
                        Log.e(TAG, "Error creating input stream: " + ioEx.getLocalizedMessage());
                        ThreadUtil.executeOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                Callback.onFailure("Failed to open file stream");
                            }
                        });
                    } catch (DbxException e) {
                        Log.e(TAG, "DBx exception: " + e.getLocalizedMessage());
                        ThreadUtil.executeOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                Callback.onFailure("Failed to commit batch files");
                            }
                        });
                    }
                }
                //now batch commit
                UploadSessionFinishBatchLaunch result = null;
                try {
                    result = mDbxClient.files().uploadSessionFinishBatch(entries);
                    ThreadUtil.executeOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            Callback.onSuccess(null);
                        }
                    });
                } catch (DbxException e) {
                    e.printStackTrace();
                    ThreadUtil.executeOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            Callback.onFailure("Failed to upload batch files");
                        }
                    });
                }
            }
        });
    }
}
