package itmo.lab.client.gui.canvas;

import javafx.scene.paint.Color;

import java.util.LinkedHashMap;
import java.util.Map;

/** Сопоставляет логину владельца стабильный цвет из фиксированной палитры. */
public class ColorRegistry {

    private static final Color[] PALETTE = {
        Color.web("#4E79A7"), Color.web("#F28E2B"), Color.web("#E15759"),
        Color.web("#76B7B2"), Color.web("#59A14F"), Color.web("#EDC948"),
        Color.web("#B07AA1"), Color.web("#FF9DA7"), Color.web("#9C755F"),
        Color.web("#BAB0AC")
    };

    private final Map<String, Color> assigned = new LinkedHashMap<>();
    private int nextIndex = 0;

    /** Возвращает цвет, назначенный данному логину, выделяя новый при необходимости. */
    public synchronized Color forLogin(String login) {
        if (login == null) return Color.GRAY;
        return assigned.computeIfAbsent(login, k -> PALETTE[nextIndex++ % PALETTE.length]);
    }
}
