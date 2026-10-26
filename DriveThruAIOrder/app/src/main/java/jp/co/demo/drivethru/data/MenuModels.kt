package jp.co.demo.drivethru.data

import androidx.annotation.DrawableRes

data class MenuProduct(val id:Int, val name:String, val aliases:List<String>, val price:Int, @DrawableRes val imageRes:Int)
data class CartLine(val product:MenuProduct, val quantity:Int)
