package com.movieous.media.mvp.model;

import android.content.Context;
import android.util.Log;

import java.io.IOException;

public class FilterManager {
    private static final String TAG = "FilterManager";

    private FilterItem[] mFilterList;

    public FilterItem[] getFilterList(Context context) {
        if (mFilterList != null) {
            return mFilterList;
        }
        try {
            String[] filterNames = context.getAssets().list("filters");
            if (filterNames != null) {
                FilterItem[] filters = new FilterItem[filterNames.length];
                for (int i = 0; i < filterNames.length; ++i) {
                    filters[i] = new FilterItem();
                    filters[i].setName(filterNames[i]);
                    filters[i].setFilterPath("filters/" + filterNames[i] + "/filter.png");
                    filters[i].setThumbPath("filters/" + filterNames[i] + "/thumb.png");
                }
                mFilterList = filters;
                return filters;
            }
        } catch (IOException e) {
            Log.e(TAG, "get filter list failed:" + e.getMessage());
        }

        return null;
    }
}
