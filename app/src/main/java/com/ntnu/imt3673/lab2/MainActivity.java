package com.ntnu.imt3673.lab2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Main activity - displays a list of items from the RSS feed.
 */
public class MainActivity extends AppCompatActivity {

    public ItemAdapter         adapter;
    public ArrayList<ItemData> itemsList = new ArrayList<>();
    public int                 freq;
    public int                 limit;
    public String              url;
    public String              result;
    public ProgressBar         progressBar;
    public boolean             isVisible = false;

    @Override
    public void onResume() {
        super.onResume();
        this.isVisible = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        this.isVisible = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_item_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, Constants.SETTINGS_RET);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == Constants.SETTINGS_RET) && (resultCode == RESULT_OK)) {
            String freq  = data.getStringExtra(Constants.PREFS_FREQ);
            String limit = data.getStringExtra(Constants.PREFS_LIMIT);
            String url   = data.getStringExtra(Constants.PREFS_URL);

            if (TextUtils.isEmpty(url))
                url = data.getStringExtra(Constants.PREFS_URLS);

            if (!TextUtils.isEmpty(url))
                this.url = url;

            if (!TextUtils.isEmpty(freq))
                this.freq = Integer.parseInt(freq);

            if (!TextUtils.isEmpty(limit))
                this.limit = Integer.parseInt(limit);

            this.scheduleDownload();
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
            this.adapter = new ItemAdapter(this, R.layout.layout_item, R.id.tv_itemTitle, this.itemsList);
            itemsView.setAdapter(this.adapter);

            // View the content details in another activity when a user clicks on an item
            itemsView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
                ItemData itemData = itemsList.get(position);
                Intent   intent   = new Intent(this, ItemContentActivity.class);

                intent.putExtra(Constants.ITEM_CONTENT, itemData.content);
                startActivity(intent);
            });

            // Get the user selected preferences
            this.freq  = Integer.parseInt(preferences.getString(Constants.PREFS_FREQ, ""));
            this.limit = Integer.parseInt(preferences.getString(Constants.PREFS_LIMIT, ""));
            this.url   = preferences.getString(Constants.PREFS_URL,  "");

            // If the user didn't enter an URL, use the selected example URL
            if (TextUtils.isEmpty(this.url))
                this.url = preferences.getString(Constants.PREFS_URLS, "");

            this.scheduleDownload();
        } catch (NullPointerException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void scheduleDownload() {
        Context context = this;
        Handler handler = new Handler();

        TimerTask downloadTask = new TimerTask() {
            public void run() {
                handler.post(() -> { new DownloadRSSAsyncTask(context).execute(); });
            }
        };

        Timer timer = new Timer();
        timer.schedule(downloadTask, 0, this.freq * 60 * 1000);
    }

}
