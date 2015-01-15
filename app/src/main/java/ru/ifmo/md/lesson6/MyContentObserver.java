package ru.ifmo.md.lesson6;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

/**
 * Created by lightning95 on 12/20/14.
 */

public class MyContentObserver extends ContentObserver {
    Handler handler;
    Callbacks callbacks;

    public interface Callbacks {
        public void onChannelsObserverFired();
    }

    public MyContentObserver(Callbacks callback) {
        super(null);
        handler = new Handler();
        callbacks = callback;
    }

    @Override
    public void onChange(boolean selfChange) {
        callbacks.onChannelsObserverFired();
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        onChange(selfChange);
    }
}
