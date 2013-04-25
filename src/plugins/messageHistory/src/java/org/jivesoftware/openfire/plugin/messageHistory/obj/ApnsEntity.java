package org.jivesoftware.openfire.plugin.messageHistory.obj;

import org.jivesoftware.openfire.plugin.messageHistory.obj.enums.*;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: John Kuo
 * Date: 1/23/13
 * Time: 11:00 AM
 * Entity to parse the incoming APNS json obj
 */
public class ApnsEntity {

    protected Long id;
    private String appname;
    private String appversion;
    private String deviceuid;
    private String devicetoken;
    private String devicename;
    private String devicemodel;
    private String deviceversion;
    //Default is to false
    private PushBadge pushBadge = PushBadge.DISABLED;
    //Default is to false
    private PushAlert pushAlert= PushAlert.DISABLED;
    //Default is to false
    private PushSound pushSound = PushSound.DISABLED;
    //Default is to production
    private Development development = Development.PRODUCTION;
    //Default is to false
    private Status status = Status.active;
    private Date created;

    private Date modified;

    public String getAppname() {
        return appname;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public String getAppversion() {
        return appversion;
    }

    public void setAppversion(String appversion) {
        this.appversion = appversion;
    }

    public String getDeviceuid() {
        return deviceuid;
    }

    public void setDeviceuid(String deviceuid) {
        this.deviceuid = deviceuid;
    }

    public String getDevicetoken() {
        return devicetoken;
    }

    public void setDevicetoken(String devicetoken) {
        this.devicetoken = devicetoken;
    }

    public String getDevicename() {
        return devicename;
    }

    public void setDevicename(String devicename) {
        this.devicename = devicename;
    }

    public String getDevicemodel() {
        return devicemodel;
    }

    public void setDevicemodel(String devicemodel) {
        this.devicemodel = devicemodel;
    }

    public String getDeviceversion() {
        return deviceversion;
    }

    public void setDeviceversion(String deviceversion) {
        this.deviceversion = deviceversion;
    }

    public PushBadge getPushBadge() {
        return pushBadge;
    }

    public void setPushBadge(PushBadge pushBadge) {
        this.pushBadge = pushBadge;
    }

    public PushAlert getPushAlert() {
        return pushAlert;
    }

    public void setPushAlert(PushAlert pushAlert) {
        this.pushAlert = pushAlert;
    }

    public PushSound getPushSound() {
        return pushSound;
    }

    public void setPushSound(PushSound pushSound) {
        this.pushSound = pushSound;
    }

    public Development getDevelopment() {
        return development;
    }

    public void setDevelopment(Development development) {
        this.development = development;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }


}
