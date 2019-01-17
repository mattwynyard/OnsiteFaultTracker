package com.onsite.onsitefaulttracker.dropbox;

import android.content.Context;
import android.os.AsyncTask;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.users.FullAccount;

public class DropBoxCallback extends AsyncTask<String, Void, FullAccount> {
    private final DropboxClient mClient;
    private DbxClientV2 mDbxClient;
    private Exception mException;
    private String token;
    private Callback mCallback;

    public interface Callback {
        void onLoginComplete(FullAccount result, DbxClientV2 client);
        void onError(Exception e);
    }

    DropBoxCallback(DropboxClient client, String token, Callback callback) {
        mClient = client;
        this.token = token;
        mCallback = callback;
    }

    @Override
    protected void onPostExecute(FullAccount result) {
        super.onPostExecute(result);
        if (mException != null) {
            mCallback.onError(mException);
        } else if (result == null) {
            mCallback.onError(null);
        } else {
            mCallback.onLoginComplete(result, mDbxClient);
        }
    }

    @Override
    protected FullAccount doInBackground(String... params) {
        DbxRequestConfig config = new DbxRequestConfig("OnsiteFaultTracker");
        mDbxClient = new DbxClientV2(config, this.token);

        try {
            // Get current account info
            return mDbxClient.users().getCurrentAccount();
        }catch (DbxException e){
            mException = e;
        }
        return null;
    }
}
