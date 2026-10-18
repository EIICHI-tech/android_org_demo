package com.kagafei.g510sushimenu;
import android.app.*; import android.os.*; import android.graphics.Color; import android.graphics.drawable.GradientDrawable; import android.view.*; import android.widget.*; import java.util.*;
public class MainActivity extends Activity {
 static class Item {String n; int p; Item(String n,int p){this.n=n;this.p=p;}}
 final Map<String,Item[]> menus=new LinkedHashMap<>(); final LinkedHashMap<String,Integer> cart=new LinkedHashMap<>(); GridLayout grid; TextView cartView,total;
 @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_main);grid=findViewById(R.id.menuGrid);cartView=findViewById(R.id.cart);total=findViewById(R.id.total);
  menus.put("ドリンク",new Item[]{new Item("緑茶",150),new Item("ウーロン茶",180),new Item("コーラ",180),new Item("オレンジジュース",180),new Item("生ビール",520),new Item("ハイボール",420)});
  menus.put("寿司",new Item[]{new Item("まぐろ",180),new Item("サーモン",180),new Item("えび",180),new Item("いか",160),new Item("大とろ",480),new Item("いくら軍艦",320)});
  menus.put("オプションメニュー",new Item[]{new Item("茶碗蒸し",280),new Item("枝豆",220),new Item("唐揚げ",380),new Item("味噌汁",220),new Item("うどん",420),new Item("デザート",320)});
  findViewById(R.id.tabDrink).setOnClickListener(v->showMenu("ドリンク"));findViewById(R.id.tabSushi).setOnClickListener(v->showMenu("寿司"));findViewById(R.id.tabOption).setOnClickListener(v->showMenu("オプションメニュー"));
  findViewById(R.id.clear).setOnClickListener(v->{cart.clear();refresh();});findViewById(R.id.order).setOnClickListener(v->new AlertDialog.Builder(this).setTitle("注文完了").setMessage("ご注文を受け付けました。").setPositiveButton("OK",null).show()); showMenu("ドリンク"); }
 void showMenu(String cat){grid.removeAllViews();for(Item it:menus.get(cat)){Button b=new Button(this);b.setText(it.n+"\n"+it.p+"円\n＋ 追加");b.setTextSize(18);b.setAllCaps(false);GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=0;lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.rowSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.setMargins(8,8,8,8);b.setLayoutParams(lp);b.setOnClickListener(v->{cart.put(it.n,cart.getOrDefault(it.n,0)+1);refresh();});grid.addView(b);} }
 void refresh(){if(cart.isEmpty()){cartView.setText("まだ商品はありません");total.setText("合計 0円");return;}StringBuilder s=new StringBuilder();int sum=0;for(Map.Entry<String,Integer> e:cart.entrySet()){Item found=null;for(Item[] a:menus.values())for(Item i:a)if(i.n.equals(e.getKey()))found=i;s.append(e.getKey()).append("  × ").append(e.getValue()).append("\n");sum+=found.p*e.getValue();}cartView.setText(s.toString());total.setText("合計 "+sum+"円");}
}
