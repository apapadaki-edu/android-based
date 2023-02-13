package com.example.myapplication;

/**
 * Database adapter class
 *
 * It implements autocloseable in use with try with resources
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
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.LinkedList;



public class DBAdapter  implements AutoCloseable{

    private static final String DB_NAME = "calendar_db";
    private static final String DB_TABLE = "events";
    private static final int DB_VER = 1;

    // query to create db table for our events
    private static final String DB_CREATE =
            "CREATE TABLE events (id_create_date INTEGER PRIMARY KEY, " +
                    "title TEXT NOT NULL, due_date INTEGER NOT NULL, details TEXT);";


    // subclass that helps interact with sqlite database object
    private static class DBHelper extends SQLiteOpenHelper {

        //constructor with the db name and version
        DBHelper (Context c) {
            super(c, DB_NAME, null, DB_VER);
        }

        //creates a db if does not already exist
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DB_CREATE);
        }

        //deletes the table if it exists and recreates it with the new db version
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVer, int newVer) {
            db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE);
            onCreate(db);
        }

    }

    private final Context context;
    private DBHelper helper;
    private SQLiteDatabase db;


    // table fields
    private final String TITLE = "title";
    private final String DETAILS = "details";
    private final String ID_CREATE_DATE = "id_create_date";
    private final String DUE_DATE = "due_date";


    // creates adapter object
    public DBAdapter (Context c) {
        this.context = c;
    }

    // creates helper object and a reference to a writable database
    public DBAdapter open() throws SQLException {
        helper = new DBHelper(context);
        db = helper.getWritableDatabase();
        return this;
    }

    // close db with helper
    public void close() {
        helper.close();
    }


    // function that retrieves all events
    @SuppressLint("Range")
    public LinkedList<CalendarEvent> retrieveAllEvents (){
        LinkedList<CalendarEvent> allEvents = new LinkedList<CalendarEvent>();

        Cursor cursor = db.rawQuery("SELECT * FROM " + DB_TABLE + " ORDER BY "
                + ID_CREATE_DATE
                + " DESC;", null);

        if(cursor != null  && cursor.getCount() >= 1){
            CalendarEvent e = new CalendarEvent();
            cursor.moveToFirst();
           do{
                e.setCreateDate(cursor.getLong(cursor.getColumnIndex(ID_CREATE_DATE)));
                e.setTitle(cursor.getString(cursor.getColumnIndex(TITLE)));
                e.setDueDate(cursor.getLong(cursor.getColumnIndex(DUE_DATE)));
                e.setDetails(cursor.getString(cursor.getColumnIndex(DETAILS)));
                allEvents.addFirst(e);
            } while (cursor.moveToNext());

           return allEvents;
        }
        return null;
    }


    // adds new event in db with insert method
    public long insertEvent(long createDate, String title, long dueDate, String details){
        ContentValues vals = new ContentValues();
        vals.put("id_create_date", createDate);
        vals.put("title", title);
        vals.put("due_date", dueDate);
        vals.put("details", details);
        return (long) db.insert(DB_TABLE, null, vals);
    }


    // deletes it with the help of delete method query
    public int deleteEvent(long id){
        int delete = db.delete(DB_TABLE,
                ID_CREATE_DATE + " = ? ",
                new String[]{String.valueOf(id)});
        return delete;
    }


    public int updateEvent(CalendarEvent event){
        int updated = -1;
        ContentValues vals = new ContentValues();
        vals.put("title", event.getTitle());
        vals.put("due_date", event.getDueDate());
        vals.put("details", event.getDetails());

        return db.update(DB_TABLE,
                    vals,
                    ID_CREATE_DATE + " = ?",
                    new String[] {String.valueOf(event.getCreateDate())});
    }


    //query to retrieve event with due date past the current one
    @SuppressLint("Range")
    public CalendarEvent select(int position) {

        String query = "SELECT * FROM " + DB_TABLE +
                " WHERE " + DUE_DATE + " > " + System.currentTimeMillis() +
                " ORDER BY " + DUE_DATE + " ASC " +
                "LIMIT " + position + ",1";
        Cursor cursor = null;
        CalendarEvent entry = new CalendarEvent();

        try {
            if (db == null) {
                db = helper.getReadableDatabase();
            }
            cursor = db.rawQuery(query, null);
            cursor.moveToFirst();
            entry.setCreateDate(cursor.getLong(cursor.getColumnIndex(ID_CREATE_DATE)));
            entry.setTitle(cursor.getString(cursor.getColumnIndex(TITLE)));
            entry.setDueDate(cursor.getLong(cursor.getColumnIndex(DUE_DATE)));
            entry.setDetails(cursor.getString(cursor.getColumnIndex(DETAILS)));
        } catch (Exception e) {
            Log.d("SecondActivity", "QUERY EXCEPTION! " + e.getMessage());
        } finally {
            cursor.close();
            return entry;
        }
    }

    public long count(){
        return DatabaseUtils.queryNumEntries(db, DB_TABLE, DUE_DATE + " >= ?",
                new String[] {String.valueOf(System.currentTimeMillis())});
    }


    /*
    TODO: later create the method to retrieve by filter
    public CalendarEvent retrieveEventByCriteria(String title, int month, String details) {
        CalendarEvent e = new CalendarEvent();

        Cursor cursor = db.query(DB_TABLE,
                new String[] {ID_CREATE_DATE, TITLE, DUE_DATE, DETAILS},
                TITLE + " LIKE %?% OR " + DETAILS + " LIKE %?% OR DUE_DATE >= ?;" );
    }
 */


}
