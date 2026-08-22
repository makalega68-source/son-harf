from pathlib import Path

mini = Path('app/src/main/java/com/sonharf/game/MiniMascot3D.kt')
m = mini.read_text()

def once(text, old, new, label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'missing anchor: {label}')
    return text.replace(old, new, 1)

m = once(m,
'''import androidx.compose.runtime.Composable''',
'''import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable''',
'animation imports')
m = once(m,
'''import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView''',
'''import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.viewinterop.AndroidView''',
'graphicsLayer import')
m = once(m,
'''    var renderer by remember { mutableStateOf<MiniRenderer?>(null) }
    AndroidView(
        modifier = modifier,''',
'''    var renderer by remember { mutableStateOf<MiniRenderer?>(null) }
    val orbit = remember { Animatable(0f) }
    LaunchedEffect(mood) {
        orbit.snapTo(0f)
        if (mood == MiniMood.HAPPY || mood == MiniMood.STREAK) {
            orbit.animateTo(1f, tween(if (mood == MiniMood.STREAK) 1900 else 1450))
        }
    }
    val phase = orbit.value * (PI.toFloat() * 2f)
    AndroidView(
        modifier = modifier.graphicsLayer {
            translationX = if (mood == MiniMood.HAPPY || mood == MiniMood.STREAK) sin(phase) * 72f else 0f
            translationY = if (mood == MiniMood.HAPPY || mood == MiniMood.STREAK) cos(phase) * 30f - 30f else 0f
            scaleX = if (mood == MiniMood.STREAK) 1.10f else 1f
            scaleY = if (mood == MiniMood.STREAK) 1.10f else 1f
        },''',
'orbit motion')
mini.write_text(m)

arena = Path('app/src/main/java/com/sonharf/game/SketchGameOverlayV10.kt')
a = arena.read_text()
a = once(a,
'''    var seconds by remember(room.turnDeadline) { mutableIntStateOf(45) }
    val timerScale by animateFloatAsState(''',
'''    var seconds by remember(room.turnDeadline) { mutableIntStateOf(45) }
    val mascotMood = when {
        triviaFeedback?.first == true -> MiniMood.COLLECT
        feedback?.correct == true && myStreak >= 3 -> MiniMood.STREAK
        feedback?.correct == true -> MiniMood.HAPPY
        feedback != null -> MiniMood.SAD
        seconds in 1..5 -> MiniMood.CUTE
        else -> MiniMood.IDLE
    }
    val timerScale by animateFloatAsState(''',
'mascot mood mapping')
a = once(a,
'''            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().padding(13.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {''',
'''            Box(Modifier.fillMaxSize()) {
                MiniMascot3D(
                    mood = mascotMood,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = 6.dp, y = 50.dp)
                        .size(112.dp),
                )
                Column(Modifier.fillMaxSize().padding(13.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {''',
'mascot arena placement')
arena.write_text(a)
print('Mini 3D integration applied')
