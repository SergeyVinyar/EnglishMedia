package ru.vinyarsky.englishmedia;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.util.ArraySet;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Space;

import com.annimon.stream.function.Supplier;
import com.google.firebase.crash.FirebaseCrash;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

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

        RecyclerView recyclerView = (RecyclerView)view.findViewById(R.id.recyclerview_fragment_podcastlist);
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
                        RecyclerView recyclerView = (RecyclerView)view.findViewById(R.id.recyclerview_fragment_podcastlist);
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
        private LruCache<String, Bitmap> imageCache = new LruCache<>(30);

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
                    holder.flagView.setVisibility(View.VISIBLE);
                    holder.flagView.setImageResource(R.drawable.flag_uk);
                    break;
                case US:
                    holder.flagView.setVisibility(View.VISIBLE);
                    holder.flagView.setImageResource(R.drawable.flag_us);
                    break;
                case CZ:
                    holder.flagView.setVisibility(View.VISIBLE);
                    holder.flagView.setImageResource(R.drawable.flag_cz);
                    break;
                case DK:
                    holder.flagView.setVisibility(View.VISIBLE);
                    holder.flagView.setImageResource(R.drawable.flag_dk);
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

            String uriAsString = podcast.getImagePath();
            try {
                Bitmap bitmap = imageCache.get(uriAsString);
                if (bitmap == null) {
                    bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), Uri.parse(uriAsString));
                    imageCache.put(uriAsString, bitmap);
                }
                holder.podcastImageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                // OK, no image then...
                FirebaseCrash.log(uriAsString);
                FirebaseCrash.report(e);
            }

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

        private ImageView separatorView;
        private ConstraintLayout constraintView;
        private ImageView flagView;
        private View subscribedView;
        private TextView levelView;
        private TextView titleView;
        private TextView descriptionView;
        private TextView moreView;
        private ImageView podcastImageView;
        private Space bottomSpaceView;

        ViewHolder(View itemView, final Supplier<Podcast[]> getPodcasts, RecyclerViewAdapter adapter) {
            super(itemView);

            separatorView = ((ImageView)itemView.findViewById(R.id.imageview_item_podcast_separator));
            constraintView = ((ConstraintLayout) itemView.findViewById(R.id.constraintlayout_item_podcast));
            flagView = ((ImageView)itemView.findViewById(R.id.imageView_item_podcast_flag));
            subscribedView = itemView.findViewById(R.id.textview_item_podcast_subscribed);
            levelView = ((TextView)itemView.findViewById(R.id.textview_item_podcast_level));
            titleView = ((TextView)itemView.findViewById(R.id.textview_item_podcast_title));
            descriptionView = ((TextView)itemView.findViewById(R.id.textview_item_podcast_description));
            moreView = ((TextView)itemView.findViewById(R.id.textview_item_podcast_more));
            podcastImageView = ((ImageView)itemView.findViewById(R.id.imageview_item_podcast));
            bottomSpaceView = ((Space)itemView.findViewById(R.id.imageview_item_podcast_bottomspace));

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
