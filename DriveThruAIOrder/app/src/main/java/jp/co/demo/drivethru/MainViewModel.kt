package jp.co.demo.drivethru

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import jp.co.demo.drivethru.data.CartLine
import jp.co.demo.drivethru.data.MenuProduct
import jp.co.demo.drivethru.data.MenuRepository
import jp.co.demo.drivethru.data.OrderDatabase
import jp.co.demo.drivethru.data.OrderEntity
import jp.co.demo.drivethru.order.OrderParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OrderState {
 WAITING,
 ORDERING,
 CONFIRMING,
 COMPLETED
}

data class UiState(
 val state: OrderState = OrderState.WAITING,
 val heard: String = "「Hi」と話しかけてください",
 val message: String = "音声トリガー待機中",
 val cart: List<CartLine> = emptyList(),
 val orderNumber: Long? = null,
 val listening: Boolean = false
)

class MainViewModel(
 app: Application
) : AndroidViewModel(app) {

 private val db = OrderDatabase.get(app)

 private val _ui = MutableStateFlow(UiState())

 val ui: StateFlow<UiState> = _ui.asStateFlow()

 val products = MenuRepository.products

 fun listening(value: Boolean) {
  _ui.update {
   it.copy(listening = value)
  }
 }

 /**
  * Google SpeechRecognizerの認識結果を処理します。
  *
  * 戻り値はTTSで読み上げる文章です。
  */
 fun voice(text: String): String? {

  /*
   * Google音声認識結果を画面へ表示します。
   */
  _ui.update {
   it.copy(heard = text)
  }

  /*
   * 比較用に小文字化し、余分な空白を除去します。
   */
  val lower = text
   .trim()
   .lowercase()
   .replace("　", "")
   .replace(" ", "")

  /*
   * 注文開始前のトリガー判定です。
   *
   * Google音声認識は「Hi」を、
   * hi、ハイ、はいなどで返す可能性があるため、
   * 待機中だけ各表記を許可します。
   */
  if (_ui.value.state == OrderState.WAITING) {

   val wakeDetected =
    lower.contains("hi") ||
            lower.contains("ハイ") ||
            lower.contains("はい")

   if (wakeDetected) {

    val reply = "いらっしゃいませ。ご注文をどうぞ"

    _ui.update {
     it.copy(
      state = OrderState.ORDERING,
      message = reply
     )
    }

    return reply
   }

   /*
    * 待機中にトリガー以外の言葉を認識しても、
    * 商品注文として処理しません。
    */
   return null
  }

  /*
   * 注文内容を確認している状態です。
   *
   * ここでは「はい」「いいえ」だけを受け付けます。
   * その他の認識結果は必ず無視します。
   *
   * これによりTTSの確認音声を
   * 「ホットコーヒー」などと誤認識しても、
   * OrderParserへ進まず、商品は追加されません。
   */
  if (_ui.value.state == OrderState.CONFIRMING) {

   if (
    lower.contains("はい") ||
    lower.contains("確定") ||
    lower.contains("お願いします")
   ) {
    complete()

    return "ご注文ありがとうございました。受取口へお進みください"
   }

   if (
    lower.contains("いいえ") ||
    lower.contains("違") ||
    lower.contains("訂正") ||
    lower.contains("修正")
   ) {
    val reply = "訂正内容をどうぞ"

    _ui.update {
     it.copy(
      state = OrderState.ORDERING,
      message = reply
     )
    }

    return reply
   }

   /*
    * 重要な修正箇所です。
    *
    * 「はい」「いいえ」以外の場合、
    * この位置で処理を終了します。
    *
    * 元コードではこのreturnがなかったため、
    * OrderParserまで処理が進み、
    * ホットコーヒーが追加されていました。
    */
   return null
  }

  /*
   * 注文完了後は音声認識結果を無視します。
   */
  if (_ui.value.state == OrderState.COMPLETED) {
   return null
  }

  /*
   * 以下はORDERING状態で実行されます。
   */

  /*
   * 注文を最初からやり直します。
   */
  if (
   lower.contains("キャンセル") ||
   lower.contains("最初から")
  ) {
   val reply = "注文を取り消しました。ご注文をどうぞ"

   _ui.update {
    UiState(
     state = OrderState.ORDERING,
     heard = text,
     message = reply,
     cart = emptyList(),
     orderNumber = null,
     listening = it.listening
    )
   }

   return reply
  }

  /*
   * 「以上です」を商品解析より先に判定します。
   */
  if (
   lower.contains("以上") ||
   lower.contains("会計") ||
   lower.contains("注文終了")
  ) {
   return confirm()
  }

  /*
   * 商品名と数量を解析します。
   */
  val parsed = OrderParser.parse(text)

  if (parsed.isEmpty()) {

   _ui.update {
    it.copy(
     message = "商品を確認できませんでした。もう一度お願いします"
    )
   }

   return "商品名をもう一度お願いします"
  }

  /*
   * 認識した商品をカートへ追加します。
   */
  parsed.forEach {
   add(
    product = it.product,
    quantity = it.quantity
   )
  }

  val names = parsed.joinToString("、") {
   "${it.product.name}${it.quantity}個"
  }

  val reply = "$names を追加しました"

  _ui.update {
   it.copy(message = reply)
  }

  return reply
 }

 /**
  * 商品をカートに追加します。
  *
  * 元ソースと同じ動作へ戻しています。
  * 画面タッチからでも音声注文からでも追加できます。
  */
 fun add(
  product: MenuProduct,
  quantity: Int = 1
 ) {
  _ui.update { current ->

   val cartMap = current.cart
    .associateBy { it.product.id }
    .toMutableMap()

   val oldLine = cartMap[product.id]

   cartMap[product.id] = CartLine(
    product = product,
    quantity = (oldLine?.quantity ?: 0) + quantity
   )

   current.copy(
    cart = cartMap.values.toList(),
    state = OrderState.ORDERING
   )
  }
 }

 /**
  * 商品をカートから削除します。
  */
 fun remove(product: MenuProduct) {
  _ui.update {
   it.copy(
    cart = it.cart.filterNot { line ->
     line.product.id == product.id
    }
   )
  }
 }

 /**
  * 注文内容を確認します。
  */
 fun confirm(): String {

  val current = _ui.value

  if (current.cart.isEmpty()) {

   val reply = "商品が選ばれていません。ご注文をどうぞ"

   _ui.update {
    it.copy(message = reply)
   }

   return reply
  }

  val total = current.cart.sumOf {
   it.product.price * it.quantity
  }

  val summary = current.cart.joinToString("、") {
   "${it.product.name}${it.quantity}個"
  }

  val reply =
   "$summary、合計${total}円です。よろしいですか"

  /*
   * 「以上です」を認識した時点でCONFIRMINGへ移行します。
   *
   * 以降の認識結果はvoice()のCONFIRMING処理へ入り、
   * 商品解析へ進まなくなります。
   */
  _ui.update {
   it.copy(
    state = OrderState.CONFIRMING,
    message = reply
   )
  }

  return reply
 }

 /**
  * 注文をSQLiteへ保存します。
  */
 private fun complete() {

  val current = _ui.value

  viewModelScope.launch {

   val summary = current.cart.joinToString(" / ") {
    "${it.product.name} x${it.quantity}"
   }

   val total = current.cart.sumOf {
    it.product.price * it.quantity
   }

   val orderId = db.orderDao().insert(
    OrderEntity(
     orderedAt = System.currentTimeMillis(),
     totalYen = total,
     summary = summary
    )
   )

   _ui.update {
    it.copy(
     state = OrderState.COMPLETED,
     orderNumber = orderId,
     message = "注文完了"
    )
   }
  }
 }

 /**
  * 次のお客様向けに初期化します。
  */
 fun reset() {
  _ui.update {
   UiState(
    listening = it.listening
   )
  }
 }
}