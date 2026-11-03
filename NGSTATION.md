# NGStation

Launcher N-Gage ala PPSSPP untuk Android, dibangun di atas core [EKA2L1](https://github.com/EKA2L1/EKA2L1).
Setup cukup sekali; setelah itu pilih game dan langsung main.

## Yang dibutuhkan

1. **Firmware N-Gage**: hasil dump dari perangkat N-Gage/N-Gage QD milikmu sendiri,
   berupa file `.ROM` + `.RPKG` (atau `.VPL`). Taruh dalam satu folder.
   Firmware tidak disertakan karena dilindungi hak cipta.
2. **Game**: satu folder per game, isinya `System/Apps/...` (hasil dump kartu MMC),
   atau file `.zip` dari folder itu.

Contoh susunan:

```
Download/NGage/
├── Firmware/
│   ├── NGAGE.ROM
│   └── NGAGE.RPKG
└── Games/
    ├── Asphalt Urban GT/
    │   └── System/Apps/Asphalt/...
    ├── Pocket Kingdom/
    │   └── System/Apps/...
    └── Rayman 3.zip
```

## Cara pakai

1. Pasang `NGStation.apk` (dari halaman Releases, tag `ngstation-latest`).
2. Saat pertama dibuka, ikuti 3 langkah setup: izin akses file → pilih folder firmware → pilih folder game.
3. Di library, ketuk game. Ketukan pertama memasang game (sekali saja, aplikasi akan memuat ulang sebentar),
   lalu game langsung berjalan. Ketukan berikutnya langsung main.
4. Tekan lama sebuah game untuk mengatur kontrol/layar, memasang ulang, atau menghubungkannya ke aplikasi
   yang sudah terpasang.

Menu kanan atas berisi: muat ulang, ganti folder game, firmware/perangkat, pengaturan emulator,
ulangi setup, dan **Mode EKA2L1 lengkap** (tampilan asli EKA2L1, untuk fitur lanjutan).

## Build

APK di-build otomatis oleh GitHub Actions (`.github/workflows/ngstation.yml`) setiap push ke branch
`ngstation`/`master`/`main`. Hasilnya muncul sebagai artifact dan di release `ngstation-latest`.

Kode launcher ada di `src/emu/android/app/src/main/java/com/github/eka2l1/ngage/`.
APK ditandatangani dengan `src/emu/android/app/ngstation.jks` (sengaja disimpan di repo, untuk build pribadi),
supaya setiap build baru bisa langsung menimpa versi lama di HP.
