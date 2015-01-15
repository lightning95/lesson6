package ru.ifmo.md.lesson6;

import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.util.HashMap;
import java.util.Map;

import ru.ifmo.md.lesson6.DbContract.Channels;
import ru.ifmo.md.lesson6.DbContract.ChannelsColumns;
import ru.ifmo.md.lesson6.DbContract.PostsColumns;

/**
 * Created by lightning95 on 12/20/14.
 */

public class DbHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "RSS_DataBase.db";
    private static final int VERSION = 1;
    private static final HashMap<String, String> basicChannel = new HashMap<String, String>();

    static {
        basicChannel.put("http://bash.im/rss/", "Bash.im");
    }

    interface Tables {
        String CHANNELS = "channels";
        String POSTS = "posts";
    }

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.CHANNELS + " ("
                        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + ChannelsColumns.CHANNEL_LINK + " TEXT NOT NULL,"
                        + ChannelsColumns.CHANNEL_TITLE + " TEXT NOT NULL);"
        );

        db.execSQL("CREATE TABLE " + Tables.POSTS + " ("
                        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + PostsColumns.POST_LINK + " TEXT NOT NULL,"
                        + PostsColumns.POST_TITLE + " TEXT NOT NULL,"
                        + PostsColumns.POST_DESCRIPTION + " TEXT,"
                        + PostsColumns.POST_CHANNEL + " INTEGER NOT NULL,"
                        + "FOREIGN KEY(" + PostsColumns.POST_CHANNEL + ") REFERENCES " +
                        Tables.CHANNELS + "(" + BaseColumns._ID + ") ON DELETE CASCADE);"
        );

        for (Map.Entry<String, String> entry : basicChannel.entrySet()) {
            db.execSQL("INSERT INTO " + Tables.CHANNELS + "("
                            + Channels.CHANNEL_LINK + ", " + Channels.CHANNEL_TITLE + ") "
                            + "VALUES (" + DatabaseUtils.sqlEscapeString(entry.getKey())
                            + ", " + DatabaseUtils.sqlEscapeString(entry.getValue()) + ");"
            );
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        db.execSQL("PRAGMA foreign_keys=ON;");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i2) {
    }
}