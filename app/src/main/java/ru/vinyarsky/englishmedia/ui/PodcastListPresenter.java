package ru.vinyarsky.englishmedia.ui;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import ru.vinyarsky.englishmedia.EMApplication;
import ru.vinyarsky.englishmedia.db.Podcast;

@UiThread
public class PodcastListPresenter {

    private static PodcastListPresenter instance;
    private static List<ViewInfo> views = new ArrayList<>(1);

    private Context context;
    private CompositeDisposable compositeDisposable;

    private Cursor cursor;

    public static PodcastListPresenter addView(PodcastListView view) {
        ViewInfo viewInfo = new ViewInfo(view, view.getPodcastLevel());
        views.add(viewInfo);
        if (instance == null)
            instance = new PodcastListPresenter();
        instance.initNewView(viewInfo);
        return instance;
    }

    public static void removeView(PodcastListView view) {
        views.removeIf(viewInfo -> viewInfo.view.equals(view));
        if (views.isEmpty()) {
            instance.release();
            instance = null;
        }
    }

    private PodcastListPresenter() {
        this.context = EMApplication.getEmComponent().getContext();
        this.compositeDisposable = new CompositeDisposable();
    }

    private void release() {
        this.compositeDisposable.dispose();
    }

    private void initNewView(ViewInfo info) {
        if (cursor != null)
            info.view.onUpdateData(cursor);
        refresh();
    }

    private void refresh() {
        // TODO
    }

    public interface PodcastListView {

        Podcast.PodcastLevel getPodcastLevel();

        void onUpdateData(Cursor cursor);

        void onShowProgress();
        void onHideProgress();
    }

    private static class ViewInfo {

        final PodcastListView view;
        final Podcast.PodcastLevel podcastLevel;

        ViewInfo(@NonNull PodcastListView view, @Nullable Podcast.PodcastLevel podcastLevel) {
            this.view = view;
            this.podcastLevel = podcastLevel;
        }
    }
}
