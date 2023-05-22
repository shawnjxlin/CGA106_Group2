package com.ezticket.web.activity.service;

import com.ezticket.core.util.ImageUtil;
import com.ezticket.web.activity.dto.ActivityDto;
import com.ezticket.web.activity.pojo.Activity;
import com.ezticket.web.activity.pojo.Aimgt;
import com.ezticket.web.activity.repository.ActivityRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@CacheConfig(cacheNames = "ActivityService")
public class ActivityService {
    @Autowired
    private ActivityRepository activityRepository;
    @Autowired
    private ModelMapper modelMapper;



    @Cacheable(value = "activityList", keyGenerator = "redisKeyGenerator")
    public List<ActivityDto> findAllByOrderByActivityNoDesc(){
        List<ActivityDto> activityList=activityRepository.findAllByOrderByActivityNoDesc()
                                       .stream()
                                       .map(this::entityToDTO)
                                       .collect(Collectors.toList());

        for(ActivityDto act: activityList){
            for(Aimgt aimgt :act.getAimgt() ){
                byte[] shrinkAimg=ImageUtil.shrink(aimgt.getAimg(),500);
                aimgt.setAimg(shrinkAimg);
            }

        }
        return activityList;

    }
    public Optional<ActivityDto> findByaName(String aName){
        return activityRepository.findByaName(aName).map(this::entityToDTO);

    }



        @Scheduled(cron = "0 0 * * * *")
    public void checkExpiredActivity() {
            Date date = new java.util.Date();
            Timestamp today = new Timestamp(date.getTime());
        List<Activity> offStatus = activityRepository.findExpiredActivity(today);

        for (Activity activity : offStatus) {
            activity.setAStatus(2);
            activityRepository.save(activity);
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    public void checkActiveActivity() {
        Date date = new java.util.Date();
        Timestamp today = new Timestamp(date.getTime());
        List<Activity> onStatus = activityRepository.findActiveActivity(today);

        for (Activity activity : onStatus) {
            activity.setAStatus(1);
            activityRepository.save(activity);
        }
    }

    private ActivityDto entityToDTO(Activity activity){

        ActivityDto activityDto = modelMapper.map(activity,ActivityDto.class);
        activityDto.setActivityNo(activity.getActivityNo());
        return activityDto;
    }

    // Add by Shawn on 4/3
    public List<Activity> getAllActAfterSell(){
        return activityRepository.getAllAfterSellDate();
    }

    public List<Activity> getAllActBeforeSell(){ return activityRepository.getAllBeforeSellDate(); }

    public List<Activity> getAllActSelling(){ return activityRepository.getAllBetweenSellDate(); }

    public Optional<Activity> getActById(Integer actNo){ return activityRepository.findById(actNo);}

    public List<Activity> getActByBlurActName(String aname){
        return activityRepository.getByBlurActName(aname);
    }


    public Optional<Activity> findByactivityNo(Integer activityNo) {
        return activityRepository.findByactivityNo(activityNo);

    }
    public List<ActivityDto> findAllByActivityNo(Integer activityNo){

        return activityRepository.findAllByActivityNo(activityNo)
                .stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());
    }
    public Activity saveActivity(Activity activity) {
        Activity activity1 = activityRepository.save(activity);
        activity1.setActivityNo(activityRepository.findLastInsert());
        return activity1;
    }


    public void updateActivity(Activity activity) {
        activityRepository.update(activity.getActivityNo(), activity.getAName(), activity.getAClassNo(), activity.getPerformer(), activity.getHostNo(), activity.getADiscrip(), activity.getANote(), activity.getATicketRemind(), activity.getAPlace(), activity.getAPlaceAdress(), activity.getASDate(), activity.getAEDate(), activity.getWetherSeat(), activity.getASeatsImg());
    }


    public void deleteActivity(Integer activityNo, Integer aStatus) {
            activityRepository.deleteActivity(activityNo,aStatus);

    }
    public Activity findActivityByNo(Integer activityNo) {
        return activityRepository.findById(activityNo).orElse(null);
    }

    //    Add by Shawn on 04/19
    public List<Integer> findActNosByWetherSeatIsTrue(){
        return activityRepository.findActNosByWetherSeatIsTrue();
    }

    public List <Activity> findByaClassNo(Integer aclassNo) {
      return activityRepository.findByaClassNo(aclassNo);
    }
}
