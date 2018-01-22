package com.froist_inc.josh.ornamite;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;


public class UpdateCleanupService extends IntentService
{
    public static final String TAG = "UpdateCleanup";
    private static final int A_MINUTE = 1000 * 60;
    public static final int TWELVE_HOURS_INTERVAL = ( A_MINUTE * 60 ) * 12;

    public UpdateCleanupService(){
        super( TAG );
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        CleanUpdateFile( this );
    }

    private void CleanUpdateFile( final Context context )
    {
        HashMap<String, ArrayList<Utilities.EpisodeData>> tv_updates = null;
        try {
            tv_updates = Utilities.ReadTvUpdates( context, NetworkManager.ALL_UPDATES_FILENAME );
            if( tv_updates == null ) return;
            SharedPreferences shared_preferences = PreferenceManager.getDefaultSharedPreferences( context );
            int number_of_cleanup_days = shared_preferences.getInt( SettingsActivity.CLEANUP_INTERVAL,
                    SettingsActivity.DEFAULT_RANGE );
            ArrayList<String> last_seven_days = GetLastDates( number_of_cleanup_days );
            HashMap<String, ArrayList<Utilities.EpisodeData>> pardoned_data = new HashMap<>();
            for( int i = 0; i != last_seven_days.size(); ++i ){
                final String key = last_seven_days.get( i );
                if( tv_updates.containsKey( key ) ){
                    pardoned_data.put( key, tv_updates.get( key ) );
                }
            }
            Utilities.WriteTvUpdateData( context, NetworkManager.ALL_UPDATES_FILENAME, pardoned_data );
        } catch ( JSONException | IOException except ){
            Log.v( TAG, except.getLocalizedMessage() );
            if( tv_updates != null ){
                try {
                    Utilities.WriteTvUpdateData(context, NetworkManager.ALL_UPDATES_FILENAME, tv_updates);
                } catch( JSONException | IOException exception ){
                    Log.v( TAG, exception.getLocalizedMessage() );
                }
            }
        }
    }

    private ArrayList<String> GetLastDates( final int number_of_cleanup_days )
    {
        final ArrayList<String> dates = new ArrayList<>();
        final Calendar calendar = GregorianCalendar.getInstance();
        dates.add( Utilities.GetDateFromCalendar( calendar ) ); // today
        for( int i = 1; i != number_of_cleanup_days; ++i ) {
            calendar.add( Calendar.DATE, -1 );
            dates.add( Utilities.GetDateFromCalendar( calendar ) );
        }
        return dates;
    }

    public static void UpdateCleanupAlarm( final Context context, boolean starting_service )
    {
        Intent intent = new Intent( context, UpdateCleanupService.class );
        PendingIntent pending_intent = PendingIntent.getService( context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT );
        AlarmManager alarm_manager = ( AlarmManager ) context.getSystemService( Context.ALARM_SERVICE );
        if( starting_service ) {
            alarm_manager.setRepeating( AlarmManager.RTC, System.currentTimeMillis(), TWELVE_HOURS_INTERVAL,
                    pending_intent );
        } else {
            alarm_manager.cancel( pending_intent );
            pending_intent.cancel();
        }
    }
}
