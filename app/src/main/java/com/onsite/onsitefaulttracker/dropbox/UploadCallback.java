package com.onsite.onsitefaulttracker.dropbox;

public abstract class UploadCallback {

    /**
     * Created by mjw on 16/01/2019.
     *
     * Dropbox Callback, override onSuccess or onFailure functions
     */

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
    /**
     * Override this when folder exists
     *
     */
        public abstract void onFolderExists();

}
