# Vin Gallery

Aplikasi galeri foto Android native (Kotlin) — ringan, tema gelap, tanpa iklan.

## Fitur

- **Tab Foto**: grid 3 kolom semua foto dari MediaStore, urutan terbaru dulu
- **Tab Album**: grup per folder, album "Favorit" selalu di urutan pertama
- **Viewer fullscreen**: geser kiri-kanan, pinch-to-zoom / double-tap zoom, tampil nama file & tanggal
- **Aksi**: Bagikan, Hapus (konfirmasi + pakai system delete request di Android 11+), Favorit (bintang, tersimpan lokal)
- **Permission**: `READ_MEDIA_IMAGES` (Android 13+) / `READ_EXTERNAL_STORAGE` (di bawahnya), diminta saat pertama buka

## Teknologi

- Kotlin, classic Views (RecyclerView + ViewPager2), ViewBinding
- minSdk 24, targetSdk 34, AGP 8.5.2, Gradle 8.7, JDK 17
- Dependensi (semua dari Maven Central): Glide 4.16.0
- Pinch-to-zoom memakai `ZoomableImageView` buatan sendiri
  (`app/src/main/java/com/vinzdits/gallery/view/ZoomableImageView.kt`)
  karena library PhotoView hanya tersedia di JitPack.

## Cara build lokal

1. Install Android Studio (atau Android SDK + JDK 17).
2. Clone repo ini, buka di Android Studio.
3. Jalankan `./gradlew assembleDebug`.
4. APK ada di `app/build/outputs/apk/debug/app-debug.apk`.

## Cara download APK dari GitHub Actions

1. Push ke branch mana pun di GitHub — workflow **Build APK** jalan otomatis.
2. Buka tab **Actions** di repo → klik run yang sukses.
3. Di bagian bawah halaman run, download artifact **vin-gallery-debug-apk**.
4. Install APK-nya di HP (izinkan "install dari sumber tidak dikenal" sekali saja).

## Struktur project

```
app/src/main/
├── AndroidManifest.xml
├── java/com/vinzdits/gallery/
│   ├── MainActivity.kt            # Tab + runtime permission
│   ├── PhotoViewModel.kt          # Data foto & album (shared)
│   ├── GalleryFragment.kt         # Tab grid foto
│   ├── AlbumsFragment.kt          # Tab grid album
│   ├── AlbumDetailActivity.kt     # Isi satu album
│   ├── ViewerActivity.kt          # Viewer fullscreen + aksi
│   ├── adapter/                   # PhotoAdapter, AlbumAdapter, ViewerPagerAdapter
│   ├── data/                      # Photo, Album, MediaStoreRepository
│   ├── util/                      # FavoritesManager (SharedPreferences)
│   └── view/                      # ZoomableImageView (pinch-zoom)
└── res/                           # layout, values (Indonesia), drawable, mipmap
```
