## Why

目前所有 Cell 的 Menu（PokemonTerminalScreen、PortableCellTerminalMenu）透過點擊 "Withdraw" / "Deposit" 按鈕來進行存取，操作不直覺且需要先選取再按鈕的兩步驟流程。改成拖拉操作後，行為更符合 Minecraft 原生 UI 習慣，同時可縮小 Menu 尺寸，為未來顯示玩家背包（Inventory）預留空間。

## What Changes

- **移除** Detail panel 的 "Withdraw" 按鈕
- **移除** Party panel 各欄位的 "Deposit" 按鈕（共 6 個）
- **新增** 拖拉互動：
  - 從 **Party panel** 拖拉 Pokemon → 放到 **Network list / storage 區域** = deposit（存入網路）
  - 從 **Network list** 拖拉 Pokemon → 放到 **Party panel** = withdraw（領取到隊伍）
- **縮小** Menu 尺寸（移除按鈕後節省的垂直空間）
- 兩個 menu 都要改：`PokemonTerminalScreen`（Block/Part terminal）與 `PortableCellTerminalMenu` / 其 Screen（Portable Cell）

## Capabilities

### New Capabilities
- `pokemon-drag-drop-interaction`: 定義拖拉操作的行為規格，包含拖拉來源、放置目標、視覺回饋（hover highlight）、以及錯誤狀況（隊伍滿了、Cell 已滿）的處理方式

### Modified Capabilities
<!-- 目前沒有既有 spec，無需填寫 -->

## Impact

- `PokemonTerminalScreen` — 移除按鈕 widget、新增 mouseClicked / mouseDragged / mouseReleased 拖拉邏輯、調整 SCREEN_HEIGHT 及 layout 常數
- `PokemonTerminalMenu` — `withdrawPokemon` / `depositPokemon` 方法邏輯不變，只是觸發方式從按鈕 → 拖拉事件
- `PortableCellTerminalMenu` — 同上，能量檢查邏輯不變
- 可能新增獨立的 `PortableCellTerminalScreen`（若目前共用同一 Screen class）
- Network packet（`DepositPokemonPayload` / `WithdrawPokemonPayload`）不需要修改
