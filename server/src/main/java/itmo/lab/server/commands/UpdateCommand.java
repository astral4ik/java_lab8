package itmo.lab.server.commands;

import itmo.lab.common.Response;
import itmo.lab.common.IdArgument;
import itmo.lab.common.WorkerArgument;
import itmo.lab.data.Worker;
import itmo.lab.WorkersCollection;

import java.io.Serializable;

/**
 * Команда обновления данных существующего работника по ID.
 */
public class UpdateCommand implements ServerCommand {

    private final WorkersCollection collection;

    public UpdateCommand(WorkersCollection collection) {
        this.collection = collection;
    }

    @Override
    public Response execute(Serializable args, String ownerLogin) {
        Object[] arr = (Object[]) args;
        int id = ((IdArgument) arr[0]).getId();
        Worker worker = (Worker) ((WorkerArgument) arr[1]).getWorker();
        worker.setId(id);

        if (!collection.isOwner(id, ownerLogin)) {
            return new Response(false, "Нет прав на редактирование: вы не являетесь владельцем", null);
        }

        collection.update(worker, ownerLogin);
        return new Response(true, "Worker успешно обновлён", null);
    }

    @Override
    public boolean isModifying() {
        return true;
    }
}
