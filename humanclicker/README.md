# Human Clicker – rastgele tıklama aralıkları yaması

`humanclicker 0.1.0` sürümündeki `ClickScheduler` sınıfı yeniden yazıldı. Amaç:
tıklama hızı **13–18 CPS bandının içinde kalırken** araların olabildiğince
rastgele olması.

## Önceki davranış

Eski zamanlayıcı şöyle çalışıyordu:

1. `13–18` arasından bir CPS değeri seçiyor,
2. bu değeri **6–18 tıklama boyunca sabit tutuyor**,
3. her aralığı `1s / CPS` ile hesaplayıp üstüne dar bir Gauss gürültüsü
   (`jitter = 0.18`, çarpan `0.55–1.75` arasına kırpılıyor) ekliyordu.

İki sorun vardı:

- **Plato**: onlarca tıklama boyunca neredeyse sabit bir tempo oluşuyordu;
  aralık dizisi kendini tekrar eden bir yapı gösteriyordu.
- **Bant taşması**: çarpan `1.75`'e kadar çıkabildiği için tek tek aralıklar
  `7.4 CPS`'e kadar yavaşlayıp `32 CPS`'e kadar hızlanabiliyordu. Yani bant
  yalnızca ortalamada tutuluyordu, tek tek aralıklarda değil.
- **Kırpma izi**: çarpan `0.55` ve `1.75`'te kırpıldığı için bu iki değerde
  yığılma oluşuyordu.

## Yeni davranış

Her aralık **bağımsız olarak** üretilir; sabit tutulan bir CPS değeri yoktur.

Bant sınırları doğrudan ayardan gelir:

```
en kısa aralık = 1 sn / cpsMax   (18 CPS -> 55.556 ms)
en uzun aralık = 1 sn / cpsMin   (13 CPS -> 76.923 ms)
```

Üretilen her aralık bu iki değerin arasındadır — istisnasız. Bütün aralıklar
bandın içinde olduğu için, hangi zaman penceresi alınırsa alınsın ortalama hız
da kaçınılmaz olarak `13–18 CPS` arasında kalır.

Bant içindeki konum dört kaynağın karışımından seçilir, hangisinin
kullanılacağı her tıklamada yeniden rastgele belirlenir:

| Mod | Ağırlık | Ne yapar |
| --- | --- | --- |
| düz dağılım | %40 | bandın tamamına eşit olasılıkla yayılır (en yüksek entropi) |
| sürüklenme | %25 | bir önceki aralıktan geniş adımlı rastgele yürüyüş |
| tempo | %30 | saniyeler içinde yavaşça gezinen bir merkez etrafında dağılım |
| kenar | %5 | bandın en hızlı / en yavaş ucuna yakın değerler |

Ek olarak:

- **Yansıtma**: `[0,1]` dışına taşan konumlar kırpılmaz, sınırdan geri
  yansıtılır. Böylece bant uçlarında yığılma (kırpmanın bırakacağı belirgin iz)
  oluşmaz.
- **Tempo merkezi** yavaş bir rastgele yürüyüş yapar, yani saniyelik ortalama
  hız da bandın içinde gezinir; tek bir değere kilitlenmez.
- **Tohum**: üreteç `SecureRandom` ile tohumlanan bir `SplittableRandom`;
  her oturumda farklı bir diziyle başlar.
- `jitter` ayarı artık bandı genişletmez, yalnızca ince gürültü katsayısıdır;
  hangi değeri verirseniz verin aralık banttan taşmaz.

## Ölçüm (200.000 aralık, 13–18 CPS)

```
bant                : 55.556 - 76.923 ms
gözlenen            : 55.556 - 76.923 ms
bant dışı            : 0 / 200000
aynı ardışık değer   : 0
ortalama            : 66.25 ms  -> 15.10 CPS
standart sapma      : 6.34 ms
lag-1 otokorelasyon : 0.22
histogram (10 kutu)  : [21854, 21192, 18968, 18933, 18968, 19082, 18833, 19024, 21056, 22090]
1 sn pencere CPS     : 13.45 - 17.06
10 sn pencere CPS    : 14.24 - 15.96
```

Standart sapma `6.34 ms`, bandın tamamına düz yayılmış bir dağılımın teorik
sapmasıyla (`6.17 ms`) aynı mertebede; yani bant baştan sona kullanılıyor.

## Bilinen sınır: tick kuantizasyonu

Mod, tıklamaları `ClientTickEvents.END_CLIENT_TICK` içinde gönderiyor; bu olay
saniyede 20 kez (50 ms'de bir) çalışır. Dolayısıyla paketlerin çıkış anları
50 ms'nin katlarına oturur ve rastgelelik "hangi tick'te tıklama var" örüntüsüne
dönüşür (ölçümde: tick'lerin %75.5'inde 1 tıklama, %24.5'inde 0). Bu, modun
mevcut mimarisinden gelen bir sınır — zamanlayıcı değişikliği bunu kaldırmaz,
ama tick örüntüsünü rastgeleleştirir. Kaldırmak için tıklamanın kare (frame)
olayına taşınması gerekir, bu da tam bir Loom/Gradle derlemesi ister.

## Derleme

`ClickScheduler` Minecraft'a hiç dokunmadığı (yalnızca `ClickerConfig`
kullandığı) için tam Loom kurulumuna gerek yok; sınıf orijinal jar'a karşı
derlenip jar içinde değiştiriliyor. Mixin'ler ve intermediary eşlemeleri
olduğu gibi korunuyor.

```bash
./build-patch.sh /yol/humanclicker-0.1.0.jar dist/humanclicker-0.1.1.jar
```

Hazır çıktı: `dist/humanclicker-0.1.1.jar`. Orijinal jar ile karşılaştırıldığında
yalnızca iki girdi değişiyor: `com/humanclicker/ClickScheduler.class` ve
sürüm/açıklama için `fabric.mod.json`.

## Kurulum

`humanclicker-0.1.0.jar` dosyasını `mods` klasöründen silin, yerine
`humanclicker-0.1.1.jar` koyun. Ayar dosyası (`config/humanclicker.properties`)
ve komutlar aynı kalıyor:

```
/humanclicker              # durum
/humanclicker on|off
/humanclicker cps 13 18
/humanclicker jitter 0.18
/humanclicker air true|false
```
