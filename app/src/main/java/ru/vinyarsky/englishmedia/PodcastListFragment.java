package ru.vinyarsky.englishmedia;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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

import java.io.IOException;
import java.io.PipedOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Exchanger;

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
            if (mListener != null) {
                Cursor cursor = (Cursor) parent.getItemAtPosition(position);
                UUID code = UUID.fromString(cursor.getString(cursor.getColumnIndex(Podcast.CODE)));
                mListener.onSelectPodcast(code);
            }
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
                            ImageView flagView = ((ImageView)view.findViewById(R.id.imageView_item_podcast_flag));
                            Podcast.Country country = Podcast.Country.valueOf(cursor.getString(cursor.getColumnIndex(Podcast.COUNTRY)));
                            switch (country) {
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

                            View subscribedView = view.findViewById(R.id.textview_item_podcast_subscribed);
                            if (cursor.getInt(cursor.getColumnIndex(Podcast.SUBSCRIBED)) != 0)
                                subscribedView.setVisibility(View.VISIBLE);
                            else
                                subscribedView.setVisibility(View.INVISIBLE);

                            TextView levelView = ((TextView)view.findViewById(R.id.textview_item_podcast_level));
                            Podcast.PodcastLevel level = Podcast.PodcastLevel.valueOf(cursor.getString(cursor.getColumnIndex(Podcast.LEVEL)));
                            levelView.setText(level.toString());
                            switch (level) {
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

                            ((TextView)view.findViewById(R.id.textview_item_podcast_title)).setText(cursor.getString(cursor.getColumnIndex(Podcast.TITLE)));
                            ((TextView)view.findViewById(R.id.textview_item_podcast_description)).setText(cursor.getString(cursor.getColumnIndex(Podcast.DESCRIPTION)));

                            try {
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), Uri.parse(cursor.getString(cursor.getColumnIndex(Podcast.IMAGE_PATH))));
                                ((ImageView)view.findViewById(R.id.imageview_item_podcast)).setImageBitmap(bitmap);
                            } catch (IOException e) {
                                // OK, no image then...
                            }
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
