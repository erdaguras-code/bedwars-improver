# Hedef CPS — tarayıcı tıklama antrenmanı

`index.html` tek dosyalık, bağımlılıksız bir hedefe tıklama CPS testi.
Dosyayı çift tıklayıp tarayıcıda açman yeterli (kurulum, sunucu, internet gerekmez).

**Kurallar**
- Ekranda aynı anda **yalnızca 1 hedef** bulunur, her hedef **rastgele bir süre** kalır.
- Test **ilk hedef tıklamanla** başlar ve **30 saniye** sürer.
- Sonuçtaki ana skor: **hedef ekrandayken yapılan tıklama ÷ hedefin ekranda kaldığı toplam süre**.
  Hedefler arası boşlukta yapılan tıklamalar bu orana girmez, ayrıca "boşta tıklama" olarak raporlanır.

**Zorluklar** (hedef boyutu · ekranda kalma · hedefler arası boşluk)

| Seviye | Boyut | Kalma süresi | Boşluk | Not |
|---|---|---|---|---|
| Kolay | 122 px | 1.4–2.2 sn | 0.42 sn | sabit |
| Orta  | 88 px  | 0.9–1.5 sn | 0.34 sn | sabit |
| Zor   | 58 px  | 0.62–1.05 sn | 0.26 sn | yavaş hareket |
| Uzman | 40 px  | 0.42–0.72 sn | 0.19 sn | hızlı hareket |

**Kısayollar:** `R` yeniden başlat · `1`–`4` zorluk seçimi.
Rekorlar zorluk başına tarayıcının `localStorage`'ında saklanır.
