package com.example.myapplication;

/**
 * RecyclerView adapter class
 *
 * Reason for choosing it is because it can update the UI
 * if a change in the dataset or an individual item is detected
 * ex by calling adapter(this).notifyDataSetChanged()
 *
 * How it works:
 * The adapter accesses data from a data source for example a database/ array,
 * connects the data to the recycler view by using a view holder.
 * The view holder contains the view information for one item.
 *
 * So data pass from the adapter -> through a view holder -> to the layout manager
 * that arranges them in the recycler view.
 *
 * This adapter also contains an interface (OnEventClickListener)
 * that helps the main activity to access custom defined functionalities
 * of the items handled by the adapter and view holder.
 *
 */

import android.annotation.SuppressLint;
import android.content.Context;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


public class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.EventViewHolder>{
    private final LayoutInflater inflater;
    private final DBAdapter db;
    private final Context context;
    private OnEventClickListener listener;


    // Adapter constructor, initializes class fields such as the database adapter used for
    // accessing the db records, a layoutInflater that creates the view for one item from an
    // external xml file. It also passes a reference from itself to the interface when
    // instantiated in main activity.
    public EventListAdapter(Context context, DBAdapter db, OnEventClickListener listener){
        this.inflater = LayoutInflater.from(context);
        this.context = context;
        this.db = db;
        this.listener = listener;
    }


    // view holder class manages layout and information of one item
    // it extends the View.OnClickListener to customize its onclick method for each item
    class EventViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener

    {
        //reference to the view of an item (here a linear layout containing textViews and
        //a checkbox)
        public final LinearLayout eventItemView;

        //reference to this adapter
        final EventListAdapter adapter;

        public EventViewHolder(View eventView, EventListAdapter adapter){
            super(eventView);
            this.eventItemView = (LinearLayout) eventView.findViewById(R.id.event);
            this.adapter = adapter;

            //attaches the implemented click method to the item view
            eventItemView.setOnClickListener(this);


            CheckBox cb = (CheckBox) this.eventItemView.getChildAt(4);
            cb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(((CompoundButton) view).isChecked()){
                        cb.setSelected(true);
                    } else {
                        cb.setSelected(false);
                    }
                }
            });
        }

        // custom method passes in the interfaces isChecked method
        // the state of the current views checkbox and the records id.
        // of the item.
        public void onCheck(View view, long id, int position)
        {
            CheckBox viewC = (CheckBox) view;
            listener.isChecked(viewC.isChecked(), id, position);
        }

        // gets current event's info when clicked.
        // Then passes it onto interface's onEventClick method.
        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void onClick(View view)
        {
            int position = getLayoutPosition();
            listener.onEventClick((CalendarEvent) view.getTag(), position);
        }

    }



    //creates a view from the xml file and serves it to the view holder
    @NonNull
    @Override
    public EventListAdapter.EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View eventItemView = inflater.inflate(R.layout.remainder_layout, parent, false);
        return new EventViewHolder(eventItemView, this);
    }



    // binds the item view with the information in the dataset
    @Override
    public void onBindViewHolder(@NonNull EventListAdapter.EventViewHolder holder, int position)
    {
        // event returned from select query
        CalendarEvent curEvent = db.select(position);

        // set item view's id to record's position in the dataset
        holder.eventItemView.setId(position);

        // if a record was found and retrieved, then set record to item vies children
        if (curEvent != null)
        {

            TextView title = (TextView) holder.eventItemView.getChildAt(0);
            title.setText(curEvent.getTitle());

            TextView createDate = (TextView) holder.eventItemView.getChildAt(1);
            createDate.setText(curEvent.getCreateDateFormatted());

            TextView dueDate = (TextView) holder.eventItemView.getChildAt(2);
            dueDate.setText(curEvent.getDueDateFormatted());

            TextView details = (TextView) holder.eventItemView.getChildAt(3);
            details.setText(String.valueOf(curEvent.getDetails()));


            CheckBox checkBox = (CheckBox) holder.eventItemView.getChildAt(4);
            checkBox.setChecked(false); // first uncheck to cancel previous state

            // bind holder's onCheck method to each view's checkbox
            checkBox.setOnClickListener((view)-> holder.onCheck(view, curEvent.getCreateDate(), position));

            // set item view's tag to the record, as it was retrieved from the database
            holder.eventItemView.setTag(curEvent);
        }
     }

     // returns the number of records returned by the select query
    @Override
    public int getItemCount() {
        return (int) db.count();
    }


    //child interface which implementation has already been discussed
    public interface OnEventClickListener
    {
        void onEventClick(CalendarEvent data, int position);
        void isChecked(boolean checkState, long id, int position);
    }


}
