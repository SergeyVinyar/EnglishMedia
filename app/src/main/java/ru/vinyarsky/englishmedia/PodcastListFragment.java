package ru.vinyarsky.englishmedia;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.collection.ArraySet;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Space;

import com.annimon.stream.function.Supplier;
import com.squareup.picasso.Picasso;

import java.util.Set;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import ru.vinyarsky.englishmedia.db.DbHelper;
import ru.vinyarsky.englishmedia.db.Podcast;

public class PodcastListFragment extends Fragment {

    private static final String PODCAST_LEVEL_ARG = "podcast_level";

    private static final String PODCAST_LEVEL_ARG_ALL_VALUE = "all";

    private DbHelper dbHelper;

    private OnPodcastListFragmentListener mListener;

    private CompositeDisposable compositeDisposable;

    @BindView(R.id.recyclerview_fragment_podcastlist)
    RecyclerView recyclerView;

    public PodcastListFragment() {
        this.dbHelper = EMApplication.getEmComponent().getDbHelper();
    }

    /**
     * Show podcasts with given podcastLevel
     * @param podcastLevel show all podcasts if null
     */
    public static PodcastListFragment newInstance(@Nullable Podcast.PodcastLevel podcastLevel) {
        {
            Bundle bundle = new Bundle();
            bundle.putString("podcast_level", podcastLevel != null ? podcastLevel.toString() : "all");
            EMApplication.getEmComponent().getFirebaseAnalytics().logEvent("show_podcast_list", bundle);
        }

        PodcastListFragment fragment = new PodcastListFragment();

        Bundle args = new Bundle();
        args.putString(PODCAST_LEVEL_ARG, podcastLevel != null ? podcastLevel.name() : PODCAST_LEVEL_ARG_ALL_VALUE);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        compositeDisposable = new CompositeDisposable();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_podcastlist, container, false);
        ButterKnife.bind(this, view);

        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mListener.showProgress();
        try {
            compositeDisposable.add(
                    Observable.<Podcast[]>create((emitter) -> {
                        Podcast[] podcasts;

                        String podcastLevelName = getArguments().getString(PODCAST_LEVEL_ARG);
                        if (PODCAST_LEVEL_ARG_ALL_VALUE.equals(podcastLevelName))
                            podcasts = Podcast.readAll(PodcastListFragment.this.dbHelper);
                        else
                            podcasts = Podcast.readAllByPodcastLevel(PodcastListFragment.this.dbHelper, Podcast.PodcastLevel.valueOf(podcastLevelName));

                        emitter.onNext(podcasts);
                        emitter.onComplete();
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((podcastList) -> {
                        recyclerView.setAdapter(new RecyclerViewAdapter(podcastList));
                        mListener.hideProgress();
                    }));
        } catch (Throwable e) {
            mListener.hideProgress();
            throw e;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnPodcastListFragmentListener) {
            mListener = (OnPodcastListFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement " + OnPodcastListFragmentListener.class.getSimpleName());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        String podcastLevelName = getArguments().getString(PODCAST_LEVEL_ARG);
        if (podcastLevelName != null && !PODCAST_LEVEL_ARG_ALL_VALUE.equals(podcastLevelName))
            mListener.setTitle(podcastLevelName.toLowerCase());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        compositeDisposable.dispose();
    }

    public class RecyclerViewAdapter extends RecyclerView.Adapter<ViewHolder> {

        private Podcast[] podcasts;
        private Set<Integer> expandedPositions = new ArraySet<>();

        public RecyclerViewAdapter(Podcast[] podcasts) {
            this.podcasts = podcasts;
            this.expandedPositions = new ArraySet<>(podcasts.length);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_podcast, parent, false);
            return new ViewHolder(v, () -> podcasts, this);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            // No separator at the first item
            if (position != 0)
                holder.separatorView.setVisibility(View.VISIBLE);
            else
                holder.separatorView.setVisibility(View.GONE);

            Podcast podcast = podcasts[position];

            switch (podcast.getCountry()) {
                case UK:
                    Picasso.get()
                            .load(R.drawable.flag_uk)
                            .into(holder.flagView);
                    holder.flagView.setVisibility(View.VISIBLE);
                    break;
                case US:
                    Picasso.get()
                            .load(R.drawable.flag_us)
                            .into(holder.flagView);
                    holder.flagView.setVisibility(View.VISIBLE);
                    break;
                case CZ:
                    Picasso.get()
                            .load(R.drawable.flag_cz)
                            .into(holder.flagView);
                    holder.flagView.setVisibility(View.VISIBLE);
                    break;
                case DK:
                    Picasso.get()
                            .load(R.drawable.flag_dk)
                            .into(holder.flagView);
                    holder.flagView.setVisibility(View.VISIBLE);
                    break;
                default:
                    holder.flagView.setVisibility(View.INVISIBLE);
                    break;
            }

            if (podcast.isSubscribed())
                holder.subscribedView.setVisibility(View.VISIBLE);
            else
                holder.subscribedView.setVisibility(View.INVISIBLE);

            holder.levelView.setText(podcast.getLevel().toString());
            switch (podcast.getLevel()) {
                case BEGINNER:
                    holder.levelView.setTextColor(getResources().getColor(R.color.beginner));
                    break;
                case INTERMEDIATE:
                    holder.levelView.setTextColor(getResources().getColor(R.color.intermediate));
                    break;
                case ADVANCED:
                    holder.levelView.setTextColor(getResources().getColor(R.color.advanced));
                    break;
            }

            holder.titleView.setText(podcast.getTitle());
            holder.descriptionView.setText(Html.fromHtml(podcast.getDescription()));
            holder.descriptionView.setLinkTextColor(holder.descriptionView.getCurrentTextColor());

            if (expandedPositions.contains(position)) {
                holder.descriptionView.setMaxLines(50);
                holder.moreView.setVisibility(View.GONE);
            }
            else {
                holder.descriptionView.setMaxLines(1);
                holder.moreView.setVisibility(View.VISIBLE);
            }

            // Hiding "more..." if a description fits to one line
            holder.descriptionView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    boolean notExpanded = !expandedPositions.contains(position);
                    boolean noEllipsis = holder.descriptionView.getLayout().getEllipsisCount(0) == 0;
                    if (notExpanded && noEllipsis)
                        holder.moreView.setVisibility(View.GONE);
                    holder.descriptionView.getViewTreeObserver().removeOnPreDrawListener(this);
                    return true;
                }
            });

            Picasso.get()
                    .load(podcast.getImagePath())
                    .into(holder.podcastImageView);

            // Add empty space at the bottom to the last item otherwise item hides behind
            // player control view.
            if (position == podcasts.length - 1)
                holder.bottomSpaceView.setVisibility(View.VISIBLE);
            else
                holder.bottomSpaceView.setVisibility(View.GONE);
        }

        @Override
        public int getItemCount() {
            return podcasts.length;
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.imageview_item_podcast_separator) ImageView separatorView;
        @BindView(R.id.constraintlayout_item_podcast) ConstraintLayout constraintView;
        @BindView(R.id.imageView_item_podcast_flag) ImageView flagView;
        @BindView(R.id.textview_item_podcast_subscribed) TextView subscribedView;
        @BindView(R.id.textview_item_podcast_level) TextView levelView;
        @BindView(R.id.textview_item_podcast_title) TextView titleView;
        @BindView(R.id.textview_item_podcast_description) TextView descriptionView;
        @BindView(R.id.textview_item_podcast_more) TextView moreView;
        @BindView(R.id.imageview_item_podcast) ImageView podcastImageView;
        @BindView(R.id.imageview_item_podcast_bottomspace) Space bottomSpaceView;

        ViewHolder(View itemView, final Supplier<Podcast[]> getPodcasts, RecyclerViewAdapter adapter) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            View.OnClickListener expandListener = (v) -> {
                Integer position = getAdapterPosition();
                if (adapter.expandedPositions.contains(position))
                    adapter.expandedPositions.remove(position);
                else
                    adapter.expandedPositions.add(position);
                adapter.notifyItemChanged(position);
            };

            constraintView.setOnClickListener((view) -> {
                if (mListener != null) {
                    Podcast podcast = getPodcasts.get()[getAdapterPosition()];
                    mListener.onSelectPodcast(podcast.getCode());
                }
            });

            //descriptionView.setOnClickListener(expandListener);
            moreView.setOnClickListener(expandListener);
        }
    }

    public interface OnPodcastListFragmentListener {
        void onSelectPodcast(UUID podcastCode);
        void setTitle(String title);

        void showProgress();
        void hideProgress();
    }
}
