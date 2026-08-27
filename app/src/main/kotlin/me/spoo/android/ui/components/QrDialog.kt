package me.spoo.android.ui.components

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import me.spoo.android.R
import java.io.File

/**
 * QR for a short URL in the qr.spoo.me language: circle modules, rounded
 * finder eyes, the ghost mark carved into the center (error correction H
 * pays for the hole). Always dark-on-white: scanners beat theming.
 */
@Composable
fun QrDialog(
    shortUrl: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val bitmap = remember(shortUrl) { qrBitmap(context, "https://$shortUrl") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "QR code for https://$shortUrl",
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .padding(14.dp),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.High,
                )
                Spacer(Modifier.height(18.dp))
                EmojiText(
                    shortUrl,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) { Text("Done") }
                    Spacer(Modifier.weight(1f))
                    // Outlined, not tonal: medium-contrast dark turns
                    // secondaryContainer muddy; the hairline stays crisp.
                    OutlinedButton(
                        onClick = {
                            val ok = saveQr(context, bitmap, shortUrl)
                            Toast
                                .makeText(
                                    context,
                                    if (ok) "Saved to Pictures/spoo" else "Couldn't save",
                                    Toast.LENGTH_SHORT,
                                ).show()
                        },
                    ) { Text("Save") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { shareQr(context, bitmap, shortUrl) }) {
                        Text("Share")
                    }
                }
            }
        }
    }
}

/** Hands the rendered PNG to the share sheet via the cache FileProvider. */
private fun shareQr(
    context: Context,
    bitmap: Bitmap,
    shortUrl: String,
) {
    val dir = File(context.cacheDir, "share").apply { mkdirs() }
    val file = File(dir, "spoo-qr-${shortUrl.substringAfterLast('/')}.png")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send =
        Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "https://$shortUrl")
            // The sheet only draws an image preview off clipData.
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    context.startActivity(Intent.createChooser(send, null))
}

/** Saves the PNG into Pictures/spoo through MediaStore (no permissions). */
private fun saveQr(
    context: Context,
    bitmap: Bitmap,
    shortUrl: String,
): Boolean {
    val values =
        ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "spoo-qr-${shortUrl.substringAfterLast('/')}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/spoo")
        }
    val resolver = context.contentResolver
    val uri =
        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false
    return runCatching {
        resolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }.isSuccess
}

private const val INK = 0xFF1B1B1F.toInt()

private fun qrBitmap(
    context: Context,
    content: String,
    sizePx: Int = 1024,
): Bitmap {
    val code =
        Encoder.encode(
            content,
            ErrorCorrectionLevel.H,
            mapOf(EncodeHintType.CHARACTER_SET to "UTF-8"),
        )
    val m = code.matrix
    val modules = m.width
    val margin = 2
    val cell = sizePx.toFloat() / (modules + 2 * margin)
    val origin = margin * cell

    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK }

    // The center window the logo sits in; H-level correction absorbs it.
    val hole = (modules * 0.24f).toInt().let { if (it % 2 == modules % 2) it else it + 1 }
    val holeStart = (modules - hole) / 2
    val holeEnd = holeStart + hole

    fun inFinder(
        x: Int,
        y: Int,
    ) = (x < 7 && y < 7) || (x >= modules - 7 && y < 7) || (x < 7 && y >= modules - 7)

    fun inHole(
        x: Int,
        y: Int,
    ) = x in holeStart until holeEnd && y in holeStart until holeEnd

    for (y in 0 until modules) {
        for (x in 0 until modules) {
            if (m.get(x, y).toInt() != 1 || inFinder(x, y) || inHole(x, y)) continue
            canvas.drawCircle(
                origin + (x + 0.5f) * cell,
                origin + (y + 0.5f) * cell,
                cell * 0.42f,
                paint,
            )
        }
    }

    // Finder eyes: rounded ring plus rounded pupil, the qr.spoo.me look.
    fun eye(
        mx: Int,
        my: Int,
    ) {
        val left = origin + mx * cell
        val top = origin + my * cell

        fun rect(
            inset: Float,
            span: Float,
        ) = RectF(left + inset * cell, top + inset * cell, left + span * cell, top + span * cell)
        paint.color = INK
        canvas.drawRoundRect(rect(0f, 7f), 2.4f * cell, 2.4f * cell, paint)
        paint.color = android.graphics.Color.WHITE
        canvas.drawRoundRect(rect(1f, 6f), 1.7f * cell, 1.7f * cell, paint)
        paint.color = INK
        canvas.drawRoundRect(rect(2f, 5f), 1.1f * cell, 1.1f * cell, paint)
    }
    eye(0, 0)
    eye(modules - 7, 0)
    eye(0, modules - 7)

    // The ghost mark, one module of breathing room inside the window.
    val logo =
        (ContextCompat.getDrawable(context, R.drawable.logo_black) as? BitmapDrawable)
            ?.bitmap
    if (logo != null) {
        val pad = 1f * cell
        val dst =
            RectF(
                origin + holeStart * cell + pad,
                origin + holeStart * cell + pad,
                origin + holeEnd * cell - pad,
                origin + holeEnd * cell - pad,
            )
        canvas.drawBitmap(logo, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
    }
    return bitmap
}
