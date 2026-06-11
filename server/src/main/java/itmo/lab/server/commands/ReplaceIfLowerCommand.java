package itmo.lab.server.commands;

import itmo.lab.common.Response;
import itmo.lab.common.WorkerArgument;
import itmo.lab.common.IdArgument;
import itmo.lab.data.Worker;
import itmo.lab.WorkersCollection;

import java.io.Serializable;

/**
 * Команда замены работника, если зарплата нового работника меньше текущей.
 */
public class ReplaceIfLowerCommand implements ServerCommand {

    private final WorkersCollection collection;

    public ReplaceIfLowerCommand(WorkersCollection collection) {
        this.collection = collection;
    }

    @Override
    public Response execute(Serializable args, String ownerLogin) {
        Object[] arr = (Object[]) args;
        int id = ((IdArgument) arr[0]).getId();
        Worker newWorker = (Worker) ((WorkerArgument) arr[1]).getWorker();

        if (!collection.isOwner(id, ownerLogin)) {
            return new Response(false, "Нет прав на редактирование: вы не являетесь владельцем", null);
        }

        Worker existing = collection.get(id);

        if (newWorker.getSalary() < existing.getSalary()) {
            newWorker.setId(id);
            collection.update(newWorker, ownerLogin);
            return new Response(true, "Worker обновлён (зарплата нового работника меньше)", null);
        }

        return new Response(false, "Зарплата нового работника не меньше текущей", null);
    }

    @Override
    public boolean isModifying() {
        return true;
    }
}
