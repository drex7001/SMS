package com.personal.smsapp.util;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.personal.smsapp.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Checks GitHub Releases for a newer build and prompts the user to install it.
 *
 * Flow:
 *   1. GET /repos/drex7001/SMS/releases/latest  (background thread, OkHttp)
 *   2. Parse tag_name → remoteVersionCode; compare with BuildConfig.VERSION_CODE
 *   3. If newer: show AlertDialog on UI thread
 *   4. On confirm: DownloadManager downloads the APK asset to the app's external
 *      Downloads dir; a one-shot BroadcastReceiver fires the system installer via
 *      a FileProvider content URI when the download completes.
 */
public final class UpdateChecker {

    private static final String RELEASES_API =
            "https://api.github.com/repos/drex7001/SMS/releases/latest";

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

    private UpdateChecker() {}

    /** Call once from your launcher Activity's onCreate(). Silent on any failure. */
    public static void checkForUpdate(Activity activity) {
        Request request = new Request.Builder()
                .url(RELEASES_API)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .build();

        HTTP_CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Best-effort: ignore network errors silently
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) return;

                    JSONObject json     = new JSONObject(body.string());
                    String     tagName  = json.getString("tag_name");           // e.g. "v42"
                    int remoteVersion   = Integer.parseInt(tagName.replaceAll("[^0-9]", ""));

                    if (remoteVersion <= BuildConfig.VERSION_CODE) return;

                    JSONArray assets = json.getJSONArray("assets");
                    if (assets.length() == 0) return;

                    String downloadUrl = assets.getJSONObject(0)
                                               .getString("browser_download_url");

                    activity.runOnUiThread(() ->
                            showUpdateDialog(activity, remoteVersion, downloadUrl));

                } catch (Exception e) {
                    // Malformed response — ignore silently
                }
            }
        });
    }

    // ── UI ───────────────────────────────────────────────────────────────────

    private static void showUpdateDialog(Activity activity, int remoteVersion, String url) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        new AlertDialog.Builder(activity)
                .setTitle("Update available")
                .setMessage("Version " + remoteVersion + " is available. Download and install now?")
                .setPositiveButton("Download & Install", (d, w) -> startDownload(activity, url))
                .setNegativeButton("Later", null)
                .show();
    }

    // ── Download & install ───────────────────────────────────────────────────

    private static void startDownload(Activity activity, String downloadUrl) {
        // Use application context so the receiver isn't tied to the Activity lifecycle.
        Context appCtx = activity.getApplicationContext();

        // Destination: app-private external Downloads dir — no permission required.
        File destFile = new File(
                activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "sms-update.apk");
        if (destFile.exists()) destFile.delete();

        DownloadManager dm =
                (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);

        DownloadManager.Request req = new DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle("SMS App Update")
                .setDescription("Downloading update\u2026")
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(
                        activity, Environment.DIRECTORY_DOWNLOADS, "sms-update.apk");

        long downloadId = dm.enqueue(req);
        Toast.makeText(activity, "Downloading update\u2026", Toast.LENGTH_SHORT).show();

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                long completedId =
                        intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (completedId != downloadId) return;

                try {
                    appCtx.unregisterReceiver(this);
                } catch (IllegalArgumentException ignored) {}

                if (!destFile.exists()) return;

                // On API 26+, REQUEST_INSTALL_PACKAGES is an appop that must be
                // explicitly granted by the user in Settings — the manifest declaration
                // alone is not sufficient. Without it, startActivity throws a
                // SecurityException or ActivityNotFoundException and crashes the app.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        && !appCtx.getPackageManager().canRequestPackageInstalls()) {
                    Toast.makeText(appCtx,
                            "Allow \"Install unknown apps\" for this app, then re-download.",
                            Toast.LENGTH_LONG).show();
                    Intent settings = new Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:" + appCtx.getPackageName()))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        appCtx.startActivity(settings);
                    } catch (Exception ignored) {}
                    return;
                }

                Uri apkUri = FileProvider.getUriForFile(
                        appCtx,
                        appCtx.getPackageName() + ".fileprovider",
                        destFile);

                Intent install = new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(apkUri, "application/vnd.android.package-archive")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    appCtx.startActivity(install);
                } catch (Exception e) {
                    Toast.makeText(appCtx, "Could not launch installer: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }
        };

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+: must explicitly state exported; EXPORTED because DownloadManager
            // (system process) sends this broadcast.
            appCtx.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            appCtx.registerReceiver(receiver, filter);
        }
    }
}
