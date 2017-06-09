package com.example.kimuratomoya.sensor_db;

/**
 * Created by kimuratomoya on 2017/04/28.
 */
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MyOpenHelper extends SQLiteOpenHelper {
    public MyOpenHelper(Context context) {
        super(context, "SensorDB", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("gps", "create1");
        db.execSQL("create table sensor(" + " created_at text not null,"
                + " acceleration_x real not null,"
                + " acceleration_y real not null,"
                + " acceleration_z real not null,"
                + " linear_acceleration_x real not null,"
                + " linear_acceleration_y real not null,"
                + " linear_acceleration_z real not null,"
                + " gyroscope_x real not null,"
                + " gyroscope_y real not null,"
                + " gyroscope_z real not null"
                + ");");

        Log.d("gps", "create2");
        db.execSQL("create table gps(" + " created_at integer not null,"
                + " latitude real not null,"
                + " longitude real not null"
                + ");");
        Log.d("gps", "create3");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}