package com.ezticket.web.activity.controller;

import com.ezticket.ecpay.service.OrderService;
import com.ezticket.fontpay.service.FonPayService;
import com.ezticket.web.activity.dto.AddTorderDTO;
import com.ezticket.web.activity.dto.TorderDto;
import com.ezticket.web.activity.pojo.Torder;
import com.ezticket.web.activity.service.CollectCrudService;
import com.ezticket.web.activity.service.TorderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

@CrossOrigin("*")
@RestController
@RequestMapping("/Torder")
public class TorderController {
    @Autowired
    private TorderService torderService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private FonPayService fonPayService;
    @Autowired
    private CollectCrudService collectCrudService;

    @GetMapping("/findAll")
    public List<TorderDto> findAll() {

        return torderService.findAll();
    }

    @GetMapping("/findById")
    public Optional<Torder> findById(@RequestParam("torderNo") Integer torderNo) {
        return torderService.findById(torderNo);
    }

    // Add by Shawn on 04/17
    @PostMapping("/addTorder")
    @ResponseBody
    public String addTOrder(@RequestBody AddTorderDTO addTorderDTO) throws IOException {
//        torderService.addTOrder(addTorderDTO);
//        return "";
//        return orderService.ecpayTCheckout(torderService.addTOrder(addTorderDTO).getTorderNo());

//        String fonPayResponse = fonPayService.paymentCreateOrder(torderService.addTOrder(addTorderDTO).getTorderNo());

//        try {
//            Gson gson = new Gson();
//            JsonElement jsonElement = gson.fromJson(fonPayResponse, JsonElement.class);
//            JsonObject jsonObject = jsonElement.getAsJsonObject();
//
//            String paymentUrl = jsonObject.get("result").getAsJsonObject().get("payment").getAsJsonObject().get("paymentUrl").getAsString();
//            return paymentUrl;
//        } catch (Exception e) {
//            return ("FontPay 建立訂單異常: " + e.getMessage());
//        }

        return fonPayService.paymentCreateOrder(torderService.addTOrder(addTorderDTO).getTorderNo());
    }

    // Add by Shawn on 04/19
    @GetMapping("/getById")
    public Torder getById(@RequestParam Integer torderNo) {
        return torderService.getById(torderNo);
    }

    @PostMapping("/deleteTorder")
    public void deleteTorder(@RequestParam Integer torderNo) {
        torderService.deleteTorder(torderNo);
    }

    @GetMapping("/established")
    public void paymentReturn(@RequestParam("id") String id,
                              @RequestParam("status") String status,
                              @RequestParam("paidDate") String paidDate) {
        Torder torder = torderService.getById(Integer.valueOf(id));
        if(status.equals("SUCCESS")){
            handleSuccess(torder, paidDate);
            torderService.updateTorder(torder);
        }
    }


    public void handleSuccess(Torder torder, String paidDate) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

        try {
            java.util.Date date = dateFormat.parse(paidDate);
            Timestamp timestamp = new Timestamp(date.getTime());
            torder.setTpayDate(timestamp);
            torder.setTpaymentStatus(1);
            torder.setTprocessStatus(1);

            // 票券 QR Code 產生應於此處 - 2 (Melody)
            collectCrudService.insertCollect(torder);

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
