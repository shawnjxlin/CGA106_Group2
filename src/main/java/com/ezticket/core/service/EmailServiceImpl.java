package com.ezticket.core.service;

import com.ezticket.core.pojo.EmailDetails;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
// Implementing EmailService interface
public class EmailServiceImpl implements EmailService {

    @Autowired(required=true)
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String sender;

    // Method 1
    // To send a simple email
    public String sendSimpleMail(EmailDetails details) {

        // Try block to check for exceptions
        try {

            // Creating a simple mail message
            SimpleMailMessage mailMessage = new SimpleMailMessage();

            // Setting up necessary details
            mailMessage.setFrom(sender);
            mailMessage.setTo(details.getRecipient());
            mailMessage.setText(details.getMsgBody());
            mailMessage.setSubject(details.getSubject());

            // Sending the mail
            javaMailSender.send(mailMessage);
            return "Mail Sent Successfully...";
        }

        // Catch block to handle the exceptions
        catch (Exception e) {
            return "Error while Sending Mail";
        }
    }

    // Method 2
    // To send an email with attachment
    public String
    sendMailWithAttachment(EmailDetails details) {
        // Creating a mime message
        MimeMessage mimeMessage
                = javaMailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper;

        try {

            // Setting multipart as true for attachments to
            // be send
            mimeMessageHelper
                    = new MimeMessageHelper(mimeMessage, true);
            mimeMessageHelper.setFrom(sender);
            mimeMessageHelper.setTo(details.getRecipient());
            mimeMessageHelper.setText(details.getMsgBody());
            mimeMessageHelper.setSubject(
                    details.getSubject());

            // Adding the attachment
            FileSystemResource file
                    = new FileSystemResource(
                    new File(details.getAttachment()));

            mimeMessageHelper.addAttachment(
                    file.getFilename(), file);

            // Sending the mail
            javaMailSender.send(mimeMessage);
            return "Mail sent Successfully";
        }

        // Catch block to handle MessagingException
        catch (MessagingException e) {

            // Display message when exception occurred
            return "Error while sending mail!!!";
        }
    }

    @Override
    public String sendOrderMail(String name, String to, String porderno, String status) {
        SimpleMailMessage sendCancelOrder = new SimpleMailMessage();
        // 寄件人
        sendCancelOrder.setFrom(sender);
        // 收件人
        sendCancelOrder.setTo(to);
        // 標題
        // status 1 成立 2 付款 3 取消
        String mailText = "";
        String mailTitle = "";
        if (status.equals("1")) {
            mailTitle = "訂單成立";
            mailText = "您的訂單已成功成立，請盡快前往綠界金流完成付款手續。\n" +
                    "\n" +
                    "如需購買其他商品，歡迎再次光臨我們的網站。如有任何疑問，請隨時聯繫我們的客服人員。\n";
        }
        if (status.equals("2")) {
            mailTitle = "訂單付款";
            mailText = "您的訂單已完成付款，感謝您的購買。\n" +
                    "\n" +
                    "我們將盡快處理您的訂單，如有任何問題，請隨時聯繫我們的客服人員。\n";
        }
        if (status.equals("3")) {
            mailTitle = "訂單取消";
            mailText = "很抱歉地通知您，由於逾期未付款，您的訂單已被取消。\n" +
                    "\n" +
                    "如有需要，請重新下單。如有任何疑問，請隨時聯繫我們的客服人員。\n";
        }
        sendCancelOrder.setSubject("ezTicket： " + porderno + " " + mailTitle + "通知");
        sendCancelOrder.setText(
                "親愛的" + name + "先生/小姐：\n" +
                        "\n" +
                        "此為系統通知信件：\n" +
                        "\n" +
                        mailText +
                        "\n" +
                        "再次感謝您對我們網站的支持。祝您購物愉快！\n" +
                        "\n" +
                        "請勿直接回覆此信件。\n" +
                        "\n" +
                        "敬祝" + name + "身體健康、事事如意！\n" +
                        "\n" +
                        "                                  ezTicket - 一站式購票體驗");
        javaMailSender.send(sendCancelOrder);
        return "sending mail!!!";
    }

    public String sendFollowProductSale(String name, String to, String pname, String specialPrice,String productno) throws MessagingException {
        MimeMessage msg = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "utf-8");

        helper.setFrom(sender);
        helper.setTo(to);
        helper.setSubject("ezTicket： " + pname + "降價通知");

        String htmlContent = "<p>親愛的" + name + "先生/小姐：</p>" +
                "<p>此為系統通知信件：</p>" +
                "<p>您追蹤的商品" + pname + "已降價，目前優惠價格為" + specialPrice + "</p>" +
                "<p><a href='http://localhost:8085/front-product-product_detail.html?productno=10001'>立即前往商品頁面查看詳情</a></p>" +
                "<p>再次感謝您對我們網站的支持。祝您購物愉快！</p>" +
                "<p>請勿直接回覆此信件。</p>" +
                "<p>敬祝" + name + "身體健康、事事如意！</p>" +
                "<p>                                           ezTicket - 一站式購票體驗</p>";

        helper.setText(htmlContent, true);
        javaMailSender.send(msg);
//        SimpleMailMessage sendFollowProductSale = new SimpleMailMessage();
//        //設定寄件人
//        sendFollowProductSale.setFrom(sender);
//        //設定收件人
//        sendFollowProductSale.setTo(to);
//
//        //標題
//        sendFollowProductSale.setSubject("ezTicket： " + pname + "降價通知");
//        sendFollowProductSale.setText("親愛的" + name + "先生/小姐：\n" +
//                "\n" +
//                "此為系統通知信件：\n" +
//                "\n" +
//                "您追蹤的商品" + pname + "已降價，目前優惠價格為" + specialPrice +
//                "\n" +
//                "<a href='http://localhost:8085/front-product-product_detail.html?productno=" + productno + "'>立即前往商品頁面查看詳情</a>"+
//                "\n" +
//                "再次感謝您對我們網站的支持。祝您購物愉快！\n" +
//                "\n" +
//                "請勿直接回覆此信件。\n" +
//                "\n" +
//                "敬祝" + name + "身體健康、事事如意！\n" +
//                "\n" +
//                "                                           ezTicket - 一站式購票體驗");
//        javaMailSender.send(sendFollowProductSale);
        return "sending FollowProductSaleMail success!!!";
    }




    //驗證碼信件
    @Override
    public void sendVerificationCode(String email, String code) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(sender);
        helper.setTo(email);
        helper.setSubject("Welcome to ezTicket,請盡快領取您的驗證碼!");
        helper.setText("您的驗證碼是: " + code + "<br> 請於五分鐘內返回頁面驗證!",true);

        javaMailSender.send(message);
    }
}