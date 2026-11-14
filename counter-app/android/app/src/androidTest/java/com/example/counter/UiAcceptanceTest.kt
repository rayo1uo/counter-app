package com.example.counter
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.counter.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiAcceptanceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun countUndoBatchHistoryAndStats() {
        val app = rule.activity.application as CounterApp
        runBlocking {
            app.container.repository.clearAll()
            app.container.settings.setSound(false)
            app.container.settings.setHaptic(false)
            app.container.settings.setDark(false)
            app.container.settings.setCountMode(CountMode.DAILY)
            app.container.repository.upsertCounter(CounterEntity("ui-check", "检车", 0xFFF5921E, category="工作", target=30.0, pinned=true))
            app.container.repository.upsertCounter(CounterEntity("ui-truck", "大型车", 0xFF3B82F6, category="工作", sortOrder=1))
            app.container.repository.upsertCounter(CounterEntity("ui-coffee", "Coffee", 0xFF34A078, category="生活", sortOrder=2))
        }
        rule.waitUntil(10000) { rule.onAllNodesWithTag("plus-ui-check").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("plus-ui-check").performClick()
        rule.waitUntil(5000) { runBlocking { app.container.dao.allEntries().size == 1 } }
        rule.onNodeWithTag("value-ui-check", useUnmergedTree=true).assertTextEquals("1")
        rule.onNodeWithTag("minus-ui-check").performClick()
        rule.waitUntil(5000) { runBlocking { app.container.dao.allEntries().size == 2 } }
        rule.waitForIdle()
        rule.onNodeWithText("撤销").performClick()
        rule.waitUntil(5000) { runBlocking { app.container.dao.allEntries().size == 1 } }
        rule.onNodeWithTag("value-ui-check", useUnmergedTree=true).assertTextEquals("1")
        rule.onNodeWithTag("plus-ui-check").performTouchInput { longClick() }
        rule.onNodeWithText("记录 · 检车").assertIsDisplayed()
        rule.onNodeWithText("本次增量（可填负数）").performTextReplacement("12")
        rule.onNodeWithText("备注（可选）").performTextInput("批量验收")
        rule.onNodeWithText("记录", useUnmergedTree=true).performClick()
        rule.waitUntil(5000) { runBlocking { app.container.dao.allEntries().size == 2 } }
        rule.onNodeWithTag("value-ui-check", useUnmergedTree=true).assertTextEquals("13")
        rule.onNodeWithTag("value-ui-check", useUnmergedTree=true).performTouchInput { click() }
        rule.onNodeWithText("批量验收").assertIsDisplayed()
        rule.onNodeWithContentDescription("统计").performClick()
        rule.onNodeWithText("周期净增量").assertIsDisplayed()
        rule.onNodeWithContentDescription("首页").performClick()
        rule.onNodeWithTag("value-ui-check", useUnmergedTree=true).assertTextEquals("13")
    }
}
