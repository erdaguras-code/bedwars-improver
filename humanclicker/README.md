# Human Clicker (Fabric mod)

Sol tusu basili tuttugun surece saldiri tiklamasini senin yerine tekrarlar.
Amac el/bilek yorulmasini azaltmak.

- **Minecraft:** 1.21.1 (Java Edition)
- **Yukleyici:** Fabric Loader 0.16.5+ / Fabric API 0.102.1+1.21.1
- **Java:** 21
- **Taraf:** sadece istemci (`environment: client`)

> Bu proje `bedwars-trainer` modundan tamamen bagimsizdir; kendi Gradle
> projesi, kendi jar'i vardir.

---

## Blok kirma neden bozulmuyor?

Minecraft'ta sol tusun iki ayri isi iki ayri yoldan yurur:

| Is | Neye bakar | Nerede |
|---|---|---|
| Blok kirma | Tusun **fiziksel** basili olmasi (`attackKey.isPressed()`) | `handleBlockBreaking(...)` |
| Saldiri | Tusun **basilma sayaci** (`attackKey.wasPressed()`) | `doAttack()` |

Bu mod fiziksel tus durumuna hic dokunmuyor — sadece vanilla'nin kendi
`doAttack()` metodunu fazladan cagiriyor. Ustune bir de:

**Nisan bir blogun uzerindeyken mod hicbir sey gondermez.** Kirma sirasinda
tamamen devre disi kalir, vanilla davranisi aynen calisir.

Varsayilan olarak mod **yalnizca bir varliga nisan alindiginda** tiklar
(`clickAir=false`). Boslukta tiklamayi acmak istersen `/humanclicker air true`
diyebilirsin; o durumda mod, bosa vurusun blok kirmayi geciktirmesini onlemek
icin nisan bir bloga dondugunde kendi biraktigi `attackCooldown` artigini
temizler.

---

## Hiz

Hiz tek bir sayi degil, bir **aralik**: varsayilan `13.0 - 18.0` CPS.
Efektif hiz bu bant icinden secilir ve birkac tiklamada bir yeniden secilir;
ayrica her aralik gaussian bir sapmayla oynatilir (`jitter`, varsayilan 0.18).

Ust sinir kodda 20 CPS'te kilitli (`ClickerConfig.MAX_CPS`).

Referans olarak insan hizlari: normal tiklama 5-8 CPS, iyi oyuncu 8-10,
jitter/butterfly 12-16.

---

## Kullanim

Jar'i Fabric API ile birlikte `mods/` klasorune at.

- **V** tusu: ac/kapa (Ayarlar -> Kontroller -> Human Clicker'dan degistirilebilir)
- `/humanclicker` — durum
- `/humanclicker on` / `off`
- `/humanclicker cps <min> <max>` — hiz bandi, orn. `/humanclicker cps 13 18`
- `/humanclicker jitter <0..0.5>` — aralik sapmasi
- `/humanclicker air <true|false>` — boslukta da tiklasin mi

Ayarlar `config/humanclicker.properties` dosyasinda saklanir.

Mod GUI acikken, imlec serbestken ve sol tus basili degilken calismaz.

---

## Kullanim sinirlari

Bu bir **autoclicker**. Cogu public sunucu (Hypixel dahil) autoclicker
kullanimini kural ihlali sayar ve bunu kullanma sebebine bakmaz. Kendi
singleplayer dunyanda veya kurallarinin izin verdigi bir sunucuda kullan.

Ayrica sunu net soyleyeyim: hiz bandini rastgeleleyerek "fark edilmez" hale
gelmiyorsun. Bu sistemler ortalama CPS'e degil tiklama araliklarinin
dagilimina bakar, ve bir formulden uretilen aralik ne kadar rastgelelestirilse
de kendi imzasini birakir. Bu mod tespitten kacmak icin tasarlanmadi.

Combat mekanigi notu: 1.9+ vanilla'da saldiri cooldown'u yuzunden yuksek CPS
vurus basina hasari **dusurur**. Yuksek CPS'in ise yaradigi yer 1.8 combat'tir
(orn. `bedwars-trainer` modunun PvP arenasi veya 1.8 mekanigi calistiran
sunucular).

---

## Derleme

```bash
cd humanclicker
gradle build
```

Cikti: `build/libs/human-clicker-0.1.0.jar`

> **Gradle 8.x sart.** Fabric Loom 1.7, Gradle 9 ile calismiyor
> (`Problems.forNamespace` API'si kaldirildi). Gradle 9+ kuruluysa
> `gradle wrapper --gradle-version 8.10` bile patlar, cunku wrapper gorevi de
> build.gradle'i degerlendirip Loom'u uygulamaya calisir. CI bu yuzden
> `gradle/actions/setup-gradle` ile surumu 8.10'a sabitliyor.

Repo koku push edildiginde `.github/workflows/humanclicker.yml` bunu GitHub
Actions'ta otomatik derler ve jar'i artifact olarak yukler.

---

## Dogrulama notlari — CI ile kontrol edilecek

Kod gercek Minecraft'a karsi derlenmeden yazildi. Asagidaki iki dosya
Minecraft ic isimlerine dokunuyor ve `⚠️ CI-DOGRULA` etiketiyle isaretli:

1. `mixin/MinecraftClientInvoker.java` — `doAttack()` imzasi (Yarn 1.21.1'de
   `private boolean doAttack()` olmali).
2. `mixin/MinecraftClientAccessor.java` — `attackCooldown` alan adi (Mojang
   adiyla `missTime`).

Ayrica `MinecraftClient.crosshairTarget` alaninin public olmasina guveniliyor;
degilse ona da bir accessor gerekir.
