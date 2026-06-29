package de.froehlichmedia.adaptkey.suggestion

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import de.froehlichmedia.adaptkey.R

/**
 * Horizontally scrollable suggestion strip (S-01).
 *
 * Renders the entries produced by {@link SuggestionController}; the most probable suggestion is at
 * the far left and the strip scrolls horizontally when entries overflow. The verbatim "keep as
 * typed" chip (S-06) is rendered in a distinct style. Tapping an entry notifies [onItemClick].
 */
class SuggestionBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HorizontalScrollView(context, attrs, defStyleAttr) {
    
    /** Invoked when the user taps a suggestion entry. */
    fun interface OnItemClickListener {
        
        fun onItemClick(item: SuggestionController.DisplayItem)
    }
    
    var onItemClick: OnItemClickListener? = null
    
    private val container = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
    }
    
    init {
        isFillViewport = true
        isHorizontalScrollBarEnabled = false
        setBackgroundColor(ContextCompat.getColor(context, R.color.suggestion_bar_background))
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(44))
        addView(container)
    }
    
    /**
     * Replaces the displayed entries.
     *
     * @param items the ordered entries from the controller (left to right)
     */
    fun setItems(items: List<SuggestionController.DisplayItem>) {
        container.removeAllViews()
        scrollX = 0
        for (item in items) {
            container.addView(chipFor(item))
        }
    }
    
    private fun chipFor(item: SuggestionController.DisplayItem): View {
        val verbatim = item.kind == SuggestionController.Kind.VERBATIM
        val colorRes = if (verbatim) R.color.suggestion_verbatim_text else R.color.suggestion_text
        return TextView(context).apply {
            text = item.text
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, colorRes))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            if (verbatim) {
                setTypeface(typeface, Typeface.BOLD)
            }
            setPadding(dp(16), 0, dp(16), 0)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT)
            isClickable = true
            setOnClickListener { onItemClick?.onItemClick(item) }
        }
    }
    
    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
