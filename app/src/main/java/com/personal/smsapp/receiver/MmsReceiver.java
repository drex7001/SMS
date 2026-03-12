package com.personal.smsapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Required by Android to be registered as the default SMS app.
 * Basic MMS receive handling — stores via system MMS stack.
 */
public class MmsReceiver extends BroadcastReceiver {

    private static final String TAG = "MmsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // The system MMS stack handles downloading automatically.
        // Here we just log and could trigger a notification / refresh.
        Log.d(TAG, "MMS received (handled by system stack)");
        // TODO: query MMS content provider and show notification if desired
    }
}
