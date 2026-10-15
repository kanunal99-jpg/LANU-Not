# LANU-Not — KANITLI TEKNİK DENETİM

**Tarih:** 15.10.2026  
**Kapsam:** GitHub dosya ağacı, seçili kaynak kodu, CI logları, Releases. Gerçek cihaz ve dış servislerin tam E2E testi yapılmadı.  
**Dosya sayısı (denetim anında):** 84  
**CI gözlemi:** 21.09.2026 Android APK Release başarılı; v1.0.4 APK var; toplam bir görünür test dosyası.  
**Anayasa:** [PROJE_ANAYASASI_VE_DUZELTILECEKLER.md](https://github.com/kanunal99-jpg/LANU-Not/blob/main/PROJE_ANAYASASI_VE_DUZELTILECEKLER.md)

## Kanıtlarla Düzeltilmesi Gerekenler

### 1. Senkronizasyon çakışması güncelleme yerine oluşturma tarihine bakıyor — P0 (DOĞRULANMIŞ VERİ KAYBI RİSKİ)
- **Kanıt:** [app/src/main/java/com/lanu/notdefteri/util/FirestoreSyncManager.kt#L308-L366](https://github.com/kanunal99-jpg/LANU-Not/blob/main/app/src/main/java/com/lanu/notdefteri/util/FirestoreSyncManager.kt#L308-L366); [CI koşusu](https://github.com/kanunal99-jpg/LANU-Not/actions/runs/35611281953)
- **Gözlem:** syncTwoWay remoteNote.createdAt > localNote.createdAt ile karşılaştırıyor; eşit/yerel daha yeni oluşturulmuşsa upload ediyor. Not düzenleme zamanına göre conflict çözüm yok.
- **Somut örnek / etkisi:** A cihazında eski not bugün değiştirildi, B cihazında yeni not var; createdAt son değişikliği anlatmaz ve yazma kaybı olabilir.
- **Düzeltme kabul testi:** updatedAt/revision/device ID ve silinme tombstone yaklaşımı; iki cihaz eşzamanlı düzenleme/çevrimdışı birleşme testleri, yedek ve geri alma.
- **Durum:** ARAŞTIRILDI, DÜZELTME HENÜZ BAŞLAMADI.

### 2. Firestore yapılandırmasında sabit görünümlü client anahtarı var — P1 (GÜVENLİK / KURULUM RİSKİ)
- **Kanıt:** [app/src/main/java/com/lanu/notdefteri/util/FirestoreSyncManager.kt#L160-L169](https://github.com/kanunal99-jpg/LANU-Not/blob/main/app/src/main/java/com/lanu/notdefteri/util/FirestoreSyncManager.kt#L160-L169); [CI koşusu](https://github.com/kanunal99-jpg/LANU-Not/actions/runs/35611281953)
- **Gözlem:** FirebaseOptions uygulama bulunmazsa kod içindeki bir API key stringi ve tahmini proje adıyla başlatılıyor; key'in geçerliliği ve auth/rule güvenliği kanıtlanmadı.
- **Somut örnek / etkisi:** Yeni yüklenmiş cihazda Firebase yapılandırması yoksa bulut eşitleme başlatma başarısız olabilir.
- **Düzeltme kabul testi:** Uygun Firebase yapılandırması, auth, güvenlik kuralları, session izolasyonu ve hatayı kullanıcıya gösterme; key'i kullanıcıya ifşa ederek güvenliği sağlamaya çalışma.
- **Durum:** ARAŞTIRILDI, DÜZELTME HENÜZ BAŞLAMADI.

### 3. Eşitleme ID'si saklı notlara erişim için tek başına yeterli olmamalı — P1 (GİZLİLİK RİSKİ)
- **Kanıt:** [app/src/main/java/com/lanu/notdefteri/util/FirestoreSyncManager.kt#L120-L141](https://github.com/kanunal99-jpg/LANU-Not/blob/main/app/src/main/java/com/lanu/notdefteri/util/FirestoreSyncManager.kt#L120-L141); [CI koşusu](https://github.com/kanunal99-jpg/LANU-Not/actions/runs/35611281953)
- **Gözlem:** Yalnız kısa UUID parçasıyla `LANU-XXXXXXXX` şeklinde ID oluşturuluyor, vault yolu syncId kullanıyor; Firestore Rules/kimlik bağlamı ayrıca kanıtlanmadı.
- **Somut örnek / etkisi:** ID'yi bilen yetkisiz kullanıcı hiçbir şartta notları okuyamamalı.
- **Düzeltme kabul testi:** Firestore güvenlik kuralları deny-by-default, auth, secret-sharing modeli, tahmin koruması, iki hesap çapraz veri testi.
- **Durum:** ARAŞTIRILDI, DÜZELTME HENÜZ BAŞLAMADI.

### 4. Tek E2E smoke sayısız özellik için dar kapsam — P2 (TEST KAPSAMI EKSİK)
- **Kanıt:** [app/src/test/java/com/lanu/notdefteri/E2ESmokeTest.kt](https://github.com/kanunal99-jpg/LANU-Not/blob/main/app/src/test/java/com/lanu/notdefteri/E2ESmokeTest.kt); [CI koşusu](https://github.com/kanunal99-jpg/LANU-Not/actions/runs/35611281953)
- **Gözlem:** Not, etiket, çizim, ses, PDF, Excel, PPT, bulut, hatırlatma var ancak otomatik test sayısı sınırlı.
- **Somut örnek / etkisi:** Çöp kutusundan geri yükleme, alarm veya PDF hata durumları regresyon yapabilir.
- **Düzeltme kabul testi:** Room migrations, dosya izinleri, bulut offline sync, export dosya açma, 2 cihaz senkronizasyonu testleri.
- **Durum:** ARAŞTIRILDI, DÜZELTME HENÜZ BAŞLAMADI.

## Zorunlu 4 Faz
1. Araştır/Envanter: kapsam, kanıt ve etki; bu rapor başlangıç seviyesinde seçili dosya/CI incelemesidir.
2. Kök neden ve düzeltme: P0/P1 önce; en küçük geri alınabilir kod değişimi; gizli veriyi koru; ücret için onay al.
3. Build + test: lint, unit/integrasyon/UI/E2E, offline/kesinti/fallback, gerekirse gerçek cihaz.
4. Doğrula ve yayınla: tüm testler yeşil; Android APK Releases altında SHA-256 ile doğrudan indirilebilir; smoke ve bağlantı doğrulaması.

**Bulgu kapatma:** Kod satırı + kök neden + düzeltme commit'i + test + CI + sürüm/ürün smoke kanıtı olmadan hiçbir madde bitmiş sayılmaz.
