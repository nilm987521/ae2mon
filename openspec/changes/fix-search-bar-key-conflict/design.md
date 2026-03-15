## Context

`PokemonTerminalScreen` 繼承 `AbstractContainerScreen`，但未 override `keyPressed` 和 `charTyped`。當搜尋欄（`EditBox`）有焦點時，按鍵事件仍冒泡至父類，觸發 Minecraft 按鍵綁定（如 `E` 關閉 inventory），導致畫面被意外關閉。

## Goals / Non-Goals

**Goals:**
- 搜尋欄有焦點時，按鍵事件優先由搜尋欄消耗，不觸發 Minecraft 快捷鍵
- Escape 鍵仍可正常關閉畫面

**Non-Goals:**
- 不修改 Menu、網路封包、或資料模型
- 不更動搜尋邏輯本身

## Decisions

### Override keyPressed to intercept keys when search is focused

在 `PokemonTerminalScreen` override `keyPressed(int keyCode, int scanCode, int modifiers)`：
1. 若搜尋欄有焦點且 `searchBox.keyPressed(...)` 消耗了事件 → return true
2. 若按下 Escape → 交給 super 處理（正常關閉）
3. 若搜尋欄有焦點但 EditBox 未消耗（例如方向鍵）→ return true 仍消耗，避免觸發快捷鍵
4. 否則 → 交給 super 正常處理

同樣 override `charTyped(char codePoint, int modifiers)`：搜尋欄有焦點時轉發字元，否則交給 super。

## Risks / Trade-offs

- [Risk] 過度攔截方向鍵可能影響清單捲動 → 可接受，未來如需方向鍵捲動再個別處理

## Migration Plan

單一檔案修改，無 migration 需求。
