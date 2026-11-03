/*
 * Copyright (c) 2026 NGStation contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.github.eka2l1.ngage;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;

import com.github.eka2l1.R;
import com.github.eka2l1.applist.DeviceListFragment;
import com.github.eka2l1.emu.Emulator;
import com.github.eka2l1.util.AppUtils;
import com.github.eka2l1.util.FileUtils;
import com.github.eka2l1.util.PickDirResultContract;
import com.github.eka2l1.util.SAFDirResultContract;

import java.io.File;
import java.util.Locale;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * First-run setup: storage access, the N-Gage firmware (once), and the games folder.
 * Everything after this is the library screen.
 */
public class SetupWizardFragment extends Fragment {
    private final CompositeDisposable disposables = new CompositeDisposable();

    private TextView storageStatus;
    private TextView firmwareStatus;
    private TextView gamesStatus;
    private Button finishButton;
    private LinearLayout storageCard;

    private final ActivityResultLauncher<Void> firmwarePathPicker =
            registerForActivityResult(new PickDirResultContract(), this::onFirmwareFolder);
    private final ActivityResultLauncher<Void> firmwareTreePicker =
            registerForActivityResult(new SAFDirResultContract(true), this::onFirmwareTree);
    private final ActivityResultLauncher<Void> gamesPathPicker =
            registerForActivityResult(new PickDirResultContract(), this::onGamesFolder);
    private final ActivityResultLauncher<Void> gamesTreePicker =
            registerForActivityResult(new SAFDirResultContract(true), this::onGamesTree);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context ctx = requireContext();
        ScrollView scroll = new ScrollView(ctx);
        scroll.setFillViewport(true);
        LinearLayout root = Ui.column(ctx);
        int pad = Ui.dp(ctx, 16);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        root.addView(Ui.text(ctx, "Selamat datang di NGStation", 22, true));
        TextView intro = Ui.text(ctx, "Tiga langkah sekali saja, setelah itu tinggal pilih game dan main.", 14, false);
        intro.setPadding(0, Ui.dp(ctx, 4), 0, Ui.dp(ctx, 16));
        root.addView(intro);

        // Step 1: storage access (Android 11+ only).
        storageCard = Ui.card(ctx);
        storageCard.addView(Ui.text(ctx, "1. Izin akses file", 17, true));
        storageCard.addView(Ui.text(ctx, "Supaya emulator bisa membaca folder game dan firmware langsung dari penyimpanan.", 14, false));
        storageStatus = Ui.text(ctx, "", 14, true);
        storageCard.addView(storageStatus);
        Button storageButton = Ui.button(ctx, "Beri izin akses semua file");
        storageButton.setOnClickListener(v -> openAllFilesAccessSettings());
        storageCard.addView(storageButton);
        root.addView(storageCard);

        // Step 2: firmware.
        LinearLayout firmwareCard = Ui.card(ctx);
        firmwareCard.addView(Ui.text(ctx, "2. Firmware N-Gage", 17, true));
        firmwareCard.addView(Ui.text(ctx,
                "Pilih folder yang berisi hasil dump perangkat N-Gage kamu: file .ROM dan .RPKG "
                        + "(atau file .VPL firmware). File dikenali otomatis.", 14, false));
        firmwareStatus = Ui.text(ctx, "", 14, true);
        firmwareCard.addView(firmwareStatus);
        Button firmwareButton = Ui.button(ctx, "Pilih folder firmware");
        firmwareButton.setOnClickListener(v -> pickFirmware());
        firmwareCard.addView(firmwareButton);
        Button manualButton = Ui.button(ctx, "Pasang manual (layar perangkat EKA2L1)");
        manualButton.setOnClickListener(v -> openDeviceManager());
        firmwareCard.addView(manualButton);
        root.addView(firmwareCard);

        // Step 3: games folder.
        LinearLayout gamesCard = Ui.card(ctx);
        gamesCard.addView(Ui.text(ctx, "3. Folder game", 17, true));
        gamesCard.addView(Ui.text(ctx,
                "Pilih folder yang berisi game-game N-Gage. Tiap game satu folder "
                        + "(berisi System/Apps), atau file .zip.", 14, false));
        gamesStatus = Ui.text(ctx, "", 14, true);
        gamesCard.addView(gamesStatus);
        Button gamesButton = Ui.button(ctx, "Pilih folder game");
        gamesButton.setOnClickListener(v -> pickGamesFolder());
        gamesCard.addView(gamesButton);
        root.addView(gamesCard);

        finishButton = Ui.button(ctx, "Selesai — buka library");
        finishButton.setOnClickListener(v -> finishSetup());
        root.addView(finishButton);

        return scroll;
    }

    @Override
    public void onResume() {
        super.onResume();
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setTitle("Setup NGStation");
        }
        refresh();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }

    private void refresh() {
        Context ctx = requireContext();

        boolean needsStoragePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
        storageCard.setVisibility(needsStoragePermission ? View.VISIBLE : View.GONE);
        if (FileUtils.hasDirectStorageAccess()) {
            setStatus(storageStatus, "✓ Izin sudah diberikan", Ui.OK);
        } else {
            setStatus(storageStatus, "Belum diizinkan (disarankan; tanpa ini dipakai mode SAF yang lebih lambat)", Ui.MUTED);
        }

        String[] devices = Emulator.getDevices();
        if (devices != null && devices.length > 0) {
            String names = TextUtils.join(", ", devices);
            setStatus(firmwareStatus, "✓ Terpasang: " + names, Ui.OK);
        } else {
            setStatus(firmwareStatus, "Belum ada firmware", Ui.MUTED);
        }

        String gamesDir = NGageLibrary.getGamesDir(ctx);
        if (gamesDir != null) {
            setStatus(gamesStatus, "✓ " + displayPath(gamesDir), Ui.OK);
        } else {
            setStatus(gamesStatus, "Belum dipilih", Ui.MUTED);
        }

        finishButton.setEnabled(devices != null && devices.length > 0 && gamesDir != null);
    }

    private static void setStatus(TextView view, String text, int color) {
        view.setText(text);
        view.setTextColor(color);
        view.setPadding(0, 12, 0, 0);
    }

    static String displayPath(String path) {
        if (NGageLibrary.isContentUri(path)) {
            String decoded = Uri.decode(path);
            int idx = decoded.lastIndexOf(':');
            return idx >= 0 ? decoded.substring(idx + 1) : decoded;
        }
        return path;
    }

    // ------------------------------------------------------------------ storage

    private void openAllFilesAccessSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return;
        }
        Uri packageUri = Uri.parse("package:" + requireContext().getPackageName());
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, packageUri));
            return;
        } catch (ActivityNotFoundException ignored) {
        }
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
        } catch (ActivityNotFoundException ignored) {
            Toast.makeText(getContext(), "Pengaturan izin tidak ditemukan di perangkat ini", Toast.LENGTH_LONG).show();
        }
    }

    // ----------------------------------------------------------------- firmware

    private void pickFirmware() {
        if (FileUtils.hasDirectStorageAccess()) {
            firmwarePathPicker.launch(null);
        } else {
            firmwareTreePicker.launch(null);
        }
    }

    private void openDeviceManager() {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.container, new DeviceListFragment())
                .addToBackStack(null)
                .commit();
    }

    private void onFirmwareFolder(String path) {
        if (path == null) {
            return;
        }
        File[] files = new File(path).listFiles();
        String rom = null, rpkg = null, vpl = null;
        if (files != null) {
            for (File f : files) {
                if (!f.isFile()) {
                    continue;
                }
                String name = f.getName().toLowerCase(Locale.ROOT);
                if (name.endsWith(".rom") && (rom == null || f.length() > new File(rom).length())) {
                    rom = f.getAbsolutePath();
                } else if (name.endsWith(".rpkg") && rpkg == null) {
                    rpkg = f.getAbsolutePath();
                } else if (name.endsWith(".vpl") && vpl == null) {
                    vpl = f.getAbsolutePath();
                }
            }
        }
        installFirmware(rom, rpkg, vpl);
    }

    private void onFirmwareTree(String treeUri) {
        if (treeUri == null) {
            return;
        }
        DocumentFile root = DocumentFile.fromTreeUri(requireContext(), Uri.parse(treeUri));
        String rom = null, rpkg = null, vpl = null;
        long romSize = -1;
        if (root != null) {
            for (DocumentFile f : root.listFiles()) {
                if (!f.isFile() || f.getName() == null) {
                    continue;
                }
                String name = f.getName().toLowerCase(Locale.ROOT);
                if (name.endsWith(".rom") && f.length() > romSize) {
                    rom = f.getUri().toString();
                    romSize = f.length();
                } else if (name.endsWith(".rpkg") && rpkg == null) {
                    rpkg = f.getUri().toString();
                } else if (name.endsWith(".vpl") && vpl == null) {
                    vpl = f.getUri().toString();
                }
            }
        }
        installFirmware(rom, rpkg, vpl);
    }

    @SuppressLint("CheckResult")
    private void installFirmware(String rom, String rpkg, String vpl) {
        final String rpkgPath;
        final String romPath;
        final boolean installRpkg;

        if (rom != null) {
            if (Emulator.doesRomNeedRPKG(rom) && rpkg == null) {
                showError("File .ROM ditemukan, tapi firmware ini juga butuh file .RPKG "
                        + "(isi drive Z: hasil dump). Taruh keduanya di folder yang sama.");
                return;
            }
            romPath = rom;
            rpkgPath = rpkg == null ? "" : rpkg;
            installRpkg = true;
        } else if (vpl != null) {
            romPath = vpl;
            rpkgPath = "";
            installRpkg = false;
        } else {
            showError("Tidak ada file .ROM atau .VPL di folder itu.");
            return;
        }

        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.setMessage("Memasang firmware… bisa makan waktu beberapa menit.");
        dialog.show();

        disposables.add(Emulator.subscribeInstallDevice(rpkgPath, romPath, installRpkg, false)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        dialog.dismiss();
                        Toast.makeText(getContext(), "Firmware terpasang", Toast.LENGTH_SHORT).show();
                        refresh();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        dialog.dismiss();
                        int code = -1;
                        try {
                            code = Integer.parseInt(e.getMessage());
                        } catch (NumberFormatException ignored) {
                        }
                        showError(describeDeviceError(code));
                    }
                }));
    }

    static String describeDeviceError(int code) {
        switch (code) {
            case Emulator.INSTALL_DEVICE_ERROR_NOT_EXIST:
                return "File firmware tidak bisa dibaca.";
            case Emulator.INSTALL_DEVICE_ERROR_INSUFFICENT:
                return "Ruang penyimpanan tidak cukup.";
            case Emulator.INSTALL_DEVICE_ERROR_RPKG_CORRUPT:
                return "File .RPKG rusak atau bukan dump yang valid.";
            case Emulator.INSTALL_DEVICE_ERROR_DETERMINE_PRODUCT_FAIL:
                return "Jenis perangkat tidak dikenali dari firmware ini.";
            case Emulator.INSTALL_DEVICE_ERROR_ALREADY_EXIST:
                return "Perangkat ini sudah terpasang.";
            case Emulator.INSTALL_DEVICE_ERROR_ROM_FAIL_TO_COPY:
                return "Gagal menyalin file .ROM.";
            case Emulator.INSTALL_DEVICE_ERROR_VPL_FILE_INVALID:
                return "File .VPL tidak valid.";
            case Emulator.INSTALL_DEVICE_ERROR_ROFS_CORRUPTED:
            case Emulator.INSTALL_DEVICE_ERROR_ROM_CORRUPTED:
            case Emulator.INSTALL_DEVICE_ERROR_FPSX_CORRUPTED:
                return "File firmware rusak.";
            default:
                return "Gagal memasang firmware (kode " + code + ").";
        }
    }

    // -------------------------------------------------------------------- games

    private void pickGamesFolder() {
        if (FileUtils.hasDirectStorageAccess()) {
            gamesPathPicker.launch(null);
        } else {
            gamesTreePicker.launch(null);
        }
    }

    private void onGamesFolder(String path) {
        if (path == null) {
            return;
        }
        NGageLibrary.setGamesDir(requireContext(), path);
        refresh();
    }

    private void onGamesTree(String treeUri) {
        if (treeUri == null) {
            return;
        }
        try {
            requireContext().getContentResolver().takePersistableUriPermission(Uri.parse(treeUri),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (SecurityException ignored) {
        }
        NGageLibrary.setGamesDir(requireContext(), treeUri);
        refresh();
    }

    // ------------------------------------------------------------------- finish

    private void finishSetup() {
        NGageLibrary.setSetupDone(requireContext(), true);
        // The core only picks up a freshly installed device on a new process.
        AppUtils.restart(requireContext());
    }

    private void showError(String message) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Ada masalah")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
