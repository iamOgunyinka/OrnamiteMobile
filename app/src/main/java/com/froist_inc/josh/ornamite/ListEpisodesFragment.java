package com.froist_inc.josh.ornamite;

import android.content.ClipData;
import android.content.ClipboardManager;
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
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class ListEpisodesFragment extends ListFragment
{
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
    public void onResume()
    {
        super.onResume();
        setEmptyText( "No episodes listed yet" );
        if( main_ui_handler == null ) main_ui_handler = new Handler( getActivity().getMainLooper() );
        FetchData();
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
                        final ArrayList<Utilities.EpisodeData> episode_list = ParseResultAndDisplay( details );
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

    private ArrayList<Utilities.EpisodeData> ParseResultAndDisplay(final JSONArray details )
    {
        ArrayList<Utilities.EpisodeData> data_list = new ArrayList<>();
        try {
            for ( int i = 0; i != details.length(); ++i ) {
                final JSONObject episode_object = details.getJSONObject( i );
                final Utilities.EpisodeData episode = new Utilities.EpisodeData( episode_object.getString("name"));
                JSONArray episode_links = episode_object.getJSONArray( "links");
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
                    link_view.setAdapter( new DownloadLinksAdapter( getActivity(), data_item.download_links ));
                    AlertDialog dialog = new AlertDialog.Builder( ListEpisodesFragment.this.getContext() )
                            .setPositiveButton( android.R.string.ok, null ).setTitle( "Download links" )
                            .setView( link_view ).create();
                    dialog.show();
                }
            });
            return convert_view;
        }
    }

    class DownloadLinksAdapter extends ArrayAdapter<Utilities.DownloadsData>
    {
        DownloadLinksAdapter( final Context context, final ArrayList<Utilities.DownloadsData> download_list )
        {
            super( context, 0, download_list );
        }

        @Override
        public View getView( int position, View convert_view, ViewGroup parent )
        {
            if( convert_view == null ){
                convert_view = getActivity().getLayoutInflater().inflate( R.layout.list_links_item, parent, false );
            }
            final Utilities.DownloadsData data = getItem( position );
            TextView download_type_text = ( TextView ) convert_view.findViewById( R.id.link_type_text );
            download_type_text.setText( data.download_type );
            Button copy_link_button = (Button) convert_view.findViewById( R.id.copy_link_button );
            copy_link_button.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick( View v )
                {
                    ClipData clip = ClipData.newPlainText( "Download Link", data.download_link );
                    final ClipboardManager clipboard = (ClipboardManager) getActivity()
                            .getSystemService( Context.CLIPBOARD_SERVICE );
                    clipboard.setPrimaryClip( clip );
                    Toast.makeText( getActivity(), "Link copied", Toast.LENGTH_SHORT ).show();
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
