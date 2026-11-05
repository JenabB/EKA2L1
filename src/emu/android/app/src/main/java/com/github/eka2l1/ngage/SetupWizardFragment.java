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
import com.github.eka2l1.util.SAFFileResultContract;

import java.io.File;
import java.util.Locale;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * First-run setup: storage access, the N-Gage firmware (once), and the games folder.
 * Everything after this is the library screen.
 */
public class SetupWizardFragment extends Fragment {
    private final CompositeDisposable disposables = new CompositeDisposable();

    private StepCard storageStep;
    private StepCard firmwareStep;
    private StepCard gamesStep;
    private Button finishButton;
    private TextView progressText;
    private final View[] progressDots = new View[3];
    private View content;
    private TextView loadingText;
    private View loadingView;
    private boolean nativeReady;
    private boolean autoFirmwareChecked;

    private final ActivityResultLauncher<Void> firmwarePathPicker =
            registerForActivityResult(new PickDirResultContract(), this::onFirmwareFolder);
    private final ActivityResultLauncher<Void> firmwareTreePicker =
            registerForActivityResult(new SAFDirResultContract(true), this::onFirmwareTree);
    private final ActivityResultLauncher<String[]> firmwareZipPicker =
            registerForActivityResult(new SAFFileResultContract(), this::onFirmwareZip);
    private final ActivityResultLauncher<Void> gamesPathPicker =
            registerForActivityResult(new PickDirResultContract(), this::onGamesFolder);
    private final ActivityResultLauncher<Void> gamesTreePicker =
            registerForActivityResult(new SAFDirResultContract(true), this::onGamesTree);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context ctx = requireContext();
        android.widget.FrameLayout frame = new android.widget.FrameLayout(ctx);
        Ui.setBackground(frame);

        LinearLayout loading = Ui.column(ctx);
        loading.setGravity(android.view.Gravity.CENTER);
        android.widget.ProgressBar spinner = new android.widget.ProgressBar(ctx);
        spinner.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(Ui.ORANGE));
        loading.addView(spinner);
        loadingText = Ui.muted(ctx, "Preparing the emulator…", 15);
        loadingText.setGravity(android.view.Gravity.CENTER);
        loadingText.setPadding(0, Ui.dp(ctx, 12), 0, 0);
        loading.addView(loadingText);
        loadingView = loading;
        frame.addView(loading, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

        ScrollView scroll = new ScrollView(ctx);
        scroll.setFillViewport(true);
        scroll.setVisibility(View.GONE);
        content = scroll;
        frame.addView(scroll);
        LinearLayout root = Ui.column(ctx);
        int pad = Ui.dp(ctx, 18);
        root.setPadding(pad, Ui.dp(ctx, 8), pad, pad);
        scroll.addView(root);

        // Hero: the handheld, the brand and how far along setup is.
        android.widget.ImageView hero = new android.widget.ImageView(ctx);
        hero.setImageResource(R.drawable.ngs_hero);
        hero.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        hero.setAdjustViewBounds(true);
        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(ctx, 150));
        root.addView(hero, heroLp);

        TextView brand = Ui.text(ctx, Ui.brand(), 30, true);
        brand.setLetterSpacing(0.14f);
        brand.setGravity(android.view.Gravity.CENTER);
        root.addView(brand);
        TextView tagline = Ui.muted(ctx, "Play N-Gage games on Android", 14);
        tagline.setGravity(android.view.Gravity.CENTER);
        tagline.setPadding(0, Ui.dp(ctx, 2), 0, Ui.dp(ctx, 14));
        root.addView(tagline);

        LinearLayout progress = Ui.row(ctx);
        progress.setGravity(android.view.Gravity.CENTER);
        progress.setPadding(0, 0, 0, Ui.dp(ctx, 16));
        for (int i = 0; i < progressDots.length; i++) {
            View dot = new View(ctx);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(ctx, 28), Ui.dp(ctx, 6));
            lp.rightMargin = Ui.dp(ctx, 6);
            dot.setBackground(Ui.rounded(ctx, Ui.SURFACE_HI, 0, 3));
            progress.addView(dot, lp);
            progressDots[i] = dot;
        }
        progressText = Ui.muted(ctx, "", 13);
        progressText.setPadding(Ui.dp(ctx, 6), 0, 0, 0);
        progress.addView(progressText);
        root.addView(progress);

        // Step 1: storage access (Android 11+ only).
        storageStep = new StepCard(ctx, "1", "File access",
                "Lets the emulator read games and firmware straight from storage, "
                        + "and create the Games/N-Gage folder for you.");
        Button storageButton = Ui.button(ctx, "Allow access to all files");
        storageButton.setOnClickListener(v -> openAllFilesAccessSettings());
        storageStep.addAction(storageButton);
        root.addView(storageStep.card);

        // Step 2: firmware.
        firmwareStep = new StepCard(ctx, "2", "Firmware",
                "Put the firmware pack .ZIP in Games/N-Gage/Firmware and reopen the app: "
                        + "it installs automatically. Or pick it yourself below.");
        Button zipButton = Ui.button(ctx, "Choose firmware ZIP");
        zipButton.setOnClickListener(v -> firmwareZipPicker.launch(new String[]{"application/zip"}));
        firmwareStep.addAction(zipButton);
        Button firmwareButton = Ui.secondaryButton(ctx, "Choose firmware folder");
        firmwareButton.setOnClickListener(v -> pickFirmware());
        firmwareStep.addAction(firmwareButton);
        Button manualButton = Ui.textButton(ctx, "Install manually (ROM + RPKG)");
        manualButton.setOnClickListener(v -> openDeviceManager());
        firmwareStep.addAction(manualButton);
        root.addView(firmwareStep.card);

        // Step 3: games folder.
        gamesStep = new StepCard(ctx, "3", "Games folder",
                "Put each game here as its own folder (containing System/Apps) or a .zip file "
                        + "in this folder.");
        Button defaultButton = Ui.button(ctx, "Use Games/N-Gage");
        defaultButton.setOnClickListener(v -> useDefaultGamesFolder(true));
        gamesStep.addAction(defaultButton);
        Button gamesButton = Ui.secondaryButton(ctx, "Choose another folder");
        gamesButton.setOnClickListener(v -> pickGamesFolder());
        gamesStep.addAction(gamesButton);
        root.addView(gamesStep.card);

        finishButton = Ui.button(ctx, "Start playing");
        finishButton.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 17);
        finishButton.setOnClickListener(v -> finishSetup());
        root.addView(finishButton);

        TextView note = Ui.muted(ctx, "Installed games and their saves are kept in the app folder, "
                + "so they are removed if the app is uninstalled. Firmware in Games/N-Gage/Firmware "
                + "is reinstalled automatically.", 12);
        note.setPadding(Ui.dp(ctx, 4), Ui.dp(ctx, 16), Ui.dp(ctx, 4), 0);
        root.addView(note);

        return frame;
    }

    @Override
    public void onResume() {
        super.onResume();
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
        Ui.applyChrome(activity, actionBar, "Setup");
        if (nativeReady) {
            refresh();
            return;
        }
        // Every native call needs the core started first, even with no device installed yet.
        disposables.add(Single.fromCallable(Emulator::ensureNativeStarted)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableSingleObserver<Boolean>() {
                    @Override
                    public void onSuccess(@NonNull Boolean deviceReady) {
                        nativeReady = true;
                        loadingView.setVisibility(View.GONE);
                        content.setVisibility(View.VISIBLE);
                        refresh();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        loadingText.setText("Could not prepare the emulator: " + e.getMessage());
                    }
                }));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }

    private void refresh() {
        if (!nativeReady || !isAdded()) {
            return;
        }
        Context ctx = requireContext();

        boolean needsStoragePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
        storageStep.card.setVisibility(needsStoragePermission ? View.VISIBLE : View.GONE);
        boolean storageOk = FileUtils.hasDirectStorageAccess();
        if (storageOk) {
            storageStep.setState(true, "Access granted");
            // First time with storage access: create Games/N-Gage and use it.
            if (NGageLibrary.getGamesDir(ctx) == null) {
                useDefaultGamesFolder(false);
            }
        } else {
            storageStep.setState(false, "Recommended; without it a slower mode is used");
        }

        String[] devices = Emulator.getDevices();
        boolean hasDevice = devices != null && devices.length > 0;
        firmwareStep.setState(hasDevice, hasDevice
                ? "Installed: " + TextUtils.join(", ", devices)
                : "No firmware yet");

        String gamesDir = NGageLibrary.getGamesDir(ctx);
        gamesStep.setState(gamesDir != null, gamesDir != null ? displayPath(gamesDir) : "Not chosen yet");

        int done = (storageOk || !needsStoragePermission ? 1 : 0) + (hasDevice ? 1 : 0) + (gamesDir != null ? 1 : 0);
        for (int i = 0; i < progressDots.length; i++) {
            progressDots[i].setBackground(Ui.rounded(ctx, i < done ? Ui.ORANGE : Ui.SURFACE_HI, 0, 3));
        }
        progressText.setText(done == 3 ? "Ready to play" : done + " of 3 steps");

        finishButton.setEnabled(hasDevice && gamesDir != null);
        if (!hasDevice) {
            autoInstallFirmware();
        }
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
            Toast.makeText(getContext(), "The permission settings screen was not found on this device", Toast.LENGTH_LONG).show();
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
        File packRoot = FirmwarePack.findPackRoot(new File(path));
        if (packRoot != null) {
            installPack(() -> FirmwarePack.installFromFolder(packRoot, dataDir()));
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
        DocumentFile packRoot = FirmwarePack.findPackRoot(root);
        if (packRoot != null) {
            Context app = requireContext().getApplicationContext();
            installPack(() -> FirmwarePack.installFromDocument(app, packRoot, dataDir()));
            return;
        }
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

    private void onFirmwareZip(String uri) {
        if (uri == null) {
            return;
        }
        Context app = requireContext().getApplicationContext();
        installPack(() -> FirmwarePack.installFromZip(app, Uri.parse(uri), dataDir()));
    }

    private static File dataDir() {
        return new File(Emulator.getEmulatorDir(), "data");
    }

    private interface PackJob {
        void run() throws Exception;
    }

    private void installPack(PackJob job) {
        installPack(job, "Copying the firmware pack… this can take a few minutes.");
    }

    private void installPack(PackJob job, String message) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.setMessage(message);
        dialog.show();

        Context app = requireContext().getApplicationContext();
        // Only files are copied here. The core reads devices.yml at start-up, so the new
        // device is picked up by restarting; asking the half-started core to switch device
        // from this thread is what used to crash the app.
        disposables.add(Completable.fromAction(job::run)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        dialog.dismiss();
                        Toast.makeText(app, "Firmware installed. Restarting…", Toast.LENGTH_SHORT).show();
                        AppUtils.restart(app);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        dialog.dismiss();
                        showError("Failed to install the firmware pack: " + e.getMessage());
                    }
                }));
    }

    /** First run with storage access and no device yet: install firmware found on the phone. */
    private void autoInstallFirmware() {
        if (autoFirmwareChecked || !FileUtils.hasDirectStorageAccess()) {
            return;
        }
        autoFirmwareChecked = true;
        disposables.add(Single.fromCallable(() -> {
                    FirmwarePack.Found found = FirmwarePack.findOnDevice();
                    return found == null ? new Object[0] : new Object[]{found};
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableSingleObserver<Object[]>() {
                    @Override
                    public void onSuccess(@NonNull Object[] result) {
                        if (result.length == 0 || !isAdded()) {
                            return;
                        }
                        FirmwarePack.Found found = (FirmwarePack.Found) result[0];
                        String where = found.location().getAbsolutePath();
                        if (found.zip != null) {
                            installPack(() -> FirmwarePack.installFromZipFile(found.zip, dataDir()),
                                    "Firmware found:\n" + where + "\n\nInstalling automatically…");
                        } else {
                            installPack(() -> FirmwarePack.installFromFolder(found.packRoot, dataDir()),
                                    "Firmware found:\n" + where + "\n\nInstalling automatically…");
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                    }
                }));
    }

    @SuppressLint("CheckResult")
    private void installFirmware(String rom, String rpkg, String vpl) {
        final String rpkgPath;
        final String romPath;
        final boolean installRpkg;

        if (rom != null) {
            if (Emulator.doesRomNeedRPKG(rom) && rpkg == null) {
                showError("Found a .ROM file, but this firmware also needs the .RPKG file "
                        + "(the dumped Z: drive). Put both in the same folder.");
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
            showError("No .ROM or .VPL file in that folder.");
            return;
        }

        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.setMessage("Installing firmware… this can take a few minutes.");
        dialog.show();

        disposables.add(Emulator.subscribeInstallDevice(rpkgPath, romPath, installRpkg, false)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        dialog.dismiss();
                        Toast.makeText(getContext(), "Firmware installed", Toast.LENGTH_SHORT).show();
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
                return "Cannot read the firmware file.";
            case Emulator.INSTALL_DEVICE_ERROR_INSUFFICENT:
                return "Not enough storage space.";
            case Emulator.INSTALL_DEVICE_ERROR_RPKG_CORRUPT:
                return "The .RPKG file is corrupted or not a valid dump.";
            case Emulator.INSTALL_DEVICE_ERROR_DETERMINE_PRODUCT_FAIL:
                return "Could not tell the device model from this firmware.";
            case Emulator.INSTALL_DEVICE_ERROR_ALREADY_EXIST:
                return "This device is already installed.";
            case Emulator.INSTALL_DEVICE_ERROR_ROM_FAIL_TO_COPY:
                return "Failed to copy the .ROM file.";
            case Emulator.INSTALL_DEVICE_ERROR_VPL_FILE_INVALID:
                return "The .VPL file is not valid.";
            case Emulator.INSTALL_DEVICE_ERROR_ROFS_CORRUPTED:
            case Emulator.INSTALL_DEVICE_ERROR_ROM_CORRUPTED:
            case Emulator.INSTALL_DEVICE_ERROR_FPSX_CORRUPTED:
                return "The firmware file is corrupted.";
            default:
                return "Failed to install the firmware (code " + code + ").";
        }
    }

    // -------------------------------------------------------------------- games

    /** Creates Games/N-Gage in shared storage (needs direct storage access) and selects it. */
    private void useDefaultGamesFolder(boolean userAsked) {
        if (!FileUtils.hasDirectStorageAccess()) {
            if (userAsked) {
                showError("The default folder needs access to all files (step 1). "
                        + "Without it, use \"Choose another folder\".");
            }
            return;
        }
        File dir = NGageLibrary.createDefaultGamesDir();
        if (dir == null) {
            if (userAsked) {
                showError("Cannot create the Games/N-Gage folder on this storage.");
            }
            return;
        }
        NGageLibrary.setGamesDir(requireContext(), dir.getAbsolutePath());
        if (userAsked) {
            Toast.makeText(getContext(), "Folder ready: " + dir.getAbsolutePath(), Toast.LENGTH_LONG).show();
        }
        refresh();
    }

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
                .setTitle("Something went wrong")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
