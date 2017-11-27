package com.bignerdranch.android.locatr;

import android.location.Location;
import android.net.Uri;
import android.util.Log;
import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lawren on 22/11/17.
 */

public class FlickrFetcher {
    private static final String TAG = "FlickrFetcher";
    private static final String API_KEY ="17799f23e3aa7e76d8436fb030d88745";
    private static final String FETCH_RECENTS_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    private static final Uri ENDPOINT = Uri.parse("https://api.flickr.com/services/rest")
            .buildUpon()
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s, geo")
            .build();
    private int maxPages;
    private int totalItems;
    private int itemsPerPage;
    private int currentPage = 1;

    public int getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public int getItemsPerPage() {
        return itemsPerPage;
    }

    public void setItemsPerPage(int itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }

    public int getCurrentPage(){
        return currentPage;
    }

    public void setCurrentPage(int page){
        currentPage = page;
    }

    public byte[] getUrlBytes(String urlSpec) throws IOException{
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        try{
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK){
                throw new IOException(connection.getResponseMessage() + ": with " + urlSpec);
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while((bytesRead = in.read(buffer)) > 0){
                out.write(buffer, 0, bytesRead);
            }

            out.close();
            return out.toByteArray();
        }finally{
            connection.disconnect();
        }
    }

    public String getUrlString(String urlSpec) throws IOException{
        return new String(getUrlBytes(urlSpec));
    }

    public List<GalleryItem> fetchRecentPhotos(int currentPage){
        String url = buildUrl(FETCH_RECENTS_METHOD, null,  currentPage);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> searchPhotos(String query){
        String url = buildUrl(SEARCH_METHOD, query, 0);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> searchPhotos(Location location){
        String url = buildUrl(location);
        return downloadGalleryItems(url);
    }

    private String buildUrl(String method, String query, int currentPage){
        Uri.Builder uriBuilder = ENDPOINT.buildUpon().appendQueryParameter("method", method);
        if(method.equals(SEARCH_METHOD)){
            uriBuilder.appendQueryParameter("text", query);
        }else{
            uriBuilder.appendQueryParameter("page", Integer.toString(currentPage));
            setCurrentPage(currentPage);
        }
        return uriBuilder.build().toString();
    }

    private String buildUrl(Location location){
        return ENDPOINT.buildUpon()
                .appendQueryParameter("method", SEARCH_METHOD)
                .appendQueryParameter("lat", "" + location.getLatitude())
                .appendQueryParameter("lon", "" + location.getLongitude())
                .build().toString();
    }

    private List<GalleryItem> downloadGalleryItems(String url){
        List<GalleryItem> items = new ArrayList<>();

        try{
            String jsonString = getUrlString(url);

            Log.i(TAG, "Received JSON: " + jsonString);

            Gson gson = new Gson();
            PhotoResultQuery result = gson.fromJson(jsonString, PhotoResultQuery.class);

            setMaxPages(result.getPhotos().getMaxPages());
            setTotalItems(result.getPhotos().getTotal());
            setItemsPerPage(result.getPhotos().getItemsPerPage());

            items = result.getPhotos().getGalleryItemsList();
        }catch(IOException ioe){
            Log.e(TAG, "Failed to fetch items", ioe);
        }catch(Exception e){
            Log.e(TAG, "Failed to parse JSON: " +e.getMessage(), e);
        }

        return items;
    }



}