package com.example.voicecamai
sealed interface VoiceCommand { data object Wake:VoiceCommand; data object Capture:VoiceCommand; data object Timer:VoiceCommand; data object ZoomIn:VoiceCommand; data object ZoomOut:VoiceCommand; data object Brighter:VoiceCommand; data object Darker:VoiceCommand; data object Switch:VoiceCommand; data object Center:VoiceCommand; data object Skin:VoiceCommand; data object Exit:VoiceCommand; data object Unknown:VoiceCommand }
object VoiceCommandProcessor {
 fun parse(raw:String,awake:Boolean):VoiceCommand { val t=raw.lowercase().replace(" ","").replace("　","")
  if(listOf("hiカメラ","ハイカメラ","はいカメラ","heyカメラ").any(t::contains)) return VoiceCommand.Wake
  if(!awake) return VoiceCommand.Unknown
  return when { listOf("3秒","三秒").any(t::contains)->VoiceCommand.Timer; listOf("撮って","撮影","写真").any(t::contains)->VoiceCommand.Capture; listOf("ズームアウト","引いて","小さく").any(t::contains)->VoiceCommand.ZoomOut; listOf("ズーム","寄って","大きく").any(t::contains)->VoiceCommand.ZoomIn; t.contains("明る")->VoiceCommand.Brighter; t.contains("暗く")->VoiceCommand.Darker; listOf("切り替え","背面","前面").any(t::contains)->VoiceCommand.Switch; t.contains("中央")->VoiceCommand.Center; t.contains("肌")->VoiceCommand.Skin; listOf("終了","終わり","ストップ").any(t::contains)->VoiceCommand.Exit; else->VoiceCommand.Unknown }
 }
}
