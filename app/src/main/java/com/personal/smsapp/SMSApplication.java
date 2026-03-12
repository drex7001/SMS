package com.personal.smsapp;

import android.app.Application;

import com.personal.smsapp.util.NotificationHelper;
import com.personal.smsapp.util.Prefs;
import com.personal.smsapp.data.local.SmsRepository;

public class SMSApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Set up notification channels (must do before posting any notifications)
        NotificationHelper.createChannels(this);

        // On first launch, sync existing messages from system SMS store
        if (Prefs.isFirstLaunch(this)) {
            SmsRepository.getInstance(this).syncFromSystem(null);
            Prefs.setFirstLaunchDone(this);
        }
    }
}
