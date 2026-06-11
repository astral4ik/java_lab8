package itmo.lab.client.gui;

/** Точка входа fat JAR: запускает {@link GuiMain} без наследования от {@code Application}. */
public class Launcher {
    public static void main(String[] args) {
        GuiMain.main(args);
    }
}
