package pl.touchguard;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

/** Transparent one-shot activity: the system permits a user-initiated foreground
 * service start from here even on Android versions restricting background starts.
 */
public final class ToggleActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!TouchLockService.isLocked() && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Przyznaj uprawnienie do nakładki w TouchGuard.", Toast.LENGTH_LONG).show();
            Intent main = new Intent(this, MainActivity.class);
            main.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(main);
            finish();
            return;
        }

        Intent intent = new Intent(this, TouchLockService.class);
        if (TouchLockService.isLocked()) {
            intent.setAction(TouchLockService.ACTION_UNLOCK);
            startService(intent);
        } else {
            intent.setAction(TouchLockService.ACTION_LOCK);
            startForegroundService(intent);
        }
        finish();
        overridePendingTransition(0, 0);
    }
}
