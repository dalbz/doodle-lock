package edu.osu.cse.doodleLock.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseOpenHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "DoodleLock";
    private static final String USERS_TABLE = "users";
    private static final String USERNAME = "name";
    private static final String ID = "id";
    private static final String DOODLE_LOC = "doodle";
    private static final String CREATE_USERS_TABLE = "CREATE TABLE "
            + USERS_TABLE + " ("
            + ID + " integer primary key autoincrement, "
            + USERNAME + " text not null, "
            + DOODLE_LOC + " text not null"
            + ");";

    DatabaseOpenHelper(Context context) {
        super(context, DatabaseOpenHelper.DATABASE_NAME, null,
                DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DatabaseOpenHelper.CREATE_USERS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + USERS_TABLE);
        this.onCreate(db);
    }
}
