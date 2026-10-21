package ai.copilotdrive.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.copilotdrive.core.OperationMode

object Cockpit {
    val Background = Color(0xFF090F14)
    val Panel = Color(0xFF131D25)
    val Raised = Color(0xFF1B2933)
    val VoiceSurface = Color(0xFF102A25)
    val Border = Color(0xFF34444F)
    val Ink = Color(0xFFF4F2EB)
    val Mint = Color(0xFF91DEBC)
    val Muted = Color(0xFFA6B4BE)
    val Warning = Color(0xFFF0BF77)
    val Error = Color(0xFFFFA69E)
    val SpaceSmall = 8.dp
    val SpaceMedium = 16.dp
    val SpaceLarge = 24.dp
    val PanelShape = RoundedCornerShape(16.dp)
    val ControlShape = RoundedCornerShape(12.dp)
    val VoiceShape = RoundedCornerShape(24.dp)
    val Typography = Typography(
        displayMedium = TextStyle(fontSize = 46.sp, lineHeight = 50.sp, fontWeight = FontWeight.Light, letterSpacing = (-1).sp),
        headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.6).sp),
        headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Medium),
        titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
        labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
        labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold),
    )
    val Numbers = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 36.sp, lineHeight = 42.sp, letterSpacing = (-1).sp)
}

/** Decorative, original vector marks. Visible adjacent labels provide semantics. */
enum class CockpitSymbol { VEHICLE, VOICE, ROUTE, AUDIO, SYSTEM }

enum class PanelRole { STANDARD, VEHICLE, VOICE }

@Composable
fun CockpitMark(symbol: CockpitSymbol, modifier: Modifier = Modifier, tint: Color = Mint) {
    Canvas(modifier.size(28.dp).clearAndSetSemantics {}) {
        val w = size.width; val h = size.height; val stroke = Stroke(1.6.dp.toPx())
        when (symbol) {
            CockpitSymbol.VEHICLE -> {
                val body = Path().apply {
                    moveTo(w * .12f, h * .66f); lineTo(w * .18f, h * .44f); lineTo(w * .31f, h * .38f)
                    lineTo(w * .40f, h * .23f); lineTo(w * .68f, h * .23f); lineTo(w * .80f, h * .43f)
                    lineTo(w * .91f, h * .49f); lineTo(w * .91f, h * .66f); close()
                }
                drawPath(body, tint, style = stroke)
                drawCircle(tint, w * .075f, Offset(w * .31f, h * .68f), style = stroke)
                drawCircle(tint, w * .075f, Offset(w * .75f, h * .68f), style = stroke)
            }
            CockpitSymbol.VOICE -> {
                drawRoundRect(tint, Offset(w * .37f, h * .12f), androidx.compose.ui.geometry.Size(w * .26f, h * .46f), CornerRadius(w * .13f), style = stroke)
                drawArc(tint, 0f, 180f, false, Offset(w * .24f, h * .29f), androidx.compose.ui.geometry.Size(w * .52f, h * .43f), style = stroke)
                drawLine(tint, Offset(w * .5f, h * .72f), Offset(w * .5f, h * .89f), stroke.width)
                drawLine(tint, Offset(w * .34f, h * .89f), Offset(w * .66f, h * .89f), stroke.width)
            }
            CockpitSymbol.ROUTE -> {
                val path = Path().apply { moveTo(w * .2f, h * .8f); lineTo(w * .44f, h * .26f); lineTo(w * .8f, h * .8f); lineTo(w * .48f, h * .65f); close() }
                drawPath(path, tint, style = stroke)
            }
            CockpitSymbol.AUDIO -> {
                listOf(.3f, .6f, .9f, .5f, .25f).forEachIndexed { i, bar ->
                    val x = w * (.16f + i * .17f)
                    drawLine(tint, Offset(x, h * (.5f - bar * .4f)), Offset(x, h * (.5f + bar * .4f)), stroke.width)
                }
            }
            CockpitSymbol.SYSTEM -> {
                drawCircle(tint, w * .32f, center, style = stroke)
                drawCircle(tint, w * .07f, center)
            }
        }
    }
}

@Composable
fun ModeBadge(mode: OperationMode, modifier: Modifier = Modifier) {
    val demo = mode == OperationMode.DEMO
    Surface(modifier, color = if (demo) Color(0xFF342B20) else Color(0xFF18362C), shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(Modifier.size(5.dp).background(if (demo) Amber else Mint, CircleShape))
            Text(if (demo) "DEMO · FICTICIO" else "REAL", style = MaterialTheme.typography.labelSmall, color = if (demo) Amber else Mint)
        }
    }
}

@Composable
fun ScreenHeading(kicker: String, title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Eyebrow("COPILOTDRIVE AI / $kicker")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f).semantics { heading() })
            OutlinedButton(onClick = onBack, shape = Cockpit.ControlShape, modifier = Modifier.heightIn(min = 48.dp)) { Text("Volver") }
        }
        HorizontalDivider(color = Cockpit.Border, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
    }
}

@Composable
fun EmptyState(title: String, description: String, symbol: CockpitSymbol, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.size(48.dp).background(Cockpit.Raised, Cockpit.ControlShape), contentAlignment = Alignment.Center) { CockpitMark(symbol) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(description, color = Muted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
