package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

public class Server {

    private ServerSocket serverSocket;

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public void startServer() throws NoSuchAlgorithmException {

        try {
            while (!serverSocket.isClosed()) {
                
                //Khi server kết nối thì nó sẽ tiếp tục nhân socket từ server
                Socket socket = serverSocket.accept();
                
                //Khi có kết nối nó sẽ thêm vào ClientHandler xử lý
                System.out.println("A new client has connected");
                ClientHandler clientHandler = new ClientHandler(socket);
                
                //Tạo luồng cho client đó chạy
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e) {

        }
    }

    public void closeServerSocket() {

        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
        }
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {

        ServerSocket serverSocket = new ServerSocket(1508);
        Server server = new Server(serverSocket);
        server.startServer();
    }
}
