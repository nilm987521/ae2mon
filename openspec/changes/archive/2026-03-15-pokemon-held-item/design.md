## Context

`PokemonTerminalMenu` 目前繼承 `AbstractContainerMenu`，但 **未加入任何玩家 inventory Slot**（`quickMoveStack` 直接回傳 `ItemStack.EMPTY`）。畫面採全自訂渲染，不使用 `AbstractContainerScreen` 的預設 slot 渲染。

Detail 面板目前以一個 16×16 圖示展示 held item，但僅供顯示，不可交互。

Held item 有兩種資料來源：
- **Party Pokemon**：live `Pokemon` 物件，可直接透過 Cobblemon API 讀寫
- **Network Pokemon**：以 NBT 儲存於 ME cell，修改後需重建 `PokemonKey` 並重新存入 storage

## Goals / Non-Goals

**Goals:**
- Terminal 下方顯示玩家完整 inventory（27 main + 9 hotbar）
- Detail 面板的 held item 圖示升級為可交互的 Held Item Slot widget
- 玩家可透過「cursor 交換」模式裝備、替換、卸除 Pokemon 攜帶物

**Non-Goals:**
- 不支援 shift-click 自動裝備
- 不支援從 network list 直接拖拉物品給 Pokemon
- Portable Cell 模式暫不支援（`PortableCellTerminalMenu` 不在本次範圍）

## Decisions

### 1. Inventory Slot 加入 Menu（Server 端標準 Slot）

**選擇：** 在 `PokemonTerminalMenu` 的 server 端 constructor（`host != null`）加入玩家 inventory 的 27 + 9 = 36 個標準 `Slot`。Client 端 factory（`forPart`）同樣加入，讓 Minecraft 的 cursor / carried item 系統正常運作。

**理由：**
- 標準 Minecraft Slot 系統自動處理 cursor 管理、封包同步、item 驗證
- Server 端可透過 `menu.getCarried()` 取得玩家游標上的物品，不需額外 packet 傳遞物品內容
- 不需自訂 item 同步邏輯

**Slot index 配置：**
```
slots 0–26  : player main inventory (row 0-2, col 0-8)
slots 27–35 : player hotbar (col 0-8)
```
（與標準 Minecraft chest UI 相同順序）

---

### 2. Held Item 交互採「Cursor 交換」模式

**選擇：** Held Item Slot 為 **client 端自訂 widget**（非標準 Minecraft Slot）。交互流程：

1. 玩家用游標拿起 inventory 中的物品（標準 Slot 點擊）
2. 玩家點擊 Detail 面板的 Held Item widget
3. Client 傳送 `SetHeldItemPayload(containerId, networkUUID / partySlot)`
4. Server 讀取 `menu.getCarried()`（游標物品），與 Pokemon 當前 held item 交換：
   - 舊 held item → `menu.setCarried(oldHeld)`（回到游標）
   - 游標物品 → Pokemon held item
5. Server 呼叫 `sendSyncToPlayer()` 更新 client
6. 玩家把游標上的舊 held item 放回 inventory（標準操作）

**卸除（Unequip）流程：** 游標為空時點擊 Held Item widget：
- Server：`menu.setCarried(oldHeld)`，Pokemon held item 清空

**理由：**
- 利用 Minecraft 原生 cursor 系統，不需額外封包傳遞物品 NBT
- 與玩家操作 chest / furnace 等 UI 的直覺一致
- Server 端邏輯簡單：一次讀一次寫

---

### 3. Network Pokemon Held Item 更新方式

**選擇：** Extract old key → modify NBT → create new PokemonKey → insert new key。

```
1. 找到 PokemonKey (by UUID)
2. 從 storage extract（MODULATE）
3. data.remove("HeldItem") 或 data.put("HeldItem", newItemNBT)
4. 以修改後的 data 建立新 PokemonKey（UUID 不變）
5. insert 新 key 到 storage
6. refreshAvailableKeys()
```

**理由：** `PokemonKey` 包含完整 NBT，held item 是 NBT 的一部分，無法 in-place 修改。必須先 extract 再 insert。UUID 保持不變，AE2 storage 視為不同 key（因 NBT 不同），這符合現有架構。

**Rollback：** 若 insert 失敗（cell 滿）→ 將原 key re-insert，操作取消。

---

### 4. 畫面高度擴展

新增 inventory 區塊後畫面高度增加：
```
inventory 3 rows × 18px = 54px
hotbar 1 row × 18px      = 18px
gap between rows          =  4px
top/bottom padding        = 12px
total additional          = 88px → SCREEN_HEIGHT ≈ 320px
```

三個主要 panel（Network / Detail / Party）位置不變，`SCREEN_HEIGHT` 調整。Inventory 繪製起始 Y ≈ `PANEL_Y + PANEL_H + 8`。

---

### 5. Packet 設計

新增一個 packet 涵蓋所有 held item 操作：

```java
SetHeldItemPayload(int containerId, @Nullable UUID networkUUID, int partySlot)
```
- `networkUUID != null` → 目標為 network Pokemon
- `partySlot >= 0` → 目標為 party Pokemon（兩者互斥）
- 不傳遞物品本身，server 從 `menu.getCarried()` 取得

## Risks / Trade-offs

- **Network Pokemon insert 失敗** → held item 交換失敗，rollback 原 key。Mitigation：失敗時不修改游標，通知 client 操作取消（重新 sync）。
- **Cursor 物品遺失風險** → 若 server 端 swap 成功但 sync 延遲，client 可能顯示不一致。Mitigation：操作後立即呼叫 `sendSyncToPlayer()`。
- **`quickMoveStack` 未實作** → shift-click 在 inventory slot 上目前不做任何事。本次暫不實作（Non-Goal），但加入 inventory Slot 後需確保 shift-click 不會造成 item 消失（回傳 `ItemStack.EMPTY` 即安全）。
- **Held Item widget 與拖拉系統衝突** → Held Item widget 為靜態區域，不參與現有 Pokemon 拖拉邏輯。需在 `mouseClicked` 中確保 held item 點擊優先判斷，避免與 drag 啟動邏輯衝突。

## Migration Plan

1. `PokemonTerminalMenu` — 加入 36 個 player inventory Slot，更新 `quickMoveStack`（仍回傳 EMPTY 即可）
2. 新增 `SetHeldItemPayload` record 與 `StreamCodec`
3. 在 `ModPayloads` 註冊 `SetHeldItemPayload` handler，實作 server 端 swap 邏輯
4. `PokemonTerminalMenu` — 加入 `setHeldItem(uuid/partySlot, ServerPlayer)` 方法
5. `PokemonTerminalScreen` — 新增 inventory 渲染區塊，調整 `SCREEN_HEIGHT`
6. `PokemonTerminalScreen` — 將 Detail held item 圖示升級為可點擊 widget，加入 `mouseClicked` 處理
