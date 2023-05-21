package com.ezticket.fontpay.service;

import com.ezticket.web.activity.pojo.*;
import com.ezticket.web.activity.repository.*;
import com.ezticket.web.activity.service.*;
import com.google.gson.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.*;
import java.sql.Timestamp;
import java.text.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class FonPayService {

    @Autowired
    private TorderRepository torderRepository;

    @Autowired
    private TdetailsRepository tdetailsRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private SeatsRepository seatsRepository;

    @Autowired
    private CollectCrudService collectCrudService;

    static String FONPAY_API_KEY = "588418532082";
    static String FONPAY_API_SECRET = "9Zc4DSfuzk5F4yrpGFfI";
    static String FONPAY_API_MERCHANT_CODE = "ME18315275";
    static String PAYMENT_CREATE_ORDER = "PaymentCreateOrder";

    public String paymentCreateOrder(Integer torderNo) throws IOException {
        // 取得訂單
        Torder torder = torderRepository.getReferenceById(torderNo);
        //===========================================================================

        // 訂單成立 10 分鐘後未付款則失效
        LocalDateTime time = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String paymentEndTime = time.plusMinutes(10).format(formatter);

        //===========================================================================
        // 取得使用者 ip
        InetAddress ip = null;
        try {
            // 使用可能會拋出異常的方法
            ip = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            // 處理異常
            System.err.println(e);
        }

        String hostname = ip.getHostAddress();
        String local = "http://" + hostname + ":8085";

//        // 供Fonpay於訂單狀態異動時由背景呼叫此網址通知店商
//        String callbackUrl = local + "/fonpay/return";

        String redirectUrl = local + "/front-activity-orderconfirm.html?id=" + torder.getTorderNo();
        //===========================================================================

        List<Tdetails> tdetailsList = tdetailsRepository.findByTorderno(torderNo);

        Tdetails tdetails = tdetailsList.get(0);

        Session session = sessionRepository.getById(tdetails.getSessionNo());

        // 將票券資訊濃縮為一個字串
        String itemName = "";
        int itemQuantity = 0;

        if (tdetails.getSeatNo() != null) {
            Seats seats = seatsRepository.getById(tdetails.getSeatNo());
            itemName = session.getActivity().getAName() + " - " + session.getSessionsTime() + " - " + seats.getBlockName() + " 區";
            itemQuantity = tdetailsList.size();
        } else {
            itemName = session.getActivity().getAName() + " - " + session.getSessionsTime() + " - 站席";
            itemQuantity = tdetails.getTqty();
        }
        //===========================================================================

        URL url = new URL("https://test-api.fonpay.tw/api/payment/" + PAYMENT_CREATE_ORDER);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("key", FONPAY_API_KEY);
        con.setRequestProperty("secret", FONPAY_API_SECRET);
        con.setRequestProperty("merchantCode", FONPAY_API_MERCHANT_CODE);
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("User-Agent", "Tibame_Student");
        con.setRequestProperty("X-ignore", "true");
        con.setDoOutput(true);
        String jsonInputString = "{ " +
                "'request':{" +
                "'paymentNo': 'ezTicket0000000" + torderNo + "'," +
                "'legacyId':'TS1234567'," +
                "'totalPrice':" + torder.getTcheckTotal() + "," +
                "'paymentDueDate':'" + paymentEndTime + "'," +
                "'itemName':'" + itemName + "'," +
                "'memberName':'" + torder.getMemberNo() + "'," +
//                "'callbackUrl':'" + callbackUrl + "'," +
                "'redirectUrl':'" + redirectUrl + "'," +
                "'includeItemList':[" +
                "{" +
                "'itemName':'" + itemName + "'," +
                "'itemQuantity':" + itemQuantity + "" +
                "}," +
                "]" +
                "}," +
                "'basic':{" +
                "'appVersion':'1.0'," +
                "'os':'IOS'," +
                "'appName':'SYSTEM-API-ezTicket'," +
                "'latitude':24.777678," +
                "'clientIp':'61.216.102.83'," +
                "'lang':'zh_TW'," +
                "'deviceId':'123456789'," +
                "'longitude':121.043175" +
                "}}";

        Integer errorCode = 0;
        String paymentTransactionId = "";
        String paymentUrl = "";
        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }

            System.out.println(response.toString());
            String responseString = response.toString();

            Gson gson = new Gson();
            JsonElement jsonElement = gson.fromJson(responseString, JsonElement.class);
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            errorCode = jsonObject.get("response").getAsJsonObject().get("errorCode").getAsInt();

            if (errorCode != 0) {
                paymentTransactionId = "error";
                torder.setPaymentTransactionId(paymentTransactionId);
                handleFailure(torder);
            } else {
                paymentTransactionId = jsonObject.get("result").getAsJsonObject().get("payment").getAsJsonObject().get("paymentTransactionId").getAsString();
                torder.setPaymentTransactionId(paymentTransactionId);
            }
            torderRepository.save(torder);
            paymentUrl = jsonObject.get("result").getAsJsonObject().get("payment").getAsJsonObject().get("paymentUrl").getAsString();

            return paymentUrl;
        }
    }

    public String paymentQueryOrder(Integer id) throws IOException {
        Torder torder = torderRepository.getById(id);

        URL url = new URL("https://test-api.fonpay.tw/api/payment/paymentQueryOrder");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("key", FONPAY_API_KEY);
        con.setRequestProperty("secret", FONPAY_API_SECRET);
        con.setRequestProperty("merchantCode", FONPAY_API_MERCHANT_CODE);
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("User-Agent", "Tibame_Student");
        con.setRequestProperty("X-ignore", "true");
        con.setDoOutput(true);
        String jsonInputString = "{ " +
                "'request':{" +
                "'paymentTransactionId': '" + torder.getPaymentTransactionId() + "'" +
                "}," +
                "'basic':{" +
                "'appVersion':'1.0'," +
                "'os':'IOS'," +
                "'appName':'SYSTEM-API-ezTicket'," +
                "'latitude':24.777678," +
                "'clientIp':'61.216.102.83'," +
                "'lang':'zh_TW'," +
                "'deviceId':'123456789'," +
                "'longitude':121.043175" +
                "}}";


        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            String responseString = response.toString();
            return responseString;
        }
    }


    @Scheduled(fixedRate = 30000)
    public void paymentQueryOrderRegularly() throws IOException {

        List<Torder> TOrders = torderRepository.findAll();
        List<Torder> fonyPayTOrders = TOrders.stream()
                .filter(torder -> torder.getPaymentTransactionId() != null)
                .filter(torder -> !torder.getPaymentTransactionId().equals("error"))
                .filter(torder -> torder.getTpaymentStatus() == 0)
                .toList();

        for (Torder torder : fonyPayTOrders) {
            URL url = new URL("https://test-api.fonpay.tw/api/payment/paymentQueryOrder");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("key", FONPAY_API_KEY);
            con.setRequestProperty("secret", FONPAY_API_SECRET);
            con.setRequestProperty("merchantCode", FONPAY_API_MERCHANT_CODE);
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("User-Agent", "Tibame_Student");
            con.setRequestProperty("X-ignore", "true");
            con.setDoOutput(true);
            String jsonInputString = "{ " +
                    "'request':{" +
                    "'paymentTransactionId': '" + torder.getPaymentTransactionId() + "'" +
                    "}," +
                    "'basic':{" +
                    "'appVersion':'1.0'," +
                    "'os':'IOS'," +
                    "'appName':'SYSTEM-API-ezTicket'," +
                    "'latitude':24.777678," +
                    "'clientIp':'61.216.102.83'," +
                    "'lang':'zh_TW'," +
                    "'deviceId':'123456789'," +
                    "'longitude':121.043175" +
                    "}}";


            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                String responseString = response.toString();
                Gson gson = new Gson();
                JsonElement jsonElement = gson.fromJson(responseString, JsonElement.class);
                JsonObject jsonObject = jsonElement.getAsJsonObject();

                if (torder.getPaymentTransactionId().isEmpty()) {
                    handleFailure(torder);
                    torderRepository.save(torder);
                    break;
                }

                String status = jsonObject.get("result").getAsJsonObject().get("payment").getAsJsonObject().get("status").getAsString();
                String paidDate = jsonObject.get("result").getAsJsonObject().get("payment").getAsJsonObject().get("paidDate").getAsString();
                String paidDueDate = jsonObject.get("result").getAsJsonObject().get("payment").getAsJsonObject().get("paymentDueDate").getAsString();

                // 訂單成立 10 分鐘後未付款則失效
                LocalDateTime time = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                String now = time.format(formatter);

                if (status.equals("SUCCESS")) {
                    handleSuccess(torder, paidDate);
                }

                if (Integer.valueOf(now.substring(6)) - Integer.valueOf(paidDueDate.substring(6)) >= 1000) {
                    handleFailure(torder);
                }

                torderRepository.save(torder);
            }
        }
    }

    public String paymentCancelOrder(Integer id) throws IOException {
        Torder torder = torderRepository.getById(id);

        URL url = new URL("https://test-api.fonpay.tw/api/payment/paymentCancelOrder");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("key", FONPAY_API_KEY);
        con.setRequestProperty("secret", FONPAY_API_SECRET);
        con.setRequestProperty("merchantCode", FONPAY_API_MERCHANT_CODE);
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("User-Agent", "Tibame_Student");
        con.setRequestProperty("X-ignore", "true");
        con.setDoOutput(true);
        String jsonInputString = "{ " +
                "'request':{" +
                "'paymentTransactionId': '" + torder.getPaymentTransactionId() + "'" +
                "}," +
                "'basic':{" +
                "'appVersion':'1.0'," +
                "'os':'IOS'," +
                "'appName':'SYSTEM-API-ezTicket'," +
                "'latitude':24.777678," +
                "'clientIp':'61.216.102.83'," +
                "'lang':'zh_TW'," +
                "'deviceId':'123456789'," +
                "'longitude':121.043175" +
                "}}";


        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            String responseString = response.toString();
            System.out.println(responseString);

            Gson gson = new Gson();
            JsonElement jsonElement = gson.fromJson(responseString, JsonElement.class);
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            String status = jsonObject.get("result").getAsJsonObject().get("payment").getAsJsonObject().get("status").getAsString();

            if(status.equals("CANCEL")){
                handleCancel(torder);
                torderRepository.save(torder);
            }

            return status;
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

    public void handleCancel(Torder torder) {
        torder.setTpaymentStatus(2);
        torder.setTprocessStatus(2);

        List<Tdetails> toBeModiDetails = tdetailsRepository.findByTorderno(torder.getTorderNo());
        for (Tdetails tdetail : toBeModiDetails) {
            if (tdetail.getSeatNo() != null) {
                // 若訂單失敗且節目屬於有座位的，則釋放座位，將座位改成可售出
                seatsRepository.updateStatus(1, tdetail.getSeatNo());
            } else {
                // 若訂單失敗且節目屬於有無座位的，則已售出票券數量減少
                sessionRepository.updateStandingQtyById(-tdetail.getTqty(), tdetail.getSessionNo());
            }
        }
    }

    public void handleFailure(Torder torder) {
        torder.setTpaymentStatus(3);
        torder.setTprocessStatus(3);

        List<Tdetails> toBeModiDetails = tdetailsRepository.findByTorderno(torder.getTorderNo());
        for (Tdetails tdetail : toBeModiDetails) {
            if (tdetail.getSeatNo() != null) {
                // 若訂單失敗且節目屬於有座位的，則釋放座位，將座位改成可售出
                seatsRepository.updateStatus(1, tdetail.getSeatNo());
            } else {
                // 若訂單失敗且節目屬於有無座位的，則已售出票券數量減少
                sessionRepository.updateStandingQtyById(-tdetail.getTqty(), tdetail.getSessionNo());
            }
        }
    }



}

