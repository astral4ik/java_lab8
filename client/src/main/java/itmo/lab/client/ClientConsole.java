package itmo.lab.client;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

import itmo.lab.data.Address;
import itmo.lab.data.Coordinates;
import itmo.lab.data.Location;
import itmo.lab.data.Organization;
import itmo.lab.data.Position;
import itmo.lab.data.Status;
import itmo.lab.data.Worker;

/**
 * Консоль клиента: ввод данных от пользователя, вывод результатов и построение объектов Worker.
 */
public class ClientConsole {

    private InputSource inputSource;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private boolean fromFile = false;

    private String currentLogin;
    private String currentPassword;

    private Consumer<String> outputSink = System.out::println;

    public ClientConsole() {
        this.inputSource = new ConsoleInputSource();
    }

    public void setOutputSink(Consumer<String> sink) {
        this.outputSink = sink;
    }

    public void setInputSource(InputSource newInputSource) {
        this.inputSource = newInputSource;
    }

    public InputSource getInputSource() {
        return inputSource;
    }

    public boolean isFromFile() {
        return fromFile;
    }

    public void setFromFile(boolean fromFile) {
        this.fromFile = fromFile;
    }

    /**
     * Выводит строку с переносом в стандартный поток вывода.
     */
    public void printLine(String message) {
        outputSink.accept(message);
    }

    /**
     * Выводит строку без переноса в стандартный поток вывода.
     */
    public void print(String message) {
        outputSink.accept(message);
    }

    /**
     * Выводит сообщение с переносом строки.
     */
    public void printMessage(String message) {
        outputSink.accept(message);
    }

    /**
     * Выводит приглашение и считывает строку от пользователя.
     *
     * @param prompt текст приглашения
     * @return введённая строка (без обрамляющих пробелов) или пустая строка при EOF
     */
    public String ask(String prompt) {
        print(prompt);
        String line = inputSource.readLine();
        return (line != null) ? line.trim() : "";
    }

    /**
     * Запрашивает целое число, повторяя ввод при некорректном значении.
     *
     * @param prompt текст приглашения
     * @return введённое целое число
     */
    public int askInt(String prompt) {
        while (true) {
            try {
                print(prompt);
                String line = inputSource.readLine();
                if (line == null) return 0;
                return Integer.parseInt(line.trim());
            } catch (NumberFormatException e) {
                printLine("Некорректное число. Попробуйте снова.");
            }
        }
    }

    /**
     * Запрашивает число с плавающей точкой, повторяя ввод при некорректном значении.
     *
     * @param prompt текст приглашения
     * @return введённое вещественное число
     */
    public float askFloat(String prompt) {
        while (true) {
            try {
                print(prompt);
                String line = inputSource.readLine();
                if (line == null) return 0;
                return Float.parseFloat(line.trim());
            } catch (NumberFormatException e) {
                printLine("Некорректное число. Попробуйте снова.");
            }
        }
    }

    /**
     * Запрашивает длинное целое число, повторяя ввод при некорректном значении.
     *
     * @param prompt текст приглашения
     * @return введённое значение типа long
     */
    public long askLong(String prompt) {
        while (true) {
            try {
                print(prompt);
                String line = inputSource.readLine();
                if (line == null) return 0;
                return Long.parseLong(line.trim());
            } catch (NumberFormatException e) {
                printLine("Некорректное число. Попробуйте снова.");
            }
        }
    }

    /**
     * Запрашивает дату и время в формате {@code yyyy-MM-dd HH:mm}.
     *
     * @param prompt текст приглашения
     * @return введённая дата-время
     */
    public LocalDateTime askLocalDateTime(String prompt) {
        while (true) {
            try {
                print(prompt + " (формат: yyyy-MM-dd HH:mm): ");
                String line = inputSource.readLine();
                if (line == null) return LocalDateTime.now();
                return LocalDateTime.parse(line.trim(), DATE_FORMATTER);
            } catch (Exception e) {
                printLine("Неверный формат даты. Пример: 2024-01-20 08:00");
            }
        }
    }

    /**
     * Запрашивает непустую строку, выводя сообщение об ошибке при пустом вводе.
     *
     * @param prompt       текст приглашения
     * @param errorMessage сообщение при пустом вводе
     * @return непустая введённая строка
     */
    public String askValidString(String prompt, String errorMessage) {
        while (true) {
            String input = ask(prompt).trim();
            if (input.isEmpty()) {
                printLine(errorMessage);
                continue;
            }
            return input;
        }
    }

    /**
     * Запрашивает непустую строку, выводя сообщение об ошибке при пустом вводе.
     *
     * @param prompt       текст приглашения
     * @param errorMessage сообщение при пустом вводе
     * @return непустая введённая строка
     */
    public String askNonEmptyString(String prompt, String errorMessage) {
        while (true) {
            String input = ask(prompt).trim();
            if (!input.isEmpty()) {
                return input;
            }
            printLine(errorMessage);
        }
    }

    public String getCurrentLogin() {
        return currentLogin;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    /**
     * Сохраняет учётные данные текущей сессии.
     *
     * @param login    логин пользователя
     * @param password пароль пользователя
     */
    public void setSession(String login, String password) {
        this.currentLogin = login;
        this.currentPassword = password;
    }

    /**
     * Интерактивно создаёт нового работника или редактирует существующего.
     * Если {@code existing == null} — режим создания; иначе — режим редактирования,
     * при котором пустой ввод сохраняет текущее значение поля.
     *
     * @param existing существующий работник для редактирования или {@code null} для создания
     * @return новый или изменённый объект {@link Worker}
     */
    public Worker buildWorker(Worker existing) {
        boolean isNew = existing == null;
        printLine("\n═══════════════════════════════════");
        printLine(isNew ? "       СОЗДАНИЕ НОВОГО РАБОТНИКА" : "       РЕДАКТИРОВАНИЕ РАБОТНИКА");
        printLine("═══════════════════════════════════");

        String name = askStringField("Имя", isNew ? null : existing.getName());

        printLine("Координаты:");
        int x = askIntField("  X (макс. 717)", isNew ? null : existing.getCoordinates().getX(),
                v -> v <= 717, "X не может превышать 717!");
        float y = askFloatField("  Y", isNew ? null : existing.getCoordinates().getY());

        int salary = askIntField("Зарплата (больше 0)", isNew ? null : existing.getSalary(),
                v -> v > 0, "Зарплата должна быть больше 0!");

        LocalDateTime startDate = askDateTimeField("Дата начала работы", isNew ? null : existing.getStartDate());

        Position position = askEnumField(Position.class, "Должность", isNew ? null : existing.getPosition());
        Status status = askNullableEnumField(Status.class, "Статус", isNew ? null : existing.getStatus());

        Organization organization = askOrganizationField(isNew ? null : existing.getOrganization());

        if (isNew) {
            return new Worker(0, name, new Coordinates(x, y), salary, startDate, position, status, organization);
        }

        existing.setName(name);
        existing.setCoordinates(new Coordinates(x, y));
        existing.setSalary(salary);
        existing.setStartDate(startDate);
        existing.setPosition(position);
        existing.setStatus(status);
        return existing;
    }

    /** Считывает строку из текущего источника ввода, возвращает пустую строку при EOF. */
    private String readLine() {
        String line = inputSource.readLine();
        return line == null ? "" : line.trim();
    }

    /** Выводит приглашение с текущим значением поля в скобках (если задано). */
    private void printHint(String prompt, Object current) {
        if (current != null) print(prompt + " [" + current + "]: ");
        else print(prompt + ": ");
    }

    /**
     * Запрашивает строковое поле с поддержкой сохранения текущего значения по Enter.
     *
     * @param prompt  название поля
     * @param current текущее значение или {@code null} при создании
     * @return введённое или сохранённое значение поля
     */
    private String askStringField(String prompt, String current) {
        while (true) {
            printHint(prompt, current);
            String input = readLine();
            if (input.isEmpty() && current != null) return current;
            if (!input.isEmpty()) return input;
            printLine("Поле не может быть пустым!");
        }
    }

    /**
     * Запрашивает целочисленное поле с валидацией и поддержкой сохранения текущего значения.
     *
     * @param prompt  название поля
     * @param current текущее значение или {@code null} при создании
     * @param valid   предикат проверки корректности значения
     * @param errMsg  сообщение при провале валидации
     * @return корректное введённое или сохранённое значение
     */
    private int askIntField(String prompt, Integer current, IntPredicate valid, String errMsg) {
        while (true) {
            printHint(prompt, current);
            String input = readLine();
            if (input.isEmpty() && current != null) return current;
            try {
                int v = Integer.parseInt(input);
                if (valid.test(v)) return v;
                printLine(errMsg);
            } catch (NumberFormatException e) {
                printLine("Введите целое число!");
            }
        }
    }

    /**
     * Запрашивает поле типа float с поддержкой сохранения текущего значения.
     *
     * @param prompt  название поля
     * @param current текущее значение или {@code null} при создании
     * @return введённое или сохранённое вещественное значение
     */
    private float askFloatField(String prompt, Float current) {
        while (true) {
            printHint(prompt, current);
            String input = readLine();
            if (input.isEmpty() && current != null) return current;
            try {
                return Float.parseFloat(input);
            } catch (NumberFormatException e) {
                printLine("Введите число!");
            }
        }
    }

    /**
     * Запрашивает поле даты-времени с поддержкой сохранения текущего значения.
     *
     * @param prompt  название поля
     * @param current текущее значение или {@code null} при создании
     * @return введённая или сохранённая дата-время
     */
    private LocalDateTime askDateTimeField(String prompt, LocalDateTime current) {
        String hint = current != null ? current.format(DATE_FORMATTER) : null;
        while (true) {
            if (hint != null) print(prompt + " [" + hint + "] (yyyy-MM-dd HH:mm): ");
            else print(prompt + " (yyyy-MM-dd HH:mm): ");
            String input = readLine();
            if (input.isEmpty() && current != null) return current;
            try {
                return LocalDateTime.parse(input, DATE_FORMATTER);
            } catch (Exception e) {
                printLine("Неверный формат. Пример: 2024-01-20 08:00");
            }
        }
    }

    /**
     * Запрашивает обязательное поле перечисляемого типа с поддержкой сохранения текущего значения.
     *
     * @param enumClass класс перечисления
     * @param prompt    название поля
     * @param current   текущее значение или {@code null} при создании
     * @return выбранное значение перечисления
     */
    private <T extends Enum<T>> T askEnumField(Class<T> enumClass, String prompt, T current) {
        printLine("Доступные значения:");
        for (T v : enumClass.getEnumConstants()) printLine("  - " + v.name());
        if (current != null) printLine("  Текущее: " + current.name() + "  (Enter — оставить)");

        while (true) {
            String input = ask(prompt + ": ").trim();
            if (input.isEmpty() && current != null) return current;
            try {
                return Enum.valueOf(enumClass, input.toUpperCase());
            } catch (IllegalArgumentException e) {
                printLine("Неверное значение! Выберите из списка.");
            }
        }
    }

    /**
     * Запрашивает необязательное поле перечисляемого типа; «-» сбрасывает значение в {@code null}.
     *
     * @param enumClass класс перечисления
     * @param prompt    название поля
     * @param current   текущее значение или {@code null}
     * @return выбранное значение перечисления или {@code null}
     */
    private <T extends Enum<T>> T askNullableEnumField(Class<T> enumClass, String prompt, T current) {
        printLine("Доступные значения:");
        for (T v : enumClass.getEnumConstants()) printLine("  - " + v.name());
        if (current != null)
            printLine("  Текущее: " + current.name() + "  (Enter — оставить, '-' — убрать)");
        else
            printLine("  (Enter — пропустить)");

        while (true) {
            String input = ask(prompt + ": ").trim();
            if (input.isEmpty()) return current;
            if (input.equals("-")) return null;
            try {
                return Enum.valueOf(enumClass, input.toUpperCase());
            } catch (IllegalArgumentException e) {
                printLine("Неверное значение! Выберите из списка.");
            }
        }
    }

    /**
     * Интерактивно запрашивает данные организации: создание, редактирование или удаление.
     *
     * @param current текущая организация или {@code null}
     * @return новая или изменённая организация, либо {@code null} если пользователь отказался
     */
    private Organization askOrganizationField(Organization current) {
        if (current != null) {
            printLine("Текущая организация: " + current.getFullName());
            String choice = ask("  [keep/edit/remove]: ").trim().toLowerCase();
            if (choice.isEmpty() || choice.equals("keep")) return current;
            if (choice.equals("remove")) return null;
        } else {
            String choice = ask("Добавить организацию? (y/n): ").trim().toLowerCase();
            if (!choice.equals("y") && !choice.equals("yes")) return null;
        }

        String fullName = askStringField("  Название", current != null ? current.getFullName() : null);

        Integer annualTurnover = askNullableInt("  Годовой оборот",
                current != null ? current.getAnnualTurnover() : null, v -> v > 0, "Должен быть больше 0!");

        int employeesCount = askIntField("  Количество сотрудников",
                current != null ? current.getEmployeesCount() : null,
                v -> v > 0, "Должно быть больше 0!");

        Address address = askAddressField(current != null ? current.getOfficialAddress() : null);

        return new Organization(fullName, annualTurnover, address, employeesCount);
    }

    /**
     * Запрашивает необязательное целое число; «-» сбрасывает значение в {@code null}.
     *
     * @param prompt  название поля
     * @param current текущее значение или {@code null}
     * @param valid   предикат валидации
     * @param errMsg  сообщение при провале валидации
     * @return введённое значение или {@code null}
     */
    private Integer askNullableInt(String prompt, Integer current, IntPredicate valid, String errMsg) {
        if (current != null) print(prompt + " [" + current + "] (Enter — оставить, '-' — убрать): ");
        else print(prompt + " (Enter — пропустить): ");

        String input = readLine();
        if (input.isEmpty()) return current;
        if (input.equals("-")) return null;
        try {
            int v = Integer.parseInt(input);
            if (valid.test(v)) return v;
            printLine(errMsg);
            return askNullableInt(prompt, current, valid, errMsg);
        } catch (NumberFormatException e) {
            printLine("Введите целое число!");
            return askNullableInt(prompt, current, valid, errMsg);
        }
    }

    /**
     * Запрашивает данные адреса (улица и город).
     *
     * @param current текущий адрес или {@code null}
     * @return новый объект {@link Address}
     */
    private Address askAddressField(Address current) {
        String currentStreet = current != null ? current.getStreet() : null;
        String street = askStringField("  Улица", currentStreet);

        Location currentTown = current != null ? current.getTown() : null;
        Location town = askLocationField(currentTown);
        return new Address(street, town);
    }

    /**
     * Интерактивно запрашивает данные о местоположении (город): создание, сохранение или удаление.
     *
     * @param current текущее местоположение или {@code null}
     * @return новый объект {@link Location} или {@code null}
     */
    private Location askLocationField(Location current) {
        if (current != null) {
            printLine("  Текущий город: x=" + current.getX() + " y=" + current.getY() + " z=" + current.getZ());
            String choice = ask("  [keep/edit/remove]: ").trim().toLowerCase();
            if (choice.isEmpty() || choice.equals("keep")) return current;
            if (choice.equals("remove")) return null;
        } else {
            String choice = ask("  Добавить город? (y/n): ").trim().toLowerCase();
            if (!choice.equals("y") && !choice.equals("yes")) return null;
        }
        long x = askLong("  X: ");
        int y = askInt("  Y: ");
        int z = askInt("  Z: ");
        return new Location(x, y, z);
    }

    // ─── Вывод работника ──────────────────────────────────────────────────────

    /**
     * Выводит подробную информацию об одном работнике в виде рамки.
     *
     * @param w работник для вывода
     */
    public void printWorker(Worker w) {
        printLine("  ╔══════════════════════════════════╗");
        printLine("  ║ Работник #" + w.getId());
        printLine("  ╠══════════════════════════════════╣");
        printLine("  ║ Имя: " + w.getName());
        printLine("  ║ Должность: " + w.getPosition());
        printLine("  ║ Зарплата: " + w.getSalary() + " руб.");
        if (w.getCreationDate() != null)
            printLine("  ║ Дата создания: " + w.getCreationDate().format(DATE_FORMATTER));
        if (w.getStartDate() != null)
            printLine("  ║ Дата начала: " + w.getStartDate().format(DATE_FORMATTER));
        if (w.getStatus() != null)
            printLine("  ║ Статус: " + w.getStatus());
        String orgName = w.getOrganization() != null ? w.getOrganization().getFullName() : "—";
        printLine("  ║ Организация: " + orgName);
        if (w.getOwnerLogin() != null)
            printLine("  ║ Владелец: " + w.getOwnerLogin());
        printLine("  ╚══════════════════════════════════╝");
    }

    /**
     * Выводит список работников с заголовком и счётчиком.
     *
     * @param workers список работников для вывода
     */
    public void printWorkersList(List<Worker> workers) {
        if (workers == null || workers.isEmpty()) {
            printLine("Список сотрудников пуст.");
            return;
        }

        printLine("═══════════════════════════════════");
        printLine("     СПИСОК РАБОТНИКОВ (" + workers.size() + ")");
        printLine("═══════════════════════════════════");

        for (Worker w : workers) {
            printWorker(w);
            printLine("");
        }

        printLine("═══════════════════════════════════");
    }

    /**
     * Освобождает ресурсы консоли (при необходимости переопределяется подклассами).
     */
    public void close() {
    }

    public void printAuth() {
        printLine("1. Login (вход)");
        printLine("2. Register (регистрация)");
    }
}
