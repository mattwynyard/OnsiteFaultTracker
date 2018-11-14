package com.onsite.onsitefaulttracker.dropboxapi;

/**
 * Created by hihi on 6/27/2016.
 *
 * Dropbox Callback, override onSuccess or onFailure functions
 */
public abstract class DropboxCallback {

    /**
     * Override this for success
     *
     * @param object
     */
    public abstract void onSuccess(Object object);

    /**
     * Override this for failures
     *
     * @param errorMessage
     */
    public abstract void onFailure(String errorMessage);
}
