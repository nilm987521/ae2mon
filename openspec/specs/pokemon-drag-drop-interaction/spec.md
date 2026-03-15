# pokemon-drag-drop-interaction Specification

## Purpose

TBD - created by archiving change 'pokemon-cell-drag-drop'. Update Purpose after archive.

## Requirements

### Requirement: Drag Pokemon from party to deposit
玩家可以從 Party panel 將 Pokemon 拖拉至 Network storage 區域，完成 deposit 操作。

#### Scenario: 成功 deposit
- **WHEN** 玩家在 Party panel 的某個 slot 按下滑鼠左鍵並拖拉至 Network list panel，然後放開
- **THEN** 系統 SHALL 向 server 發送 `DepositPokemonPayload`，Pokemon 從隊伍移入網路 storage，Party panel 更新顯示

#### Scenario: 拖拉至無效區域
- **WHEN** 玩家從 Party panel 拖拉 Pokemon 但放開位置不在 Network list panel 範圍內
- **THEN** 系統 SHALL 取消拖拉，不觸發任何 deposit，UI 恢復原狀

#### Scenario: 拖拉空的 party slot
- **WHEN** 玩家嘗試拖拉一個沒有 Pokemon 的 party slot
- **THEN** 系統 SHALL 不進入拖拉狀態


<!-- @trace
source: pokemon-cell-drag-drop
updated: 2026-03-15
code:
  - src/main/resources/data/ae2mon/recipe/portable_pokemon_cell.json
-->

---
### Requirement: Drag Pokemon from network to withdraw
玩家可以從 Network list 將 Pokemon 拖拉至 Party panel，完成 withdraw 操作。

#### Scenario: 成功 withdraw
- **WHEN** 玩家在 Network list 的某個 entry 按下滑鼠左鍵並拖拉至 Party panel，然後放開
- **THEN** 系統 SHALL 向 server 發送 `WithdrawPokemonPayload`，Pokemon 從網路 storage 加入玩家隊伍，Network list 更新顯示

#### Scenario: 拖拉至無效區域
- **WHEN** 玩家從 Network list 拖拉 Pokemon 但放開位置不在 Party panel 範圍內
- **THEN** 系統 SHALL 取消拖拉，不觸發任何 withdraw，UI 恢復原狀


<!-- @trace
source: pokemon-cell-drag-drop
updated: 2026-03-15
code:
  - src/main/resources/data/ae2mon/recipe/portable_pokemon_cell.json
-->

---
### Requirement: Ghost sprite 跟隨游標
在拖拉過程中，系統 SHALL 繪製一個縮小的 Pokemon sprite 跟隨游標移動，提示使用者正在進行拖拉。

#### Scenario: 拖拉中顯示 ghost
- **WHEN** 玩家進入拖拉狀態（已按下並移動）
- **THEN** 系統 SHALL 在游標位置繪製被拖拉 Pokemon 的 sprite（scale ≤ 12F），且不影響其他 panel 的正常渲染

#### Scenario: 拖拉結束清除 ghost
- **WHEN** 玩家放開滑鼠（無論是否成功 deposit/withdraw）
- **THEN** 系統 SHALL 移除 ghost sprite，恢復正常 UI


<!-- @trace
source: pokemon-cell-drag-drop
updated: 2026-03-15
code:
  - src/main/resources/data/ae2mon/recipe/portable_pokemon_cell.json
-->

---
### Requirement: Drop zone highlight
當玩家正在拖拉且游標位於有效的放置區域時，系統 SHALL 顯示視覺 highlight。

#### Scenario: 有效 drop zone highlight
- **WHEN** 玩家正在拖拉 Pokemon 且游標進入有效目標 panel（Network list 或 Party panel）
- **THEN** 系統 SHALL 在該 panel 上疊加半透明綠色遮罩（例如 `0x3300FF00`）

#### Scenario: 無效區域不 highlight
- **WHEN** 玩家正在拖拉但游標不在任何有效 drop zone
- **THEN** 系統 SHALL 不顯示任何 highlight


<!-- @trace
source: pokemon-cell-drag-drop
updated: 2026-03-15
code:
  - src/main/resources/data/ae2mon/recipe/portable_pokemon_cell.json
-->

---
### Requirement: 移除 deposit / withdraw 按鈕
系統 SHALL 不再在 UI 中顯示任何 deposit 或 withdraw 按鈕。

#### Scenario: Withdraw 按鈕不存在
- **WHEN** 玩家開啟 Pokemon terminal menu
- **THEN** Detail panel 底部 SHALL NOT 顯示 "Withdraw" 按鈕

#### Scenario: Deposit 按鈕不存在
- **WHEN** 玩家開啟 Pokemon terminal menu
- **THEN** Party panel 的每個 slot SHALL NOT 顯示 "Deposit" 按鈕


<!-- @trace
source: pokemon-cell-drag-drop
updated: 2026-03-15
code:
  - src/main/resources/data/ae2mon/recipe/portable_pokemon_cell.json
-->

---
### Requirement: 縮小 Menu 高度
移除按鈕後，系統 SHALL 縮小 Menu 的整體高度，Party panel 每格高度隨之縮減。

#### Scenario: Menu 高度減少
- **WHEN** 玩家開啟 Pokemon terminal menu
- **THEN** Menu 高度 SHALL 小於原本的 315px（目標約 230px）

#### Scenario: Party slot 高度縮減
- **WHEN** Menu 顯示 Party panel
- **THEN** 每個 party slot 的高度 SHALL 小於原本的 44px（按鈕移除後）


<!-- @trace
source: pokemon-cell-drag-drop
updated: 2026-03-15
code:
  - src/main/resources/data/ae2mon/recipe/portable_pokemon_cell.json
-->

---
### Requirement: 拖拉閾值防止誤觸
系統 SHALL 僅在滑鼠移動超過最小閾值後才進入拖拉狀態，避免點擊選取時誤觸。

#### Scenario: 小移動不觸發拖拉
- **WHEN** 玩家按下滑鼠後移動距離小於 4px 並放開
- **THEN** 系統 SHALL 視為普通點擊（保留 network list 的選取行為），不觸發拖拉

#### Scenario: 足夠移動觸發拖拉
- **WHEN** 玩家按下滑鼠後移動距離大於等於 4px
- **THEN** 系統 SHALL 進入拖拉狀態並顯示 ghost sprite

<!-- @trace
source: pokemon-cell-drag-drop
updated: 2026-03-15
code:
  - src/main/resources/data/ae2mon/recipe/portable_pokemon_cell.json
-->