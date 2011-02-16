package com.proformatique.android.xivoclient.service;

interface IXivoConnectionService {
    int connect();
    int disconnect();
    boolean isConnected();
    int authenticate();
    boolean isAuthenticated();
    void loadData();
    boolean loadDataCalled();
    long getReceivedBytes();
    boolean hasNewVoiceMail();
    long getStateId();
    String getPhoneStatusColor();
    String getPhoneStatusLongname();
    int call(String number);
    boolean isOnThePhone();
    void setState(String stateId);
}