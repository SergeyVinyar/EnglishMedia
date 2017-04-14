package ru.vinyarsky.englishmedia;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import ru.vinyarsky.englishmedia.db.Episode;
import ru.vinyarsky.englishmedia.db.Podcast;

public class EpisodeListFragment extends Fragment {

    private static final String PODCAST_CODE_ARG = "podcastCode";

    private OnEpisodeListFragmentListener mListener;

    public EpisodeListFragment() {
    }

    public static EpisodeListFragment newInstance(UUID podcastCode) {
        EpisodeListFragment fragment =  new EpisodeListFragment();

        Bundle args = new Bundle();
        args.putString(PODCAST_CODE_ARG, podcastCode.toString());

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        EMApplication app = (EMApplication)getActivity().getApplication();
        View view =  inflater.inflate(R.layout.fragment_episodelist, container, false);
        UUID podcastCode = UUID.fromString(getArguments().getString(PODCAST_CODE_ARG));
        ListView listView = (ListView)view.findViewById(R.id.listview_fragment_episodelist);

        // Podcast header
        Observable.just(podcastCode)
                .subscribeOn(Schedulers.io())
                .map((code) -> Podcast.read(app.getDbHelper(), code))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((podcast) -> {
                    ImageView flagView = ((ImageView) view.findViewById(R.id.imageview_fragment_episodelist_flag));
                    switch (podcast.getCountry()) {
                        case UK:
                            flagView.setVisibility(View.VISIBLE);
                            flagView.setImageResource(R.drawable.flag_uk);
                            break;
                        case USA:
                            flagView.setVisibility(View.VISIBLE);
                            flagView.setImageResource(R.drawable.flag_usa);
                            break;
                        default:
                            flagView.setVisibility(View.INVISIBLE);
                            break;
                    }

                    View subscribedView = view.findViewById(R.id.textview_fragment_episodelist_subscribed);
                    if (podcast.isSubscribed())
                        subscribedView.setVisibility(View.VISIBLE);
                    else
                        subscribedView.setVisibility(View.INVISIBLE);

                    TextView levelView = ((TextView) view.findViewById(R.id.textview_fragment_episodelist_level));
                    levelView.setText(podcast.getLevel().toString());
                    switch (podcast.getLevel()) {
                        case BEGINNER:
                            levelView.setTextColor(getResources().getColor(R.color.beginner));
                            break;
                        case INTERMEDIATE:
                            levelView.setTextColor(getResources().getColor(R.color.intermediate));
                            break;
                        case ADVANCED:
                            levelView.setTextColor(getResources().getColor(R.color.advanced));
                            break;
                    }

                    ((TextView) view.findViewById(R.id.textview_fragment_episodelist_title)).setText(podcast.getTitle());

                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), Uri.parse(podcast.getImagePath()));
                        ((ImageView) view.findViewById(R.id.imageview_fragment_episodelist)).setImageBitmap(bitmap);
                    } catch (IOException e) {
                        // OK, no image then...
                    }
                });

        // List of episodes
        Observable.fromFuture(app.getRssFetcher().fetchEpisodesAsync(Collections.singletonList(podcastCode)))
                .subscribeOn(Schedulers.io())
                .map((numOfFetchedEpisodes) -> Episode.readAllByPodcastCode(app.getDbHelper(), podcastCode))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((cursor) -> {
                    listView.setAdapter(new CursorAdapter(getContext(), cursor, false) {

                        @Override
                        public View newView(Context context, Cursor cursor, ViewGroup parent) {
                            return ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.item_episode, parent, false);
                        }

                        @Override
                        public void bindView(View view, Context context, Cursor cursor) {
                            ((TextView)view.findViewById(R.id.textview_item_episode_title)).setText(cursor.getString(cursor.getColumnIndex(Episode.TITLE)));
                            ((TextView)view.findViewById(R.id.textview_item_episode_description)).setText(cursor.getString(cursor.getColumnIndex(Episode.DESCRIPTION)));

                            ((Button)view.findViewById(R.id.button_item_episode_play)).setOnClickListener((v) -> {
                                if (mListener != null) {
                                    View parentRow = (View) v.getParent();
                                    ListView listView1 = (ListView) parentRow.getParent();
                                    int position = listView.getPositionForView(parentRow);
                                    Cursor c = (Cursor) listView.getItemAtPosition(position);
                                    mListener.onPlayEpisode(podcastCode, c.getString(cursor.getColumnIndex(Episode.CONTENT_URL)));
                                }
                            });
                        }
                    });
                });

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnEpisodeListFragmentListener) {
            mListener = (OnEpisodeListFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement " + OnEpisodeListFragmentListener.class.getSimpleName());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    public interface OnEpisodeListFragmentListener {
        void onPlayEpisode(UUID podcastCode, String url);
    }
}
