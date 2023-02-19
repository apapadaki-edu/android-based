package com.example.myapplication;

/**
 * Main Activity
 * The user is presented with a list of all remainders that are later than the current date.
 * There is new remainder button and a delete button for bulk delete of check/ selected remainders.
 * The user can press on any remainder and is presented with a form to modify that remainder.
 *
 * List of remainders (EventListAdapter.java) -> implemented with a recycler view
 *  (there is an adapter that handles UI changes.
 *
 * Database handling (DBAdapter.java) -> database adapter that extends SQLiteOpenHelper class,
 * responsible for queries, opening and closing the database.
 *
 * The activity in which the user modifies or creates remainders (SecondActivity.java)
 *
 * Event/Remainder class (CalendarEvent.java) a class to represent the events for better access.
 * Not much happen in that file...
 *
 * !..Initially the database transactions were handled within threads. However, because Davey! appeared
 * on the logs whether threads were used or not, I chose not to use them.
 */




import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.security.KeyStore;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;


public class CalendarActivity extends AppCompatActivity
        implements EventListAdapter.OnEventClickListener {
    // flags for in between activities' communication
    private static final int NEW_EVENT_REQUEST = 0;
    public static final int EDIT_EVENT_REQUEST = 1;

    // adapters for database and recycler view
    private static DBAdapter db;
    private EventListAdapter adapter;

    // cache for checked calendar events
    private final HashMap<Integer, Long> checkedEvents = new HashMap<Integer, Long>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // button for adding a new event
        FloatingActionButton fab_new_event = (FloatingActionButton) findViewById(R.id.fab);
        fab_new_event.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                //creates a new intent with code that signals SecondActivity that a
                //new event is requested.
                Intent intent = new Intent(view.getContext(), SecondActivity.class);
                intent.putExtra("request", NEW_EVENT_REQUEST);


                //start activity with specified request message, this is used for passing
                // data between the two activity by using explicit intents
                startActivityForResult(intent, NEW_EVENT_REQUEST);
            }
        });



        //Button for event bulk deletion
        Button delete_many = (Button) findViewById(R.id.button_delete_from_main);
        delete_many.setOnClickListener(new View.OnClickListener()
        {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onClick(View view) {
                // loop over all checked events if any and delete them from the database
                if(checkedEvents.isEmpty()) return;

                Iterator it = checkedEvents.entrySet().iterator();
                while(it.hasNext()){
                    Map.Entry event = (Map.Entry) it.next();
                    try {
                        db.deleteEvent((long) event.getValue());
                        it.remove();
                    }catch (NullPointerException e){

                        Log.i("DELETE ITEM ERROR", e.getMessage());
                        return;
                    }
                }
                //inform recyclerView adapter of the data changes to update the UI
                adapter.notifyDataSetChanged();
            }

        });



        //initialize the database adapter and open the database with write privileges
        db = new DBAdapter(this);
        db.open();

        //get recycler view
        RecyclerView recycler = (RecyclerView) findViewById(R.id.display_events);

        //initialize its adapter passing as argument
        // the database adapter (retrieves data from the database for the UI)
        // a listener, EventListAdapter's(RecyclerView Adapter) custom child interface
        // for passing information between adapter and main activity
        adapter = new EventListAdapter(this, db, this);
        recycler.setAdapter(adapter);

        // android provided manager which is responsible for the arrangement of list items
        recycler.setLayoutManager(new LinearLayoutManager(this));

    }

    public void onDestroy() {
        super.onDestroy();
        //when activity is destroyed it makes sure the database is closed
        db.close();
    }

    //Method for handling the general and message communication between main and second activity
    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        //second activity's results requested from main activity
        //if main sent a new event request (add button pressed)
        //if all went well in second, user's input are send to main for insertion in the database
        switch (requestCode) {
            // user pressed new event button, and user's input was send from second to main
            case NEW_EVENT_REQUEST:

                //if all well in second
                if (resultCode == RESULT_OK)
                {
                    //store event data passed from second in a new CalendarEvent
                    // (custom class for better management)
                    CalendarEvent ev = data.getParcelableExtra("event");

                    //insert query to database through the db adapter
                    long l = db.insertEvent(ev.getCreateDate(), ev.getTitle(),
                            ev.getDueDate(), ev.getDetails());

                    //notify recyclerView adapter in order for the UI to be updated
                    adapter.notifyDataSetChanged();

                    break;
                }

            // user pressed on an event, and user's modified event data were send from second to main
            case EDIT_EVENT_REQUEST:

                //again if all went well in second
                if (resultCode == RESULT_OK)
                {
                    // in case of updating the event, perform the appropriate db query
                    if (data.getExtras().get("action").equals("update"))
                    {

                        int u = db.updateEvent(data.getParcelableExtra("event"));

                        //notify recyclerView adapter in order for UI to be updated
                        adapter.notifyDataSetChanged();

                        break;
                    }
                    // otherwise delete the event from db
                    db.deleteEvent(data.getExtras().getLong("event_id"));

                    // again notify recyclerView adapter
                    adapter.notifyDataSetChanged();
                    break;
                }
                break;
        }
    }

    // following two methods implement RecyclerView's custom child interface
    // this is the only way I've found to define actions on individual calendar events
    // from main activity. A short description follows for each.
    //Arguments in each method are passed through the event list adapter class
    // (for more check EventListAdapter.java)


    // defines what happens when an event is clicked
    //ie. second activity starts with edit calendar event request
    //data taken from a recyclerView event are send to second activity
    @Override
    public void onEventClick(CalendarEvent data, int position)
    {
        Intent intent = new Intent(this, SecondActivity.class);
        intent.putExtra("event", data);
        intent.putExtra("request", EDIT_EVENT_REQUEST);
        intent.putExtra("position", position);
        startActivityForResult(intent, EDIT_EVENT_REQUEST);
    }


    // observes each event's checkbox state.
    @Override
    public void isChecked(boolean checkState, long id, int position)
    {
        if(checkState)
        {
            // event checked add in cache its id
            checkedEvents.put(position, id);
        }
        else
        {
            //otherwise chop its id in cache
            checkedEvents.remove(position);
        }
    }

}
