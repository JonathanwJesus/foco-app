package com.foco.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.foco.app.data.Subject

/** Paleta do Foco (os mesmos tokens do design system no Figma). */
object FocoColor {
    val Primary = Color(0xFF4F46E5)
    val PrimaryDark = Color(0xFF3730A3)
    val PrimarySoft = Color(0xFFEEF0FF)
    val Accent = Color(0xFFF59E0B)
    val AccentSoft = Color(0xFFFEF3C7)
    val Success = Color(0xFF16A34A)
    val SuccessSoft = Color(0xFFDCFCE7)
    val Danger = Color(0xFFDC2626)
    val DangerSoft = Color(0xFFFEE2E2)

    /** Versões mais escuras, para texto sobre os fundos suaves (contraste mínimo de 4,5:1). */
    val SuccessStrong = Color(0xFF15803D)
    val DangerStrong = Color(0xFFB91C1C)
    val Text = Color(0xFF111827)
    val TextSecondary = Color(0xFF4B5563)
    val TextMuted = Color(0xFF6B7280)
    val Border = Color(0xFFE5E7EB)
    val Surface = Color(0xFFFFFFFF)
    val Background = Color(0xFFF6F7FB)
}

/** Cor do texto/ícone de cada disciplina. */
val Subject.fg: Color
    get() = when (this) {
        Subject.CALCULO -> Color(0xFF1D4ED8)
        Subject.FISICA -> Color(0xFFC2410C)
        Subject.REDACAO -> Color(0xFFBE185D)
        Subject.HISTORIA -> Color(0xFF047857)
    }

/** Cor de fundo (tom suave) de cada disciplina. */
val Subject.bg: Color
    get() = when (this) {
        Subject.CALCULO -> Color(0xFFDBEAFE)
        Subject.FISICA -> Color(0xFFFFEDD5)
        Subject.REDACAO -> Color(0xFFFCE7F3)
        Subject.HISTORIA -> Color(0xFFD1FAE5)
    }

/**
 * Estilos de texto do design system. A fonte do protótipo é a Plus Jakarta Sans;
 * aqui usamos a fonte padrão do sistema. Para trocar, coloque o arquivo .ttf em
 * res/font e troque o valor de [Family] (veja o README).
 */
object FocoText {
    private val Family: FontFamily = FontFamily.SansSerif

    val Display32 = TextStyle(fontFamily = Family, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 40.sp)
    val Title24 = TextStyle(fontFamily = Family, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp)
    val Heading20 = TextStyle(fontFamily = Family, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp)
    val Subheading16 = TextStyle(fontFamily = Family, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp)
    val Body16 = TextStyle(fontFamily = Family, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp)
    val Body14 = TextStyle(fontFamily = Family, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
    val Label14 = TextStyle(fontFamily = Family, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp)
    val Caption12 = TextStyle(fontFamily = Family, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
    val Button16 = TextStyle(fontFamily = Family, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp)
    val Number48 = TextStyle(fontFamily = Family, fontWeight = FontWeight.ExtraBold, fontSize = 48.sp, lineHeight = 56.sp)
}

private val FocoColorScheme = lightColorScheme(
    primary = FocoColor.Primary,
    onPrimary = Color.White,
    primaryContainer = FocoColor.PrimarySoft,
    onPrimaryContainer = FocoColor.PrimaryDark,
    background = FocoColor.Background,
    onBackground = FocoColor.Text,
    surface = FocoColor.Surface,
    onSurface = FocoColor.Text,
    surfaceVariant = FocoColor.Background,
    onSurfaceVariant = FocoColor.TextSecondary,
    outline = FocoColor.Border,
    error = FocoColor.Danger,
)

@Composable
fun FocoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = FocoColorScheme, content = content)
}
