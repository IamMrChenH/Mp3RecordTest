package com.example.chenhao.mp3test.utils;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Message;
import android.util.Log;

import com.example.chenhao.mp3test.DataEncodeThread;
import com.example.chenhao.mp3test.SimpleLame;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class AudioUtils {

    static {
        System.loadLibrary("lamemp3");
    }


    public interface OnFinishListener {
        void onFinish(String mp3SavePath);
    }

    private OnFinishListener finishListener;

    public void setFinishListener(OnFinishListener finishListener) {
        this.finishListener = finishListener;
    }

    /**
     * TAG
     */
    private String TAG = AudioUtils.class.getSimpleName();

    /**
     * 转换线程
     */
    private DataEncodeThread mDataEncodeThread;

    /**
     * 录音的最小文件缓存
     */
    private int minBufferSize = 0;

    /**
     * 录音的工具类 就是自己，目的是 单例的录音类
     */
    private static AudioUtils mAudioUtils = null;

    /**
     * 单线程的线程池
     */
    private ExecutorService mExecutorService;
    /**
     * 录音类
     */
    private AudioRecord mAudioRecord;
    /**
     * 录音状态
     */
    private volatile boolean mIsPlayRecording = false;
    /**
     * 缓存
     */


    /**
     * 缓存大小
     */
    private static final int BUFFER_SIZE = 4096;

    /**
     * 私有的 目的是 单例的类
     * mExecutorService 线程控制类
     * mBuffer  缓存
     */
    private AudioUtils() {
        mExecutorService = Executors.newSingleThreadExecutor();
    }

    /**
     * 获得 单例的录音类
     *
     * @return mAudioUtils  工具类 对象
     */
    public static AudioUtils getInstance() {
        if (mAudioUtils == null) {
            mAudioUtils = new AudioUtils();
        }
        return mAudioUtils;
    }

    /**
     * 获取录音状态
     *
     * @return
     */
    public boolean isPlayRecording() {
        return mIsPlayRecording;
    }


    /**
     * 启动录音
     */

    public void start(final File file) {
        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                //启动录音
                mp3File = file;
                mIsPlayRecording = true;
                startRecord();
            }
        });

    }


    /**
     * 停止录音
     */
    public void stop() {
        mIsPlayRecording = false;
    }

    /**
     * 录音
     *
     * @return
     */
    private boolean startRecord() {
        //创建文件输出流
        try {
            initAudioRecord();

            //开始录音
            mAudioRecord.startRecording();

            //循环读取数据，写到输出流中
            while (mIsPlayRecording) {
                int read = mAudioRecord.read(mPCMBuffer, 0, BUFFER_SIZE);
                if (read > 0) {
                    mDataEncodeThread.addChangeBuffer(mPCMBuffer, read);
                    Log.e("chenhao", "读取成功：" + read);

                } else {
                    Log.e("chenhao", "读取失败：" + read);
                    return false;
                }
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e("chenhao", "文件流创建失败");
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("chenhao", "写入文件流失败");
            return false;
        }
        return stopRecord();
    }

    /**
     * 结束录音逻辑
     *
     * @return
     */
    private boolean stopRecord() {

        try {
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;

            // 录音完毕，通知转换线程停止，并等待直到其转换完毕
            Message msg = Message.obtain(mDataEncodeThread.getHandler(), DataEncodeThread.PROCESS_STOP);
            msg.sendToTarget();
            mDataEncodeThread.join();
            if (finishListener != null) finishListener.onFinish(mp3File.getAbsolutePath());

        } catch (Exception e) {
            return false;
        }
        return true;
    }


    private void close() {
        mExecutorService.shutdownNow();
        mExecutorService = null;
        mAudioUtils = null;

    }

    private short[] mPCMBuffer;
    private File mp3File;
    private FileOutputStream os;
    //转换周期，录音每满160帧，进行一次转换
    private static final int FRAME_COUNT = 160;

    public void initAudioRecord() throws IOException {
        //配置AudioRecord
        int audioSource = MediaRecorder.AudioSource.MIC;
        int sampleRate = 44100;
        //单声道出入
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        // 16 比特
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        //计算AudioRecord 内部 BufferSize 的最小值
        minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig,
                audioFormat);
        Log.e("chenhao", "max：" + minBufferSize);
        int bufferSize = Math.max(minBufferSize, BUFFER_SIZE);
        mAudioRecord = new AudioRecord(audioSource, sampleRate, channelConfig, audioFormat,
                bufferSize);
        mPCMBuffer = new short[bufferSize];

        SimpleLame.init(44100, 1, 44100, 32);

        os = new FileOutputStream(mp3File);
        mDataEncodeThread = new DataEncodeThread(os, bufferSize);
        mDataEncodeThread.start();
        mAudioRecord.setRecordPositionUpdateListener(mDataEncodeThread, mDataEncodeThread.getHandler());
        mAudioRecord.setPositionNotificationPeriod(FRAME_COUNT);


    }


}



