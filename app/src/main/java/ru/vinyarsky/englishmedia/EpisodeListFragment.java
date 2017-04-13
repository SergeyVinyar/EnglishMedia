package ru.vinyarsky.englishmedia;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Collections;
import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import ru.vinyarsky.englishmedia.db.Episode;

public class EpisodeListFragment extends Fragment {

    private OnPodcastListFragmentListener mListener;

    public EpisodeListFragment() {
    }

    public static EpisodeListFragment newInstance(UUID podcastCode) {
        EpisodeListFragment fragment =  new EpisodeListFragment();

        Bundle args = new Bundle();
        args.putString("podcastCode", podcastCode.toString());

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
        View view =  inflater.inflate(R.layout.fragment_episodelist, container, false);

        ListView listView = (ListView)view.findViewById(R.id.listview_fragment_episodelist);
//        listView.setOnItemClickListener((parent, clickedView, position, id) -> {
//            if (mListener != null)
//                mListener.onSelectPodcast(UUID.randomUUID());
//        });

        EMApplication app = (EMApplication)getActivity().getApplication();

        UUID podcastCode = UUID.fromString(getArguments().getString("podcastCode"));
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
                        }
                    });
                });

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
//        if (context instanceof OnPodcastListFragmentListener) {
//            mListener = (OnPodcastListFragmentListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement " + OnPodcastListFragmentListener.class.getSimpleName());
//        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    public interface OnPodcastListFragmentListener {
        void onSelectPodcast(UUID podcastCode);
    }
}
