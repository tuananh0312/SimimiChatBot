package Sercurity;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESUtil {
    
    //AES-128
    //Khóa secret key
    private SecretKey key;
    
    //Kích cỡ khóa
    private final int KEY_SIZE = 128;
    
     //Chiều dài tin nhắn sau mã hóa
    private final int T_LEN = 128;
    
    //Bộ mã hóa và giải mã
    private Cipher encryptionCipher;
    
    //Dãy số byte[] tạo ngẫu nhiên từ bộ mã hóa và giải mã
    private byte[] keytwo;

    public SecretKey getKey() {
        return key;
    }

    public void setKey(SecretKey key) {
        this.key = key;
    }
    
    //Hàm khởi tạo key, bộ mã hóa giải mã và dẫy số byte[]
    public void init() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(KEY_SIZE);
        key = generator.generateKey();
        encryptionCipher = Cipher.getInstance("AES/GCM/NoPadding");
        this.keytwo = encryptionCipher.getIV();
    }

    //Mã hóa dùng cho mã hóa key
    public String encrypt(String message) throws Exception {
        byte[] messageInBytes = message.getBytes();
        encryptionCipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedBytes = encryptionCipher.doFinal(messageInBytes);
        return encode(encryptedBytes);
    }
    
    //Mã hóa tin nhắn
    public String encrypt(String strToEncrypt, String myKey) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            byte[] key = myKey.getBytes("UTF-8");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        return null;
    }
    
    //Giải mã tin nhắn
    public String decrypt(String strToDecrypt, String myKey) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            
            //Chuyển key về dạng byte UTF-8
            byte[] key = myKey.getBytes("UTF-8");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        return null;
    }
    
    //Giải mã key
    public String decrypt(String encryptedMessage) throws Exception {
        //Chuyển string về dạng byte[]
        byte[] messageInBytes = decode(encryptedMessage);
        
        //khởi tạo bộ mã hóa giải mã
        Cipher decryptionCipher = Cipher.getInstance("AES/GCM/NoPadding");
        
        //Đưa số byte[] vào
        GCMParameterSpec spec = new GCMParameterSpec(T_LEN, this.keytwo);
        
        //Thực hiện giải mã với secrect key
        decryptionCipher.init(Cipher.DECRYPT_MODE, key, spec);
        byte[] decryptedBytes = decryptionCipher.doFinal(messageInBytes);
        
        //trả về chuỗi sau khi giải mã
        return new String(decryptedBytes);
    }
    
    //Chuyển byte[] về string theo Base64
    public String encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }
    
    //Chuyển string về byte[] theo Base64
    public byte[] decode(String data) {
        return Base64.getDecoder().decode(data);
    }
    
    
    
    //Chạy thử
    public static void main(String[] args) {
        try {
            AESUtil aes1 = new AESUtil();
            AESUtil aes2 = new AESUtil();

            aes1.init();
            aes2.init();
            String encodedkey1 = Base64.getEncoder().encodeToString(aes1.getKey().getEncoded());
            byte[] decodedKey = Base64.getDecoder().decode(encodedkey1);
            SecretKey key = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
            aes2.setKey(key);

            String encryptedMessage = aes1.encrypt("TheXCoders", aes1.encode(aes1.getKey().getEncoded()));
            String decryptedMessage = aes2.decrypt(encryptedMessage, aes2.encode(aes2.getKey().getEncoded()));

            System.out.println("Encrypted Message : " + encryptedMessage);
            System.out.println("Decrypted Message : " + decryptedMessage);

        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }
}
