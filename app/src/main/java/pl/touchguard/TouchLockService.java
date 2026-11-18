package pl.touchguard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;

/**
 * A transparent, touch-consuming window above normal apps. This does not change
 * hardware-key handling or block Android's system-level navigation/gestures.
 * The overlay is removed on stop or process death, preventing permanent lockout.
 */
public final class TouchLockService extends Service {
    public static final String ACTION_LOCK = "pl.touchguard.action.LOCK";
    public static final String ACTION_UNLOCK = "pl.touchguard.action.UNLOCK";
    private static final String CHANNEL_ID = "touchguard_active";
    private static final int NOTIFICATION_ID = 1201;
    private static final long HOLD_TO_UNLOCK_MS = 3000;
    private static volatile boolean locked = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private FrameLayout overlay;
    private TextView lockBadge;
    private long deadlineElapsed;
    private boolean foregroundStarted;

    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            if (overlay == null) return;
            long leftMs = deadlineElapsed - SystemClock.elapsedRealtime();
            if (leftMs <= 0) {
                unlockAndStop();
                return;
            }
            long secs = (leftMs + 999) / 1000;
            lockBadge.setText("🔒  " + String.format(java.util.Locale.getDefault(),
                    "%d:%02d", secs / 60, secs % 60) + "\nPrzytrzymaj 3 s");
            handler.postDelayed(this, 250);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_UNLOCK : intent.getAction();
        if (ACTION_UNLOCK.equals(action)) {
            unlockAndStop();
            return START_NOT_STICKY;
        }
        if (!ACTION_LOCK.equals(action) || overlay != null || !Settings.canDrawOverlays(this)) {
            if (overlay == null) stopSelf();
            return START_NOT_STICKY;
        }

        try {
            createNotificationChannel();
            Notification notification = buildNotification();
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
            foregroundStarted = true;
            showTouchOverlay();
            deadlineElapsed = SystemClock.elapsedRealtime() + Prefs.getSeconds(this) * 1000L;
            locked = true;
            TouchTileService.refresh(this);
            handler.post(clockTick);
        } catch (Exception error) {
            android.util.Log.e("TouchGuard", "Unable to start touch overlay", error);
            unlockAndStop();
        }
        return START_NOT_STICKY;
    }

    private void showTouchOverlay() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        overlay = new FrameLayout(this);
        overlay.setClickable(true);
        overlay.setOnTouchListener((view, event) -> true);

        lockBadge = new TextView(this);
        lockBadge.setTextColor(Color.WHITE);
        lockBadge.setTextSize(11);
        lockBadge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        lockBadge.setGravity(Gravity.CENTER);
        lockBadge.setPadding(dp(14), dp(8), dp(14), dp(8));
        GradientDrawable badgeBackground = new GradientDrawable();
        badgeBackground.setColor(0xE21B4248);
        badgeBackground.setCornerRadius(dp(28));
        badgeBackground.setStroke(dp(1), 0x70FFFFFF);
        lockBadge.setBackground(badgeBackground);
        lockBadge.setContentDescription("Przytrzymaj przez 3 sekundy, aby wyłączyć blokadę dotyku");
        installHoldToUnlock(lockBadge);

        FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
        badgeParams.setMargins(dp(12), dp(12), dp(12), dp(12));
        overlay.addView(lockBadge, badgeParams);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.setTitle("TouchGuard: temporary touch blocker");
        // Do not use FLAG_LAYOUT_NO_LIMITS or immersive mode; system UI stays usable.
        windowManager.addView(overlay, params);
    }

    private void installHoldToUnlock(TextView badge) {
        final Runnable unlockRunnable = this::unlockAndStop;
        final float touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        final float[] downXY = new float[2];
        badge.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downXY[0] = event.getX();
                    downXY[1] = event.getY();
                    badge.setAlpha(0.65f);
                    handler.postDelayed(unlockRunnable, HOLD_TO_UNLOCK_MS);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(event.getX() - downXY[0]) > touchSlop ||
                            Math.abs(event.getY() - downXY[1]) > touchSlop) {
                        handler.removeCallbacks(unlockRunnable);
                        badge.setAlpha(1f);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    handler.removeCallbacks(unlockRunnable);
                    badge.setAlpha(1f);
                    return true;
                default:
                    return true;
            }
        });
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void createNotificationChannel() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Aktywna blokada dotyku", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Powiadomienie z przyciskiem awaryjnego odblokowania");
        manager.createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        Intent unlockIntent = new Intent(this, TouchLockService.class).setAction(ACTION_UNLOCK);
        PendingIntent unlock = PendingIntent.getService(this, 120, unlockIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent open = PendingIntent.getActivity(this, 121, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Action action = new Notification.Action.Builder(
                R.drawable.ic_lock_mono, "Odblokuj", unlock).build();
        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_lock_mono)
                .setContentTitle("TouchGuard – dotyk zablokowany")
                .setContentText("Automatyczne odblokowanie: " +
                        Prefs.formatDuration(Prefs.getSeconds(this)))
                .setContentIntent(open)
                .addAction(action)
                .setOngoing(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    private void unlockAndStop() {
        handler.removeCallbacksAndMessages(null);
        if (overlay != null) {
            try {
                windowManager.removeViewImmediate(overlay);
            } catch (Exception ignored) { }
            overlay = null;
            lockBadge = null;
        }
        locked = false;
        TouchTileService.refresh(this);
        if (foregroundStarted) {
            stopForeground(STOP_FOREGROUND_REMOVE);
            foregroundStarted = false;
        }
        stopSelf();
    }

    public static boolean isLocked() { return locked; }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (overlay != null) {
            try { windowManager.removeViewImmediate(overlay); }
            catch (Exception ignored) { }
            overlay = null;
        }
        locked = false;
        foregroundStarted = false;
        TouchTileService.refresh(this);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
