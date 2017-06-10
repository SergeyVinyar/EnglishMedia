package ru.vinyarsky.englishmedia;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.google.firebase.crash.FirebaseCrash;

import java.util.UUID;

import ru.vinyarsky.englishmedia.db.Podcast;
import ru.vinyarsky.englishmedia.media.MediaService;

public class MainActivity extends AppCompatActivity
        implements
            NavigationView.OnNavigationItemSelectedListener,
            PodcastListFragment.OnPodcastListFragmentListener,
            EpisodeListFragment.OnEpisodeListFragmentListener {

    private static final String PODCASTLEVEL_PREFERENCE = "podcast_level";

    private static final String PODCASTLEVEL_PREFERENCE_ALL_VALUE = "all";

    private ServiceConnection mediaServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MainActivity.this.mediaServiceBinder = (MediaService.MediaServiceBinder) service;

            EMPlaybackControlView controlView = (EMPlaybackControlView) findViewById(R.id.playbackcontrolview_layout_main_appbar);
            MainActivity.this.mediaServiceBinder.mountPlaybackControlView(controlView);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            EMPlaybackControlView controlView = (EMPlaybackControlView) findViewById(R.id.playbackcontrolview_layout_main_appbar);
            MainActivity.this.mediaServiceBinder.unMountPlaybackControlView(controlView);

            MainActivity.this.mediaServiceBinder = null;
        }
    };
    private MediaService.MediaServiceBinder mediaServiceBinder;

    private BroadcastReceiver noNetworkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Snackbar.make(findViewById(R.id.activity_main), R.string.all_no_network, Snackbar.LENGTH_SHORT).show();
        }
    };

    private BroadcastReceiver contentNotFoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Snackbar.make(findViewById(R.id.activity_main), R.string.main_activity_content_not_found, Snackbar.LENGTH_SHORT).show();
            Uri url = intent.getParcelableExtra(MediaService.EPISODE_URL_EXTRA);
            FirebaseCrash.report(new Exception(String.format("Content not found (url: %s)", url != null ? url.toString() : "null")));
        }
    };

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

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(noNetworkReceiver, new IntentFilter(MediaService.NO_NETWORK_BROADCAST_ACTION));
        broadcastManager.registerReceiver(contentNotFoundReceiver, new IntentFilter(MediaService.CONTENT_NOT_FOUND_BROADCAST_ACTION));
    }

    @Override
    public void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // if we create an initial fragment inside onCreate we wrongly get default toolbar title instead of our's

        NavigationView navigationView = (NavigationView) findViewById(R.id.navview_main);
        String podcastLevelName = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getString(PODCASTLEVEL_PREFERENCE, PODCASTLEVEL_PREFERENCE_ALL_VALUE);

        if (PODCASTLEVEL_PREFERENCE_ALL_VALUE.equals(podcastLevelName)) {
            showPodcastList(null);
            navigationView.setCheckedItem(R.id.menuitem_drawer_all);
        }
        else {
            Podcast.PodcastLevel podcastLevel = Podcast.PodcastLevel.valueOf(podcastLevelName);
            showPodcastList(podcastLevel);
            switch (podcastLevel) {
                case BEGINNER:
                    navigationView.setCheckedItem(R.id.menuitem_drawer_beginner);
                    break;
                case INTERMEDIATE:
                    navigationView.setCheckedItem(R.id.menuitem_drawer_intermediate);
                    break;
                case ADVANCED:
                    navigationView.setCheckedItem(R.id.menuitem_drawer_advanced);
                    break;
            }
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
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.unregisterReceiver(noNetworkReceiver);
        broadcastManager.unregisterReceiver(contentNotFoundReceiver);
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
    public boolean onNavigationItemSelected(MenuItem item) {
        Podcast.PodcastLevel podcastLevel;
        switch (item.getItemId()) {
            case R.id.menuitem_drawer_beginner:
                podcastLevel = Podcast.PodcastLevel.BEGINNER;
                break;
            case R.id.menuitem_drawer_intermediate:
                podcastLevel = Podcast.PodcastLevel.INTERMEDIATE;
                break;
            case R.id.menuitem_drawer_advanced:
                podcastLevel = Podcast.PodcastLevel.ADVANCED;
                break;
            case R.id.menuitem_drawer_all:
                podcastLevel = null;
                break;
            default:
                return false;
        }
        showPodcastList(podcastLevel);

        getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE)
                .edit()
                .putString(PODCASTLEVEL_PREFERENCE, podcastLevel != null ? podcastLevel.name() : PODCASTLEVEL_PREFERENCE_ALL_VALUE)
                .apply();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.activity_main);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onSelectPodcast(@NonNull UUID podcastCode) {
        showEpisodeList(podcastCode);
    }

    @Override
    public void onPlayPauseEpisode(UUID episodeCode) {
        Intent intent = MediaService.newPlayPauseToggleIntent(getApplicationContext(), episodeCode);
        startService(intent);
    }

    @Override
    public void addTabLayout(TabLayout view) {
        ((AppBarLayout) findViewById(R.id.appbarlayout_layout_main_appbar)).addView(view);
    }

    @Override
    public void removeTabLayout(TabLayout view) {
        ((AppBarLayout) findViewById(R.id.appbarlayout_layout_main_appbar)).removeView(view);
    }

    @Override
    public void setTitle(String title) {
        ((Toolbar) findViewById(R.id.toolbar_layout_main_appbar)).setTitle(getResources().getString(R.string.all_app_name) + " - " + title);
    }

    private void setDefaultTitle() {
        ((Toolbar) findViewById(R.id.toolbar_layout_main_appbar)).setTitle(getResources().getString(R.string.all_app_name));
    }

    @Override
    public void showProgress() {
        ((ProgressBar) findViewById(R.id.progressbar_layout_main_appbar)).setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgress() {
        ((ProgressBar) findViewById(R.id.progressbar_layout_main_appbar)).setVisibility(View.GONE);
    }

    /**
     * Returns id of a backstack entry for a fragment with particular tag.
     * -1 if not found.
     */
    private int getBackstackEntryIdForFragmentTag(String tag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        for (int id = 0; id < fragmentManager.getBackStackEntryCount(); id++)
            if (tag.equals(fragmentManager.getBackStackEntryAt(id).getName()))
                return id;
        return -1;
    }

    /**
     * @param podcastLevel Show all podcasts if null
     */
    private void showPodcastList(@Nullable Podcast.PodcastLevel podcastLevel) {
        setDefaultTitle();

        String fragmentTag = PodcastListFragment.class.getName();
        if (podcastLevel != null)
            fragmentTag += "_" + podcastLevel.name();

        int fragmentId = getBackstackEntryIdForFragmentTag(fragmentTag);
        if (fragmentId == -1) {
            Fragment oldFragment = getSupportFragmentManager().findFragmentById(R.id.framelayout_layout_main_appbar_fragment);;
            Fragment newFragment = PodcastListFragment.newInstance(podcastLevel);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            if (oldFragment != null)
                transaction.remove(oldFragment);

            transaction.add(R.id.framelayout_layout_main_appbar_fragment, newFragment);

            if (oldFragment != null)
                transaction.addToBackStack(fragmentTag);

            transaction.commit();
        }
        else {
            getSupportFragmentManager().popBackStackImmediate(fragmentId, 0);
        }
    }

    private void showEpisodeList(@NonNull UUID podcastCode) {
        setDefaultTitle();

        Fragment oldFragment = getSupportFragmentManager().findFragmentById(R.id.framelayout_layout_main_appbar_fragment);;
        Fragment newFragment = EpisodeListFragment.newInstance(podcastCode);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if (oldFragment != null)
            transaction.remove(oldFragment);

        transaction.add(R.id.framelayout_layout_main_appbar_fragment, newFragment);

        if (oldFragment != null)
            transaction.addToBackStack(null);

        transaction.commit();
    }
}
