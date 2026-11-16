@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package dev.podlink.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.podlink.R
import dev.podlink.ui.theme.PodThemeTokens
import dev.podlink.ui.theme.tabular
import kotlin.math.roundToInt

private val SingleRowShape = RoundedCornerShape(PodThemeTokens.outerRadius)
private val FirstRowShape = RoundedCornerShape(
    topStart = PodThemeTokens.outerRadius,
    topEnd = PodThemeTokens.outerRadius,
    bottomStart = PodThemeTokens.innerRadius,
    bottomEnd = PodThemeTokens.innerRadius,
)
private val MiddleRowShape = RoundedCornerShape(PodThemeTokens.innerRadius)
private val LastRowShape = RoundedCornerShape(
    topStart = PodThemeTokens.innerRadius,
    topEnd = PodThemeTokens.innerRadius,
    bottomStart = PodThemeTokens.outerRadius,
    bottomEnd = PodThemeTokens.outerRadius,
)

fun segmentedShape(index: Int, count: Int): RoundedCornerShape = when {
    count <= 1 -> SingleRowShape
    index == 0 -> FirstRowShape
    index >= count - 1 -> LastRowShape
    else -> MiddleRowShape
}

private val SwitchCheckedThumb: @Composable () -> Unit = {
    Icon(
        imageVector = Icons.Rounded.Check,
        contentDescription = null,
        modifier = Modifier.size(SwitchDefaults.IconSize),
    )
}

@Composable
fun ExpressiveLazyScaffold(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    val pageColor = PodThemeTokens.pageContainer

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = pageColor,
        snackbarHost = snackbarHost,
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                subtitle = subtitle?.let { sub ->
                    {
                        Text(
                            text = sub,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        FilledTonalIconButton(
                            onClick = onBack,
                            shapes = IconButtonDefaults.shapes(),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.remake_close),
                            )
                        }
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = pageColor,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    subtitleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        val listPadding = remember(innerPadding) {
            object : PaddingValues {
                override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
                    innerPadding.calculateLeftPadding(layoutDirection) + 16.dp

                override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
                    innerPadding.calculateRightPadding(layoutDirection) + 16.dp

                override fun calculateTopPadding(): Dp =
                    innerPadding.calculateTopPadding() + 8.dp

                override fun calculateBottomPadding(): Dp =
                    innerPadding.calculateBottomPadding() + 28.dp
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = listPadding,
            verticalArrangement = Arrangement.spacedBy(PodThemeTokens.segmentGap),
            content = content,
        )
    }
}

fun LazyListScope.sectionGap(key: String) {
    item(key = key, contentType = "gap") {
        Spacer(modifier = Modifier.height(PodThemeTokens.sectionSpacing))
    }
}

fun LazyListScope.sectionHeader(
    key: String,
    @StringRes titleRes: Int,
) {
    item(key = key, contentType = "header") {
        SectionHeader(
            title = stringResource(titleRes),
            modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
        )
    }
}

fun LazyListScope.sectionHeader(
    key: String,
    title: String,
) {
    item(key = key, contentType = "header") {
        SectionHeader(
            title = title,
            modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmallEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 4.dp),
    )
}

@Composable
fun SegmentedCardGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PodThemeTokens.segmentGap),
        content = content,
    )
}

@Composable
private fun rowBackground(enabled: Boolean) =
    if (enabled) PodThemeTokens.rowContainer else PodThemeTokens.rowDisabledContainer

@Composable
private fun primaryInk(enabled: Boolean) =
    if (enabled) MaterialTheme.colorScheme.onSurface else PodThemeTokens.primaryDisabledInk

@Composable
private fun secondaryInk(enabled: Boolean) =
    if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else PodThemeTokens.secondaryDisabledInk

@Composable
private fun LeadingRowIcon(
    icon: ImageVector?,
    tint: Color,
) {
    if (icon != null) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
fun SegmentedActionRow(
    index: Int,
    count: Int,
    title: String,
    subtitle: String? = null,
    trailingText: String? = null,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = segmentedShape(index, count)
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Button },
        shape = shape,
        color = rowBackground(enabled),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LeadingRowIcon(
                icon = leadingIcon,
                tint = if (enabled) MaterialTheme.colorScheme.primary else secondaryInk(false),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLargeEmphasized,
                    color = primaryInk(enabled),
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryInk(enabled),
                    )
                }
            }
            if (!trailingText.isNullOrBlank()) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryInk(enabled),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = secondaryInk(enabled),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun SegmentedSwitchRow(
    index: Int,
    count: Int,
    title: String,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val shape = segmentedShape(index, count)
    Surface(
        onClick = { onCheckedChange(!checked) },
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Switch },
        shape = shape,
        color = rowBackground(enabled),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LeadingRowIcon(
                icon = leadingIcon,
                tint = if (enabled) MaterialTheme.colorScheme.primary else secondaryInk(false),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLargeEmphasized,
                    color = primaryInk(enabled),
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryInk(enabled),
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = if (enabled) onCheckedChange else null,
                enabled = enabled,
                thumbContent = if (checked) SwitchCheckedThumb else null,
            )
        }
    }
}

@Composable
fun SegmentedSelectableRow(
    index: Int,
    count: Int,
    title: String,
    subtitle: String? = null,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = segmentedShape(index, count)
    val bg = when {
        !enabled -> PodThemeTokens.rowDisabledContainer
        selected -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.52f)
        else -> PodThemeTokens.rowContainer
    }
    Surface(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.RadioButton },
        shape = shape,
        color = bg,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLargeEmphasized,
                    color = if (!enabled) {
                        primaryInk(false)
                    } else if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (!enabled) {
                            secondaryInk(false)
                        } else if (selected) {
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            RadioButton(
                selected = selected,
                onClick = null,
                enabled = enabled,
            )
        }
    }
}

@Composable
fun SegmentedSliderRow(
    index: Int,
    count: Int,
    title: String,
    valueLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    subtitle: String? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    steps: Int = 0,
    enabled: Boolean = true,
    startLabel: String? = null,
    centerLabel: String? = null,
    endLabel: String? = null,
) {
    val shape = segmentedShape(index, count)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = rowBackground(enabled),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLargeEmphasized,
                    color = primaryInk(enabled),
                )
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.labelLargeEmphasized.tabular,
                    color = if (enabled) MaterialTheme.colorScheme.primary else secondaryInk(false),
                )
            }
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryInk(enabled),
                )
            }
            val sliderState = remember(steps, valueRange) {
                SliderState(value, steps, valueRange)
            }
            var dragging by remember { mutableStateOf(false) }
            if (!dragging && sliderState.value != value) {
                sliderState.value = value
            }
            Slider(
                state = sliderState,
                onValueChange = { next ->
                    dragging = true
                    sliderState.value = next
                    onValueChange(next)
                },
                onValueChangeFinished = {
                    dragging = false
                    onValueChangeFinished?.invoke()
                },
                enabled = enabled,
            )
            if (startLabel != null || centerLabel != null || endLabel != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = startLabel.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryInk(enabled),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (centerLabel != null) {
                        Text(
                            text = centerLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryInk(enabled),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(
                        text = endLabel.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryInk(enabled),
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
fun SegmentedDiscreteStepRow(
    index: Int,
    count: Int,
    title: String,
    subtitle: String? = null,
    selectedStep: Int,
    stepLabels: List<String>,
    enabled: Boolean = true,
    onStepSelected: (Int) -> Unit,
) {
    val safeIndex = selectedStep.coerceIn(0, (stepLabels.size - 1).coerceAtLeast(0))
    val label = stepLabels.getOrElse(safeIndex) { "" }
    val steps = (stepLabels.size - 2).coerceAtLeast(0)
    SegmentedSliderRow(
        index = index,
        count = count,
        title = title,
        subtitle = subtitle,
        valueLabel = label,
        value = safeIndex.toFloat(),
        onValueChange = { onStepSelected(it.roundToInt().coerceIn(0, stepLabels.lastIndex)) },
        valueRange = 0f..(stepLabels.lastIndex.coerceAtLeast(1)).toFloat(),
        steps = steps,
        enabled = enabled,
        startLabel = stepLabels.firstOrNull(),
        centerLabel = if (stepLabels.size == 3) stepLabels[1] else null,
        endLabel = stepLabels.lastOrNull(),
    )
}

@Composable
fun SegmentedKeyValueRow(
    index: Int,
    count: Int,
    title: String,
    value: String,
    leadingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    val shape = segmentedShape(index, count)
    val stacked = value.length > 16 || title.length + value.length > 26
    val rowContent: @Composable () -> Unit = {
        if (stacked) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                LeadingRowIcon(
                    icon = leadingIcon,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLargeEmphasized.tabular,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                LeadingRowIcon(
                    icon = leadingIcon,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLargeEmphasized.tabular,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 180.dp),
                )
            }
        }
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { role = Role.Button },
            shape = shape,
            color = PodThemeTokens.rowContainer,
            content = rowContent,
        )
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = PodThemeTokens.rowContainer,
            content = rowContent,
        )
    }
}
