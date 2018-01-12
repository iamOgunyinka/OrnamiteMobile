package com.froist_inc.josh.ornamite;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

public class ListSeasonsActivity extends AppCompatActivity
{
    public static final String SHOW_NAME = "SHOW_NAME";
    public static final String SHOW_ID = "SHOW_ID";

    private String show_name;
    private long show_id;

    @Override
    protected void onCreate( @Nullable Bundle saved_instance_state )
    {
        super.onCreate( saved_instance_state );

        setContentView( R.layout.list_seasons_layout );
        Intent starting_intent = getIntent();
        show_name = starting_intent.getStringExtra( SHOW_NAME );
        show_id = starting_intent.getLongExtra( SHOW_ID, -1 );
        SetViewFragment();
    }

    private void SetViewFragment()
    {
        FragmentManager fragment_manager = getSupportFragmentManager();
        Fragment fragment = fragment_manager.findFragmentById( R.id.dummy_view );
        if( fragment == null ){
            fragment = ListSeasonsFragment.GetFragmentInstance( show_name, show_id );
        }
        fragment_manager.beginTransaction().add( R.id.dummy_view, fragment ).commit();
    }
}
