package com.froist_inc.josh.ornamite;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

public class MainActivity extends AppCompatActivity
{

    @Override
    protected void onCreate( Bundle saved_instance_bundle )
    {
        super.onCreate( saved_instance_bundle );
        setContentView( R.layout.activity_main );

        Toolbar toolbar = ( Toolbar ) findViewById( R.id.toolbar );
        setSupportActionBar( toolbar );
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        SectionsPagerAdapter sections_pager_adapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        ViewPager view_pager = (ViewPager) findViewById(R.id.container);
        assert view_pager != null;
        view_pager.setAdapter(sections_pager_adapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        assert tabLayout != null;
        tabLayout.setupWithViewPager(view_pager);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter
    {
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem( int position )
        {
            switch ( position ) {
                case 0:
                    return new AllSeriesFragment();
                case 1:
                    return new UpdatesFragment();
                case 2: default:
                    return new ListSubscriptionsFragment();
            }
        }

        @Override
        public int getCount()
        {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle( final int position )
        {
            switch ( position ) {
                case 0:
                    return "All Series";
                case 1:
                    return "Updates";
                case 2:
                    return "Subscriptions";
            }
            return null;
        }
    }
}
