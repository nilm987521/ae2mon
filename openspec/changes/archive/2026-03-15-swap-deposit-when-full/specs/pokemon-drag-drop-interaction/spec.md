# pokemon-drag-drop-interaction Delta Spec
# Change: swap-deposit-when-full

## MODIFIED Requirements

### Requirement: Drag Pokemon from party to deposit
玩家可以從 Party panel 將 Pokemon 拖拉至 Network storage 區域，完成 deposit 或 swap 操作。

#### Scenario: 成功 deposit（落在空白區域或 Network 未滿）
- **WHEN** 玩家在 Party panel 的某個 slot 按下滑鼠左鍵並拖拉至 Network list panel 的空白區域或 Network 未滿，然後放開
- **THEN** 系統 SHALL 向 server 發送 `DepositPokemonPayload`，Pokemon 從隊伍移入網路 storage，Party panel 更新顯示

#### Scenario: 落在 Network 已有 Pokemon 的項目上觸發 swap
- **WHEN** 玩家從 Party slot 拖拉 Pokemon，放開在 Network list 中某個已有 Pokemon 的列表項目上
- **THEN** 系統 SHALL 向 server 發送 `SwapPokemonPayload(networkUUID, partySlot)`，而非 `DepositPokemonPayload`

#### Scenario: 拖拉至無效區域
- **WHEN** 玩家從 Party panel 拖拉 Pokemon 但放開位置不在 Network list panel 範圍內
- **THEN** 系統 SHALL 取消拖拉，不觸發任何 deposit 或 swap，UI 恢復原狀

#### Scenario: 拖拉空的 party slot
- **WHEN** 玩家嘗試拖拉一個沒有 Pokemon 的 party slot
- **THEN** 系統 SHALL 不進入拖拉狀態

### Requirement: Drag Pokemon from network to withdraw
玩家可以從 Network list 將 Pokemon 拖拉至 Party panel，完成 withdraw 或 swap 操作。

#### Scenario: 成功 withdraw（落在空 slot 或 Party 未滿）
- **WHEN** 玩家在 Network list 的某個 entry 按下滑鼠左鍵並拖拉至 Party panel 的空 slot 或 Party 未滿，然後放開
- **THEN** 系統 SHALL 向 server 發送 `WithdrawPokemonPayload`，Pokemon 從網路 storage 加入玩家隊伍，Network list 更新顯示

#### Scenario: 落在 Party 已有 Pokemon 的 slot 上觸發 swap
- **WHEN** 玩家從 Network list 拖拉 Pokemon，放開在 Party panel 中某個已有 Pokemon 的 slot 上
- **THEN** 系統 SHALL 向 server 發送 `SwapPokemonPayload(networkUUID, partySlot)`，而非 `WithdrawPokemonPayload`

#### Scenario: 拖拉至無效區域
- **WHEN** 玩家從 Network list 拖拉 Pokemon 但放開位置不在 Party panel 範圍內
- **THEN** 系統 SHALL 取消拖拉，不觸發任何 withdraw 或 swap，UI 恢復原狀

### Requirement: Drop zone highlight
當玩家正在拖拉且游標位於有效的放置區域時，系統 SHALL 顯示視覺 highlight。若游標懸停在已有 Pokemon 的特定位置（可觸發 swap），SHALL 顯示區別性的 swap 高亮色（例如橙色/紫色遮罩）。

#### Scenario: 有效 drop zone highlight（空白區域）
- **WHEN** 玩家正在拖拉 Pokemon 且游標進入有效目標 panel 的空白區域
- **THEN** 系統 SHALL 在該 panel 上疊加半透明綠色遮罩（`0x3300FF00`）

#### Scenario: Swap 目標 highlight（懸停在已佔用位置）
- **WHEN** 玩家從 Party 拖拉 Pokemon，游標懸停在 Network list 中某個已有 Pokemon 的項目上；或玩家從 Network 拖拉，游標懸停在 Party 中已有 Pokemon 的 slot 上
- **THEN** 系統 SHALL 在該項目或 slot 上疊加 swap 高亮色（與普通 drop zone 綠色區別），顯示將發生交換

#### Scenario: 無效區域不 highlight
- **WHEN** 玩家正在拖拉但游標不在任何有效 drop zone
- **THEN** 系統 SHALL 不顯示任何 highlight
