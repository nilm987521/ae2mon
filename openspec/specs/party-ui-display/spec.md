# Capability: party-ui-display

## Purpose

Party UI 的顯示行為規格，涵蓋 Party slot 的視覺呈現，包含互動高亮效果等。

## Requirements

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
