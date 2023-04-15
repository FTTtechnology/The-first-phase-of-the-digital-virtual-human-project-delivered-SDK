package com.niren.microsoft_speech;

public interface KeywordListener {
    void onError();
    void onSuccess(String text);
}
