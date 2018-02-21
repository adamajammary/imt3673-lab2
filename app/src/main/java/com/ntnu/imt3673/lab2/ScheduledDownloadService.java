package com.ntnu.imt3673.lab2;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.PersistableBundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

/**
 * Scheduled Download Service
 */
public class ScheduledDownloadService extends JobService {

    private DownloadRSSAsyncTask downloadTask;
    private int                  limit;
    private String               url;
    private JobParameters        parameters;

    @Override
    public boolean onStartJob(JobParameters parameters) {
        this.parameters = parameters;

        PersistableBundle bundle = this.parameters.getExtras();

        this.limit = bundle.getInt(Constants.DOWNLOAD_LIMIT);
        this.url   = bundle.getString(Constants.DOWNLOAD_URL);

        this.downloadTask = new DownloadRSSAsyncTask(this);
        this.downloadTask.execute();

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters parameters) {
        if (this.downloadTask != null)
            this.downloadTask.cancel(true);

        return false;
    }

    /**
     * Downloads the RSS feed in a background thread and updates the list view when complete.
     */
    private class DownloadRSSAsyncTask extends AsyncTask<Void, Integer, Void> {

        private ArrayList<ItemData>      result = new ArrayList<>();
        private String                   message;
        private ScheduledDownloadService serviceContext;

        public DownloadRSSAsyncTask(Context context) {
            this.serviceContext = (ScheduledDownloadService)context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            this.sendToastMessage("Downloading RSS feed ...");
            this.sendProgressValue(5);
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                InputStream inputStream = this.connectURL(new URL(url));

                if (inputStream == null) {
                    this.message = "Failed to download RSS\n- Check your internet connection";
                } else {
                    this.result = this.parseXML(inputStream, limit);

                    if (!this.result.isEmpty())
                        this.message = "Download completed successfully";
                    else
                        this.message = "Failed to download RSS\n- Make sure the URL is valid";

                    inputStream.close();
                }
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            this.sendProgressValue(values[0]);
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            this.sendToastMessage(this.message);
            this.sendDownloadComplete();
            jobFinished(parameters, false);
        }

        /**
         * Connects to the specified URL.
         * @param url The URL to connect to
         * @return The connection stream
         */
        private InputStream connectURL(URL url) {
            int         responseCode;
            InputStream inputStream = null;
            final int   TIMEOUT     = 3000;

            try {
                URLConnection connection = url.openConnection();

                if (connection instanceof HttpsURLConnection)
                    ((HttpsURLConnection)connection).setRequestMethod("GET");
                else
                    ((HttpURLConnection)connection).setRequestMethod("GET");

                connection.setReadTimeout(TIMEOUT);
                connection.setConnectTimeout(TIMEOUT);
                connection.setDoInput(true);
                connection.connect();

                if (connection instanceof HttpsURLConnection)
                    responseCode = ((HttpsURLConnection)connection).getResponseCode();
                else
                    responseCode = ((HttpURLConnection)connection).getResponseCode();

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

        private void sendDownloadComplete() {
            Intent intent = new Intent(Constants.DOWNLOAD_COMPLETED);
            intent.putParcelableArrayListExtra(Constants.DOWNLOAD_RESULT, this.result);
            LocalBroadcastManager.getInstance(this.serviceContext).sendBroadcast(intent);
        }

        private void sendProgressValue(int progress) {
            Intent intent = new Intent(Constants.DOWNLOAD_PROGRESS);
            intent.putExtra(Constants.DOWNLOAD_PROGRESS_VAL, progress);
            LocalBroadcastManager.getInstance(this.serviceContext).sendBroadcast(intent);
        }

        private void sendToastMessage(String message) {
            Intent intent = new Intent(Constants.DOWNLOAD_TOAST);
            intent.putExtra(Constants.DOWNLOAD_TOAST_MSG, message);
            LocalBroadcastManager.getInstance(this.serviceContext).sendBroadcast(intent);
        }

    }

}
