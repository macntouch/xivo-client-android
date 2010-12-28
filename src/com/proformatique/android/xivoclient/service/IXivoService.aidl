// AIDL file for the XiVO service on Android

package com.proformatique.android.xivoclient.service;

interface IXivoService {
	
	// Returns true if the contact list has changed since the last call
	boolean contactsChanged();
	
	
}
