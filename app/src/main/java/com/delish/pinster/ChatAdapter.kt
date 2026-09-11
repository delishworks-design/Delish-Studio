package com.delish.pinster

import android.graphics.Color
import android.graphics.Typeface
import android.text.format.DateUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private val messages: MutableList<ChatMessage>,
    private val isDarkMode: () -> Boolean,
    private val onRetryClick: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_USER = 0
        const val VIEW_TYPE_BOT = 1
        const val VIEW_TYPE_USER_IMAGE = 2
    }

    override fun getItemViewType(position: Int): Int {
        val msg = messages[position]
        return when {
            msg.isUserImage -> VIEW_TYPE_USER_IMAGE
            msg.role == ChatMessage.ROLE_USER -> VIEW_TYPE_USER
            else -> VIEW_TYPE_BOT
        }
    }

    override fun getItemCount(): Int = messages.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val context = parent.context
        return when (viewType) {
            VIEW_TYPE_USER_IMAGE -> UserImageViewHolder(createUserImageView(context))
            VIEW_TYPE_USER -> UserViewHolder(createUserBubble(context))
            else -> BotViewHolder(createBotBubble(context))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        when (holder) {
            is UserViewHolder -> holder.bind(msg)
            is BotViewHolder -> holder.bind(msg, position)
            is UserImageViewHolder -> holder.bind(msg)
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < DateUtils.MINUTE_IN_MILLIS -> "now"
            diff < DateUtils.HOUR_IN_MILLIS -> "${diff / DateUtils.MINUTE_IN_MILLIS}m ago"
            diff < DateUtils.DAY_IN_MILLIS -> {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
            else -> {
                val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }

    private fun createUserBubble(context: android.content.Context): LinearLayout {
        val d = context.resources.displayMetrics.density
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }
    }

    private fun createBotBubble(context: android.content.Context): LinearLayout {
        val d = context.resources.displayMetrics.density
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.START
        }
    }

    private fun createUserImageView(context: android.content.Context): LinearLayout {
        val d = context.resources.displayMetrics.density
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }
    }

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(msg: ChatMessage) {
            val container = itemView as LinearLayout
            container.removeAllViews()
            val d = container.context.resources.displayMetrics.density

            val card = FrameLayout(container.context).apply {
                val bg = android.graphics.drawable.GradientDrawable().apply {
                    setColor(if (isDarkMode()) Color.rgb(55, 55, 55) else Color.rgb(28, 28, 28))
                    cornerRadius = 16f * d
                }
                background = bg
                setPadding((14 * d).toInt(), (10 * d).toInt(), (14 * d).toInt(), (10 * d).toInt())
            }
            val tv = TextView(container.context).apply {
                text = msg.content
                textSize = 14f
                typeface = Typeface.MONOSPACE
                setTextColor(Color.WHITE)
                setLineSpacing(0f, 1.3f)
                setTextIsSelectable(true)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            card.addView(tv)
            container.addView(card, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))

            val timeTv = TextView(container.context).apply {
                text = formatTimestamp(msg.timestamp)
                textSize = 10f
                setTextColor(Color.argb(150, 255, 255, 255))
                setPadding(0, (2 * d).toInt(), 0, 0)
                gravity = Gravity.END
            }
            container.addView(timeTv, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.END })

            container.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins((8 * d).toInt(), (4 * d).toInt(), (8 * d).toInt(), (4 * d).toInt())
            }
        }
    }

    inner class UserImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(msg: ChatMessage) {
            val container = itemView as LinearLayout
            container.removeAllViews()
            val d = container.context.resources.displayMetrics.density

            if (msg.userImagePath != null) {
                val imgView = ImageView(container.context).apply {
                    load(msg.userImagePath) {
                        crossfade(true)
                        size(180, 180)
                        transformations(RoundedCornersTransformation(12f))
                    }
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    adjustViewBounds = true
                    maxWidth = (180 * d).toInt()
                    setPadding(0, 0, 0, (6 * d).toInt())
                    clipToOutline = true
                    outlineProvider = object : android.view.ViewOutlineProvider() {
                        override fun getOutline(view: View, outline: android.graphics.Outline) {
                            outline.setRoundRect(0, 0, view.width, view.height, (12 * d).toFloat())
                        }
                    }
                }
                container.addView(imgView, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { gravity = Gravity.END })
            }

            if (msg.content.isNotEmpty() && msg.content != "(sent an image)") {
                val tv = TextView(container.context).apply {
                    text = msg.content
                    textSize = 14f
                    typeface = Typeface.MONOSPACE
                    setTextColor(Color.WHITE)
                    setPadding((4 * d).toInt(), (6 * d).toInt(), (4 * d).toInt(), (6 * d).toInt())
                    gravity = Gravity.END
                }
                container.addView(tv, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ))
            }

            val timeTv = TextView(container.context).apply {
                text = formatTimestamp(msg.timestamp)
                textSize = 10f
                setTextColor(Color.argb(150, 255, 255, 255))
                setPadding(0, (2 * d).toInt(), 0, 0)
                gravity = Gravity.END
            }
            container.addView(timeTv, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.END })

            container.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins((8 * d).toInt(), (4 * d).toInt(), (8 * d).toInt(), (4 * d).toInt())
            }
        }
    }

    inner class BotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(msg: ChatMessage, position: Int) {
            val container = itemView as LinearLayout
            container.removeAllViews()
            val d = container.context.resources.displayMetrics.density

            val card = FrameLayout(container.context).apply {
                val bg = android.graphics.drawable.GradientDrawable().apply {
                    setColor(if (isDarkMode()) Color.rgb(30, 30, 30) else Color.WHITE)
                    cornerRadius = 16f * d
                }
                background = bg
                setPadding((14 * d).toInt(), (10 * d).toInt(), (14 * d).toInt(), (10 * d).toInt())
            }
            val tv = TextView(container.context).apply {
                text = formatBotText(msg.content)
                textSize = 14f
                typeface = Typeface.MONOSPACE
                setTextColor(if (isDarkMode()) Color.rgb(245, 245, 243) else Color.rgb(28, 28, 28))
                setLineSpacing(0f, 1.3f)
                setTextIsSelectable(true)
                linksClickable = true
                movementMethod = android.text.method.LinkMovementMethod.getInstance()
                val linkColor = Color.rgb(100, 160, 255)
                setLinkTextColor(linkColor)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            card.addView(tv)

            if (msg.isFailed) {
                val retryBtn = TextView(container.context).apply {
                    text = "↻"
                    textSize = 18f
                    setTextColor(Color.rgb(100, 160, 255))
                    setPadding((8 * d).toInt(), 0, 0, 0)
                    setOnClickListener { onRetryClick?.invoke(adapterPosition) }
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.CENTER_VERTICAL or Gravity.END
                    }
                }
                card.addView(retryBtn)
            }

            container.addView(card, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))

            val timeTv = TextView(container.context).apply {
                text = formatTimestamp(msg.timestamp)
                textSize = 10f
                setTextColor(if (isDarkMode()) Color.argb(100, 155, 155, 150) else Color.argb(100, 100, 97, 93))
                setPadding(0, (2 * d).toInt(), 0, 0)
            }
            container.addView(timeTv, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))

            container.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins((8 * d).toInt(), (4 * d).toInt(), (8 * d).toInt(), (4 * d).toInt())
            }
        }

        @Suppress("DEPRECATION")
        private fun formatBotText(text: String): CharSequence {
            if (text.isBlank()) return text
            val muted = if (isDarkMode()) Color.rgb(155, 155, 150) else Color.rgb(100, 97, 93)
            var html = text
                .replace(Regex("\\[([^]]+)]\\(([^)]+)\\)")) { m ->
                    "<a href=\"${m.groupValues[2]}\">${m.groupValues[1]}</a>"
                }
                .replace(Regex("(https?://[^\\s<>\"]+)")) { m ->
                    val url = m.value.trimEnd('.', ',', ')', '>')
                    "<a href=\"$url\">$url</a>"
                }
                .replace(Regex("\\*\\*(.+?)\\*\\*")) { "<b>${it.groupValues[1]}</b>" }
                .replace(Regex("(?<!\\*)\\*([^*]+?)\\*(?!\\*)")) { "<i>${it.groupValues[1]}</i>" }
                .replace(Regex("_(.+?)_")) { "<i>${it.groupValues[1]}</i>" }
                .replace(Regex("`(.+?)`")) { "<font face=\"monospace\" color=\"#${Integer.toHexString(muted).removePrefix("ff")}\">${it.groupValues[1]}</font>" }
                .replace("\n", "<br>")
            return try {
                android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY)
            } catch (_: Exception) {
                text
            }
        }
    }
}
