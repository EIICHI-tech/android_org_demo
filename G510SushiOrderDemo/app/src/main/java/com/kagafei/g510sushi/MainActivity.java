package com.kagafei.g510sushi;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.speech.tts.TextToSpeech;
import android.view.Surface;
import android.view.TextureView;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private TextView prompt, result, badge;
    private TextureView texture;
    private TextToSpeech tts;
    private SpeechRecognizer recognizer;
    private int state = 0;
    private CameraDevice cameraDevice;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b); setContentView(R.layout.activity_main);
        prompt=findViewById(R.id.prompt); result=findViewById(R.id.result); badge=findViewById(R.id.detectBadge); texture=findViewById(R.id.camera);
        requestPermissions(new String[]{Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO},10);
        tts=new TextToSpeech(this, s->{ if(s==TextToSpeech.SUCCESS) tts.setLanguage(Locale.JAPAN); });
        recognizer=SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener(){
            public void onResults(Bundle b){ ArrayList<String> x=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION); if(x!=null&&!x.isEmpty()) handle(x.get(0)); }
            public void onError(int e){ result.setText("認識できませんでした。もう一度お話しください"); }
            public void onReadyForSpeech(Bundle b){} public void onBeginningOfSpeech(){} public void onRmsChanged(float f){} public void onBufferReceived(byte[] b){} public void onEndOfSpeech(){} public void onPartialResults(Bundle b){} public void onEvent(int i,Bundle b){}
        });
        texture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener(){ public void onSurfaceTextureAvailable(SurfaceTexture st,int w,int h){ openCamera(); } public void onSurfaceTextureSizeChanged(SurfaceTexture s,int w,int h){} public boolean onSurfaceTextureDestroyed(SurfaceTexture s){return true;} public void onSurfaceTextureUpdated(SurfaceTexture s){} });
        findViewById(R.id.seat).setOnClickListener(v->seated());
        findViewById(R.id.speak).setOnClickListener(v->listen());
        findViewById(R.id.reset).setOnClickListener(v->reset());
    }
    private void seated(){ state=1; badge.setText("着席検知"); say("いらっしゃいませ。本日は何名様でいらっしゃっていますか？"); }
    private void listen(){ Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH); i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ja-JP"); i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); result.setText("音声を聞いています…"); recognizer.startListening(i); }
    private void handle(String text){ result.setText("認識結果: "+text); if(state==1){ state=2; say(text+"ですね。お飲み物は何にされますか？"); } else if(state==2){ state=3; say(text+"を承りました。ご注文ありがとうございます。"); } else say("着席検知ボタンから開始してください。"); }
    private void say(String text){ prompt.setText(text); tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"order"); }
    private void reset(){ state=0; badge.setText("カメラ待機中"); prompt.setText("着席を待っています"); result.setText("認識結果: -"); }
    private void openCamera(){ if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)return; try{ CameraManager m=(CameraManager)getSystemService(CAMERA_SERVICE); String id=m.getCameraIdList()[0]; m.openCamera(id,new CameraDevice.StateCallback(){ public void onOpened(CameraDevice c){cameraDevice=c; startPreview();} public void onDisconnected(CameraDevice c){c.close();} public void onError(CameraDevice c,int e){c.close();}},null);}catch(Exception e){badge.setText("カメラを開けません");}}
    private void startPreview(){ try{ SurfaceTexture st=texture.getSurfaceTexture(); st.setDefaultBufferSize(1280,720); Surface sf=new Surface(st); CaptureRequest.Builder b=cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW); b.addTarget(sf); cameraDevice.createCaptureSession(Collections.singletonList(sf),new CameraCaptureSession.StateCallback(){public void onConfigured(CameraCaptureSession s){try{s.setRepeatingRequest(b.build(),null,null);}catch(Exception ignored){}}public void onConfigureFailed(CameraCaptureSession s){}},null);}catch(Exception ignored){} }
    @Override protected void onDestroy(){ super.onDestroy(); if(recognizer!=null)recognizer.destroy(); if(tts!=null)tts.shutdown(); if(cameraDevice!=null)cameraDevice.close(); }
}
