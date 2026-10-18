package jp.co.kagafei.g510pickplace

import kotlinx.coroutines.delay
import kotlin.math.abs

class RobotPlanner(private val arm:EArmClient, private val state:(RobotState,String)->Unit) {
    @Volatile private var canceled=false
    fun cancel(){ canceled=true }
    private suspend fun check(){ if(canceled){arm.stopAll();throw InterruptedException()} }
    private suspend fun pulse(cmd:String,ms:Long,label:String){check();state(RobotState.APPROACH,label);if(!arm.pulse(cmd,ms)) throw IllegalStateException("eArm通信失敗")}

    suspend fun execute(target:Detection, dropRight:Boolean=true){
        canceled=false
        try {
            state(RobotState.HOMING,"安全開始位置へ移動"); arm.stopAll(); delay(200)
            state(RobotState.APPROACH,"グリッパーを開く"); arm.pulse("d_minus",500)
            // Camera normalized coordinate to conservative open-loop motion. Calibrate constants on the real arm.
            val x=(target.cx-.5f).coerceIn(-.45f,.45f)
            val y=(target.cy-.5f).coerceIn(-.45f,.45f)
            if(abs(x)>.04f) pulse(if(x<0) "a_minus" else "a_plus",(abs(x)*1250).toLong()+100,"ターゲット方向へ旋回")
            pulse(if(y<0) "b_minus" else "b_plus",(abs(y)*700).toLong()+120,"距離を調整")
            state(RobotState.DESCEND,"ターゲットへ下降"); arm.pulse("c_minus",650)
            check(); state(RobotState.GRAB,"把持"); arm.pulse("d_plus",750); delay(250)
            check(); state(RobotState.LIFT,"持ち上げ"); arm.pulse("c_plus",700)
            check(); state(RobotState.MOVE_TO_DROP,"配置場所へ移動"); arm.pulse(if(dropRight)"a_plus" else "a_minus",1050)
            check(); state(RobotState.DESCEND,"配置位置へ下降"); arm.pulse("c_minus",500)
            check(); state(RobotState.RELEASE,"開放"); arm.pulse("d_minus",650)
            check(); arm.pulse("c_plus",550)
            state(RobotState.COMPLETE,"Pick & Place 完了")
        } catch(_:InterruptedException){state(RobotState.STOPPED,"緊急停止")}
        catch(e:Exception){arm.stopAll();state(RobotState.ERROR,e.message?:"制御エラー")}
    }
}
