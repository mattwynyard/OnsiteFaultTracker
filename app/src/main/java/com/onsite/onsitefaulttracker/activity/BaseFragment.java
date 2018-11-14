package com.onsite.onsitefaulttracker.activity;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * Created by hihi on 6/6/2016.
 *
 * The Base Fragment for Fragments of this application to extend off.
 */
public abstract class BaseFragment extends Fragment {

    // The tag name for this class
    private static final String TAG = BaseFragment.class.getSimpleName();

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
        if(container == null) {
            // if container is null layout is never used
            return null;
        }

        getBaseActivity().setTitle(getDisplayTitle());
        return inflater.inflate(getLayoutResourceId(), container, false);
    }

    /**
     * Override this and return the fragments default layout resource id
     *
     * @return
     */
    protected abstract int getLayoutResourceId();

    /**
     * Return the parent activity as the class BaseActivity, this function is for
     * easier access to BaseActivities functionality
     *
     * @return
     */
    public BaseActivity getBaseActivity() {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            return (BaseActivity) activity;
        } else {
            return null;
        }
    }

    /**
     * Override and handle on back action return true if consumed the event
     * (if the parent activity should not close as is its normal coarse of action) otherwise
     * false
     *
     * @return true if activity should handle the back action, otherwise false
     */
    public boolean onBackClicked() {
        return false;
    }

    /**
     * Override this and return the default display title (action bar title) for each fragment
     *
     * @return
     */
    protected String getDisplayTitle() {
        return null;
    }

}
