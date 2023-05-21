package com.ezticket.web.activity.pojo;

import com.ezticket.web.users.pojo.Member;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor

public class Torder {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "torderno")
	private Integer torderNo;
	@Column(name = "memberno")
	private Integer memberNo;
	@Column
    private Integer ttotal;
	@Column(name = "tdiscounttotal")
    private Integer tdiscountTotal;
	@Column(name = "tcoupontotal")
    private Integer tcouponTotal;
	@Column(name = "tchecktotal")
	private Integer tcheckTotal;
	@Column(name = "tcouponno")
	private Integer tcouponNo;
	@Column(name = "torderdate")
	@JsonFormat(pattern = "yyyy/MM/dd kk:mm:ss", timezone = "GMT+8")
	private Timestamp torderDate;
	@Column(name = "tpaydate")
	@JsonFormat(pattern = "yyyy/MM/dd kk:mm:ss", timezone = "GMT+8")
	private Timestamp tpayDate;
	@Column(name = "treceivedate")
	@JsonFormat(pattern = "yyyy/MM/dd kk:mm:ss", timezone = "GMT+8")
	private Timestamp treceiveDate;
	@Column(name = "tpaymentstatus")
	private Integer tpaymentStatus;
	@Column(name = "tprocessstatus")
	private Integer tprocessStatus;
	@Column(name = "PAYMENTTRANSACTIONID")
	private String paymentTransactionId;
	@ManyToOne
	@JoinColumn(name = "MEMBERNO" ,insertable = false, updatable = false)
	private Member member;

//	@ManyToOne
//	@JoinColumn(name = "MEMBERNO" ,insertable = false, updatable = false)
//	private Member member;

//	@OneToMany
//	@JoinColumn(name = "TDETAILSNO",
//			referencedColumnName = "torderno")
//	private List<Tdetails> tdetails;
//@ManyToMany
//@JoinTable(
//		joinColumns = @JoinColumn(
//				referencedColumnName = "torderno",
//				name = "TORDERNO"),
//		name = "Tdetails",
//		inverseJoinColumns = @JoinColumn(
//				name = "SESSIONNO",
//				referencedColumnName = "SESSIONNO")
//)
//private List<Session> session;


}
