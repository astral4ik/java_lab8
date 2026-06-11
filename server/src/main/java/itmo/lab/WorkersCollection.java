package itmo.lab;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import itmo.lab.data.Worker;
import itmo.lab.server.db.WorkerRepository;

/**
 * Потокобезопасная коллекция работников с синхронизацией через ReadWriteLock и персистентностью через БД.
 */
public class WorkersCollection {

    private final TreeMap<Integer, Worker> workers = new TreeMap<>();
    private final LocalDateTime creationTime;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    private final WorkerRepository workerRepo = new WorkerRepository();

    public WorkersCollection() {
        this.creationTime = LocalDateTime.now();
    }

    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    /**
     * Возвращает текущее количество работников в коллекции.
     */
    public int size() {
        readLock.lock();
        try {
            return workers.size();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Загружает всех работников из БД, полностью заменяя содержимое коллекции.
     */
    public void loadFromDatabase() {
        writeLock.lock();
        try {
            workers.clear();
            Map<Integer, Worker> loaded = workerRepo.loadAll();
            workers.putAll(loaded);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Сохраняет нового работника в БД и добавляет его в коллекцию.
     *
     * @param worker     работник для добавления
     * @param ownerLogin Login владельца
     * @return сгенерированный ID работника
     */
    public long insert(Worker worker, String ownerLogin) {
        long generatedId = workerRepo.insert(worker, ownerLogin);
        writeLock.lock();
        try {
            worker.setId((int) generatedId);
            worker.setOwnerLogin(ownerLogin);
            workers.put((int) generatedId, worker);
        } finally {
            writeLock.unlock();
        }
        return generatedId;
    }

    /**
     * Обновляет данные работника в БД и в коллекции, если владелец совпадает.
     *
     * @param worker     работник с обновлёнными данными
     * @param ownerLogin Login владельца
     * @return {@code true}, если запись была обновлена
     */
    public boolean update(Worker worker, String ownerLogin) {
        boolean updated = workerRepo.update(worker, ownerLogin);
        if (updated) {
            writeLock.lock();
            try {
                worker.setOwnerLogin(ownerLogin);
                workers.put(worker.getId(), worker);
            } finally {
                writeLock.unlock();
            }
        }
        return updated;
    }

    /**
     * Удаляет работника из БД и коллекции, если пользователь является его владельцем.
     *
     * @param id         ID работника
     * @param ownerLogin Login владельца
     * @return {@code true}, если работник был удалён
     */
    public boolean remove(int id, String ownerLogin) {
        boolean deleted = workerRepo.delete(id, ownerLogin);
        if (deleted) {
            writeLock.lock();
            try {
                workers.remove(id);
            } finally {
                writeLock.unlock();
            }
        }
        return deleted;
    }

    /**
     * Удаляет всех работников указанного пользователя из БД и коллекции.
     *
     * @param ownerLogin Login владельца
     * @return количество удалённых записей
     */
    public int clear(String ownerLogin) {
        int count = workerRepo.deleteByOwner(ownerLogin);
        writeLock.lock();
        try {
            workers.entrySet().removeIf(
                e -> ownerLogin.equals(e.getValue().getOwnerLogin())
            );
        } finally {
            writeLock.unlock();
        }
        return count;
    }

    /**
     * Возвращает список всех работников коллекции.
     */
    public List<Worker> getAll() {
        readLock.lock();
        try {
            return new ArrayList<>(workers.values());
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Возвращает работника по ID или {@code null}, если не найден.
     */
    public Worker get(int id) {
        readLock.lock();
        try {
            return workers.get(id);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Проверяет наличие работника с указанным ID в коллекции.
     */
    public boolean containsKey(int id) {
        readLock.lock();
        try {
            return workers.containsKey(id);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Возвращает копию внутреннего TreeMap с работниками.
     */
    public TreeMap<Integer, Worker> getWorkers() {
        readLock.lock();
        try {
            return new TreeMap<>(workers);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Удаляет всех работников пользователя с ID меньше заданного.
     *
     * @param id         пороговый ID
     * @param ownerLogin Login владельца
     * @return количество удалённых работников
     */
    public int removeLower(int id, String ownerLogin) {
        readLock.lock();
        List<Integer> toDelete;
        try {
            toDelete = workers.values().stream()
                .filter(w -> w.getId() < id && ownerLogin.equals(w.getOwnerLogin()))
                .map(Worker::getId)
                .collect(Collectors.toList());
        } finally {
            readLock.unlock();
        }
        int count = 0;
        for (int wId : toDelete) {
            if (remove(wId, ownerLogin)) count++;
        }
        return count;
    }

    /**
     * Удаляет всех работников пользователя с ID больше заданного.
     *
     * @param id         пороговый ID
     * @param ownerLogin Login владельца
     * @return количество удалённых работников
     */
    public int removeGreaterKey(int id, String ownerLogin) {
        readLock.lock();
        List<Integer> toDelete;
        try {
            toDelete = workers.values().stream()
                .filter(w -> w.getId() > id && ownerLogin.equals(w.getOwnerLogin()))
                .map(Worker::getId)
                .collect(Collectors.toList());
        } finally {
            readLock.unlock();
        }
        int count = 0;
        for (int wId : toDelete) {
            if (remove(wId, ownerLogin)) count++;
        }
        return count;
    }

    /**
     * Проверяет, пуста ли коллекция.
     */
    public boolean isEmpty() {
        readLock.lock();
        try {
            return workers.isEmpty();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Проверяет, является ли указанный пользователь владельцем работника с данным ID.
     *
     * @param id    ID работника
     * @param login Login пользователя
     * @return {@code true}, если пользователь является владельцем
     */
    public boolean isOwner(int id, String login) {
        readLock.lock();
        try {
            Worker w = workers.get(id);
            return w != null && login.equals(w.getOwnerLogin());
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Возвращает список уникальных дат начала работы среди всех работников.
     */
    public List<LocalDateTime> getUniqueStartDates() {
        readLock.lock();
        try {
            List<LocalDateTime> dates = new ArrayList<>();
            for (Worker w : workers.values()) {
                if (!dates.contains(w.getStartDate())) {
                    dates.add(w.getStartDate());
                }
            }
            return dates;
        } finally {
            readLock.unlock();
        }
    }
}
