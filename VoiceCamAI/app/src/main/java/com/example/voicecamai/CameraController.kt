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
class CameraController(private val context:Context,private val owner:LifecycleOwner,private val view:PreviewView,private val status:(String)->Unit){
 private var provider:ProcessCameraProvider?=null; private var camera:Camera?=null; private var capture:ImageCapture?=null; private var front=true; private var zoom=1f; private var exposure=0
 fun start(){val f=ProcessCameraProvider.getInstance(context);f.addListener({provider=f.get();bind()},ContextCompat.getMainExecutor(context))}
 private fun bind(){val p=Preview.Builder().build().also{it.setSurfaceProvider(view.surfaceProvider)};capture=ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build();val s=if(front)CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA;try{provider?.unbindAll();camera=provider?.bindToLifecycle(owner,s,p,capture);zoom=1f;exposure=0}catch(e:Exception){status("カメラ開始失敗: ${e.message}")}}
 fun switch(){front=!front;bind()}
 fun zoom(delta:Float){val max=camera?.cameraInfo?.zoomState?.value?.maxZoomRatio?:1f;zoom=(zoom+delta).coerceIn(1f,max);camera?.cameraControl?.setZoomRatio(zoom);status("ズーム %.1fx".format(zoom))}
 fun expose(delta:Int){val range=camera?.cameraInfo?.exposureState?.exposureCompensationRange?:return;exposure=(exposure+delta).coerceIn(range.lower,range.upper);camera?.cameraControl?.setExposureCompensationIndex(exposure);status("露出 $exposure")}
 fun take(){val c=capture?:return;val name="VoiceCam_"+SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(Date());val cv=ContentValues().apply{put(MediaStore.MediaColumns.DISPLAY_NAME,name);put(MediaStore.MediaColumns.MIME_TYPE,"image/jpeg");put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/VoiceCamAI")};val output=ImageCapture.OutputFileOptions.Builder(context.contentResolver,MediaStore.Images.Media.EXTERNAL_CONTENT_URI,cv).build();c.takePicture(output,ContextCompat.getMainExecutor(context),object:ImageCapture.OnImageSavedCallback{override fun onImageSaved(r:ImageCapture.OutputFileResults){status("保存しました: Pictures/VoiceCamAI")};override fun onError(e:ImageCaptureException){status("撮影失敗: ${e.message}")}})}
}
