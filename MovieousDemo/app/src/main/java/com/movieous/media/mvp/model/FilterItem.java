package com.movieous.media.mvp.model;

public class FilterItem {
    private String mName;
    private String mFilterPath;
    private String mThumbPath;

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getFilterPath() {
        return mFilterPath;
    }

    public void setFilterPath(String path) {
        this.mFilterPath = path;
    }

    public String getThumbPath() {
        return mThumbPath;
    }

    public void setThumbPath(String path) {
        this.mThumbPath = path;
    }
}
