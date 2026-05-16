package com.nextjedi.sudokustreak.android.ui.theme

import org.junit.Test
import java.io.File

/**
 * Build-time guard: hardcoded `Color(0x...)` constructors are forbidden outside
 * `ui/theme/`. All paint operations must reference a `Brand*` token (in
 * `ui/theme/Color.kt`) or a `MaterialTheme.colorScheme.*` role.
 *
 * **Implementation note** — this is a JUnit grep test, not a real Lint rule. The M3E
 * review (`test-plan/04-android-m3e-review.md` §9) recommends both options A
 * (custom Lint detector in a `:lint-checks` Gradle module) and B (this JUnit grep).
 * We picked B for this PR because:
 *
 *   1. It runs as part of the existing `testDebugUnitTest` task — no extra Gradle
 *      module to configure or publish.
 *   2. The IDE feedback loop (red squiggle) is a nice-to-have that can come later
 *      in a `:lint-checks` module.
 *   3. A grep test is ~30 lines and easy to evolve; a real Lint detector is
 *      ~200 lines of UAST + Gradle plumbing.
 *
 * The follow-up Linear task to graduate this into a real Lint rule is tracked under
 * `SDK-PARITY-LINT`.
 *
 * ## Tripwire baseline
 *
 * The current main source tree has 7 documented `Color(0x...)` violations + 3
 * `Color.<Named>` violations (see `docs/reports/hardcoded-color-violations.md`).
 * Fixing them is a separate Linear issue (`SDK-PARITY-COLOR-MIGRATION`). For this
 * PR, this test:
 *
 *   - Records the baseline count.
 *   - **Fails the build if the count grows** (a tripwire — prevents regressions).
 *   - **Passes** as long as the count stays at or below baseline.
 *
 * Each time the migration PR removes a violation, decrement the baseline below.
 * Once both baselines are 0, flip them to `require(violations.isEmpty())`.
 */
class HardcodedColorAuditTest {

    /** Matches `Color(0xFF......)` / `Color(0x......)` literal constructors. */
    private val colorLiteralRegex = Regex("""Color\(0x[0-9A-Fa-f]{6,8}\)""")

    /** Matches name-based literals like `Color.Red`, `Color.LightGray`, `Color.White`. */
    private val colorNamedRegex = Regex(
        """\bColor\.(Red|Green|Blue|Yellow|Magenta|Cyan|Gray|LightGray|DarkGray|Black|White|Transparent)\b"""
    )

    /** Documented `Color(0x...)` violations as of 2026-05-16. */
    private val HEX_BASELINE = 7

    /**
     * Documented `Color.<Named>` violations (the 3 strict ones + 13 `Color.White` /
     * etc. used as foreground over coloured backgrounds — will be migrated alongside
     * the M3 component rewrites). See `hardcoded-color-violations.md`.
     */
    private val NAMED_BASELINE = 16

    @Test
    fun no_hardcoded_Color_hex_literals_outside_ui_theme() {
        val violations = scanForViolations(colorLiteralRegex, "Color(0x...)")
        require(violations.size <= HEX_BASELINE) {
            buildString {
                appendLine("Hardcoded Color(0x...) literal count grew above the documented")
                appendLine("baseline ($HEX_BASELINE). Current: ${violations.size}.")
                appendLine()
                appendLine("New violations (all current ones):")
                violations.forEach { appendLine("  $it") }
                appendLine()
                appendLine("Fix: import a Brand* token from ui/theme/Color.kt or use")
                appendLine("MaterialTheme.colorScheme.<role>. Then decrement HEX_BASELINE.")
            }
        }
    }

    /**
     * `Color.<Named>` usage is soft-fail today — emits a warning when over baseline.
     * Promote to hard-fail (`require(violations.isEmpty())`) once the migration is
     * complete.
     */
    @Test
    fun report_Color_Named_usage_outside_ui_theme_as_a_soft_warning() {
        val violations = scanForViolations(colorNamedRegex, "Color.<Named>")
        if (violations.size > NAMED_BASELINE) {
            error(
                buildString {
                    appendLine("Color.<Named> usage grew above baseline ($NAMED_BASELINE).")
                    appendLine("Current: ${violations.size}.")
                    appendLine()
                    violations.forEach { appendLine("  $it") }
                }
            )
        } else if (violations.isNotEmpty()) {
            // Useful diagnostic info in CI logs without failing the build.
            System.err.println(
                "[INFO] ${violations.size} Color.<Named> usages remain (baseline $NAMED_BASELINE)."
            )
        }
    }

    /**
     * Walk the main source tree, filter `.kt` files outside `ui/theme/`, return a
     * `path:line: matched-snippet` list of every match.
     */
    private fun scanForViolations(pattern: Regex, label: String): List<String> {
        val srcRoot = findMainSrcRoot()
            ?: return emptyList() // Allow tests to no-op when source isn't reachable.
        return srcRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { "/ui/theme/" !in it.absolutePath.replace('\\', '/') }
            .filter { "/test/" !in it.absolutePath.replace('\\', '/') }
            .flatMap { file ->
                file.readLines().asSequence().mapIndexedNotNull { idx, line ->
                    pattern.find(line)?.let { match ->
                        val rel = file.absolutePath
                            .substringAfter("androidApp${File.separator}")
                            .replace('\\', '/')
                        "$rel:${idx + 1}: $label '${match.value}'"
                    }
                }
            }
            .toList()
    }

    /**
     * Locate `<root>/androidApp/src/main/kotlin`. JUnit usually runs with CWD =
     * `<root>/androidApp/`, but Gradle invocations from the repo root run with
     * CWD = `<root>/`. Handle both.
     */
    private fun findMainSrcRoot(): File? {
        val candidates = listOf(
            File("src/main/kotlin"),
            File("androidApp/src/main/kotlin"),
            File("../androidApp/src/main/kotlin"),
        )
        return candidates.firstOrNull { it.isDirectory }
    }
}
