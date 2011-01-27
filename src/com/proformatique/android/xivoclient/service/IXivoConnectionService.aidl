package com.proformatique.android.xivoclient.service;

interface IXivoConnectionService {
    boolean isLoggedIn();
    void login();
    void logOff();
    int connect();
    void disconnect();
}