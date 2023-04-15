package media;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

public class MediaManager {
    private static final String TAG = MediaManager.class.getSimpleName();
    private MediaPlayer mediaPlayer;
    private Context mContext;
    private MediaManagerListener mListener;
    private boolean isPause = false;
    private progressThread progressThread;

    private MediaManager() {
    }

    public MediaManager(Context context) {
        mContext = context;
    }


    /**
     * 播放raw文件
     *
     * @param raw
     */
    public MediaPlayer doPlay(final int raw) {
        try {
            if (isPause && mediaPlayer != null) {
                if (progressThread != null) {
                    progressThread.stopThread();
                    progressThread.interrupt();
                    progressThread = null;
                }
                doStart();
                return mediaPlayer;
            }
            mediaPlayer = MediaPlayer.create(mContext, raw);
            if (mediaPlayer == null) {
                Log.d(TAG, "mediaPlayer is null");
                return null;
            }

            mediaPlayer.setOnErrorListener(null);
            //开始播放监听
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    if (mListener != null) {
                        mListener.onPrepare(mediaPlayer);
                    }
                }
            });

            //播放完毕监听
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (mListener != null) {
                        mListener.onCompletion();
                    }
                }
            });
            return mediaPlayer;
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
        return null;
    }


    /**
     * play audio by file path
     *
     * @param filePath file path of audio
     */
    public MediaPlayer doPlay(final String filePath) {
        try {

            if (isPause && mediaPlayer != null) {
                doStart();
                return mediaPlayer;
            }

            mediaPlayer = MediaPlayer.create(mContext, Uri.parse(filePath));
            if (mediaPlayer == null) {
                Log.d(TAG, "mediaPlayer is null");
                return mediaPlayer;
            }

            mediaPlayer.setOnErrorListener(null);
            //开始播放监听
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    if (mListener != null) {
                        mListener.onPrepare(mediaPlayer);
                    }
                }
            });

            //播放完毕监听
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (mListener != null) {
                        mListener.onCompletion();
                    }
                }
            });

            return mediaPlayer;
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
        return null;
    }


    class progressThread extends Thread {
        boolean flag = true;

        @Override
        public void run() {
            super.run();
            while (flag) {
                try {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        mListener.onCurrentPosition(mediaPlayer.getCurrentPosition()); //实时获取播放音乐的位置并且设置进度条的位置
                    }
                }catch (Exception e){
//                    e.printStackTrace();
                }
            }
        }

        //下面的函数是外部调用种植线程的，因为现在是不提倡直接调用stop方法的
        public void stopThread() {
            this.flag = false;
        }
    }


    public void onSeekTo(int msec){
        mediaPlayer.seekTo(msec);
    }

    public int getDuration() {
        if (mediaPlayer != null) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }


    /**
     * 开始播放
     */
    public void doStart() {
        if (mediaPlayer != null) {
            mediaPlayer.start();

            if (progressThread == null) {
                progressThread = new progressThread();
                progressThread.start();
            } else{
                if(isPause){
                    progressThread.start();
                }else{
                    progressThread.stopThread();
                    progressThread.interrupt();
                    progressThread = null;
                }
            }
            isPause = false;
        }
    }

    /**
     * 暂停播放
     */
    public void doPause() {
        if (mediaPlayer != null) {
            isPause = true;
            mediaPlayer.pause();
            if(progressThread != null){
                progressThread.stopThread();
            }
        }
    }

    /**
     * 是否循环播放
     *
     * @param isLoop
     */
    public void isLoop(boolean isLoop) {
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(isLoop);
        }
    }

    /**
     * 下一首
     *
     * @param filePath
     */
    public void onNext(final String filePath) {
        if (mediaPlayer != null) {
            release();
        }
        doPlay(filePath);
    }

    /**
     * 上一首
     *
     * @param filePath
     */
    public void onPrevious(final String filePath) {
        if (mediaPlayer != null) {
            release();
        }
        doPlay(filePath);
    }


    public int getMediaPlayerId() {
        return mediaPlayer.getAudioSessionId();
    }

    public void setMediaManagerListener(MediaManagerListener listener) {
        mListener = listener;
    }

    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;

            if (progressThread != null) {
                progressThread = null;
            }
        }
    }
}
