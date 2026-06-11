package itmo.lab.client;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Источник ввода из файла.
 */
public class FileInputSource implements InputSource, AutoCloseable {

    private final BufferedReader reader;

    /**
     * Открывает файл по указанному пути для построчного чтения.
     *
     * @param filePath путь к файлу
     * @throws IOException если файл не найден или недоступен
     */
    public FileInputSource(String filePath) throws IOException {
        this.reader = new BufferedReader(new FileReader(filePath));
    }

    /**
     * Читает следующую строку из файла.
     *
     * @return строка или {@code null} при достижении конца файла
     */
    @Override
    public String readLine() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Закрывает файловый поток чтения.
     */
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }
}
