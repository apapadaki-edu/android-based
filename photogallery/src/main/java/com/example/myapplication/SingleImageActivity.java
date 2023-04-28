package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;


public class SingleImageActivity extends AppCompatActivity{

    // request flags coming from main
    private static final int NEW_EVENT_REQUEST = 0;
    public static final int EDIT_EVENT_REQUEST = 1;

    //deceleration of input fields
    private ImageView img;
    private TextView time;
    private TextView location;
    private EditText comment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        Bundle data = getIntent().getExtras().getBundle("data");

        img = (ImageView) findViewById(R.id.image);
        Bitmap imageBitmap = setPic(data.getString("uri"));
        img.setImageBitmap(imageBitmap);

        time = (TextView) findViewById(R.id.date);
        time.setText(data.getString("date"));
        location = (TextView) findViewById(R.id.location);
        location.setText(data.getString("location"));
        Log.i("LocationFromUi", data.getString("location"));

        comment = (EditText) findViewById(R.id.comment);
        comment.setText(data.getString("comment"));
        Log.i("from ui",  data.getString("comment"));

        //get reference to buttons
        Button save = (Button) findViewById(R.id.button_save);
        Button delete = (Button) findViewById(R.id.button_delete);



        // Delete button listener
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int recDeleted = getContentResolver().delete(
                        Uri.parse(ImageContentProvider.CONTENT_URI + "/"+data.getString("id")), null, null);
                Log.i("Delete", recDeleted +"");
                //returns black screen because I am modifying the parent activity
                //this will be resolved after I create a dynamically updated view instead of
                // a flexboxLayout and call datasetModified in Main activity
                int imageParentId = ((View)img.getParent()).getId();
                LinearLayout v = (LinearLayout) findViewById(imageParentId);
                ((ViewManager)v.getParent()).removeView(v);
                finish();
            }
        });


        // Save button listener
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO: do the update
                ContentValues values = new ContentValues();
                Log.i("comment Up", comment.getText().toString());
                values.put(ImageContentProvider.comment, comment.getText().toString());
                int recUpdated = getContentResolver().update(ImageContentProvider.CONTENT_URI,
                        values, ImageContentProvider._ID + "=?", new String[] {data.getString("id")});
                finish();
            }
        });
    }

    private Bitmap setPic(String imageFile){

        // Get the dimensions of the View
        //TODO: define as arguments
        Display display = getWindowManager().getDefaultDisplay();
        int widthP = display.getWidth() - (2 * (int) getResources().getDimension(R.dimen.activity_margin));
        int dim = widthP;


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
}