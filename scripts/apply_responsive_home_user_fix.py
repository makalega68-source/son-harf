from pathlib import Path

p = Path('app/src/main/java/com/sonharf/game/PremiumMasterHome.kt')
t = p.read_text(encoding='utf-8')

if 'val compact = maxWidth < 600.dp' in t:
    print('responsive home repair already applied')
    raise SystemExit(0)

def r(old: str, new: str, label: str):
    global t
    if old not in t:
        raise SystemExit(f'missing anchor: {label}')
    t = t.replace(old, new, 1)

r('''        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MasterSonHarfCard(
                    modifier = Modifier.weight(1.18f),
                    playerName = profile?.displayName ?: sh("Sen", "You"),
                    rivalName = leaders.firstOrNull()?.displayName ?: sh("Rakip", "Rival"),
                    streak = streak.coerceAtLeast(4),
                    onPlay = onQuickGame,
                )
                MasterBilBakalimCard(Modifier.weight(.82f), onBilBakalim)
            }
        }''', '''        item {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val compact = maxWidth < 600.dp
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MasterSonHarfCard(Modifier.fillMaxWidth(), profile?.displayName ?: sh("Sen", "You"), profile?.avatarPath, profile?.gender, leaders.firstOrNull()?.displayName ?: sh("Rakip", "Rival"), streak.coerceAtLeast(4), onQuickGame)
                        MasterBilBakalimCard(Modifier.fillMaxWidth(), onBilBakalim)
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MasterSonHarfCard(Modifier.weight(1.18f), profile?.displayName ?: sh("Sen", "You"), profile?.avatarPath, profile?.gender, leaders.firstOrNull()?.displayName ?: sh("Rakip", "Rival"), streak.coerceAtLeast(4), onQuickGame)
                        MasterBilBakalimCard(Modifier.weight(.82f), onBilBakalim)
                    }
                }
            }
        }''', 'adaptive cards')

r('@Composable private fun MasterSonHarfCard(modifier: Modifier, playerName:String, rivalName:String, streak:Int, onPlay:()->Unit) {',
  '@Composable private fun MasterSonHarfCard(modifier: Modifier, playerName:String, playerAvatarPath:String?, playerGender:String?, rivalName:String, streak:Int, onPlay:()->Unit) {',
  'arena signature')

r('PlayerVsCell(playerName, "2150", Modifier.weight(1f)); Text("VS", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black); PlayerVsCell(rivalName, "2186", Modifier.weight(1f))',
  'PlayerVsCell(playerName, "2150", Modifier.weight(1f), playerAvatarPath, playerGender); Text("VS", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black); PlayerVsCell(rivalName, "2186", Modifier.weight(1f))',
  'player photo use')

r('@Composable private fun PlayerVsCell(name:String,rating:String,modifier:Modifier){ Column(modifier,horizontalAlignment=Alignment.CenterHorizontally){Surface(shape=CircleShape,color=Color.White.copy(.20f),border=BorderStroke(2.dp,Color.White)){Box(Modifier.size(36.dp),contentAlignment=Alignment.Center){Text(name.take(1).uppercase(),color=Color.White,fontWeight=FontWeight.Black)}};Text(name,color=Color.White,fontWeight=FontWeight.Black,fontSize=9.sp,maxLines=1);Text("🏆 $rating",color=MasterGold,fontSize=8.sp)} }',
  '@Composable private fun PlayerVsCell(name:String,rating:String,modifier:Modifier,avatarPath:String?=null,gender:String?=null){ Column(modifier,horizontalAlignment=Alignment.CenterHorizontally){ if(!avatarPath.isNullOrBlank()) ProfilePhotoAvatarWithGender(avatarPath=avatarPath,gender=gender,name=name,size=40.dp,accent=Color.White) else Surface(shape=CircleShape,color=Color.White.copy(.20f),border=BorderStroke(2.dp,Color.White)){Box(Modifier.size(36.dp),contentAlignment=Alignment.Center){Text(name.take(1).uppercase(),color=Color.White,fontWeight=FontWeight.Black)}};Text(name,color=Color.White,fontWeight=FontWeight.Black,fontSize=9.sp,maxLines=1);Text("🏆 $rating",color=MasterGold,fontSize=8.sp)} }',
  'player cell')

r('Surface(Modifier.weight(.75f), shape=RoundedCornerShape(16.dp), color=Color(0xFF0B5AA6)) { Column(Modifier.padding(8.dp), horizontalAlignment=Alignment.CenterHorizontally){Text("🔥",fontSize=19.sp);Text("$streak ${sh("GALİBİYET", "WINS")}",color=Color.White,fontWeight=FontWeight.Black,fontSize=7.sp)} }',
  'Surface(Modifier.weight(.9f).height(54.dp), shape=RoundedCornerShape(16.dp), color=Color(0xFF0B5AA6)) { Column(Modifier.fillMaxSize().padding(horizontal=6.dp), horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text("🔥 $streak",fontSize=15.sp);Text(sh("GALİBİYET", "WINS"),color=Color.White,fontWeight=FontWeight.Black,fontSize=7.sp,maxLines=1,softWrap=false)} }',
  'streak')

r('Button(onClick=onPlay, modifier=Modifier.weight(1.65f).height(54.dp), shape=RoundedCornerShape(19.dp), colors=ButtonDefaults.buttonColors(containerColor=MasterBlue2)) { Icon(Icons.Rounded.PlayArrow,null); Spacer(Modifier.width(4.dp)); Text(sh("OYNA", "PLAY"),fontWeight=FontWeight.Black,fontSize=17.sp) }',
  'Button(onClick=onPlay, modifier=Modifier.weight(1.8f).height(54.dp), shape=RoundedCornerShape(19.dp), colors=ButtonDefaults.buttonColors(containerColor=MasterBlue2),contentPadding=PaddingValues(horizontal=12.dp)) { Icon(Icons.Rounded.PlayArrow,null); Spacer(Modifier.width(5.dp)); Text(sh("OYNA", "PLAY"),fontWeight=FontWeight.Black,fontSize=16.sp,maxLines=1,softWrap=false) }',
  'play')

r('Text("💡",fontSize=37.sp); Text("BİL",color=MasterInk,fontSize=31.sp,fontWeight=FontWeight.Black,lineHeight=28.sp);Text("BAKALIM",color=MasterInk,fontSize=31.sp,fontWeight=FontWeight.Black,lineHeight=30.sp)',
  'Text("💡",fontSize=35.sp); Text("BİL",color=MasterInk,fontSize=30.sp,fontWeight=FontWeight.Black,lineHeight=31.sp,maxLines=1,softWrap=false);Text("BAKALIM",color=MasterInk,fontSize=28.sp,fontWeight=FontWeight.Black,lineHeight=30.sp,maxLines=1,softWrap=false)',
  'bil title')

r('Text(sh("Doğru cevaba en yakın tahmin kazanır!", "Closest estimate wins!"),color=MasterInk,fontSize=8.sp,textAlign=TextAlign.Center)',
  'Text(sh("Doğru cevaba en yakın tahmin kazanır!", "Closest estimate wins!"),modifier=Modifier.fillMaxWidth().padding(horizontal=8.dp),color=MasterInk,fontSize=11.sp,lineHeight=15.sp,textAlign=TextAlign.Center,maxLines=2)',
  'bil description')

r('Button(onClick=onPlay,modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.buttonColors(containerColor=MasterBlue)){Icon(Icons.Rounded.PlayArrow,null);Text(sh(" HEMEN OYNA", " PLAY NOW"),fontWeight=FontWeight.Black,fontSize=13.sp)}',
  'Button(onClick=onPlay,modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.buttonColors(containerColor=MasterBlue),contentPadding=PaddingValues(horizontal=12.dp)){Icon(Icons.Rounded.PlayArrow,null);Spacer(Modifier.width(5.dp));Text(sh("HEMEN OYNA", "PLAY NOW"),fontWeight=FontWeight.Black,fontSize=13.sp,maxLines=1,softWrap=false)}',
  'bil button')

p.write_text(t, encoding='utf-8')
print('responsive home repair applied')
