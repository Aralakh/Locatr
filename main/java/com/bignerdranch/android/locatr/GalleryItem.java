package com.bignerdranch.android.locatr;

import android.net.Uri;

/**
 * Created by lawren on 22/11/17.
 */

public class GalleryItem {
    private String title;
    private String id;
    private String url_s;
    private String owner;

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String caption) {
        title = caption;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url_s;
    }

    public Uri getPhotoPageUri(){
        return Uri.parse("https://www.flickr.com/photos/")
                .buildUpon()
                .appendPath(owner)
                .appendPath(id)
                .build();
    }

    public void setUrl(String url) {
        this.url_s = url;
    }

    @Override
    public String toString(){
        return title;
    }
}