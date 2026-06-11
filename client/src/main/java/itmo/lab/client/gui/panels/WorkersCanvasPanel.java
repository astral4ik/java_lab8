package itmo.lab.client.gui.panels;

import itmo.lab.client.gui.AppState;
import itmo.lab.client.gui.WorkerFilter;
import itmo.lab.client.gui.canvas.ColorRegistry;
import itmo.lab.client.gui.canvas.WorkerSprite;
import itmo.lab.data.Worker;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** Анимированная визуализация работников на канвасе с поддержкой фильтрации и кликов. */
public class WorkersCanvasPanel extends Pane {

    private static final int PADDING = 60;

    private final AppState state;
    private final ColorRegistry colorRegistry = new ColorRegistry();
    private final Canvas canvas = new Canvas();
    private final Map<Integer, WorkerSprite> sprites = new LinkedHashMap<>();

    private long lastNano = System.nanoTime();
    private Consumer<Worker> onWorkerClick;
    private String filterText = "";
    private WorkerFilter advancedFilter = new WorkerFilter();

    private final AnimationTimer animTimer;

    public WorkersCanvasPanel(AppState state) {
        this.state = state;
        getChildren().add(canvas);

        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        canvas.widthProperty().addListener(obs -> redraw());
        canvas.heightProperty().addListener(obs -> redraw());

        animTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                float dt = Math.min((now - lastNano) / 1_000_000_000f, 0.1f);
                lastNano = now;
                tick(dt);
            }
        };
        animTimer.start();

        state.addChangeListener(this::reconcile);

        canvas.setOnMouseClicked(e -> handleClick(e.getX(), e.getY()));
    }

    private synchronized void reconcile(List<Worker> workers) {
        List<Integer> currentIds = new ArrayList<>(sprites.keySet());
        List<Integer> newIds = new ArrayList<>();
        for (Worker w : workers) newIds.add(w.getId());

        for (int id : currentIds) {
            if (!newIds.contains(id)) {
                WorkerSprite s = sprites.get(id);
                s.pendingRemoval = true;
                s.targetAlpha = 0f;
            }
        }

        for (Worker w : workers) {
            double[] pos = coordsToScreen(w, canvas.getWidth(), canvas.getHeight());
            if (!sprites.containsKey(w.getId())) {
                float r = salaryToRadius(w.getSalary(), workers);
                WorkerSprite s = new WorkerSprite(w.getId(), (float) pos[0], (float) pos[1], r, w.getOwnerLogin(), w.getName());
                s.salary = w.getSalary();
                sprites.put(w.getId(), s);
            } else {
                WorkerSprite s = sprites.get(w.getId());
                if (!s.pendingRemoval) {
                    s.targetX = (float) pos[0];
                    s.targetY = (float) pos[1];
                    s.salary = w.getSalary();
                    s.ownerLogin = w.getOwnerLogin();
                    s.name = w.getName();
                }
            }
        }

        for (WorkerSprite s : sprites.values()) {
            if (!s.pendingRemoval) {
                Worker w = findWorker(workers, s.workerId);
                if (w != null) {
                    s.targetRadius = salaryToRadius(w.getSalary(), workers);
                    s.targetAlpha = workerMatchesFilter(w) ? 1f : 0f;
                }
            }
        }
    }

    private synchronized void tick(float dt) {
        boolean changed = false;
        List<Integer> toRemove = new ArrayList<>();

        for (WorkerSprite s : sprites.values()) {
            if (s.update(dt)) changed = true;
            if (s.pendingRemoval && s.alpha < 0.01f) toRemove.add(s.workerId);
        }
        toRemove.forEach(sprites::remove);

        if (changed || !toRemove.isEmpty()) redraw();
    }

    private synchronized void redraw() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        gc.setFill(Color.web("#F8F9FA"));
        gc.fillRect(0, 0, w, h);

        List<Worker> workers = state.getWorkers();

        for (WorkerSprite s : sprites.values()) {
            if (!s.pendingRemoval) {
                Worker worker = findWorker(workers, s.workerId);
                if (worker != null) {
                    double[] pos = coordsToScreen(worker, w, h);
                    s.targetX = (float) pos[0];
                    s.targetY = (float) pos[1];
                }
            }
        }

        for (WorkerSprite s : sprites.values()) {
            if (s.alpha < 0.01f) continue;

            Color base = colorRegistry.forLogin(s.ownerLogin);
            double r = Math.max(10, s.displayRadius);
            double cx = s.screenX;
            double cy = s.screenY;

            gc.save();
            gc.setGlobalAlpha(s.alpha);

            gc.setFill(Color.rgb(0, 0, 0, 0.15));
            gc.fillOval(cx - r + 3, cy - r + 3, r * 2, r * 2);

            gc.setFill(base);
            gc.fillOval(cx - r, cy - r, r * 2, r * 2);

            gc.setStroke(base.darker());
            gc.setLineWidth(1.5);
            gc.strokeOval(cx - r, cy - r, r * 2, r * 2);

            gc.setFill(Color.BLACK);
            gc.setFont(Font.font(null, FontWeight.BOLD, 10));
            String label = s.name != null && s.name.length() > 10
                    ? s.name.substring(0, 9) + "…"
                    : (s.name != null ? s.name : "");
            double lw = measureText(label, 10);
            gc.fillText(label, cx - lw / 2, cy + 4);

            gc.setGlobalAlpha(s.alpha * 0.85);
            gc.setFill(Color.DARKGRAY);
            gc.setFont(Font.font(null, 9));
            String idLabel = "#" + s.workerId;
            double idW = measureText(idLabel, 9);
            gc.fillText(idLabel, cx - idW / 2, cy - r - 3);

            gc.restore();
        }

        drawLegend(gc, workers);
    }

    private void drawLegend(GraphicsContext gc, List<Worker> workers) {
        Set<String> logins = new LinkedHashSet<>();
        for (Worker w : workers) if (w.getOwnerLogin() != null) logins.add(w.getOwnerLogin());
        if (logins.isEmpty()) return;

        double x = 10, y = 20;
        gc.setFont(Font.font(null, 11));
        for (String login : logins) {
            Color c = colorRegistry.forLogin(login);
            gc.setFill(c);
            gc.fillRect(x, y - 10, 12, 12);
            gc.setFill(Color.DARKGRAY);
            gc.fillText(login, x + 16, y);
            y += 18;
        }
    }

    private void handleClick(double mx, double my) {
        if (onWorkerClick == null) return;
        List<Worker> workers = state.getWorkers();
        WorkerSprite closest = null;
        float minDist = Float.MAX_VALUE;

        synchronized (this) {
            for (WorkerSprite s : sprites.values()) {
                if (s.alpha < 0.3f) continue;
                double r = Math.max(10, s.displayRadius);
                float dist = (float) Math.hypot(mx - s.screenX, my - s.screenY);
                if (dist <= r + 4 && dist < minDist) {
                    minDist = dist;
                    closest = s;
                }
            }
        }

        if (closest != null) {
            Worker found = findWorker(workers, closest.workerId);
            if (found != null) onWorkerClick.accept(found);
        }
    }

    private double[] coordsToScreen(Worker w, double panelW, double panelH) {
        if (panelW < 1 || panelH < 1) return new double[]{panelW / 2, panelH / 2};
        List<Worker> all = state.getWorkers();

        int minX = all.stream().mapToInt(ww -> ww.getCoordinates().getX()).min().orElse(0);
        int maxX = all.stream().mapToInt(ww -> ww.getCoordinates().getX()).max().orElse(100);
        float minY = (float) all.stream().mapToDouble(ww -> ww.getCoordinates().getY()).min().orElse(0);
        float maxY = (float) all.stream().mapToDouble(ww -> ww.getCoordinates().getY()).max().orElse(100);

        if (maxX == minX) { minX -= 50; maxX += 50; }
        if (maxY == minY) { minY -= 50; maxY += 50; }

        float nx = (float) (w.getCoordinates().getX() - minX) / (maxX - minX);
        float ny = (w.getCoordinates().getY() - minY) / (maxY - minY);

        double sx = PADDING + nx * (panelW - 2.0 * PADDING);
        double sy = PADDING + (1f - ny) * (panelH - 2.0 * PADDING);
        return new double[]{sx, sy};
    }

    private float salaryToRadius(int salary, List<Worker> workers) {
        int minS = workers.stream().mapToInt(Worker::getSalary).min().orElse(salary);
        int maxS = workers.stream().mapToInt(Worker::getSalary).max().orElse(salary);
        if (maxS == minS) return 20;
        float t = (float) (salary - minS) / (maxS - minS);
        return 10 + (float) (Math.sqrt(t) * 30);
    }

    private Worker findWorker(List<Worker> workers, int id) {
        return workers.stream().filter(w -> w.getId() == id).findFirst().orElse(null);
    }

    private boolean workerMatchesFilter(Worker w) {
        if (!filterText.isEmpty()) {
            boolean textMatch = Stream.of(
                    w.getName(),
                    String.valueOf(w.getId()),
                    w.getOwnerLogin(),
                    w.getPosition() != null ? w.getPosition().name() : "",
                    w.getStatus() != null ? w.getStatus().name() : "",
                    w.getOrganization() != null ? w.getOrganization().getFullName() : ""
            ).filter(s -> s != null).anyMatch(s -> s.toLowerCase().contains(filterText));
            if (!textMatch) return false;
        }
        return advancedFilter.matches(w);
    }

    /** Устанавливает текстовый фильтр и перерисовывает канвас. */
    public void setFilter(String text) {
        this.filterText = text == null ? "" : text.trim().toLowerCase();
        reconcile(state.getWorkers());
    }

    /** Устанавливает расширенный фильтр и перерисовывает канвас. */
    public void setAdvancedFilter(WorkerFilter f) {
        this.advancedFilter = f != null ? f : new WorkerFilter();
        reconcile(state.getWorkers());
    }

    /** Устанавливает обработчик клика по спрайту работника. */
    public void setOnWorkerClick(Consumer<Worker> handler) {
        this.onWorkerClick = handler;
    }

    /** Останавливает AnimationTimer. */
    public void stopAnimation() {
        animTimer.stop();
    }

    private double measureText(String text, double size) {
        return text.length() * size * 0.6;
    }
}
