package com.example.dokaraoke;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {


    VideoView mvideoview;
    String video = "startup_video";
    String packageName = MyApp.getContext().getPackageName();
    Uri uri = getMedia(video);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mvideoview = findViewById(R.id.videoView);
        mvideoview.setVideoURI(uri);
        mvideoview.requestFocus();


        mvideoview.setOnPreparedListener(mp -> mvideoview.start());



        mvideoview.setOnCompletionListener(mp -> {
            mvideoview.stopPlayback();
            mvideoview.suspend();
            finish();
            mp.release();
            Intent myIntent = new Intent(MainActivity.this, MainActivity2.class);
            startActivity(myIntent);
        });
    }


    private Uri getMedia(String mediaName) {
        return Uri.parse("android.resource://" + packageName +
                "/raw/" + mediaName);
    }


}

