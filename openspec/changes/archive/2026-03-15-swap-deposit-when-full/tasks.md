## 1. Network Payload

- [x] 1.1 新增 `SwapPokemonPayload(int containerId, UUID networkUUID, int partySlot)` — 包含 TYPE、STREAM_CODEC
- [x] 1.2 在 `CobblemonAE2` 中註冊 `SwapPokemonPayload`（`PayloadRegistrar`）
- [x] 1.3 新增 Server-side handler：收到 `SwapPokemonPayload` 後呼叫 `menu.swapPokemon(uuid, partySlot, player)`

## 2. Server 邏輯

- [x] 2.1 在 `PokemonTerminalMenu` 新增 `swapPokemon(UUID networkUUID, int partySlot, ServerPlayer)` 方法
- [x] 2.2 實作 SIMULATE 預驗：extract networkUUID、確認 partySlot 有 Pokemon
- [x] 2.3 實作實際 swap 操作：移除 party slot Pokemon → extract network Pokemon → insert evicted → party.set(slot, networkPokemon)
- [x] 2.4 實作 rollback：任一步驟失敗時還原兩邊狀態
- [x] 2.5 實作 fallback：partySlot 在 server 端已空時，改呼叫普通 `withdrawPokemon`

## 3. Client — 拖放目標細化

- [x] 3.1 在 `PokemonTerminalScreen.mouseReleased` 中，拖 Party → Network 時計算游標落在哪個 Network list entry（取得其 UUID）
- [x] 3.2 若目標 entry 存在（有 Pokemon），改發送 `SwapPokemonPayload`；否則維持發送 `DepositPokemonPayload`
- [x] 3.3 在 `mouseReleased` 中，拖 Network → Party 時計算游標落在哪個 Party slot（0-5）
- [x] 3.4 若目標 slot 有 Pokemon，改發送 `SwapPokemonPayload`；否則維持發送 `WithdrawPokemonPayload`

## 4. Client — Swap 高亮

- [x] 4.1 新增顏色常數 `COL_SWAP_HOVER`（例如 `0x44FF8800`，橙色半透明）
- [x] 4.2 拖 Party Pokemon 時，若游標懸停在 Network list 某項目上，高亮該項目（swap 色）
- [x] 4.3 拖 Network Pokemon 時，若游標懸停在 Party 已有 Pokemon 的 slot 上，高亮該 slot（swap 色）
- [x] 4.4 確認原有的整體 panel 綠色遮罩（`COL_DROP_ZONE`）在 swap 目標高亮時不重疊顯示
