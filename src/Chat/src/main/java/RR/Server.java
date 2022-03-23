package RR;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final Logger LOGGER = LogManager.getLogger(Server.class);
    private final AuthService authService;
    private final Map<String, ClientHandler> clients;
    private File f = new File("chat_log.txt");
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    public AuthService getAuthService() {
        return authService;
    }

    public Server() {
        this.authService = new JdbcRunner();
        LOGGER.info("Запущена БД");
        this.clients = new HashMap<>();
    }

    public void run() {

        try (ServerSocket serverSocket = new ServerSocket(8180)) {
            while (true) {
                final Socket socket = serverSocket.accept();
                new ClientHandler(socket, this, threadPool);
                LOGGER.info("Новый клиент подключился");
            }
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error("Ошибка подключения");
        } finally {
            LOGGER.info("Сервер отключен");
            getAuthService().disconnect();

        }
    }

    public boolean isNickBusy(String nick) {
        return clients.containsKey(nick);
    }

    public void subscribe(ClientHandler client) {
        clients.put(client.getNick(), client);
        onLineClients();
        LOGGER.info("Клиент прошел авторизацию");
    }

    public void unsubscribe(ClientHandler client) {
        clients.remove(client.getNick());
        onLineClients();
        LOGGER.info("Клиент отключился");
    }

    public void broadcast(String s) {
        for (ClientHandler client : clients.values()) {
            client.sendMessage(s);
            LOGGER.info("Отправлено сообщение в общий чат");
        }
    }

    public void privMsg(String message, String receiver) {
        for (ClientHandler client : clients.values()) {
            if (client.getNick().equals(receiver)) {
                client.sendMessage(message);
                LOGGER.info("Отправлено приватное сообщение");
            }

        }
    }

    public void onLineClients() {
        StringBuilder list = new StringBuilder("/clients ");
        for (ClientHandler client : clients.values()) {
            list.append(client.getNick()).append(" ");
        }
        broadcast(list.toString());

    }

    public void logSaver(String msg) throws IOException {

        final BufferedWriter writer = new BufferedWriter(new FileWriter(f, true));


        new Thread(() -> {
            try {
                writer.write(msg + "\n");
                writer.flush();
                System.out.println("Запись в лог: " + msg);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }).start();
    }


}





