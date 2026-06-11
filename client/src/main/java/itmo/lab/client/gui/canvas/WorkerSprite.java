package itmo.lab.client.gui.canvas;

/** Анимированный спрайт работника на канвасе: хранит позицию, радиус и прозрачность. */
public class WorkerSprite {

    private static final float ALPHA_SPEED  = 2.5f;
    private static final float POS_SPEED    = 4.0f;
    private static final float RADIUS_SPEED = 4.0f;

    public final int workerId;
    public float screenX;
    public float screenY;
    public float targetX;
    public float targetY;
    public float alpha;
    public float targetAlpha;
    public float displayRadius;
    public float targetRadius;
    public int salary;
    public String ownerLogin;
    public String name;
    public boolean pendingRemoval;

    public WorkerSprite(int workerId, float screenX, float screenY,
                        float radius, String ownerLogin, String name) {
        this.workerId = workerId;
        this.screenX = screenX;
        this.screenY = screenY;
        this.targetX = screenX;
        this.targetY = screenY;
        this.displayRadius = radius;
        this.targetRadius  = radius;
        this.ownerLogin = ownerLogin;
        this.name = name;
        this.alpha = 0f;
        this.targetAlpha = 1f;
        this.pendingRemoval = false;
    }

    /** Интерполирует прозрачность, позицию и радиус. Возвращает {@code true}, если что-то изменилось. */
    public boolean update(float dt) {
        boolean changed = false;

        float dAlpha = targetAlpha - alpha;
        if (Math.abs(dAlpha) > 0.005f) {
            alpha += dAlpha * ALPHA_SPEED * dt;
            alpha = Math.max(0f, Math.min(1f, alpha));
            changed = true;
        } else if (alpha != targetAlpha) {
            alpha = targetAlpha;
            changed = true;
        }

        float dx = targetX - screenX;
        float dy = targetY - screenY;
        if (Math.abs(dx) > 0.5f || Math.abs(dy) > 0.5f) {
            screenX += dx * POS_SPEED * dt;
            screenY += dy * POS_SPEED * dt;
            changed = true;
        } else {
            if (screenX != targetX || screenY != targetY) {
                screenX = targetX;
                screenY = targetY;
                changed = true;
            }
        }

        float dr = targetRadius - displayRadius;
        if (Math.abs(dr) > 0.2f) {
            displayRadius += dr * RADIUS_SPEED * dt;
            changed = true;
        } else if (displayRadius != targetRadius) {
            displayRadius = targetRadius;
            changed = true;
        }

        return changed;
    }
}
