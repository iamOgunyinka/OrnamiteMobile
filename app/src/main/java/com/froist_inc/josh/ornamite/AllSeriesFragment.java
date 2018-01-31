package com.froist_inc.josh.ornamite;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Filterable;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class AllSeriesFragment extends ListFragment implements SearchView.OnQueryTextListener
{
    private ArrayList<String> keys;
    private Handler main_ui_handler;
    private MenuItem refresh_menu;

    @Override
    public void onCreate( @Nullable Bundle saved_bundle_instance )
    {
        super.onCreate( saved_bundle_instance );
        ReadSourceFile();
        setHasOptionsMenu( true );
    }

    @Override
    public void onCreateOptionsMenu( Menu menu, MenuInflater inflater )
    {
        super.onCreateOptionsMenu( menu, inflater );
        inflater.inflate( R.menu.menu_main, menu );
        refresh_menu = menu.findItem( R.id.action_refresh );
        SearchManager search_manager = ( SearchManager ) getActivity().getSystemService( Context.SEARCH_SERVICE );
        MenuItem search_menu = menu.findItem( R.id.action_search );
        SearchView search_view = ( SearchView ) search_menu.getActionView();

        assert search_view != null;

        search_view.setSearchableInfo( search_manager.getSearchableInfo( getActivity().getComponentName() ));
        search_view.setSubmitButtonEnabled( true );
        search_view.setOnQueryTextListener( this );
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch( id ){
            case R.id.action_refresh:
                RefreshData();
                return true;
            case R.id.action_settings:
                DisplaySettings();
                return true;
            default:
                return super.onOptionsItemSelected( item );
        }
    }

    private void DisplaySettings()
    {
        Intent settings_intent = new Intent( AllSeriesFragment.this.getContext(), SettingsActivity.class );
        startActivity( settings_intent );
        getActivity().overridePendingTransition( R.anim.push_left_in, R.anim.push_left_out );
    }

    @Override
    public boolean onQueryTextSubmit( final String query )
    {
        return false;
    }

    @Override
    public boolean onQueryTextChange( final String new_text ){
        ((AllSeriesAdapter ) getListAdapter() ).getFilter().filter( new_text );

        if( TextUtils.isEmpty( new_text ) ){
            getListView().clearTextFilter();
        } else {
            getListView().setFilterText( new_text );
        }
        return true;
    }

    private void RefreshData()
    {
        refresh_menu.setEnabled( false );
        setListShown( false );

        Thread new_thread = new Thread( new Runnable() {
            @Override
            public void run()
            {
                try {
                    final byte[] response = NetworkManager.GetNetwork().GetData( NetworkManager.ALL_SERIES_URL );
                    JSONObject result = null;
                    if( response != null ) {
                        result = new JSONObject( new String( response ));
                    }
                    final String message = result != null ? result.getString( "detail" ) :
                            "Could not get any data from the server";
                    if( result != null && ( result.getInt("status") == Utilities.Success )){
                        JSONArray details = result.getJSONArray( "detail" );
                        ParseResultAndDisplay( details );
                        main_ui_handler.post( new Runnable() {
                            @Override
                            public void run()
                            {
                                OnFetchSuccessful();
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

    private void OnFetchFailed(final String message )
    {
        refresh_menu.setEnabled( true );
        setListShown( true );
        AlertDialog dialog = new AlertDialog.Builder( AllSeriesFragment.this.getContext() ).setTitle( "Error" )
                .setMessage( message ).setPositiveButton( android.R.string.ok, null ).create();
        dialog.show();
    }

    private void OnFetchSuccessful()
    {
        refresh_menu.setEnabled( true );
        SetAdapter( AllSeriesFragment.this.getContext() );
        setListShown( true );
    }

    @Override
    public void onListItemClick( ListView list_view, View v, int position, long id )
    {
        AllSeriesAdapter list_adapter;
        /* for weird reasons, I sometimes get a crash error with a message:
        ```AllSeriesAdapter cannot be cast to android.widget.HeaderViewListAdapter```
        so let's see if this will work.
         */
        ListAdapter generic_adapter = list_view.getAdapter();
        if( generic_adapter instanceof HeaderViewListAdapter ){
            final HeaderViewListAdapter header_adapter = ( HeaderViewListAdapter ) generic_adapter;
            list_adapter = ( AllSeriesAdapter ) header_adapter.getWrappedAdapter();
        } else {
            list_adapter = ( AllSeriesAdapter ) generic_adapter;
        }
        final String show_name = list_adapter.getItem( position );
        final Utilities.TvSeriesData data = Utilities.AllSeries.get( show_name );
        Intent intent = new Intent( AllSeriesFragment.this.getContext(), ListSeasonsActivity.class );
        intent.putExtra( ListSeasonsActivity.SHOW_NAME, show_name );
        intent.putExtra( ListSeasonsActivity.SHOW_ID, data.GetSeriesID() );
        startActivity( intent );
        getActivity().overridePendingTransition( R.anim.push_left_in, R.anim.push_left_out );
    }

    private void ParseResultAndDisplay( final JSONArray details )
    {
        final HashSet<String> subscribed = new HashSet<>();
        for ( HashMap.Entry pair : Utilities.AllSeries.entrySet() ) {
            final Utilities.TvSeriesData value = ( Utilities.TvSeriesData ) pair.getValue();
            if( value.IsSubscribed() ) subscribed.add( (String)pair.getKey() );
        }
        if( Utilities.AllSeries == null ){
            Utilities.AllSeries = new HashMap<>();
        }
        Utilities.AllSeries.clear();
        try {
            for ( int i = 0; i != details.length(); ++i) {
                final JSONObject series = details.getJSONObject( i );
                final String series_title = series.getString( "title" );
                final Long series_id = series.getLong( "id" );

                Utilities.TvSeriesData data = new Utilities.TvSeriesData( series_title, series_id,
                        subscribed.contains( series_title ) );
                Utilities.AllSeries.put( series_title, data );
            }
        } catch ( JSONException exception ){
            Log.v( "ParseResult", exception.getLocalizedMessage() );
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if( main_ui_handler == null ) main_ui_handler = new Handler( Looper.getMainLooper() );
        setEmptyText( "Nothing found, refresh to fetch more" );
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try {
            Utilities.WriteTvSeriesData(this.getContext(), Utilities.AllSeries );
        } catch ( JSONException | IOException except ){
            Log.v( "AllSeriesFragment", except.getLocalizedMessage() );
        }
    }

    private void ReadSourceFile()
    {
        ListSeriesAsyncTask task = new ListSeriesAsyncTask( AllSeriesFragment.this.getContext(),
                new ListSeriesAsyncTask.PostResultAction(){
            @Override
            public void OnErrorCallback() {
                setListAdapter( null );
            }
            @Override
            public void OnSuccessCallback( Context context, HashMap<String, Utilities.TvSeriesData> results ) {
                Utilities.AllSeries = results;
                SetAdapter( context );
            }
        });
        task.execute();
    }

    private class AllSeriesAdapter extends ArrayAdapter<String> implements Filterable
    {
        public AllSeriesAdapter( final Context context, final ArrayList<String> data_list )
        {
            super( context, 0, data_list );
        }

        @Override
        public View getView( int position, View convert_view, ViewGroup parent )
        {
            if( convert_view == null ){
                convert_view = getActivity().getLayoutInflater().inflate( R.layout.series_item_layout, parent, false );
            }
            final Utilities.TvSeriesData item = Utilities.AllSeries.get( getItem( position ));
            final TextView series_name_text = (TextView) convert_view.findViewById( R.id.tv_series_name_text );
            final TextView subcription_status_text = (TextView) convert_view.findViewById( R.id.subscription_status_text );
            final Button  sub_unsub_button = (Button) convert_view.findViewById( R.id.subscribe_unsubscribe_button );

            series_name_text.setText( item.GetSeriesName() );
            subcription_status_text.setText( item.IsSubscribed() ? R.string.subscribed : R.string.unsubscribed );
            sub_unsub_button.setText( item.IsSubscribed() ? R.string.unsubscribe : R.string.subscribe );
            sub_unsub_button.setOnClickListener( new View.OnClickListener(){
                @Override
                public void onClick( View v )
                {
                    item.SetIsSubscribed( !item.IsSubscribed() );
                    subcription_status_text.setText( item.IsSubscribed() ? R.string.subscribed : R.string.unsubscribed );
                    sub_unsub_button.setText( item.IsSubscribed() ? R.string.unsubscribe : R.string.subscribe );
                    (( AllSeriesAdapter ) getListAdapter() ).notifyDataSetChanged();
                }
            });
            return convert_view;
        }
    }

    public static class ListSeriesAsyncTask extends AsyncTask<Void, Void, HashMap<String, Utilities.TvSeriesData>>
    {
        private final Context context;
        private String error_message;
        private final PostResultAction callback_listener;

        public interface PostResultAction
        {
            void OnErrorCallback();
            void OnSuccessCallback( Context context, HashMap<String, Utilities.TvSeriesData> results);
        }

        public ListSeriesAsyncTask( final Context context, final PostResultAction listener ){
            this.context = context;
            this.callback_listener = listener;
        }

        @Override
        protected HashMap<String, Utilities.TvSeriesData> doInBackground( Void... params )
        {
            try {
                if( Utilities.AllSeries == null || Utilities.AllSeries.size() != 0 ) {
                    return Utilities.ReadTvSeriesData( context);
                }
                return Utilities.AllSeries;
            } catch ( JSONException | IOException except ){
                error_message = except.getLocalizedMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute( HashMap<String, Utilities.TvSeriesData> results )
        {
            if( results == null ){
                AlertDialog error_dialog = new AlertDialog.Builder( context )
                        .setTitle( "Error" ).setMessage( error_message )
                        .setPositiveButton( android.R.string.ok, null )
                        .create();
                error_dialog.show();
                callback_listener.OnErrorCallback();
            } else {
                callback_listener.OnSuccessCallback( context, results );
            }
        }
    }

    private void SetAdapter( final Context context )
    {
        if( keys == null ){
            keys = new ArrayList<>();
        }
        keys.clear();
        for( HashMap.Entry data_pair : Utilities.AllSeries.entrySet() ) {
            keys.add( ( String ) data_pair.getKey() );
        }

        LayoutInflater inflater = ( LayoutInflater ) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View footer_view = inflater.inflate( R.layout.list_view_footer, null, false );
        TextView series_count_text = (TextView) footer_view.findViewById( R.id.series_count_text );
        series_count_text.setText( getString( R.string.series_count, keys.size() ));
        getListView().addFooterView( footer_view );

        setListAdapter( new AllSeriesAdapter( context, keys ) );
        (( AllSeriesAdapter ) getListAdapter() ).notifyDataSetChanged();
    }
}
