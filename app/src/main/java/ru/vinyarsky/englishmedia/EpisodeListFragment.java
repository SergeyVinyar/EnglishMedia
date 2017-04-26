package ru.vinyarsky.englishmedia;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.util.ArraySet;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Space;
import android.widget.TextView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import ru.vinyarsky.englishmedia.db.Episode;
import ru.vinyarsky.englishmedia.db.Podcast;

import static java.text.DateFormat.getTimeInstance;

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
        UUID podcastCode = UUID.fromString(getArguments().getString(PODCAST_CODE_ARG));

        RecyclerView recyclerView = (RecyclerView) inflater.inflate(R.layout.fragment_episodelist, container, false);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        Observable<Podcast> podcastObservable = Observable.just(podcastCode)
                .subscribeOn(Schedulers.io())
                .map((code) -> Podcast.read(app.getDbHelper(), code))
                .observeOn(AndroidSchedulers.mainThread());

        Observable<Cursor> episodesObservable = Observable.fromFuture(app.getRssFetcher().fetchEpisodesAsync(Collections.singletonList(podcastCode)))
                .subscribeOn(Schedulers.io())
                .map((numOfFetchedEpisodes) -> Episode.readAllByPodcastCode(app.getDbHelper(), podcastCode))
                .observeOn(AndroidSchedulers.mainThread());

        Observable
                .zip(podcastObservable, episodesObservable, (podcast, episodesCursor) -> new EpisodeListFragment.RecyclerViewAdapter(podcast, episodesCursor))
                .subscribe(adapter -> recyclerView.setAdapter(adapter));

        return recyclerView;
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

    private class RecyclerViewAdapter extends RecyclerView.Adapter {

        private final static int PODCAST_HEADER_VIEWTYPE = 0;
        private final static int EPISODE_VIEWTYPE = 1;

        private Podcast podcast;
        private Cursor episodesCursor;
        private Set<Integer> expandedPositions = new ArraySet<>();

        RecyclerViewAdapter(Podcast podcast, Cursor episodesCursor) {
            this.podcast = podcast;
            this.episodesCursor = episodesCursor;
            this.expandedPositions = new ArraySet<>(episodesCursor.getCount());
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? PODCAST_HEADER_VIEWTYPE : EPISODE_VIEWTYPE;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == PODCAST_HEADER_VIEWTYPE) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_podcast_header, parent, false);
                return new PodcastHeaderViewHolder(v, podcast);
            }
            else { // EPISODE_VIEWTYPE
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_episode, parent, false);
                return new EpisodeViewHolder(v, episodesCursor, this);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (position == 0) { // Podcast header
                PodcastHeaderViewHolder podcastHeaderViewHolder = (PodcastHeaderViewHolder) holder;

                switch (podcast.getCountry()) {
                    case UK:
                        podcastHeaderViewHolder.flagView.setVisibility(View.VISIBLE);
                        podcastHeaderViewHolder.flagView.setImageResource(R.drawable.flag_uk);
                        break;
                    case USA:
                        podcastHeaderViewHolder.flagView.setVisibility(View.VISIBLE);
                        podcastHeaderViewHolder.flagView.setImageResource(R.drawable.flag_usa);
                        break;
                    default:
                        podcastHeaderViewHolder.flagView.setVisibility(View.INVISIBLE);
                        break;
                }

                if (podcast.isSubscribed())
                    podcastHeaderViewHolder.subscribedView.setVisibility(View.VISIBLE);
                else
                    podcastHeaderViewHolder.subscribedView.setVisibility(View.INVISIBLE);

                podcastHeaderViewHolder.levelView.setText(podcast.getLevel().toString());
                switch (podcast.getLevel()) {
                    case BEGINNER:
                        podcastHeaderViewHolder.levelView.setTextColor(getResources().getColor(R.color.beginner));
                        break;
                    case INTERMEDIATE:
                        podcastHeaderViewHolder.levelView.setTextColor(getResources().getColor(R.color.intermediate));
                        break;
                    case ADVANCED:
                        podcastHeaderViewHolder.levelView.setTextColor(getResources().getColor(R.color.advanced));
                        break;
                }

                podcastHeaderViewHolder.titleView.setText(podcast.getTitle());

                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), Uri.parse(podcast.getImagePath()));
                    podcastHeaderViewHolder.podcastImageView.setImageBitmap(bitmap);
                } catch (IOException e) {
                    // OK, no image then...
                }
            }
            else { // Some episode
                EpisodeViewHolder episodeViewHolder = (EpisodeViewHolder) holder;

                position--; // Because podcast header is always at position 0

                episodesCursor.move(position - episodesCursor.getPosition());

                Episode episode = new Episode(episodesCursor);

                // No separator at the first item
                if (episodesCursor.getPosition() != 0)
                    episodeViewHolder.separatorView.setVisibility(View.VISIBLE);
                else
                    episodeViewHolder.separatorView.setVisibility(View.GONE);

                switch (episode.getStatus()) {
                    case NEW:
                        episodeViewHolder.statusView.setImageResource(R.drawable.episode_status_new);
                        episodeViewHolder.statusView.setVisibility(View.VISIBLE);
                        break;
                    case LISTENING:
                        episodeViewHolder.statusView.setImageResource(R.drawable.episode_status_listening);
                        episodeViewHolder.statusView.setVisibility(View.VISIBLE);
                        break;
                    case COMPLETED:
                        episodeViewHolder.statusView.setVisibility(View.GONE);
                        break;
                }

                episodeViewHolder.titleView.setText(episode.getTitle());

                SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
                episodeViewHolder.pubDateView.setText(dateFormat.format(episode.getPubDate()));

                SimpleDateFormat timeFormat;
                if (episode.getDuration() >= 60 * 60)
                    timeFormat = new SimpleDateFormat("HH:mm:ss");
                else
                    timeFormat = new SimpleDateFormat("mm:ss");
                timeFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                if (episode.getCurrentPosition() == 0) {
                    episodeViewHolder.durationView.setText(timeFormat.format(((long) episode.getDuration()) * 1000));
                }
                else {
                    episodeViewHolder.durationView.setText("Remained " + timeFormat.format(((long) episode.getDuration() - episode.getCurrentPosition()) * 1000)); // TODO Move to resource
                }

                episodeViewHolder.descriptionView.setText(episode.getDescription());

                if (expandedPositions.contains(episodesCursor.getPosition())) {
                    episodeViewHolder.descriptionView.setMaxLines(50);
                    episodeViewHolder.moreView.setVisibility(View.GONE);
                }
                else {
                    episodeViewHolder.descriptionView.setMaxLines(1);
                    episodeViewHolder.moreView.setVisibility(View.VISIBLE);
                }

                // Add empty space at the bottom to the last item otherwise this item hides behind
                // player control view
                if (episodesCursor.getPosition() == episodesCursor.getCount() - 1)
                    episodeViewHolder.bottomSpaceView.setVisibility(View.VISIBLE);
                else
                    episodeViewHolder.bottomSpaceView.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return episodesCursor.getCount() + 1; // + podcast header at position 0
        }
    }

    private class EpisodeViewHolder extends RecyclerView.ViewHolder {

        private ImageView separatorView;
        private ConstraintLayout constraintView;
        private ImageView statusView;
        private TextView titleView;
        private TextView pubDateView;
        private TextView durationView;
        private TextView descriptionView;
        private TextView moreView;
        private Space bottomSpaceView;

        EpisodeViewHolder(View itemView, Cursor cursor, RecyclerViewAdapter adapter) {
            super(itemView);

            separatorView = ((ImageView)itemView.findViewById(R.id.imageview_item_episode_separator));
            constraintView = ((ConstraintLayout) itemView.findViewById(R.id.constraintlayout_item_episode));
            statusView = ((ImageView)itemView.findViewById(R.id.imageView_item_episode_status));
            titleView = ((TextView)itemView.findViewById(R.id.textview_item_episode_title));
            descriptionView = ((TextView)itemView.findViewById(R.id.textview_item_episode_description));
            pubDateView = ((TextView)itemView.findViewById(R.id.textview_item_episode_pubdate));
            durationView = ((TextView)itemView.findViewById(R.id.textview_item_episode_duration));
            moreView = ((TextView)itemView.findViewById(R.id.textview_item_episode_more));
            bottomSpaceView = ((Space)itemView.findViewById(R.id.imageview_item_episode_bottomspace));

            View.OnClickListener expandListener = (v) -> {
                Integer position = getAdapterPosition() - 1; // Because podcast header is always at position 0
                if (adapter.expandedPositions.contains(position))
                    adapter.expandedPositions.remove(position);
                else
                    adapter.expandedPositions.add(position);
                adapter.notifyItemChanged(position + 1);
            };

            constraintView.setOnClickListener((view) -> {
                if (mListener != null) {
                    cursor.moveToPosition(getAdapterPosition() - 1);
                    UUID code = UUID.fromString(cursor.getString(cursor.getColumnIndex(Episode.CODE)));
                    mListener.onPlayPauseEpisode(code);
                }
            });

            //descriptionView.setOnClickListener(expandListener);
            moreView.setOnClickListener(expandListener);
        }
    }

    private class PodcastHeaderViewHolder extends RecyclerView.ViewHolder {

        private ImageView flagView;
        private View subscribedView;
        private TextView levelView;
        private TextView titleView;
        private ImageView podcastImageView;

        PodcastHeaderViewHolder(View itemView, Podcast podcast) {
            super(itemView);

            flagView = ((ImageView)itemView.findViewById(R.id.imageview_item_podcast_header_flag));
            subscribedView = itemView.findViewById(R.id.textview_item_podcast_header_subscribed);
            levelView = ((TextView)itemView.findViewById(R.id.textview_item_podcast_header_level));
            titleView = ((TextView)itemView.findViewById(R.id.textview_item_podcast_header_title));
            podcastImageView = ((ImageView)itemView.findViewById(R.id.imageview_item_podcast_header));
        }
    }

    public interface OnEpisodeListFragmentListener {
        void onPlayPauseEpisode(UUID episodeCode);
    }
}
