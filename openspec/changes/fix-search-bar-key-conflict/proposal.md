## Why

在 `PokemonTerminalScreen` 搜尋欄輸入文字時，按鍵事件未被攔截，導致符合 Minecraft 按鍵綁定的按鍵（如 `E` 關閉背包、`W`/`A`/`S`/`D` 移動）觸發對應動作而關閉畫面。根本原因是 `PokemonTerminalScreen` 未 override `keyPressed` 和 `charTyped`，未在搜尋欄取得焦點時消耗按鍵事件。

## What Changes

- 在 `PokemonTerminalScreen` override `keyPressed`：當搜尋欄有焦點時，將按鍵事件優先交給搜尋欄處理，並消耗事件（Escape 除外，Escape 應仍可關閉畫面）
- 在 `PokemonTerminalScreen` override `charTyped`：當搜尋欄有焦點時，將字符事件轉發給搜尋欄

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
<!-- This is a bug fix only — no spec-level requirement changes -->

## Impact

- 僅影響 `PokemonTerminalScreen.java`（client-side UI）
- 不影響 Menu、網路封包、或資料模型
