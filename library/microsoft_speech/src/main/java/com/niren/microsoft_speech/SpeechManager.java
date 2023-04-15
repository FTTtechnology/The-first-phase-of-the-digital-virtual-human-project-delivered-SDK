package com.niren.microsoft_speech;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.microsoft.cognitiveservices.speech.CancellationDetails;
import com.microsoft.cognitiveservices.speech.KeywordRecognitionModel;
import com.microsoft.cognitiveservices.speech.KeywordRecognitionResult;
import com.microsoft.cognitiveservices.speech.KeywordRecognizer;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SpeechManager implements Handler.Callback {
    private static final String TAG = SpeechManager.class.getSimpleName();
    // Replace below with your own subscription key
    private static final String SPEECH_SUBSCRIPTION_KEY = "22ae49d3b8b24a22ab6d174ff8382b1e";
    // Replace below with your own service region (e.g., "westus").
    private static final String SERVICE_REGION = "eastus";
    private static final int MSG_SPEECH_ERROR = 100;

    private SpeechListener mSpeechListener;
    private KeywordRecognizer keywordRecognizer;
    private SpeechRecognizer speechRecognizer;

    private HandlerThread mGLThread;
    private GLHandler mGLHandler;

    public static final String[] LANGUAGE_TYPE = {"en-US","zh-CN","es-ES","fr-FR","de-DE","it-IT","hi-IN"};

    private String language = "en-US";

    public void addSpeechListener(SpeechListener speechListener) {
        this.mSpeechListener = speechListener;
    }


    private KeywordListener mKeywordListener;

    public void addKeywordListener(KeywordListener keywordListener) {
        this.mKeywordListener = keywordListener;
    }

    private SpeechManager() {
    }

    private static final ExecutorService s_executorService;

    static {
        s_executorService = Executors.newCachedThreadPool();
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (msg.what == MSG_SPEECH_ERROR) {
            if(mSpeechListener != null && !isStop){
                mSpeechListener.onError();
                stopASR();
            }
        }
        return false;
    }

    private static final class MInstanceHolder {
        static final SpeechManager mInstance = new SpeechManager();
    }

    public static SpeechManager getInstance() {
        return MInstanceHolder.mInstance;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    private boolean isStop;


    /**
     * 微软语音识别
     */
    public void startSpeechAsr() {
        mGLThread = new HandlerThread(TAG);
        mGLThread.start();
        mGLHandler = new GLHandler(mGLThread.getLooper(), this);

        Message message = Message.obtain(mGLHandler, MSG_SPEECH_ERROR);
//        mGLHandler.sendMessageDelayed(message,10*1000);

        isStop = false;
        try {
            Log.d(TAG,"startSpeechAsr language:"+language);
            SpeechConfig speechConfig = SpeechConfig.fromSubscription(SPEECH_SUBSCRIPTION_KEY, SERVICE_REGION);
//            speechConfig.setEndpointId("fef0c36bde474dbd8e0060af7043bd2f");
            speechConfig.setSpeechRecognitionLanguage(language);
            final AudioConfig audioInput = AudioConfig.fromStreamInput(createMicrophoneStream());
            speechRecognizer = new SpeechRecognizer(speechConfig, audioInput);
            Future<SpeechRecognitionResult> task = speechRecognizer.recognizeOnceAsync();

            setOnTaskCompletedListener(task, result -> {
//                mGLHandler.removeMessages(MSG_SPEECH_ERROR);
                String s = result.getText();
                if(result.getReason() == ResultReason.RecognizedSpeech){
                    Log.i(TAG, "Recognizer returned: " + s);
                    if(mSpeechListener != null && !isStop){
                        mSpeechListener.onSuccess(s);
                    }
                }else{
                    String errorDetails = (result.getReason() == ResultReason.Canceled) ? CancellationDetails.fromResult(result).getErrorDetails() : "";
                    s = "Recognition failed with " + result.getReason() + ". Did you enter your subscription?" + System.lineSeparator() + errorDetails;
                    Log.d(TAG,"error:"+s);
                    if(mSpeechListener != null && !isStop){
                        mSpeechListener.onError();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopASR(){
        if(speechRecognizer != null){
            isStop = true;
        }
    }

    public void startWake(Context context) {
        ExecutorService es = Executors.newFixedThreadPool(2);
        Runnable asyncTask = () -> {
            try {
                AudioConfig config = AudioConfig.fromDefaultMicrophoneInput();
                keywordRecognizer = new KeywordRecognizer(config);
                InputStream is = context.getAssets().open("kws.table");
                KeywordRecognitionModel model = KeywordRecognitionModel.fromStream(is, "Hi Angus", false);
                Future<KeywordRecognitionResult> task = keywordRecognizer.recognizeOnceAsync(model);
                // Note: this will block the UI thread, so eventually, you want to
                //       register for the event (see full samples)
                KeywordRecognitionResult result = task.get();

                ((Activity)context).runOnUiThread(() -> {
                    if (result.getReason() == ResultReason.RecognizedKeyword) {
                        Log.d(TAG,"text:"+result.getText());
                        if(mKeywordListener != null){
                            mKeywordListener.onSuccess(result.getText());
                        }
                    }
                    else {
                        if(mKeywordListener != null){
                            mKeywordListener.onError();
                        }
                    }
                });

            } catch (Exception ex) {
                Log.e(TAG, "unexpected " + ex.getMessage());
//                assert(false);
            }
        };
        es.execute(asyncTask);
    }


    public void stopWake(){
        if(keywordRecognizer != null){
            keywordRecognizer.stopRecognitionAsync();
        }
    }

    private MicrophoneStream microphoneStream;

    private MicrophoneStream createMicrophoneStream() {
        this.releaseMicrophoneStream();

        microphoneStream = new MicrophoneStream();
        return microphoneStream;
    }

    private void releaseMicrophoneStream() {
        if (microphoneStream != null) {
            microphoneStream.close();
            microphoneStream = null;
        }
    }

    private <T> void setOnTaskCompletedListener(Future<T> task, OnTaskCompletedListener<T> listener) {
        s_executorService.submit(() -> {
            T result = task.get();
            listener.onCompleted(result);
            return null;
        });
    }

    private interface OnTaskCompletedListener<T> {
        void onCompleted(T taskResult);
    }

    public static class GLHandler extends Handler {
        public GLHandler(Looper looper, Callback callback) {
            super(looper, callback);
        }
    }
}
