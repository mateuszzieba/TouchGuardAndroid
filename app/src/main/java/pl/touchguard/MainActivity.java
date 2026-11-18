package pl.touchguard;

import android.Manifest;
import android.app.Activity;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final int REQUEST_NOTIFICATIONS = 42;
    private TextView statusText;
    private TextView durationText;
    private Button toggleButton;
    private Button overlayPermissionButton;
    private Button notificationPermissionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        durationText = findViewById(R.id.durationText);
        toggleButton = findViewById(R.id.toggleButton);
        overlayPermissionButton = findViewById(R.id.overlayPermissionButton);
        notificationPermissionButton = findViewById(R.id.notificationPermissionButton);
        SeekBar durationSeek = findViewById(R.id.durationSeek);
        durationSeek.setProgress(Prefs.getSeconds(this) / 15 - 1);
        durationText.setText(Prefs.formatDuration(Prefs.getSeconds(this)));

        durationSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int seconds = (progress + 1) * 15;
                durationText.setText(Prefs.formatDuration(seconds));
                if (fromUser) Prefs.setSeconds(MainActivity.this, seconds);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        toggleButton.setOnClickListener(v -> toggleLock());
        overlayPermissionButton.setOnClickListener(v -> openOverlaySettings());
        notificationPermissionButton.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
            }
        });
        findViewById(R.id.addTileButton).setOnClickListener(v -> addTile());
        updateUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUi();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATIONS) updateUi();
    }

    private void updateUi() {
        boolean locked = TouchLockService.isLocked();
        statusText.setText(locked ? "Dotyk zablokowany" : "Dotyk odblokowany");
        toggleButton.setText(locked ? "Wyłącz blokadę" : "Włącz blokadę");
        boolean canOverlay = Settings.canDrawOverlays(this);
        overlayPermissionButton.setText(canOverlay ? "✓ Uprawnienie do nakładki przyznane" : "Zezwól na nakładkę");
        overlayPermissionButton.setEnabled(!canOverlay);
        boolean needsNotifications = Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED;
        notificationPermissionButton.setVisibility(needsNotifications ? View.VISIBLE : View.GONE);
    }

    private void toggleLock() {
        if (!TouchLockService.isLocked() && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Najpierw zezwól na wyświetlanie nad innymi aplikacjami.", Toast.LENGTH_LONG).show();
            openOverlaySettings();
            return;
        }
        Intent service = new Intent(this, TouchLockService.class);
        if (TouchLockService.isLocked()) {
            service.setAction(TouchLockService.ACTION_UNLOCK);
            startService(service);
        } else {
            service.setAction(TouchLockService.ACTION_LOCK);
            startForegroundService(service);
        }
        // An overlay will cover this screen immediately; it can always be
        // dismissed by holding its small lock badge or waiting for timeout.
        getWindow().getDecorView().postDelayed(this::updateUi, 350);
    }

    private void openOverlaySettings() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        try {
            startActivity(intent);
        } catch (Exception exception) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private void addTile() {
        if (Build.VERSION.SDK_INT >= 33) {
            StatusBarManager manager = getSystemService(StatusBarManager.class);
            if (manager != null) {
                ComponentName component = new ComponentName(this, TouchTileService.class);
                try {
                    manager.requestAddTileService(
                            component,
                            getString(R.string.quick_tile_label),
                            Icon.createWithResource(this, R.drawable.ic_lock_mono),
                            getMainExecutor(),
                            result -> {
                                if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED) {
                                    Toast.makeText(this, "Kafelek dodany do szybkich ustawień.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Możesz też dodać kafelek ręcznie z edycji panelu.", Toast.LENGTH_LONG).show();
                                }
                            });
                    return;
                } catch (Exception ignored) {
                    // Some manufacturer Android builds do not expose the tile prompt.
                }
            }
        }
        Toast.makeText(this, "Rozwiń szybkie ustawienia → Edytuj → dodaj Blokada dotyku.", Toast.LENGTH_LONG).show();
    }
}
