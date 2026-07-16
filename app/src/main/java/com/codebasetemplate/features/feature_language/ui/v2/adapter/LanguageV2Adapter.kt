package com.codebasetemplate.features.feature_language.ui.v2.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.codebasetemplate.R
import com.codebasetemplate.databinding.ItemLanguageGroupV2Binding
import com.codebasetemplate.databinding.ItemLanguageOptionV2Binding
import java.util.Locale

class LanguageV2Adapter : ListAdapter<LanguageRow, RecyclerView.ViewHolder>(DIFF) {

    var onSelectionChanged: ((LanguageOption) -> Unit)? = null
    var onGroupClicked: ((LanguageGroup) -> Boolean)? = null

    private var groups: List<LanguageGroup> = emptyList()
    private var expandedGroupId: String? = null
    private var selectedLanguageTag: String? = null

    init {
        setHasStableIds(true)
    }

    val selectedOption: LanguageOption?
        get() = groups.asSequence()
            .flatMap { it.options.asSequence() }
            .firstOrNull { isSelectedTag(it.languageTag) }

    override fun getItemId(position: Int): Long {
        return when (val item = getItem(position)) {
            is LanguageRow.Group -> "group:${item.group.id}".hashCode().toLong()
            is LanguageRow.Option -> "option:${item.option.id}".hashCode().toLong()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is LanguageRow.Group -> VIEW_TYPE_GROUP
            is LanguageRow.Option -> VIEW_TYPE_OPTION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_GROUP -> GroupViewHolder(
                ItemLanguageGroupV2Binding.inflate(inflater, parent, false)
            )

            else -> OptionViewHolder(
                ItemLanguageOptionV2Binding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is LanguageRow.Group -> (holder as GroupViewHolder).bind(item)
            is LanguageRow.Option -> (holder as OptionViewHolder).bind(item)
        }
    }

    fun submitGroups(
        languageGroups: List<LanguageGroup>,
        expandedGroupId: String? = null,
        selectedLanguageTag: String? = null,
    ) {
        groups = languageGroups
        this.selectedLanguageTag = selectedLanguageTag
        this.expandedGroupId = expandedGroupId
        submitList(buildRows())
    }

    private fun toggleGroup(group: LanguageGroup) {
        val isHandled = onGroupClicked?.invoke(group) == true
        if (isHandled) return

        expandedGroupId = if (expandedGroupId == group.id) null else group.id
        submitList(buildRows())
    }

    private fun selectOption(option: LanguageOption) {
        if (tagsExactlyMatch(option.languageTag, selectedLanguageTag.orEmpty())) return
        selectedLanguageTag = option.languageTag
        onSelectionChanged?.invoke(option)
        submitList(buildRows())
    }

    private fun buildRows(): List<LanguageRow> {
        return buildList {
            groups.forEach { group ->
                val isExpanded = group.id == expandedGroupId
                val isSelected = group.options.any {
                    isSelectedTag(it.languageTag)
                }
                add(LanguageRow.Group(group, isExpanded, isSelected))
                if (isExpanded) {
                    group.options.forEachIndexed { index, option ->
                        add(
                            LanguageRow.Option(
                                groupId = group.id,
                                option = option,
                                isFirst = index == 0,
                                isLast = index == group.options.lastIndex,
                                isSelected = isSelectedTag(option.languageTag)
                            )
                        )
                    }
                }
            }
        }
    }

    private fun isSelectedTag(candidateLanguageTag: String): Boolean {
        val selected = selectedLanguageTag.orEmpty()
        if (tagsExactlyMatch(candidateLanguageTag, selected)) return true
        return Locale.forLanguageTag(selected).country.isBlank() &&
                languageMatches(candidateLanguageTag, selected)
    }

    private fun bindFlag(view: TextView, countryCode: String?) {
        if (countryCode.isNullOrBlank()) {
            view.text = ""
            view.setBackgroundResource(R.drawable.bg_language_flag_circle)
            return
        }

        val resourceName = "flag_${countryCode.lowercase(Locale.US)}"
        val flagResource = view.context.resources.getIdentifier(
            resourceName,
            "drawable",
            view.context.packageName
        )
        if (flagResource != 0) {
            view.text = ""
            view.setBackgroundResource(flagResource)
        } else {
            view.text = countryFlagEmoji(countryCode)
            view.setBackgroundResource(R.drawable.bg_language_flag_circle)
        }
    }

    private inner class GroupViewHolder(
        private val binding: ItemLanguageGroupV2Binding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: LanguageRow.Group) {
            setBottomSpacing(binding.root, if (row.isExpanded) 0 else 12)
            val group = row.group
            binding.languageGroupTitle.text = group.title
            binding.languageGroupSubtitle.text = group.nativeName
            val flags = group.options.take(3)
            bindFlag(binding.languageGroupFlagOne, flags.getOrNull(0)?.countryCode)
            bindFlag(binding.languageGroupFlagTwo, flags.getOrNull(1)?.countryCode)
            bindFlag(binding.languageGroupFlagThree, flags.getOrNull(2)?.countryCode)
            binding.languageGroupFlagTwo.visibility = if (flags.size > 1) View.VISIBLE else View.GONE
            binding.languageGroupFlagThree.visibility = if (flags.size > 2) View.VISIBLE else View.GONE
            binding.languageExpandIcon.animate()
                .rotation(if (row.isExpanded) 180f else 0f)
                .setDuration(180L)
                .start()
            binding.root.setOnClickListener {
                toggleGroup(group)
            }
            binding.languageGroupCard.setOnClickListener {
                toggleGroup(group)
            }
        }
    }

    private fun setBottomSpacing(view: View, bottomDp: Int) {
        val params = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        val bottomPx = (bottomDp * view.resources.displayMetrics.density).toInt()
        if (params.bottomMargin == bottomPx) return
        params.bottomMargin = bottomPx
        view.layoutParams = params
    }

    private inner class OptionViewHolder(
        private val binding: ItemLanguageOptionV2Binding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: LanguageRow.Option) {
            val option = row.option
            setOptionSpacing(
                root = binding.root,
                card = binding.languageOptionCard,
                branch = binding.languageBranch,
                isFirst = row.isFirst,
                isLast = row.isLast
            )
            binding.languageBranch.isLast = row.isLast
            binding.languageOptionName.text = option.title
            bindFlag(binding.languageOptionFlag, option.countryCode)
            binding.languageOptionCard.setBackgroundResource(
                if (row.isSelected) {
                    R.drawable.bg_language_option_selected
                } else {
                    R.drawable.bg_language_option_normal
                }
            )
            binding.languageOptionName.setTextColor(
                binding.root.context.getColor(
                    if (row.isSelected) R.color.white else R.color.language_v2_text
                )
            )
            binding.languageOptionRadio.setImageResource(
                if (row.isSelected) {
                    R.drawable.ic_language_v2_radio_selected
                } else {
                    R.drawable.ic_language_v2_radio_unselected
                }
            )
            binding.root.setOnClickListener {
                selectOption(option)
            }
            binding.languageOptionCard.setOnClickListener {
                selectOption(option)
            }
        }
    }

    private fun setOptionSpacing(
        root: View,
        card: View,
        branch: LanguageBranchView,
        isFirst: Boolean,
        isLast: Boolean,
    ) {
        val topDp = if (isFirst) OPTION_OUTER_SPACING_DP else OPTION_INNER_SPACING_DP
        val bottomDp = if (isLast) OPTION_OUTER_SPACING_DP else OPTION_INNER_SPACING_DP
        val density = root.resources.displayMetrics.density
        val rootHeight = ((topDp + OPTION_CARD_HEIGHT_DP + bottomDp) * density).toInt()
        if (root.layoutParams.height != rootHeight) {
            root.layoutParams = root.layoutParams.apply {
                height = rootHeight
            }
        }

        val topMargin = (topDp * density).toInt()
        val cardParams = card.layoutParams as? ViewGroup.MarginLayoutParams
        if (cardParams != null && cardParams.topMargin != topMargin) {
            cardParams.topMargin = topMargin
            card.layoutParams = cardParams
        }

        branch.cardCenterY = (topDp + OPTION_CARD_HEIGHT_DP / 2f) * density
    }

    companion object {
        private const val VIEW_TYPE_GROUP = 1
        private const val VIEW_TYPE_OPTION = 2
        private const val OPTION_CARD_HEIGHT_DP = 56
        private const val OPTION_INNER_SPACING_DP = 8
        private const val OPTION_OUTER_SPACING_DP = 16

        private val DIFF = object : DiffUtil.ItemCallback<LanguageRow>() {
            override fun areItemsTheSame(oldItem: LanguageRow, newItem: LanguageRow): Boolean {
                return when {
                    oldItem is LanguageRow.Group && newItem is LanguageRow.Group ->
                        oldItem.group.id == newItem.group.id

                    oldItem is LanguageRow.Option && newItem is LanguageRow.Option ->
                        oldItem.option.id == newItem.option.id

                    else -> false
                }
            }

            override fun areContentsTheSame(oldItem: LanguageRow, newItem: LanguageRow): Boolean {
                return oldItem == newItem
            }
        }

        fun languageMatches(left: String, right: String): Boolean {
            if (left.isBlank() || right.isBlank()) return false
            val normalizedLeft = left.replace('_', '-')
            val normalizedRight = right.replace('_', '-')
            if (tagsExactlyMatch(normalizedLeft, normalizedRight)) return true
            return Locale.forLanguageTag(normalizedLeft).language.equals(
                Locale.forLanguageTag(normalizedRight).language,
                ignoreCase = true
            )
        }

        private fun tagsExactlyMatch(left: String, right: String): Boolean {
            return left.replace('_', '-').equals(right.replace('_', '-'), ignoreCase = true)
        }

        fun countryFlagEmoji(countryCode: String): String {
            val normalized = countryCode.uppercase(Locale.US)
            if (normalized.length != 2) return ""
            val first = Character.codePointAt(normalized, 0) - 'A'.code + 0x1F1E6
            val second = Character.codePointAt(normalized, 1) - 'A'.code + 0x1F1E6
            return String(Character.toChars(first)) + String(Character.toChars(second))
        }
    }
}
