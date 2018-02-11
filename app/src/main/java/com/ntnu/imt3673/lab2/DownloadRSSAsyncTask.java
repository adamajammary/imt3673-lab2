package com.ntnu.imt3673.lab2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Downloads the RSS feed in a background thread and updates the list view when complete.
 */
public class DownloadRSSAsyncTask extends AsyncTask<Void, Integer, Void> {

    MainActivity mainActivity;

    public DownloadRSSAsyncTask(Context context) {
        this.mainActivity = (MainActivity)context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        if (this.mainActivity.isVisible) {
            Toast.makeText(this.mainActivity.getApplicationContext(), "Downloading RSS feed ...", Toast.LENGTH_SHORT).show();

            this.mainActivity.adapter.clear();
            this.mainActivity.itemsList.clear();
            this.mainActivity.adapter.notifyDataSetChanged();
            mainActivity.progressBar.setVisibility(View.VISIBLE);
            mainActivity.progressBar.setProgress(0);
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            InputStream inputStream = this.connectURL(new URL(this.mainActivity.url));

            if (inputStream == null) {
                this.mainActivity.result = "Failed to download RSS\n- Check your internet connection";
            } else {
                this.mainActivity.itemsList = this.parseXML(inputStream, this.mainActivity.limit);

                if (!this.mainActivity.itemsList.isEmpty())
                    this.mainActivity.result = "Download completed successfully";
                else
                    this.mainActivity.result = "Failed to download RSS\n- Make sure the URL is valid";
            }
        } catch (MalformedURLException | NullPointerException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);

        mainActivity.progressBar.setProgress(values[0]);
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);

        mainActivity.progressBar.setVisibility(View.GONE);

        if (!TextUtils.isEmpty(this.mainActivity.result) && this.mainActivity.isVisible)
            Toast.makeText(this.mainActivity.getApplicationContext(), this.mainActivity.result, Toast.LENGTH_LONG).show();

        this.mainActivity.adapter.addAll(this.mainActivity.itemsList);
        this.mainActivity.adapter.notifyDataSetChanged();
    }

    /**
     * Connects to the specified URL.
     * @param url The URL to connect to
     * @return The connection stream
     */
    private InputStream connectURL(URL url) {
        int         responseCode;
        InputStream inputStream = null;
        final int   TIMEOUT     = 10000;

        try {
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();

            connection.setReadTimeout(TIMEOUT);
            connection.setConnectTimeout(TIMEOUT);
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.connect();

            responseCode = connection.getResponseCode();

            if ((responseCode >= 200) && (responseCode < 400))
                inputStream  = connection.getInputStream();
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
            inputStream = null;
        }

        return inputStream;
    }

    /**
     * Parses the XML result.
     * @param inputStream The connection stream
     * @return A list of RSS items found in the XML result
     */
    private ArrayList<ItemData> parseXML(InputStream inputStream, int limit) {
        ArrayList<ItemData> result = new ArrayList<>();

        try {
            XmlPullParserFactory xmlFactory = XmlPullParserFactory.newInstance();
            xmlFactory.setNamespaceAware(true);

            XmlPullParser xmlParser = xmlFactory.newPullParser();
            xmlParser.setInput(inputStream, null);

            SimpleDateFormat dateFormatRSS   = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
            SimpleDateFormat dateFormatAtom  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ",      Locale.ENGLISH);
            SimpleDateFormat dateFormatAtom2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",       Locale.ENGLISH);
            ItemData         itemData        = null;
            boolean          itemIsReady     = false;
            int              xmlEvent        = xmlParser.getEventType();
            String           xmlText         = null;
            String           xmlTagName;
            String           xmlTagPrefix;

            while (xmlEvent != XmlPullParser.END_DOCUMENT) {
                xmlTagName   = xmlParser.getName();
                xmlTagPrefix = xmlParser.getPrefix();

                switch (xmlEvent) {
                    case XmlPullParser.START_TAG:
                        if ((xmlTagName.equals("item") || xmlTagName.equals("entry")) && (xmlTagPrefix == null)) {
                            itemData    = new ItemData();
                            itemIsReady = true;
                        }
                        break;
                    case XmlPullParser.TEXT:
                        xmlText = (itemIsReady ? xmlParser.getText().trim() : null);
                        break;
                    case XmlPullParser.END_TAG:
                        if (itemIsReady) {
                            if (xmlTagName.equals("title") && (xmlTagPrefix == null)) {
                                itemData.title = xmlText;
                            } else if (xmlTagName.equals("thumbnail") && xmlTagPrefix.equals("media")) {
                                for (int i = 0; i < xmlParser.getAttributeCount(); i++) {
                                    if (xmlParser.getAttributeName(i).equals("url")) {
                                        itemData.imageURL = xmlParser.getAttributeValue(i);
                                        break;
                                    }
                                }

                                if (!TextUtils.isEmpty(itemData.imageURL)) {
                                    itemData.imageBitmap = BitmapFactory.decodeStream(new URL(itemData.imageURL).openStream());
                                    itemData.imageBitmap = Bitmap.createScaledBitmap(itemData.imageBitmap, 128, 128, false);
                                }
                            } else if (xmlTagName.equals("pubDate") && (xmlTagPrefix == null)) {
                                itemData.date = dateFormatRSS.format(dateFormatRSS.parse(xmlText));
                            } else if (xmlTagName.equals("updated") && (xmlTagPrefix == null)) {
                                if (xmlText.length() > 19)
                                    itemData.date = dateFormatAtom.format(dateFormatAtom.parse(xmlText));
                                else
                                    itemData.date = dateFormatAtom2.format(dateFormatAtom2.parse(xmlText));
                            } else if ((xmlTagName.equals("description") || xmlTagName.equals("summary") || xmlTagName.equals("content")) && (xmlTagPrefix == null)) {
                                itemData.content = xmlText;
                            } else if ((xmlTagName.equals("item") || xmlTagName.equals("entry")) && (xmlTagPrefix == null)) {
                                result.add(itemData);
                                publishProgress(result.size() * 100 / limit);
                                itemIsReady = false;
                            }
                        }
                        break;
                    default:
                        break;
                }

                // Limit the result set
                if (result.size() >= limit)
                    break;

                xmlEvent = xmlParser.next();
            }
        } catch (IOException | XmlPullParserException | ParseException | NullPointerException e) {
            e.printStackTrace();
        }

        publishProgress(100);

        return result;
    }

}
