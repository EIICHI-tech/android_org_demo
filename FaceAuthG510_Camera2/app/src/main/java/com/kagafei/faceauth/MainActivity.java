package com.kagafei.faceauth;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.media.Image;
import android.media.ImageReader;
import android.os.*;
import android.util.Base64;
import android.util.Size;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.*;
import java.nio.*;
import java.util.*;

public class MainActivity extends AppCompatActivity {
 private static final int REQ=20, N=48; private static final float THRESHOLD=.90f;
 private TextureView texture; private TextView status, score, cameraInfo; private Button enroll, verify, clear;
 private CameraDevice camera; private CameraCaptureSession session; private CaptureRequest.Builder previewBuilder; private ImageReader reader;
 private HandlerThread cameraThread; private Handler cameraHandler; private String cameraId="0"; private int sensorOrientation=270;
 private volatile boolean captureForEnroll=false, capturePending=false;
 private final FaceDetector detector=FaceDetection.getClient(new FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE).setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL).setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL).build());

 @Override public void onCreate(Bundle b){super.onCreate(b);buildUi();texture.setSurfaceTextureListener(surfaceListener);if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},REQ);updateState();}
 private void buildUi(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.HORIZONTAL);root.setPadding(22,22,22,22);root.setBackgroundColor(0xff07152e);
  FrameLayout box=new FrameLayout(this);texture=new TextureView(this);box.addView(texture,new FrameLayout.LayoutParams(-1,-1));TextView overlay=tv("顔を中央に合わせてください",19,Color.WHITE);overlay.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);overlay.setPadding(0,24,0,0);box.addView(overlay,new FrameLayout.LayoutParams(-1,-1));root.addView(box,new LinearLayout.LayoutParams(0,-1,3));
  LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setGravity(Gravity.CENTER);p.setPadding(26,8,8,8);TextView title=tv("顔認証デモ Camera2版",30,Color.WHITE);status=tv("カメラ準備中",21,Color.WHITE);score=tv("一致度: --",27,0xff50dca0);cameraInfo=tv("Camera ID: --",14,0xff9fc2ff);enroll=button("顔を登録",0xff2f80ed);verify=button("本人確認",0xff00a77b);clear=button("登録消去",0xff68758a);
  enroll.setOnClickListener(v->take(true));verify.setOnClickListener(v->take(false));clear.setOnClickListener(v->{getPreferences(MODE_PRIVATE).edit().clear().apply();score.setText("一致度: --");updateState();});TextView note=tv("G510専用: CameraXを使用せずCamera2 APIでCamera ID 0を開きます。\n展示用PoCです。重要な本人認証には使用しないでください。",14,0xffc2cad8);
  p.addView(title);p.addView(cameraInfo);p.addView(status);p.addView(score);p.addView(enroll);p.addView(verify);p.addView(clear);p.addView(note);root.addView(p,new LinearLayout.LayoutParams(0,-1,2));setContentView(root);}
 private TextView tv(String s,int z,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);v.setGravity(Gravity.CENTER);v.setPadding(5,10,5,10);return v;}
 private Button button(String s,int c){Button b=new Button(this);b.setText(s);b.setTextSize(18);b.setTextColor(Color.WHITE);b.setBackgroundColor(c);LinearLayout.LayoutParams q=new LinearLayout.LayoutParams(-1,74);q.setMargins(6,7,6,7);b.setLayoutParams(q);return b;}
 private final TextureView.SurfaceTextureListener surfaceListener=new TextureView.SurfaceTextureListener(){public void onSurfaceTextureAvailable(@NonNull SurfaceTexture s,int w,int h){startBackground();openCamera();}public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture s,int w,int h){}public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture s){return true;}public void onSurfaceTextureUpdated(@NonNull SurfaceTexture s){}};
 private void startBackground(){if(cameraThread!=null)return;cameraThread=new HandlerThread("G510Camera2");cameraThread.start();cameraHandler=new Handler(cameraThread.getLooper());}
 private void selectCamera(CameraManager m)throws Exception{String[] ids=m.getCameraIdList();if(ids.length==0)throw new Exception("カメラがありません");cameraId=ids[0];for(String id:ids){CameraCharacteristics c=m.getCameraCharacteristics(id);Integer f=c.get(CameraCharacteristics.LENS_FACING);if(f!=null&&f==CameraCharacteristics.LENS_FACING_FRONT){cameraId=id;break;}}CameraCharacteristics c=m.getCameraCharacteristics(cameraId);Integer o=c.get(CameraCharacteristics.SENSOR_ORIENTATION);sensorOrientation=o==null?0:o;runOnUiThread(()->cameraInfo.setText("Camera ID: "+cameraId+" / FRONT / Camera2"));}
 private void openCamera(){try{if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)return;CameraManager m=(CameraManager)getSystemService(Context.CAMERA_SERVICE);selectCamera(m);reader=ImageReader.newInstance(1280,720,android.graphics.ImageFormat.JPEG,2);reader.setOnImageAvailableListener(this::onJpeg,cameraHandler);m.openCamera(cameraId,stateCallback,cameraHandler);}catch(Exception e){showError("カメラ起動失敗: "+e);}}
 private final CameraDevice.StateCallback stateCallback=new CameraDevice.StateCallback(){public void onOpened(@NonNull CameraDevice c){camera=c;createPreview();}public void onDisconnected(@NonNull CameraDevice c){c.close();showError("カメラが切断されました");}public void onError(@NonNull CameraDevice c,int e){c.close();showError("Camera2 error="+e);}};
 private void createPreview(){try{SurfaceTexture st=texture.getSurfaceTexture();if(st==null)throw new Exception("SurfaceTexture=null");st.setDefaultBufferSize(1280,720);Surface surface=new Surface(st);previewBuilder=camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);previewBuilder.addTarget(surface);camera.createCaptureSession(Arrays.asList(surface,reader.getSurface()),new CameraCaptureSession.StateCallback(){public void onConfigured(@NonNull CameraCaptureSession s){session=s;try{previewBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);s.setRepeatingRequest(previewBuilder.build(),null,cameraHandler);runOnUiThread(()->{status.setText(load()==null?"未登録です。顔を登録してください":"登録済みです。本人確認できます");setBusy(false);});}catch(Exception e){showError("プレビュー開始失敗: "+e);}}public void onConfigureFailed(@NonNull CameraCaptureSession s){showError("カメラセッション構成失敗");}},cameraHandler);}catch(Exception e){showError("プレビュー構成失敗: "+e);}}
 private void take(boolean isEnroll){if(session==null||camera==null){toast("カメラ準備中です");return;}if(!isEnroll&&load()==null){toast("先に顔を登録してください");return;}captureForEnroll=isEnroll;capturePending=true;setBusy(true);status.setText("撮影・顔解析中…");try{CaptureRequest.Builder b=camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);b.addTarget(reader.getSurface());b.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);b.set(CaptureRequest.JPEG_ORIENTATION,(sensorOrientation+getWindowManager().getDefaultDisplay().getRotation()*90)%360);session.capture(b.build(),null,cameraHandler);}catch(Exception e){capturePending=false;setBusy(false);showError("撮影失敗: "+e);}}
 private void onJpeg(ImageReader r){Image image=r.acquireLatestImage();if(image==null||!capturePending){if(image!=null)image.close();return;}capturePending=false;ByteBuffer buf=image.getPlanes()[0].getBuffer();byte[] bytes=new byte[buf.remaining()];buf.get(bytes);image.close();Bitmap bmp=BitmapFactory.decodeByteArray(bytes,0,bytes.length);if(bmp==null){showError("JPEG変換失敗");return;}analyze(bmp,captureForEnroll);}
 private void analyze(Bitmap bmp,boolean isEnroll){detector.process(InputImage.fromBitmap(bmp,0)).addOnSuccessListener(faces->{if(faces.size()!=1){setBusy(false);status.setText(faces.isEmpty()?"顔が見つかりません":"1人だけ映してください");return;}Rect r=new Rect(faces.get(0).getBoundingBox());int pad=(int)(Math.max(r.width(),r.height())*.12f);r.left=Math.max(0,r.left-pad);r.top=Math.max(0,r.top-pad);r.right=Math.min(bmp.getWidth(),r.right+pad);r.bottom=Math.min(bmp.getHeight(),r.bottom+pad);if(r.width()<100||r.height()<100){setBusy(false);status.setText("顔をもう少し大きく映してください");return;}float[] v=feature(Bitmap.createBitmap(bmp,r.left,r.top,r.width(),r.height()));if(isEnroll){save(v);score.setText("一致度: 100.0%");status.setText("顔の登録が完了しました");}else{float sim=cos(v,load());boolean ok=sim>=THRESHOLD;score.setText(String.format(Locale.JAPAN,"一致度: %.1f%%",sim*100));score.setTextColor(ok?0xff50dca0:0xffff6e64);status.setText(ok?"本人と判定しました":"登録者と一致しません");}setBusy(false);}).addOnFailureListener(e->{setBusy(false);showError("顔解析失敗: "+e);});}
 private float[] feature(Bitmap src){Bitmap b=Bitmap.createScaledBitmap(src,N,N,true);float[] v=new float[N*N];float mean=0;for(int y=0;y<N;y++)for(int x=0;x<N;x++){int c=b.getPixel(x,y);float g=(Color.red(c)*.299f+Color.green(c)*.587f+Color.blue(c)*.114f)/255f;v[y*N+x]=g;mean+=g;}mean/=v.length;float n=0;for(int i=0;i<v.length;i++){v[i]-=mean;n+=v[i]*v[i];}n=(float)Math.sqrt(n);for(int i=0;i<v.length;i++)v[i]/=(n+1e-6f);return v;}
 private float cos(float[] a,float[] b){float s=0;for(int i=0;i<a.length;i++)s+=a[i]*b[i];return Math.max(-1,Math.min(1,s));}
 private void save(float[] v){ByteBuffer b=ByteBuffer.allocate(v.length*4).order(ByteOrder.LITTLE_ENDIAN);for(float x:v)b.putFloat(x);getPreferences(MODE_PRIVATE).edit().putString("face",Base64.encodeToString(b.array(),Base64.NO_WRAP)).apply();}
 private float[] load(){String s=getPreferences(MODE_PRIVATE).getString("face",null);if(s==null)return null;try{ByteBuffer b=ByteBuffer.wrap(Base64.decode(s,Base64.NO_WRAP)).order(ByteOrder.LITTLE_ENDIAN);float[] v=new float[N*N];for(int i=0;i<v.length;i++)v[i]=b.getFloat();return v;}catch(Exception e){return null;}}
 private void updateState(){boolean ok=load()!=null;if(status!=null)status.setText(ok?"登録済みです。本人確認できます":"未登録です。顔を登録してください");if(verify!=null)verify.setEnabled(ok&&session!=null);}
 private void setBusy(boolean x){runOnUiThread(()->{enroll.setEnabled(!x&&session!=null);verify.setEnabled(!x&&session!=null&&load()!=null);clear.setEnabled(!x);});}
 private void showError(String s){runOnUiThread(()->{status.setText(s);setBusy(false);});}
 private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
 @Override public void onRequestPermissionsResult(int r,@NonNull String[] p,@NonNull int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED){if(texture.isAvailable()){startBackground();openCamera();}}else status.setText("カメラ権限が必要です");}
 @Override protected void onPause(){closeCamera();super.onPause();}
 @Override protected void onResume(){super.onResume();if(texture!=null&&texture.isAvailable()&&camera==null&&ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED){startBackground();openCamera();}}
 private void closeCamera(){try{if(session!=null)session.close();if(camera!=null)camera.close();if(reader!=null)reader.close();}catch(Exception ignored){}session=null;camera=null;reader=null;if(cameraThread!=null){cameraThread.quitSafely();cameraThread=null;cameraHandler=null;}}
 @Override protected void onDestroy(){closeCamera();detector.close();super.onDestroy();}
}
