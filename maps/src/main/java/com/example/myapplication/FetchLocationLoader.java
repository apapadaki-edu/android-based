package com.example.myapplication;

import android.content.Context;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.loader.content.AsyncTaskLoader;


import java.util.List;


public class FetchLocationLoader extends AsyncTaskLoader<Bundle> {


    Context context;
    Double lat;
    Double lon;
    Bundle replyL = null;
    Geocoder geocoder;
    public FetchLocationLoader(@NonNull Context context, Bundle latLong) {
        super(context);
        this.context = context;
        this.lat = latLong.getDouble("lat");
        this.lon = latLong.getDouble("long");
        geocoder = new Geocoder(context);

    }

    @Nullable
    @Override
    public Bundle loadInBackground() {
        replyL = new Bundle();

        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);

            Address a = addresses.get(0);
            replyL.putDouble("lat", lat);
            replyL.putDouble("long", lon);
            replyL.putString("address", a.getAddressLine(0) != null ? a.getAddressLine(0) : "");
            replyL.putString("city", a.getLocality());
            replyL.putString("country", a.getCountryName());
            replyL.putString("postalCode", a.getPostalCode());
            replyL.putString("knownName", a.getFeatureName()!=null?a.getFeatureName():"");

            Log.i("InThread", a.toString());
            return replyL;

        }catch (Exception e){
            e.printStackTrace();
        }
        Log.i("InThread","");

        return null;
    }

    @Override
    protected void onStartLoading(){
            forceLoad();
    }
}
