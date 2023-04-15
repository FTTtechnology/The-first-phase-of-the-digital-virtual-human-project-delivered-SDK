package com.finddreams.languagelib;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import android.util.DisplayMetrics;
import android.util.Log;


import org.greenrobot.eventbus.EventBus;

import java.util.Locale;

/**
 * 多语言切换的帮助类
 *
 */
public class MultiLanguageUtil {

    private static final String TAG = "MultiLanguageUtil";
    private static MultiLanguageUtil instance;
    private static final String SAVE_LANGUAGE = "save_language";

    private static Locale mCurrentSystemLocal = Locale.SIMPLIFIED_CHINESE;

    public static MultiLanguageUtil getInstance() {
        if (instance == null) {
            synchronized (MultiLanguageUtil.class) {
                if (instance == null) {
                    instance = new MultiLanguageUtil();
                }
            }
        }
        return instance;
    }

    private MultiLanguageUtil() {
    }

    /**
     * 如果不是英文、简体中文、繁体中文，默认返回简体中文
     *
     * @return
     */
    public Locale getLanguageLocale(Context context) {
        int languageType = CommSharedUtil.getInstance(context).getInt(MultiLanguageUtil.SAVE_LANGUAGE, 2);
        Locale locale = Locale.SIMPLIFIED_CHINESE;
        if (languageType == LanguageType.LANGUAGE_FOLLOW_SYSTEM) {
            //Locale sysLocale = mSystemCurrentLocal;
            locale = mCurrentSystemLocal;
        } else if (languageType == LanguageType.LANGUAGE_EN) {
            locale = Locale.ENGLISH;
        }else if (languageType == LanguageType.LANGUAGE_SPANISH) {
            locale = new Locale("es","");
        } else if (languageType == LanguageType.LANGUAGE_FRENCH) {
            locale = Locale.FRENCH;
        } else if (languageType == LanguageType.LANGUAGE_GERMAN) {
            locale = Locale.GERMAN;
        } else if (languageType == LanguageType.LANGUAGE_ITALIAN) {
            locale = Locale.ITALIAN;
        } else if (languageType == LanguageType.LANGUAGE_HINDI) {
            locale = new Locale("in","");
        }
        Log.e(TAG, "getLanguageLocale  " + getLanguage(locale) + " languageType:"+languageType);
        return locale;
    }

    private String getLanguage(Locale locale) {
        return locale.getLanguage() + "_" + locale.getCountry();
    }

    /**
     * 不要获取系统的Locale直接来判断系统语言，不同的rom获取的Locale会有差别,可以通过Locale的language字段来判断语言
     * 比如华为手机的中文语言下Locale为zh_CN_#Hans
     * @return
     */
    public Locale getSysLocale() {
        return mCurrentSystemLocal;
    }

    public void saveSystemCurrentLanguage(Context context) {
        Locale locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = LocaleList.getDefault().get(0);
        } else {
            locale = Locale.getDefault();
        }
        mCurrentSystemLocal = locale;

    }

    /**
     * 更新语言
     * @param context
     * @param languageType
     */
    public void updateLanguage(Context context, int languageType) {
        CommSharedUtil.getInstance(context).putInt(MultiLanguageUtil.SAVE_LANGUAGE, languageType);
        setConfiguration(context);
        EventBus.getDefault().post(new OnChangeLanguageEvent(languageType));
    }

    public String getLanguageName(int languageType){
        if (languageType == LanguageType.LANGUAGE_CHINESE_SIMPLIFIED) {
            return "中文";
        } else if (languageType == LanguageType.LANGUAGE_EN) {
            return "英文";
        } else if (languageType == LanguageType.LANGUAGE_SPANISH) {
            return "西班牙语";
        }else if (languageType == LanguageType.LANGUAGE_FRENCH) {
            return "法语";
        }else if (languageType == LanguageType.LANGUAGE_GERMAN) {
            return "德语";
        }else if (languageType == LanguageType.LANGUAGE_ITALIAN) {
            return "意大利语";
        } else if (languageType == LanguageType.LANGUAGE_HINDI) {
            return "印地语";
        }
        return "中文";
    }


    /**
     * 获取到用户保存的语言类型
     *
     * @return
     */
    public int getLanguageType(Context context) {
        int languageType = CommSharedUtil.getInstance(context).getInt(MultiLanguageUtil.SAVE_LANGUAGE, LanguageType.LANGUAGE_CHINESE_SIMPLIFIED);
        if (languageType == LanguageType.LANGUAGE_CHINESE_SIMPLIFIED) {
            return LanguageType.LANGUAGE_CHINESE_SIMPLIFIED;
        } else if (languageType == LanguageType.LANGUAGE_FOLLOW_SYSTEM) {
            return LanguageType.LANGUAGE_FOLLOW_SYSTEM;
        } else if (languageType == LanguageType.LANGUAGE_EN) {
            return LanguageType.LANGUAGE_EN;
        } else if (languageType == LanguageType.LANGUAGE_SPANISH) {
            return LanguageType.LANGUAGE_SPANISH;
        }else if (languageType == LanguageType.LANGUAGE_FRENCH) {
            return LanguageType.LANGUAGE_FRENCH;
        }else if (languageType == LanguageType.LANGUAGE_GERMAN) {
            return LanguageType.LANGUAGE_GERMAN;
        }else if (languageType == LanguageType.LANGUAGE_HINDI) {
            return LanguageType.LANGUAGE_HINDI;
        }
        Log.e(TAG, "getLanguageType  " + languageType);
        return languageType;
    }

    public static Context attachBaseContext(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return updateResources(context);
        } else {
            MultiLanguageUtil.getInstance().setConfiguration(context);
            return context;
        }
    }

    /**
     * 设置语言
     */
    public void setConfiguration(Context context) {
        if (context == null) {
            Log.e(TAG, "No context, MultiLanguageUtil will not work!");
            return;
        }
        Context appContext = context.getApplicationContext();
        Log.e(TAG, "setConfiguration " + context);
        Locale targetLocale = getLanguageLocale(appContext);
        Locale.setDefault(targetLocale);
        Configuration configuration = appContext.getResources().getConfiguration();
        configuration.setLocale(targetLocale);
        context.createConfigurationContext(configuration);
        Resources resources = appContext.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        resources.updateConfiguration(configuration, dm);//语言更换生效的代码!
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static Context updateResources(Context context) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        Locale locale = getInstance().getLanguageLocale(context);
        Log.d(TAG, "getLanguage ${getLanguage(locale)}");
        LocaleList localeList = new LocaleList(locale);
        LocaleList.setDefault(localeList);
        configuration.setLocales(localeList);
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }

    /**
     * 设置语言类型
     */
    public void setApplicationLanguage(Context context) {
        Resources resources = context.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        Configuration config = resources.getConfiguration();
        Locale locale = getLanguageLocale(context);
        config.locale = locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList localeList = new LocaleList(locale);
            LocaleList.setDefault(localeList);
            config.setLocales(localeList);
            context.createConfigurationContext(config);
            Locale.setDefault(locale);
        }
        resources.updateConfiguration(config, dm);
    }
}
