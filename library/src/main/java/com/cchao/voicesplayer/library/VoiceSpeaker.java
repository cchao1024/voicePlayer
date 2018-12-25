package com.cchao.voicesplayer.library;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.util.Log;

import com.cchao.simplelib.LibCore;
import com.cchao.simplelib.core.Logs;
import com.cchao.simplelib.core.RxHelper;
import com.cchao.simplelib.util.ExceptionCollect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * 播放者，通过 多个 MediaPlayer 分别播放音频文件，
 *
 * @author cchao
 * @version 11/6/18.
 */
public class VoiceSpeaker {

    private final static String TAG = "VoiceSpeaker";
    private final String[] mStringVoice = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "dot", "hundred", "kjs", "ten", "ten_thousand", "thousand", "yuan"};

    Context mContext;

    private int mIndex = 0;
    Queue<List<String>> mListQueue = new LinkedBlockingDeque<>();
    List<String> mCurPlayList = new ArrayList<>();
    HashMap<String, MediaPlayer> mPlayers = new HashMap<>();

    private boolean mIsPlaying;
    private String mSameNum;
    //  记录这个list开始的播放时间，如果超过 10秒 就清空队列
    long mPlayListStartTime = 0;

    public VoiceSpeaker(Context context) {
        mContext = context;
    }

    public void init() {
        Executors.newCachedThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                for (String item : mStringVoice) {
                    try {
                        MediaPlayer player = new MediaPlayer();
                        String path = String.format("kjs/%s.mp3", item);
                        AssetFileDescriptor fd = getAssetFileDescription(path);
                        player.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
                        player.prepareAsync();
                        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
//                            playNext();
                                if (isNeedPlayEnd(mp)) {
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
        if (!isNeedPlayEnd(mediaPlayer)) {
            RxHelper.timerConsumer((long) (duration * 0.85), new Consumer<Long>() {
                @Override
                public void accept(Long aLong) throws Exception {
                    playNext();
                }
            });
        }
    }

    private void togglePlaying(boolean b) {
        if (b == mIsPlaying) {
            return;
        }
        mIsPlaying = b;
        if (!mIsPlaying) {
            return;
        }
        mDisposable = Observable.interval(5, TimeUnit.MILLISECONDS)
            .subscribe(aLong -> {
                if (!mIsPlaying) {
                    return;
                }
                if (mIsPlaying) {
                    return;
                }
            });
    }

    private boolean isNeedPlayEnd(MediaPlayer mediaPlayer) {
        if (mediaPlayer == mPlayers.get(mSameNum)) {
            return true;
        }
        return mediaPlayer.getDuration() < 300 || mediaPlayer.getDuration() > 1400;
    }

    public void putQueue(final List<String> list) {
        //  记录这个list开始的播放时间，如果超过 10秒 就清空队列
        if (System.currentTimeMillis() - mPlayListStartTime > 30 * 1000) {
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
