package com.example.chenhao.mp3test;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.chenhao.mp3test.utils.AudioUtils;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "alame.mp3";
    private Button btn_Record;
    private Button btn_play;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();

    }

    private void initViews() {
        btn_Record = (Button) findViewById(R.id.record);
        btn_play = (Button) findViewById(R.id.play);
        btn_Record.setOnClickListener(this);
        btn_play.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            //录音
            case R.id.record:
                Log.e("233", "path: " + path);
                AudioUtils.getInstance().start(new File(path));
                break;
            //播放
            case R.id.play:
                AudioUtils.getInstance().stop();

                break;


        }
    }
}
