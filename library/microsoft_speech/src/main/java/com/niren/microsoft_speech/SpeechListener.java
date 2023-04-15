package com.niren.microsoft_speech;

public interface SpeechListener {
    void onError();

    void onSuccess(String text);

}