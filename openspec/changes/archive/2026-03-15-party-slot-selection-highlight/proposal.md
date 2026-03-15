## Why

Party panel 的 Pokemon slot 只有 hover highlight，缺乏點擊選取後的持續高亮，與 Network list 的選取行為不一致，使用者無法從視覺上確認目前選取的 Party Pokemon。

## What Changes

- Party slot 點擊後，顯示持續的選取高亮（與 Network list entry 相同風格）
- 再次點擊同一 slot 或點擊空白處可取消選取
- 選取狀態獨立於 hover 狀態（兩者可同時存在）

## Capabilities

### New Capabilities

（無）

### Modified Capabilities

- `party-ui-display`: 新增 Party slot 點擊選取高亮的需求（Requirement: Party slot selection highlight）

## Impact

- `PokemonTerminalScreen` — 新增 `selectedPartySlot` 欄位，調整 `mouseClicked()` 更新選取狀態，調整 `renderBg()` 繪製選取高亮
