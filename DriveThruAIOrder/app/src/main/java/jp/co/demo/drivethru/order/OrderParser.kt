package jp.co.demo.drivethru.order
import jp.co.demo.drivethru.data.*

data class ParsedOrder(val product:MenuProduct,val quantity:Int)
object OrderParser {
 fun parse(text:String):List<ParsedOrder>{
  val normalized=text.replace(" ","").replace("　","")
  val kanji=mapOf('一' to 1,'二' to 2,'三' to 3,'四' to 4,'五' to 5)
  return MenuRepository.products.mapNotNull { p ->
   val names=listOf(p.name)+p.aliases
   val pos=names.map{normalized.indexOf(it.replace(" ",""))}.filter{it>=0}.minOrNull()?:return@mapNotNull null
   val tail=normalized.substring(pos).take(18)
   val q=Regex("""([1-9])\s*(個|つ|点)?""").find(tail)?.groupValues?.get(1)?.toIntOrNull()
      ?: tail.firstOrNull{kanji.containsKey(it)}?.let{kanji[it]} ?: 1
   ParsedOrder(p,q.coerceIn(1,9))
  }
 }
}
