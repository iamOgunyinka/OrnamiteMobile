package com.froist_inc.josh.ornamite;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;


public class UpdatesFragment extends Fragment
{
    private MenuItem refresh_menu;
    private ExpandableListView root_list_view;
    private View overlay_view;

    private Handler main_ui_handler;
    @Override
    public void onCreate( @Nullable Bundle saved_instance_state )
    {
        super.onCreate( saved_instance_state );
        setHasOptionsMenu( true );
        LoadUpdates();
    }

    @Override
    public void onCreateOptionsMenu( Menu menu, MenuInflater inflater )
    {
        super.onCreateOptionsMenu( menu, inflater );
        inflater.inflate( R.menu.update_menu, menu );
        refresh_menu = menu.findItem( R.id.action_refresh_menu );
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        final int item_id = item.getItemId();
        switch( item_id ){
            case R.id.action_refresh_menu:
                RefreshTodaysUpdate();
                return true;
            case R.id.action_automatic_check_menu:
                boolean should_start_alarm = !UpdateBackgroundService.IsServiceAlarmOn( getActivity() );
                UpdateBackgroundService.SetServiceAlarm( getActivity(), should_start_alarm );
                return true;
            default:
                return super.onOptionsItemSelected( item );
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
        super.onPrepareOptionsMenu(menu);
        MenuItem automatic_update_item = menu.findItem( R.id.action_automatic_check_menu );
        automatic_update_item.setChecked( UpdateBackgroundService.IsServiceAlarmOn( getActivity() ) );
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try {
            Utilities.WriteTvUpdateData( this.getContext(), NetworkManager.ALL_UPDATES_FILENAME, Utilities.AllUpdates );
        } catch ( JSONException | IOException exception ){
            Log.v( "SavingState", exception.getLocalizedMessage() );
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if( main_ui_handler == null ) main_ui_handler = new Handler( getActivity().getMainLooper() );
    }

    private void LoadUpdates()
    {
        new LoadUpdatesTask().execute();
    }

    @Nullable
    @Override
    public View onCreateView( LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle saved_instance_state )
    {
        View root_view = inflater.inflate( R.layout.updates_fragment, container, false );
        root_list_view = ( ExpandableListView ) root_view.findViewById( R.id.expandable_updates_listview );
        overlay_view = root_view.findViewById( R.id.list_overlay );
        overlay_view.setVisibility( View.VISIBLE );

        TextView empty_view = new TextView( this.getContext() );
        empty_view.setId( R.id.empty_text );
        empty_view.setText( R.string.no_updates );
        root_list_view.setEmptyView( empty_view );

        return root_view;
    }

    private class LoadUpdatesTask extends AsyncTask<Void, Void, HashMap<String, ArrayList<Utilities.EpisodeData>>>
    {
        String error_message;
        @Override
        protected HashMap<String, ArrayList<Utilities.EpisodeData>> doInBackground( Void... params )
        {
            try {
                if( Utilities.AllUpdates == null || Utilities.AllUpdates.size() != 0 ) {
                    return Utilities.ReadTvUpdates( getActivity(), NetworkManager.ALL_UPDATES_FILENAME );
                }
                return Utilities.AllUpdates;
            } catch ( JSONException | IOException except ){
                error_message = except.getLocalizedMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute( final HashMap<String, ArrayList<Utilities.EpisodeData>> data_map )
        {
            SetAdapter( data_map );
        }
    }

    void SetAdapter( final HashMap<String, ArrayList<Utilities.EpisodeData>> data_map )
    {
        int position = 0, index = 0;
        final String today = Utilities.GetDateFromCalendar( GregorianCalendar.getInstance() );
        ArrayList<String> headers = new ArrayList<>();

        if ( Utilities.AllUpdates == null ) {
            Utilities.AllUpdates = data_map;
        } else {
            // usually a single shot
            for ( HashMap.Entry<String, ArrayList<Utilities.EpisodeData>> entry : data_map.entrySet() ) {
                Utilities.AllUpdates.put( entry.getKey(), entry.getValue() );
            }
        }
        for ( HashMap.Entry<String, ArrayList<Utilities.EpisodeData>> entry : Utilities.AllUpdates.entrySet() ) {
            final String key = entry.getKey();
            headers.add( key );
            if( key.equals( today ) ) position = index;
            index += 1;
        }

        AddFooter();

        root_list_view.setAdapter( new UpdatesFragmentAdapter( this.getContext(), Utilities.AllUpdates, headers ));
        if( Utilities.AllUpdates != null && Utilities.AllUpdates.size() > 0 ) {
            root_list_view.smoothScrollToPosition( position );
            root_list_view.expandGroup( position );
        }
        overlay_view.setVisibility( View.INVISIBLE );
    }

    private void AddFooter()
    {
        LayoutInflater inflater = ( LayoutInflater ) getActivity().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View footer_view = inflater.inflate( R.layout.list_view_footer, null, false );
        root_list_view.addFooterView( footer_view );
    }

    void RefreshTodaysUpdate()
    {
        refresh_menu.setEnabled( false );
        overlay_view.setVisibility( View.VISIBLE );
        Thread new_thread = new Thread( new Runnable() {
            @Override
            public void run()
            {
                try {
                    final String url = NetworkManager.UPDATES_URL + "?date=" + Utilities.Today();
                    final byte[] response = NetworkManager.GetNetwork().GetData( url );
                    JSONObject result = null;
                    if( response != null ) {
                        result = new JSONObject( new String( response ));
                    }
                    final String message = result != null ? result.getString( "detail" ) :
                            "Could not get any data from the server";
                    if( result != null && ( result.getInt("status") == Utilities.Success )){
                        JSONArray details = result.getJSONArray( "detail" );
                        final HashMap<String, ArrayList<Utilities.EpisodeData>> parsing_result = ParseResult( details );
                        main_ui_handler.post( new Runnable() {
                            @Override
                            public void run()
                            {
                                OnFetchSuccessful( parsing_result );
                            }
                        });
                    } else {
                        main_ui_handler.post( new Runnable() {
                            @Override
                            public void run() {
                                OnFetchFailed( message );
                            }
                        });
                    }
                } catch ( JSONException | IOException exception ){
                    main_ui_handler.post( new Runnable() {
                        @Override
                        public void run() {
                            OnFetchFailed( exception.getMessage() );
                        }
                    });
                }
            }
        });
        new_thread.start();
    }

    public static HashMap<String, ArrayList<Utilities.EpisodeData>> ParseResult( final JSONArray result_list )
    {
        HashMap<String, ArrayList<Utilities.EpisodeData>> today_data = new HashMap<>();
        try {
            ArrayList<Utilities.EpisodeData> tv_series_updates = new ArrayList<>();
            for ( int index = 0; index != result_list.length(); ++index ) {
                final JSONObject updates_object = result_list.getJSONObject( index );
                final String episode_name = updates_object.getString( "episode" );
                final String series_name = updates_object.getString( "name" );
                final String season_name = updates_object.getString( "season" );

                final String name = String.format( "%s - %s ( %s )", series_name, season_name, episode_name );
                final JSONArray download_links = updates_object.optJSONArray( "download_links" );
                if( download_links != null ){
                    Utilities.EpisodeData episode = new Utilities.EpisodeData( name );
                    for( int x = 0; download_links.length() != x; ++x ){
                        JSONObject download_object = download_links.getJSONObject( x );
                        final String download_type = download_object.getString( "download_type" );
                        final String download_link = download_object.getString( "link" );
                        episode.AddDownloadsData( new Utilities.DownloadsData( download_type, download_link ));
                    }
                    tv_series_updates.add( episode );
                }
            }

            final String today = Utilities.GetDateFromCalendar( GregorianCalendar.getInstance() );
            today_data.put( today, tv_series_updates );
        } catch ( JSONException exception ){
            Log.v( "ParseUpdateResult", exception.getLocalizedMessage() );
        }
        return today_data;
    }

    private void OnFetchFailed( final String message )
    {
        refresh_menu.setEnabled( true );
        overlay_view.setVisibility( View.INVISIBLE );
        AlertDialog dialog = new AlertDialog.Builder( UpdatesFragment.this.getContext() ).setTitle( "Error" )
                .setMessage( message ).setPositiveButton( android.R.string.ok, null ).create();
        dialog.show();
    }

    private void OnFetchSuccessful( HashMap<String, ArrayList<Utilities.EpisodeData>> parsing_result)
    {
        refresh_menu.setEnabled( true );
        SetAdapter( parsing_result );
        overlay_view.setVisibility( View.INVISIBLE );
    }

    private class UpdatesFragmentAdapter extends BaseExpandableListAdapter
    {
        private ArrayList<String> header_lists; // dates of updates
        private final HashMap<String, ArrayList<Utilities.EpisodeData>> data_map;
        private Context context;

        UpdatesFragmentAdapter( final Context context, HashMap<String, ArrayList<Utilities.EpisodeData>> data_map,
                                final ArrayList<String> headers )
        {
            this.context = context;
            this.data_map = data_map;
            header_lists = headers;
        }

        @Override
        public long getChildId( int group_position, int child_position )
        {
            return child_position;
        }

        @Override
        public long getGroupId( int group_position ) {
            return group_position;
        }

        @Override
        public Object getChild( int group_position, int child_position )
        {
            return data_map.get( header_lists.get( group_position ) ).get( child_position );
        }

        @Override
        public int getChildrenCount( int group_position ) {
            return data_map.get( header_lists.get( group_position ) ).size();
        }

        @Override
        public int getGroupCount() {
            return header_lists.size();
        }

        @Override
        public Object getGroup( int group_position ) {
            return header_lists.get( group_position );
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable( int group_position, int child_position ) {
            return true;
        }

        @Override
        public View getChildView( int group_position, int child_position, boolean is_last_child,
                                  View convert_view, ViewGroup parent )
        {
            final Utilities.EpisodeData episode_data = (Utilities.EpisodeData) getChild( group_position, child_position );
            if( convert_view == null ){
                LayoutInflater inflater = ( LayoutInflater )context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
                convert_view = inflater.inflate( R.layout.list_episodes_item, parent, false );
            }
            TextView season_name_text = (TextView) convert_view.findViewById( R.id.episode_name_text );
            season_name_text.setText( episode_data.episode_name );
            season_name_text.setTextSize( 10 );

            TextView download_link_arity_text = (TextView) convert_view.findViewById( R.id.download_link_arity_text );
            final String link_arity = String.valueOf( episode_data.download_links.size() ) + " links";
            download_link_arity_text.setText( link_arity );

            Button view_links_button = (Button) convert_view.findViewById( R.id.show_link_button );
            view_links_button.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick( View view )
                {
                    ListView link_view = new ListView( context );
                    link_view.setId( R.id.download_link_list );
                    link_view.setAdapter(new Utilities.DownloadLinksAdapter( context, episode_data.download_links));
                    AlertDialog dialog = new AlertDialog.Builder( UpdatesFragment.this.getContext() )
                            .setPositiveButton( android.R.string.ok, null ).setTitle( "Download links" )
                            .setView( link_view ).create();
                    dialog.show();
                }
            });
            return convert_view;
        }

        @Override
        public View getGroupView( int group_position, boolean is_expanded, View convert_view, ViewGroup parent )
        {
            if( convert_view == null ){
                LayoutInflater inflater = ( LayoutInflater ) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
                convert_view = inflater.inflate( R.layout.updates_root_item, parent, false );
            }
            TextView date_text = ( TextView ) convert_view.findViewById( R.id.updates_root_text );
            date_text.setText( header_lists.get( group_position ));
            return convert_view;
        }
    }
}
