package com.example.myapplication;

/**
 * Second Activity
 * Presents the user with a form to either edit or delete an existing event
 * or create a new one
 *
 * Data for each event come from main activity
 * and are communicate between the two through intents
 *
 * User input is the sent back to main with
 * a flag which indicates what action must be taken on the data
 * ie. delete, insert, update or nothing
 */


import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class SecondActivity extends AppCompatActivity{

    // request flags coming from main
    private static final int NEW_EVENT_REQUEST = 0;
    public static final int EDIT_EVENT_REQUEST = 1;

    //deceleration of input fields
    private EditText title;
    private EditText dueDate;
    private EditText details;

    // extracted request value coming from main through intents
    private static int request;

    //id of an event, either comes with the intent data in case of editing,
    //or is initialized if it has to do with a new calendar event
    private static long createDate;
    private static int position;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        request = getIntent().getExtras().getInt("request");

        //get reference to input fields
        title = (EditText) findViewById(R.id.title_input);
        dueDate = (EditText) findViewById(R.id.due_date_input);
        details = (EditText) findViewById(R.id.details_input);

        //get reference to buttons
        Button save = (Button) findViewById(R.id.button_save);
        Button delete = (Button) findViewById(R.id.button_delete);
        Button cancel = (Button) findViewById(R.id.button_cancel);


        // in case of editing an already added event, extract its information
        // from the intent message coming from main activity
        if (request == EDIT_EVENT_REQUEST) {
            CalendarEvent event = (CalendarEvent) getIntent().getParcelableExtra("event");
            title.setText(event.getTitle());
            dueDate.setText(event.getDueDateFormatted());
            details.setText(event.getDetails());
            createDate = event.getCreateDate();
            position = getIntent().getExtras().getInt("position");

        }



        // Cancel button listener, if pressed return to main.
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                setResult(RESULT_CANCELED);
                finish();
            }
        });



        // Delete button listener
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // in case of a new event do nothing and return to main
                if (request == NEW_EVENT_REQUEST) {
                    setResult(RESULT_CANCELED);
                    finish();
                }

                // otherwise send to main a reply intent, which include: ,

                Intent reply = new Intent();

                //with the event's id
                reply.putExtra("event_id", createDate);

                //a flag indicating that it has to do with deletion of record
                reply.putExtra("action", "delete");

                // the position of the calendar event in recycler view
                reply.putExtra("position", position);

                // and a flag indicating that there are data to be returned
                setResult(RESULT_OK, reply);

                //back to main
                finish();
            }
        });



        // Save button listener
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent reply = new Intent();

                // in case of empty title and due date fields,
                // inform user to add values in those fields
                if (title.getText().toString().equals("") || dueDate.getText().toString().equals(""))
                {

                    Toast.makeText(SecondActivity.this, R.string.empty_fields,
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // Otherwise inputs are passed in reply intent
                CalendarEvent event = new CalendarEvent();
                event.setTitle(title.getText().toString());
                event.setDueDate(dueDate.getText().toString());
                event.setDetails(details.getText().toString());

                // in case of a new event s
                if (request == NEW_EVENT_REQUEST)
                {
                    // set the creation (which is also its id)
                    // to the current time
                    event.setCreateDate(System.currentTimeMillis());
                    reply.putExtra("event", event);;

                    // return to main with an ok signal
                    setResult(RESULT_OK, reply);
                    finish();
                }

                // in case of editing an existing one
                event.setCreateDate(createDate);
                reply.putExtra("event", event);

                // set its creation date to the one
                // passed from main
                reply.putExtra("action", "update");

                // return to main with an ok signal
                setResult(RESULT_OK, reply);
                finish();
            }
        });

    }

}