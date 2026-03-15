## Why

當 ME Network 儲存空間已滿或玩家 Party 已達上限時，存入／提取 Pokemon 的操作會靜默失敗。改為依目標位置進行交換，讓玩家在滿載狀態下依然能完成預期動作。

## What Changes

- 拖放 Pokemon 到 Network 中已佔用的 slot 時，若 Network 已滿，改為將目標 slot 的 Pokemon 提取至 Party，並將手上的 Pokemon 存入該 slot（交換）
- 拖放 Pokemon 到 Party 中已佔用的 slot 時，若 Party 已滿，改為將目標 slot 的 Pokemon 存入 Network，並將手上的 Pokemon 放入該 slot（交換）
- 交換目標由拖放的目的地位置決定，不自動選擇
- 若交換本身也無法完成（例如兩邊都滿且無法移動），維持失敗行為並給予明確提示

## Capabilities

### New Capabilities
- `pokemon-swap-interaction`: 定義位置導向的 swap 語義——存入滿載 Network 或提取至滿載 Party 時，以目標 slot 的 Pokemon 進行雙向交換

### Modified Capabilities
- `pokemon-drag-drop-interaction`: 拖放行為需擴充以支援 swap 語義（目前只定義 deposit / withdraw，需加入 swap 路徑）

## Impact

- `PokemonTerminalMenu` — deposit/withdraw 邏輯需加入 swap fallback
- `DepositPokemonPayload` / `WithdrawPokemonPayload` — 需攜帶目標 slot index 以支援位置導向交換，或新增 `SwapPokemonPayload`
- `PokemonTerminalScreen` — 拖放到已佔用位置時的視覺反饋（hover highlight 是否顯示 swap 意圖）
- `PortableCellTerminalMenu` — 若 Portable Cell 也需支援此行為，同樣受影響
