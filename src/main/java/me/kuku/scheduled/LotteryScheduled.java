package me.kuku.scheduled;

import me.kuku.bean.User;
import me.kuku.entity.PhoneLa;
import me.kuku.entity.Prize;
import me.kuku.repository.PhoneRepository;
import me.kuku.repository.PrizeRepository;
import me.kuku.service.BaiDuAIService;
import me.kuku.service.LotteryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.util.List;

@Component
@EnableScheduling
public class LotteryScheduled {

    @Autowired
    LotteryService lotteryService;
    @Autowired
    PhoneRepository phoneRepository;
    @Autowired
    PrizeRepository prizeRepository;
    @Autowired
    User user;

    @Scheduled(cron = "${user.cron}")
    public void flow() throws Exception{
        List<PhoneLa> phoneAll = phoneRepository.findAll();
        String phone = "";
        for (PhoneLa phoneLa : phoneAll){
            phone = phoneLa.getPhone();
            String gifts = lotteryService.run(phone, user, new BaiDuAIService());
            prizeRepository.save(new Prize(null, phone, gifts, new Date(new java.util.Date().getTime())));
        }
    }
}
