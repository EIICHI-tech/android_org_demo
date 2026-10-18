package jp.eiichi.worldphotojourney

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class JourneyPhoto(val uri:Uri,val location:LatLng?,val takenAt:Long?,val dateText:String,val fileName:String)

class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{JourneyTheme{JourneyApp()}}}}
@Composable fun JourneyTheme(content: @Composable () -> Unit){MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF42D3FF),secondary=Color(0xFFFFC86B),background=Color(0xFF071422),surface=Color(0xFF102236)),content=content)}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun JourneyApp(){
    val context=androidx.compose.ui.platform.LocalContext.current
    val scope=rememberCoroutineScope()
    var photos by remember{mutableStateOf(emptyList<JourneyPhoto>())}
    var index by remember{mutableIntStateOf(0)}
    var loading by remember{mutableStateOf(false)}
    var playing by remember{mutableStateOf(false)}
    var status by remember{mutableStateOf("写真を複数選択してください")}
    val camera=rememberCameraPositionState{position=CameraPosition.fromLatLngZoom(LatLng(20.0,0.0),1.5f)}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){uris->
        if(uris.isNotEmpty()){
            uris.forEach{runCatching{context.contentResolver.takePersistableUriPermission(it,Intent.FLAG_GRANT_READ_URI_PERMISSION)}}
            loading=true;status="EXIFの撮影日時とGPSを読取中…"
            scope.launch{photos=readPhotos(context,uris);index=0;loading=false;status="${photos.size}枚を読込、位置情報あり ${photos.count{it.location!=null}}枚";fitAll(camera,photos.mapNotNull{it.location})}
        }
    }
    LaunchedEffect(playing,photos){while(playing&&photos.isNotEmpty()){delay(2500);index=(index+1)%photos.size}}
    LaunchedEffect(index,photos){photos.getOrNull(index)?.location?.let{camera.animate(CameraUpdateFactory.newLatLngZoom(it,6f),800)}}
    val route=photos.mapNotNull{it.location}
    Scaffold(topBar={TopAppBar(title={Column{Text("世界写真ジャーニー",fontWeight=FontWeight.Bold);Text("縦画面版",style=MaterialTheme.typography.labelSmall)}},actions={TextButton(onClick={picker.launch(arrayOf("image/jpeg","image/heic","image/heif","image/webp"))},enabled=!loading){Icon(Icons.Default.AddPhotoAlternate,null);Text("写真選択")}})}){pad->
        Column(Modifier.padding(pad).fillMaxSize().padding(horizontal=8.dp,vertical=6.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Card(Modifier.fillMaxWidth().weight(.43f),shape=RoundedCornerShape(16.dp)){
                Box(Modifier.fillMaxSize()){
                    GoogleMap(Modifier.fillMaxSize(),cameraPositionState=camera,properties=MapProperties(mapType=MapType.NORMAL),uiSettings=MapUiSettings(zoomControlsEnabled=true,compassEnabled=true)){
                        if(route.size>=2)Polyline(points=route,color=Color(0xFFFF5F57),width=9f,startCap=RoundCap(),endCap=RoundCap())
                        photos.forEachIndexed{i,p->p.location?.let{Marker(state=rememberMarkerState(position = it),title=p.fileName,snippet=p.dateText,onClick={index=i;false})}}
                    }
                    Surface(Modifier.align(Alignment.TopStart).padding(8.dp),shape=RoundedCornerShape(10.dp),color=Color(0xD9102236)){Text(status,Modifier.padding(10.dp),style=MaterialTheme.typography.labelMedium)}
                }
            }
            Card(Modifier.fillMaxWidth().weight(.57f),shape=RoundedCornerShape(16.dp)){
                if(photos.isEmpty())Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(if(loading)"EXIFを解析中…" else "写真を選択すると旅程を表示します")}
                else PortraitPhotoPanel(photos,index,{index=it},{playing=!playing},{scope.launch{fitAll(camera,route)}},playing)
            }
        }
    }
}

@Composable fun PortraitPhotoPanel(photos:List<JourneyPhoto>,index:Int,onIndex:(Int)->Unit,onPlay:()->Unit,onFit:()->Unit,playing:Boolean){
    val p=photos[index]
    Column(Modifier.fillMaxSize().padding(10.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
        Box(Modifier.fillMaxWidth().weight(1f).background(Color.Black,RoundedCornerShape(12.dp))){
            Image(rememberAsyncImagePainter(p.uri),null,Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
            Surface(Modifier.align(Alignment.BottomStart).padding(7.dp),color=Color(0xB0000000),shape=RoundedCornerShape(7.dp)){Text("${index+1} / ${photos.size}",Modifier.padding(7.dp))}
        }
        Text(p.fileName,fontWeight=FontWeight.Bold,maxLines=1)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(14.dp)){Info(Icons.Default.CalendarMonth,p.dateText);Info(Icons.Default.LocationOn,p.location?.let{"%.5f, %.5f".format(Locale.US,it.latitude,it.longitude)}?:"GPSなし")}
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically){
            IconButton({onIndex(if(index==0)photos.lastIndex else index-1)}){Icon(Icons.Default.SkipPrevious,null)}
            FilledTonalIconButton(onPlay){Icon(if(playing)Icons.Default.Pause else Icons.Default.PlayArrow,null)}
            IconButton({onIndex((index+1)%photos.size)}){Icon(Icons.Default.SkipNext,null)}
            OutlinedButton(onFit){Icon(Icons.Default.FitScreen,null);Text("全体")}
        }
    }
}
@Composable fun Info(icon:androidx.compose.ui.graphics.vector.ImageVector,text:String){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){Icon(icon,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.primary);Text(text,style=MaterialTheme.typography.bodySmall,maxLines=1)}}

suspend fun readPhotos(context:Context,uris:List<Uri>):List<JourneyPhoto> = withContext(Dispatchers.IO){
    uris.map{uri->runCatching{context.contentResolver.openInputStream(uri)!!.use{input->val exif=ExifInterface(input);val ll=exif.latLong;val time=exif.dateTimeOriginal?:exif.dateTime;val raw=exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?:exif.getAttribute(ExifInterface.TAG_DATETIME);JourneyPhoto(uri,ll?.let{LatLng(it[0],it[1])},time,raw?.replaceFirst(Regex("^(\\d{4}):(\\d{2}):(\\d{2})"),"$1-$2-$3")?:"撮影日時なし",queryName(context,uri))}}.getOrElse{JourneyPhoto(uri,null,null,"EXIF読取不可",queryName(context,uri))}}.sortedWith(compareBy<JourneyPhoto>{it.takenAt?:Long.MAX_VALUE}.thenBy{it.fileName})
}
fun queryName(context:Context,uri:Uri):String{context.contentResolver.query(uri,arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),null,null,null)?.use{if(it.moveToFirst())return it.getString(0)};return uri.lastPathSegment?:"photo"}
suspend fun fitAll(camera:CameraPositionState,points:List<LatLng>){if(points.isEmpty())return;if(points.size==1){camera.animate(CameraUpdateFactory.newLatLngZoom(points[0],7f),700);return};val b=LatLngBounds.builder();points.forEach{b.include(it)};runCatching{camera.animate(CameraUpdateFactory.newLatLngBounds(b.build(),90),900)}}
