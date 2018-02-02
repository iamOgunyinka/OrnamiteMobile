package com.froist_inc.josh.ornamite;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class ListSeasonsFragment extends Fragment
{
    private static final int REQUEST_CODE = 0;

    private long   show_id;
    private String show_name;
    private String show_description;
    private Handler main_ui_handler;

    private MenuItem refresh_menu;
    private ArrayList<SeasonData> seasons_list;
    private ListView  list_view;
    private View      loading_view;
    private View      empty_view;
    private TextView  description_text;

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

    @Nullable @Override
    public View onCreateView( LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle saved_instance_state )
    {
        final View root_view = inflater.inflate( R.layout.seasons_fragment_layout, container, false );
        list_view = ( ListView ) root_view.findViewById(R.id.seasons_list_view );
        loading_view = root_view.findViewById( R.id.seasons_loading_view );
        description_text = ( TextView ) root_view.findViewById( R.id.series_description_text );

        empty_view = root_view.findViewById( R.id.seasons_empty_view );
        list_view.setEmptyView( empty_view );

        return root_view;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        final String saved_instance_data = getActivity().getIntent().getStringExtra( "SavedInstance" );
        if( saved_instance_data == null ){
            GetShowSeasons();
        } else {
            ReadSavedState( saved_instance_data );
        }

        list_view.setOnItemClickListener( new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick( AdapterView<?> parent, View view, int position, long id ) {
                SeasonData data = (( SeasonsAdapter ) list_view.getAdapter() ).getItem( position );
                Intent intent = new Intent( getActivity(), ListEpisodesActivity.class );

                intent.putExtra( ListEpisodesActivity.SEASON_NAME, show_name + " - " + data.season_name );
                intent.putExtra( ListEpisodesActivity.SEASON_ID, data.season_id );
                startActivityForResult( intent, ListSeasonsFragment.REQUEST_CODE );
                getActivity().overridePendingTransition( R.anim.push_left_in, R.anim.push_left_out );
            }
        });
    }

    private void ReadSavedState( final String saved_instance_data )
    {
        try {
            JSONObject root_item = new JSONObject( saved_instance_data );
            show_id = root_item.getLong( "show_id" );
            show_name = root_item.getString( "show_name" );
            show_description = root_item.getString( "desc" );
            if( root_item.has( "items" ) ){
                JSONArray items = root_item.getJSONArray( "items" );
                seasons_list = new ArrayList<>();
                for( int i = 0; i != items.length(); ++i ){
                    JSONObject item = items.getJSONObject( i );
                    seasons_list.add( new SeasonData( item.getString( "name" ), item.getLong( "id" )) );
                }
            }
            description_text.setText( show_description );
            list_view.setAdapter( new SeasonsAdapter( ListSeasonsFragment.this.getContext(), seasons_list.isEmpty() ?
                    null : seasons_list ) );
        } catch ( JSONException exception ){
            Toast.makeText( getActivity(), "Unable to restore old state", Toast.LENGTH_LONG ).show();
            getActivity().onBackPressed();
        }
    }

    private void SaveStateToStorage()
    {
        JSONObject persistent_data = new JSONObject();
        try {
            persistent_data.put( "show_name", show_name );
            persistent_data.put( "show_id", show_id );
            persistent_data.put( "desc", show_description );
            
            if ( seasons_list != null && !seasons_list.isEmpty() ) {
                JSONArray items = new JSONArray();
                for ( int i = 0; i != seasons_list.size(); ++i ) {
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
    public void onPause()
    {
        super.onPause();
        SaveStateToStorage();
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
        loading_view.setVisibility( View.VISIBLE );
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
                        JSONObject details = result.getJSONObject( "detail" );
                        seasons_list = ParseResultAndDisplay( details );

                        if( main_ui_handler != null && main_ui_handler.getLooper() != null ) {
                            main_ui_handler.post( new Runnable() {
                                @Override
                                public void run() {
                                    final Context context = ListSeasonsFragment.this.getContext();
                                    if( context == null ) return;
                                    list_view.setAdapter( new SeasonsAdapter( context,
                                            seasons_list.isEmpty() ? null : seasons_list));
                                    description_text.setText( show_description );
                                    loading_view.setVisibility( View.INVISIBLE );
                                    if ( refresh_menu != null ) refresh_menu.setEnabled( true );
                                }
                            });
                        }
                    } else {
                        if( main_ui_handler != null && main_ui_handler.getLooper() != null ) {
                            main_ui_handler.post( new Runnable() {
                                @Override
                                public void run() {
                                    OnFetchFailed( message );
                                }
                            });
                        }
                    }
                } catch ( JSONException | IOException exception ){
                    if( main_ui_handler != null && main_ui_handler.getLooper() != null ) {
                        main_ui_handler.post( new Runnable() {
                            @Override
                            public void run() {
                                OnFetchFailed( exception.getMessage() );
                            }
                        });
                    }
                }
            }
        });
        new_thread.start();
    }

    private void OnFetchFailed( final String message )
    {
        if( refresh_menu != null ) refresh_menu.setEnabled( true );
        loading_view.setVisibility( View.INVISIBLE );
        list_view.setEmptyView( empty_view );
        final Context fragment_context = ListSeasonsFragment.this.getContext();
        if( fragment_context == null ) return;
        
        AlertDialog dialog = new AlertDialog.Builder( fragment_context ).setTitle( "Error" ).setMessage( message )
                .setPositiveButton( android.R.string.ok, null ).create();
        dialog.show();
    }

    private ArrayList<SeasonData> ParseResultAndDisplay( final JSONObject data_object )
    {
        ArrayList<SeasonData> season_list = new ArrayList<>();
        try {
            show_description = data_object.optString( "desc" );
            JSONArray data_list = data_object.getJSONArray( "seasons" );
            for ( int i = 0; i != data_list.length(); ++i ) {
                JSONObject data_item = data_list.getJSONObject( i );
                season_list.add( new SeasonData( data_item.getString( "name" ), data_item.getLong( "id" ) ) );
            }
        } catch ( JSONException except ){
            Log.v( "ParseResult", except.getLocalizedMessage() );
        }
        return season_list;
    }

    class SeasonData
    {
        public final String season_name;
        public final Long   season_id;
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

    /*
    @Override
    public void onListItemClick( ListView l, View view, int position, long id )
    {
        SeasonData data = (( SeasonsAdapter ) list_view.getAdapter() ).getItem( position );
        Intent intent = new Intent( getActivity(), ListEpisodesActivity.class );

        intent.putExtra( ListEpisodesActivity.SEASON_NAME, show_name + " - " + data.season_name );
        intent.putExtra( ListEpisodesActivity.SEASON_ID, data.season_id );
        startActivityForResult( intent, ListSeasonsFragment.REQUEST_CODE );
        getActivity().overridePendingTransition( R.anim.push_left_in, R.anim.push_left_out );
    }
    */

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
