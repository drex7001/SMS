package com.personal.smsapp.ui;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Required to declare as a service for the default SMS app role.
 * Heads-up display is handled through the notification system instead.
 */
public class HeadsUpService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
