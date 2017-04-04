package ru.vinyarsky.englishmedia.rss;

import android.content.Context;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.annimon.stream.function.Supplier;

public final class RssFetcher {

    private Context appContext;
    private Supplier<ExecutorService> executorSupplier;

    public RssFetcher(Context appContext, Supplier<ExecutorService> executorSupplier) {
        this.appContext = appContext;
        this.executorSupplier = executorSupplier;
    }

    /**
     *
     * @param podcastCodes
     * @return Total number of fetched episodes
     */
    public Future<Integer> fetchEpisodesAsync(List<UUID> podcastCodes) {



        return null;
    }

    public synchronized void close() {
        // TODO
    }
}
