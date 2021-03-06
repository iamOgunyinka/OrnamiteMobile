package com.froist_inc.josh.ornamite;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class ListEpisodesFragment extends ListFragment
{
    private static final String TAG = "ListEpisodeFrag";
    private static final String EPISODE_DATA = "EPISODIAL_DATA";

    private MenuItem refresh_menu;
    private long season_id;
    private Handler main_ui_handler;

    @Override
    public void onCreate( @Nullable Bundle saved_instance_state )
    {
        super.onCreate( saved_instance_state );
        getActivity().setTitle( getArguments().getString( ListEpisodesActivity.SEASON_NAME ) );
        season_id = getArguments().getLong( ListEpisodesActivity.SEASON_ID, -1 );
        setHasOptionsMenu( true );
    }

    @Override
    public void onPause()
    {
        super.onPause();
        final EpisodeAdapter adapter = ( EpisodeAdapter ) getListAdapter();
        final int episode_count = adapter.getCount();
        final JSONArray list_of_episodes = new JSONArray();
        try {
            for (int i = 0; i != episode_count; ++i) {
                final Utilities.EpisodeData episode = adapter.getItem( i );
                final JSONArray download_links = new JSONArray();
                for ( int j = 0; j != episode.download_links.size(); ++j ) {
                    final Utilities.DownloadsData download_item = episode.download_links.get( j );
                    final JSONObject download_object = new JSONObject();
                    download_object.put( "link", download_item.download_link );
                    download_object.put( "type", download_item.download_type );
                    download_links.put( download_object );
                }
                final JSONObject episode_data = new JSONObject();
                episode_data.put( "name", episode.episode_name );
                episode_data.put( "links", download_links );
                list_of_episodes.put( episode_data );
            }
        } catch ( JSONException exception ){
            Log.v( TAG, exception.getLocalizedMessage() );
        }
        getActivity().getIntent().putExtra( EPISODE_DATA, list_of_episodes.toString() );
    }

    @Override
    public void onResume()
    {
        super.onResume();
        setEmptyText( "No episodes listed yet" );
        if( main_ui_handler == null ) main_ui_handler = new Handler( getActivity().getMainLooper() );

        final String saved_instance_data = getActivity().getIntent().getStringExtra( EPISODE_DATA );
        if( saved_instance_data == null ){
            FetchData();
        } else {
            try {
                ReadSavedState(saved_instance_data);
            } catch ( JSONException exception ){
                Log.v( TAG, exception.getLocalizedMessage() );
                FetchData();
            }
        }
    }

    private void ReadSavedState( final String saved_instance_state ) throws JSONException
    {
        final JSONArray list_of_episodes = new JSONArray( saved_instance_state );
        final ArrayList<Utilities.EpisodeData> episodes = new ArrayList<>();

        for( int i = 0; i != list_of_episodes.length(); ++i ){
            final JSONObject episode = list_of_episodes.getJSONObject( i );
            final String episode_name = episode.getString( "name" );
            final JSONArray download_links = episode.getJSONArray( "links" );
            final Utilities.EpisodeData episode_data = new Utilities.EpisodeData( episode_name );
            for( int j = 0; j != download_links.length(); ++j ){
                final JSONObject links = download_links.getJSONObject( j );
                final String download_type = links.getString( "type" );
                final String download_link = links.getString( "link" );
                episode_data.AddDownloadsData( new Utilities.DownloadsData( download_type, download_link ));
            }
            episodes.add( episode_data );
        }
        setListAdapter( new EpisodeAdapter( ListEpisodesFragment.this.getContext(), episodes ));
    }

    @Override
    public void onCreateOptionsMenu( Menu menu, MenuInflater inflater )
    {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate( R.menu.other_menu, menu );
        refresh_menu = menu.findItem( R.id.action_refresh );
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        switch ( item.getItemId() ){
            case android.R.id.home:
                getActivity().onBackPressed();
                return true;
            case R.id.action_refresh:
                FetchData();
                return true;
            default:
                return super.onOptionsItemSelected( item );
        }
    }

    private void FetchData()
    {
        if( refresh_menu != null ){
            refresh_menu.setEnabled( false );
        }
        setListShown( false );
        Thread new_thread = new Thread( new Runnable() {
            @Override
            public void run()
            {
                try {
                    final String url = NetworkManager.EPISODE_URL + "?season_id=" + String.valueOf( season_id );
                    final byte[] response = NetworkManager.GetNetwork().GetData( url );
                    JSONObject result = null;
                    if( response != null ) {
                        result = new JSONObject( new String( response ));
                    }
                    final String message = result != null ? result.getString( "detail" ) :
                            "Could not get any data from the server";
                    if( result != null && ( result.getInt( "status" ) == Utilities.Success )){
                        final JSONArray details = result.getJSONArray( "detail" );
                        final ArrayList<Utilities.EpisodeData> episode_list = ParseNetworkResult( details );
                        main_ui_handler.post( new Runnable() {
                            @Override
                            public void run()
                            {
                                OnFetchSuccessful( episode_list );
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

    private ArrayList<Utilities.EpisodeData> ParseNetworkResult( final JSONArray details )
    {
        ArrayList<Utilities.EpisodeData> data_list = new ArrayList<>();
        try {
            for ( int i = 0; i != details.length(); ++i ) {
                final JSONObject episode_object = details.getJSONObject( i );
                final Utilities.EpisodeData episode = new Utilities.EpisodeData( episode_object.getString( "name" ));
                JSONArray episode_links = episode_object.getJSONArray( "links" );
                for ( int j = 0; j != episode_links.length(); ++j ) {
                    final JSONObject link_object = episode_links.getJSONObject( j );
                    final String download_type = link_object.getString( "type" );
                    final String download_link = link_object.getString( "link" );
                    episode.AddDownloadsData(new Utilities.DownloadsData( download_type, download_link ));
                }
                data_list.add( episode );
            }
        } catch ( JSONException exception ){
            Log.v( "ParseResult", exception.getLocalizedMessage() );
        }
        return data_list;
    }

    private void OnFetchSuccessful( final ArrayList<Utilities.EpisodeData> episode_list )
    {
        final Context context = ListEpisodesFragment.this.getContext();
        if( context == null ) return;
        setListAdapter( new EpisodeAdapter( ListEpisodesFragment.this.getContext(),
                episode_list.isEmpty() ? null : episode_list ) );
        setListShown( true );
        if( refresh_menu != null ) refresh_menu.setEnabled( true );
    }

    private void OnFetchFailed( final String message )
    {
        if( refresh_menu != null ) refresh_menu.setEnabled( true );
        setListShown( true );
        setEmptyText( "Nothing found" );
        AlertDialog dialog = new AlertDialog.Builder( ListEpisodesFragment.this.getContext() ).setTitle( "Error" )
                .setMessage( message ).setPositiveButton( android.R.string.ok, null ).create();
        dialog.show();
    }

    class EpisodeAdapter extends ArrayAdapter<Utilities.EpisodeData>
    {
        public EpisodeAdapter( final Context context, ArrayList<Utilities.EpisodeData> data_list )
        {
            super( context, 0, data_list );
        }

        @Override
        public View getView( int position, View convert_view, ViewGroup parent )
        {
            if( convert_view == null ){
                convert_view = getActivity().getLayoutInflater().inflate( R.layout.list_episodes_item, parent, false );
            }
            final Utilities.EpisodeData data_item = getItem( position );
            TextView season_name_text = (TextView) convert_view.findViewById( R.id.episode_name_text );
            season_name_text.setText( data_item.episode_name );

            TextView download_link_arity_text = (TextView) convert_view.findViewById( R.id.download_link_arity_text );
            final String link_arity = String.valueOf( data_item.download_links.size() ) + " links";
            download_link_arity_text.setText( link_arity );

            Button view_links_button = (Button) convert_view.findViewById( R.id.show_link_button );
            view_links_button.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick( View view )
                {
                    ListView link_view = new ListView( getActivity() );
                    link_view.setId( R.id.download_link_list );
                    link_view.setAdapter( new Utilities.DownloadLinksAdapter( getActivity(), data_item.download_links ));
                    AlertDialog dialog = new AlertDialog.Builder( ListEpisodesFragment.this.getContext() )
                            .setPositiveButton( android.R.string.ok, null ).setTitle( "Download links" )
                            .setView( link_view ).create();
                    dialog.show();
                }
            });
            return convert_view;
        }
    }

    public static Fragment GetInstance( final String title, final long season_id )
    {
        Bundle extra_argument = new Bundle();
        extra_argument.putString( ListEpisodesActivity.SEASON_NAME, title );
        extra_argument.putLong( ListEpisodesActivity.SEASON_ID, season_id );
        Fragment fragment = new ListEpisodesFragment();
        fragment.setArguments( extra_argument );
        return fragment;
    }
}
