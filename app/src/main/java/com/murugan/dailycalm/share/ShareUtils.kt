package com.murugan.dailycalm.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
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
import com.murugan.dailycalm.Links
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

    /**
     * Where a share sends people.
     *
     * The app is in closed testing, so the public store page returns 404 — sharing it means every
     * shared card carries a dead link. This is the closed-test opt-in page instead, which resolves
     * today. Note it only admits testers whose email is on the list in Play Console; adding people
     * there is what turns this link into an install.
     *
     * 🔧 On production launch, switch to:
     *    https://play.google.com/store/apps/details?id=com.murugan.dailycalm
     */
    const val APP_LINK = "https://play.google.com/apps/testing/com.murugan.dailycalm"

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

    private fun caption(title: String): String = buildString {
        append("🙏 $title\n\n")
        append("VetriDaily — தினசரி முருகன் அருள்\n")
        append("📲 $APP_LINK\n")
        append("▶ ${Links.YOUTUBE_CHANNEL_URL}")
    }

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

        val side = margin + 70f
        val textWidth = (W - 2 * side).toInt()

        val name = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF7FBFF.toInt()
            textSize = 66f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF4B73E.toInt()
            textSize = 74f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFEAF4FB.toInt()
            textSize = 56f
        }

        // Lay the text out before drawing anything, so the whole block can be centred rather than
        // stacked from a fixed top — otherwise short content leaves a large void above the footer.
        val titleLayout = buildLayout(title, titlePaint, textWidth)
        val bodyLayout = buildLayout(body, bodyPaint, textWidth)

        val logoSize = 220
        val logoGap = 34f
        val nameHeight = 78f
        val nameGap = 56f
        val titleGap = 46f

        val contentHeight = logoSize + logoGap + nameHeight + nameGap +
            titleLayout.height + titleGap + bodyLayout.height

        // Keep the block clear of the border at the top and the footer at the bottom.
        val topLimit = margin + 90f
        val bottomLimit = H - 300f
        var y = ((topLimit + bottomLimit - contentHeight) / 2f).coerceAtLeast(topLimit)

        // Logo, masked to a circle. The source is a 1024x1024 square with an opaque white
        // background, which reads as a white block against the dark card if drawn as-is.
        val logo = (ResourcesCompat.getDrawable(context.resources, R.drawable.vetri_daily_logo, null)
                as? BitmapDrawable)?.bitmap
        if (logo != null) {
            val scaled = Bitmap.createScaledBitmap(logo, logoSize, logoSize, true)
            val radius = logoSize / 2f
            val centerX = W / 2f
            val centerY = y + radius

            val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            }
            canvas.save()
            canvas.translate(centerX - radius, y)
            canvas.drawCircle(radius, radius, radius, logoPaint)
            canvas.restore()

            // Thin gold ring, so the circle reads as deliberate rather than a cropped square.
            canvas.drawCircle(
                centerX, centerY, radius,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                    color = 0x88F4B73E.toInt()
                }
            )
        }
        y += logoSize + logoGap

        canvas.drawText("VetriDaily", W / 2f, y + nameHeight - 18f, name)
        y += nameHeight + nameGap

        y = drawLayout(canvas, titleLayout, side, y) + titleGap
        drawLayout(canvas, bodyLayout, side, y)

        // Footer blessing + brand.
        val foot = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFC8DFEE.toInt()
            textSize = 46f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(FOOTER_BLESSING, W / 2f, H - 210f, foot)

        // Every shared card carries the channel. These land in family WhatsApp groups, which is
        // exactly the audience the channel wants and costs nothing to reach.
        canvas.drawText("VetriDaily • ${Links.YOUTUBE_HANDLE}", W / 2f, H - 140f, foot)

        return bmp
    }

    /** Wrapped, centred text. Built separately from drawing so the block can be measured first. */
    private fun buildLayout(text: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(14f, 1f)
            .build()

    /** Draws a prepared layout at [top]; returns the y just below the block. */
    private fun drawLayout(canvas: Canvas, layout: StaticLayout, x: Float, top: Float): Float {
        canvas.save()
        canvas.translate(x, top)
        layout.draw(canvas)
        canvas.restore()
        return top + layout.height
    }
}
