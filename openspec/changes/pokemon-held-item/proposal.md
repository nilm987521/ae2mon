## Why

Terminal 目前只能在 Detail 面板顯示 Pokemon 攜帶物的圖示，無法直接在 terminal 內進行物品的裝備與替換。玩家必須先提取 Pokemon 到 Party、手動操作攜帶物、再存回 ME，流程繁瑣。新增玩家 inventory 顯示與攜帶物 slot 交互，讓玩家能直接在 terminal 視窗內完成攜帶物的裝備、卸除與替換。

## What Changes

- Terminal 畫面下方新增 **玩家 Inventory 區塊**（標準 Minecraft 格式：3行×9 格 + 1行 hotbar）
- Detail 面板中的攜帶物顯示從圖示升級為可交互的 **Held Item Slot**
- 玩家可點擊 Held Item Slot，再點擊 Inventory 中的物品，完成攜帶物裝備或替換
- 玩家可點擊 Held Item Slot 卸除攜帶物（將物品放回 inventory）
- 支援 Party Pokemon 與 Network Pokemon 兩種情境
- 畫面高度隨 Inventory 區塊增加
- `PokemonTerminalMenu` 新增玩家 inventory Slot 物件（server 端管理），並支援 Held Item 相關操作的新 packet

## Capabilities

### New Capabilities
- `pokemon-held-item-management`: 在 terminal 內顯示玩家 inventory、Pokemon 攜帶物 slot，並支援物品裝備 / 替換 / 卸除操作

### Modified Capabilities
- `pokemon-drag-drop-interaction`: 無需修改（held item 操作為點擊而非拖拉，與現有拖拉不衝突）

## Impact

- `PokemonTerminalMenu` — 新增 36 個玩家 inventory Slot（27 main + 9 hotbar），新增 `equipHeldItem` / `unequipHeldItem` server 端方法
- 新增 `EquipHeldItemPayload` packet（client → server）：`(containerId, pokemonUUID / partySlot, slotIndex)`
- 新增 `UnequipHeldItemPayload` packet（client → server）：`(containerId, pokemonUUID / partySlot)`
- `SyncPokemonListPayload` — 無需修改（held item 資訊已在 `heldItem` 欄位）
- `PokemonTerminalScreen` — 新增 Inventory 渲染區塊、Held Item Slot 交互邏輯、畫面高度調整
- 對 Network Pokemon 的 held item 修改需更新 ME cell 中的 NBT，並重新整理 `availableKeys`
