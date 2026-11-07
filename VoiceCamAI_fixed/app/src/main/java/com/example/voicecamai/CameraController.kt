package com.example.voicecamai
import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.text.SimpleDateFormat
import java.util.*
class CameraController(private val c:Context,private val owner:LifecycleOwner,private val view:PreviewView,private val status:(String)->Unit){private var provider:ProcessCameraProvider?=null;private var camera:Camera?=null;private var capture:ImageCapture?=null;private var front=true;private var zoom=1f;private var exposure=0
 fun start(){val f=ProcessCameraProvider.getInstance(c);f.addListener({provider=f.get();bind()},ContextCompat.getMainExecutor(c))}
 private fun bind(){val p=Preview.Builder().build().also{it.setSurfaceProvider(view.surfaceProvider)};capture=ImageCapture.Builder().build();try{provider?.unbindAll();camera=provider?.bindToLifecycle(owner,if(front)CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA,p,capture);zoom=1f;exposure=0}catch(e:Exception){status("カメラ失敗: ${e.message}")}}
 fun switch(){front=!front;bind()};fun zoom(d:Float){val max=camera?.cameraInfo?.zoomState?.value?.maxZoomRatio?:1f;zoom=(zoom+d).coerceIn(1f,max);camera?.cameraControl?.setZoomRatio(zoom);status("ズーム %.1fx".format(zoom))};fun expose(d:Int){val x=camera?.cameraInfo?.exposureState?:return;exposure=(exposure+d).coerceIn(x.exposureCompensationRange.lower,x.exposureCompensationRange.upper);camera?.cameraControl?.setExposureCompensationIndex(exposure);status("露出 $exposure")}
 fun take(){val x=capture?:return;val cv=ContentValues().apply{put(MediaStore.MediaColumns.DISPLAY_NAME,"VoiceCam_"+SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(Date()));put(MediaStore.MediaColumns.MIME_TYPE,"image/jpeg");put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/VoiceCamAI")};val o=ImageCapture.OutputFileOptions.Builder(c.contentResolver,MediaStore.Images.Media.EXTERNAL_CONTENT_URI,cv).build();x.takePicture(o,ContextCompat.getMainExecutor(c),object:ImageCapture.OnImageSavedCallback{override fun onImageSaved(r:ImageCapture.OutputFileResults)=status("写真を保存しました");override fun onError(e:ImageCaptureException)=status("撮影失敗: ${e.message}")})}}
