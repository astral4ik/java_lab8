package itmo.lab.client.gui;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Периодически опрашивает сервер и обновляет список работников в {@link AppState}. */
public class PollingService {

    private static final int INTERVAL_SEC = 5;

    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor();

    private final ClientBridge bridge;
    private final AppState state;
    public PollingService(ClientBridge bridge, AppState state) {
        this.bridge = bridge;
        this.state = state;
    }

    /** Запускает периодический опрос сервера. */
    public void start() {
        executor.scheduleAtFixedRate(this::poll, 0, INTERVAL_SEC, TimeUnit.SECONDS);
    }

    /** Немедленно останавливает опрос. */
    public void stop() {
        executor.shutdownNow();
    }

    private void poll() {
        try {
            var workers = bridge.show();
            if (workers != null) {
                state.setWorkers(workers);
            }
        } catch (Exception ignored) {
        }
    }
}
