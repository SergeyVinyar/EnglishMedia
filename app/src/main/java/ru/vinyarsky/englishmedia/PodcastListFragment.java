package ru.vinyarsky.englishmedia;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.PipedOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import ru.vinyarsky.englishmedia.db.DbHelper;
import ru.vinyarsky.englishmedia.db.Podcast;

public class PodcastListFragment extends Fragment {

    private OnPodcastListFragmentListener mListener;

    public PodcastListFragment() {
    }

    public static PodcastListFragment newInstance() {
        return new PodcastListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_podcastlist, container, false);

        ListView listView = (ListView)view.findViewById(R.id.listview_fragment_podcastlist);
        listView.setOnItemClickListener((parent, clickedView, position, id) -> {
            if (mListener != null)
                mListener.onSelectPodcast(UUID.randomUUID()); // TODO
        });

        EMApplication app = (EMApplication)getActivity().getApplication();

        Observable.<Cursor>create((e) -> {
            e.onNext(Podcast.readAll(app.getDbHelper()));
            e.onComplete();
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((cursor) -> {
                    listView.setAdapter(new CursorAdapter(getContext(), cursor, false) {

                        @Override
                        public View newView(Context context, Cursor cursor, ViewGroup parent) {
                            return ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.item_podcast, parent, false);
                        }

                        @Override
                        public void bindView(View view, Context context, Cursor cursor) {
                            ((TextView)view.findViewById(R.id.textview_item_podcast_title)).setText(cursor.getString(cursor.getColumnIndex(Podcast.TITLE)));
                            ((TextView)view.findViewById(R.id.textview_item_podcast_description)).setText(cursor.getString(cursor.getColumnIndex(Podcast.DESCRIPTION)));
                            ((ImageView)view.findViewById(R.id.imageview_item_podcast)).setImageURI(Uri.parse(cursor.getString(cursor.getColumnIndex(Podcast.IMAGE_PATH))));
                        }
                    });
                });

        return view;
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
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    public interface OnPodcastListFragmentListener {
        void onSelectPodcast(UUID podcastCode);
    }
}
