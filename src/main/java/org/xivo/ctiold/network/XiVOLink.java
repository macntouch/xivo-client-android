package org.xivo.ctiold.network;

public interface XiVOLink {

    void sendGetPhoneConfig(Integer lineId);

    void sendGetUserStatus(Integer userId);

    void sendGetPhoneStatus(Integer lineId);

}
