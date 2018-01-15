package com.froist_inc.josh.ornamite;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class UpdateBackgroundService extends IntentService
{
    public static final String AUTO_CHECK_UPDATE_IS_ON = "IsServiceAlarmOn";
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

        CheckUpdateAndNotifyUserIfAny( this );
    }

    public static void CheckUpdateAndNotifyUserIfAny( final Context context )
    {
        try {
            final HashMap<String, ArrayList<Utilities.EpisodeData>> tv_updates =
                    Utilities.ReadTvUpdates( context, NetworkManager.ALL_UPDATES_FILENAME );
            if( tv_updates == null ) return;
            final String todays_date = UpdatesFragment.TodaysDate();
            final ArrayList<Utilities.EpisodeData> updates_data = tv_updates.get( todays_date );
            if( updates_data == null || updates_data.isEmpty() ){
                Log.v( TAG, "Fetching updates" );
                FetchUpdates( context, tv_updates );
            }
        } catch ( JSONException | IOException except ){
            Log.v( TAG, except.getLocalizedMessage() );
        }
    }

    public static void FetchUpdates( final Context context,
                                     final HashMap<String, ArrayList<Utilities.EpisodeData>> all_updates )
            throws JSONException, IOException
    {
        try {
            final byte[] response = NetworkManager.GetNetwork().GetData( NetworkManager.UPDATES_URL );
            JSONObject result = null;
            if( response != null ) {
                result = new JSONObject( new String( response ));
            }
            final String message = result != null ? result.getString( "detail" ) :
                    "Could not get any data from the server";
            if( result != null && ( result.getInt( "status" ) == Utilities.Success )){
                final JSONArray result_details = result.getJSONArray( "detail" );
                final HashMap<String, ArrayList<Utilities.EpisodeData>> today_updates =
                        UpdatesFragment.ParseResult( result_details );
                for( HashMap.Entry<String, ArrayList<Utilities.EpisodeData>> entry: today_updates.entrySet())
                {
                    all_updates.put( entry.getKey(), entry.getValue() );
                }
                Utilities.WriteTvUpdateData( context, NetworkManager.ALL_UPDATES_FILENAME, all_updates );
                Log.v( TAG, "Filtering TV Updates" );
                FilterSubscriptionAndNotifyUser( context, result_details );
            } else {
                OnFailure( message );
            }
        } catch ( JSONException | IOException exception ){
            OnFailure( exception.getMessage() );
        }
    }

    private static void FilterSubscriptionAndNotifyUser( final Context context, final JSONArray today_updates )
    {
        try {
            final HashMap<String, Utilities.TvSeriesData> all_tv_series =
                    Utilities.ReadTvSeriesData( context, NetworkManager.ALL_SERIES_FILENAME );
            final HashSet<String> subscribed = new HashSet<>();
            for ( HashMap.Entry<String, Utilities.TvSeriesData> entry_pair : all_tv_series.entrySet() ) {
                if( entry_pair.getValue().IsSubscribed() ) subscribed.add( entry_pair.getKey().toLowerCase() );
            }
            ArrayList<String> tv_series_for_today = new ArrayList<>();
            for( int i = 0; i != today_updates.length(); ++i ){
                final JSONObject today_updates_object = today_updates.getJSONObject( i );
                final String tv_name = today_updates_object.optString( "name" );
                if( subscribed.contains( tv_name ) ){
                    tv_series_for_today.add( tv_name );
                }
            }
            Log.v( TAG, "Sending notification" );
            if( tv_series_for_today.isEmpty() ) return;
            NotifyUser( context, tv_series_for_today );
        } catch ( JSONException | IOException except ){
            OnFailure( except.getMessage() );
        }
    }

    private static void NotifyUser( final Context context, final ArrayList<String> today_series )
    {
        PendingIntent pending_intent = PendingIntent.getActivity( context, 1,
                new Intent( context, MainActivity.class ), 0 );
        StringBuilder text_builder = new StringBuilder();
        for( String series: today_series ){
            text_builder.append( series ).append( "\n" );
        }
        Notification notification = new NotificationCompat.Builder( context )
                .setTicker( "New TV Series available" )
                .setSmallIcon( android.R.drawable.ic_dialog_alert )
                .setContentTitle( "TV Series updates" )
                .setContentText( text_builder.toString() )
                .setContentIntent( pending_intent )
                .setAutoCancel( true )
                .build();
        NotificationManager notification_manager = ( NotificationManager ) context.getSystemService(
                Context.NOTIFICATION_SERVICE );
        notification_manager.notify( 0, notification );
    }

    private static void OnFailure( final String message )
    {
        Log.v( TAG, message );
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
        PreferenceManager.getDefaultSharedPreferences( context ).edit()
                .putBoolean( UpdateBackgroundService.AUTO_CHECK_UPDATE_IS_ON, keeping_alarm_on ).commit();
    }
}
