package com.ezticket.web.product.service;

import com.ezticket.core.service.EmailService;
import com.ezticket.web.product.pojo.Followproduct;
import com.ezticket.web.product.pojo.Product;
import com.ezticket.web.product.repository.ProductDAO;
import com.ezticket.web.product.util.PageResult;
import com.ezticket.web.users.pojo.Member;
import com.ezticket.web.users.repository.MemberRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Service
@CacheConfig(cacheNames = "ProductService")
public class ProductService {
    @Autowired
    private ProductDAO dao;
    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FollowproductService followproductSvc;

    @Autowired
    private EmailService emailSvc;

    @Transactional
    public Product addProduct(Integer pclassno, String pname, Integer hostno, String pdiscrip, Integer pprice,
                              Integer pspecialprice, Integer pqty, Timestamp psdate, Timestamp pedate, String ptag, Integer pstatus) {
        Product productVO = new Product();
        productVO.setPratetotal(0);
        productVO.setPrateqty(0);
        productVO.setPclassno(pclassno);
        productVO.setPname(pname);
        productVO.setHostno(hostno);
        productVO.setPdiscrip(pdiscrip);
        productVO.setPprice(pprice);
        productVO.setPspecialprice(pspecialprice);
        productVO.setPqty(pqty);
        productVO.setPsdate(psdate);
        productVO.setPedate(pedate);
        productVO.setPtag(ptag);
        productVO.setPstatus(pstatus);

        dao.insert(productVO);
        return productVO;
    }

    @Transactional
    public Product updateProduct(Integer productno, Integer pclassno, String pname, Integer hostno, String pdiscrip,
                                 Integer pprice, Integer pspecialprice, Integer pqty, Timestamp psdate, Timestamp pedate, String ptag,
                                 Integer pstatus, Integer pratetotal, Integer prateqty) {

        Product product = dao.getByPrimaryKey(productno);
        if (product.getPspecialprice() > pspecialprice) {
            Set<Integer> memberSet = followproductSvc.getFollowerByProduct(productno);
            for (Integer memberno : memberSet) {
                Member member = memberRepository.getReferenceById(memberno);
                try {
                    emailSvc.sendFollowProductSale(member.getMname(), member.getMemail(), pname, String.valueOf(pspecialprice), String.valueOf(productno));
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
        product.setPclassno(pclassno);
        product.setPname(pname);
        product.setHostno(hostno);
        product.setPdiscrip(pdiscrip);
        product.setPprice(pprice);
        product.setPspecialprice(pspecialprice);
        product.setPqty(pqty);
        product.setPsdate(psdate);
        product.setPedate(pedate);
        product.setPtag(ptag);
        product.setPstatus(pstatus);
        product.setPratetotal(pratetotal);
        product.setPrateqty(prateqty);
        dao.update(product);

        return product;
    }

    public Product updateProduct(Integer productno, Integer prate) {

        Product product = dao.getByPrimaryKey(productno);
        Integer pratetotal = product.getPratetotal();
        Integer prateqty = product.getPrateqty();
        product.setPratetotal(pratetotal + prate);
        product.setPrateqty(prateqty + 1);
        dao.update(product);
        return product;
    }


    public Product getOneProduct(Integer productno) {
        return dao.getByPrimaryKey(productno);
    }
    public List<Product> getAllProduct() {
        return dao.getAll();
    }
    public List<Product> getAllByName(String pname) {
        return dao.findByProductName(pname);
    }
//    @Cacheable(value = "ProductSearch", keyGenerator = "redisKeyGenerator")
    public List<Product> getAllByproductSearch(Map<String, String[]> map) {
        return dao.getAll(map);
    }

    //複合查詢搭配分頁
    public PageResult<Product> getAllByproductSearch(Map<String, String[]> map, Integer pageNumber, Integer pageSize) {
        return dao.getAll(map, pageNumber, pageSize);
    }

    //將過了下架時間的商品狀態更動為下架
    // 這是一個 Spring 框架的定時任務設定，表示每小時的整點觸發一次，其中各個欄位的意義如下：
    // 第一個 * 代表秒數，表示不限定秒數。
    // 第二個 0 代表分鐘數，表示每小時的 0 分鐘觸發。
    // 第三個 * 代表小時數，表示每小時都要觸發。
    // 第四個 * 代表天數，表示不限定天數。
    // 第五個 * 代表月份，表示不限定月份。
    // 第六個 * 代表星期幾，表示不限定星期幾。
    @Scheduled(cron = "0 0 * * * *")
    public void checkPstatus() {
        Date date = new java.util.Date();
        Timestamp today = new Timestamp(date.getTime());
        List<Product> productList = dao.findExpiredProduct(today);

        for (Product product : productList) {
            product.setPstatus(1);
            dao.update(product);
        }
    }
}
