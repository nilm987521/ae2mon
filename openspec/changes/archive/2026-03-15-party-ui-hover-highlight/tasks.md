## 1. 移除 Party slot 個性顯示

- [x] 1.1 刪除 `PokemonTerminalScreen.java` party loop 中渲染 Nature 文字的那一行（`formatNature(pe.nature())`，約第 408 行）
- [x] 1.2 調整 sprite 與名稱文字的垂直位置，使其在 30px slot 高度內視覺置中

## 2. 新增 Party slot hover 高亮

- [x] 2.1 在 party loop 中，對每個有 Pokemon 的 slot 計算 hover 狀態（mouseX/mouseY 是否落在 slot 範圍內）
- [x] 2.2 當 hover 成立且未拖曳（`!isDragging` 或 `dragSource != PARTY || dragIndex != slot`）時，在 sprite/文字繪製前 fill `COL_HOVER`
- [x] 2.3 確認拖曳中的來源 slot 仍只顯示拖曳高亮（`0x44FFFFFF`），不疊加 hover 高亮

## 3. 調整 Party row 高度

- [x] 3.1 將 `PARTY_ENTRY_H` 從 30 改為 23
- [x] 3.2 調整 party loop 中 sprite Y（`ey + ENTRY_HEIGHT / 2 - 5`）與文字 Y（`ey + (ENTRY_HEIGHT - 9) / 2`）使用新高度

## 4. Detail 面板支援 Party Pokemon

- [x] 4.1 新增 `selectedPartySlot` 欄位（int，預設 -1）
- [x] 4.2 點擊 Party slot 時設定 `selectedPartySlot` 並清除 `selectedUUID`；點擊 Network list 時清除 `selectedPartySlot`
- [x] 4.3 `renderDetailPanel()` 支援從 `PartyEntry` 取資料渲染（`selectedPartySlot >= 0` 時使用）
