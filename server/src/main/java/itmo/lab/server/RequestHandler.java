package itmo.lab.server;

import itmo.lab.server.commands.ServerCommand;
import itmo.lab.server.commands.*;
import itmo.lab.server.auth.AuthService;
import itmo.lab.common.Response;
import itmo.lab.WorkersCollection;

import java.util.HashMap;
import java.util.Map;

/**
 * Маршрутизатор запросов: аутентифицирует пользователя и делегирует выполнение нужной команде.
 */
public class RequestHandler {

    private static final org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger(RequestHandler.class);

    private final Map<String, ServerCommand> commands;
    private final WorkersCollection collection;
    private final AuthService authService;

    /**
     * @param collection коллекция работников, передаваемая во все команды
     */
    public RequestHandler(WorkersCollection collection) {
        this.collection = collection;
        this.authService = new AuthService();

        commands = new HashMap<>();
        commands.put("insert", new InsertCommand(collection));
        commands.put("update", new UpdateCommand(collection));
        commands.put("get", new GetWorkerCommand(collection));
        commands.put("remove_key", new RemoveKeyCommand(collection));
        commands.put("remove_lower", new RemoveLowerCommand(collection));
        commands.put("remove_greater_key", new RemoveGreaterKeyCommand(collection));
        commands.put("clear", new ClearCommand(collection));
        commands.put("show", new ShowCommand(collection));
        commands.put("info", new InfoCommand(collection));
        commands.put("filter_contains_name", new FilterContainsNameCommand(collection));
        commands.put("count_less_than_start_date", new CountLessThanStartDateCommand(collection));
        commands.put("print_unique_start_date", new PrintUniqueStartDateCommand(collection));
        commands.put("replace_if_lower", new ReplaceIfLowerCommand(collection));
    }

    /**
     * Обрабатывает входящий запрос: проверяет аутентификацию и вызывает соответствующую команду.
     *
     * @param request входящий запрос от клиента
     * @return ответ с результатом выполнения команды
     */
    public Response handle(itmo.lab.common.Request request) {
        logger.info("Обработка команды: " + request.getCommandName());

        String cmd = request.getCommandName();

        if ("register".equals(cmd)) {
            boolean ok = authService.register(request.getLogin(), request.getPassword());
            if (ok) logger.info("Зарегистрирован новый пользователь: " + request.getLogin());
            return new Response(ok, ok ? "OK_REGISTERED" : "ERR_LOGIN_TAKEN", null);
        }

        if ("login".equals(cmd)) {
            boolean ok = authService.authenticate(request.getLogin(), request.getPassword());
            if (ok) logger.info("Пользователь вошёл: " + request.getLogin());
            else logger.warn("Неудачная попытка входа: " + request.getLogin());
            return new Response(ok, ok ? "OK_LOGIN" : "ERR_AUTH_FAILED", null);
        }

        if (!authService.authenticate(request.getLogin(), request.getPassword())) {
            return new Response(false, "ERR_NOT_AUTH", null);
        }

        ServerCommand command = commands.get(cmd);

        if (command == null) {
            logger.error("Команда не найдена: " + cmd);
            return new Response(false, "Неизвестная команда: " + cmd, null);
        }

        try {
            logger.info("Выполнение команды: " + cmd);
            Response response = command.execute(request.getArgs(), request.getLogin());
            logger.info("Команда выполнена, успех: " + response.isSuccess());

            return response;
        } catch (Exception e) {
            logger.error("Ошибка выполнения команды " + cmd + ": " + e.getMessage(), e);
            return new Response(false, "Ошибка выполнения: " + e.getMessage(), null);
        }
    }
}
