---
title: Android 本地收款语音实现
date: 2019-01-13 14:53:51
tags: Android
---
# Sample

可以通过运行 **sample** 查看使用的范例代码以及默认的效果
sample界面如下：

![voice2.png](https://upload-images.jianshu.io/upload_images/1633382-4b22a477f04d153d.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

# 如何使用

```java
# 在应用级依赖文件添加 bintray 的maven仓库地址
allprojects {
    repositories {
        jcenter()
        google()
        maven {url 'https://dl.bintray.com/cchao1024/maven'}
    }
}
// 添加 jar 包依赖
implementation 'com.github.cchao:VoiceSpeaker:1.0.0'

// 使用初始化
mVoiceSpeaker = VoiceSpeaker.getInstance(this);
// 进入下一音频的速度
mVoiceSpeaker.setPlayRatio(0.88f);
// 设置允许完成完整播放的音频长度（太短或太长 均让其完整播放完，才进入下一音频的部分）
mVoiceSpeaker.setMinMaxPlayEnd(100, 1500);

// 放入队列播放 音频
mVoiceSpeaker.putQueue(new VoiceSynthesize()
 .prefix("success")
 .numString(money)
 .build());
```

详细代码请移步 [https://github.com/cchao1024/voicePlayer](https://github.com/cchao1024/voicePlayer)


# 需求

> 需实现收到推送消息 （比如：支付宝到账 234.23元） 能播放语音文件。
> 遵循队列结构，先来的先播报，后来的排队等待。

原本使用的 **第三方语音合成SDK** 的，但在生产环境中发现播放播报会不播放或者播放一半就停止。

所以后面开始使用 **本地的语音音频** (.mp3)  拼接播放，不再采用 sdk 方案。 当前使用android标配的 **MediaPlayer** 。

# 方案确定

为了实现本地收款语音的播放，笔者想过多种方式实现：

1. 使用 单个MediaPlayer 循环，逐个播放音频文件
2. 使用 两个（或多个）MediaPlayer，交易播放音频队列
3. 使用多个MediaPlayer(对应音频文件数)，逐个播放音频文件


笔者在实现编码中3种方案均有实现过，但发现方案1，2因为来回切换播放源会有严重延迟。

最终确认方案3 是可行且稳妥的方案，以下是详细思路

# 基本思路

1. 初始化VoiceSpeaker时同时构建多个MediaPlayer(对应音频文件数)并设置好音频源
2. 调用者放入**待播放语音文字**（如：收款成功 23.5 元），判断如果正在播放，就将语音文字放入**待播放队列**
3. 取出队列头部的**待播放语音文字**，将其转化成大写中文金额写法（如：收款成功 二十三点五元）
4. 根据中文金额的待播放语音文字**映射**成对应的音频文件名，封装成List （如：[2,ten,3,dot,5,元]
5. 遍历**待播放音频文件名List**  依次取出MediaPlayer列表播放
6. 播放完成，查看**待播放队列**是否有**待播放语音文字**，
7. 存在则继续执行 3-6，否则 END


思路还是很清晰的，但是编写过程中还是遇到了问题。如下图

```
    二
-----------       三
              ----------      点
                          ----------       五
                                       ----------      元
                                                    -----------
```



通过 **setOnCompletionListener ** 监听上一播放音频的结束，结束了 才开始下一音频的播放，但是音频文件的前后部分 是会有一部分是没有声音的，这就导致 读起来像

【收款成功，二    十    三   点   五  元】中间有间隔。

所以 笔者想到了一种方法：

# 最终思路

在上一音频还未播放结束时(到 85%这样）

就开始播放下一音频（这个值可控）,如下图

```
     二
  ----------   三
          ----------    点
                   ----------    五
                            ----------   元
                                     -----------
```

So，这个实现过程就是这样

# code

## 核心播放器

```java
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

    // 池
    private ExecutorService mExecutorService = Executors.newCachedThreadPool();
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
    private float mRatio = Constant.NEXT_PLAY_RATIO;

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
}
```


## 暴露的公共方法

```java
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
```


## 播放

```java
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
    final int duration = mediaPlayer.getDuration();
    mediaPlayer.start();
    if (!isAllowPlayEnd(mediaPlayer)) {
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep((long) (duration * mRatio));
                    playNext();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}
```

# ref

[https://www.jianshu.com/p/df2022b3937d](https://www.jianshu.com/p/df2022b3937d)

