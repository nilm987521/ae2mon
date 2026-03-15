## Why

Detail 面板目前只顯示原始 IV / EV 數值，玩家無法直接看出 Pokemon 的實際能力值（Stat）。加入計算後的 Stat 數值，讓玩家在 ME terminal 中就能評估 Pokemon 的實戰強度，不需切換到其他 UI。

## What Changes

- Detail 面板新增 **Stats 區塊**，位於現有 IVs / EVs 區塊之下（或整合同一區域）
- 顯示六項實際能力值：HP、Atk、Def、SpA、SpD、Spe（依 Pokemon 公式計算）
- IVs 與 EVs 數字保持顯示不變
- `SyncPokemonListPayload`（`PokemonEntry` 與 `PartyEntry`）新增六項 base stat 欄位，供 client 端計算使用
- Client 端依 Stat 公式計算最終數值並渲染於 Detail 面板

## Capabilities

### New Capabilities
- `pokemon-detail-stats-display`: Detail 面板顯示計算後的六項實際能力值（Stats），與現有 IV / EV 並列呈現

### Modified Capabilities
- `pokemon-drag-drop-interaction`: 無需修改（行為不變，僅新增顯示）

## Impact

- `SyncPokemonListPayload` — `PokemonEntry` 與 `PartyEntry` record 新增 `baseStat*` 六欄位（HP / Atk / Def / SpA / SpD / Spe）
- `SyncPokemonListPayload`（Server 端）— 讀取 Cobblemon Species base stats 並填入 payload
- `PokemonTerminalScreen` — `renderDetailPanel()` 新增 Stats 渲染區塊
- Stat 計算公式（Gen 3+）在 client 端實作，不需額外 network roundtrip
