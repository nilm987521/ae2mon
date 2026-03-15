## 1. Payload 擴充（Server）

- [x] 1.1 在 `SyncPokemonListPayload.PokemonEntry` record 新增六個 stat 欄位：`statHp, statAtk, statDef, statSpA, statSpD, statSpe`
- [x] 1.2 在 `SyncPokemonListPayload.PartyEntry` record 新增同樣六個 stat 欄位
- [x] 1.3 更新 `PokemonEntry.STREAM_CODEC` — encode/decode 加入六個 stat int
- [x] 1.4 更新 `PartyEntry.STREAM_CODEC` — encode/decode 加入六個 stat int

## 2. Server 端 Stat 計算

- [x] 2.1 在 `ModPayloads` 新增 `extractBaseStats(CompoundTag data)` helper — 查 `PokemonSpecies.INSTANCE.getByIdentifier()` 取得 base stats map，回傳 `int[6]`（hp/atk/def/spA/spD/spe），查找失敗回傳全 0
- [x] 2.2 在 `ModPayloads` 新增 `computeStats(int[] base, int[] ivs, int[] evs, int level, String nature)` helper — 依 Gen 3 公式計算並回傳 `int[6]`；HP 用 `+level+10` 公式，其他套性格修正（1.1/1.0/0.9）
- [x] 2.3 建立 nature → stat modifier 對應表（25 種性格對應 boosted/hindered stat index）
- [x] 2.4 在 `sendSyncToPlayer()` 的 network Pokemon 迴圈中呼叫 `extractBaseStats` + `computeStats`，填入 `PokemonEntry` stat 欄位
- [x] 2.5 在 `sendSyncToPlayer()` 的 party Pokemon 迴圈中呼叫 `pokemon.getStat()` 取得六項數值，填入 `PartyEntry` stat 欄位（需確認 Cobblemon Kotlin API 從 Java 呼叫方式）

## 3. Client 渲染重構

- [x] 3.1 在 `PokemonTerminalScreen.renderDetailPanel()` 中接收新的 stat 欄位（`statHp` … `statSpe`）
- [x] 3.2 將現有 IVs / EVs 渲染區塊重構為三行統一表格：新增共用欄位標題行（HP Atk Def SpA SpD Spe）
- [x] 3.3 三行數值各帶左側標籤（"IVs" / "EVs" / "Stats"），行距緊湊以符合 `DETAIL_H = 205px` 限制
- [x] 3.4 移除原本「IVs (total / 186)」與「EVs (total / 510)」獨立 section 標題；IV/EV 合計資訊可整合至行標籤或省略
- [x] 3.5 Stats 行數值以亮色（`COL_TEXT`）顯示，不套色彩分級
- [x] 3.6 驗證重構後的表格在面板高度內無截切（statValsY < DETAIL_Y + DETAIL_H）
