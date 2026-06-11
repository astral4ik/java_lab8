package itmo.lab.server;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import itmo.lab.WorkersCollection;
import itmo.lab.common.Request;
import itmo.lab.server.db.DatabaseManager;

/**
 * Многопоточный TCP-сервер приложения: принимает подключения клиентов и обрабатывает запросы.
 */
public class Server {

    private static final Logger logger = LogManager.getLogger(Server.class);

    private static final int PORT = 6767;

    private final WorkersCollection collection;
    private final RequestHandler requestHandler;

    private final ExecutorService readPool = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors());
    private final ExecutorService processPool = Executors.newCachedThreadPool();
    private final ExecutorService sendPool = Executors.newCachedThreadPool();

    /**
     * @param collection коллекция работников, которой управляет сервер
     */
    public Server(WorkersCollection collection) {
        this.collection = collection;
        this.requestHandler = new RequestHandler(collection);
    }

    /**
     * Загружает коллекцию из БД и запускает цикл приёма клиентских подключений.
     */
    public void start() throws IOException {
        logger.info("Загрузка коллекции из базы данных...");

        collection.loadFromDatabase();
        logger.info("Загружено " + collection.size() + " работников из БД");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("Сервер запущен на порту " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("Подключился клиент: " + clientSocket.getInetAddress());
                readPool.submit(() -> handleConnection(clientSocket));
            }
        }
    }

    /**
     * Обслуживает отдельное клиентское соединение в цикле чтения запросов.
     */
    private void handleConnection(Socket socket) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            while (!socket.isClosed()) {
                final Request request;
                try {
                    request = (Request) in.readObject();
                } catch (EOFException | SocketException e) {
                    logger.info("Клиент отключился: " + socket.getInetAddress());
                    break;
                }

                CompletableFuture.supplyAsync(() -> requestHandler.handle(request), processPool)
                    .thenAcceptAsync(response -> {
                        try {
                            synchronized (out) {
                                out.reset();
                                out.writeObject(response);
                                out.flush();
                            }
                        } catch (IOException e) {
                            logger.error("ERROR при отправке ответа: " + e.getMessage());
                        }
                    }, sendPool);
            }

        } catch (IOException | ClassNotFoundException e) {
            logger.error("ERROR соединения: " + e.getMessage());
        } finally {
            closeQuietly(socket);
        }
    }

    /**
     * Закрывает сокет, подавляя возможное исключение.
     */
    private void closeQuietly(Socket socket) {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}
    }

    /**
     * Точка входа: инициализирует DB-соединение и запускает сервер.
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Введите: java -jar Server.jar <db_user> <db_password>");
            System.exit(1);
        }

        String dbUser = args[0];
        String dbPassword = args[1];

        DatabaseManager.init(dbUser, dbPassword);

        WorkersCollection collection = new WorkersCollection();
        Server server = new Server(collection);

        try {
            server.start();
        } catch (IOException e) {
            logger.error("ERROR сервера: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
