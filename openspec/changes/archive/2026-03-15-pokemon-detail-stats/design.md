## Context

Detail 面板目前在一條分隔線下方顯示兩個獨立區塊：IVs（含合計與六格數值）、EVs（含合計與六格數值）。面板高度 `DETAIL_H = 205px`，分隔線位於 `+87px`，兩個區塊合計佔用 `~80px`，剩餘約 `38px`，不足以再加一個完整的 Stats 區塊（label row + value row + gap ≈ 43px）。

`SyncPokemonListPayload` 目前傳送 IV 與 EV 原始值，但**不包含 base stats 或計算後的 stat 值**。Client 端無法獨立計算最終 stat，因為 base stats 屬於 species-level 資料，不在 NBT 中。

## Goals / Non-Goals

**Goals:**
- Detail 面板新增顯示六項計算後的實際能力值（HP / Atk / Def / SpA / SpD / Spe）
- IV 與 EV 原始數值保持可見
- 在現有面板高度內容納新區塊，不強制擴大 UI 視窗

**Non-Goals:**
- 不顯示 stat 的 bar chart 或進度條
- 不在 filter/search 中使用 stat 值過濾
- 不支援顯示 stat stage 或戰鬥中的能力值變化

## Decisions

### 1. Server 傳送預計算的 stat 值，而非 base stats

**選擇：** 在 payload 加入六個 `statHp / statAtk / statDef / statSpA / statSpD / statSpe` 欄位，傳送已計算完成的最終數值。

**理由：**
- Client 無需知道 stat 公式，避免公式版本歧異（Cobblemon 可能調整計算方式）
- 對 party Pokemon，server 可直接呼叫 `pokemon.getStat(Stats.HP)` 等方法，精確且省事
- 對 network Pokemon（NBT 儲存），可從 NBT 反序列化 `Pokemon` 物件後呼叫相同方法，或從 species 查 base stats 套公式；前者更正確（尊重 Cobblemon 內部邏輯），後者效能較佳

**實作選擇：** 使用 `PokemonSpecies.INSTANCE.getByIdentifier()` 取得 base stats，在 server 端套用 Gen 3 公式計算——因為 network Pokemon 數量可能多，完整反序列化 NBT → Pokemon 成本較高；base stats 查找 + 公式計算代價低。Party Pokemon（live object）直接呼叫 `pokemon.getStat()`。

**Gen 3 Stat 公式：**
```
HP  = floor((2 * base + IV + floor(EV / 4)) * level / 100) + level + 10
其他 = floor((floor((2 * base + IV + floor(EV / 4)) * level / 100) + 5) * nature_modifier)
nature_modifier: 1.1 (boosted) | 1.0 (neutral) | 0.9 (hindered)
```

Nature → stat modifier 對應表在 server 或 client 端均可維護；建議放在 server 端計算並直接傳送最終值。

---

### 2. 統一表格佈局（IV / EV / Stats 合併為一個區塊）

**選擇：** 取消個別 section 的 label row，改用一個共用的欄位標題行（HP Atk Def SpA SpD Spe），下方三行分別為 IVs / EVs / Stats 的數值，每行左側有 section 標籤。

```
        HP   Atk  Def  SpA  SpD  Spe
IVs     31   31   31   31   31   31
EVs      0  252    0  252    0    0
Stats  356  266  185  266  165  166
```

**理由：**
- 節省約 26px（原本兩個 section 各有一個 label row），讓三個 section 在原有面板高度內完整顯示
- 視覺更緊湊，欄位對齊更清楚
- 移除原來的「IVs (total / 186)」與「EVs (total / 510)」標題行；total 資訊可移至 section 標籤旁（`IVs 186`、`EVs 510`），或省略

**Layout 估算（relative to DETAIL_Y）：**
- divY: +87
- gap: +8 → 95
- 共用欄位標題行: +9 → 104
- IVs 數值行: +13 → 117
- EVs 數值行: +13 → 130
- Stats 數值行: +13 → 143
- 底部 margin: 205 − 143 = 62px 剩餘（充裕）

---

### 3. Cobblemon base stats 存取方式（network Pokemon）

`PokemonSpecies.INSTANCE.getByIdentifier(loc).getBaseStats()` 回傳 `Map<Stat, Integer>`（Kotlin：`Map<Stat, Int>`）。
Stat key 使用 Cobblemon 的 `Stats` singleton（`Stats.HP`、`Stats.ATTACK`、`Stats.DEFENCE`、`Stats.SPECIAL_ATTACK`、`Stats.SPECIAL_DEFENCE`、`Stats.SPEED`），或透過 `CobblemonStatProvider` 查詢。

實際 API 存取方式需在實作時驗證（Cobblemon Kotlin API 從 Java 呼叫可能需要特殊處理）；fallback 為 `0`，避免 NullPointerException。

## Risks / Trade-offs

- **Base stats 查找失敗** → 若 species 未載入或識別碼不符，stat 顯示為 0。Mitigation：fallback 為 0，UI 不崩潰，日後可加 warning log。
- **公式與 Cobblemon 內部不符** → Cobblemon 可能使用非標準公式或有 mod 修改空間。Mitigation：對 party Pokemon 優先使用 `pokemon.getStat()` 直接取值；network Pokemon 套公式為近似值，標示欄位名稱即可，使用者理解這是估算。
- **Payload 大小增加** → 每個 entry 新增 6 個 int（24 bytes），對 large ME network 可能有數 KB 增量。可接受範圍，不需壓縮。
- **Section 標籤寬度擠壓數值欄** → `IVs` / `EVs` / `Stats` 標籤佔約 20–24px，需調整六欄 offset 起始位置（`colOffsets` 右移）。

## Migration Plan

1. 擴充 `SyncPokemonListPayload.PokemonEntry` 與 `PartyEntry`，新增 `statHp` … `statSpe` 六欄
2. 更新 `StreamCodec`（encode / decode 對稱）
3. 更新 `ModPayloads.sendSyncToPlayer()` — network Pokemon 套公式計算，party Pokemon 呼叫 `pokemon.getStat()`
4. 更新 `PokemonTerminalScreen.renderDetailPanel()` — 重構 IV/EV 渲染為三行共用表格，加入 Stats 行

無 migration 問題：`SyncPokemonListPayload` 為 mod-internal packet，不涉及存檔格式或第三方相容性。
