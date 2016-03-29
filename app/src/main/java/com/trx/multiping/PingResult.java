package com.trx.multiping;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class PingResult {

    InetAddress remoteIP;
    long echoTime;
    boolean reachable;

    public InetAddress getRemoteIP() {
        return remoteIP;
    }

    public void setRemoteIP(InetAddress remoteIP) {
        this.remoteIP = remoteIP;
    }

    public void setRemoteIP (String remoteIP) {
        boolean valid = PingFragment.validIP(remoteIP);
        if (valid) {
            try {
                InetAddress ip =  InetAddress.getByName(remoteIP);
                setRemoteIP(ip);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
    }

    public long getEchoTime() {
        return echoTime;
    }

    public void setEchoTime(long echoTime) {
        this.echoTime = echoTime;
    }

    public boolean isReachable() {
        return reachable;
    }

    public void setReachable(boolean result) {
        this.reachable = result;
    }
}
