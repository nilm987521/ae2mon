## Context

目前 `PokemonTerminalScreen` 透過 Minecraft 的 `Button` widget 處理 deposit/withdraw：
- Detail panel 底部有一個 "Withdraw" 按鈕（需先在 list 選取 Pokemon）
- Party panel 每格有一個 "Deposit" 按鈕（共 6 個）

這個設計需要兩步驟操作，且按鈕佔用大量垂直空間。
`PortableCellTerminalMenu` 繼承 `PokemonTerminalMenu`，目前共用同一個 `PokemonTerminalScreen`。

## Goals / Non-Goals

**Goals:**
- 將 deposit/withdraw 改為單步驟拖拉操作
- 移除所有 deposit/withdraw 按鈕
- 縮小 Menu 高度，為未來加入玩家 Inventory 預留空間
- 兩個 terminal（Block/Part terminal 與 Portable Cell）共用同一套拖拉邏輯

**Non-Goals:**
- 不在本次實作玩家 Inventory 顯示
- 不改動 packet、server-side menu logic（`withdrawPokemon` / `depositPokemon` 方法不變）
- 不修改 Portable Cell 的能量檢查邏輯

## Decisions

### 1. 拖拉實作方式：純 client-side 狀態機，無新 widget

Minecraft GUI 沒有內建 drag-and-drop 機制。透過覆寫 `AbstractContainerScreen` 的三個 mouse hook 實作：

| Hook | 職責 |
|------|------|
| `mouseClicked` | 若點擊到 Pokemon（network list 或 party），記錄 drag source，進入 `DRAGGING` 狀態 |
| `mouseDragged` | 更新游標位置，繪製跟隨游標的 ghost sprite |
| `mouseReleased` | 判斷放置目標，發送 packet 或取消 |

Screen 內新增欄位：
```java
private enum DragSource { NONE, NETWORK, PARTY }
private DragSource dragSource = DragSource.NONE;
private int        dragIndex  = -1;   // network displayList index 或 party slot
private double     dragX, dragY;      // 目前游標位置
```

**Alternative considered**: 用獨立 `DraggableWidget` 包裝 — 拒絕，因為 Pokemon 不是 widget，且需要存取 screen 的 layout 常數，直接在 screen 內處理最簡單。

### 2. 放置目標判斷（Hit zones）

| 拖拉方向 | 放置目標區域 | 觸發動作 |
|----------|------------|---------|
| Party slot → Network list panel | `LIST_X` ～ `LIST_X + LIST_WIDTH`, `LIST_Y` ～ `LIST_Y + VISIBLE_ENTRIES * ENTRY_HEIGHT` | deposit（`DepositPokemonPayload`） |
| Network list entry → Party panel | `PARTY_X` ～ `PARTY_X + PANEL_W`, `PARTY_Y` ～ `PARTY_Y + PANEL_H` | withdraw（`WithdrawPokemonPayload`） |

拖拉到無效區域放開 → 直接取消，無副作用。

### 3. 視覺回饋

- **Ghost sprite**: 在 `render()` 最後繪製，以游標為中心，scale 略小（例如 12F），半透明（若 Cobblemon API 支援 alpha，否則直接全色）
- **Drop zone highlight**: 當 `dragSource != NONE` 且游標在有效放置區域時，在該面板上疊加一層 `0x3300FF00`（半透明綠）

### 4. 縮小 Menu 尺寸

移除按鈕後，Party panel 每格高度從 44px 縮回 ~30px（移除 16px 按鈕列），整體 `SCREEN_HEIGHT` 預計從 315 降至約 230px。Detail panel 不再需要底部按鈕預留空間。

確切數值在實作時依新 layout 調整，此處為估算。

### 5. 不分離 PortableCellTerminalScreen

目前兩個 menu 共用 `PokemonTerminalScreen`，拖拉邏輯全在 Screen 層，不依賴 menu 的具體類型，因此不需要拆出獨立 Screen class。

Party的區域，每個Pokemon的 sprite 顯示，需要往上放到垂直置中

## Risks / Trade-offs

- **拖拉誤觸** → 只有實際移動超過閾值（如 4px）才進入 DRAGGING 狀態，mouseClicked 仍保留選取 network list entry 的功能
- **Ghost sprite render 順序** → 必須在 `super.render()` 之後、`renderTooltip()` 之前繪製，避免被其他 widget 覆蓋
- **Layout 常數大量調整** → 集中在 screen 頂部的 `static final int` 區塊，改動容易追蹤

## Open Questions

- Cobblemon 的 `drawProfilePokemon` 是否支援 alpha 參數控制透明度？若不支援，ghost sprite 將以全色繪製，視覺上需靠縮小 scale 來區分「正在拖」。
