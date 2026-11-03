## NGStation

Launcher N-Gage ala PPSSPP untuk Android: setup sekali, lalu pilih game dan main. Dibangun di atas core [EKA2L1](https://github.com/EKA2L1/EKA2L1); bagian emulasinya tidak diubah.

**Unduh:** file `NGStation.apk` di bawah. Pasang menimpa versi lama; firmware dan game yang sudah terpasang tetap ada.

- Target: Android, game N-Gage generasi pertama (S60v1), format folder atau `.zip`
- Firmware: disediakan sendiri (dump N-Gage / N-Gage QD), tidak dibundel. Taruh ZIP-nya di `Games/N-Gage/Firmware`, nanti terpasang otomatis
- Bisa terpasang berdampingan dengan EKA2L1 resmi (app id `com.github.eka2l1.ngstation`)

## Riwayat rilis

Semua build pada 3 November 2026, terbaru di atas.

### Build 5 — Layout N-Gage QD (`52aa60e`)
- **Baru:** layout tombol gaya N-Gage QD. D-pad berbentuk salib dengan titik oranye di cekungan bulat (bagian yang ditekan menyala oranye), keypad melengkung di panel arang, soft-L dan tombol panggil di bawah D-pad, soft-R di atas keypad, tombol tutup di kanan bawah
- **Baru:** warna tombol ala QD. Tulisan oranye, kolom tengah (2/5/8/0) hitam, angka lain silver, tombol fungsi arang, oranye saat ditekan
- **Baru:** bodi abu-abu di sekitar layar game, dengan bingkai hitam
- **Perbaikan:** tombol selalu tampil transparan karena keyboard menganggap seluruh layar adalah layar game; posisi layar game kini dihitung dengan rumus yang sama dengan core

### Build 4 — Firmware otomatis (`b528239`)
- **Perbaikan:** aplikasi tertutup sendiri setelah memasang firmware; kini file hanya disalin lalu aplikasi memuat ulang dengan rapi
- **Baru:** firmware dipasang otomatis. ZIP atau folder paket firmware di `Games/N-Gage/Firmware`, `Games/N-Gage`, atau `Download` dikenali tanpa perlu dipilih
- **Baru:** folder `Games/N-Gage/Firmware` dibuat bersama folder game; ZIP firmware tidak ikut tampil sebagai game
- **Perbaikan:** folder `roms/<kode>` disalin dengan huruf kecil, sesuai yang dicari core

### Build 3 — Tema N-Gage, loading, landscape (`976b079`, `95aaf0d`)
- **Baru:** tema hitam–merah–putih untuk setup dan library
- **Baru:** folder `Games/N-Gage` (berisi `BACA SAYA.txt`) dibuat otomatis begitu izin akses file diberikan, dan dipakai sebagai folder game default
- **Baru:** layar loading saat game dimuat; hilang sendiri begitu gambar game muncul, bisa diketuk untuk ditutup
- **Baru:** default per game landscape, layar game di tengah, layout tombol "N-Gage" yang menjauhi sudut layar yang membulat

### Build 2 — Paket firmware (`9a0e464`)
- **Perbaikan:** aplikasi langsung tertutup saat pertama dibuka; layar setup kini menunggu core emulator siap
- **Baru:** paket firmware EKA2L1 (folder `Data` berisi `devices.yml`, `drives`, `roms`) bisa dipasang dari folder atau `.zip`, digabung tanpa menghapus perangkat lama

### Build 1 — Rilis pertama (`eb645fd`)
- **Baru:** wizard setup 3 langkah: izin akses file, firmware dari satu folder (`.ROM`/`.RPKG`/`.VPL` dikenali otomatis), folder game
- **Baru:** library ala PPSSPP. Tiap folder game (`System/Apps`) atau `.zip` jadi satu kotak; ketukan pertama memasang game, berikutnya langsung main
- **Baru:** perangkat N-Gage dipilih otomatis; tekan lama game untuk pengaturan, pasang ulang, atau menghubungkan ke aplikasi terpasang
- **Baru:** build APK otomatis lewat GitHub Actions, dengan rilis `ngstation-latest`

## Batasan
- **Save state / load state:** belum ada; core EKA2L1 belum mendukungnya
- **Upscaling:** tersedia lewat shader per game (xBR, 4xHQ, LCD, CRT, dan lainnya) di Pengaturan game; berupa filter, bukan render ulang di resolusi tinggi
- **Layout tersimpan:** game yang layout atau pengaturannya pernah disimpan tetap memakai yang lama; pilih layout "N-Gage" di editor layout tombol
