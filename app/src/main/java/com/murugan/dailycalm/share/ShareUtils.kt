package com.murugan.dailycalm.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.murugan.dailycalm.R
import java.io.File
import java.io.FileOutputStream

/**
 * Builds a branded "quote card" image from the day's content and shares it.
 *
 * Powers three growth features:
 *   - shareCard()       → share the branded image anywhere (WhatsApp groups, FB, etc.)
 *   - shareToWhatsApp() → one-tap into WhatsApp (user picks "My Status" or a contact)
 *   - sendToFriend()    → plain text invite with the app link
 *
 * Every shared image carries the logo + app name, so each share advertises VetriDaily.
 */
object ShareUtils {

    // 🔧 CHANGE ME: while in testing, paste your Play "closed testing opt-in" link here.
    //    After launch, use the normal store link (the default below already works post-launch).
    const val APP_LINK = "https://play.google.com/store/apps/details?id=com.murugan.dailycalm"

    // 🔧 CHANGE ME: Tamil blessing line printed at the bottom of the card.
    private const val FOOTER_BLESSING = "முருகன் அருளுடன்"

    private const val W = 1080
    private const val H = 1920

    // ---- public actions -----------------------------------------------------

    fun shareCard(context: Context, title: String, body: String) {
        val uri = saveToCache(context, renderQuoteCard(context, title, body)) ?: return
        val intent = imageIntent(uri, caption(title))
        context.startActivity(
            Intent.createChooser(intent, "Share via")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun shareToWhatsApp(context: Context, title: String, body: String) {
        val uri = saveToCache(context, renderQuoteCard(context, title, body)) ?: return
        val intent = imageIntent(uri, caption(title)).apply { setPackage("com.whatsapp") }
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            // WhatsApp (or WhatsApp Business) not found → fall back to the normal chooser.
            try {
                context.startActivity(
                    imageIntent(uri, caption(title))
                        .apply { setPackage("com.whatsapp.w4b") }
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e2: Exception) {
                shareCard(context, title, body)
            }
        }
    }

    fun sendToFriend(context: Context, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, caption(title))
        }
        context.startActivity(
            Intent.createChooser(intent, "Send to friend")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // ---- helpers ------------------------------------------------------------

    private fun imageIntent(uri: Uri, text: String): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    private fun caption(title: String): String =
        "🙏 $title\n\nVetriDaily — தினசரி முருகன் அருள்\n📲 $APP_LINK"

    private fun saveToCache(context: Context, bitmap: Bitmap): Uri? = try {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "vetridaily_card.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not prepare image", Toast.LENGTH_SHORT).show()
        null
    }

    /** Draws the branded portrait card (1080x1920, ideal for WhatsApp Status). */
    fun renderQuoteCard(context: Context, title: String, body: String): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Background gradient — matches the app's theme.
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, H.toFloat(),
                intArrayOf(0xFF071521.toInt(), 0xFF0D2535.toInt(), 0xFF13344A.toInt()),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), bg)

        // Soft gold border.
        val margin = 48f
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = 0x66F4B73E
        }
        canvas.drawRoundRect(RectF(margin, margin, W - margin, H - margin), 40f, 40f, border)

        var y = 170f

        // Logo, centered near the top.
        val logo = (ResourcesCompat.getDrawable(context.resources, R.drawable.vetri_daily_logo, null)
                as? BitmapDrawable)?.bitmap
        if (logo != null) {
            val size = 200
            val scaled = Bitmap.createScaledBitmap(logo, size, size, true)
            canvas.drawBitmap(scaled, (W - size) / 2f, y, null)
            y += size + 24f
        }

        // App name.
        val name = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF7FBFF.toInt()
            textSize = 66f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("VetriDaily", W / 2f, y + 60f, name)
        y += 130f

        // Centre block: title (gold) + body (white), both wrapped.
        val side = margin + 70f
        val textWidth = (W - 2 * side).toInt()

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF4B73E.toInt()
            textSize = 74f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        y = drawWrapped(canvas, title, titlePaint, side, y + 60f, textWidth)

        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFEAF4FB.toInt()
            textSize = 56f
        }
        drawWrapped(canvas, body, bodyPaint, side, y + 50f, textWidth)

        // Footer blessing + brand.
        val foot = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFC8DFEE.toInt()
            textSize = 46f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(FOOTER_BLESSING, W / 2f, H - 210f, foot)
        canvas.drawText("VetriDaily • Play Store", W / 2f, H - 140f, foot)

        return bmp
    }

    /** Draws wrapped, centred text starting at [top]; returns the y just below the block. */
    private fun drawWrapped(
        canvas: Canvas,
        text: String,
        paint: TextPaint,
        x: Float,
        top: Float,
        width: Int
    ): Float {
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(14f, 1f)
            .build()
        canvas.save()
        canvas.translate(x, top)
        layout.draw(canvas)
        canvas.restore()
        return top + layout.height
    }
}
