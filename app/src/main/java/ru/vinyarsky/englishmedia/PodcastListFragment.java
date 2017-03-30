package ru.vinyarsky.englishmedia;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PodcastListFragment extends Fragment implements ListView.OnItemClickListener {


    private OnPodcastListFragmentListener mListener;

    public PodcastListFragment() {
        // Required empty public constructor
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
        View view =  inflater.inflate(R.layout.fragment_podcastlist, container, false);

        ListView listView = (ListView) view.findViewById(R.id.listview_fragment_podcastlist);
        listView.setOnItemClickListener(this);

        List<String> data = new ArrayList<>(20);
        for(int i = 0; i < 20; i++)
            data.add(Integer.toString(i));
        listView.setAdapter(new ArrayAdapter<String>(this.getContext(), android.R.layout.simple_list_item_1, data));

//        try(XmlResourceParser parser = getResources().getXml(0)) {
//
//
//        }



        return view;
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mListener != null) {
            mListener.onSelectPodcast(UUID.randomUUID());
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnPodcastListFragmentListener) {
            mListener = (OnPodcastListFragmentListener) context;
        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

//    private class PodcastAdapter extends ListAdapter {
//
//    }


    public interface OnPodcastListFragmentListener {
        void onSelectPodcast(UUID podcastId);
    }
}
