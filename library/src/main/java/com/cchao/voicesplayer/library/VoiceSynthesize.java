package com.cchao.voicesplayer.library;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 音频队列合成（生成待播放的音频文件名列表）
 *
 * @author cchao
 * @version 11/6/18.
 */

public class VoiceSynthesize {

    private String mNumText;

    /**
     * 语音开头的音频文件名 如（wechat)
     */
    private String mPrefix = "alipay";

    /**
     * 语音开头的音频文件名 默认为 yuan
     */
    private String mSuffix = "yuan";

    public VoiceSynthesize prefix(String prefix) {
        this.mPrefix = prefix;
        return this;
    }

    public String getSuffix() {
        return mSuffix;
    }

    public VoiceSynthesize suffix(String suffix) {
        this.mSuffix = suffix;
        return this;
    }

    public VoiceSynthesize numString(String numString) {
        this.mNumText = numString;
        return this;
    }

    /**
     * 拼接头尾，生成播放队列
     */
    public List<String> build() {
        List<String> result = new ArrayList<>();
        if (!TextUtils.isEmpty(mPrefix)) {
            result.add(mPrefix);
        }
        if (!TextUtils.isEmpty(mNumText)) {
            result.addAll(getMoneyPlayQueue(mNumText));
        }

        if (!TextUtils.isEmpty(mSuffix)) {
            result.add(mSuffix);
        }
        return result;
    }

    /**
     * 获取金额播放队列，拆分为 小数和整数部分 分别解析
     */
    private List<String> getMoneyPlayQueue(String numString) {
        List<String> result = new ArrayList<>();
        if (TextUtils.isEmpty(numString)) {
            return result;
        }
        if (numString.contains(".")) {
            String integerPart = numString.split("\\.")[0];
            String decimalPart = numString.split("\\.")[1];
            List<String> intList = genIntPart(integerPart);
            List<String> decimalList = genDecimalPart(decimalPart);
            result.addAll(intList);
            if (!decimalList.isEmpty()) {
                result.add("dot");
                result.addAll(decimalList);
            }
        } else {
            result.addAll(genIntPart(numString));
        }
        return result;
    }

    /**
     * 小数部分
     */
    private List<String> genDecimalPart(String decimalPart) {
        List<String> result = new ArrayList<>();
        // 结尾是 .00的不播 （如：  2.00 播放为 贰元）
        if ("00".equals(decimalPart)) {
            return result;
        }
        char[] chars = decimalPart.toCharArray();
        for (char ch : chars) {
            result.add(String.valueOf(ch));
        }
        // 结尾是 .0的不播 （如：  2.10 播放为 贰点一元）
        if (!result.isEmpty()) {
            if ("0".equals(result.get(result.size() - 1))) {
                result.remove(result.size() - 1);
            }
        }
        return result;
    }

    /**
     * 整数部分
     */
    private List<String> genIntPart(String integerPart) {
        List<String> result = new ArrayList<>();
        String intString = tranIntToUpCase(Integer.parseInt(integerPart));
        int len = intString.length();
        for (int i = 0; i < len; i++) {
            char current = intString.charAt(i);
            switch (current) {
                case '拾':
                    result.add("ten");
                    break;
                case '佰':
                    result.add("hundred");
                    break;
                case '仟':
                    result.add("thousand");
                    break;
                case '万':
                    result.add("ten_thousand");
                    break;
                case '亿':
                    result.add("ten_million");
                    break;
                default:
                    result.add(String.valueOf(current));
                    break;
            }
        }
        return result;
    }

    /**
     * 生成中文大写金额
     */
    public static String tranIntToUpCase(int num) {
        String res = "";
        int i = 0;
        if (num == 0) {
            return "0";
        }

        if (num == 10) {
            return "拾";
        }

        if (num > 10 && num < 20) {
            return "拾" + num % 10;
        }

        while (num > 0) {
            res = Constant.CHINESE_UNIT[i++] + res;
            res = Constant.NUM[num % 10] + res;
            num /= 10;
        }
        return res.replaceAll("0[拾佰仟]", "0")
            .replaceAll("0+亿", "亿")
            .replaceAll("0+万", "万")
            .replaceAll("0+元", "元")
            .replaceAll("0+", "0")
            .replace("元", "");
    }
}
