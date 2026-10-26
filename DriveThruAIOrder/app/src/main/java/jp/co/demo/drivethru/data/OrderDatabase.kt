package jp.co.demo.drivethru.data

import android.content.Context
import androidx.room.*

@Entity(tableName="orders") data class OrderEntity(@PrimaryKey(autoGenerate=true) val id:Long=0,val orderedAt:Long,val totalYen:Int,val summary:String)
@Dao interface OrderDao { @Insert suspend fun insert(order:OrderEntity):Long; @Query("SELECT * FROM orders ORDER BY orderedAt DESC") suspend fun recent():List<OrderEntity> }
@Database(entities=[OrderEntity::class],version=1,exportSchema=false)
abstract class OrderDatabase:RoomDatabase(){abstract fun orderDao():OrderDao
 companion object { @Volatile private var instance:OrderDatabase?=null
  fun get(context:Context)=instance?:synchronized(this){instance?:Room.databaseBuilder(context.applicationContext,OrderDatabase::class.java,"drive_thru_orders.db").build().also{instance=it}}
 }
}
