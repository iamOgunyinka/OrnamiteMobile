package com.froist_inc.josh.ornamite;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.TextView;

public class SettingsActivity extends AppCompatActivity
{
    public static final String CLEANUP_INTERVAL = "CLEANUP_INTERVAL";
    private static final int MAX_RANGE = 10;
    private static final int MIN_RANGE = 1;
    public static final int DEFAULT_RANGE = 7;

    private int updated_value;

    @Override
    protected void onCreate( @Nullable Bundle saved_instance_state )
    {
        super.onCreate( saved_instance_state );
        setContentView( R.layout.activity_settings );
        setTitle( R.string.action_settings );

        final SharedPreferences shared_preferences = PreferenceManager.getDefaultSharedPreferences( SettingsActivity.this );
        int initial_value = shared_preferences.getInt( CLEANUP_INTERVAL, DEFAULT_RANGE );
        initial_value = initial_value >= MAX_RANGE ? MAX_RANGE : ( initial_value <= MIN_RANGE ? MIN_RANGE : initial_value );
        updated_value = initial_value;

        final SeekBar cleanup_bar = ( SeekBar ) findViewById( R.id.seekBar );
        cleanup_bar.setMax( MAX_RANGE );

        final TextView cleanup_text = ( TextView ) findViewById( R.id.cleanup_text_view );
        cleanup_text.setText( getString( R.string.cleanup_text, updated_value));

        cleanup_bar.setProgress(initial_value);
        cleanup_bar.setOnSeekBarChangeListener( new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged( SeekBar seekBar, int progress, boolean from_user ) {
                updated_value = ( progress <= MIN_RANGE ) ? MIN_RANGE : progress;
                cleanup_text.setText( getString( R.string.cleanup_text, updated_value ));
                shared_preferences.edit().putInt( CLEANUP_INTERVAL, updated_value ).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        Toolbar settings_toolbar = ( Toolbar )findViewById( R.id.settings_toolbar );
        setSupportActionBar( settings_toolbar );

        getSupportActionBar().setHomeButtonEnabled( true );
        getSupportActionBar().setDisplayHomeAsUpEnabled( true );
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        if( item.getItemId() == android.R.id.home ){
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected( item );
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        this.finish();
        overridePendingTransition( R.anim.push_left_in, R.anim.push_left_out );
    }
}
