package media;

import android.media.audiofx.Visualizer;
import android.util.Log;

import java.util.Arrays;

/**
 * Date: 2020/11/24
 * Author: Yang
 * Describe: hold the visualizer and callback something
 */
public class VisualizerHelper {
    private static final String TAG = VisualizerHelper.class.getSimpleName();
    private Visualizer mVisualizer;
    private VisualizeCallback mCallback;
    private float NPT = 1024;
    /*
    由于FFT计算出来的数据是对称的，因此通常而言输出数组取一半的数据，为lBufOutArray[NPT/2]。
    */
    private final int SPECTRUM_WND_SIZE = 32; //窗口数
    private final int THRESHOLD_WINDOW_SIZE = 16;   //均值窗口数
    private final float MULTIPLIER = 1.0f;   //增益系数
    private int[] spectralFlux = new int[SPECTRUM_WND_SIZE]; //前后差值
    private int[] threshold = new int[SPECTRUM_WND_SIZE];   //均值阈值
    private int[] peakSpectrum = new int[SPECTRUM_WND_SIZE]; //节拍值
    private final int[] lastSpectrum = new int[(int) (NPT / 2)];//上一次幅值   差值处理使用
    int wndNum = 0;

    public void setVisualizeCallback(VisualizeCallback callback) {
        mCallback = callback;
    }

    /**
     * Sets the audio session id for the currently playing audio
     *
     * @param audioSessionId of the media to be visualised
     */
    public void setAudioSessionId(int audioSessionId) {
        if (mVisualizer != null) release();
        mVisualizer = new Visualizer(audioSessionId);
        mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
            }

            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
//                NPT = fft.length;
                float[] model = new float[fft.length / 2 + 1];
                model[0] = (byte) Math.abs(fft[1]);
                int j = 1;
                for (int i = 2; i < fft.length / 2; ) {
                    model[j] = (float) Math.hypot(fft[i], fft[i + 1]);
                    i += 2;
                    j++;
                    model[j] = (float) Math.abs(fft[j]);
                }
                FFT_Calculate(model);
                if (mCallback != null) {
                    mCallback.onFftDataCapture(model);
                }
            }
        }, Visualizer.getMaxCaptureRate() / 2, false, true);
        mVisualizer.setEnabled(true);
    }

    private void FFT_Calculate(float[] model) {
        int i;
        int flux = 0;
        int diffValue = 0;
        int m = 0, start, end, j;
        float mean;
        //pc_show_freq_info();
        /* 差分算法	当前窗口数据减去上一个窗口数据，得到差值数据*/
        for (i = 0; i < NPT / 2; i++) {
            diffValue = (int) (model[m] - lastSpectrum[m]);
            flux += Math.max(diffValue, 0);
        }
        spectralFlux[wndNum] = flux;
        // 一个窗口所花时间=1000/(44100/1024) = 23ms;1s包含窗口数 = 43
        if (wndNum == SPECTRUM_WND_SIZE - 1) {
            /* 均值算法	这里取前16个窗口+后16个窗口计算均值*/
            for (m = 0; m < SPECTRUM_WND_SIZE; m++) {
                //取 0 和m-THRESHOLD_WINDOW_SIZE中的较大值
                start = Math.max(0, (m - THRESHOLD_WINDOW_SIZE));
                //取 SPECTRUM_WND_SIZE-1 和m+THRESHOLD_WINDOW_SIZE中的较小值
                end = Math.min((SPECTRUM_WND_SIZE - 1), (m + THRESHOLD_WINDOW_SIZE));
                mean = 0;
                for (j = start; j <= end; j++) {
                    mean += spectralFlux[j];
                }
                mean /= (end - start);
                threshold[m] = (int) (mean * MULTIPLIER); //
            }
            /*节拍判断*/
            for (i = 0; i < SPECTRUM_WND_SIZE; i++) {
                if (threshold[i] <= spectralFlux[i]) {
                    peakSpectrum[i] = spectralFlux[i] - threshold[i];
                    peakSpectrum[i] = peakSpectrum[i] / 10 + peakSpectrum[i] % 10;
                    if (peakSpectrum[i] >= 255) {
                        peakSpectrum[i] = 255;
                    }
                } else {
                    peakSpectrum[i] = 0;
                }
            }
        }
        wndNum++;
        if (wndNum == SPECTRUM_WND_SIZE) {
            wndNum = 0;
        }
        Log.d(TAG, "peakSpectrum:" + Arrays.toString(peakSpectrum));
    }

    /**
     * Releases the visualizer
     */
    public void release() {
        if (mVisualizer != null) mVisualizer.release();
    }
}
