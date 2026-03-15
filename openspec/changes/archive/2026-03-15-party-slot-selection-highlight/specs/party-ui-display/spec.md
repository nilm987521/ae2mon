## ADDED Requirements

### Requirement: Party slot selection highlight
A Party slot that is currently selected (i.e., `selectedPartySlot` equals the slot index) SHALL display a persistent selection highlight using `COL_SELECTED` (`0x66A0A0FF`), covering the full slot area. The selection highlight SHALL be rendered with lower priority than the drag-active highlight and higher priority than the hover highlight.

#### Scenario: Selected slot displays highlight
- **WHEN** the player has clicked a Party slot containing a Pokemon (setting `selectedPartySlot`) and is not currently dragging that slot
- **THEN** that slot's background SHALL be filled with `COL_SELECTED` (`0x66A0A0FF`)

#### Scenario: Drag-active highlight takes priority over selection
- **WHEN** `isDragging == true` AND `dragSource == PARTY` AND `dragIndex == slot`
- **THEN** the slot SHALL display the drag-dim highlight (`0x44FFFFFF`), NOT `COL_SELECTED`

#### Scenario: Hover highlight does not override selection
- **WHEN** the mouse is hovering over a selected Party slot and the slot is not being dragged
- **THEN** the slot SHALL display `COL_SELECTED`, NOT `COL_HOVER`

#### Scenario: Unselected slot shows hover highlight
- **WHEN** the mouse is hovering over a Party slot that is NOT selected and NOT being dragged
- **THEN** the slot SHALL display `COL_HOVER` (`0x33FFFFFF`)
