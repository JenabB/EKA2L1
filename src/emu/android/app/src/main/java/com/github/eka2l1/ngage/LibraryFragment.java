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
    private LinearLayout continueCard;
    private GameCoverView continueCover;
    private TextView continueTitle;
    private NGageGame continueGame;
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
        root.setPadding(Ui.dp(ctx, 16), pad, Ui.dp(ctx, 16), 0);

        TextView brand = Ui.text(ctx, Ui.brand(), 24, true);
        brand.setLetterSpacing(0.12f);
        brand.setPadding(Ui.dp(ctx, 4), Ui.dp(ctx, 4), 0, 0);
        root.addView(brand);
        headerText = Ui.muted(ctx, "", 13);
        headerText.setPadding(Ui.dp(ctx, 4), Ui.dp(ctx, 2), 0, Ui.dp(ctx, 14));
        root.addView(headerText);

        // "Continue playing": the last game played, one tap away.
        continueCard = Ui.row(ctx);
        continueCard.setBackground(Ui.rounded(ctx, Ui.SURFACE, Ui.BORDER, 18));
        int cpad = Ui.dp(ctx, 12);
        continueCard.setPadding(cpad, cpad, cpad, cpad);
        continueCover = new GameCoverView(ctx);
        continueCard.addView(continueCover, new LinearLayout.LayoutParams(Ui.dp(ctx, 56), LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout continueText = Ui.column(ctx);
        continueText.setPadding(Ui.dp(ctx, 14), 0, Ui.dp(ctx, 8), 0);
        TextView continueLabel = Ui.text(ctx, "CONTINUE PLAYING", 11, true);
        continueLabel.setTextColor(Ui.ORANGE);
        continueLabel.setLetterSpacing(0.12f);
        continueText.addView(continueLabel);
        continueTitle = Ui.text(ctx, "", 17, true);
        continueTitle.setMaxLines(2);
        continueTitle.setEllipsize(TextUtils.TruncateAt.END);
        continueText.addView(continueTitle);
        continueCard.addView(continueText, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView playButton = Ui.text(ctx, "▶", 18, true);
        playButton.setGravity(Gravity.CENTER);
        playButton.setBackground(Ui.circle(Ui.ORANGE));
        int pb = Ui.dp(ctx, 46);
        continueCard.addView(playButton, new LinearLayout.LayoutParams(pb, pb));
        continueCard.setOnClickListener(v -> {
            if (continueGame != null) {
                onGameClicked(continueGame);
            }
        });
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.bottomMargin = Ui.dp(ctx, 18);
        continueCard.setVisibility(View.GONE);
        root.addView(continueCard, clp);

        TextView allLabel = Ui.text(ctx, "ALL GAMES", 11, true);
        allLabel.setTextColor(Ui.MUTED);
        allLabel.setLetterSpacing(0.12f);
        allLabel.setPadding(Ui.dp(ctx, 4), 0, 0, Ui.dp(ctx, 10));
        root.addView(allLabel);

        grid = new GridView(ctx);
        grid.setNumColumns(GridView.AUTO_FIT);
        grid.setColumnWidth(Ui.dp(ctx, 150));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setVerticalSpacing(Ui.dp(ctx, 18));
        grid.setHorizontalSpacing(Ui.dp(ctx, 14));
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
        Button chooseButton = Ui.secondaryButton(ctx, "Choose another games folder");
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

        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.setMessage("Loading library…");
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
                        sortGames();
                        adapter.notifyDataSetChanged();
                        updateHeader();
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

    /** Recently played first, then by name. */
    private void sortGames() {
        Context ctx = requireContext();
        for (NGageGame g : games) {
            g.lastPlayed = NGageLibrary.getLastPlayed(ctx, g.path);
        }
        java.util.Collections.sort(games, (a, b) -> {
            if (a.lastPlayed != b.lastPlayed) {
                return Long.compare(b.lastPlayed, a.lastPlayed);
            }
            return a.displayTitle.compareToIgnoreCase(b.displayTitle);
        });
    }

    private void updateHeader() {
        int installed = 0;
        for (NGageGame g : games) {
            if (g.isInstalled()) {
                installed++;
            }
        }
        headerText.setText(games.size() + (games.size() == 1 ? " game" : " games")
                + " · " + installed + " installed");
        continueGame = (!games.isEmpty() && games.get(0).lastPlayed > 0 && games.get(0).isInstalled())
                ? games.get(0) : null;
        if (continueGame != null) {
            continueCover.bind(continueGame.displayTitle, continueGame.icon, true);
            continueTitle.setText(continueGame.displayTitle);
            continueCard.setVisibility(View.VISIBLE);
        } else {
            continueCard.setVisibility(View.GONE);
        }
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
            emptyText.setText("No games folder yet. Choose the folder that holds your N-Gage games.");
        } else {
            emptyText.setText("No games in\n" + SetupWizardFragment.displayPath(dir)
                    + "\n\nCopy games there: one folder per game (containing System/Apps) "
                    + "or a .zip file. Then pick Refresh in the menu.");
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
                .setTitle("Emulator failed to start")
                .setMessage("Firmware is installed, but the emulator could not start. "
                        + "Try another device or reinstall the firmware.")
                .setPositiveButton("Manage devices", (d, w) -> openFragment(new DeviceListFragment()))
                .setNegativeButton("Cancel", null)
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
        Toast.makeText(getContext(), "Switching to device " + names[found], Toast.LENGTH_SHORT).show();
        Emulator.setCurrentDevice(found, false);
        AppUtils.restart(requireContext());
        return false;
    }

    private static String codeAt(String[] codes, int index) {
        return (codes != null && index < codes.length) ? codes[index] : "";
    }

    // ------------------------------------------------------------------ launch

    private void onGameClicked(NGageGame game) {
        NGageLibrary.setLastPlayed(requireContext(), game.path, System.currentTimeMillis());
        if (game.isInstalled()) {
            launch(game.uid, game.displayTitle);
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
        dialog.setMessage("Installing " + game.displayTitle + "…\nOnly needed once; next time it starts straight away.");
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
                        showError("Failed to install the game: " + e.getMessage());
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
            launch(match.getUid(), game.displayTitle);
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
            showError("The game was installed but does not show in the emulator app list. "
                    + "Try Full EKA2L1 mode from the menu.");
            return;
        }
        String[] labels = new String[installedApps.size()];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = installedApps.get(i).getTitle();
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Which app is \"" + game.displayTitle + "\"?")
                .setItems(labels, (d, which) -> {
                    AppItem item = installedApps.get(which);
                    bind(game, item);
                    if (launchAfter) {
                        launch(item.getUid(), game.displayTitle);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    static String describeGameError(int code) {
        switch (code) {
            case Emulator.INSTALL_NG_GAME_ERROR_NO_GAME_DATA_FOLDER:
                return "No System/Apps folder found in this game.";
            case Emulator.INSTALL_NG_GAME_ERROR_MORE_THAN_ONE_DATA_FOLDER:
                return "This folder holds more than one game. Split it into one folder per game.";
            case Emulator.INSTALL_NG_GAME_ERROR_NO_GAME_REGISTERATION_INFO:
                return "Game info file (.aif/.app) not found.";
            case Emulator.INSTALL_NG_GAME_ERROR_REGISTERATION_CORRUPTED:
                return "Game info file is corrupted.";
            default:
                return "Failed to install the game (code " + code + ").";
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
        menu.setHeaderTitle(contextGame.displayTitle);
        if (contextGame.isInstalled()) {
            menu.add(Menu.NONE, CTX_SETTINGS, 0, "Game settings (controls, screen)");
        }
        menu.add(Menu.NONE, CTX_REINSTALL, 1, "Reinstall");
        menu.add(Menu.NONE, CTX_LINK, 2, "Link to an installed app…");
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
                args.putString(KEY_APP_NAME, game.displayTitle);
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
        menu.add(Menu.NONE, MENU_RESCAN, 0, "Refresh");
        menu.add(Menu.NONE, MENU_CHANGE_FOLDER, 1, "Change games folder");
        menu.add(Menu.NONE, MENU_DEVICES, 2, "Firmware / devices");
        menu.add(Menu.NONE, MENU_SETTINGS, 3, "Emulator settings");
        menu.add(Menu.NONE, MENU_SETUP, 4, "Run setup again");
        menu.add(Menu.NONE, MENU_ADVANCED, 5, "Full EKA2L1 mode");
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
                .setTitle("Something went wrong")
                .setMessage(message)
                .setPositiveButton("OK", null)
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
                holder = new Holder();
                holder.cover = new GameCoverView(ctx);
                tile.addView(holder.cover, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                holder.title = Ui.text(ctx, "", 14, true);
                holder.title.setMaxLines(2);
                holder.title.setEllipsize(TextUtils.TruncateAt.END);
                holder.title.setPadding(Ui.dp(ctx, 2), Ui.dp(ctx, 8), Ui.dp(ctx, 2), 0);
                tile.addView(holder.title);

                holder.status = Ui.muted(ctx, "", 12);
                holder.status.setPadding(Ui.dp(ctx, 2), Ui.dp(ctx, 2), Ui.dp(ctx, 2), 0);
                tile.addView(holder.status);

                tile.setTag(holder);
                convertView = tile;
            } else {
                holder = (Holder) convertView.getTag();
            }

            NGageGame game = games.get(position);
            holder.cover.bind(game.displayTitle, game.icon, game.isInstalled());
            holder.title.setText(game.displayTitle);
            String state = game.isInstalled() ? "Ready to play" : "Tap to install";
            holder.status.setText(game.region != null ? game.region + "  ·  " + state : state);
            holder.status.setTextColor(game.isInstalled() ? Ui.SILVER : Ui.MUTED);
            return convertView;
        }
    }

    private static final class Holder {
        GameCoverView cover;
        TextView title;
        TextView status;
    }
}
