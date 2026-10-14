package com.abhayshah.challengealarm;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

final class UpdateManager {
    private static final String MANIFEST = "https://raw.githubusercontent.com/AbhayShah06/ChallengeAlarm/main/update.json";
    private UpdateManager() {}

    static void attach(Activity activity, ViewGroup parent) {
        cleanupInstallers(activity);
        new Thread(() -> check(activity, parent), "update-check").start();
    }

    private static void check(Activity activity, ViewGroup parent) {
        try {
            JSONObject json = new JSONObject(readUrl(MANIFEST));
            int current = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionCode;
            if (json.getInt("versionCode") <= current) return;
            activity.runOnUiThread(() -> showBanner(activity, parent, json));
        } catch (Exception ignored) { }
    }

    private static void showBanner(Activity activity, ViewGroup parent, JSONObject json) {
        LinearLayout banner = new LinearLayout(activity); banner.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(14 * activity.getResources().getDisplayMetrics().density); banner.setPadding(pad, pad, pad, pad); banner.setBackgroundColor(Color.rgb(32, 58, 49));
        TextView message = new TextView(activity); message.setTextColor(Color.WHITE); message.setText("Update " + json.optString("versionName") + " is ready\n" + json.optString("notes", "Bug fixes and improvements")); banner.addView(message);
        Button update = new Button(activity); update.setText("Update now"); banner.addView(update); parent.addView(banner, 0);
        update.setOnClickListener(v -> beginInstall(activity, update, json));
    }

    private static void beginInstall(Activity activity, Button button, JSONObject json) {
        if (!activity.getPackageManager().canRequestPackageInstalls()) {
            activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + activity.getPackageName())));
            Toast.makeText(activity, "Allow installs, then tap Update now again.", Toast.LENGTH_LONG).show(); return;
        }
        button.setEnabled(false); button.setText("Downloading…");
        new Thread(() -> {
            try {
                File file = new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "pending-update.apk");
                download(json.getString("apkUrl"), file);
                if (!sha256(file).equalsIgnoreCase(json.getString("sha256"))) throw new SecurityException("Update checksum mismatch");
                activity.runOnUiThread(() -> launchInstaller(activity, file));
            } catch (Exception error) {
                activity.runOnUiThread(() -> { button.setEnabled(true); button.setText("Retry update"); Toast.makeText(activity, "Update download failed. Try again.", Toast.LENGTH_LONG).show(); });
            }
        }, "update-download").start();
    }

    private static void launchInstaller(Activity activity, File file) {
        Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".updates", file);
        Intent install = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(install);
    }

    private static String readUrl(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(url).openConnection(); c.setConnectTimeout(5000); c.setReadTimeout(5000); c.setRequestProperty("User-Agent", "ChallengeAlarm-Updater");
        try (java.io.InputStream in = c.getInputStream(); java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) { byte[] b = new byte[4096]; int n; while ((n = in.read(b)) > 0) out.write(b, 0, n); return out.toString("UTF-8"); } finally { c.disconnect(); }
    }

    private static void download(String url, File target) throws Exception {
        if (!url.startsWith("https://github.com/AbhayShah06/ChallengeAlarm/")) throw new SecurityException("Unexpected update host");
        HttpURLConnection c = (HttpURLConnection)new URL(url).openConnection(); c.setInstanceFollowRedirects(true); c.setConnectTimeout(10000); c.setReadTimeout(30000);
        try (java.io.InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(target)) { byte[] b = new byte[8192]; int n; while ((n = in.read(b)) > 0) out.write(b, 0, n); } finally { c.disconnect(); }
    }

    private static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256"); byte[] buffer = new byte[8192]; int read;
        try (FileInputStream in = new FileInputStream(file)) { while ((read = in.read(buffer)) > 0) digest.update(buffer, 0, read); }
        StringBuilder hex = new StringBuilder(); for (byte b : digest.digest()) hex.append(String.format("%02x", b)); return hex.toString();
    }

    private static void cleanupInstallers(Activity activity) {
        File dir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS); if (dir == null) return;
        File pending = new File(dir, "pending-update.apk"); if (pending.exists()) pending.delete();
    }
}
