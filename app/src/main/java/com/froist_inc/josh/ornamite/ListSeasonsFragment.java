package com.froist_inc.josh.ornamite;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class ListSeasonsFragment extends ListFragment
{
    public static final int REQUEST_CODE = 0;

    private long   show_id;
    private String show_name;
    private Handler main_ui_handler;

    private MenuItem refresh_menu;
    private ArrayList<SeasonData> seasons_list;

    @Override
    public void onCreate( @Nullable Bundle saved_instance_state )
    {
        super.onCreate( saved_instance_state );
        setHasOptionsMenu( true );

        show_name = getArguments().getString( ListSeasonsActivity.SHOW_NAME );
        show_id = getArguments().getLong( ListSeasonsActivity.SHOW_ID, -1 );

        if( main_ui_handler == null ) main_ui_handler = new Handler( Looper.getMainLooper() );
        getActivity().setTitle( show_name );
    }

    @Override
    public void onResume()
    {
        super.onResume();
        final String saved_instance_data = getActivity().getIntent().getStringExtra( "SavedInstance" );

        if( saved_instance_data == null ) {
            GetShowSeasons();
        } else {
            try {
                JSONObject root_item = new JSONObject( saved_instance_data );
                show_id = root_item.getLong( "show_id" );
                show_name = root_item.getString( "show_name" );
                if( root_item.has( "items" ) ){
                    JSONArray items = root_item.getJSONArray( "items" );
                    seasons_list = new ArrayList<>();
                    for( int i = 0; i != items.length(); ++i ){
                        JSONObject item = items.getJSONObject( i );
                        seasons_list.add( new SeasonData( item.getString( "name" ), item.getLong( "id" )) );
                    }
                }
                setListAdapter( new SeasonsAdapter( ListSeasonsFragment.this.getContext(), seasons_list.isEmpty() ?
                        null : seasons_list ) );
            } catch ( JSONException exception ){
                Toast.makeText( getActivity(), "Unable to restore old state", Toast.LENGTH_LONG ).show();
                getActivity().onBackPressed();
            }
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        JSONObject persistent_data = new JSONObject();
        try {
            persistent_data.put( "show_name", show_name );
            persistent_data.put( "show_id", show_id );
            if ( seasons_list != null && !seasons_list.isEmpty() ) {
                JSONArray items = new JSONArray();
                for (int i = 0; i != seasons_list.size(); ++i) {
                    JSONObject item = new JSONObject();
                    item.put( "id", seasons_list.get( i ).season_id );
                    item.put( "name", seasons_list.get(i).season_name );
                    items.put( item );
                }
                persistent_data.put( "items", items );
            }
        } catch ( JSONException exception ){
            Log.v( "SavedInstance", exception.getLocalizedMessage() );
        }
        getActivity().getIntent().putExtra( "SavedInstance", persistent_data.toString() );
    }

    @Override
    public void onCreateOptionsMenu( Menu menu, MenuInflater inflater )
    {
        super.onCreateOptionsMenu( menu, inflater );
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
                GetShowSeasons();
                return true;
            default:
                return super.onOptionsItemSelected( item );
        }
    }

    private void GetShowSeasons()
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
                    final String url = NetworkManager.LIST_SEASONS_URL + "?show_id=" + String.valueOf( show_id );
                    final byte[] response = NetworkManager.GetNetwork().GetData( url );
                    JSONObject result = null;
                    if( response != null ) {
                        result = new JSONObject( new String( response ));
                    }
                    final String message = result != null ? result.getString( "detail" ) :
                            "Could not get any data from the server";
                    if( result != null && ( result.getInt( "status" ) == Utilities.Success )){
                        JSONArray details = result.getJSONArray( "detail" );
                        seasons_list = ParseResultAndDisplay( details );
                        main_ui_handler.post( new Runnable() {
                            @Override
                            public void run()
                            {
                                setListAdapter( new SeasonsAdapter( ListSeasonsFragment.this.getContext(),
                                        seasons_list.isEmpty() ? null : seasons_list ) );
                                setListShown( true );
                                if( refresh_menu != null ) refresh_menu.setEnabled( true );
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

    private void OnFetchFailed( final String message )
    {
        if( refresh_menu != null ) refresh_menu.setEnabled( true );
        setListShown( true );
        setEmptyText( "Nothing found" );
        AlertDialog dialog = new AlertDialog.Builder( ListSeasonsFragment.this.getContext() ).setTitle( "Error" )
                .setMessage( message ).setPositiveButton( android.R.string.ok, null ).create();
        dialog.show();
    }

    private ArrayList<SeasonData> ParseResultAndDisplay( final JSONArray data_list )
    {
        ArrayList<SeasonData> season_list = new ArrayList<>();
        try {
            for (int i = 0; i != data_list.length(); ++i) {
                JSONObject data_item = data_list.getJSONObject( i );
                season_list.add( new SeasonData(data_item.getString("name"), data_item.getLong( "id" ) ) );
            }
        } catch ( JSONException except ){
            Log.v( "ParseResult", except.getLocalizedMessage() );
        }
        return season_list;
    }

    class SeasonData
    {
        public String season_name;
        public Long   season_id;
        public SeasonData( final String season_name, final long season_id ){
            this.season_name = season_name;
            this.season_id = season_id;
        }
    }

    class SeasonsAdapter extends ArrayAdapter<SeasonData>
    {
        public SeasonsAdapter( final Context context, ArrayList<SeasonData> data_list )
        {
            super( context, 0, data_list );
        }

        @Override
        public View getView( int position, View convert_view, ViewGroup parent )
        {
            if( convert_view == null ){
                convert_view = getActivity().getLayoutInflater().inflate( R.layout.list_seasons_item, parent, false );
            }
            TextView season_name_text = (TextView) convert_view.findViewById( R.id.season_name_text );
            season_name_text.setText( getItem( position ).season_name );
            return convert_view;
        }
    }

    @Override
    public void onListItemClick( ListView l, View view, int position, long id )
    {
        SeasonData data = (( SeasonsAdapter ) getListAdapter() ).getItem( position );
        Intent intent = new Intent( getActivity(), ListEpisodesActivity.class );

        intent.putExtra( ListEpisodesActivity.SEASON_NAME, show_name + " - " + data.season_name );
        intent.putExtra( ListEpisodesActivity.SEASON_ID, data.season_id );
        startActivityForResult( intent, ListSeasonsFragment.REQUEST_CODE );
        getActivity().overridePendingTransition( R.anim.push_left_in, R.anim.push_left_out );
    }


    public static Fragment GetFragmentInstance(final String show_name, final long show_id )
    {
        Bundle extra_bundle = new Bundle();
        extra_bundle.putString( ListSeasonsActivity.SHOW_NAME, show_name );
        extra_bundle.putLong( ListSeasonsActivity.SHOW_ID, show_id );
        ListSeasonsFragment fragment = new ListSeasonsFragment();
        fragment.setArguments( extra_bundle );

        return fragment;
    }
}
