package com.onsite.onsitefaulttracker.activity;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.onsite.onsitefaulttracker.R;
import com.onsite.onsitefaulttracker.activity.settings.SettingsActivity;

/**
 * Created by hihi on 6/6/2016.
 *
 * BaseActivity for this application.
 *
 * Provides common setup functionality that activities
 * will use throughout this application.
 */
public abstract class BaseActivity extends AppCompatActivity {

    // The TAG name for this activity
    public final static String TAG = BaseActivity.class.getSimpleName();

    // the key to set when orientation is changed if the default fragment has already been loaded
    protected static final String KEY_INSTANCE_STATE_FRAGMENT_LOADED = "fragment_loaded";

    /**
     * The various configurations for the action bar
     */
    public enum ActionBarConfig {
        Hidden,
        NoButtons,
        Settings,
        Back
    };

    // Is this activity the current foreground activity?
    private boolean mCurrentActivity;

    // The Action bar configuration for this activity
    private ActionBarConfig mActionBarConfig;

    // The title text view on the action bar
    private TextView mActionBarTitleTextView;

    // The action bar settings button
    private ImageButton mSettingsButton;

    // The action bar back button
    private ImageButton mBackButton;

    // The instance of the default fragment for this activity
    private BaseFragment mDefaultFragment;

    // The custom view of the action bar
    // (generated from R.layout.action_bar_layout)
    private RelativeLayout mActionBarCustomView;

    /**
     * Each activity overrides getDefaultFragment and returns the main fragment for that activity
     *
     * @return (to be overridden) The default fragment for this activity
     */
    protected abstract BaseFragment getDefaultFragment();

    /**
     * onCreate function, called by the system on creation of the activity.
     *
     * Common activity setup,
     * sets up the action bar,
     * loads the default fragment for this activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ActionBarConfig actionBarConfig = getDefaultActionBarConfig();
        if (actionBarConfig == ActionBarConfig.Hidden) {
            getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        } else {
            getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        }
        if (actionBarConfig == ActionBarConfig.Hidden) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setContentInsetsAbsolute(0,0);
        if (actionBarConfig == ActionBarConfig.Hidden) {
            toolbar.setVisibility(View.GONE);
        }

        setupActionBar();

        if (actionBarConfig == ActionBarConfig.Hidden) {
            setActionBarTransparent();
        }

        boolean addFragment = true;
        if(savedInstanceState != null) {
            addFragment = !savedInstanceState.getBoolean(KEY_INSTANCE_STATE_FRAGMENT_LOADED, false);
        }
        if (addFragment) {
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            mDefaultFragment = getDefaultFragment();
            fragmentTransaction.add(R.id.fragment_container, mDefaultFragment);
            fragmentTransaction.commit();
        }
    }

    /**
     * On Pause update the currentActivity flag,  set to false
     */
    @Override
    public void onPause() {
        super.onPause();
        mCurrentActivity = false;
    }

    /**
     * On Resume set current activity as true
     */
    @Override
    public void onResume() {
        super.onResume();
        mCurrentActivity = true;
    }

    /**
     * On SavedInstanceState set fragment_loaded to true so the activity knows not to reload it.
     * @param saveState
     */
    @Override
    protected void onSaveInstanceState(Bundle saveState) {
        super.onSaveInstanceState(saveState);
        saveState.putBoolean(KEY_INSTANCE_STATE_FRAGMENT_LOADED, true);
    }

    /**
     * Override this in child activities returning their default action bar configuration.
     *
     * @return
     */
    protected ActionBarConfig getDefaultActionBarConfig() {
        return ActionBarConfig.NoButtons;
    }

    /**
     * Make the action bar background fully transparent
     */
    protected void setActionBarTransparent() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#00000000")));
            actionBar.setStackedBackgroundDrawable(new ColorDrawable(Color.parseColor("#00000000")));
        }
    }

    /**
     * Sets up the action bar
     *
     * Override getTitle in child classes and return the title
     * to be displayed in the action bar
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);
            actionBar.setDisplayUseLogoEnabled(false);

            actionBar.setCustomView(R.layout.action_bar_layout);
            actionBar.setDisplayShowCustomEnabled(true);
            RelativeLayout customView = (RelativeLayout) actionBar.getCustomView();
            if (customView != null) {
                mActionBarCustomView = customView;
                mActionBarTitleTextView = (TextView) customView.findViewById(R.id.action_bar_title);

                mSettingsButton = (ImageButton) customView.findViewById(R.id.settings_button);
                mSettingsButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onSettingsClicked();
                    }
                });

                mBackButton = (ImageButton) customView.findViewById(R.id.back_button);
                mBackButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onBackClicked();
                    }
                });

                setActionBarConfig(getDefaultActionBarConfig());
            }
        }
    }

    /**
     * Set the action bar configuration (Shows and hides the relevant action bar buttons based on
     * the specified configuration)
     *
     * @param actionBarConfig
     */
    protected void setActionBarConfig(ActionBarConfig actionBarConfig) {
        mActionBarConfig = actionBarConfig;
        switch (actionBarConfig) {
            case NoButtons:
                mSettingsButton.setVisibility(View.GONE);
                mBackButton.setVisibility(View.GONE);
                break;

            case Back:
                mSettingsButton.setVisibility(View.GONE);
                mBackButton.setVisibility(View.VISIBLE);
                break;

            case Settings:
                mSettingsButton.setVisibility(View.VISIBLE);
                mBackButton.setVisibility(View.GONE);
                break;
        }
    }

    /**
     * Sets the display title on the action bar
     *
     * @param title
     */
    public void setTitle(String title) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
            actionBar.setDisplayShowTitleEnabled(false);
        }
        mActionBarTitleTextView.setText(title);
    }

    /**
     * Action when the user clicks on the settings button in the action bar
     * Opens the settings activity
     */
    protected void onSettingsClicked() {
        Intent settingsIntent = new Intent();
        settingsIntent.setClass(this, SettingsActivity.class);
        startActivity(settingsIntent);
    }

    /**
     * Action when the user clicks on the back button in the action bar
     */
    private void onBackClicked() {
        onBackPressed();
    }

    /**
     * Capture back press
     */
    @Override
    public void onBackPressed() {
        if (mCurrentActivity && mDefaultFragment != null && mDefaultFragment.onBackClicked()) {
            // The fragment has consumed the back click action,
            // just return
            return;
        }
        // The fragment has not consumed the back press action
        // let the activity handle it in its default way
        super.onBackPressed();
    }

}
