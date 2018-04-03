package com.jikai.waveanimation;

import android.Manifest;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.tbruyelle.rxpermissions.RxPermissions;

import rx.functions.Action1;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();

    View animView;
    VolumeWaveDrawable mAnimDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int[] colors = new int[] { 0xFF985FEE, 0xFFAB4FF9, 0xFF916AFB,
                0xFF6694FC, 0xFF3AC4FD };
        mAnimDrawable = new VolumeWaveDrawable.Builder().colors(colors)
                .minAmplitude(0.1).Build(this);
        animView = findViewById(R.id.anim_view);
        animView.setBackground(mAnimDrawable);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        new RxPermissions(this)
                .request(Manifest.permission.RECORD_AUDIO)
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        if (aBoolean) {
                            startRecording();
                        } else {
                            finish();
                        }
                    }
                });
    }

    private void startRecording() {
        new AudioRecorder(new AudioRecorder.VolumeListener() {
            @Override
            public void onVolumeChanged(double volume) {
                if (mAnimDrawable.isRunning()) {
                    double amp = volume / 50;
                    Log.d(TAG, "onVolumeChanged: " + amp);
                    mAnimDrawable.setAmplitude(amp);
                }
            }
        }).getNoiseLevel();
    }

    public void startAnimation(View view) {
        mAnimDrawable.start();
    }

    public void stopAnimation(View view) {
        mAnimDrawable.stop();
    }
}
