package itmo.lab.client.gui;

import itmo.lab.data.Worker;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Общее изменяемое состояние приложения: список работников и данные текущего пользователя. */
public class AppState {

    private final List<Worker> workers = new CopyOnWriteArrayList<>();
    private volatile String currentLogin;
    private volatile String currentPassword;

    private final List<Consumer<List<Worker>>> listeners = new CopyOnWriteArrayList<>();

    /** Заменяет список работников и уведомляет всех слушателей на FX-потоке. */
    public void setWorkers(List<Worker> updated) {
        workers.clear();
        workers.addAll(updated);
        List<Worker> snapshot = List.copyOf(workers);
        Platform.runLater(() -> listeners.forEach(l -> l.accept(snapshot)));
    }

    /** Возвращает неизменяемый снимок текущего списка работников. */
    public List<Worker> getWorkers() {
        return List.copyOf(workers);
    }

    /** Регистрирует слушателя, вызываемого при каждом изменении списка работников. */
    public void addChangeListener(Consumer<List<Worker>> l) {
        listeners.add(l);
    }

    /** Удаляет ранее зарегистрированного слушателя. */
    public void removeChangeListener(Consumer<List<Worker>> l) {
        listeners.remove(l);
    }

    /** Сохраняет логин и пароль текущего пользователя. */
    public void setCredentials(String login, String password) {
        this.currentLogin = login;
        this.currentPassword = password;
    }

    /** Возвращает логин текущего пользователя. */
    public String getCurrentLogin() {
        return currentLogin;
    }

    /** Возвращает пароль текущего пользователя. */
    public String getCurrentPassword() {
        return currentPassword;
    }
}
