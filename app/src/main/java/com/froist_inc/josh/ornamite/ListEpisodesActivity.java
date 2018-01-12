package com.froist_inc.josh.ornamite;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;


public class ListEpisodesActivity extends AppCompatActivity
{
    public static final String SEASON_NAME = "SEASON_NAME";
    public static final String SEASON_ID = "SEASON_ID";

    @Override
    protected void onCreate( @Nullable Bundle saved_instance_state )
    {
        super.onCreate( saved_instance_state );
        setContentView( R.layout.list_episodes_layout );
        Intent starting_intent = getIntent();
        final String title = starting_intent.getStringExtra( SEASON_NAME );
        final long   season_id = starting_intent.getLongExtra( SEASON_ID, -1 );
        StartFragment( title, season_id );
    }

    private void StartFragment( final String title, final long season_id )
    {
        FragmentManager fragment_manager = getSupportFragmentManager();
        Fragment fragment = fragment_manager.findFragmentById( R.id.episode_parent_view );
        if( fragment == null ){
            fragment = ListEpisodesFragment.GetInstance( title, season_id );
        }
        fragment_manager.beginTransaction().replace( R.id.episode_parent_view, fragment ).commit();
    }
}
