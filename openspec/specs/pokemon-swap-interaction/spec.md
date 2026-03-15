# pokemon-swap-interaction Specification

## Purpose

定義位置導向的 Pokemon 交換語義：當玩家將 Pokemon 拖放到一個已有 Pokemon 的目標位置時，系統執行雙向交換，而非靜默失敗。

## Requirements

### Requirement: Deposit swap — Party 存入 Network 時交換
當玩家將 Party 中的 Pokemon 拖拉，並放開在 Network list 中**某個已有 Pokemon 的項目**上，系統 SHALL 執行 swap：將該 Network Pokemon 提取至 Party（替換原 slot），並將原 Party Pokemon 存入 Network（替換原位置）。

#### Scenario: Network 滿時放開在 Network 某項目上執行 swap
- **WHEN** 玩家從 Party slot S 拖拉 Pokemon A，放開在 Network list 中 UUID 為 N 的項目上，且 Network 已滿
- **THEN** 系統 SHALL 發送 `SwapPokemonPayload(networkUUID=N, partySlot=S)`，Server 將 A 存入 Network 取代 N 的位置，將 N 提取至 Party slot S

#### Scenario: Network 未滿時放開在 Network 某項目上執行 deposit
- **WHEN** 玩家從 Party slot S 拖拉 Pokemon A，放開在 Network list 中**已有 Pokemon 的項目**上，且 Network **未**滿
- **THEN** 系統 SHALL 發送 `DepositPokemonPayload(partySlot=S)`，執行普通 deposit，不觸發 swap

#### Scenario: 放開在 Network 空白區域執行普通 deposit
- **WHEN** 玩家從 Party 拖拉 Pokemon，放開在 Network list 中沒有項目的空白區域
- **THEN** 系統 SHALL 發送 `DepositPokemonPayload`，執行普通 deposit

### Requirement: Withdraw swap — Network 提取至 Party 時交換
當玩家將 Network 中的 Pokemon 拖拉，並放開在 Party 中**某個已有 Pokemon 的 slot** 上，系統 SHALL 執行 swap：將該 Party Pokemon 存入 Network，並將 Network Pokemon 放入該 Party slot。

#### Scenario: Party 滿時放開在 Party 某 slot 上執行 swap
- **WHEN** 玩家從 Network list 拖拉 UUID 為 N 的 Pokemon，放開在 Party slot S（已有 Pokemon B），且 Party 已滿
- **THEN** 系統 SHALL 發送 `SwapPokemonPayload(networkUUID=N, partySlot=S)`，Server 將 B 存入 Network，將 N 放入 Party slot S

#### Scenario: Party 未滿時放開在已佔用 Party slot 上執行 withdraw
- **WHEN** 玩家從 Network list 拖拉 Pokemon N，放開在 Party 已有 Pokemon 的 slot S，且 Party **未**滿
- **THEN** 系統 SHALL 發送 `WithdrawPokemonPayload(uuid=N)`，執行普通 withdraw，不觸發 swap

#### Scenario: 放開在空 Party slot 執行普通 withdraw
- **WHEN** 玩家從 Network list 拖拉 Pokemon N，放開在空的 Party slot
- **THEN** 系統 SHALL 發送 `WithdrawPokemonPayload(uuid=N)`，執行普通 withdraw

### Requirement: Server swap 原子性
Server 執行 swap 時 SHALL 確保操作原子性：任何一步失敗，整個 swap 必須 rollback，不留下中間狀態。

#### Scenario: Swap 成功完整執行
- **WHEN** Server 收到 `SwapPokemonPayload`，SIMULATE 階段均通過
- **THEN** Server SHALL 依序執行：從 Party 移除目標 Pokemon、從 Network 移除目標 Pokemon、將原 Party Pokemon 插入 Network、將原 Network Pokemon 放入 Party，並重新整理 availableKeys

#### Scenario: Swap 失敗 rollback
- **WHEN** Server 收到 `SwapPokemonPayload`，但 SIMULATE 或執行過程中任何步驟失敗
- **THEN** Server SHALL rollback 所有已執行的步驟，兩邊的 Pokemon 保持原狀，不執行部分交換

#### Scenario: 目標 slot 在封包到達前已空
- **WHEN** Server 收到 `SwapPokemonPayload`，但目標 party slot 在封包到達時已無 Pokemon
- **THEN** Server SHALL fallback 執行普通 withdraw（NetworkPokemon → Party），不觸發 swap
