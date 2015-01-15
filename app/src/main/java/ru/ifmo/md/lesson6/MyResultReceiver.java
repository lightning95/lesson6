package ru.ifmo.md.lesson6;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

/**
 * Created by lightning95 on 12/20/14.
 */

public class MyResultReceiver extends ResultReceiver {
    private Receiver receiver;

    public interface Receiver {
        public void onReceiveResult(int resultCode, Bundle data);
    }

    public MyResultReceiver(Handler handler) {
        super(handler);
    }

    public void setReceiver(Receiver receiver) {
        this.receiver = receiver;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle data) {
        if (receiver != null) {
            receiver.onReceiveResult(resultCode, data);
        }
    }
}
