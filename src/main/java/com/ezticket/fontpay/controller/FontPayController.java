package com.ezticket.fontpay.controller;

import com.ezticket.fontpay.service.FonPayService;
import com.ezticket.web.activity.repository.*;
import com.ezticket.web.activity.service.CollectCrudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/fonpay")
public class FontPayController {
    @Autowired
    FonPayService fonPayService;

    @Autowired
    TorderRepository torderRepository;

    @Autowired
    CollectCrudService collectCrudService;

    @Autowired
    SeatsRepository seatsRepository;

    @Autowired
    SessionRepository sessionRepository;

    @Autowired
    TdetailsRepository tdetailsRepository;

//    @PostMapping("/return")
//    public String fonpayReturn(HttpServletRequest req) throws IOException {
//
//        System.out.println("有進到 fonpayReturn");
//
//        Enumeration<String> parameterNames = req.getParameterNames();
//        while (parameterNames.hasMoreElements()) {
//            String paramName = parameterNames.nextElement();
//            String paramValue = req.getParameter(paramName);
//            System.out.println(paramName + ": " + paramValue);
//        }
//
//        // 電商系統若在建立訂單時設定 callbackUrl，
//        // Fonapy 在確認訂單授權完成或訂單請款完成後，
//        // 會以 HTTP POST 方法傳送 Payment Callback 參數予電商的 callbackUrl。
//        // 電商收到 Payment Callback 參數後需回傳 SUCCESS 作為確認訊息，以確認收到通知。
//        // 若未回傳，Fonpay 系統會視為電商未收到此次通知。
//        return "SUCCESS";
//    }

    @GetMapping("/paymentCancel")
    public void paymentCancel(@RequestParam("id") String id) throws IOException {
        String status = fonPayService.paymentCancelOrder(Integer.valueOf(id));
        System.out.println(status);
    }

    @GetMapping("/paymentQuery")
    public void paymentQuery(@RequestParam("id") String id) throws IOException {
        String response = fonPayService.paymentQueryOrder(Integer.valueOf(id));
        System.out.println(response);
    }



}
