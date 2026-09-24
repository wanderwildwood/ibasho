package com.wanderwildwood.ibasho.data;


import static com.wanderwildwood.ibasho.net.FmdServerRepositoryKt.FMD_SERVER_PROTO_V1;

import androidx.annotation.Keep;

import java.util.HashMap;

import com.wanderwildwood.ibasho.BuildConfig;
import com.wanderwildwood.ibasho.commands.FmdPermission;
import com.wanderwildwood.ibasho.utils.RingerUtils;


@Keep
public class Settings extends HashMap<Integer, Object> {

    public static final int SETTINGS_VERSION = 5;

    public static final int SET_WIPE_ENABLED = 0;
    // public static final int SET_ACCESS_VIA_PIN = 1;
    public static final int SET_LOCKSCREEN_MESSAGE = 2;
    public static final int SET_PIN = 3;
    public static final int SET_FMD_COMMAND = 4;
    public static final int SET_OPENCELLID_API_KEY = 5;
    //public static final int SET_INTRODUCTION_VERSION = 6;
    public static final int SET_RINGER_TONE = 7;
    public static final int SET_SET_VERSION = 8;

    //public static final int SET_FMDSERVER_UPLOAD_SERVICE = 101;
    public static final int SET_FMDSERVER_URL = 102;
    public static final int SET_FMDSERVER_UPDATE_TIME = 103;
    public static final int SET_FMDSERVER_ID = 104;
    //public static final int SET_FMDSERVER_PASSWORD_SET = 105;
    public static final int SET_FMDSERVER_LOCATION_TYPE = 106; // BackgroundLocationType
    //public static final int SET_FMDSERVER_AUTO_UPLOAD = 107;
    public static final int SET_FMD_CRYPT_PUBKEY = 108;
    public static final int SET_FMD_CRYPT_PRIVKEY = 109;
    public static final int SET_FMD_CRYPT_HPW = 110;
    public static final int SET_FMD_LOW_BAT_SEND = 111;
    //public static final int SET_FMD_CRYPT_NEW_SALT = 112;
    public static final int SET_UPDATEBOARDING_MODERN_CRYPTO_COMPLETED = 113;
    public static final int SET_FMDSERVER_PUSH_URL = 114;
    public static final int SET_FMDSERVER_LAST_LOCATION_UPLOAD_TIME = 115;
    public static final int SET_FMDSERVER_LAST_CMD_MILLIS = 116;
    public static final int SET_FMD_EDGE_INFO_SHOWN = 117;
    public static final int SET_FMDSERVER_PERMISSIONS = 118;
    public static final int SET_FMD_CRYPT_PROTO = 119;
    public static final int SET_FMDSERVER_PUSH_SOURCE = 120; // PushChoice: built_in or another_app
    // When the regular upload job last started. It says whether Android is still letting the
    // app run at all, which the last upload cannot: no fix indoors also means no upload.
    public static final int SET_FMDSERVER_LAST_UPLOAD_JOB_MILLIS = 121;
    // What the setup guide was told: "" before it is answered, then VAL_SETUP_TEXTS or
    // VAL_SETUP_SERVER. Texts only keeps the server out of sight until one is added.
    public static final int SET_SETUP_MODE = 122;
    public static final String VAL_SETUP_TEXTS = "texts";
    public static final String VAL_SETUP_SERVER = "server";
    public static final int SET_SETUP_GUIDE_DONE = 123;

    public static final int SET_FMD_SERVER_CONNECTIVITY_CHECK_INTERVAL_HOURS = 201;
    public static final int SET_FMD_SERVER_CONNECTIVITY_CHECK_NOTIFY_AFTER_HOURS = 202;
    public static final int SET_FMD_SERVER_LAST_CONNECTIVITY_UNIX_TIME = 203;

    public static final int SET_FIRST_TIME_WHITELIST = 301;
    public static final int SET_FIRST_TIME_CONTACT_ADDED = 302;
    //public static final int SET_FIRST_TIME_FMD_SERVER = 303;

    public static final int SET_APP_CRASHED_LOG_ENTRY = 401; // 0=no crash, 1=crash to show
    public static final int SET_FMDSMS_COUNTER = 402;

    //public static final int SET_GPS_STATE = 501;         // 0=GPS is off 1=GPS is on 2=GPS is turned on by FMD
    public static final int SET_LAST_KNOWN_LOCATION_LAT = 502;
    public static final int SET_LAST_KNOWN_LOCATION_LON = 503;
    public static final int SET_LAST_KNOWN_LOCATION_TIME = 504;
    public static final int SET_LAST_LOW_BAT_UPLOAD = 505;
    public static final int SET_LAST_KNOWN_LOCATION_ACCURACY = 506;
    public static final int SET_LAST_KNOWN_LOCATION_ALTITUDE = 507;
    public static final int SET_LAST_KNOWN_LOCATION_BEARING = 508;
    public static final int SET_LAST_KNOWN_LOCATION_SPEED = 509;

    public static final int SET_THEME = 601;
    public static final String VAL_THEME_FOLLOW_SYSTEM = "follow_system";
    public static final String VAL_THEME_LIGHT = "light";
    public static final String VAL_THEME_DARK = "dark";
    public static final int SET_DYNAMIC_COLORS = 602;

    public static final int SET_LOCAL_PASSWORD_SALT_B64 = 702;

    public Settings() {
    }

    // Warning: for numbers, all default values must be either Long or Double!

    public Object get(int key) {
        if (super.containsKey(key)) {
            return super.get(key);
        } else {
            switch (key) {
                case SET_WIPE_ENABLED:
                    // case SET_ACCESS_VIA_PIN:
                case SET_FIRST_TIME_WHITELIST:
                case SET_FIRST_TIME_CONTACT_ADDED:
                    //case SET_FIRST_TIME_FMD_SERVER:
                    //case SET_FMDSERVER_PASSWORD_SET:
                    //case SET_FMD_CRYPT_NEW_SALT:
                case SET_UPDATEBOARDING_MODERN_CRYPTO_COMPLETED:
                case SET_FMD_EDGE_INFO_SHOWN:
                case SET_SETUP_GUIDE_DONE:
                    return false;
                case SET_FMD_LOW_BAT_SEND:
                    return true;
                case SET_FMD_COMMAND:
                    return BuildConfig.DEFAULT_FMD_COMMAND;
                case SET_FMDSERVER_UPDATE_TIME:
                    return 60L;
                //case SET_INTRODUCTION_VERSION:
                case SET_FMDSMS_COUNTER:
                case SET_SET_VERSION:
                    return 0L;
                case SET_FMDSERVER_LOCATION_TYPE:
                    return (long) BackgroundLocationType.Companion.getEMPTY();
                case SET_RINGER_TONE:
                    return RingerUtils.getDefaultRingtoneAsString();
                case SET_PIN:
                case SET_FMDSERVER_ID:
                case SET_LAST_KNOWN_LOCATION_LAT:
                case SET_LAST_KNOWN_LOCATION_LON:
                case SET_FMD_CRYPT_HPW:
                case SET_FMD_CRYPT_PRIVKEY:
                case SET_FMD_CRYPT_PUBKEY:
                case SET_FMDSERVER_URL:
                case SET_FMDSERVER_PUSH_URL:
                case SET_FMDSERVER_PUSH_SOURCE:
                case SET_SETUP_MODE:
                    return "";
                case SET_FMDSERVER_LAST_CMD_MILLIS:
                case SET_FMDSERVER_LAST_UPLOAD_JOB_MILLIS:
                    return 0L;
                case SET_FMDSERVER_PERMISSIONS:
                    // Use ALL instead of DEFAULT for FMD Server because
                    // 1) the server currently has no response channel for "access denied" error messages
                    // 2) the server is primarily for your own usage, not for giving access to
                    //    your phone to others (unlike SMS/Notification Reply).
                    return FmdPermission.Companion.getALL();
                case SET_FMD_CRYPT_PROTO:
                    return FMD_SERVER_PROTO_V1;
                //case SET_GPS_STATE:
                //    return 1;
                case SET_APP_CRASHED_LOG_ENTRY:
                    return 0L;
                case SET_LAST_KNOWN_LOCATION_TIME:
                case SET_LAST_LOW_BAT_UPLOAD:
                case SET_FMDSERVER_LAST_LOCATION_UPLOAD_TIME:
                    return -1L;
                case SET_THEME:
                    return VAL_THEME_FOLLOW_SYSTEM;
                case SET_DYNAMIC_COLORS:
                    return false;

                case SET_FMD_SERVER_CONNECTIVITY_CHECK_INTERVAL_HOURS:
                    return 0L;
                case SET_FMD_SERVER_CONNECTIVITY_CHECK_NOTIFY_AFTER_HOURS:
                    return 36L;
                case SET_FMD_SERVER_LAST_CONNECTIVITY_UNIX_TIME:
                    return 0L;

                case SET_LAST_KNOWN_LOCATION_ACCURACY,
                     SET_LAST_KNOWN_LOCATION_ALTITUDE,
                     SET_LAST_KNOWN_LOCATION_BEARING,
                     SET_LAST_KNOWN_LOCATION_SPEED:
                    return Double.NaN;
                case SET_LOCAL_PASSWORD_SALT_B64:
                    return "";
            }
        }
        return "";
    }

}
