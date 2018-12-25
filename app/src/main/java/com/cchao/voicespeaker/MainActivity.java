package com.cchao.voicespeaker;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.cchao.voicesplayer.library.VoiceSpeaker;
import com.cchao.voicesplayer.library.VoiceSynthesize;

public class MainActivity extends AppCompatActivity {

    VoiceSpeaker mVoiceSpeaker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initPlayer();

        findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText = findViewById(R.id.edit);
                String money=editText.getText().toString();
                if (verifyInCorrect(money)) {
                    return;
                }
                play(money);
            }
        });
    }

    private void play(String money) {
        mVoiceSpeaker.putQueue(new VoiceSynthesize()
            .prefix("kjs")
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

    private void initPlayer() {
        mVoiceSpeaker = new VoiceSpeaker();
        mVoiceSpeaker.init();
    }
}
