package itmo.lab.client.gui;

import itmo.lab.client.Client;
import itmo.lab.common.IdArgument;
import itmo.lab.common.Request;
import itmo.lab.common.Response;
import itmo.lab.common.StringArgument;
import itmo.lab.common.WorkerArgument;
import itmo.lab.data.Worker;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/** Фасад над {@link Client}: отправляет команды серверу и возвращает результат. */
public class ClientBridge {

    private final Client client;
    private final AppState state;

    public ClientBridge(Client client, AppState state) {
        this.client = client;
        this.state = state;
    }

    /** Возвращает {@code true}, если соединение с сервером установлено. */
    public boolean isConnected() {
        return client.isConnected();
    }

    /** Устанавливает соединение; бросает {@code IOException} при неудаче. */
    public synchronized void tryConnect() throws IOException {
        client.tryConnect();
    }

    /** Регистрирует нового пользователя; возвращает {@code null} при успехе или текст ошибки. */
    public synchronized String tryRegister(String login, String password) {
        return result(client.sendRequest(new Request("register", null, login, password)));
    }

    /** Выполняет вход и сохраняет учётные данные; возвращает {@code null} при успехе или текст ошибки. */
    public synchronized String tryLogin(String login, String password) {
        Response r = client.sendRequest(new Request("login", null, login, password));
        if (r == null) return "Нет ответа от сервера";
        if (r.isSuccess()) {
            client.setCredentials(login, password);
            state.setCredentials(login, password);
            return null;
        }
        return r.getMessage();
    }

    /** Запрашивает полный список работников с сервера. */
    @SuppressWarnings("unchecked")
    public synchronized List<Worker> show() {
        Response r = client.sendRequestWithAuth(new Request("show", null));
        if (r != null && r.isSuccess() && r.getData() instanceof List) {
            return (List<Worker>) r.getData();
        }
        return Collections.emptyList();
    }

    /** Добавляет работника в коллекцию; возвращает {@code null} при успехе или текст ошибки. */
    public synchronized String insert(Worker w) {
        return result(client.sendRequestWithAuth(new Request("insert", new WorkerArgument(w))));
    }

    /** Обновляет работника с указанным ID; возвращает {@code null} при успехе или текст ошибки. */
    public synchronized String update(int id, Worker w) {
        return result(client.sendRequestWithAuth(
                new Request("update", new Object[]{new IdArgument(id), new WorkerArgument(w)})));
    }

    /** Удаляет работника с указанным ключом; возвращает {@code null} при успехе или текст ошибки. */
    public synchronized String removeKey(int id) {
        return result(client.sendRequestWithAuth(new Request("remove_key", new IdArgument(id))));
    }

    /** Удаляет всех работников с ключом меньше указанного; возвращает {@code null} при успехе или текст ошибки. */
    public synchronized String removeLower(int id) {
        return result(client.sendRequestWithAuth(new Request("remove_lower", new IdArgument(id))));
    }

    /** Удаляет все записи с ключом больше указанного; возвращает {@code null} при успехе или текст ошибки. */
    public synchronized String removeGreaterKey(int id) {
        return result(client.sendRequestWithAuth(new Request("remove_greater_key", new IdArgument(id))));
    }

    /** Очищает коллекцию (только записи текущего пользователя); возвращает {@code null} при успехе или текст ошибки. */
    public synchronized String clear() {
        return result(client.sendRequestWithAuth(new Request("clear", null)));
    }

    /** Возвращает информацию о коллекции. */
    public synchronized String info() {
        Response r = client.sendRequestWithAuth(new Request("info", null));
        return r != null ? r.getMessage() : "Нет ответа от сервера";
    }

    /** Возвращает работников, чьё имя содержит подстроку. */
    @SuppressWarnings("unchecked")
    public synchronized List<Worker> filterContainsName(String name) {
        Response r = client.sendRequestWithAuth(new Request("filter_contains_name", new StringArgument(name)));
        if (r != null && r.isSuccess() && r.getData() instanceof List) {
            return (List<Worker>) r.getData();
        }
        return Collections.emptyList();
    }

    /** Возвращает количество работников с датой начала меньше указанной. */
    public synchronized String countLessThanStartDate(String date) {
        Response r = client.sendRequestWithAuth(new Request("count_less_than_start_date", new StringArgument(date)));
        return r != null ? r.getMessage() : "Нет ответа от сервера";
    }

    /** Возвращает строку с уникальными датами начала работников. */
    public synchronized String printUniqueStartDate() {
        Response r = client.sendRequestWithAuth(new Request("print_unique_start_date", null));
        if (r == null) return "Нет ответа от сервера";
        if (r.getData() instanceof List) {
            List<?> dates = (List<?>) r.getData();
            StringBuilder sb = new StringBuilder(r.getMessage()).append("\n");
            for (Object d : dates) sb.append("  • ").append(d).append("\n");
            return sb.toString().trim();
        }
        return r.getMessage();
    }

    /** Заменяет работника, если новый «меньше» текущего; возвращает {@code null} при успехе или текст ошибки. */
    public synchronized String replaceIfLower(int id, Worker w) {
        return result(client.sendRequestWithAuth(
                new Request("replace_if_lower", new Object[]{new IdArgument(id), new WorkerArgument(w)})));
    }

    private String result(Response r) {
        if (r == null) return "Нет ответа от сервера";
        return r.isSuccess() ? null : r.getMessage();
    }

    /** Возвращает работника по ключу, или {@code null} если не найден. */
    public synchronized Worker get(int id) {
        Response r = client.sendRequestWithAuth(new Request("get", new IdArgument(id)));
        if (r != null && r.isSuccess() && r.getData() instanceof Worker) {
            return (Worker) r.getData();
        }
        return null;
    }
}
