## Why

Party UI 目前在每個欄位顯示個性（Nature），此資訊在 Party 欄位中相對次要，且佔用了有限的行高空間。同時，Party 欄位缺少與 Network 清單一致的 hover 高亮效果，使兩個互動區塊的視覺回饋不一致。

## What Changes

- 移除 Party 欄位中每個 slot 的個性（Nature）文字顯示（`PokemonTerminalScreen.java` 第 408 行）
- 新增 hover 高亮效果至 Party 欄位中有 Pokemon 的 slot，沿用現有 `COL_HOVER = 0x33FFFFFF` 常數，與 Network 清單的 hover 行為一致

## Capabilities

### New Capabilities
- `party-ui-display`: Party 欄位的顯示與互動規格——定義每個 slot 的渲染內容與 hover 狀態行為

### Modified Capabilities
（無）

## Impact

- `PokemonTerminalScreen.java` — Party 渲染區塊（`render()` 方法內 party loop，約第 388–412 行）
