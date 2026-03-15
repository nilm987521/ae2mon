## ADDED Requirements

### Requirement: Party slot hover highlight
有 Pokemon 的 Party slot，當滑鼠懸停其上時，SHALL 顯示半透明白色高亮（`COL_HOVER = 0x33FFFFFF`），覆蓋整個 slot 區域（`PARTY_X+1` 至 `PARTY_X+PANEL_W-1`，高度 `PARTY_ENTRY_H`）。高亮 SHALL 繪製於 sprite 與文字之下。

#### Scenario: 滑鼠懸停於有 Pokemon 的 slot
- **WHEN** 滑鼠座標位於某個有 Pokemon 的 Party slot 範圍內，且未進行拖曳操作（`isDragging == false`）
- **THEN** 該 slot 背景顯示 `COL_HOVER` 半透明高亮

#### Scenario: 拖曳進行中不顯示 hover 高亮
- **WHEN** 正在拖曳（`isDragging == true`）且拖曳來源為該 Party slot（`dragSource == PARTY && dragIndex == slot`）
- **THEN** 顯示拖曳高亮（`0x44FFFFFF`），不疊加 hover 高亮

#### Scenario: 空 slot 不顯示高亮
- **WHEN** 滑鼠懸停於沒有 Pokemon 的 Party slot
- **THEN** 不顯示任何高亮效果

## REMOVED Requirements

### Requirement: Party slot 顯示個性（Nature）
**Reason**: 個性資訊在 Party 欄位中次要，佔用有限的行高空間，已在 Detail 面板中顯示。
**Migration**: 個性資訊仍可透過 Network 清單選取 Pokemon 後在 Detail 面板查閱；`PartyEntry` 資料結構保留 `nature` 欄位不移除。

#### Scenario: Party slot 不再渲染個性文字
- **WHEN** Party slot 有 Pokemon
- **THEN** slot 內 SHALL NOT 渲染個性（Nature）文字行
