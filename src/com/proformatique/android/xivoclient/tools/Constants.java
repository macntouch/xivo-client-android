/* XiVO Client Android
 * Copyright (C) 2010-2011, Proformatique
 *
 * This file is part of XiVO Client Android.
 *
 * XiVO Client Android is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XiVO Client Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XiVO client Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.proformatique.android.xivoclient.tools;

public final class Constants {
    
    private Constants() { } // Prevent instantiation
    
    /**
     * Return codes
     */
    public static final int NO_NETWORK_AVAILABLE = -2;
    public static final int BAD_HOST = -1;
    public static final int NOT_CTI_SERVER = -3;
    public static final int JSON_POPULATE_ERROR = -4;
    public static final int VERSION_MISMATCH = -5;
    public static final int CTI_SERVER_NOT_SUPPORTED = -6;
    public static final int LOGIN_PASSWORD_ERROR = -7;
    public static final int FORCED_DISCONNECT = -8;
    public static final int NO_LOGIN_PASSWORD = -9;
    public static final int CONNECTION_FAILED = -10;
    public static final int ALGORITH_NOT_AVAILABLE = -11;
    public static final int REMOTE_EXCEPTION = -12;
    public static final int CONNECTION_TIMEDOUT = -13;
    public static final int ALREADY_CONNECTING = -14;
    public static final int ALREADY_AUTHENTICATING = -15;
    public static final int CONNECTION_REFUSED = -16;
    public static final int LOGIN_KO = 0;
    public static final int OK = 1;
    public static final int CONNECTION_OK = 2;
    public static final int CANCEL = 3;
    public static final int AUTHENTICATION_OK = 4;
    
    /**
     * ActivityForResult requestCodes and codeResults
     */
    public static final int CODE_IDENTITY_STATE_LIST = 1;
    public static final int CODE_ADD_APPLICATION = 2;
    public static final int CODE_LAUNCH = 100;
    public static final int CODE_EXIT = 666; // Kill kill !!
    public static final int CODE_SERVICE_ASK1 = 101;
    public static final int CODE_SERVICE_ASK2 = 102;
    public static final int CODE_SERVICE_ASK3 = 103;
    
    /**
     * Application constants
     */
    public static final String PACK = "com.proformatique.android.xivoclient";
    public static final String XIVO_SERVER = "xivoserver";
    public static final String XIVO_ASTID = "xivo";
    public static final String XIVO_CONTEXT = "default";
    public static final String XIVO_LOGIN_VERSION = "9999";
    public static final String XIVO_VERSION = "1.1";
    public static final String XIVO_LOGIN_OK = "login_id_ok";
    public static final String XIVO_PASSWORD_OK = "login_pass_ok";
    public static final String XIVO_LOGIN_KO = "loginko";
    public static final String XIVO_LOGIN_CAPAS_OK = "login_capas_ok";
    public static final String XIVO_VERSION_NOT_COMPATIBLE = "xivoversion_client:1.1;1.0";
    public static final String XIVO_CTI_VERSION_NOT_SUPPORTED = "wrong_client_os_identifier:android-";
    public static final String XIVO_LOGIN_PASSWORD = "login_password";
    public static final String XIVO_LOGIN_UNKNOWN_USER = "user_not_found";
    public static final int XIVO_DEFAULT_PORT = 5003;
    public static final int XIVO_NOTIF = 375942;
    public static final int ANDROID_CONTACT_HASH_SIZE = 6;
    public static final int CONTACT_PICKER_RESULT = 1001;
    public static final int TRANSFER_MENU = 1;
    public static final int ATXFER_ITEM_INDEX = 1;
    public static final int TRANSFER_ITEM_INDEX = 2;
    public static final String DEFAULT_HINT_COLOR = "gray";
    public static final String DEFAULT_HINT_CODE = "away";
    public static final int MAX_PHONE_NUMBER_LEN = 7;
    public static final int HINTSTATUS_AVAILABLE_CODE = 0;
    public static final String AVAILABLE_STATUS_CODE = "0";
    public static final String CALLING_STATUS_CODE = "1";
    
    /**
     * Intent actions
     */
    public static final String ACTION_LOAD_USER_LIST = "xivo.intent.action.LOAD_USER_LIST";
    public static final String ACTION_LOAD_XLETS = "xivo.intent.action.LOAD_XLETS";
    public static final String ACTION_DISCONNECT = "xivo.intent.action.ACTION_DISCONNECT";
    public static final String ACTION_LOAD_PHONE_STATUS = "xivo.intent.action.LOAD_PHONE_STATUS";
    public static final String ACTION_LOAD_HISTORY_LIST = "xivo.intent.action.LOAD_HISTORY_LIST";
    public static final String ACTION_LOAD_FEATURES = "xivo.intent.action.LOAD_FEATURES";
    public static final String ACTION_REFRESH_USER_LIST = "xivo.intent.action.USER_SEARCH";
    public static final String ACTION_FORCED_DISCONNECT = "xivo.intent.action.FORCED_DISCONNECT";
    public static final String ACTION_DISCONNECT_REQUEST = "xivo.intent.action.DISCONNECT_REQUEST";
    public static final String ACTION_OFFHOOK = "xivo.intent.action.OFFHOOK";
    public static final String ACTION_MWI_UPDATE = "xivo.intent.action.MWI_UPDATE";
    public static final String ACTION_MY_STATUS_CHANGE = "xivo.intent.action.MY_STATUS_CHANGE";
    public static final String ACTION_MY_PHONE_CHANGE = "xivo.intent.action.MY_PHONE_CHANGE";
    public static final String ACTION_CALL_PROGRESS = "xivo.intent.action.CALL_PROGRESS";
    public static final String ACTION_UPDATE_IDENTITY = "xivo.intent.action.UPDATE_IDENTITY";
    public static final String ACTION_ONGOING_CALL = "xivo.intent.action.ONGOING_CALL";
    public static final String ACTION_SETTINGS_CHANGE = "xivo.intent.action.SETTINGS_CHANGED";
    
    /**
     * Preferences
     */
    public static final String AUTO_START_SERVICE = "xivo.preference.auto_start_service";
    public static final String PREF_USER_NAME = "login";
    public static final String PREF_PASSWORD = "password";
}
