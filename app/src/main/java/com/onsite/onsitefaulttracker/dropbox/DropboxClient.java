package com.onsite.onsitefaulttracker.dropbox;

import android.app.Activity;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;

public class DropboxClient {

    private static final String ACCESS_TOKEN =
            "Ctnea--u6xAAAAAAAAAAFRpzlIy5hHTs8uAddZ4cpjIGk0Qjv7OInBgo2HVqOMGV";
    // The tag name for this class
    private static final String TAG = com.onsite.onsitefaulttracker.dropboxapi.DropboxClient.class.getSimpleName();

    // The parent activity which this dropbox client has been initialized with
    private Activity mParentActivity;

    // The dropbox client listener, communicates events with the parent activity
    private com.onsite.onsitefaulttracker.dropboxapi.DropboxClient.DropboxClientListener mDropboxClientListener;

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
    public void setDropboxClientListener(final com.onsite.onsitefaulttracker.dropboxapi.DropboxClient.DropboxClientListener dropboxClientListener) {
        mDropboxClientListener = dropboxClientListener;
    }
}
