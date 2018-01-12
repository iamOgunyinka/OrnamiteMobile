package com.froist_inc.josh.ornamite;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;


public class UpdatesFragment extends Fragment
{
    private MenuItem refresh_menu;
    private ExpandableListView root_list_view;
    private View overlay_view;

    @Override
    public void onCreate( @Nullable Bundle saved_instance_state )
    {
        super.onCreate( saved_instance_state );
        setHasOptionsMenu( true );
    }

    @Override
    public void onCreateOptionsMenu( Menu menu, MenuInflater inflater )
    {
        super.onCreateOptionsMenu( menu, inflater );
        inflater.inflate( R.menu.other_menu, menu );
        refresh_menu = menu.findItem( R.id.action_refresh );
    }

    @Override
    public void onResume()
    {
        super.onResume();
        LoadUpdates();
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

        TextView empty_view = new TextView( getActivity() );
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

        // TODO: 12-Jan-18 Add meaningful onPostExecute action
        @Override
        protected void onPostExecute( final HashMap<String, ArrayList<Utilities.EpisodeData>> data_map )
        {
            super.onPostExecute( data_map );
        }
    }
}
