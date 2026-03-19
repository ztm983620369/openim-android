package io.openim.android.ouicore.utils;

import android.app.AppOpsManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

public final class BackgroundStartPermissions {
    public static final BackgroundStartPermissions INSTANCE = new BackgroundStartPermissions();

    private BackgroundStartPermissions() {
    }

    private boolean isXiaoMi() {
        return checkManufacturer("xiaomi");
    }

    private boolean isOppo() {
        return checkManufacturer("oppo");
    }

    private boolean isVivo() {
        return checkManufacturer("vivo");
    }

    private boolean checkManufacturer(String manufacturer) {
        return manufacturer.equalsIgnoreCase(Build.MANUFACTURER);
    }

    public boolean isBackgroundStartAllowed(Context context) {
        if (isXiaoMi()) {
            return isXiaomiBgStartPermissionAllowed(context);
        }
        if (isVivo()) {
            return isVivoBgStartPermissionAllowed(context);
        }
        if (isOppo() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }

    private boolean isXiaomiBgStartPermissionAllowed(Context context) {
        AppOpsManager ops = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        try {
            int op = 10021;
            Object result = ops.getClass()
                .getMethod(
                    "checkOpNoThrow",
                    Integer.TYPE,
                    Integer.TYPE,
                    String.class
                )
                .invoke(ops, op, android.os.Process.myUid(), context.getPackageName());
            return result instanceof Integer && ((Integer) result) == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean isVivoBgStartPermissionAllowed(Context context) {
        return getVivoBgStartPermissionStatus(context) == 0;
    }

    private int getVivoBgStartPermissionStatus(Context context) {
        Uri uri =
            Uri.parse("content://com.vivo.permissionmanager.provider.permission/start_bg_activity");
        String selection = "pkgname = ?";
        String[] selectionArgs = new String[] {context.getPackageName()};
        int state = 1;
        try (android.database.Cursor cursor =
                 context.getContentResolver().query(uri, null, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                state = cursor.getInt(cursor.getColumnIndexOrThrow("currentstate"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return state;
    }
}
