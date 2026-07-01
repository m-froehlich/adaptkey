package de.froehlichmedia.adaptkey.emoji

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import de.froehlichmedia.adaptkey.R

/**
 * Emoji panel (L-03): a category tab bar over a scrollable emoji grid, replacing the letter
 * keyboard in the same screen area while shown. Emoji are delivered as raw Unicode codepoints via
 * [OnEmojiSelectedListener] - the caller is responsible for committing them via `commitText` (no
 * app-side support needed) and for finalising any in-progress composing token first, exactly like a
 * delimiter. A dedicated "recent" tab shows the most recently used emoji (MRU, see [RecentEmojis]);
 * the back button ([OnBackListener]) returns to the letter keyboard.
 *
 * Thin Android glue over the pure [EmojiDataset] / [RecentEmojis]; left to instrumented tests.
 */
class EmojiPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    
    fun interface OnEmojiSelectedListener {
        
        fun onEmojiSelected(emoji: String)
    }
    
    fun interface OnBackListener {
        
        fun onBack()
    }
    
    var onEmojiSelectedListener: OnEmojiSelectedListener? = null
    
    var onBackListener: OnBackListener? = null
    
    /** The bundled dataset (L-03); defaults to empty until [EmojiDatasetLoader] hands one in. */
    var dataset: EmojiDataset = EmojiDataset.EMPTY
        set(value) {
            field = value
            rebuildTabs()
        }
    
    private var recentEmojis: List<String> = emptyList()
    
    // null selects the "recent" tab.
    private var selectedCategory: EmojiCategory? = null
    
    private val tabBar = LinearLayout(context).apply {
        orientation = HORIZONTAL
    }
    
    private val gridContainer = GridLayout(context).apply {
        columnCount = GRID_COLUMNS
    }
    
    init {
        orientation = VERTICAL
        setBackgroundColor(ContextCompat.getColor(context, R.color.keyboard_background))
        
        val tabScroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(TAB_BAR_HEIGHT_DP))
            addView(tabBar)
        }
        addView(tabScroll)
        
        val gridScroll = ScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(GRID_HEIGHT_DP))
            addView(gridContainer)
        }
        addView(gridScroll)
        
        rebuildTabs()
    }
    
    /**
     * Updates the recent/frequently-used emoji shown on the recent tab (MRU).
     *
     * @param recents the current recent-emoji list, most recent first
     */
    fun setRecentEmojis(recents: List<String>) {
        recentEmojis = recents
        if (selectedCategory == null) {
            showGrid(recentEmojis)
        }
    }
    
    private fun rebuildTabs() {
        tabBar.removeAllViews()
        tabBar.addView(tabButton(BACK_ICON) { onBackListener?.onBack() })
        tabBar.addView(tabButton(RECENT_ICON) { selectTab(null) })
        for (category in EmojiCategory.entries) {
            tabBar.addView(tabButton(category.icon) { selectTab(category) })
        }
        selectTab(selectedCategory)
    }
    
    private fun selectTab(category: EmojiCategory?) {
        selectedCategory = category
        val emoji = if (category == null) recentEmojis else dataset.byCategory[category] ?: emptyList()
        showGrid(emoji)
    }
    
    private fun showGrid(emoji: List<String>) {
        gridContainer.removeAllViews()
        for (item in emoji) {
            gridContainer.addView(emojiCell(item))
        }
    }
    
    private fun emojiCell(emoji: String): View {
        return TextView(context).apply {
            text = emoji
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, EMOJI_TEXT_SIZE_SP)
            layoutParams = GridLayout.LayoutParams().apply {
                width = dp(CELL_SIZE_DP)
                height = dp(CELL_SIZE_DP)
            }
            isClickable = true
            setOnClickListener { onEmojiSelectedListener?.onEmojiSelected(emoji) }
        }
    }
    
    private fun tabButton(icon: String, onClick: () -> Unit): View {
        return TextView(context).apply {
            text = icon
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, TAB_TEXT_SIZE_SP)
            setTextColor(ContextCompat.getColor(context, R.color.key_text))
            setPadding(dp(TAB_PADDING_DP), 0, dp(TAB_PADDING_DP), 0)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT)
            isClickable = true
            setOnClickListener { onClick() }
        }
    }
    
    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
    
    companion object {
        
        private const val GRID_COLUMNS = 7
        private const val TAB_BAR_HEIGHT_DP = 44
        private const val GRID_HEIGHT_DP = 216
        private const val CELL_SIZE_DP = 40
        private const val TAB_PADDING_DP = 10
        private const val EMOJI_TEXT_SIZE_SP = 22f
        private const val TAB_TEXT_SIZE_SP = 18f
        private const val BACK_ICON = "⌨"
        private const val RECENT_ICON = "🕐"
    }
}
