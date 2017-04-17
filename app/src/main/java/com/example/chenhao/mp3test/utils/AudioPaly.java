package com.example.chenhao.mp3test.utils;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by chenhao on 17-3-8.
 */

public class AudioPaly {

    private String mStoragePCM_Path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/123" + ".pcm";
    private String mStorageWAV_Path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/456" + ".wav";

    private static AudioPaly mAudioPaly = null;
    //单线程的线程池
    private ExecutorService mExecutorService;
    //播放类
    private AudioTrack mAudioTrack;
    //播放状态
    private volatile boolean mIsPlaying = false;

    //缓存
    private byte[] mBuffer;
    //缓存大小
    private static final int BUFFER_SIZE = 4096;

    private AudioPaly() {
        mExecutorService = Executors.newSingleThreadExecutor();
        mBuffer = new byte[BUFFER_SIZE];
    }

    public static AudioPaly getInstance() {
        if (mAudioPaly == null)
            mAudioPaly = new AudioPaly();
        return mAudioPaly;
    }

    public boolean isPlaying() {
        return mIsPlaying;
    }


    public void play() {
        mIsPlaying = true;
        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                File file = new File(mStoragePCM_Path);
                doPlay(file);

            }
        });

    }

    public void stop() {
        mIsPlaying = false;
    }

    /**
     * 错误处理，防止闪退
     */
    private void playFail() {
        Log.e("chenhao", "playFail");
    }

    private void closeQuietly(FileInputStream fileInputStream) {
        try {
            if (fileInputStream != null) {
                fileInputStream.close();
                fileInputStream = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void resetQuietly(AudioTrack audioTrack) {

        try {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        } catch (RuntimeException e) {
            e.printStackTrace();
            Log.e("chenhao", "resetQuietly ---->停止播放异常");
        }
    }

    /***********************************************/

    public void initPlay() {
        initDoPlay();
        mIsPlaying = true;

    }


    private void initDoPlay() {
        FileInputStream fileInputStream = null;
        try {
            //配置播放器
            //音乐类型 扬声器播放
            int streamType = AudioManager.STREAM_MUSIC;

            int sampleRate = 44100;
            int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
            int audioFromat = AudioFormat.ENCODING_PCM_16BIT;
            //流模式
            int mode = AudioTrack.MODE_STREAM;
            int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFromat);
            Log.e("chenhao", "doPlay: " + minBufferSize);
            //循环读取数据写入播放
            mAudioTrack = new AudioTrack(streamType, sampleRate, channelConfig, audioFromat,
                    Math.max(minBufferSize, BUFFER_SIZE), mode);

            mAudioTrack.play();

        } catch (RuntimeException e) {
            e.printStackTrace();
            //错误处理，防止闪退
            playFail();

        }
    }

    public void playByte(byte[] mBuffer, int read) {
        int ret = mAudioTrack.write(mBuffer, 0, read);
        Log.e("chenhao", "doPlay: ret ==  " + ret);
        switch (ret) {
            case AudioTrack.ERROR_INVALID_OPERATION:
                break;
            case AudioTrack.ERROR_BAD_VALUE:
                break;
            case AudioManager.ERROR_DEAD_OBJECT:
                //错误处理，防止闪退
                playFail();
                break;
            default:
                break;
        }


    }

    private void stopByte() {
        mIsPlaying = false;
        resetQuietly(mAudioTrack);
    }

    /************************************************/
    private void doPlay(File file) {
        FileInputStream fileInputStream = null;
        try {
            //配置播放器
            //音乐类型 扬声器播放
            int streamType = AudioManager.STREAM_MUSIC;

            int sampleRate = 44100;
            int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
            int audioFromat = AudioFormat.ENCODING_PCM_16BIT;
            //流模式
            int mode = AudioTrack.MODE_STREAM;
            int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFromat);
            Log.e("chenhao", "doPlay: " + minBufferSize);
            //循环读取数据写入播放
            mAudioTrack = new AudioTrack(streamType, sampleRate, channelConfig, audioFromat,
                    Math.max(minBufferSize, BUFFER_SIZE), mode);

            fileInputStream = new FileInputStream(file);
            mAudioTrack.play();
            int read = 0;
            while (0 < (read = fileInputStream.read(mBuffer)) && mIsPlaying) {
                int ret = mAudioTrack.write(mBuffer, 0, read);
                Log.e("chenhao", "doPlay: ret ==  " + ret);
                switch (ret) {
                    case AudioTrack.ERROR_INVALID_OPERATION:
                        break;
                    case AudioTrack.ERROR_BAD_VALUE:
                        break;
                    case AudioManager.ERROR_DEAD_OBJECT:
                        //错误处理，防止闪退
                        playFail();
                        break;
                    default:
                        break;
                }


            }
        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
            //错误处理，防止闪退
            playFail();

        } finally {
            mIsPlaying = false;
            closeQuietly(fileInputStream);
        }
        resetQuietly(mAudioTrack);
    }

    public void close() {
        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;
        }

        mExecutorService.shutdownNow();
        mExecutorService = null;

        if (mAudioPaly != null)
            mAudioPaly = null;
    }

}
