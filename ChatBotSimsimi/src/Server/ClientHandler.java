package Server;

import Sercurity.AESUtil;
import Sercurity.RSAUtil;
import Tools.CurrencyExchange;
import Tools.IpLocateAPI;
import Tools.LanguageTranslator;
import Tools.PortScan;
import Tools.SimsimiAPI;
import Tools.VNCharacterUtils;
import Tools.WeatherAPI;
import Tools.WhoIs;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class ClientHandler implements Runnable {
    
    //Array list chứa danh sách các client
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUsername;
    private RSAUtil rsa;
    private SecretKey key;
    private AESUtil aes;

    public ClientHandler(Socket socket) throws NoSuchAlgorithmException {
        try {
            //Khởi tạo RSA và AES của client server tương ứng client đó
            rsa = new RSAUtil();
            aes = new AESUtil();
            aes.init();
            
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            //Đọc tên username nhận từ client lần đầu kết nối
            this.clientUsername = bufferedReader.readLine();
            
            //Thêm client vào ArrayList
            clientHandlers.add(this);
            
            //Thực hiện hàm gửi public key 
            sendkey(rsa.getPublickey());
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        } catch (Exception ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        String messageFromClient;
        
        //Nếu mà client kết nối với server
        while (socket.isConnected()) {
            try {
                //Đọc tin nhắn từ client
                messageFromClient = bufferedReader.readLine();
                String msg = messageFromClient;
                try {
                     //Giải mã rsa lấy secret key
                    if (rsa.Decrypt(msg).contains("#secretkey#")) {
                        messageFromClient = rsa.Decrypt(msg);
                    }
                } catch (InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
                }
                //Thông báo nhận từ client
                System.out.println("Tin nhắn từ " + clientUsername + ": " + messageFromClient);
                
                //Kiểm tra điều kiện trong tin nhắn
                if (messageFromClient.contains("#secretkey#")) {
                    //Nếu có #secretkey# thì lấy key ra và gắn vào bộ AES của server
                    String params = messageFromClient.split("#secretkey#")[1];
                    System.out.println("Secret Key: " + params);
                    byte[] decodedKey = Base64.getDecoder().decode(params);
                    this.key = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
                    aes.setKey(key);
                } else {
                    
                    //Thực hiện giải mã tin nhắn nhận được
                    String text = "Hello";
                    try {
                        //Giải mã tin nhắn
                        text = aes.decrypt(messageFromClient, aes.encode(aes.getKey().getEncoded()));
                    } catch (Exception e) {
                    }
                    //Trả kết qua simsimi
                    String result = getsimi(text);
                    //Mã hóa tin nhắn simsimi để gửi cho client
                    System.out.println("Simsimi trả lời: " + aes.encrypt(result, aes.encode(aes.getKey().getEncoded())));
                    broadcastMessage(aes.encrypt(result, aes.encode(aes.getKey().getEncoded())));
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            } catch (Exception ex) {
                Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    //Hàm gửi public key
    public void sendkey(PublicKey publics) {
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                //Nếu client username bằng đúng client đó thì lấy ra và gửi cho nó
                if (clientHandler.clientUsername.equals(clientUsername)) {
                    //Public key chuyển về dạng byte[] rồi chuyển sang dạng string
                    String publicK = Base64.getEncoder().encodeToString(publics.getEncoded());
                    System.out.println("Public key: " + publicK);
                    
                    //Gửi public key đi
                    clientHandler.bufferedWriter.write("#publickey#" + publicK);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            } catch (Exception ex) {
                Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    //Hàm Gửi tin nhắn
    public void broadcastMessage(String messageFromClient) {
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                if (clientHandler.clientUsername.equals(clientUsername)) {
                    clientHandler.bufferedWriter.write(messageFromClient);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            } catch (Exception ex) {
                Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    //Xử lý khi người dùng thoát ra
    public void removeClientHandler() {
        clientHandlers.remove(this);
        broadcastMessage("SERVER: " + clientUsername + " has left the chat");
    }
    
    
    //Đóng socket, bộ đọc ghi của client lại sau khi thoát ra
    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        removeClientHandler();
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
        }
    }
    
    
    //Hàm chức năng simsimi và các tools khác
    public String getsimi(String text) {
        String result = "";
        if (text.equals("hello")) {
            result = "Hello,tui là simsimi là 1 AI chính cống nhé"
                    + "\nNgoài khả năng tám chuyện ra tui còn có siêu năng lực làm được nhiều thứ\r\n"
                    + "Để liệt kê ra cho biết nà: \r\n"
                    + "\r\n"
                    + "Thứ nhất: Khả năng nhìn trời nhìn đất đoán mây đoán gió đoán mưa\r\n"
                    + "*Lệnh nhập: #weather# Tên 1 địa điểm\n"
                    + "\r\n"
                    + "Thứ hai: Xác định vị trí của bất kì thằng ất ơ và bằng cách tra địa chỉ IP nó\r\n"
                    + "*Lệnh nhập: #ip# IP hoặc tên miền\n"
                    + "\r\n"
                    + "Thứ ba: Khả năng nhìn ra kinh mạch của 1 trang web :V\r\n"
                    + "*Lệnh nhập: #portscan# tên miền hoặc IP bất kì; port bắt đầu ; port kết thúc\r\n"
                    + "\r\n"
                    + "Thứ tư: Khả năng đổi đơn vị tiền tệ siu tốc\r\n"
                    + "*Lệnh nhập: #exchange#số tiền#Đơn vị của nó;Đơn vị muốn đổi"
                    + "\r\n"
                    + "Thứ năm: Khả năng nhìn ra chủ sở hữu của tên miền\r\n"
                    + "*Lệnh nhập: #whois# tên miền hoặc ip\r\n";
            
            //Tool thời tiết
        } else if (text.matches("^#weather#[^/ ].*$")) {
            String params = text.split("(#weather#)")[1];
            String translate  = new LanguageTranslator().getdata(params);
            result = new WeatherAPI().getdata(translate);
            
            //Tool quét port
        } else if (text.matches("^\\#portscan\\#([^/ ].*);([^/ ].*);([^/ ].*)$")) {
            String[] params = text.split("(#portscan#)");
            String[] params2 = params[1].split(";");
            result = new PortScan().scan(params2[0], params2[1], params2[2]);
            
            //Tool ip
        } else if (text.matches("^#ip#[^/ ].*$")) {
            String params = text.split("(#ip#)")[1];
            result = new IpLocateAPI().getdata(params);
            
            //Tool Whois
        } else if (text.matches("^#whois#[^/ ].*$")) {
            String params = text.split("(#whois#)")[1];
            result = new WhoIs().getdata(params);
            
            //Tool đổi tiền tệ
        } else if (text.matches("^#exchange#([^/ ].*);([^/ ].*);([^/ ].*)$")) {
            String[] params = text.split("(#exchange#)");
            String[] params2 = params[1].split(";");
            result = new CurrencyExchange().getdata(params2[0], params2[1], params2[2]);
            
            //Tool Simsimi trả lời nếu các Tool trên không được gọi
        } else {
            result = new SimsimiAPI().getdata(text);
        }
        if (result == null) {
            result = "Hết 100 tin nhắn rồi m không tám chuyện với t dc nữa đâu :3";
        }
        return result;
    }
}
