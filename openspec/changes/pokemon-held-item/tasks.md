## 1. Menu — 加入 Player Inventory Slots

- [ ] 1.1 在 `PokemonTerminalMenu` constructor 中加入 27 個 main inventory Slot（slot index 0–26）
- [ ] 1.2 在 `PokemonTerminalMenu` constructor 中加入 9 個 hotbar Slot（slot index 27–35）
- [ ] 1.3 確認 client-side factory `forPart()` 同樣加入相同 Slot（讓 Minecraft cursor 系統正常運作）

## 2. Packet — SetHeldItemPayload

- [ ] 2.1 建立 `SetHeldItemPayload` record：`(int containerId, @Nullable UUID networkUUID, int partySlot)`
- [ ] 2.2 實作 `SetHeldItemPayload.STREAM_CODEC`（encode / decode）
- [ ] 2.3 在 `ModPayloads.onRegisterPayloads()` 中註冊 `SetHeldItemPayload`（playToServer）

## 3. Server 端 Held Item 邏輯

- [ ] 3.1 在 `PokemonTerminalMenu` 新增 `setHeldItem(@Nullable UUID networkUUID, int partySlot, ServerPlayer player)` 方法
- [ ] 3.2 實作 **Party Pokemon** 分支：讀取 `menu.getCarried()`，與 `pokemon.heldItem()` 交換，呼叫 `menu.setCarried(oldHeld)`
- [ ] 3.3 實作 **Network Pokemon** 分支：找到 `PokemonKey`（by UUID）→ extract → 修改 NBT held item → 建立新 `PokemonKey` → insert 新 key → rollback 若 insert 失敗
- [ ] 3.4 在 `ModPayloads` 的 `SetHeldItemPayload` handler 中呼叫 `menu.setHeldItem()`，操作完成後呼叫 `sendSyncToPlayer()`

## 4. Client 畫面 — Inventory 區塊

- [ ] 4.1 調整 `SCREEN_HEIGHT` 以容納 inventory 區塊（目前 232 → 約 320）
- [ ] 4.2 計算 inventory 起始 Y（`PANEL_Y + PANEL_H + 8`）並定義相關常數
- [ ] 4.3 在 `render()` 中繪製 inventory 背景 panel
- [ ] 4.4 呼叫標準 `renderSlot()` 系列方法（或手動 `graphics.renderItem()`）渲染 36 個 inventory slot 格子與物品

## 5. Client 畫面 — Held Item Slot Widget

- [ ] 5.1 在 `renderDetailPanel()` 中將現有 held item 圖示升級為帶邊框的可交互 widget（固定位置，單一 16×16 格）
- [ ] 5.2 當游標懸停於 Held Item widget 上時顯示 tooltip（物品名稱，或「Empty」）
- [ ] 5.3 在 `mouseClicked()` 中偵測 Held Item widget 點擊：若選取中的 Pokemon 存在，傳送 `SetHeldItemPayload` 至 server
- [ ] 5.4 確保 Held Item widget 點擊判斷優先於 drag 啟動邏輯，避免衝突
