package com.ntnu.imt3673.lab2;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

/**
 * Main activity - displays a list of items from the RSS feed.
 */
public class MainActivity extends AppCompatActivity {

    private ItemAdapter     adapter;
    private JobInfo.Builder builder;
    private ProgressBar     progressBar;
    private MainReceiver    receiver;

    @Override
    public void onResume() {
        super.onResume();

        this.receiver  = new MainReceiver();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.DOWNLOAD_COMPLETED);
        intentFilter.addAction(Constants.DOWNLOAD_PROGRESS);
        intentFilter.addAction(Constants.DOWNLOAD_TOAST);

        LocalBroadcastManager.getInstance(this).registerReceiver(this.receiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(this.receiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_item_list, menu);
        return true;
    }

    /**
     * Displays the Settings (preferences) menu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, Constants.SETTINGS_RET);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Applies user preferences when returning from the Settings menu.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == Constants.SETTINGS_RET) && (resultCode == RESULT_OK)) {
            String frequency = data.getStringExtra(Constants.PREFS_FREQ);
            String limit     = data.getStringExtra(Constants.PREFS_LIMIT);
            String url       = data.getStringExtra(Constants.PREFS_URL);

            if (TextUtils.isEmpty(url))
                url = data.getStringExtra(Constants.PREFS_URLS);

            this.scheduleDownload(url, Integer.parseInt(limit), Integer.parseInt(frequency));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_list);

        // Set the default preferences if not already set
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Get the user selected preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Get the list view which will hold the RSS items
        ListView itemsView = this.findViewById(R.id.lv_items);

        try {
            this.progressBar = findViewById(R.id.progress_bar);

            // Set a custom adapter to handle the layout of each item in the list view
            this.adapter = new ItemAdapter(this, R.layout.layout_item);

            itemsView.setAdapter(this.adapter);

            // View the content details in another activity when a user clicks on an item
            itemsView.setOnItemClickListener(
                (AdapterView<?> parent, View view, int position, long id) -> {
                    ItemData itemData = adapter.getItem(position);
                    Intent   intent   = new Intent(this, ItemContentActivity.class);

                    intent.putExtra(Constants.ITEM_CONTENT, itemData.content);
                    startActivity(intent);
                }
            );

            // Get the user selected preferences
            int    frequency = Integer.parseInt(preferences.getString(Constants.PREFS_FREQ, ""));
            int    limit     = Integer.parseInt(preferences.getString(Constants.PREFS_LIMIT, ""));
            String url       = preferences.getString(Constants.PREFS_URL,  "");

            // If the user didn't enter an URL, use the selected example URL
            if (TextUtils.isEmpty(url))
                url = preferences.getString(Constants.PREFS_URLS, "");

            this.scheduleDownload(url, limit, frequency);
        } catch (NullPointerException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    /**
     * Schedules a periodic download job based on user preferences using JobScheduler.
     * https://developer.android.com/training/run-background-service/index.html
     * https://developer.android.com/topic/performance/scheduling.html
     * https://developer.android.com/reference/android/app/job/JobScheduler.html
     */
    private void scheduleDownload(String url, int limit, int frequency) {
        if (this.builder == null) {
            this.builder = new JobInfo.Builder(
                Constants.DOWNLOAD_JOB_ID, new ComponentName(this, ScheduledDownloadService.class)
            );
        }

        PersistableBundle bundle = new PersistableBundle();

        bundle.putInt(Constants.DOWNLOAD_LIMIT,  limit);
        bundle.putString(Constants.DOWNLOAD_URL, url);

        this.builder.setExtras(bundle);
        this.builder.setPeriodic(frequency * Constants.MINUTES_IN_HOUR * Constants.MS_IN_SECOND);

        getSystemService(JobScheduler.class).schedule(this.builder.build());
    }

    /**
     * Receives a message from the download service when a download is complete.
     * Refreshes the ListView with downloaded data.
     */
    private class MainReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == Constants.DOWNLOAD_PROGRESS) {
                int progress = intent.getIntExtra(Constants.DOWNLOAD_PROGRESS_VAL, 0);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(progress);
            } else if (intent.getAction() == Constants.DOWNLOAD_TOAST) {
                String message = intent.getStringExtra(Constants.DOWNLOAD_TOAST_MSG);
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            } else if (intent.getAction() == Constants.DOWNLOAD_COMPLETED) {
                progressBar.setVisibility(View.GONE);
                adapter.addAll(intent.getParcelableArrayListExtra(Constants.DOWNLOAD_RESULT));
                adapter.notifyDataSetChanged();
            }
        }

    }

}
