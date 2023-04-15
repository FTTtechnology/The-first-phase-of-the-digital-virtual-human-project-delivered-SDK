package com.niren.common_library.media;

import android.media.MediaPlayer;

public interface MediaManagerListener {
    void onPrepare(MediaPlayer mediaPlayer);
    void onCompletion();
    void onCurrentPosition(int position);
}
