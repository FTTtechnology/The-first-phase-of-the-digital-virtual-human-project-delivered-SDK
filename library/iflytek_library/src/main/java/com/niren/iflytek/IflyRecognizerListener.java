package com.niren.iflytek;

public interface IflyRecognizerListener {
    void onBeginOfSpeech();
    void onError();
    void onResult(StringBuilder resultBuffer);

    void onWakeUp(String resultString);
}
