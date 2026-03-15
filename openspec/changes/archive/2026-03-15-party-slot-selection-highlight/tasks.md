## 1. 實作 Party slot 選取高亮

- [x] 1.1 在 `PokemonTerminalScreen.render()` 的 Party slot 渲染迴圈中，依照 design 中「Party slot 選取高亮插入 drag/hover 條件之間」決策，於 drag-active 條件後、hover 條件前插入 `else if (selectedPartySlot == slot)` 分支，填入 `COL_SELECTED`，實作 Party slot selection highlight（選取高亮優先序：drag > selection > hover）

## 2. 驗收

- [x] 2.1 在遊戲中驗收：點擊 Party slot → 該 slot 顯示 `COL_SELECTED` 藍色高亮，Detail panel 同步顯示 Pokemon 資訊
- [x] 2.2 確認未選取的 slot hover 時仍顯示 `COL_HOVER`（白色半透明），選取 slot hover 時保持 `COL_SELECTED`（drag-active highlight takes priority over selection）
