package org.omnione.did.sdk.mdoc.proximity.reader.core;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import java.lang.ref.WeakReference;

public class PlatformController {
    private WeakReference<Activity> activityRef;
    private final String appVersion;
    private final String buildType;
    private final String flavor;

    public PlatformController(String appVersion, String buildType, String flavor) {
        this.appVersion = appVersion;
        this.buildType = buildType;
        this.flavor = flavor;
    }

    public void registerActivity(Activity activity) {
        this.activityRef = new WeakReference<>(activity);
    }

    public void closeApp() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.finish();
        }
    }

    public void openAppSettings() {
        Activity activity = getActivity();
        if (activity != null) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", activity.getPackageName(), null));
            activity.startActivity(intent);
        }
    }

    public Activity getActivity() {
        return activityRef != null ? activityRef.get() : null;
    }

    public String getAppVersion() { return appVersion; }
    public String getBuildType() { return buildType; }
    public String getFlavor() { return flavor; }

    public boolean isDebug() {
        return "debug".equalsIgnoreCase(buildType);
    }
}
