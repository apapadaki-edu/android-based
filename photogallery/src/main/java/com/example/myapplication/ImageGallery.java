package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;

import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.flexbox.FlexboxLayout;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;


public class ImageGallery extends AppCompatActivity {

    private final int MY_PERMISSION_CAMERA = 99;
    private final int MY_PERMISSION_STORAGE = 100;
    private final int MY_PERMISSION_LOCATION = 101;

    private final int REQUEST_IMAGE_CAPTURE = 199;

    private static String curImageTime;
    private static String curImageLocation;

    Button takePhoto;

    FlexboxLayout parent;
    LayoutInflater inflater;
    //added
    /**
     * Contains parameters used by {@link com.google.android.gms.location.FusedLocationProviderApi}.
     */
    private LocationRequest mLocationRequest;

    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient; // will keep

    /**
     * Callback for changes in location.
     */
    private LocationCallback mLocationCallback;
    /**
     * The current location.
     */
    private Location mLocation;

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 2000;

    /**
     * The fastest rate for active location updates. Updates will never be more frequent
     * than this value.
     */
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    //added end


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        takePhoto = (Button) findViewById(R.id.take_photo);

        takePhoto.setOnClickListener(view -> {
            if (!checkPermissions(Manifest.permission.CAMERA)) {
                requestPermissions(Manifest.permission.CAMERA, MY_PERMISSION_CAMERA,
                        R.string.permission_rationale_camera);
            }
            dispatchTakePictureIntent();
        });


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationRequest = LocationRequest.create().setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setInterval(UPDATE_INTERVAL_IN_MILLISECONDS).setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                //TODO:check if the commented line changes anything when uncommented
                //super.onLocationResult(locationResult);
                mLocation = locationResult.getLastLocation();
                Log.i("locSuccess", (mLocation==null)?"No Location Found": mLocation.toString());
            }
        };

        inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        parent = (FlexboxLayout) findViewById(R.id.gallery);

        new UpdateUI(this).execute();

    }


    @Override
    protected void onStart() {
        super.onStart();
        if (!checkPermissions(Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestPermissions(Manifest.permission.ACCESS_FINE_LOCATION, MY_PERMISSION_LOCATION,
                    R.string.permission_rationale_location);
        }

        if (!checkPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            requestPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, MY_PERMISSION_STORAGE,
                    R.string.permission_rationale_storage);
        }

    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!checkPermissions(Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestPermissions(Manifest.permission.ACCESS_FINE_LOCATION, MY_PERMISSION_LOCATION,
                    R.string.permission_rationale_location);
        }
        getLastLocation();
        new UpdateUI(this).execute();
    }


    @Override
    protected void onPause() {
        super.onPause();
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // Bitmap image = (Bitmap) data.getExtras().get("data"); //thumbnail only
            // Bitmap takerImage = BitmapFactory.decodeFile(curImageLocation);
            galleryAddPic();
            Bundle image = new Bundle();
            image.putString(ImageContentProvider.imageUri, curImageLocation);
            image.putString(ImageContentProvider.location, (mLocation == null)? " ":Double.toString(mLocation.getLatitude())
                    + ',' + mLocation.getLongitude());
            image.putString(ImageContentProvider.date, curImageTime);
            image.putString(ImageContentProvider.comment, "");
            // Insert image in db
            addImage(image);
        }

    }


    private boolean checkPermissions(String permission) {
        int permissionCode = ActivityCompat.checkSelfPermission(this,
                permission);
        return permissionCode == PackageManager.PERMISSION_GRANTED;
    }


    private void requestPermissions(String permission, int mRequestCode, int snackBarStringResourceId) {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i("ImageGallery", "Displaying permission rationale to provide additional context.");

            showSnackbar(snackBarStringResourceId, android.R.string.ok,
                    view -> {
                        // Request permission
                        ActivityCompat.requestPermissions(ImageGallery.this,
                                new String[]{permission},
                                mRequestCode);
                    });

        } else {
            Log.i("MapActivity", "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(ImageGallery.this,
                    new String[]{permission},
                    mRequestCode);
        }
    }


    private void onPermissionDenied() {
        showSnackbar(R.string.permission_denied_explanation, R.string.settings,
                view -> {
                    // Build intent that displays the App settings screen.
                    Intent intent = new Intent();
                    intent.setAction(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package",
                            BuildConfig.APPLICATION_ID, null);
                    intent.setData(uri);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                });
    }


    /**
     * Callback received when a permissions request has been completed.
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i("ImageGallery", "onRequestPermissionResult");

        switch (requestCode) {
            case MY_PERMISSION_LOCATION:
                if (grantResults.length <= 0) {
                    // If user interaction was interrupted, the permission request is cancelled and you
                    // receive empty arrays.
                    Log.i("ImageGallery", "User interaction was cancelled.");
                } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted.
                    getLastLocation();
                    //TODO: CHECK IF I WILL KEEP THIS

                } else {
                    // Permission denied.

                    // Notify the user via a SnackBar that they have rejected a core permission for the
                    // app, which makes the Activity useless. In a real app, core permissions would
                    // typically be best requested during a welcome-screen flow.

                    // Additionally, it is important to remember that a permission might have been
                    // rejected without asking the user for permission (device policy or "Never ask
                    // again" prompts). Therefore, a user interface affordance is typically implemented
                    // when permissions are denied. Otherwise, your app could appear unresponsive to
                    // touches or interactions which have required permissions.
                    onPermissionDenied();
                }
                break;

            case MY_PERMISSION_STORAGE:
                // If request is cancelled, the result arrays are empty
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("ImageGallery", "User interaction was cancelled.");
                    //TODO: load photos

                } else {
                    onPermissionDenied();
                }
                break;

            case MY_PERMISSION_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //TODO: CAMERA ACTION
                } else {
                    onPermissionDenied();
                }
                break;
        }
    }


    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(findViewById(android.R.id.content),
                        getString(mainTextStringId),
                        Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }



    @SuppressLint("QueryPermissionsNeeded")
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        //if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
        // Create the File where the photo should go

        File photoFile = null;
        try {
            photoFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
        } catch (IOException ex) {
            // Error occurred while creating the File
            Log.e("ImageGallery", ex.getMessage());
        }
        // Continue only if the File was successfully created
        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(Objects.requireNonNull(getApplicationContext()),
                    BuildConfig.APPLICATION_ID + ".provider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
        //}

    }


    /**
     * Create a File for saving an image or video
     */
    private File getOutputMediaFile(int type) throws IOException {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        //File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
        //        Environment.DIRECTORY_PICTURES), "GalleryApp");

        //Log.i("Image Gallery Activity:", mediaStorageDir.getAbsolutePath());
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        //if (!mediaStorageDir.exists()) {
        //    if (!mediaStorageDir.mkdirs()) {
        //        Log.d("GalleryApp", "failed to create directory");
        //        return null;
        //    }
        //}

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;

        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = File.createTempFile(
                    "IMG_" + timeStamp + "_",
                    ".jpg",
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            );
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = File.createTempFile(
                    "VID_" + timeStamp,
                    ".mp4",
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            );
        } else {
            return null;
        }
        curImageLocation = mediaFile.getAbsolutePath();
        Log.i("PATH", curImageLocation);
        curImageTime = timeStamp;
        return mediaFile;
    }


    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);

        if (curImageLocation != null){
            File f = new File(curImageLocation);
            Uri contentUri = Uri.fromFile(f);
            mediaScanIntent.setData(contentUri);
            this.sendBroadcast(mediaScanIntent);
        }
    }


    public void addImage(Bundle image){
        ContentValues values = new ContentValues();
        Log.i("inAdd", "inAdd");
        values.put(ImageContentProvider.imageUri, image.getString("image_uri"));
        values.put(ImageContentProvider.location, image.getString("location"));
        values.put(ImageContentProvider.date, image.getString("date"));
        values.put(ImageContentProvider.comment, image.getString("comment"));
        Uri uri = getContentResolver().insert(
                ImageContentProvider.CONTENT_URI, values);
        Toast.makeText(getBaseContext(), uri.toString(), Toast.LENGTH_LONG).show();
        ImageView img = createUIThumb();
        try{
            Bitmap imageBitmap = setPic(image.getString("image_uri"));
            img.setImageBitmap(imageBitmap);
        }catch (FileNotFoundException ex){
            //todo
        }

    }


    private class UpdateUI extends AsyncTask<int[], Integer, Integer> {
        private ArrayList<Bundle> imagesArray = new ArrayList<>();
        private ArrayList<Bitmap> bitmaps = new ArrayList<>();
        private Activity activity;

        public UpdateUI(Activity activity) {
            this.activity = activity;
        }

        @SuppressLint("Range")
        protected Integer doInBackground(int[]... ints) {
            ArrayList<Bundle> imagesRetrieved = new ArrayList<>();
            ArrayList<Bitmap> bitmapsRetrieved = new ArrayList<>();
            Bundle image = new Bundle();

            String URL = "content://com.example.myapplication.ImageContentProvider";
            Uri images = Uri.parse(URL);
            // Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            //  startActivityForResult(i, LOAD_IMAGE_REQUEST);

            Cursor c = managedQuery(images, null, null, null, null);
            if(c.moveToFirst()){
                do {
                    image.putString("id", c.getString(c.getColumnIndex(ImageContentProvider._ID)));
                    image.putString("uri",c.getString(c.getColumnIndex(ImageContentProvider.imageUri)));
                    image.putString("location", c.getString(c.getColumnIndex(ImageContentProvider.location)));
                    image.putString("date", c.getString(c.getColumnIndex(ImageContentProvider.date)));
                    image.putString("comment", c.getString(c.getColumnIndex(ImageContentProvider.comment + "")));
                    imagesRetrieved.add(image);

                    try {
                        bitmapsRetrieved.add(setPic(c.getString(c.getColumnIndex(ImageContentProvider.imageUri))));

                    } catch (FileNotFoundException ex) {
                        bitmapsRetrieved.add(BitmapFactory.decodeResource(activity.getResources(), R.drawable.no_image));
                    }
                } while (c.moveToNext());

                imagesArray = imagesRetrieved;
                bitmaps = bitmapsRetrieved;
            }
            return imagesArray.size();
        }

        protected void onPostExecute(Integer result) {
            parent.removeAllViews();
            for (int i=0; i<result; i++){
                ImageView img = createUIThumb();
                img.setTag(imagesArray.get(i));
                img.setImageBitmap(bitmaps.get(i));

                img.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(view.getContext(), SingleImageActivity.class);
                        intent.putExtra("data", (Bundle) img.getTag());
                        startActivity(intent);
                    }
                });
            }
        }
    }


    private Bitmap setPic(String imageFile) throws FileNotFoundException{

        // Get the dimensions of the View
        //TODO: define as arguments
        Display display = getWindowManager().getDefaultDisplay();
        int widthP = display.getWidth() - (2 * (int) getResources().getDimension(R.dimen.activity_margin)) - 20;
        int dim = widthP/5;


        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(imageFile, bmOptions);

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.max(1, Math.min(photoW / dim, photoH / dim));

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(imageFile, bmOptions);
        return bitmap;
    }


    public ImageView createUIThumb(){
        View eventItemView = inflater.inflate(R.layout.image_thumb, null, false);
        int id = View.generateViewId();
        eventItemView.setId(id);
        parent.addView(eventItemView);
        LinearLayout  imageParent = (LinearLayout) findViewById(id);
        ImageView imageView = (ImageView) imageParent.getChildAt(0);
        return imageView;
    }


    private void getLastLocation() {
        try {
            mFusedLocationClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (!task.isSuccessful() || task.getResult() == null) {
                                Log.w("LocPer", "Failed to get location.");
                                //Makes a request for location updates. Note that in this sample we merely log the
                                try {
                                    mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                            mLocationCallback, Looper.getMainLooper());
                                } catch (SecurityException unlikely) {
                                    Log.e("LocPer", "Lost location permission. Could not request updates. " + unlikely);
                                }
                            } else {
                                mLocation = task.getResult();
                                Log.i("locSuccess", mLocation.toString());
                            }
                        }
                    });
        } catch (SecurityException unlikely) {
            Log.e("LocPer", "Lost location permission." + unlikely);
        }
    }
}