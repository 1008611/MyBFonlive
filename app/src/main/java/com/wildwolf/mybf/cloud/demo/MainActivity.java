package com.wildwolf.mybf.cloud.demo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.wildwolf.mybf.R;

public class MainActivity extends AppCompatActivity {

    private Button btVod = null;
    private Button btLive = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void init() {
        btVod = (Button)findViewById(R.id.vod);
        btVod.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, VodDemo.class);
                startActivity(intent);
            }
        });
        btLive = (Button) findViewById(R.id.live);
        btLive.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, LiveDemo.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.media_center_demo).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, MediaCenterDemo.class);
                startActivity(intent);
            }
        });
    }
}
