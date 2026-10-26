package jp.co.demo.drivethru.data
import jp.co.demo.drivethru.R
object MenuRepository {
    val products = listOf(
        MenuProduct(1,"ダブルチーズバーガー",listOf("ダブルチーズ","ダブチ"),520,R.drawable.burger_double),
        MenuProduct(2,"チキンバーガー",listOf("チキン"),440,R.drawable.burger_chicken),
        MenuProduct(3,"ポテトM",listOf("ポテト","フライドポテト"),330,R.drawable.fries_m),
        MenuProduct(4,"ナゲット5個",listOf("ナゲット"),290,R.drawable.nuggets),
        MenuProduct(5,"コーラM",listOf("コーラ"),270,R.drawable.cola_m),
        MenuProduct(6,"ホットコーヒー",listOf("コーヒー"),220,R.drawable.coffee_m)
    )
}
