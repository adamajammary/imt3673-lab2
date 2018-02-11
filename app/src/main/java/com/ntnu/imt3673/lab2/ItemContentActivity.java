package com.ntnu.imt3673.lab2;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;

/**
 * Displays the content details of the RSS item the user clicked on.
 */
public class ItemContentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_item_content);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        WebView webView = this.findViewById(R.id.wv_itemContent);

        // Load the HTML content in the web view
        try {
            Intent intent  = getIntent();
            String content = intent.getStringExtra(Constants.ITEM_CONTENT);

            webView.loadData(content, "text/html; charset=utf-8", "utf-8");
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }
}
