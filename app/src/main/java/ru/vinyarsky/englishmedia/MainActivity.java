package ru.vinyarsky.englishmedia;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import ru.vinyarsky.englishmedia.EMPlaybackControlView;

import java.util.UUID;

import ru.vinyarsky.englishmedia.media.MediaService;

public class MainActivity extends AppCompatActivity
        implements
            NavigationView.OnNavigationItemSelectedListener,
            PodcastListFragment.OnPodcastListFragmentListener,
            EpisodeListFragment.OnEpisodeListFragmentListener {

    private ServiceConnection mediaServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MainActivity.this.mediaServiceBinder = (MediaService.MediaServiceBinder) service;

            EMPlaybackControlView controlView = (EMPlaybackControlView) findViewById(R.id.playbackcontrolview_layout_main_appbar);
            MainActivity.this.mediaServiceBinder.mountPlaybackControlView(controlView);
            controlView.setShowTimeoutMs(-1);
            controlView.show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            EMPlaybackControlView controlView = (EMPlaybackControlView) findViewById(R.id.playbackcontrolview_layout_main_appbar);
            MainActivity.this.mediaServiceBinder.unMountPlaybackControlView(controlView);

            MainActivity.this.mediaServiceBinder = null;
        }
    };
    private MediaService.MediaServiceBinder mediaServiceBinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_layout_main_appbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.activity_main);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.main_navigation_drawer_open, R.string.main_navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.navview_main);
        navigationView.setNavigationItemSelectedListener(this);

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.framelayout_layout_main_appbar_fragment);
        if (fragment == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.framelayout_layout_main_appbar_fragment, PodcastListFragment.newInstance())
                    .commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, MediaService.class), this.mediaServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(this.mediaServiceConnection);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.activity_main);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.activity_main);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onSelectPodcast(UUID podcastCode) {
        Fragment oldFragment = getSupportFragmentManager().findFragmentById(R.id.framelayout_layout_main_appbar_fragment);;
        Fragment newFragment = EpisodeListFragment.newInstance(podcastCode);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if (oldFragment != null)
            transaction.remove(oldFragment);

        transaction
                .add(R.id.framelayout_layout_main_appbar_fragment, newFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onPlayPauseEpisode(UUID episodeCode) {
        Intent intent = MediaService.newPlayPauseToggleIntent(getApplicationContext(), episodeCode);
        startService(intent);
    }
}
