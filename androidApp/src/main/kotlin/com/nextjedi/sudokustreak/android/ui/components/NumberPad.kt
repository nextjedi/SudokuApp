package com.nextjedi.sudokustreak.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextjedi.sudokustreak.android.a11y.toggleSemantics
import com.nextjedi.sudokustreak.android.ui.theme.BrandErrorRed
import com.nextjedi.sudokustreak.android.ui.theme.BrandPrimary

/**
 * Number pad — 1..9 + erase + pencil-mode toggle.
 *
 * Phase 5.5 a11y additions:
 *  - Every digit button has an explicit `contentDescription` ("Number 5") so
 *    TalkBack distinguishes it from a generic "5, button" announcement.
 *  - Erase button has `contentDescription = "Erase selected cell"` (instead of
 *    the visual "✕" glyph which TalkBack reads as "multiplication sign").
 *  - Minimum 48 × 48 dp touch target via `sizeIn(minHeight = 48.dp)`. Per
 *    WCAG 2.5.5 Target Size and Material's `MinimumInteractiveComponentSize`.
 *  - The pencil-mode `Switch` uses [toggleSemantics] so TalkBack reads
 *    "Pencil notes mode, currently on, switch".
 *
 * Decoupled from `GameViewModel` — caller passes the digit-press / erase /
 * pencil-toggle callbacks. Pencil mode is an opt-in feature; pass `null` for
 * [onPencilToggle] if the caller doesn't want to render the toggle.
 */
@Composable
fun NumberPad(
    onNumberPress: (Int) -> Unit,
    onErase: () -> Unit,
    disabled: Boolean,
    modifier: Modifier = Modifier,
    pencilModeOn: Boolean = false,
    onPencilToggle: ((Boolean) -> Unit)? = null,
    remainingCounts: Map<Int, Int>? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag(TAG_NUMBER_PAD),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Optional pencil-mode toggle row — WCAG 2.5.1 alternative for the
        // stylus side-button squeeze gesture (multi-pointer / path-based input
        // must have a single-tap equivalent).
        if (onPencilToggle != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleSemantics("Pencil notes mode", pencilModeOn)
                    .testTag(TAG_PENCIL_TOGGLE_ROW),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Pencil notes",
                    modifier = Modifier.padding(start = 4.dp, top = 12.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Switch(
                    checked = pencilModeOn,
                    onCheckedChange = onPencilToggle,
                    enabled = !disabled,
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .semantics {
                            // Re-applied here so the inner Switch (not the row)
                            // also exposes the toggle role for unit tests that
                            // assert it directly.
                            role = Role.Switch
                            stateDescription = if (pencilModeOn) "On" else "Off"
                            contentDescription =
                                "Pencil notes mode, currently ${if (pencilModeOn) "on" else "off"}"
                        }
                        .testTag(TAG_PENCIL_TOGGLE),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            (1..5).forEach { num ->
                NumberButton(
                    num = num,
                    remaining = remainingCounts?.get(num),
                    onClick = { onNumberPress(num) },
                    disabled = disabled,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            (6..9).forEach { num ->
                NumberButton(
                    num = num,
                    remaining = remainingCounts?.get(num),
                    onClick = { onNumberPress(num) },
                    disabled = disabled,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedButton(
                onClick = onErase,
                enabled = !disabled,
                modifier = Modifier
                    .weight(1f)
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .height(52.dp)
                    .semantics {
                        contentDescription = "Erase selected cell"
                        role = Role.Button
                    }
                    .testTag(TAG_ERASE),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = BrandErrorRed,
                ),
            ) {
                Text("✕", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun NumberButton(
    num: Int,
    remaining: Int?,
    onClick: () -> Unit,
    disabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val numStr = num.toString()
    val description = if (remaining != null) {
        "Number $numStr, $remaining remaining"
    } else {
        "Number $numStr"
    }
    Button(
        onClick = onClick,
        enabled = !disabled,
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .height(52.dp)
            .semantics {
                contentDescription = description
                role = Role.Button
            }
            .testTag(numberButtonTag(num)),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandPrimary,
            contentColor = Color.White,
            disabledContainerColor = Color.LightGray,
        ),
    ) {
        Text(numStr, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---- testTags ----

const val TAG_NUMBER_PAD: String = "number_pad"
const val TAG_ERASE: String = "number_pad_erase"
const val TAG_PENCIL_TOGGLE: String = "pencil_toggle"
const val TAG_PENCIL_TOGGLE_ROW: String = "pencil_toggle_row"

/** Test tag for digit button N — e.g. `numberButtonTag(7)` → `"num_btn_7"`. */
fun numberButtonTag(num: Int): String = "num_btn_$num"
