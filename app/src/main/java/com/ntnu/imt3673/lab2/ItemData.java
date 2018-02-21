package com.ntnu.imt3673.lab2;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Holds RSS data for each item in the feed.
 * https://developer.android.com/reference/android/os/Parcelable.html
 */
public class ItemData implements Parcelable {

    public String date;
    public String title;
    public Bitmap imageBitmap;
    public String imageURL;
    public String content;

    public ItemData() {}

    protected ItemData(Parcel in) {
        this.date        = in.readString();
        this.title       = in.readString();
        this.imageBitmap = in.readParcelable(Bitmap.class.getClassLoader());
        this.imageURL    = in.readString();
        this.content     = in.readString();
    }

    public static final Creator<ItemData> CREATOR = new Creator<ItemData>() {
        @Override
        public ItemData createFromParcel(Parcel in) {
            return new ItemData(in);
        }

        @Override
        public ItemData[] newArray(int size) {
            return new ItemData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(date);
        parcel.writeString(title);
        parcel.writeParcelable(imageBitmap, i);
        parcel.writeString(imageURL);
        parcel.writeString(content);
    }

}
