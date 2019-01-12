package com.cchao.voicesplayer.library;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * 播放者，通过 多个 MediaPlayer 分别播放音频文件。
 * 为了使播放更流畅，会利用TimerTask延时控制（比分比可控制）音频在还未结结算时就开始播放下一个音频。
 * 但特别的，短的（小于{@link Constant#ALL_PLAY_END_MIN}ms）
 * 或长的（大于{@link Constant#ALL_PLAY_END_MAX}ms)音频允许其播放完整个音频文件
 *
 * @author cchao
 * @version 11/6/18.
 */
public class VoiceSpeaker {

    private final static String TAG = "VoiceSpeaker";
    private static Context mContext;
    private static VoiceSpeaker mInstance;

    Timer mTimer = new Timer();
    // MediaPlayer 实体列表
    HashMap<String, MediaPlayer> mPlayers = new HashMap<>();

    // 当前播放标识
    private int mIndex = 0;
    // 待播放的队列（如多个语音需要播放 则会排队播放）
    Queue<List<String>> mListQueue = new LinkedBlockingDeque<>();

    // 当前播放的语音列表
    List<String> mCurPlayList = new ArrayList<>();

    // 当前播放状态
    private boolean mIsPlaying;
    private float mRatio;

    // 如果存在相同的数字 （比如：2.33 二点三三元） 则第一个三播放时需完整播放
    private String mSameNum;

    //  记录这个list开始的播放时间，如果超过 认为的出错时间 就清空队列（避免污染下一次播放）
    private long mPlayListStartTime = 0;

    private VoiceSpeaker(Context context) {
        mContext = context.getApplicationContext();
        init();
    }

    /**
     * 单例
     */
    public static VoiceSpeaker getInstance(Context context) {
        if (mInstance != null) {
            return mInstance;
        }
        mInstance = new VoiceSpeaker(context);
        return mInstance;
    }

    /**
     * 设置音频播放多少比例后 开启下一音频的播放
     *
     * @param ratio 比例
     */
    public void setPlayRatio(float ratio) {
        if (ratio > 1 || ratio < 0.5) {
            return;
        }
        mRatio = ratio;
    }

    /**
     * 设置允许播放完成的音频长度
     */
    public void setMinMaxPlayEnd(long min, long max) {
        Constant.ALL_PLAY_END_MIN = min;
        Constant.ALL_PLAY_END_MAX = max;
    }

    /**
     * 初始化过程，生成 音频文件的对应 mediaPlay 实体
     */
    public void init() {
        Executors.newCachedThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                for (String item : Constant.VOICE_NAME_ARR) {
                    try {
                        MediaPlayer player = new MediaPlayer();
                        String path = String.format("sound/%s.mp3", item);
                        AssetFileDescriptor fd = getAssetFileDescription(path);
                        player.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
                        player.prepareAsync();
                        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                if (isAllowPlayEnd(mp)) {
                                    playNext();
                                }
                            }
                        });
                        mPlayers.put(item, player);

                        fd.close();
                    } catch (Exception ex) {
                        Log.e(TAG, ex.getMessage());
                    }
                }
            }
        });
    }

    /**
     * 播放下一音频
     */
    private synchronized void playNext() {
        if (mIndex >= mCurPlayList.size()) {
            if (mListQueue.size() > 0) {
                startPlayList(mListQueue.remove());
                return;
            }
            togglePlaying(false);
            return;
        }
        String key = mCurPlayList.get(mIndex++);
        togglePlaying(true);
        MediaPlayer mediaPlayer = mPlayers.get(key);
        int duration = mediaPlayer.getDuration();
        mediaPlayer.start();
        if (!isAllowPlayEnd(mediaPlayer)) {
            //延时执行
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    playNext();
                }
            }, (long) (duration * mRatio));
        }
    }

    /**
     * 切换播放标识
     */
    private void togglePlaying(boolean b) {
        if (b == mIsPlaying) {
            return;
        }
        mIsPlaying = b;
    }

    /**
     * 是否 允许当前 mediaPlayer 播放完成才进入下一个mediaPlayer的播放
     *
     * @param mediaPlayer mediaPlayer
     * @return 是否
     */
    private boolean isAllowPlayEnd(MediaPlayer mediaPlayer) {
        if (mediaPlayer == mPlayers.get(mSameNum)) {
            return true;
        }
        if (mediaPlayer.getDuration() < Constant.ALL_PLAY_END_MIN) {
            return true;
        }
        if (mediaPlayer.getDuration() > Constant.ALL_PLAY_END_MAX) {
            return true;
        }
        return false;
    }

    /**
     * 将待播放语音放入列表，如果当前非播放状态，则无需排队，直接播放
     *
     * @param list 语音列表
     */
    public void putQueue(final List<String> list) {
        //  记录这个list开始的播放时间，如果超过 认为的出错时间 就清空队列（避免污染下一次播放）
        if (System.currentTimeMillis() - mPlayListStartTime > Constant.ERROR_TIME_OUT) {
            mIsPlaying = false;
        }
        if (!mIsPlaying) {
            togglePlaying(true);
            startPlayList(list);
        } else {
            mListQueue.add(list);
        }
    }

    private void startPlayList(List<String> list) {
        mCurPlayList = list;
        mSameNum = "";
        for (int i = 0; i < mCurPlayList.size() - 1; i++) {
            if (mCurPlayList.get(i).equals(mCurPlayList.get(i + 1))) {
                mSameNum = mCurPlayList.get(i);
                break;
            }
        }
        mIndex = 0;
        mPlayListStartTime = System.currentTimeMillis();
        playNext();
    }

    public AssetFileDescriptor getAssetFileDescription(String filename) throws IOException {
        AssetManager manager = mContext.getAssets();
        return manager.openFd(filename);
    }
}
