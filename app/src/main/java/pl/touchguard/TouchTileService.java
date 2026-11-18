package pl.touchguard;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** User-configured shortcut in the Quick Settings panel. */
public final class TouchTileService extends TileService {
    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        // Power + volume button combinations are reserved by Android. A one-shot
        // transparent Activity is a reliable entry point across Android 12+.
        Intent toggle = new Intent(this, ToggleActivity.class);
        toggle.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        if (Build.VERSION.SDK_INT >= 34) {
            PendingIntent pending = PendingIntent.getActivity(this, 71, toggle,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            startActivityAndCollapse(pending);
        } else {
            startActivityAndCollapse(toggle);
        }
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;
        boolean active = TouchLockService.isLocked();
        tile.setState(active ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel("Blokada dotyku");
        tile.updateTile();
    }

    public static void refresh(Context context) {
        try {
            requestListeningState(context, new ComponentName(context, TouchTileService.class));
        } catch (Exception ignored) {
            // Android may defer tile updates until the panel is next expanded.
        }
    }
}
