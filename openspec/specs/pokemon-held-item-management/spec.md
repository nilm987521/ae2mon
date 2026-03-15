# pokemon-held-item-management Specification

## Purpose

TBD - created by archiving change 'pokemon-held-item'. Update Purpose after archive.

## Requirements

### Requirement: Terminal 下方顯示玩家 Inventory
Terminal 畫面 SHALL 在三個主要 panel（Network / Detail / Party）下方顯示玩家完整 inventory，包含 27 格主欄位（3×9）與 9 格 hotbar，排列與標準 Minecraft chest UI 相同。

#### Scenario: 開啟 Terminal 時顯示 Inventory
- **WHEN** 玩家開啟 Pokemon Terminal
- **THEN** 畫面下方 SHALL 顯示玩家 inventory 的 36 個格子（27 main + 9 hotbar），內容與玩家實際 inventory 一致

#### Scenario: Inventory 格子顯示正確物品
- **WHEN** 玩家 inventory 中有物品
- **THEN** 對應格子 SHALL 顯示該物品的圖示與堆疊數量

#### Scenario: 點擊 Inventory 格子可拿起物品
- **WHEN** 玩家左鍵點擊有物品的 inventory 格子
- **THEN** 系統 SHALL 將該物品放至游標（cursor），格子變空，行為與標準 Minecraft 容器 UI 相同


<!-- @trace
source: pokemon-held-item
updated: 2026-03-15
code:
  - src/main/java/cc/nilm/mcmod/ae2mon/common/network/ModPayloads.java
  - src/main/java/cc/nilm/mcmod/ae2mon/common/network/SetHeldItemPayload.java
  - src/main/java/cc/nilm/mcmod/ae2mon/common/network/SyncPokemonListPayload.java
  - src/main/java/cc/nilm/mcmod/ae2mon/client/screen/PokemonTerminalScreen.java
  - src/main/java/cc/nilm/mcmod/ae2mon/common/menu/PokemonTerminalMenu.java
-->

---
### Requirement: Detail 面板 Held Item Slot 可交互
Detail 面板 SHALL 顯示一個可交互的 Held Item Slot widget，展示當前選取 Pokemon 的攜帶物。玩家可透過 cursor 與此 widget 交互來裝備、替換或卸除攜帶物。

#### Scenario: 選取 Pokemon 時顯示 Held Item Slot
- **WHEN** 玩家選取一個 Pokemon（network 或 party）
- **THEN** Detail 面板 SHALL 在固定位置顯示 Held Item Slot widget；若該 Pokemon 有攜帶物則顯示物品圖示，否則顯示空格子圖示

#### Scenario: 未選取 Pokemon 時不顯示 Held Item Slot
- **WHEN** 沒有任何 Pokemon 被選取
- **THEN** Detail 面板 SHALL 不顯示可交互的 Held Item Slot widget

#### Scenario: 游標有物品時點擊 Held Item Slot 裝備物品
- **WHEN** 玩家游標持有物品，且 Pokemon 當前沒有攜帶物，玩家點擊 Held Item Slot widget
- **THEN** 系統 SHALL 將游標物品裝備為該 Pokemon 的攜帶物，游標變空，Held Item Slot 顯示新攜帶物

#### Scenario: 游標有物品時點擊 Held Item Slot 替換攜帶物
- **WHEN** 玩家游標持有物品，且 Pokemon 已有攜帶物，玩家點擊 Held Item Slot widget
- **THEN** 系統 SHALL 將游標物品裝備為新攜帶物，並將舊攜帶物放至游標，Held Item Slot 顯示新攜帶物

#### Scenario: 游標為空時點擊 Held Item Slot 卸除攜帶物
- **WHEN** 玩家游標為空，且 Pokemon 有攜帶物，玩家點擊 Held Item Slot widget
- **THEN** 系統 SHALL 將攜帶物移至游標，Pokemon held item 清空，Held Item Slot 顯示空格子

#### Scenario: 游標為空且 Pokemon 無攜帶物時點擊無效
- **WHEN** 玩家游標為空，且 Pokemon 沒有攜帶物，玩家點擊 Held Item Slot widget
- **THEN** 系統 SHALL 不執行任何操作


<!-- @trace
source: pokemon-held-item
updated: 2026-03-15
code:
  - src/main/java/cc/nilm/mcmod/ae2mon/common/network/ModPayloads.java
  - src/main/java/cc/nilm/mcmod/ae2mon/common/network/SetHeldItemPayload.java
  - src/main/java/cc/nilm/mcmod/ae2mon/common/network/SyncPokemonListPayload.java
  - src/main/java/cc/nilm/mcmod/ae2mon/client/screen/PokemonTerminalScreen.java
  - src/main/java/cc/nilm/mcmod/ae2mon/common/menu/PokemonTerminalMenu.java
-->

---
### Requirement: Server 原子性執行 Held Item 交換
Server 收到 `SetHeldItemPayload` 後 SHALL 原子性地完成 held item 交換，任何步驟失敗須 rollback，不留中間狀態。

#### Scenario: Party Pokemon Held Item 交換成功
- **WHEN** Server 收到 `SetHeldItemPayload`，目標為 party Pokemon，游標有物品
- **THEN** Server SHALL 以游標物品替換 Pokemon 當前 held item，舊 held item 回到游標，並重新同步 client

#### Scenario: Network Pokemon Held Item 交換成功
- **WHEN** Server 收到 `SetHeldItemPayload`，目標為 network Pokemon，游標有物品
- **THEN** Server SHALL：從 storage extract 該 Pokemon（舊 key）、修改 NBT held item 欄位、以新 key insert 回 storage、舊 held item 回到游標，並重新同步 client

#### Scenario: Network Pokemon Held Item 交換失敗 rollback
- **WHEN** Server 執行 network Pokemon held item 交換，但 re-insert 失敗（如 cell 已滿）
- **THEN** Server SHALL rollback：將原 Pokemon key re-insert 回 storage，游標物品不變，不執行任何持久修改

#### Scenario: Pokemon 不存在時操作取消
- **WHEN** Server 收到 `SetHeldItemPayload`，但目標 Pokemon（UUID 或 party slot）在 server 端已不存在
- **THEN** Server SHALL 忽略請求，不修改任何狀態，重新同步 client


<!-- @trace
source: pokemon-held-item
updated: 2026-03-15
code:
  - src/main/java/cc/nilm/mcmod/ae2mon/common/network/ModPayloads.java
  - src/main/java/cc/nilm/mcmod/ae2mon/common/network/SetHeldItemPayload.java
  - src/main/java/cc/nilm/mcmod/ae2mon/common/network/SyncPokemonListPayload.java
  - src/main/java/cc/nilm/mcmod/ae2mon/client/screen/PokemonTerminalScreen.java
  - src/main/java/cc/nilm/mcmod/ae2mon/common/menu/PokemonTerminalMenu.java
-->

---
### Requirement: Held Item 操作後即時同步
Server 完成 held item 操作後 SHALL 立即呼叫 `sendSyncToPlayer()`，確保 client 顯示最新的 Pokemon 資料（包含更新後的 held item）。

#### Scenario: 操作成功後 Client 顯示更新
- **WHEN** Server 成功完成 held item 裝備或卸除
- **THEN** Client SHALL 收到更新的 `SyncPokemonListPayload`，Detail 面板的 Held Item Slot 顯示最新狀態

<!-- @trace
source: pokemon-held-item
updated: 2026-03-15
code:
  - src/main/java/cc/nilm/mcmod/ae2mon/common/network/ModPayloads.java
  - src/main/java/cc/nilm/mcmod/ae2mon/common/network/SetHeldItemPayload.java
  - src/main/java/cc/nilm/mcmod/ae2mon/common/network/SyncPokemonListPayload.java
  - src/main/java/cc/nilm/mcmod/ae2mon/client/screen/PokemonTerminalScreen.java
  - src/main/java/cc/nilm/mcmod/ae2mon/common/menu/PokemonTerminalMenu.java
-->