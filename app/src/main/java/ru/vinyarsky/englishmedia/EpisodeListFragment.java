package ru.vinyarsky.englishmedia;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Space;
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

        RecyclerView recyclerView = (RecyclerView)view.findViewById(R.id.recyclerview_fragment_episodelist);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

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
                    recyclerView.setAdapter(new EpisodeListFragment.RecyclerViewAdapter(cursor));
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

    public class RecyclerViewAdapter extends RecyclerView.Adapter<ViewHolder> {

        private Cursor cursor;

        public RecyclerViewAdapter(Cursor cursor) {
            this.cursor = cursor;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_episode, parent, false);
            return new ViewHolder(v, cursor);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            cursor.move(position - cursor.getPosition());

            // No separator at the first item
            if (cursor.getPosition() != 0)
                holder.separatorView.setVisibility(View.VISIBLE);
            else
                holder.separatorView.setVisibility(View.GONE);

            holder.titleView.setText(cursor.getString(cursor.getColumnIndex(Episode.TITLE)));
            holder.descriptionView.setText(cursor.getString(cursor.getColumnIndex(Episode.DESCRIPTION)));

            // Add empty space at the bottom to the last item otherwise item hides behind
            // player control view.
            if (cursor.getPosition() == cursor.getCount() - 1)
                holder.bottomSpaceView.setVisibility(View.VISIBLE);
            else
                holder.bottomSpaceView.setVisibility(View.GONE);
        }

        @Override
        public int getItemCount() {
            return cursor.getCount();
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private Cursor cursor;

        private ImageView separatorView;
        private TextView titleView;
        private TextView descriptionView;
        private Button playButtonView;
        private Space bottomSpaceView;

        ViewHolder(View itemView, Cursor cursor) {
            super(itemView);

            this.cursor = cursor;

            separatorView = ((ImageView)itemView.findViewById(R.id.imageview_item_episode_separator));
            titleView = ((TextView)itemView.findViewById(R.id.textview_item_episode_title));
            descriptionView = ((TextView)itemView.findViewById(R.id.textview_item_episode_description));
            playButtonView = (Button)itemView.findViewById(R.id.button_item_episode_play);
            bottomSpaceView = ((Space)itemView.findViewById(R.id.imageview_item_episode_bottomspace));

            playButtonView.setOnClickListener((view) -> {
                if (mListener != null) {
                    cursor.moveToPosition(getAdapterPosition());
                    UUID podcastCode = UUID.fromString(cursor.getString(cursor.getColumnIndex(Episode.PODCAST_CODE)));
                    mListener.onPlayEpisode(podcastCode, cursor.getString(cursor.getColumnIndex(Episode.CONTENT_URL)));
                }
            });
        }
    }

    public interface OnEpisodeListFragmentListener {
        void onPlayEpisode(UUID podcastCode, String url);
    }
}
