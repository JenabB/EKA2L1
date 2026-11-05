/*
 * Copyright (c) 2026 NGStation contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.github.eka2l1.ngage;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.github.eka2l1.R;
import com.github.eka2l1.applist.AppItem;
import com.github.eka2l1.applist.AppsListFragment;
import com.github.eka2l1.applist.DeviceListFragment;
import com.github.eka2l1.config.ConfigFragment;
import com.github.eka2l1.emu.Emulator;
import com.github.eka2l1.emu.EmulatorActivity;
import com.github.eka2l1.settings.SettingsFragment;
import com.github.eka2l1.util.AppUtils;
import com.github.eka2l1.util.FileUtils;
import com.github.eka2l1.util.PickDirResultContract;
import com.github.eka2l1.util.SAFDirResultContract;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

import static com.github.eka2l1.emu.Constants.ACTION_EDIT;
import static com.github.eka2l1.emu.Constants.KEY_ACTION;
import static com.github.eka2l1.emu.Constants.KEY_APP_NAME;
import static com.github.eka2l1.emu.Constants.KEY_APP_UID;

/**
 * The main screen: every game in the games folder as a tile. Tapping a tile installs the
 * game on first use and launches it; afterwards it launches straight away.
 */
public class LibraryFragment extends Fragment {
    private static final int MENU_RESCAN = 1;
    private static final int MENU_CHANGE_FOLDER = 2;
    private static final int MENU_DEVICES = 3;
    private static final int MENU_ADVANCED = 4;
    private static final int MENU_SETTINGS = 5;
    private static final int MENU_SETUP = 6;

    private static final int CTX_SETTINGS = 101;
    private static final int CTX_REINSTALL = 102;
    private static final int CTX_LINK = 103;

    private final CompositeDisposable disposables = new CompositeDisposable();
    private final List<NGageGame> games = new ArrayList<>();
    private final List<AppItem> installedApps = new ArrayList<>();
    private GameAdapter adapter;
    private TextView headerText;
    private LinearLayout emptyView;
    private TextView emptyText;
    private GridView grid;
    private NGageGame contextGame;

    private final ActivityResultLauncher<Void> gamesPathPicker =
            registerForActivityResult(new PickDirResultContract(), this::onGamesFolder);
    private final ActivityResultLauncher<Void> gamesTreePicker =
            registerForActivityResult(new SAFDirResultContract(true), this::onGamesTree);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context ctx = requireContext();
        FrameLayout frame = new FrameLayout(ctx);
        Ui.setBackground(frame);

        LinearLayout root = Ui.column(ctx);
        int pad = Ui.dp(ctx, 12);
        root.setPadding(pad, pad, pad, 0);

        TextView brand = Ui.text(ctx, Ui.brand(), 24, true);
        brand.setLetterSpacing(0.12f);
        brand.setPadding(Ui.dp(ctx, 4), Ui.dp(ctx, 4), 0, 0);
        root.addView(brand);
        headerText = Ui.muted(ctx, "", 12);
        headerText.setPadding(Ui.dp(ctx, 4), Ui.dp(ctx, 2), 0, Ui.dp(ctx, 12));
        root.addView(headerText);

        grid = new GridView(ctx);
        grid.setNumColumns(GridView.AUTO_FIT);
        grid.setColumnWidth(Ui.dp(ctx, 112));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setVerticalSpacing(Ui.dp(ctx, 10));
        grid.setHorizontalSpacing(Ui.dp(ctx, 10));
        grid.setClipToPadding(false);
        grid.setPadding(0, 0, 0, Ui.dp(ctx, 24));
        adapter = new GameAdapter();
        grid.setSelector(new android.graphics.drawable.ColorDrawable(0));
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) ->
                onGameClicked(games.get(position)));
        registerForContextMenu(grid);
        root.addView(grid, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        frame.addView(root);

        emptyView = Ui.column(ctx);
        emptyView.setGravity(Gravity.CENTER);
        int epad = Ui.dp(ctx, 32);
        emptyView.setPadding(epad, epad, epad, epad);
        emptyText = Ui.muted(ctx, "", 15);
        emptyText.setGravity(Gravity.CENTER);
        emptyView.addView(emptyText);
        Button chooseButton = Ui.secondaryButton(ctx, "Pilih folder game lain");
        chooseButton.setOnClickListener(v -> pickGamesFolder());
        emptyView.addView(chooseButton);
        emptyView.setVisibility(View.GONE);
        frame.addView(emptyView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        return frame;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
        load();
    }

    @Override
    public void onResume() {
        super.onResume();
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
        Ui.applyChrome(activity, actionBar, "Library");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }

    // -------------------------------------------------------------------- load

    private static final class LoadResult {
        List<AppItem> apps;
        List<NGageGame> games;
    }

    private void load() {
        Context app = requireContext().getApplicationContext();
        if (NGageLibrary.getGamesDir(app) == null && FileUtils.hasDirectStorageAccess()) {
            java.io.File def = NGageLibrary.createDefaultGamesDir();
            if (def != null) {
                NGageLibrary.setGamesDir(app, def.getAbsolutePath());
            }
        }
        String dir = NGageLibrary.getGamesDir(app);
        headerText.setText(dir == null ? "" : "Folder: " + SetupWizardFragment.displayPath(dir));

        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.setMessage("Memuat library…");
        dialog.show();

        Single<LoadResult> single = Single.fromCallable(() -> {
            LoadResult result = new LoadResult();
            // Starts the core on first call; throws when no device is installed.
            result.apps = Emulator.getAppsList().blockingGet();
            result.games = NGageLibrary.scan(app, dir);
            resolveInstalled(app, result.games, result.apps);
            return result;
        });

        disposables.add(single.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableSingleObserver<LoadResult>() {
                    @Override
                    public void onSuccess(@NonNull LoadResult result) {
                        dialog.dismiss();
                        installedApps.clear();
                        installedApps.addAll(result.apps);
                        games.clear();
                        games.addAll(result.games);
                        adapter.notifyDataSetChanged();
                        updateEmptyState(dir);
                        if (!ensureNGageDevice()) {
                            return;
                        }
                        handlePendingLaunch();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        dialog.dismiss();
                        onCoreUnavailable();
                    }
                }));
    }

    /** Fills in uid and icon for every game that is already installed. */
    private static void resolveInstalled(Context context, List<NGageGame> games, List<AppItem> apps) {
        Set<Long> claimed = new HashSet<>();
        for (NGageGame game : games) {
            long mapped = NGageLibrary.getMappedUid(context, game.path);
            AppItem item = findByUid(apps, mapped);
            if (item == null && mapped != -1) {
                // The mapped app is gone (uninstalled or device switched).
                NGageLibrary.clearMappedUid(context, game.path);
            }
            if (item == null) {
                AppItem byName = NGageLibrary.matchByName(game, apps, 3);
                if (byName != null && !claimed.contains(byName.getUid())) {
                    item = byName;
                    NGageLibrary.setMappedUid(context, game.path, item.getUid());
                }
            }
            if (item != null) {
                claimed.add(item.getUid());
                game.uid = item.getUid();
                game.icon = item.getIcon();
            }
        }
    }

    private static AppItem findByUid(List<AppItem> apps, long uid) {
        if (uid == -1) {
            return null;
        }
        for (AppItem item : apps) {
            if (item.getUid() == uid) {
                return item;
            }
        }
        return null;
    }

    private void updateEmptyState(String dir) {
        boolean empty = games.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        grid.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (dir == null) {
            emptyText.setText("Belum ada folder game. Pilih folder yang berisi game N-Gage kamu.");
        } else {
            emptyText.setText("Belum ada game di\n" + SetupWizardFragment.displayPath(dir)
                    + "\n\nSalin game ke folder itu: tiap game satu folder (berisi System/Apps) "
                    + "atau file .zip. Lalu pilih Muat ulang di menu.");
        }
    }

    /** The core failed to start: normally no firmware is installed yet. */
    private void onCoreUnavailable() {
        String[] devices = Emulator.getDevices();
        if (devices == null || devices.length == 0) {
            NGageLibrary.setSetupDone(requireContext(), false);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.container, new SetupWizardFragment())
                    .commitAllowingStateLoss();
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Emulator gagal dimulai")
                .setMessage("Firmware terpasang, tapi emulator tidak bisa dijalankan. "
                        + "Coba pilih perangkat lain atau pasang ulang firmware.")
                .setPositiveButton("Kelola perangkat", (d, w) -> openFragment(new DeviceListFragment()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Makes sure the current device is the N-Gage when one is installed. Returns false
     * when the process is about to restart to switch devices.
     */
    private boolean ensureNGageDevice() {
        String[] names = Emulator.getDevices();
        String[] codes = Emulator.getDeviceFirmwareCodes();
        if (names == null || names.length < 2) {
            return true;
        }
        int current = Emulator.getCurrentDevice();
        if (current >= 0 && current < names.length
                && NGageLibrary.looksLikeNGage(names[current], codeAt(codes, current))) {
            return true;
        }
        int found = -1;
        for (int i = 0; i < names.length; i++) {
            if (NGageLibrary.looksLikeNGage(names[i], codeAt(codes, i))) {
                if (found != -1) {
                    return true; // Several candidates: leave the choice to the user.
                }
                found = i;
            }
        }
        if (found == -1) {
            return true;
        }
        Toast.makeText(getContext(), "Beralih ke perangkat " + names[found], Toast.LENGTH_SHORT).show();
        Emulator.setCurrentDevice(found, false);
        AppUtils.restart(requireContext());
        return false;
    }

    private static String codeAt(String[] codes, int index) {
        return (codes != null && index < codes.length) ? codes[index] : "";
    }

    // ------------------------------------------------------------------ launch

    private void onGameClicked(NGageGame game) {
        if (game.isInstalled()) {
            launch(game.uid, game.title);
        } else {
            install(game);
        }
    }

    private void launch(long uid, String title) {
        Intent intent = new Intent(requireContext(), EmulatorActivity.class);
        intent.putExtra(KEY_APP_UID, uid);
        intent.putExtra(KEY_APP_NAME, title);
        startActivity(intent);
    }

    private void install(NGageGame game) {
        Set<Long> before = new HashSet<>();
        for (AppItem item : installedApps) {
            before.add(item.getUid());
        }

        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.setMessage("Memasang " + game.title + "…\nHanya sekali, berikutnya langsung main.");
        dialog.show();

        Context app = requireContext().getApplicationContext();
        disposables.add(Single.fromCallable(() -> Emulator.installNGageGame(game.path))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableSingleObserver<Integer>() {
                    @Override
                    public void onSuccess(@NonNull Integer code) {
                        dialog.dismiss();
                        if (code != Emulator.INSTALL_NG_GAME_ERROR_NONE) {
                            showError(describeGameError(code));
                            return;
                        }
                        // The app list is only rebuilt on a fresh process, so restart and
                        // pick the launch up from there.
                        NGageLibrary.setPendingLaunch(app, game.path, before);
                        AppUtils.restart(app);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        dialog.dismiss();
                        showError("Gagal memasang game: " + e.getMessage());
                    }
                }));
    }

    private void handlePendingLaunch() {
        Context ctx = requireContext();
        String pendingPath = NGageLibrary.getPendingPath(ctx);
        if (pendingPath == null) {
            return;
        }
        Set<Long> before = NGageLibrary.getPendingBefore(ctx);
        NGageLibrary.clearPendingLaunch(ctx);

        NGageGame game = null;
        for (NGageGame g : games) {
            if (g.path.equals(pendingPath)) {
                game = g;
                break;
            }
        }
        if (game == null) {
            return;
        }

        AppItem match = NGageLibrary.matchInstalledApp(game, installedApps, before);
        if (match != null) {
            bind(game, match);
            launch(match.getUid(), game.title);
        } else {
            askWhichApp(game, true);
        }
    }

    private void bind(NGageGame game, AppItem item) {
        NGageLibrary.setMappedUid(requireContext(), game.path, item.getUid());
        game.uid = item.getUid();
        game.icon = item.getIcon();
        adapter.notifyDataSetChanged();
    }

    /** Lets the user say which installed app is this game when matching was not sure. */
    private void askWhichApp(NGageGame game, boolean launchAfter) {
        if (installedApps.isEmpty()) {
            showError("Game terpasang, tapi tidak muncul di daftar aplikasi emulator. "
                    + "Coba buka Mode EKA2L1 lengkap dari menu.");
            return;
        }
        String[] labels = new String[installedApps.size()];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = installedApps.get(i).getTitle();
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Aplikasi mana yang \"" + game.title + "\"?")
                .setItems(labels, (d, which) -> {
                    AppItem item = installedApps.get(which);
                    bind(game, item);
                    if (launchAfter) {
                        launch(item.getUid(), game.title);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    static String describeGameError(int code) {
        switch (code) {
            case Emulator.INSTALL_NG_GAME_ERROR_NO_GAME_DATA_FOLDER:
                return "Folder System/Apps tidak ditemukan di game ini.";
            case Emulator.INSTALL_NG_GAME_ERROR_MORE_THAN_ONE_DATA_FOLDER:
                return "Ada lebih dari satu game di folder ini. Pisahkan jadi satu folder per game.";
            case Emulator.INSTALL_NG_GAME_ERROR_NO_GAME_REGISTERATION_INFO:
                return "File info game (.aif/.app) tidak ditemukan.";
            case Emulator.INSTALL_NG_GAME_ERROR_REGISTERATION_CORRUPTED:
                return "File info game rusak.";
            default:
                return "Gagal memasang game (kode " + code + ").";
        }
    }

    // ------------------------------------------------------------ context menu

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v,
                                    @Nullable ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (!(menuInfo instanceof AdapterView.AdapterContextMenuInfo)) {
            return;
        }
        int position = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;
        contextGame = games.get(position);
        menu.setHeaderTitle(contextGame.title);
        if (contextGame.isInstalled()) {
            menu.add(Menu.NONE, CTX_SETTINGS, 0, "Pengaturan game (kontrol, layar)");
        }
        menu.add(Menu.NONE, CTX_REINSTALL, 1, "Pasang ulang");
        menu.add(Menu.NONE, CTX_LINK, 2, "Hubungkan ke aplikasi terpasang…");
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        NGageGame game = contextGame;
        if (game == null) {
            return super.onContextItemSelected(item);
        }
        switch (item.getItemId()) {
            case CTX_SETTINGS: {
                ConfigFragment configFragment = new ConfigFragment();
                Bundle args = new Bundle();
                args.putLong(KEY_APP_UID, game.uid);
                args.putString(KEY_APP_NAME, game.title);
                args.putString(KEY_ACTION, ACTION_EDIT);
                configFragment.setArguments(args);
                openFragment(configFragment);
                return true;
            }
            case CTX_REINSTALL:
                NGageLibrary.clearMappedUid(requireContext(), game.path);
                game.uid = -1;
                game.icon = null;
                install(game);
                return true;
            case CTX_LINK:
                askWhichApp(game, false);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    // ------------------------------------------------------------ options menu

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        menu.add(Menu.NONE, MENU_RESCAN, 0, "Muat ulang");
        menu.add(Menu.NONE, MENU_CHANGE_FOLDER, 1, "Ganti folder game");
        menu.add(Menu.NONE, MENU_DEVICES, 2, "Firmware / perangkat");
        menu.add(Menu.NONE, MENU_SETTINGS, 3, "Pengaturan emulator");
        menu.add(Menu.NONE, MENU_SETUP, 4, "Ulangi setup");
        menu.add(Menu.NONE, MENU_ADVANCED, 5, "Mode EKA2L1 lengkap");
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESCAN:
                load();
                return true;
            case MENU_CHANGE_FOLDER:
                pickGamesFolder();
                return true;
            case MENU_DEVICES:
                openFragment(new DeviceListFragment());
                return true;
            case MENU_SETTINGS:
                openFragment(new SettingsFragment());
                return true;
            case MENU_SETUP:
                openFragment(new SetupWizardFragment());
                return true;
            case MENU_ADVANCED:
                openFragment(new AppsListFragment());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openFragment(Fragment fragment) {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.container, fragment)
                .addToBackStack(null)
                .commit();
    }

    // ------------------------------------------------------------ games folder

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
        load();
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
        load();
    }

    private void showError(String message) {
        if (!isAdded()) {
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Ada masalah")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    // ----------------------------------------------------------------- adapter

    private final class GameAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return games.size();
        }

        @Override
        public Object getItem(int position) {
            return games.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Context ctx = parent.getContext();
            Holder holder;
            if (convertView == null) {
                LinearLayout tile = Ui.column(ctx);
                tile.setGravity(Gravity.CENTER_HORIZONTAL);
                int pad = Ui.dp(ctx, 10);
                tile.setPadding(pad, pad, pad, pad);
                android.graphics.drawable.StateListDrawable bg = new android.graphics.drawable.StateListDrawable();
                bg.addState(new int[]{android.R.attr.state_pressed}, Ui.rounded(ctx, 0xFF3A2A20, Ui.ORANGE, 14));
                bg.addState(new int[]{}, Ui.rounded(ctx, Ui.SURFACE, Ui.BORDER, 14));
                tile.setBackground(bg);

                holder = new Holder();
                holder.icon = new ImageView(ctx);
                holder.icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                int size = Ui.dp(ctx, 64);
                tile.addView(holder.icon, new LinearLayout.LayoutParams(size, size));

                holder.title = Ui.text(ctx, "", 13, true);
                holder.title.setGravity(Gravity.CENTER);
                holder.title.setMaxLines(2);
                holder.title.setEllipsize(TextUtils.TruncateAt.END);
                holder.title.setPadding(0, Ui.dp(ctx, 6), 0, 0);
                tile.addView(holder.title);

                holder.status = Ui.text(ctx, "", 11, false);
                holder.status.setGravity(Gravity.CENTER);
                tile.addView(holder.status);

                tile.setTag(holder);
                convertView = tile;
            } else {
                holder = (Holder) convertView.getTag();
            }

            NGageGame game = games.get(position);
            holder.title.setText(game.title);
            if (game.icon != null) {
                holder.icon.setImageBitmap(game.icon);
            } else {
                holder.icon.setImageResource(R.mipmap.ic_ducky_foreground);
            }
            if (game.isInstalled()) {
                holder.status.setText("▶ Siap main");
                holder.status.setTextColor(Ui.SILVER);
            } else {
                holder.status.setText(game.archive ? "ZIP · ketuk untuk pasang" : "Ketuk untuk pasang");
                holder.status.setTextColor(Ui.ORANGE);
            }
            return convertView;
        }
    }

    private static final class Holder {
        ImageView icon;
        TextView title;
        TextView status;
    }
}
