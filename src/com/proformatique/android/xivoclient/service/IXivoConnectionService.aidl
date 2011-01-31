package com.proformatique.android.xivoclient.service;

interface IXivoConnectionService {
    int connect();
    int disconnect();
    boolean isConnected();
    int authenticate();
    boolean isAuthenticated();
    void loadData();
}