package de.froehlichmedia.adaptkey.emoji

/**
 * Pure parser for the bundled emoji dataset asset (L-03): one `<CATEGORY>\t<emoji>` pair per line.
 * Blank lines and lines naming an unknown category are skipped, so a malformed line never crashes
 * the panel - it just loses that one entry.
 */
object EmojiDatasetParser {
    
    private const val SEPARATOR = '\t'
    
    /**
     * Parses the raw asset text into a dataset.
     *
     * @param raw the asset file content, one `<CATEGORY>\t<emoji>` pair per line
     * @return a dataset with every [EmojiCategory] present, populated in file order
     */
    fun parse(raw: String): EmojiDataset {
        val byCategory = LinkedHashMap<EmojiCategory, MutableList<String>>()
        for (category in EmojiCategory.entries) {
            byCategory[category] = ArrayList()
        }
        raw.lineSequence().forEach { line -> parseLine(line, byCategory) }
        return EmojiDataset(byCategory)
    }
    
    private fun parseLine(line: String, byCategory: MutableMap<EmojiCategory, MutableList<String>>) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            return
        }
        val separatorIndex = trimmed.indexOf(SEPARATOR)
        if (separatorIndex <= 0 || separatorIndex == trimmed.length - 1) {
            return
        }
        val category = runCatching { EmojiCategory.valueOf(trimmed.substring(0, separatorIndex)) }.getOrNull() ?: return
        byCategory.getValue(category).add(trimmed.substring(separatorIndex + 1))
    }
}
