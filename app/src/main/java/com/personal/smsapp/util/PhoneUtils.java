package com.personal.smsapp.util;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class PhoneUtils {

    public static boolean isDefaultSmsApp(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager rm = ctx.getSystemService(RoleManager.class);
            return rm != null && rm.isRoleHeld(RoleManager.ROLE_SMS);
        }
        String def = Telephony.Sms.getDefaultSmsPackage(ctx);
        return ctx.getPackageName().equals(def);
    }

    public static void requestDefaultSmsApp(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager rm = activity.getSystemService(RoleManager.class);
            if (rm != null && !rm.isRoleHeld(RoleManager.ROLE_SMS)) {
                Intent intent = rm.createRequestRoleIntent(RoleManager.ROLE_SMS);
                activity.startActivityForResult(intent, requestCode);
            }
        } else {
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
                activity.getPackageName());
            activity.startActivityForResult(intent, requestCode);
        }
    }

    /**
     * Formats timestamp as:
     *   - Today    → "14:32"
     *   - This week → "Mon"
     *   - Older    → "12/05/24"
     */
    public static String formatTimestamp(long millis) {
        Calendar now  = Calendar.getInstance();
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(millis);

        if (now.get(Calendar.DATE)  == time.get(Calendar.DATE)
         && now.get(Calendar.MONTH) == time.get(Calendar.MONTH)
         && now.get(Calendar.YEAR)  == time.get(Calendar.YEAR)) {
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(millis));
        }

        // Same week
        long diff = now.getTimeInMillis() - millis;
        if (diff < 7L * 24 * 60 * 60 * 1000) {
            return new SimpleDateFormat("EEE", Locale.getDefault()).format(new Date(millis));
        }

        return new SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(new Date(millis));
    }

    public static String formatFullTimestamp(long millis) {
        return new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            .format(new Date(millis));
    }

    /** Normalize number to just digits for basic deduplication */
    public static String normalizeNumber(String number) {
        if (number == null) return "";
        return number.replaceAll("[^0-9+]", "");
    }
}
