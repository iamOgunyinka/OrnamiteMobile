package com.froist_inc.josh.ornamite;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class StartupReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive( final Context context, final Intent intent )
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences( context );

        boolean auto_update_is_on = preferences.getBoolean( UpdateBackgroundService.AUTO_CHECK_UPDATE_IS_ON, false );
        UpdateBackgroundService.SetServiceAlarm( context, auto_update_is_on );
        UpdateCleanupService.UpdateCleanupAlarm( context, true );
    }
}
