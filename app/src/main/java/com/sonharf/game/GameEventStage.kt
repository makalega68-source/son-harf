package com.sonharf.game

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val EventGold = Color(0xFFFFD77A)
internal val EventInk = Color(0xFF382B1A)
internal val EventGreen = Color(0xFF226B49)

/** A sculpted event panel: recessed rim, stage lighting and a restrained travelling glint. */
@Composable internal fun GameEventStage(
 modifier:Modifier=Modifier,
 gold:Boolean=false,
 onClick:(()->Unit)?=null,
 content:@Composable ColumnScope.()->Unit,
) {
 val shine=rememberInfiniteTransition(label="event-light").animateFloat(0f,1f,infiniteRepeatable(tween(5200,easing=LinearEasing)),label="event-glint")
 val shape=RoundedCornerShape(26.dp)
 Box(modifier.fillMaxWidth().shadow(5.dp,shape).clip(shape)
  .background(Brush.linearGradient(if(gold)listOf(Color(0xFFFFE8A2),Color(0xFFE6B65D),Color(0xFFC68B36))else listOf(Color(0xFF3D9765),EventGreen,Color(0xFF173F32))))
  .border(1.5.dp,if(gold)Color(0xFFFFF2C9)else EventGold.copy(alpha=.65f),shape)
  .then(if(onClick!=null)Modifier.clickable(onClick=onClick)else Modifier)) {
  Canvas(Modifier.matchParentSize()) {
   drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha=.16f),Color.Transparent),center=Offset(size.width*.8f,0f),radius=size.width*.9f),size.width*.9f,Offset(size.width*.8f,0f))
   val p=shine.value
   val x=(size.width+size.height)*p-size.height
   drawLine(Color.White.copy(alpha=.08f),Offset(x,0f),Offset(x+size.height,size.height),size.width*.13f)
   repeat(8){i->val sx=size.width*(.1f+.11f*i);val sy=size.height*(.12f+((i*17)%7)*.11f)
    val a=(.18f+.15f*kotlin.math.sin((p*6.28f+i).toDouble()).toFloat()).coerceIn(0f,1f)
    drawLine(EventGold.copy(alpha=a),Offset(sx-3.dp.toPx(),sy),Offset(sx+3.dp.toPx(),sy),1.dp.toPx())
    drawLine(EventGold.copy(alpha=a),Offset(sx,sy-3.dp.toPx()),Offset(sx,sy+3.dp.toPx()),1.dp.toPx())
   }
  }
  Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)
 }
}

@Composable internal fun EventTag(text:String,gold:Boolean=true) {
 Text(text,Modifier.clip(RoundedCornerShape(7.dp)).background(if(gold)EventGold else Color.White.copy(alpha=.14f)).padding(horizontal=9.dp,vertical=5.dp),
  color=if(gold)EventInk else Color.White,fontSize=10.sp,fontWeight=FontWeight.Black,letterSpacing=1.sp,maxLines=1)
}

@Composable internal fun EventAction(text:String,onClick:()->Unit,enabled:Boolean=true,modifier:Modifier=Modifier) {
 val press=remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
 val pressed by press.collectIsPressedAsState()
 val scale by animateFloatAsState(if(pressed).97f else 1f,tween(110),label="event-press")
 Box(modifier.fillMaxWidth().heightIn(min=52.dp).graphicsLayer{scaleX=scale;scaleY=scale}.shadow(if(enabled)3.dp else 0.dp,RoundedCornerShape(15.dp))
  .clip(RoundedCornerShape(15.dp)).background(Brush.verticalGradient(if(enabled)listOf(Color(0xFFFFE8A8),EventGold,Color(0xFFEAB550))else listOf(Color(0xFFE7DECA),Color(0xFFD7CFBC))))
  .border(1.dp,Color.White.copy(alpha=.6f),RoundedCornerShape(15.dp)).clickable(interactionSource=press,indication=null,enabled=enabled){SonHarfSoundFx.tap();onClick()}.padding(horizontal=12.dp,vertical=12.dp),contentAlignment=Alignment.Center) {
  Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
   Icon(if(enabled)Icons.Rounded.PlayArrow else Icons.Rounded.Schedule,null,tint=EventInk,modifier=Modifier.size(22.dp))
   Text(text,color=EventInk,fontWeight=FontWeight.Black,fontSize=14.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
  }
 }
}

/** Digits move only once a second; decorative motion is read in draw/layer phases. */
@Composable internal fun EventCountdown(value:String) {
 val parts=value.split(":").takeLast(3)
 Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
  parts.forEachIndexed { index,part ->
   if(index>0)Text(":",color=EventGold,fontSize=23.sp,fontWeight=FontWeight.Black)
   Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(3.dp)) {
    Box(Modifier.widthIn(min=44.dp).height(52.dp).shadow(2.dp,RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp))
     .background(Brush.verticalGradient(listOf(Color(0xFFFFF5CE),EventGold))).border(1.dp,Color.White.copy(alpha=.6f),RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center) {
     AnimatedContent(targetState=part,label="countdown-digit") { digit->Text(digit,color=EventInk,fontSize=26.sp,fontWeight=FontWeight.Black,fontFamily=FontFamily.Monospace) }
    }
    Text(listOf(sh("SAAT","HOURS"),sh("DAKİKA","MINUTES"),sh("SANİYE","SECONDS")).getOrElse(index){""},color=Color.White.copy(alpha=.8f),fontSize=8.sp,fontWeight=FontWeight.Bold,letterSpacing=.7.sp)
   }
  }
 }
}

/** Crown silhouette, upholstered back, arms and raised plinth; all geometry stays off the labels. */
@Composable internal fun ThroneSeat(modifier:Modifier=Modifier,portrait:@Composable ()->Unit) {
 Box(modifier.width(174.dp).height(178.dp),contentAlignment=Alignment.Center) {
  Canvas(Modifier.matchParentSize()) {
   val w=size.width;val h=size.height
   drawOval(Color(0xFF6D451C).copy(alpha=.22f),Offset(w*.04f,h*.86f),androidx.compose.ui.geometry.Size(w*.92f,h*.12f))
   drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFFE6A1),Color(0xFFB1792D))),Offset(w*.19f,h*.19f),androidx.compose.ui.geometry.Size(w*.62f,h*.65f),androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()))
   drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFA84539),Color(0xFF622A28))),Offset(w*.24f,h*.23f),androidx.compose.ui.geometry.Size(w*.52f,h*.55f),androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()))
   val crown=Path().apply{moveTo(w*.32f,h*.2f);lineTo(w*.27f,h*.04f);lineTo(w*.43f,h*.11f);lineTo(w*.5f,0f);lineTo(w*.57f,h*.11f);lineTo(w*.73f,h*.04f);lineTo(w*.68f,h*.2f);close()}
   drawPath(crown,Brush.verticalGradient(listOf(Color(0xFFFFF0BA),Color(0xFFE4A846))))
   listOf(.1f,.76f).forEach{x->drawRoundRect(Brush.verticalGradient(listOf(EventGold,Color(0xFFB88230))),Offset(w*x,h*.58f),androidx.compose.ui.geometry.Size(w*.14f,h*.29f),androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))}
   drawRoundRect(Brush.verticalGradient(listOf(EventGold,Color(0xFFBC8333))),Offset(w*.18f,h*.8f),androidx.compose.ui.geometry.Size(w*.64f,h*.1f),androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))
  }
  Box(Modifier.padding(top=12.dp),contentAlignment=Alignment.Center){portrait()}
 }
}
