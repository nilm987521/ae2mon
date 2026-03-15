## ADDED Requirements

### Requirement: Detail 面板顯示計算後的實際能力值（Stats）
Detail 面板 SHALL 在 IVs / EVs 旁以統一表格形式顯示六項計算後的實際能力值（HP、Atk、Def、SpA、SpD、Spe）。Stats 數值 SHALL 依 Pokemon 公式（base stat + IV + EV + 等級 + 性格修正）由 server 計算後傳送至 client，client 端直接呈現，不自行套公式。

#### Scenario: 選取 Network Pokemon 時顯示 Stats
- **WHEN** 玩家在 Terminal 中選取一個 Network list 中的 Pokemon
- **THEN** Detail 面板 SHALL 顯示該 Pokemon 六項實際能力值數字（HP / Atk / Def / SpA / SpD / Spe），與 IVs、EVs 排列於同一欄位對齊的表格中

#### Scenario: 選取 Party Pokemon 時顯示 Stats
- **WHEN** 玩家在 Party panel 中選取一個 slot 中的 Pokemon
- **THEN** Detail 面板 SHALL 顯示該 Pokemon 六項實際能力值數字，與 IVs、EVs 同框呈現

#### Scenario: 未選取 Pokemon 時不顯示 Stats
- **WHEN** 沒有任何 Pokemon 被選取（selectedUUID == null 且 selectedPartySlot == -1）
- **THEN** Detail 面板 SHALL 顯示空白提示，不顯示任何 Stats、IVs 或 EVs 數值

### Requirement: IV / EV / Stats 統一表格佈局
Detail 面板 SHALL 以共用欄位標題行（HP Atk Def SpA SpD Spe）加三行數值（IVs、EVs、Stats）的表格形式顯示，取代原本兩個獨立 section 的佈局。每行左側 SHALL 顯示 section 標籤（"IVs" / "EVs" / "Stats"）。

#### Scenario: 欄位標題行對齊六項能力值
- **WHEN** Detail 面板顯示已選取的 Pokemon
- **THEN** 面板 SHALL 在分隔線下方顯示一行包含「HP」「Atk」「Def」「SpA」「SpD」「Spe」六個欄位標題，且標題位置與下方三行數值欄位對齊

#### Scenario: IVs 行顯示原始 IV 值並保留色彩
- **WHEN** Detail 面板顯示已選取的 Pokemon
- **THEN** IVs 行 SHALL 顯示六項 IV 原始數值（0–31），各項目 SHALL 依數值套用色彩（31=綠色、高值=淡綠、中值=灰、0=紅）

#### Scenario: EVs 行顯示原始 EV 值並保留色彩
- **WHEN** Detail 面板顯示已選取的 Pokemon
- **THEN** EVs 行 SHALL 顯示六項 EV 原始數值（0–252），各項目 SHALL 依數值套用色彩（252=金色、有值=淡金、0=暗灰）

#### Scenario: Stats 行顯示計算後的實際數值
- **WHEN** Detail 面板顯示已選取的 Pokemon
- **THEN** Stats 行 SHALL 顯示六項計算後的實際能力值（整數），以白色或亮色顯示

### Requirement: SyncPokemonListPayload 包含計算後的 stat 值
`SyncPokemonListPayload` 的 `PokemonEntry` 與 `PartyEntry` SHALL 各包含六個 stat 欄位（`statHp`、`statAtk`、`statDef`、`statSpA`、`statSpD`、`statSpe`），由 server 端計算後傳送。

#### Scenario: Network Pokemon 的 stat 值由 server 套公式計算
- **WHEN** Server 建立 `PokemonEntry` 時
- **THEN** Server SHALL 依 Gen 3 公式（`floor((2*base + IV + floor(EV/4)) * level / 100) + level + 10` 用於 HP；其他項目加上 +5 與性格修正）計算六項 stat 值並填入欄位；若 base stats 查找失敗則填 0

#### Scenario: Party Pokemon 的 stat 值由 Cobblemon API 直接取得
- **WHEN** Server 建立 `PartyEntry` 時
- **THEN** Server SHALL 呼叫 Cobblemon 的 `pokemon.getStat()` 系列方法取得計算後的實際數值，確保與遊戲內顯示一致
