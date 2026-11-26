package com.kelompok10.canteencare.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kelompok10.canteencare.R

val MontserratFontFamily = FontFamily(
    Font(R.font.montserrat, FontWeight.Normal)
)

private val Base = Typography()

/** Hierarki teks: judul tebal dan jelas, isi teks nyaman dibaca. */
val Typography = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = MontserratFontFamily),
    displayMedium = Base.displayMedium.copy(fontFamily = MontserratFontFamily),
    displaySmall = Base.displaySmall.copy(fontFamily = MontserratFontFamily),
    headlineLarge = Base.headlineLarge.copy(fontFamily = MontserratFontFamily),
    headlineMedium = Base.headlineMedium.copy(fontFamily = MontserratFontFamily),
    headlineSmall = Base.headlineSmall.copy(fontFamily = MontserratFontFamily, fontWeight = FontWeight.Bold),
    titleLarge = Base.titleLarge.copy(fontFamily = MontserratFontFamily, fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontFamily = MontserratFontFamily, fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall.copy(fontFamily = MontserratFontFamily),
    bodyLarge = TextStyle(fontFamily = MontserratFontFamily, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = Base.bodyMedium.copy(fontFamily = MontserratFontFamily),
    bodySmall = Base.bodySmall.copy(fontFamily = MontserratFontFamily),
    labelLarge = Base.labelLarge.copy(fontFamily = MontserratFontFamily, fontWeight = FontWeight.SemiBold),
    labelMedium = Base.labelMedium.copy(fontFamily = MontserratFontFamily),
    labelSmall = Base.labelSmall.copy(fontFamily = MontserratFontFamily),
)

/** Sudut kartu dan tombol yang membulat. */
val Shapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)
