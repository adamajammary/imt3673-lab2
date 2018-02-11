package com.ntnu.imt3673.lab2;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Custom array adapter which returns our custom item view.
 */
public class ItemAdapter extends ArrayAdapter<ItemData> {

    private MainActivity        activity;
    private ArrayList<ItemData> itemsList = new ArrayList<>();
    private int                 layoutId;

    /**
     * ItemAdapter constructor
     * @param context Current activity context
     * @param resource Resource ID of the layout file
     * @param textViewResourceId Resource ID of the TextView component in the layout
     * @param objects Array of item data
     */
    public ItemAdapter(Context context, int resource, int textViewResourceId, ArrayList<ItemData> objects) {
        super(context, resource, textViewResourceId, objects);

        this.activity  = (MainActivity)context;
        this.layoutId  = resource;
        this.itemsList = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = null;

        // Set the title, date and thumbnail image for the item
        try {
            rowView = this.activity.getLayoutInflater().inflate(this.layoutId, null);

            ItemData  itemData  = this.itemsList.get(position);
            TextView  titleView = rowView.findViewById(R.id.tv_itemTitle);
            TextView  dateView  = rowView.findViewById(R.id.tv_itemDate);
            ImageView imageView = rowView.findViewById(R.id.iv_itemImage);

            titleView.setText(itemData.title);
            dateView.setText(itemData.date);

            if (!TextUtils.isEmpty(itemData.imageURL) && (itemData.imageBitmap != null))
                imageView.setImageBitmap(itemData.imageBitmap);
            else
                imageView.setImageResource(R.mipmap.ic_launcher);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        return rowView;
    }

}
