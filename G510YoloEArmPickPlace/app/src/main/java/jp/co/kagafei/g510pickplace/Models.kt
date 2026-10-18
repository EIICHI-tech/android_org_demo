package jp.co.kagafei.g510pickplace

import android.graphics.RectF

data class Detection(val label: String, val score: Float, val box: RectF) {
    val cx: Float get() = (box.left + box.right) / 2f
    val cy: Float get() = (box.top + box.bottom) / 2f
}

data class Target(val detection: Detection, val selectedAt: Long = System.currentTimeMillis())
enum class RobotState { IDLE, HOMING, APPROACH, DESCEND, GRAB, LIFT, MOVE_TO_DROP, RELEASE, COMPLETE, STOPPED, ERROR }
