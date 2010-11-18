/*
 * (#) net.brainage.apkinstaller.AppListActivity
 * Created on 2010. 10. 11.
 */
package net.brainage.apkinstaller.ui;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.brainage.apkinstaller.AppConstants;
import net.brainage.apkinstaller.R;
import net.brainage.apkinstaller.service.AppScanService;
import net.brainage.apkinstaller.ui.adapter.AppArrayAdapter;
import net.brainage.apkinstaller.util.AppArrayList;
import net.brainage.apkinstaller.util.UiUtil;

/**
 * 설치할 APK 파일의 목록을 표시한다.
 * 
 * @author ntmyoungseok.seo@lge.com
 */
public class AppListActivity extends ListActivity
{

    /**
     * 
     */
    private static final String TAG = "AppListActivity";

    /**
     * 
     */
    private AppArrayList mAppList;

    /**
     * 
     */
    private AppArrayAdapter mAdapter;

    /**
     * 
     */
    private TextView mEmptyText;

    /**
     * 
     */
    private ProgressBar mRefreshProgress;

    /**
     * 
     */
    private ImageButton mRefreshButton;

    /**
     * 
     */
    private boolean mExternalStorageAvailable = false;

    /**
     * 
     */
    private boolean mExternalStorageWriteable = false;

    /**
     * 
     * @param savedInstanceState
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initActivity();
    }

    /**
     * 
     */
    private void initActivity() {
        setContentView(R.layout.application_list);

        /* custom title */
        ((TextView) findViewById(R.id.title_text)).setText(getTitle());

        mRefreshProgress = (ProgressBar) findViewById(R.id.title_refresh_progress);
        mRefreshButton = (ImageButton) findViewById(R.id.btn_title_refresh);

        mEmptyText = (TextView) findViewById(android.R.id.empty);

        mAppList = AppArrayList.getInstance();

        mAdapter = new AppArrayAdapter(this, R.layout.application_list_item, mAppList.getList());
        setListAdapter(mAdapter);

        /* update external storage state */
        updateExternalStorageState();
    }

    /**
     * 
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(AppConstants.ACTION_REFRESHED_APPLIST);
        registerReceiver(applicationRefreshReceiver, filter);

        refreshAppList();
    }

    /**
     * 
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {
        unregisterReceiver(applicationRefreshReceiver);

        super.onPause();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onStop()
     */
    @Override
    protected void onStop() {
        if ( AppConstants.DEBUG ) {
            Log.d(TAG, "onStop() ---------------------------");
        }
        stopService(new Intent(this, AppScanService.class));
        super.onStop();
    }

    /**
     * @param newConfig
     * @see android.app.Activity#onConfigurationChanged(android.content.res.Configuration)
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if ( newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE ) {
            if ( AppConstants.DEBUG ) {
                Log.d(TAG, "onConfigurationChanged() --------------");
                Log.d(TAG, "    Configuration.ORIENTATION_LANDSCAPE");
            }
            initActivity();
        }
    }

    /**
     * 
     * @param menu
     * @return
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.app_list_menus, menu);
        return true;
    }

    /**
     * 
     * @param item
     * @return
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch ( item.getItemId() ) {
            case R.id.menu_settings:
                UiUtil.goSettings(this);
                return true;
        }
        return false;
    }

    /**
     * @param v
     */
    public void onRefreshClick(View v) {
        refreshAppList();
    }

    /**
     * 
     */
    private void refreshAppList() {
        if ( !mExternalStorageAvailable ) {
            /* display alert dialog for not available external storage */
            mEmptyText.setText(R.string.txt_noavailable_extstorage);
            return;
        }

        if ( AppConstants.DEBUG ) {
            Log.d(TAG, "Refresh Application List...");
        }

        reloadNowPlaying(true);

        mEmptyText.setText(R.string.txt_loading);

        mAppList.clear();

        Intent serviceIntent = new Intent(this, AppScanService.class);
        startService(serviceIntent);
    }

    /**
     * @param flag
     */
    private void reloadNowPlaying(boolean flag) {
        if ( flag ) {
            mRefreshProgress.setVisibility(View.VISIBLE);
            mRefreshButton.setVisibility(View.GONE);
        } else {
            mRefreshProgress.setVisibility(View.GONE);
            mRefreshButton.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 
     */
    BroadcastReceiver applicationRefreshReceiver = new BroadcastReceiver() {
        /**
         * 
         * @param context
         * @param intent
         * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            mAdapter.notifyDataSetChanged();

            reloadNowPlaying(false);

            if ( mAppList.getList().size() == 0 ) {
                mEmptyText.setText(R.string.no_applications);
            }
        }
    };

    /**
     * update external storage state
     */
    private void updateExternalStorageState() {
        String state = Environment.getExternalStorageState();
        if ( Environment.MEDIA_MOUNTED.equals(state) ) {
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if ( Environment.MEDIA_MOUNTED_READ_ONLY.equals(state) ) {
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }
    }

}
