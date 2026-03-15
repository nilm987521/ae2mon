## Context

`PokemonTerminalScreen` 的 Party 渲染區塊位於 `render()` 方法中的 party loop（約第 388–412 行）。目前每個有 Pokemon 的 slot 渲染兩行文字：第一行為名稱 + 性別 + shiny 星號，第二行為個性（Nature）。Network 清單已有完整的 hover 高亮邏輯（`COL_HOVER = 0x33FFFFFF`），Party 欄位則只有拖曳中的來源 slot 有半透明白色高亮，無靜態 hover 效果。

此修改範圍僅限單一 client-side 渲染檔案，不涉及網路封包、伺服器邏輯或資料模型。

## Goals / Non-Goals

**Goals:**
- 移除 Party slot 中的個性（Nature）文字行，需適當調整sprite的位置
- 對有 Pokemon 的 Party slot 加入 mouse hover 高亮，沿用 `COL_HOVER` 常數
- 將Party的Pokemon row 的高度挑整成跟Network一樣
- 中間顯示狀態的區塊，不限定只顯示Network中的Pokemon，需要也能夠顯示Party的

**Non-Goals:**
- 不調整 Network 清單或 Detail 面板的既有渲染邏輯（僅擴充 Detail 支援 Party 來源）
- 不修改網路同步資料結構（`PartyEntry` 已包含完整欄位，無需擴充）

## Decisions

**D1：hover 判斷方式 — 沿用同檔案現有模式**

Network 清單的 hover 判斷在 `render()` 迴圈內直接以 `mouseX/mouseY` 參數比對 slot 的螢幕座標。Party loop 已在同一 `render()` 呼叫中，可直接套用相同模式，無需引入額外欄位或 widget。

替代方案考量：引入 `hoveredPartySlot` 欄位並在 `mouseMoved()` 更新 — 不必要的複雜度，對於純渲染判斷不需要跨幀狀態。

**D2：高亮層的繪製順序**

高亮 fill（`COL_HOVER`）繪製於 sprite 與文字之前（與 Network 清單相同）。若 slot 正在拖曳中（`isDragging && dragSource == PARTY && dragIndex == slot`），則改用現有的拖曳高亮（`0x44FFFFFF`），不疊加 hover 高亮，避免雙重效果。

**D3：移除 Nature 後的空間處理**

`PARTY_ENTRY_H = 30` 維持不變。移除 Nature 文字後，slot 內會有額外的垂直空間。第一行文字（名稱 + 性別 + shiny）垂直置中於 30px 高度（`ey + 10` 或類似偏移）以維持視覺平衡。具體偏移量以實際渲染效果為準。

**D4：Party row 高度改為 23px**

`PARTY_ENTRY_H` 從 30 改為 23，與 Network 清單的 `ENTRY_HEIGHT` 一致。sprite 位置與文字 Y 偏移需對應調整，沿用 Network 清單的 `ey + ENTRY_HEIGHT / 2 - 5`（sprite）與 `ey + (ENTRY_HEIGHT - 9) / 2`（文字）比例。

**D5：Detail 面板顯示 Party Pokemon**

新增 `selectedPartySlot` 欄位（int，-1 = 未選取）。點擊 Party slot 時設定 `selectedPartySlot` 並清除 `selectedUUID`（反之亦然），兩者互斥。`renderDetailPanel()` 接受一個統一的資料介面：當 `selectedPartySlot >= 0` 時從 `partyList` 取 `PartyEntry`，將其欄位對應至 Detail 渲染邏輯。`PartyEntry` 已有完整欄位（IVs/EVs/ability/types/heldItem），無需擴充 payload。Detail 面板中 Party Pokemon 不顯示 withdraw 提示文字（改為「from party」說明文字）。

## Risks / Trade-offs

- **[視覺空白]** 移除 Nature 後 30px slot 高度內只剩一行文字，可能顯得稀疏 → 可將文字行垂直置中補償，或未來考慮加入其他資訊（ability、HP 等）填補空間；目前先以置中處理。
- **[hover 在拖曳時的狀態]** 拖曳進行中仍會觸發 hover 判斷 → 以 `isDragging` 旗標區分，拖曳中優先顯示拖曳高亮而非 hover 高亮。
