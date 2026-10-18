package com.kagafei.faceauth;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
 private static final int REQ=20, N=32; private static final float THRESHOLD=0.91f;
 private PreviewView preview; private TextView status, score; private Button enroll, verify, clear;
 private final ExecutorService cameraExecutor=Executors.newSingleThreadExecutor();
 private final FaceDetector detector=FaceDetection.getClient(new FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE).setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL).setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL).build());

 @Override public void onCreate(Bundle b){super.onCreate(b); buildUi(); if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) startCamera(); else ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},REQ); updateState();}
 private void buildUi(){
  LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.HORIZONTAL); root.setBackgroundColor(Color.rgb(7,21,46)); root.setPadding(24,24,24,24);
  FrameLayout cameraBox=new FrameLayout(this); preview=new PreviewView(this); preview.setScaleType(PreviewView.ScaleType.FIT_CENTER); cameraBox.addView(preview,new FrameLayout.LayoutParams(-1,-1));
  TextView frame=new TextView(this); frame.setText("顔を中央に合わせてください\n正面・明るい場所・1人だけ"); frame.setTextColor(Color.WHITE); frame.setTextSize(18); frame.setGravity(Gravity.CENTER); frame.setBackgroundColor(0x2200A0FF); cameraBox.addView(frame,new FrameLayout.LayoutParams(-1,-1));
  root.addView(cameraBox,new LinearLayout.LayoutParams(0,-1,3));
  LinearLayout panel=new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL); panel.setGravity(Gravity.CENTER); panel.setPadding(28,10,10,10);
  TextView title=tv("顔認証デモ",32,Color.WHITE); status=tv("カメラ準備中",21,Color.WHITE); score=tv("一致度: --",28,Color.rgb(80,220,160));
  enroll=button("顔を登録",0xff2f80ed); verify=button("本人確認",0xff00a77b); clear=button("登録消去",0xff68758a);
  enroll.setOnClickListener(v->capture(true)); verify.setOnClickListener(v->capture(false)); clear.setOnClickListener(v->{getPreferences(MODE_PRIVATE).edit().clear().apply(); score.setText("一致度: --"); updateState();});
  TextView note=tv("端末内処理の展示用PoC\n登録画像そのものは保存せず、\n32×32の正規化特徴量のみ保存します。\n重要な本人認証には使用しないでください。",14,0xffc2cad8); note.setGravity(Gravity.CENTER);
  panel.addView(title); panel.addView(status); panel.addView(score); panel.addView(enroll); panel.addView(verify); panel.addView(clear); panel.addView(note);
  root.addView(panel,new LinearLayout.LayoutParams(0,-1,2)); setContentView(root);
 }
 private TextView tv(String s,int z,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);v.setGravity(Gravity.CENTER);v.setPadding(6,12,6,12);return v;}
 private Button button(String s,int c){Button b=new Button(this);b.setText(s);b.setTextSize(18);b.setTextColor(Color.WHITE);b.setBackgroundColor(c);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,76);p.setMargins(8,8,8,8);b.setLayoutParams(p);return b;}
 private void startCamera(){ListenableFuture<ProcessCameraProvider> f=ProcessCameraProvider.getInstance(this);f.addListener(()->{try{ProcessCameraProvider p=f.get();Preview pr=new Preview.Builder().build();pr.setSurfaceProvider(preview.getSurfaceProvider());p.unbindAll();CameraSelector front=new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();p.bindToLifecycle(this,front,pr);status.setText("顔を登録してください");}catch(Exception e){status.setText("カメラ起動失敗: "+e.getMessage());}},ContextCompat.getMainExecutor(this));}
 private void capture(boolean isEnroll){if(!isEnroll&&load()==null){toast("先に顔を登録してください");return;} Bitmap bmp=preview.getBitmap();if(bmp==null){toast("カメラ画像を取得できません");return;}setBusy(true);status.setText("顔を解析中…");detector.process(InputImage.fromBitmap(bmp,0)).addOnSuccessListener(faces->onFaces(bmp,faces,isEnroll)).addOnFailureListener(e->{setBusy(false);status.setText("解析エラー: "+e.getMessage());});}
 private void onFaces(Bitmap bmp,List<Face> faces,boolean isEnroll){if(faces.size()!=1){setBusy(false);status.setText(faces.isEmpty()?"顔が見つかりません":"1人だけ映してください");return;}Face face=faces.get(0);Rect r=new Rect(face.getBoundingBox());int pad=(int)(Math.max(r.width(),r.height())*.12);r.left=Math.max(0,r.left-pad);r.top=Math.max(0,r.top-pad);r.right=Math.min(bmp.getWidth(),r.right+pad);r.bottom=Math.min(bmp.getHeight(),r.bottom+pad);if(r.width()<100||r.height()<100){setBusy(false);status.setText("顔をもう少し大きく映してください");return;}float[] vec=feature(Bitmap.createBitmap(bmp,r.left,r.top,r.width(),r.height()));if(isEnroll){save(vec);score.setText("一致度: 100.0%");status.setText("顔の登録が完了しました");}else{float sim=cos(vec,load());boolean ok=sim>=THRESHOLD;score.setText(String.format(Locale.JAPAN,"一致度: %.1f%%",sim*100));score.setTextColor(ok?0xff50dca0:0xffff6e64);status.setText(ok?"本人と判定しました":"登録者と一致しません");}setBusy(false);}
 private float[] feature(Bitmap src){Bitmap b=Bitmap.createScaledBitmap(src,N,N,true);float[] v=new float[N*N];float mean=0;for(int y=0;y<N;y++)for(int x=0;x<N;x++){int c=b.getPixel(x,y);float g=(Color.red(c)*.299f+Color.green(c)*.587f+Color.blue(c)*.114f)/255f;v[y*N+x]=g;mean+=g;}mean/=v.length;float n=0;for(int i=0;i<v.length;i++){v[i]-=mean;n+=v[i]*v[i];}n=(float)Math.sqrt(n);for(int i=0;i<v.length;i++)v[i]/=(n+1e-6f);return v;}
 private float cos(float[] a,float[] b){float s=0;for(int i=0;i<a.length;i++)s+=a[i]*b[i];return Math.max(-1,Math.min(1,s));}
 private void save(float[] v){ByteBuffer b=ByteBuffer.allocate(v.length*4).order(ByteOrder.LITTLE_ENDIAN);for(float x:v)b.putFloat(x);getPreferences(MODE_PRIVATE).edit().putString("face",Base64.encodeToString(b.array(),Base64.NO_WRAP)).apply();}
 private float[] load(){String s=getPreferences(MODE_PRIVATE).getString("face",null);if(s==null)return null;try{ByteBuffer b=ByteBuffer.wrap(Base64.decode(s,Base64.NO_WRAP)).order(ByteOrder.LITTLE_ENDIAN);float[] v=new float[N*N];for(int i=0;i<v.length;i++)v[i]=b.getFloat();return v;}catch(Exception e){return null;}}
 private void updateState(){boolean ok=load()!=null;status.setText(ok?"登録済みです。本人確認できます":"未登録です。顔を登録してください");verify.setEnabled(ok);}
 private void setBusy(boolean x){enroll.setEnabled(!x);verify.setEnabled(!x&&load()!=null);clear.setEnabled(!x);}
 private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
 @Override public void onRequestPermissionsResult(int r,@NonNull String[] p,@NonNull int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)startCamera();else status.setText("カメラ権限が必要です");}
 @Override protected void onDestroy(){detector.close();cameraExecutor.shutdown();super.onDestroy();}
}
