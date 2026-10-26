package jp.g510.zramcompare
import android.content.*
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
class BaselineProvider:ContentProvider(){override fun onCreate()=true
 override fun query(u:Uri,p:Array<out String>?,s:String?,a:Array<out String>?,o:String?):Cursor{val x=context!!.getSharedPreferences("off",0);return MatrixCursor(arrayOf("avail","used","workers")).apply{if(x.contains("a"))addRow(arrayOf(x.getLong("a",0),x.getLong("u",0),x.getInt("w",0)))}}
 override fun getType(u:Uri)="vnd.android.cursor.item/g510-baseline";override fun insert(u:Uri,v:ContentValues?)=null;override fun delete(u:Uri,s:String?,a:Array<out String>?)=0;override fun update(u:Uri,v:ContentValues?,s:String?,a:Array<out String>?)=0}
