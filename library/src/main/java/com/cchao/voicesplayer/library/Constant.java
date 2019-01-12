package com.cchao.voicesplayer.library;

/**
 * @author cchao
 * @version 1/12/19.
 */
public class Constant {

    /**
     * 判定为播放出错的超时时间
     */
    public static long ERROR_TIME_OUT = 30 * 1000;

    /**
     * 允许完整的播放的最小值
     */
    public static long ALL_PLAY_END_MIN = 150;
    /**
     * 允许完整的播放的最大值
     */
    public static long ALL_PLAY_END_MAX = 500;

    /**
     * 音频文件名 数组
     */
    public static final String[] VOICE_NAME_ARR = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "dot", "hundred", "success", "ten", "ten_thousand", "thousand", "yuan"};

    /**
     * 数字的 char 数组
     */
    public static final char[] NUM = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    /**
     * 金额的大写
     */
    public static final char[] CHINESE_UNIT = {'元', '拾', '佰', '仟', '万', '拾', '佰', '仟', '亿', '拾', '佰', '仟'};

}
