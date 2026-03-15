## 1. Override keyPressed to intercept keys when search is focused

- [x] 1.1 在 `PokemonTerminalScreen` 新增 `keyPressed` override：搜尋欄有焦點時攔截按鍵，Escape 除外
- [x] 1.2 在 `PokemonTerminalScreen` 新增 `charTyped` override：搜尋欄有焦點時轉發字元至 searchBox
