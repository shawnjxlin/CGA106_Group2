package com.ezticket.web.activity.repository;
import com.ezticket.web.activity.pojo.Activity;
import com.ezticket.web.activity.pojo.Torder;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TorderRepository extends JpaRepository<Torder, Integer> {
//    @Query("SELECT t FROM Torder t WHERE t.torderNo LIKE %:torderNo%")
//    List<Torder> findByTorderNoContaining(@Param("torderNo") Integer torderNo);

//    Add by Shawn on 04/18
    @Query(value = "SELECT * FROM torder o WHERE o.tpaymentstatus = 0 AND o.torderdate < DATE_SUB(NOW(), INTERVAL 10 MINUTE)", nativeQuery = true)
    List<Torder> findAllByUnpaidAndBeforeTenMins();

    Torder findByPaymentTransactionId(String paymentTransactionId);

}
