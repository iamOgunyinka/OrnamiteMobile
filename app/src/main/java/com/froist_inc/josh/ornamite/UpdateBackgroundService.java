package com.froist_inc.josh.ornamite;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class UpdateBackgroundService extends IntentService
{
    private static final String TAG = "UpdateBackgroundService";
    private static final int AN_HOUR_INTERVAL = 1000 * 60 * 60;
    public UpdateBackgroundService()
    {
        super( TAG );
    }

    @Override
    protected void onHandleIntent( Intent intent )
    {
        ConnectivityManager connectivity_manager = ( ConnectivityManager ) getSystemService(Context.CONNECTIVITY_SERVICE );
        final NetworkInfo active_network = connectivity_manager.getActiveNetworkInfo();
        if( active_network == null || !active_network.isConnected() ) return;

        Log.v( TAG, "Network is available" );
    }

    public static boolean IsServiceAlarmOn( Context context )
    {
        Intent intent = new Intent( context, UpdateBackgroundService.class );
        PendingIntent pending_intent = PendingIntent.getService( context, 0, intent, PendingIntent.FLAG_NO_CREATE );
        return pending_intent != null;
    }

    public static void SetServiceAlarm( Context context, boolean keeping_alarm_on )
    {
        Intent intent = new Intent( context, UpdateBackgroundService.class );
        PendingIntent pending_intent = PendingIntent.getService( context, 0, intent, 0 );
        AlarmManager alarm_manager = ( AlarmManager ) context.getSystemService( Context.ALARM_SERVICE );
        if( keeping_alarm_on ){
            alarm_manager.setRepeating( AlarmManager.RTC, System.currentTimeMillis(), AN_HOUR_INTERVAL, pending_intent );
        } else {
            alarm_manager.cancel( pending_intent );
            pending_intent.cancel();
        }
    }
}
