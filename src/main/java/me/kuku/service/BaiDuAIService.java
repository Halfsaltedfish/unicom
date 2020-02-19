package me.kuku.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import me.kuku.bean.Key;
import me.kuku.bean.User;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class BaiDuAIService {


    private CloseableHttpClient httpClient = null;

    private int num = 0;
    private int i = -1;
    private String token = "";


    public BaiDuAIService(){
        httpClient = HttpClients.custom().build();
    }

    //
    public String getToken(String API_KEY, String SECRET_KEY){
        HttpGet httpGet = new HttpGet("https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id=" + API_KEY +
                "&client_secret=" + SECRET_KEY);
        String token = null;
        try {
            CloseableHttpResponse execute = httpClient.execute(httpGet);
            if (execute.getStatusLine().getStatusCode() == 200){
                String result = EntityUtils.toString(execute.getEntity(), "utf8");
                token = JSON.parseObject(result).getString("access_token");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return token;
    }

    public String Literacy(byte[] imgByte, User user){
//        HttpPost httpPost = new HttpPost("https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic?access_token=" + ACCESS_TOKEN);
        if (num == 0 || num > 198){
            num = 0;
            i++;
            if (i == user.getKey().size()){
                throw new RuntimeException("木得免费次数了");
            }
            token = getToken(user.getKey().get(i).getApiKey(), user.getKey().get(i).getSecretKey());
        }
        num++;
        HttpPost httpPost = new HttpPost("https://aip.baidubce.com/rest/2.0/ocr/v1/numbers?access_token=" + token);
        String words = null;
        List<NameValuePair> params = new ArrayList<>();
        Base64.Encoder encoder = Base64.getEncoder();
        String s = encoder.encodeToString(imgByte);
        try {
            params.add(new BasicNameValuePair("image", s));
            httpPost.setEntity(new UrlEncodedFormEntity(params, "utf8"));
            CloseableHttpResponse execute = httpClient.execute(httpPost);
            if (execute.getStatusLine().getStatusCode() == 200){
                String result = EntityUtils.toString(execute.getEntity(), "utf8");
                JSONArray word = JSON.parseObject(result).getJSONArray("words_result");
                if (word == null || word.size() == 0){
                    return null;
                }
                words = word.getJSONObject(0).getString("words");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return words;
    }

}
