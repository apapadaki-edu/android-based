package com.example.myapplication;

/**
 * Calendar Event Class
 *
 * Event/Remainder class (CalendarEvent.java) a class to represent the events for better access.
 *
 * date class fields are in millis from epoch
 *
 * methods are provided for returning them formatted
 * or setting them to millis, in case they are given as strings
 *
 * this class implements the parsable interface,
 * it is required for a class to implement it
 * if it is for its objects to be passed in bundles
 *
 */

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Objects;


public class CalendarEvent implements Parcelable {
    private String title;
    private long dueDate;
    private long createDate;
    private String details;

    public CalendarEvent(){}


    // getters
    public String getTitle() {return this.title;}
    public long getDueDate() {return this.dueDate;}
    public long getCreateDate() { return this.createDate;}
    public String getDetails() {return this.details;}

    // setters
    public void setTitle(String title){this.title = title;}
    public void setDueDate(long dueDate){this.dueDate = dueDate;}
    public void setCreateDate(long createDate) {this.createDate = createDate; }
    public void setDetails(String details) {this.details = details;}

    // getters for getting the dates formatted
    public String getDueDateFormatted(){
        try {
            Date date = new Date(this.dueDate);
            String s = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(date);
            return s;
        } catch (NullPointerException ex) {
            Log.e("CalendarActivity", ex.getMessage());
            return null;
        }
    }
    public String getCreateDateFormatted(){
        try {
            Date date = new Date(this.createDate);
            String s = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(date);
            return s;
        }catch (NullPointerException ex) {
            Log.e("CalendarActivity", ex.getMessage());
            return null;
        }
    }

    //setters in case dates are given in strings
    public void setDueDate (@NonNull String dueDate) {
        SimpleDateFormat pattern = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        try {
            if (dueDate == null){
                return;
            }
            String dateInput = dueDate;
            if(dueDate.matches("\\d{1,2}/\\d{1,2}/\\d{4}"))
            {
                dateInput += " 00:00:00";
            }

            Date date = pattern.parse(dateInput);
            this.dueDate = date.getTime();

        } catch (ParseException e) {
            Log.e("CalendarActivity", e.getMessage());
            e.printStackTrace();
        }
    }
    public void setCreateDate (@NonNull String createDate) {
        SimpleDateFormat pattern = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        try {
            if (createDate == null){
                return;
            }
            Date date = pattern.parse(createDate);
            this.createDate = date.getTime();

        } catch (ParseException e) {
            Log.e("CalendarActivity", e.getMessage());
            e.printStackTrace();
        }
    }


    // parsable methods implementation

    // describes the kinds of special objects included in parsable
    @Override
    public int describeContents() {
        return 0;
    }

    // flattens object to parcel (careful for the order of the fields
    // the fields must be in the same order in writeToParcel and
    // out (CalendarEvent(Parcel) method implemented later on
    @Override
    public void writeToParcel(Parcel out, int i) {
        out.writeString(title);
        out.writeLong(dueDate);
        out.writeLong(createDate);
        out.writeString(details);
    }

    // generates instances of this class from a parcel
    // a container for data and object references
    // that are flattened in and unflattened out
    public static final Parcelable.Creator<CalendarEvent> CREATOR
            = new Parcelable.Creator<CalendarEvent>()
    {
        //new instance of Parcelable class, given the data passed in from writeToParcel
        public CalendarEvent createFromParcel(Parcel in)
        {
            return new CalendarEvent(in);
        }

        //creates array of the Parcelable class
        public CalendarEvent[] newArray(int size)
        {
            return new CalendarEvent[size];
        }
    };

    // unflattened object
    private CalendarEvent(Parcel in) {
        title = in.readString();
        dueDate = in.readLong();
        createDate = in.readLong();
        details = in.readString();
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CalendarEvent that = (CalendarEvent) o;
        return this.createDate == that.createDate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, dueDate, createDate, details);
    }
}

// More flexible way of managing dates
/* For java ver >= 8
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void setDateToMillis(String date, boolean isDueTime){
        LocalDateTime time = LocalDateTime.parse(date,
                DateTimeFormatter.ofPattern("EEE, dd/MM/yyyy"));
        long millis = time.atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli();
        if (isDueTime){
            dueDate = millis;
            return;
        }
        createDate = millis;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public String getDateFromMillis(boolean isDueTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd/MM/yyyy");
        Calendar calendar = Calendar.getInstance();
        if (isDueTime) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(dueDate),
                    ZoneId.systemDefault()).format(formatter);
        }

        return LocalDateTime.ofInstant(Instant.ofEpochMilli(createDate)
                ,ZoneId.systemDefault()).format(formatter);
    }

 */
