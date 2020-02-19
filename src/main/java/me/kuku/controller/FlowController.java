package me.kuku.controller;

import me.kuku.bean.User;
import me.kuku.entity.PhoneLa;
import me.kuku.entity.Prize;
import me.kuku.repository.PhoneRepository;
import me.kuku.repository.PrizeRepository;
import me.kuku.service.FlowService;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Controller
public class FlowController {
    @Autowired
    PhoneRepository phoneRepository;
    @Autowired
    FlowService flowService;
    @Autowired
    PrizeRepository prizeRepository;
    @Autowired
    User user;

    @RequestMapping("/")
    public String toIndex(Map<String, Object> map){
        map.put("con", user.getHtmlCon());
        map.put("max", user.getMax());
        return "index";
    }

    @RequestMapping("/add")
    @ResponseBody
    public Integer addPhone(PhoneLa phoneLa){
        PhoneLa phoneObj = phoneRepository.findByPhone(phoneLa.getPhone());
        if (!flowService.checkUnicom(phoneLa.getPhone())){
            return -2;
        }
        int num = phoneRepository.findAll().size();
        if (num > user.getMax()){
            return -1;
        }
        if (phoneObj != null){
            return 0;
        }
        PhoneLa save = phoneRepository.save(phoneLa);
        return 1;
    }

    @RequestMapping("/query")
    @ResponseBody
    public List<Prize> queryPhone(@RequestParam("phone") String phone){
        List<Prize> prizeList = prizeRepository.findAllByPhone(phone);
        return prizeList;
    }

    @RequestMapping("/delete")
    @ResponseBody
    public String deletePhone(@RequestParam("phone") String phone){
        PhoneLa phoneLa = phoneRepository.findByPhone(phone);
        if (phoneLa == null){
            return "未找到该号码";
        }else{
            phoneRepository.delete(phoneLa);
            return "删除成功";
        }
    }

    @RequestMapping("/get")
    @ResponseBody
    public Integer getCaptcha(@RequestParam("phone") String phone, HttpServletRequest request){
        //不知道联通识别验证码是不是需要cookie来识别的。。所以把httpClient存在session中
        FlowService flow = new FlowService();
        Integer i = flow.getCaptcha(phone);
        if (i == 1){
            HttpSession session = request.getSession();
            session.setMaxInactiveInterval(60 * 5);
            session.setAttribute("client", flow.getHttpClient());
        }
        return i;
    }

    @RequestMapping("/verify")
    @ResponseBody
    public boolean verifyCaptcha(@RequestParam("phone") String phone, @RequestParam("captcha") String captcha, HttpServletRequest request){
        HttpSession session = request.getSession();
        CloseableHttpClient client = (CloseableHttpClient) session.getAttribute("client");
        if (client == null){
            //过期了
            return false;
        }else{
            FlowService flow = new FlowService();
            flow.setHttpClient(client);
            boolean b = flow.receiveFlow(phone, captcha);
            if (b) {
                session.invalidate();
            }
            return b;
        }
    }
}
