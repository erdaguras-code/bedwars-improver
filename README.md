# Bedwars Trainer (Fabric mod)

Bedwars becerilerini (bridging, PvP, kaynak yonetimi) tek basina antrenman
yapabilecegin bir Minecraft dunyasi. "Dunya olustur" ekranindan **Bedwars
Antrenman** dunya tipini secince bos bir dunyada otomatik bir lobi olusur;
lobideki NPC'lere tiklayarak egitim bolumlerine gecersin.

- **Minecraft:** 1.21.1 (Java Edition)
- **Yukleyici:** Fabric Loader 0.16.5+ / Fabric API 0.102.1+1.21.1
- **Java:** 21

> Not: Surumler `gradle.properties` icinde. En guncel/ dogru degerleri her
> zaman https://fabricmc.net/develop adresinden kontrol edebilirsin.

---

## Nasil derlenir?

Gereken: JDK 21 kurulu olmali.

```bash
# proje klasorunde
./gradlew build        # Linux/Mac
gradlew.bat build      # Windows
```

Cikan mod dosyasi: `build/libs/bedwars-trainer-0.1.0.jar`

### GitHub Actions ile otomatik derleme (onerilen dogrulama yolu)
Bu repoda `.github/workflows/build.yml` var. Repoya **push** ettiginde
GitHub sunucularinda (JDK 21 + Gradle hazir) mod otomatik derlenir:
- Basarili olursa: derlenen `.jar` "Artifacts" altina yuklenir.
- Hata olursa: Actions log'unda tam hata satirlari gorunur.
Token/kimlik gerekmez; GitHub kendi otomatik token'iyla calisir.
Kurulum: yeni bir GitHub repo olustur -> bu dosyalari push et -> Actions sekmesine bak.

> `gradlew` sarmalayici dosyalari (gradle-wrapper.jar + gradlew scriptleri)
> bu pakette yok. IntelliJ IDEA'da projeyi acinca otomatik olusur; ya da
> tek seferlik `gradle wrapper --gradle-version 8.10` calistirabilirsin.

### IntelliJ IDEA ile gelistirme (onerilen)
1. IntelliJ IDEA'yi ac -> **Open** -> bu klasoru sec.
2. Gradle senkronu bitene kadar bekle (bagimliliklar iner).
3. `runClient` gorevini calistir -> gelistirme Minecraft'i acilir.
4. Kod hatalari IDE'de aninda gozukur (asagidaki "Dogrulama notlari").

---

## Nasil kullanilir?

1. Modu (ve Fabric API'yi) `mods` klasorune koy, oyunu ac.
2. **Singleplayer -> Create New World**.
3. **World Type** dugmesine basip **Bedwars Antrenman**'i sec.
4. Dunyaya girince lobiye isinlanirsin.
5. NPC'lere **sag tikla** -> ilgili modul arenasina gidersin.

**Test komutlari (herhangi bir dunyada calisir):**
- `/trainer lobby` — lobiyi kurar ve seni isinlar
- `/trainer goto bridging|pvp|resources` — modul arenasina isinlar
- `/trainer build` — lobiyi yeniden kurar
- `/trainer pvp start [seviye]` — PvP arenasina gecer, botu baslatir (1.8 combat acilir)
- `/trainer pvp stop` — botu durdurur, combat'i normale dondurur

---

## Proje yapisi

```
src/main/
  java/com/bedwarstrainer/
    BedwarsTrainerMod.java      # giris noktasi, olay kayitlari
    module/TrainingModule.java  # modul tanimlari (konum, hedef, isim)
    lobby/LobbyBuilder.java     # lobi + iskelet arenalari blok blok kurar
    event/InteractionHandler.java # NPC'ye tiklama -> isinlanma
    command/TrainerCommands.java  # /trainer komutlari
    util/TeleportUtil.java      # isinlanma cagrisi tek yerde (surum guvenligi)
  resources/
    fabric.mod.json
    bedwarstrainer.mixins.json
    assets/bedwarstrainer/lang/{en_us,tr_tr}.json
    data/bedwarstrainer/worldgen/world_preset/trainer_lobby.json  # ozel dunya tipi
    data/minecraft/tags/worldgen/world_preset/normal.json         # tipi menude gosterir
```

### Mimari mantik
- **Dunya tipi datapack ile eklenir** (Java/mixin yok) -> surumler arasi stabil.
  Void bir dunya uretir; lobi ve arenalar kod tarafindan insa edilir.
- **Tespit:** sunucu basinca spawn cevresi bostaysa (void) lobi kurulur.
  "Kuruldu mu?" isareti origin altindaki bir beacon blogu ile tutulur
  (PersistentState API'sine bagimli olmamak icin - o API 1.21'de degisti).
- **Modul eklemek** = `TrainingModule` enum'una bir satir eklemek.

---

## Dogrulama notlari — CI ile duzeltilecek riskli noktalar

Kod gercek Minecraft'a karsi derlenmeden yazildi. Lobi/menu/komut kismi
yuksek guvenilir. Asagidaki dosyalar Minecraft'in EN IC kisimlarina
dokunuyor; ilk `gradlew build`/Actions ciktisinda hata verebilir, kod
icinde `⚠️ CI-DOGRULA` etiketiyle isaretlendi:

1. `pvp/fake/FakePlayerBot.java` — ServerPlayerEntity kurucu, onPlayerConnect,
   ConnectedClientData, SyncedClientOptions imzalari (fake player kurulumu).
2. `pvp/fake/FakeClientConnection.java` — override edilecek `send`/baglanti
   metotlari surumle degisebilir (referans: Carpet FakeClientConnection).
3. `mixin/ClientConnectionAccessor.java` — `channel` alan adi.
4. `mixin/PlayerAttackCooldownMixin.java` — `getAttackCooldownProgress(F)F` imzasi.
5. `mixin/LivingEntityKnockbackMixin.java` — `takeKnockback` imzasi, `velocityModified` alani.
6. `pvp/BotController.java` — hareket fizigi (playtest ile ince ayar).

Is akisi: push et -> Actions log'unu bana yapistir -> tam isabetle duzeltelim.

---

## Yol haritasi

- [x] **Modul 1 — Lobi + menu sistemi**
- [x] **Modul 2 v2 — PvP + tam 1.8 combat:**
      - `PlayerAttackCooldownMixin`: cooldown kaldirildi (spam = tam hasar)
      - `LivingEntityKnockbackMixin`: 1.8 knockback formulu (OldCombatMechanics mantigi)
      - **Fake player bot** (gercek oyuncu entity): sprint atar, taktiksel
        yaklasir/strafe atar (bodoslama YOK), gercek oyuncu saldiri kodundan
        gecer -> otantik sprint-reset knockback. Tek bot, artan seviye.
      - Not: netcode kismi CI ile dogrulanacak (yukaridaki liste).
- [ ] Bot davranis ince ayari (playtest sonrasi)
- [ ] Sweep saldirisini kapatma (1v1'de onemsiz; ileride opsiyonel)
- [ ] **Modul 3 — Bridging egitimi:** speedbridge / ninja / moonwalk icin
      adim adim uygulamali anlatim, basarana kadar bekleyen kontrol, sure/skor.
- [ ] Kaynak yonetimi modulu (jeneratör timing'i, upgrade önceligi vb.)
- [ ] Skor/istatistik takibi, kayit
