## Context

`PokemonTerminalScreen` 已有 `selectedPartySlot` 欄位（int，-1 為未選取），並在 `mouseClicked()` 中於點擊有 Pokemon 的 Party slot 時設定。`renderDetailPanel()` 也已使用此欄位顯示 detail 內容。

然而 Party panel 的 render 迴圈（`render()` 約第 389–417 行）只處理了 hover（`COL_HOVER`）與 drag 中（`0x44FFFFFF`）的高亮，**沒有** 選取狀態的高亮，導致點擊 Party slot 後 Detail panel 有內容但 Party panel 本身無視覺回饋。

Network list 的對應邏輯已正確實作：
```java
if (selected) {
    graphics.fill(..., COL_SELECTED);
} else if (hovered) {
    graphics.fill(..., COL_HOVER);
}
```

## Goals / Non-Goals

**Goals:**
- Party slot 被選取時，顯示 `COL_SELECTED`（`0x66A0A0FF`）高亮，與 Network list 一致
- 優先級：drag 中 > 選取 > hover（不改變現有優先序，僅插入選取層）

**Non-Goals:**
- 不新增任何欄位或狀態機
- 不修改 `mouseClicked()` 邏輯（選取行為已正確）
- 不修改 Network list render 邏輯

## Decisions

### Party slot 選取高亮插入 drag/hover 條件之間

現有條件鏈：
```
isDragging && dragIndex == slot  →  0x44FFFFFF (drag dim)
partyHovered                     →  COL_HOVER
```

修改後：
```
isDragging && dragIndex == slot  →  0x44FFFFFF (drag dim)
selectedPartySlot == slot        →  COL_SELECTED
partyHovered                     →  COL_HOVER
```

只需在 `else if (partyHovered)` 前插入一個 `else if`，改動極小。

## Risks / Trade-offs

- 改動範圍極小（1個 `else if` 分支），風險可忽略
