package Tools;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class LanguageTranslator {

    public LanguageTranslator() {

    }

    public String getdata(String tp) {
        String result = "";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://google-translate1.p.rapidapi.com/language/translate/v2"))
                    .header("content-type", "application/x-www-form-urlencoded")
                    .header("accept-encoding", "application/gzip").header("x-rapidapi-key",
                    "8c67cd1297mshbb0ccc63502ff9dp1c9123jsnee1a70e1ebbb")
                    .header("x-rapidapi-host", "google-translate1.p.rapidapi.com")
                    .method("POST", HttpRequest.BodyPublishers.ofString("q=" + tp + "&target=en&source=vi")).build();
            HttpResponse<String> res = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            //Đưa dữ liệu lấy được vào trong obj
            JSONObject obj = new JSONObject(res.body());
            //Nếu trả về cod = 200 tức là thành công

            JSONObject obj1 = obj.getJSONObject("data");
            JSONArray obj3 = obj1.getJSONArray("translations");
            JSONObject obj2 = obj3.getJSONObject(0);
            result = obj2.getString("translatedText");
        } catch (IOException e) {
            return "API hết hạn sử dụng rồi";
        } catch (InterruptedException ex) {
            return "API hết hạn sử dụng rồi";
        }
        return result;
    }

    public static void main(String[] args) throws IOException {
        LanguageTranslator w = new LanguageTranslator();
        System.out.println(w.getdata("Hồ chí minh"));
    }
}
