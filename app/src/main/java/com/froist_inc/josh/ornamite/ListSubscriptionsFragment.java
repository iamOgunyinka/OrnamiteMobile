package com.froist_inc.josh.ornamite;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;


public class ListSubscriptionsFragment extends ListFragment
{
    @Override
    public void onCreate( @Nullable Bundle saved_instance_state )
    {
        super.onCreate( saved_instance_state );
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try {
            Utilities.WriteTvSeriesData( getActivity(), NetworkManager.ALL_SERIES_FILENAME, Utilities.AllSeries );
        } catch ( JSONException | IOException except ){
            Log.v( "ListSubscriptionFrag", except.getLocalizedMessage() );
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        ReadSeriesSource();
        setEmptyText( "You have not subscribed to any series yet" );
    }

    private void ReadSeriesSource()
    {
        if( Utilities.AllSeries == null || Utilities.AllSeries.isEmpty() ) {
            AllSeriesFragment.ListSeriesAsyncTask series_source_reader_task = new AllSeriesFragment.ListSeriesAsyncTask(
                    this.getContext(), new AllSeriesFragment.ListSeriesAsyncTask.PostResultAction() {
                @Override
                public void OnErrorCallback() {
                    setListAdapter(null);
                }

                @Override
                public void OnSuccessCallback( final Context context,
                                               final HashMap<String, Utilities.TvSeriesData> results ){
                    Utilities.AllSeries = results;
                    SetAdapter( context, Utilities.AllSeries );
                }
            });
            series_source_reader_task.execute();
        } else {
            SetAdapter( this.getContext(), Utilities.AllSeries );
        }
    }

    private void SetAdapter( final Context context, final HashMap<String, Utilities.TvSeriesData> all_series )
    {
        final ArrayList<String> subscribed = new ArrayList<>();
        for ( HashMap.Entry <String, Utilities.TvSeriesData> data_entry: all_series.entrySet() ) {
            final Utilities.TvSeriesData value = data_entry.getValue();
            if (value.IsSubscribed()) subscribed.add( data_entry.getKey() );
        }
        setListAdapter( new SubscriptionsAdapter( context, subscribed ) );
    }

    private class SubscriptionsAdapter extends ArrayAdapter<String>
    {
        private Context context;

        public SubscriptionsAdapter( Context context, ArrayList<String> data_list )
        {
            super( context, 0, data_list );
            this.context = context;
        }

        @Override
        public View getView( int position, View convert_view, ViewGroup parent )
        {
            if( convert_view == null ){
                LayoutInflater inflater = ( LayoutInflater ) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
                convert_view = inflater.inflate( R.layout.list_subscriptions_item, parent, false );
            }
            final Utilities.TvSeriesData data_item = Utilities.AllSeries.get( getItem( position ) );
            TextView series_name_text = ( TextView ) convert_view.findViewById( R.id.series_name_text );
            series_name_text.setText( data_item.GetSeriesName() );
            Button unsubscribe = (Button) convert_view.findViewById( R.id.unsubscribe_button );
            unsubscribe.setText( R.string.unsubscribe );
            unsubscribe.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick( View v )
                {
                    data_item.SetIsSubscribed( !data_item.IsSubscribed() );
                    SubscriptionsAdapter.this.remove( data_item.GetSeriesName() );
                    SubscriptionsAdapter.this.notifyDataSetChanged();
                }
            });

            return convert_view;
        }
    }
}
