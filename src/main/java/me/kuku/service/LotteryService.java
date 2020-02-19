package me.kuku.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import me.kuku.bean.User;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class LotteryService {

    private CloseableHttpClient httpClient = null;
    private BasicCookieStore basicCookieStore = null;

    public LotteryService(){
        basicCookieStore = new BasicCookieStore();
        httpClient = HttpClients.custom().setDefaultCookieStore(basicCookieStore).build();
    }

    public String getUserId(){
        HttpGet httpGet = new HttpGet("http://m.client.10010.com/sma-lottery/qpactivity/qingpiindex");
        String id = null;
        try {
            CloseableHttpResponse execute = httpClient.execute(httpGet);
            if (execute.getStatusLine().getStatusCode() == 200){
                String html = EntityUtils.toString(execute.getEntity(), "utf8");
                List<Cookie> cookies = basicCookieStore.getCookies();
                for (Cookie cookie : cookies){
                    if (cookie.getName().equals("JSESSIONID")){
                        id = cookie.getValue();
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return id;
    }

    public String getCaptchaUrl(String userId){
        return "http://m.client.10010.com/sma-lottery/qpactivity/getSysManageLoginCode.htm?userid=" + userId + "&code=" + new Date().getTime();
    }

    public String getCaptcha(String captchaUrl){
        HttpPost httpPost = new HttpPost("http://47.94.234.77:9527/getCode");
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("url", captchaUrl));
        String str = null;
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params, "utf8"));
            CloseableHttpResponse execute = httpClient.execute(httpPost);
            str = EntityUtils.toString(execute.getEntity(), "utf8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return str;
    }

    public byte[] getCaptchaImg(String userId){
        HttpGet httpGet = new HttpGet(getCaptchaUrl(userId));
        byte[] byteArr = null;
        InputStream is = null;
        ByteArrayOutputStream bao = null;
        try {
            CloseableHttpResponse execute = httpClient.execute(httpGet);
            if (execute.getStatusLine().getStatusCode() == 200) {
                is = execute.getEntity().getContent();
                bao = new ByteArrayOutputStream();
                int len;
                byte[] buffer = new byte[1024];
                while ((len = is.read(buffer)) != -1){
                    bao.write(buffer, 0 , len);
                }
                byteArr = bao.toByteArray();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (is != null){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bao != null){
                try {
                    bao.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return byteArr;
    }

    //返回值为null  为验证码错误
    public String getMobile(String phone, String captcha, String userId){
        HttpPost httpPost = new HttpPost("http://m.client.10010.com/sma-lottery/validation/qpImgValidation.htm");
        String mobile = null;
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("mobile", phone));
        params.add(new BasicNameValuePair("image", captcha));
        params.add(new BasicNameValuePair("userid", userId));
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params, "utf8"));
            CloseableHttpResponse execute = httpClient.execute(httpPost);
            if (execute.getStatusLine().getStatusCode() == 200){
                String result = EntityUtils.toString(execute.getEntity(), "utf8");
                //{"code":"YES","mobile":"fb2f3802770d041736ccb642ef96adc1"}
                JSONObject jsonObject = JSON.parseObject(result);
                if (jsonObject.getString("code").equals("YES")){
                    mobile = jsonObject.getString("mobile");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mobile;
    }

    public String lottery(String encryptPhone, String captcha, String userId){
        HttpPost httpPost = new HttpPost("http://m.client.10010.com/sma-lottery/qpactivity/qpLuckdraw.htm");
        String gift = "";
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("mobile", encryptPhone));
        params.add(new BasicNameValuePair("image", captcha));
        params.add(new BasicNameValuePair("userid", userId));
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params, "utf8"));
            CloseableHttpResponse execute = httpClient.execute(httpPost);
            if (execute.getStatusLine().getStatusCode() == 200){
                String result = EntityUtils.toString(execute.getEntity(), "utf8");
                //{"data":{"assetCategory":"0","assetName":"100MB","everyDayNum":"2000","id":"qp100","level":"6","price":"100","prizeCount":"17694885424"},"isunicom":true,"msg":"2","status":200}
                //{"data":{"assetName":"幸运奖","level":"3","prizeCount":"15529772351"},"isunicom":true,"msg":"1","status":0}
                //{"data":{"assetName":"幸运奖","level":"3","prizeCount":"15529772351"},"isunicom":true,"msg":"0","status":0}
                //看level的值
                //1、50mb流量   2、100mb流量  3、200mb流量  4、1000mb流量  5、20砖石  6、15元开卡红包  7、50元开卡红包
                //1、100mb流量  2、50元开卡礼包  3、幸运奖  4、50mb流量  5、10000mb流量  6、15元开卡礼包  7、20钻石
                //1、50mb流量  2、100mb流量  3、幸运奖  4、10000mb流量  5、20钻石  6、15元开卡礼包  7、50元开卡礼包
                //isunicom=false  错误 然后读msg
                //status = 200 or status = 0 正常
                //status = 500 没有抽奖次数了
                //status = 400 or status = 700 抽奖人数过多
                JSONObject jsonObject = JSON.parseObject(result);
                Integer status = jsonObject.getInteger("status");
                if (status == 500){
                    return "没有抽奖次数了";
                }else if (status == 400 || status == 700){
                    return "抽奖人数过多";
                }else if (jsonObject.getBoolean("isunicom").equals(false)){
                    return "错误：" + jsonObject.getString("msg");
                }else if (status == 200 || status == 0){
                    String level = jsonObject.getJSONObject("data").getString("level");
                    switch (level){
                        case "1":
                            gift = "50mb流量";
                            break;
                        case "2":
                            gift = "100mb流量";
                            break;
                        case "3":
                            gift = "幸运奖";
                            break;
                        case "4":
                            gift = "1000mb流量";
                            break;
                        case "5":
                            gift = "20砖石";
                            break;
                        case "6":
                            gift = "15元开卡礼包";
                            break;
                        case "7":
                            gift = "50元开卡礼包";
                            break;
                        default:
                            gift = "未知奖品";
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return gift;
    }

    public String run(String phone, User user, BaiDuAIService baiDuAIService){
        String userId = getUserId();
        byte[] captchaImg = null;
        String encryptMobile = null;
        String code = null;
        String gifts = "";
        while (true){
            code = getCaptcha(getCaptchaUrl(userId));
            if (code == null){
                captchaImg = getCaptchaImg(userId);
                code = baiDuAIService.Literacy(captchaImg, user);
            }
            if (code == null){
                continue;
            }
            encryptMobile = getMobile(phone, code, userId);
            if (encryptMobile != null){
                break;
            }
        }
        for (int i = 0; i < 3; i++) {
            gifts += lottery(encryptMobile, code, userId) + "；";
        }
        return gifts;
    }
}
