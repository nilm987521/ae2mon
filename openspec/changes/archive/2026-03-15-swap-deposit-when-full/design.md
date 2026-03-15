## Context

目前 `depositPokemon` 在 `storage.insert(SIMULATE)` 回傳 0 時直接 return false（Network 滿），`withdrawPokemon` 在 party 已有 6 隻時 `party.add()` 會失敗。兩種情況都靜默失敗，玩家沒有明確回饋也無法完成操作。

拖放互動目前使用「區域」判定——拖到整個 Network 列表區域即存入、拖到整個 Party 區域即提取。要實現位置導向的交換，需要將拖放目標從「區域」細化到「特定項目」。

## Goals / Non-Goals

**Goals:**
- 拖放到 Network 列表的**特定項目**上時，若 Network 已滿，執行雙向交換（Network 那隻進 Party，Party 那隻進 Network）
- 拖放到 Party 的**特定 slot** 上時，若 Party 已滿，執行雙向交換（Party 那隻進 Network，Network 那隻進 Party）
- 拖放目標有 Pokemon 的位置才顯示 swap 高亮

**Non-Goals:**
- 不支援 drag 到空白位置觸發 swap（只是原來的 deposit/withdraw）
- 不改變 Portable Cell Terminal 的行為（範圍外）
- 不實作動畫或多步驟 undo

## Decisions

### 1. 新增 `SwapPokemonPayload` 而非擴充現有 payload

**決定**：新增獨立的 `SwapPokemonPayload(int containerId, UUID networkUUID, int partySlot)`。

**理由**：swap 的語義（雙向交換）和現有的 deposit/withdraw（單向）不同。共用 payload 需要加入可空欄位，增加理解難度。新 payload 清楚表達意圖，也方便獨立處理器和日誌追蹤。

**替代方案**：在 `DepositPokemonPayload` 加 `targetNetworkUUID`（nullable），用 null 表示「找空位插入」。被排除——nullable 語義模糊，serialization 需要特殊處理。

### 2. 交換目標由 Client 在拖放 release 時決定

**決定**：Client 在 `mouseReleased` 時計算 hovered entry / party slot，若目標有 Pokemon 且是 occupied 位置，發送 `SwapPokemonPayload`；否則仍走原有的 `DepositPokemonPayload` / `WithdrawPokemonPayload`。

**理由**：保持現有的 deposit/withdraw 路徑不變，swap 是在「落到有 Pokemon 的位置」時才觸發的額外路徑。Server 不需要判斷「要不要 swap」——Client 已決定。

**替代方案**：Server 自動判斷滿時改走 swap。被排除——Server 不知道 Client 拖放的目標位置，必須由 Client 傳遞。

### 3. Server 端 swap 邏輯：先移除再插入，失敗全部 rollback

**決定**：`swapPokemon(UUID networkUUID, int partySlot, ServerPlayer)` 步驟：
1. SIMULATE extract networkUUID → 若失敗 return false
2. SIMULATE insert partyPokemon → 若失敗 return false（理論上不會，因為剛空出一個位置）
3. 從 party 移除 slot 的 Pokemon（記錄為 `evicted`）
4. storage.extract networkUUID (MODULATE)
5. party.set(partySlot, networkPokemon)
6. storage.insert evictedKey (MODULATE)
7. 若步驟 6 失敗，rollback：party.remove(networkPokemon)，storage.insert(networkKey)，party.set(partySlot, evicted)

**理由**：先用 SIMULATE 預驗，實際操作盡量減少失敗可能；失敗時有明確的 rollback 路徑。

### 4. 高亮行為：拖動時 hover 到有 Pokemon 的位置才顯示 swap 顏色

**決定**：
- 拖 Party Pokemon 懸停在 Network 列表某項目上 → 高亮該項目（`COL_SWAP_HOVER`，橙色/紫色）
- 拖 Network Pokemon 懸停在某 Party slot 上（且有 Pokemon）→ 高亮該 slot
- 懸停在空位或原有的「區域高亮」一律只顯示原本的 `COL_DROP_ZONE`（綠色）

## Risks / Trade-offs

- **Party slot index 穩定性**：Client 傳 party slot index，Server 從 `Cobblemon.INSTANCE.getStorage().getParty(player).get(slot)` 取得。若在封包傳輸途中 party 變化（極少見），Server 會操作到錯誤的 Pokemon → **Mitigation**：Server 在步驟 3 前再次確認 slot 有 Pokemon，若已空則 fallback 到普通 withdraw。
- **Network 列表顯示 index vs 實際 UUID**：Client 傳 `networkUUID`（UUID 是穩定識別碼），不傳 display list index，避免 displayList 過濾後 index 對不上。
- **雙向 swap 時 party 已滿但 network 也滿**：兩邊都滿時，swap 依然可以執行（一進一出，不改變總數），不需要額外處理。

## Migration Plan

無需 migration。新 payload 是加法，現有 deposit/withdraw payload 保持不變。舊版本客戶端連接新版 Server 不會發送 swap packet，行為退化為原本的靜默失敗，可接受。
