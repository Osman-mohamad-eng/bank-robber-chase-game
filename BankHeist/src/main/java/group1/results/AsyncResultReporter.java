package group1.results;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Sends immutable snapshots away from Swing's event thread. */
public final class AsyncResultReporter implements ResultReporter, AutoCloseable {
    private static final Logger LOG = Logger.getLogger(AsyncResultReporter.class.getName());
    private final HttpGameServicesClient api;
    private final PlayerProfile profile;
    private final PendingResultStore store;
    private String registrationFailure;
    private boolean registered;
    private final ConcurrentHashMap<UUID, HeistResult> unpersisted = new ConcurrentHashMap<>();
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "heist-submission");
        thread.setDaemon(true);
        return thread;
    });

    public AsyncResultReporter(HttpGameServicesClient api, PlayerProfile profile, PendingResultStore store) {
        this(api, profile, store, Duration.ofSeconds(15));
    }

    AsyncResultReporter(HttpGameServicesClient api, PlayerProfile profile, PendingResultStore store, Duration retryDelay) {
        this.api = api;
        this.profile = profile;
        this.store = store;
        worker.scheduleWithFixedDelay(this::drain, 0, retryDelay.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void submit(HeistResult result) {
        unpersisted.putIfAbsent(result.sessionId(), result);
        worker.execute(this::drain);
    }

    /** Serialized by the worker; package access allows focused retry tests. */
    synchronized void drain() {
        try {
            persistQueued();
            var pending = store.pending();
            if (registered && pending.isEmpty()) return;
            if (registrationFailure == null) {
                try { api.register(profile); registered = true; }
                catch (HttpGameServicesClient.ApiException failure) {
                    if (failure.retryable()) throw failure;
                    registrationFailure = "Player registration needs attention: " + failure.getMessage();
                    LOG.severe(registrationFailure + "; results preserved in pending/failed");
                }
            }
            if (registrationFailure != null) {
                for (HeistResult result : pending) store.reject(result, registrationFailure);
                return;
            }
            for (HeistResult result : pending) {
                // A copied outbox must never submit another local profile's results.
                if (!profile.playerId().equals(result.playerId())) {
                    store.reject(result, "Result belongs to a different local player profile");
                    continue;
                }
                try {
                    api.submit(result);
                    store.acknowledge(result);
                    LOG.info("Heist recorded: " + result.sessionId());
                } catch (HttpGameServicesClient.ApiException failure) {
                    if (failure.retryable()) throw failure;
                    store.reject(result, failure.getMessage());
                    LOG.warning("Heist needs review in pending/failed: " + result.sessionId());
                }
            }
        } catch (Exception failure) {
            if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
            LOG.log(Level.WARNING, "Game services unavailable; pending results retained: " + failure.getMessage());
        }
    }

    private void persistQueued() throws IOException {
        for (HeistResult result : unpersisted.values()) {
            store.save(result);
            unpersisted.remove(result.sessionId(), result);
        }
    }

    @Override
    public void close() {
        worker.shutdown();
        try {
            if (!worker.awaitTermination(2, TimeUnit.SECONDS)) worker.shutdownNow();
        } catch (InterruptedException interrupted) {
            worker.shutdownNow();
            Thread.currentThread().interrupt();
        }
        try { persistQueued(); }
        catch (IOException failure) { LOG.log(Level.SEVERE, "Could not preserve pending results on shutdown", failure); }
    }
}
