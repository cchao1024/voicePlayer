package com.cchao.voicespeaker;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.cchao.voicesplayer.library.VoiceSpeaker;
import com.cchao.voicesplayer.library.VoiceSynthesize;

/**
 * 使用 VoiceSpeaker sample
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    VoiceSpeaker mVoiceSpeaker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mVoiceSpeaker = VoiceSpeaker.getInstance(this);
        mVoiceSpeaker.setPlayRatio(0.88f);
        mVoiceSpeaker.setMinMaxPlayEnd(100, 1500);
    }

    private void play(String money) {
        mVoiceSpeaker.putQueue(new VoiceSynthesize()
            .prefix("success")
            .numString(money)
            .build());
    }

    boolean verifyInCorrect(String money) {
        if (TextUtils.isEmpty(money)) {
            return true;
        }
        float value;
        try {
            value = Float.valueOf(money);
        } catch (Exception ex) {
            showText("请输入正确的金额");
            return true;
        }
        if (money.endsWith(".")
            || money.startsWith(".")
            || value <= 0) {
            showText("请输入正确的金额");
            return true;
        }
        return false;
    }

    private void showText(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play:
                EditText editText = findViewById(R.id.edit);
                String money = editText.getText().toString();
                if (verifyInCorrect(money)) {
                    return;
                }
                play(money);
                break;
            case R.id.sample_1:
                play("109.84");
                break;
            case R.id.sample_2:
                play("9.84");
                break;
            case R.id.sample_3:
                play("1234");
                break;
        }
    }
}
