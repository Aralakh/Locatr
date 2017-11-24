package com.bignerdranch.android.locatr;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.List;

/**
 * Created by lawren on 22/11/17.
 */

public class LocatrFragment extends Fragment {
    private static final String TAG = "LocatrFragment";
    private ImageView mImageView;
    private ProgressBar mProgressBar;
    private GoogleApiClient mClient;
    private static final String[] LOCATION_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
    };
    private static final int REQUEST_LOCATION_PERMISSIONS = 0;
    public static LocatrFragment newInstance(){
        return new LocatrFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mClient = new GoogleApiClient.Builder(getActivity())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        getActivity().invalidateOptionsMenu();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .build();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.fragment_locatr, container, false);

        mProgressBar = v.findViewById(R.id.progress_bar);
        mProgressBar.setVisibility(View.GONE);
        mImageView = v.findViewById(R.id.image);
        mImageView.setVisibility(View.GONE);
        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_locatr, menu);

        MenuItem searchItem = menu.findItem(R.id.action_locate);
        searchItem.setEnabled(mClient.isConnected());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.action_locate:
                if(hasLocationPermission()) {
                    findImage();
                }else if(ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) || ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)){
                    AlertDialog.Builder alertBuilder = permissionsDialog();
                    AlertDialog alert = alertBuilder.create();
                    alert.show();
                }else{
                    requestPermissions(LOCATION_PERMISSIONS, REQUEST_LOCATION_PERMISSIONS);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public AlertDialog.Builder permissionsDialog(){
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
        alertBuilder.setCancelable(true);
        alertBuilder.setTitle(R.string.permissions_required);
        alertBuilder.setMessage(R.string.permissions_message);
        alertBuilder.setPositiveButton(R.string.dialog_accept, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                requestPermissions(LOCATION_PERMISSIONS, REQUEST_LOCATION_PERMISSIONS);
            }
        });
        return alertBuilder;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        switch(requestCode){
            case REQUEST_LOCATION_PERMISSIONS:
                if(hasLocationPermission()){
                    findImage();
                }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onStart(){
        super.onStart();
        getActivity().invalidateOptionsMenu();
        mClient.connect();
    }

    @Override
    public void onStop(){
        super.onStop();
        mClient.disconnect();
    }

    public void showProgressBar(){
        mProgressBar.setVisibility(View.VISIBLE);
        mImageView.setVisibility(View.GONE);
    }

    public void hideProgressBar(){
        mProgressBar.setVisibility(View.GONE);
        mImageView.setVisibility(View.VISIBLE);
    }

    private void findImage(){
        showProgressBar();
        LocationRequest request = LocationRequest.create();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setNumUpdates(1);
        request.setInterval(0);
       try{
           LocationServices.FusedLocationApi
                   .requestLocationUpdates(mClient, request, new LocationListener() {
                       @Override
                       public void onLocationChanged(Location location) {
                           Log.i(TAG, "Got a fix: " + location);
                           new SearchTask().execute(location);
                       }
                   });
       }catch(SecurityException se){
           Log.i(TAG, "LocationServices exception: " + se);
       }
    }

    private boolean hasLocationPermission(){
        int result = ContextCompat.checkSelfPermission(getActivity(), LOCATION_PERMISSIONS[0]);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private class SearchTask extends AsyncTask<Location, Void, Void>{
        private GalleryItem mGalleryItem;
        private Bitmap mBitmap;

        @Override
        protected Void doInBackground(Location...params){
            FlickrFetcher fetchr = new FlickrFetcher();
            List<GalleryItem> items = fetchr.searchPhotos(params[0]);

            if(items.size()==0){
                return null;
            }

            mGalleryItem = items.get(0);
            try{
                byte[] bytes = fetchr.getUrlBytes(mGalleryItem.getUrl());
                mBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            }catch(IOException ioe){
                Log.i(TAG, "Unable to download bitmap", ioe);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result){
            hideProgressBar();
            mImageView.setImageBitmap(mBitmap);
        }
    }

}
