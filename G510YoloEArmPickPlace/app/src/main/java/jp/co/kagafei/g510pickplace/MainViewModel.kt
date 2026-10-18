package jp.co.kagafei.g510pickplace

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel(app:Application):AndroidViewModel(app){
    private val detector=YoloV8Detector(app)
    private val arm=EArmClient()
    private val planner=RobotPlanner(arm){s,m->state=s;message=m}
    var detections=emptyList<Detection>(); private set
    var selected:Detection?=null; private set
    var state=RobotState.IDLE; private set
    var message=detector.status; private set
    var armOnline=false; private set
    var inferenceMs=0L; private set
    private var robotJob:Job?=null
    private var busy=false

    init { viewModelScope.launch { while(true){armOnline=arm.ping();delay(3000)} } }
    fun analyze(bitmap:Bitmap){if(busy)return;busy=true;viewModelScope.launch{kotlin.runCatching{val st=System.currentTimeMillis();detections=detector.detect(bitmap);inferenceMs=System.currentTimeMillis()-st}.onFailure{message=it.message?:"推論エラー"};busy=false}}
    fun select(d:Detection){selected=d;message="選択: ${d.label}。実行前に周囲を確認してください"}
    fun execute(){val t=selected?:return;if(!armOnline){message="eArm Wi-Fiが未接続です";return};if(robotJob?.isActive==true)return;robotJob=viewModelScope.launch{planner.execute(t)}}
    fun emergencyStop(){planner.cancel();robotJob?.cancel();robotJob=viewModelScope.launch{arm.stopAll();state=RobotState.STOPPED;message="緊急停止しました"}}
    override fun onCleared(){detector.close();planner.cancel();super.onCleared()}
}
