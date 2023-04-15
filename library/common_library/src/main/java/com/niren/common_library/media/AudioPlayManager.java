package com.niren.common_library.media;

import static android.os.Process.THREAD_PRIORITY_URGENT_AUDIO;
import static android.os.Process.setThreadPriority;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.util.Log;


import com.niren.common_library.data.Pools;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 音频播放
 */
public class AudioPlayManager {
    private static final String TAG = AudioPlayManager.class.getSimpleName();
    // 用于播放音乐的音频流
    private static final int mStreamType = AudioManager.STREAM_MUSIC;
    //声道数
    private static final int mChannelConfig = AudioFormat.CHANNEL_OUT_MONO;
    //采样精度，一个采样点16比特，相当于2个字节
    private static final int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    //流播放模式 需要按照一定的时间间隔，不断的写入音频数据
    private static final int mMode = AudioTrack.MODE_STREAM;
    private AudioTrack mAudioTrack;
    private int mMinBufferSize;
    private volatile static AudioPlayManager mInstance;
    //采样率 默认16000
    private int samplePerSecond = 16000;
    private DataInputStream mDis = null;
    //是否path路径
    private boolean isPath = false;
    //是否pcm数据
    private boolean isPcm = false;

    // 任务队列
    private final Pools.SynchronizedPool<Object> dataPool;

    // 单一线程池,永远会维护存在一条线程
    private ExecutorService singleThreadPool;
    private int sessionId;

    public AudioPlayManager() {
        dataPool = new Pools.SynchronizedPool<>(10);
        singleThreadPool = Executors.newSingleThreadExecutor();
    }

    public static AudioPlayManager getInstance() {
        if (mInstance == null) {
            synchronized (AudioPlayManager.class) {
                if (mInstance == null) {
                    mInstance = new AudioPlayManager();
                }
            }
        }
        return mInstance;
    }

    /**
     * 初始化
     */
    private void initAudio() {
        mMinBufferSize = AudioTrack.getMinBufferSize(samplePerSecond, mChannelConfig, mAudioFormat);
        mAudioTrack = new AudioTrack(mStreamType, samplePerSecond, mChannelConfig,
                mAudioFormat, mMinBufferSize, mMode);
        sessionId = mAudioTrack.getAudioSessionId();
    }

    public int getSessionId() {
        return sessionId;
    }

    /**
     * 开始播放
     */
    public void startPlay() {
        if (singleThreadPool.isShutdown()) {
            singleThreadPool = Executors.newSingleThreadExecutor();
        }

        singleThreadPool.submit(recordRunnable);
    }

    /**
     * 设置PCM数据
     *
     * @param pcmS16Buff      pcm数据
     * @param numOfChan       音频通道
     * @param samplePerSecond 采样率
     */
    public void setData(byte[] pcmS16Buff, int numOfChan, int samplePerSecond) {
        this.samplePerSecond = samplePerSecond;
        isPcm = true;
        dataPool.release(pcmS16Buff);
    }

    /**
     * 设置音频路径
     */
    public void setPath(String path) {
        Log.d(TAG, "path:" + path);
        isPath = true;
        samplePerSecond = 16000;
        File file = new File(path);
        try {
            DataInputStream dis = new DataInputStream(new FileInputStream(file));
            dataPool.release(dis);
        } catch (Exception e) {
            Log.i(TAG, "setPathException:" + e.toString());
        }
    }

    /**
     * 开启线程 读写音频数据
     */
    Runnable recordRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "recordRunnable");
            try {
                setThreadPriority(THREAD_PRIORITY_URGENT_AUDIO);
                if (mAudioTrack == null || mAudioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
                    initAudio();
                }

                byte[] data = null;
                if (isPcm) {
                    data = (byte[]) dataPool.acquire();
                    isPcm = false;
                }

                if (isPath) {
                    mDis = (DataInputStream) dataPool.acquire();
                    data = new byte[mMinBufferSize];
                    isPath = false;
                }
                mAudioTrack.play();
                while (true) {
                    int i = 0;
                    if (mDis != null) {
                        while (mDis.available() > 0) {
                            assert data != null;
                            if (!(i < data.length)) break;
                            data[i] = mDis.readByte();
                            i++;
                        }
                    } else {
                        if (data == null || !(i < data.length)) {
                            break;
                        }
                        while (i < data.length) {
                            i++;
                        }
                    }

                    assert data != null;
                    mAudioTrack.write(data, 0, data.length);
                    if (i != mMinBufferSize) //表示读取完了
                    {
                        stopPlay();
                        startPlay();
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };


    /**
     * 暂停播放
     */
    public void pausePlay() {
        if (mAudioTrack != null) {
            Log.d(TAG, "pausePlay mAudioTrack.getState():" + mAudioTrack.getState());
            if (mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                mAudioTrack.pause();
                if (isPath) {
                    mAudioTrack.flush();
                }
            }
            Log.d(TAG, "pausePlay: isPlaying false");
        }
    }


    /**
     * 继续播放
     */
    public void resumePlay() {
        if (mAudioTrack != null) {
            Log.d(TAG, "resumePlay mAudioTrack.getState():" + mAudioTrack.getState());
            if (mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                mAudioTrack.play();
            }
            Log.d(TAG, "resumePlay: isPlaying false");
        }
    }

    /**
     * 停止播放  释放资源
     */
    public void stopPlay() {
        try {
            if (mAudioTrack == null) {
                return;
            }
            if (mAudioTrack.getState() == AudioTrack.PLAYSTATE_STOPPED) {
                mAudioTrack = null;
            } else {
                if (mAudioTrack.getState() == AudioRecord.STATE_INITIALIZED) {
                    mAudioTrack.stop();
                }
                if (mAudioTrack != null) {
                    mAudioTrack.release();
                }
                if (mDis != null) {
                    mDis.close();
                    mDis = null;
                }

                dataPool.clear();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
