## 1. 移除按鈕

- [x] 1.1 移除 `PokemonTerminalScreen.init()` 中的 "Withdraw" 按鈕（`addRenderableWidget` 呼叫）
- [x] 1.2 移除 `PokemonTerminalScreen.init()` 中的 6 個 "Deposit" 按鈕（party slot loop）
- [x] 1.3 移除 `onWithdraw()` 和 `onDeposit(int slot)` 方法

## 2. 縮小 Layout

- [x] 2.1 調整 `SCREEN_HEIGHT` 常數（315 → 232px）
- [x] 2.2 調整 `PARTY_ENTRY_H` 常數（44 → 30px，移除按鈕佔用的空間）
- [x] 2.3 調整 `PANEL_H` 和 `DETAIL_H` 常數以符合新高度（288 → 205）
- [x] 2.4 確認 party panel、detail panel、list panel 在新尺寸下排版正常

## 3. 拖拉狀態機

- [x] 3.1 在 `PokemonTerminalScreen` 新增 drag 狀態欄位：`DragSource dragSource`（enum: NONE/NETWORK/PARTY）、`int dragIndex`、`double dragX`、`double dragY`
- [x] 3.2 新增 `double dragStartX`、`double dragStartY` 用於計算拖拉閾值（4px）
- [x] 3.3 覆寫 `mouseClicked`：若點擊 Party panel 的有 Pokemon 的 slot → 記錄 `PARTY` drag source；若點擊 Network list entry → 記錄 `NETWORK` drag source
- [x] 3.4 覆寫 `mouseDragged`：更新 `dragX`、`dragY`；若移動距離 ≥ 4px 則確認進入拖拉狀態
- [x] 3.5 覆寫 `mouseReleased`：判斷 drop zone（Network panel 或 Party panel），發送對應 packet，最後清除 drag 狀態

## 4. 視覺回饋

- [x] 4.1 在 `render()` 末端（`renderTooltip` 之前）繪製 ghost sprite：使用 `dragX`、`dragY` 為中心，scale=12F
- [x] 4.2 在 `renderBg()` 中，若 drag 中且游標在有效 drop zone，疊加 `0x3300FF00` 半透明遮罩於目標 panel

## 5. 測試與驗收

- [ ] 5.1 在遊戲中測試：從 Party 拖拉 Pokemon 到 Network list → Pokemon 成功 deposit，party 更新
- [ ] 5.2 在遊戲中測試：從 Network list 拖拉 Pokemon 到 Party panel → Pokemon 成功 withdraw，network 更新
- [ ] 5.3 測試拖拉到無效區域放開 → 無任何操作，UI 恢復正常
- [ ] 5.4 測試點擊 Network list entry（短按不拖）→ 仍正常選取，不觸發 withdraw
- [ ] 5.5 使用 Portable Cell 開啟 menu，重複上述測試，確認能量消耗邏輯正常
