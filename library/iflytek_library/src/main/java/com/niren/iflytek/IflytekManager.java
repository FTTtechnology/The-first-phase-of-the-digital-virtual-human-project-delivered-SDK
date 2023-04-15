package com.niren.iflytek;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.Setting;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.VoiceWakeuper;
import com.iflytek.cloud.WakeuperListener;
import com.iflytek.cloud.WakeuperResult;
import com.iflytek.cloud.util.ResourceUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class IflytekManager {
    private static final String TAG = IflytekManager.class.getSimpleName();
    private SpeechRecognizer mIat;
    private VoiceWakeuper mIvw;
    private Context mContext;
    // 用HashMap存储听写结果
    private final HashMap<String, String> mIatResults = new LinkedHashMap<>();
    private StringBuffer buffer = new StringBuffer();

    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;
    private final String resultType = "json";
    private String language = "zh_cn";

    private int curThresh = 1450;
    private String threshStr = "门限值：";
    private String keep_alive = "1";
    private String ivwNetMode = "0";

    /**
     * 中文唤醒词
     */
    public static final String KEYWORD_ZH = "xiao3-wan4-xiao3-wan4";
    /**
     * 其他语言唤醒词
     */
    public static final String KEYWORD_EN = "hai1-an1-ge2-si1";

    private IflyRecognizerListener iflyRecognizerListener;
    public void addIflyRecognizerListener(IflyRecognizerListener iflyRecognizerListener){
        this.iflyRecognizerListener = iflyRecognizerListener;
    }

    private static final class InstanceHolder {
        static final IflytekManager instance = new IflytekManager();
    }

    public static IflytekManager getInstance() {
        return InstanceHolder.instance;
    }


    private IflytekManager(){

    }

    public void initSDK(Context context){
        this.mContext = context;
        SpeechUtility.createUtility(context, SpeechConstant.APPID +"=12cece45");
        Setting.setShowLog(false);
    }


    /**
     * 初始化监听器。
     */
    private final InitListener mInitListener = code -> {
        Log.d(TAG,"SpeechRecognizer init() code = " + code);
        if (code != ErrorCode.SUCCESS) {
//                showTip("初始化失败，错误码：" + code);
        }
    };

    public void startASR(){
        mIat = SpeechRecognizer.createRecognizer(mContext, mInitListener);
        mIatResults.clear();
        buffer.setLength(0);
        setParam();

        // 不显示听写对话框
        int ret = mIat.startListening(mRecognizerListener);
        if (ret != ErrorCode.SUCCESS) {
            Log.d(TAG,"听写失败,错误码：" + ret);
        }
    }


    /**
     * 参数设置
     *
     * @return
     */
    public void setParam() {
        Log.d(TAG,"mIat:" + mIat);
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);
        // 设置听写引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, resultType);

        if (language.equals("zh_cn")) {
            String lag = "mandarin";
            // 设置语言
            Log.d(TAG,"language = " + language);
            mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
            // 设置语言区域
            mIat.setParameter(SpeechConstant.ACCENT, lag);
        } else {
            mIat.setParameter(SpeechConstant.LANGUAGE, language);
        }
        Log.d(TAG,"last language:" + mIat.getParameter(SpeechConstant.LANGUAGE));

        //此处用于设置dialog中不显示错误码信息
        //mIat.setParameter("view_tips_plain","false");

        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS, "4000");

        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS, "1000");

        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, "0");

        // 设置音频保存路径，保存音频格式支持pcm、wav.
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, mContext.getExternalFilesDir("msc").getAbsolutePath() + "/iat.wav");
    }


    /**
     * 听写监听器。
     */
    private final RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
//            showTip("开始说话");
            buffer = new StringBuffer();
            if(iflyRecognizerListener != null){
                iflyRecognizerListener.onBeginOfSpeech();
            }
        }

        @Override
        public void onError(SpeechError error) {
            // Tips：
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
            Log.d(TAG,"onError " + error.getPlainDescription(true));
            if(iflyRecognizerListener != null){
                iflyRecognizerListener.onError();
            }
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
//            showTip("结束说话");
        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.d(TAG,results.getResultString());
            Log.d(TAG,"onResult 结束:" + buffer.toString());
            if (isLast) {
                parseResult(buffer.toString());
            }
            if (resultType.equals("json")) {//结果
                Log.d(TAG,"results:" + results.getResultString());
                buffer.append(JsonParser.parseIatResult(results.getResultString()));
                return;
            }
            if (resultType.equals("plain")) {
                buffer.append(results.getResultString());
                Log.d(TAG,"buffer:" + buffer.toString());
//                text_tip.setText(buffer.toString());
//                playdynamic(buffer.toString(), true);
            }
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
//            showTip("当前正在说话，音量大小 = " + volume + " 返回音频数据 = " + data.length);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }
    };

    /**
     * 显示结果
     */
    private void parseResult(String results) {

        String sn = null;
        // 读取json结果中的sn字段
        try {
            org.json.JSONObject resultJson = new org.json.JSONObject(results);
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
//            e.printStackTrace();
        }

        mIatResults.put(sn, results);

        StringBuilder resultBuffer = new StringBuilder();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }
        Log.d(TAG,"onResult resultBuffer:" + resultBuffer.length());
        if(iflyRecognizerListener != null){
            iflyRecognizerListener.onResult(resultBuffer);
        }
    }


    public void stopASR(){
        Log.d(TAG,"stopASR mIat:"+mIat);
        if(mIat != null){
            mIat.stopListening();
            mIat.cancel();
        }
    }

    private String resultString = "";
    public void startWake(){
        // 初始化唤醒对象
        mIvw = VoiceWakeuper.createWakeuper(mContext, null);

        //非空判断，防止因空指针使程序崩溃
        mIvw = VoiceWakeuper.getWakeuper();
        if (mIvw != null) {

            resultString = "";
            // 清空参数
            mIvw.setParameter(SpeechConstant.PARAMS, null);
            // 唤醒门限值，根据资源携带的唤醒词个数按照“id:门限;id:门限”的格式传入
            mIvw.setParameter(SpeechConstant.IVW_THRESHOLD, "0:" + curThresh);
            // 设置唤醒模式
            mIvw.setParameter(SpeechConstant.IVW_SST, "wakeup");
            // 设置持续进行唤醒
            mIvw.setParameter(SpeechConstant.KEEP_ALIVE, keep_alive);
            // 设置闭环优化网络模式
            mIvw.setParameter(SpeechConstant.IVW_NET_MODE, ivwNetMode);
            // 设置唤醒资源路径
            mIvw.setParameter(SpeechConstant.IVW_RES_PATH, getResource());
            // 设置唤醒录音保存路径，保存最近一分钟的音频
            mIvw.setParameter(SpeechConstant.IVW_AUDIO_PATH,
                    mContext.getExternalFilesDir("msc").getAbsolutePath() + "/ivw.wav");
            mIvw.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
            // 如有需要，设置 NOTIFY_RECORD_DATA 以实时通过 onEvent 返回录音音频流字节
            //mIvw.setParameter( SpeechConstant.NOTIFY_RECORD_DATA, "1" );
            // 启动唤醒
            /*	mIvw.setParameter(SpeechConstant.AUDIO_SOURCE, "-1");*/

            mIvw.startListening(mWakeupListener);
        } else {
            Log.d(TAG,"唤醒未初始化");
        }
    }


    private final WakeuperListener mWakeupListener = new WakeuperListener() {

        @Override
        public void onResult(WakeuperResult result) {
            Log.d(TAG,"onResult");
            if (!"1".equalsIgnoreCase(keep_alive)) {
//                setRadioEnable(true);
            }
            String text = "";
            try {
                text = result.getResultString();
                JSONObject object;
                object = new JSONObject(text);
                StringBuffer buffer = new StringBuffer();
                buffer.append("【RAW】 ").append(text);
                buffer.append("\n");
                buffer.append("【操作类型】").append(object.optString("sst"));
                buffer.append("\n");
                buffer.append("【唤醒词id】").append(object.optString("id"));
                buffer.append("\n");
                buffer.append("【得分】").append(object.optString("score"));
                buffer.append("\n");
                buffer.append("【前端点】").append(object.optString("bos"));
                buffer.append("\n");
                buffer.append("【尾端点】").append(object.optString("eos"));
                resultString = buffer.toString();
            } catch (JSONException e) {
                resultString = "结果解析出错";
                e.printStackTrace();
            }
            Log.d(TAG,"resultString:" + resultString);
            if(iflyRecognizerListener !=null){
                iflyRecognizerListener.onWakeUp(text);
            }
        }

        @Override
        public void onError(SpeechError error) {
            Log.d(TAG,error.getPlainDescription(true));
        }

        @Override
        public void onBeginOfSpeech() {
        }

        @Override
        public void onEvent(int eventType, int isLast, int arg2, Bundle obj) {
            switch (eventType) {
                // EVENT_RECORD_DATA 事件仅在 NOTIFY_RECORD_DATA 参数值为 真 时返回
                case SpeechEvent.EVENT_RECORD_DATA:
                    final byte[] audio = obj.getByteArray(SpeechEvent.KEY_EVENT_RECORD_DATA);
                    Log.d(TAG,"ivw audio length: " + audio.length);
                    break;
            }
        }

        @Override
        public void onVolumeChanged(int volume) {

        }
    };


    private String getResource() {
        final String resPath = ResourceUtil.generateResourcePath(mContext, ResourceUtil.RESOURCE_TYPE.assets, "ivw/" + mContext.getString(R.string.app_id) + ".jet");
        Log.d(TAG,"resPath: " + resPath);

        return resPath;
    }

    /**
     * 停止讯飞唤醒
     */
    public void stopWake(){
        if(mIvw != null){
            mIvw.stopListening();
        }
    }
}
