package com.fermanis.volumebuddy;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class LocationList extends FragmentActivity {

    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_list);
        listView = (ListView) findViewById(R.id.list);


        // Create the Database
        LocationDbHelper locationDbHelper = new LocationDbHelper(getApplicationContext());

        SQLiteDatabase db = locationDbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("select * from locations", null);

        List<String> names = new ArrayList<>();
        if (cursor.moveToFirst()) {
            while(cursor.isAfterLast() == false) {
                names.add(cursor.getString(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_NAME_NAME)));
                cursor.moveToNext();
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, names);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // ListView Clicked item index
                int itemPosition     = position;

                // ListView Clicked item value
                String  itemValue    = (String) listView.getItemAtPosition(position);

                // Show Alert
                Toast.makeText(getApplicationContext(),
                        "Position :"+itemPosition+"  ListItem : " +itemValue , Toast.LENGTH_LONG)
                        .show();
            }
        });


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), LocateMe.class);
                startActivity(intent);
            }
        });
    }
}
