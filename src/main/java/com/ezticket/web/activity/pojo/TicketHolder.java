package com.ezticket.web.activity.pojo;

import java.sql.Blob;
import java.sql.Timestamp;

public class TicketHolder {
    private Integer collectno;
    private Integer memberno;
    private String mname;
    private String memail;
    private Integer tdetailsno;
    private Integer tstatus;
    private Integer activityno;
    private String aname;
    private Integer wetherseat;
    private byte[] aimg;
    private Timestamp sessionstime;
    private Timestamp sessionetime;

    private String blockname;

    private String realx;

    private String realy;
    private String anote;
    private String aticketremind;
    private String aplace;
    private String aplaceAddress;

    public Integer getCollectno() {
        return collectno;
    }

    public void setCollectno(Integer collectno) {
        this.collectno = collectno;
    }

    public Integer getMemberno() {
        return memberno;
    }

    public void setMemberno(Integer memberno) {
        this.memberno = memberno;
    }

    public String getMname() {
        return mname;
    }

    public void setMname(String mname) {
        this.mname = mname;
    }

    public String getMemail() {
        return memail;
    }

    public void setMemail(String memail) {
        this.memail = memail;
    }

    public Integer getTdetailsno() {
        return tdetailsno;
    }

    public void setTdetailsno(Integer tdetailsno) {
        this.tdetailsno = tdetailsno;
    }

    public Integer getTstatus() {
        return tstatus;
    }

    public void setTstatus(Integer tstatus) {
        this.tstatus = tstatus;
    }

    public Integer getActivityno() {
        return activityno;
    }

    public void setActivityno(Integer activityno) {
        this.activityno = activityno;
    }

    public String getAname() {
        return aname;
    }

    public void setAname(String aname) {
        this.aname = aname;
    }

    public Integer getWetherseat() {
        return wetherseat;
    }

    public byte[] getAimg() {
        return aimg;
    }

    public void setAimg(byte[] aimg) {
        this.aimg = aimg;
    }

    public void setWetherseat(Integer wetherseat) {
        this.wetherseat = wetherseat;
    }



    public Timestamp getSessionstime() {
        return sessionstime;
    }

    public void setSessionstime(Timestamp sessionstime) {
        this.sessionstime = sessionstime;
    }

    public Timestamp getSessionetime() {
        return sessionetime;
    }

    public void setSessionetime(Timestamp sessionetime) {
        this.sessionetime = sessionetime;
    }

    public String getBlockname() {
        return blockname;
    }

    public void setBlockname(String blockname) {
        this.blockname = blockname;
    }

    public String getRealx() {
        return realx;
    }

    public void setRealx(String realx) {
        this.realx = realx;
    }

    public String getRealy() {
        return realy;
    }

    public void setRealy(String realy) {
        this.realy = realy;
    }

    public String getAnote() {
        return anote;
    }

    public void setAnote(String anote) {
        this.anote = anote;
    }

    public String getAticketremind() {
        return aticketremind;
    }

    public void setAticketremind(String aticketremind) {
        this.aticketremind = aticketremind;
    }

    public String getAplace() {
        return aplace;
    }

    public void setAplace(String aplace) {
        this.aplace = aplace;
    }

    public String getAplaceAddress() {
        return aplaceAddress;
    }

    public void setAplaceAddress(String aplaceAddress) {
        this.aplaceAddress = aplaceAddress;
    }
}
