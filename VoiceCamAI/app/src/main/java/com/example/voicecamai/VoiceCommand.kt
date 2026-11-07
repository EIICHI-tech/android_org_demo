package com.example.voicecamai
sealed interface VoiceCommand {
 data object Wake: VoiceCommand; data object Capture: VoiceCommand; data object TimedCapture: VoiceCommand
 data object ZoomIn: VoiceCommand; data object ZoomOut: VoiceCommand; data object Brighter: VoiceCommand
 data object Darker: VoiceCommand; data object SwitchCamera: VoiceCommand; data object CenterFace: VoiceCommand
 data object SkinBright: VoiceCommand; data object Exit: VoiceCommand; data class Unknown(val text:String): VoiceCommand
}
object VoiceCommandProcessor {
 fun parse(raw:String, awake:Boolean): VoiceCommand {
  val t=raw.lowercase().replace(" ","").replace("　","")
  if (listOf("hiカメラ","ハイカメラ","はいカメラ","heyカメラ").any{t.contains(it)}) return VoiceCommand.Wake
  if(!awake) return VoiceCommand.Unknown(raw)
  return when {
   t.contains("3秒") || t.contains("三秒") -> VoiceCommand.TimedCapture
   listOf("撮って","撮影","写真").any{t.contains(it)} -> VoiceCommand.Capture
   listOf("ズームアウト","引いて","小さく").any{t.contains(it)} -> VoiceCommand.ZoomOut
   listOf("ズーム","寄って","大きく").any{t.contains(it)} -> VoiceCommand.ZoomIn
   listOf("明るく","明るい").any{t.contains(it)} -> VoiceCommand.Brighter
   listOf("暗く","暗い").any{t.contains(it)} -> VoiceCommand.Darker
   listOf("切り替え","背面","前面").any{t.contains(it)} -> VoiceCommand.SwitchCamera
   t.contains("中央") -> VoiceCommand.CenterFace
   t.contains("肌") -> VoiceCommand.SkinBright
   listOf("終了","おわり","ストップ").any{t.contains(it)} -> VoiceCommand.Exit
   else -> VoiceCommand.Unknown(raw)
  }
 }
}
