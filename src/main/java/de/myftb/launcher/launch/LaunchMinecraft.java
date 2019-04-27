/*
 * MyFTBLauncher
 * Copyright (C) 2019 MyFTB <https://myftb.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.myftb.launcher.launch;

import com.google.common.base.Joiner;
import com.mojang.authlib.UserAuthentication;

import de.myftb.launcher.Constants;
import de.myftb.launcher.Launcher;
import de.myftb.launcher.models.launcher.Platform;
import de.myftb.launcher.models.minecraft.Arguments;
import de.myftb.launcher.models.minecraft.AssetIndex;
import de.myftb.launcher.models.minecraft.Library;
import de.myftb.launcher.models.minecraft.MinecraftVersionManifest;
import de.myftb.launcher.models.modpacks.FileTask;
import de.myftb.launcher.models.modpacks.ModpackManifest;
import de.myftb.launcher.models.modpacks.ModpackManifestList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LaunchMinecraft {
    private static final Logger log = LoggerFactory.getLogger(LaunchMinecraft.class);
    private static final ExecutorService downloadThreadPool = Executors
            .newFixedThreadPool(java.lang.Runtime.getRuntime().availableProcessors(), runnable -> {
                Thread thread = new Thread(runnable);
                thread.setDaemon(true);
                return thread;
            });

    private static List<Library> getAdditionalLibraries(ModpackManifest modpackManifest, MinecraftVersionManifest minecraftManifest) {
        return Collections.emptyList();
    }

    public static boolean install(ModpackManifest modpackManifest, List<String> selectedFeatures, InstallationStatusListener statusListener)
            throws IOException {
        File instanceDir = modpackManifest.getInstanceDir();
        File manifestFile = new File(instanceDir, "manifest.json");
        ModpackManifest oldManifest = null;
        if (manifestFile.isFile()) {
            oldManifest = LaunchHelper.mapper.readValue(manifestFile, ModpackManifest.class);
        }
        LaunchHelper.mapper.writeValue(manifestFile, modpackManifest);

        List<DownloadCallable> tasks = new ArrayList<>();

        MinecraftVersionManifest minecraftManifest = ManifestHelper.getManifest(modpackManifest.getGameVersion());
        List<Library> modpackLibs = modpackManifest.getVersionManifest().getLibraries().stream()
                .filter(library -> minecraftManifest.getLibraries().stream().noneMatch(lib -> lib.getName().equals(library.getName())))
                .collect(Collectors.toList());

        // Client Jar
        MinecraftVersionManifest.Download clientDownload = minecraftManifest.getDownloads().get("client");
        tasks.add(new DownloadCallable(new DownloadCallable.Downloadable(
                clientDownload.getUrl(),
                clientDownload.getSha1(),
                new File(Launcher.getInstance().getSaveSubDirectory("versions"), minecraftManifest.getId() + ".jar"))));

        // Vanilla Minecraft Libraries (Durch Vanilla Manifest)
        tasks.addAll(minecraftManifest.getLibraries().stream()
                .flatMap(libary -> libary.getDownloadables().stream())
                .map(DownloadCallable::new)
                .collect(Collectors.toList()));

        // Zusätzliche Libraries (Launcherfeatures oä.)
        tasks.addAll(LaunchMinecraft.getAdditionalLibraries(modpackManifest, minecraftManifest).stream()
                .flatMap(libary -> libary.getDownloadables().stream())
                .map(DownloadCallable::new)
                .collect(Collectors.toList()));

        // Asset Index
        String assetIndexStr = LaunchHelper.download(minecraftManifest.getAssetIndex().getUrl(), minecraftManifest.getAssetIndex().getSha1());
        AssetIndex assetIndex = LaunchHelper.mapper.readValue(assetIndexStr, AssetIndex.class);
        Files.write(new File(Launcher.getInstance().getSaveSubDirectory("assets/indexes"),
                minecraftManifest.getAssetIndex().getId() + ".json").toPath(), assetIndexStr.getBytes(StandardCharsets.UTF_8));

        // Assets
        tasks.addAll(assetIndex.getObjects().entrySet().stream()
                .map(entry -> new DownloadCallable(new DownloadCallable.Downloadable(entry.getValue().getDownloadUrl(),
                        entry.getValue().getHash(),
                        new File(Launcher.getInstance().getSaveSubDirectory(assetIndex.isVirtual()
                                ? "assets/virtual/" + minecraftManifest.getAssetIndex().getId()
                                : "assets/objects"),
                                assetIndex.isVirtual() ? entry.getKey() : entry.getValue().getSubPath()))))
                .collect(Collectors.toList()));

        // Libraries welche durch das Modpack benötigt werden (Beispielsweise Forge)
        tasks.addAll(modpackLibs.stream()
                .map(library -> new MavenDownloadCallable(library.getName(),
                        new File(Launcher.getInstance().getSaveSubDirectory("libraries"), library.getPath(null))))
                .collect(Collectors.toList()));

        // Modpack Installation Tasks
        if (modpackManifest.getTasks() != null) {
            List<FileTask> currentTasks = modpackManifest.getTasks().stream()
                    .filter(task -> task.getCondition() == null || task.getCondition().matches(selectedFeatures))
                    .collect(Collectors.toList());

            tasks.addAll(currentTasks.stream()
                    .map(task -> new DownloadCallable(new DownloadCallable.Downloadable(String.format(Constants.launcherObjects,
                            task.getLocation()), task.getHash(), new File(instanceDir, task.getTo())), task.isUserFile()))
                    .collect(Collectors.toList()));

            if (oldManifest != null) {
                // Lösche alte Dateien welche nicht mehr im aktuellen Manifest vorhanden sind
                oldManifest.getTasks().stream()
                        .filter(task -> currentTasks.stream().noneMatch(task1 -> task1.getTo().equals(task.getTo())))
                        .map(task -> new File(instanceDir, task.getTo()))
                        .forEach(File::delete);
            }
        }

        ExecutorCompletionService<File> completionService = new ExecutorCompletionService<>(LaunchMinecraft.downloadThreadPool);
        tasks.forEach(completionService::submit);

        int failed = 0;
        for (int i = 0; i < tasks.size(); i++) {
            try {
                completionService.take().get();
            } catch (InterruptedException | ExecutionException e) {
                LaunchMinecraft.log.warn("Fehler beim Herunterladen von Datei", e);
                failed++;
            }
            statusListener.progressChange(tasks.size(), i + 1, failed);
        }

        LaunchMinecraft.log.info("Modpack " + modpackManifest.getTitle() + " installiert");
        boolean success = failed == 0;
        Files.write(new File(instanceDir, ".success").toPath(), new byte[success ? 1 : 0]);

        return success;
    }

    public static void launch(ModpackManifest modpackManifest, UserAuthentication userAuthentication) throws IOException, InterruptedException {
        MinecraftVersionManifest minecraftManifest = ManifestHelper.getManifest(modpackManifest.getGameVersion());
        File instanceDir = modpackManifest.getInstanceDir();
        File runtimeDir = new File(Launcher.getInstance().getExecutableDirectory(), "runtime");

        if (!Launcher.getInstance().getRemotePacks().getPackByName(modpackManifest.getName())
                .map(ModpackManifestList.ModpackManifestReference::getVersion).orElse(modpackManifest.getVersion())
                .equals(modpackManifest.getVersion())) {

        }

        AssetIndex assetIndex = LaunchHelper.mapper.readValue(new File(Launcher.getInstance().getSaveSubDirectory("assets/indexes"),
                minecraftManifest.getAssetIndex().getId() + ".json"), AssetIndex.class);

        LaunchMinecraft.log.trace("Extrahiere Natives");
        File nativesDir = new File(Launcher.getInstance().getSaveSubDirectory("temp"), String.valueOf(System.currentTimeMillis()));
        nativesDir.mkdirs();

        minecraftManifest.getLibraries().stream()
                .filter(library -> library.getNatives() != null)
                .forEach(library -> {
                    if (library.getNatives().containsKey(Platform.getPlatform().name().toLowerCase())) {
                        String classifier = library.getNatives().get(Platform.getPlatform().name().toLowerCase())
                                .replace("${arch}", System.getProperty("os.arch").contains("64") ? "64" : "32");
                        if (library.getDownloads().getClassifiers() == null || !library.getDownloads().getClassifiers().containsKey(classifier)) {
                            return;
                        }
                        File nativeFile = new File(Launcher.getInstance().getSaveSubDirectory("libraries"), library.getPath(classifier));

                        try {
                            JarFile jarFile = new JarFile(nativeFile);
                            Enumeration<JarEntry> entries = jarFile.entries();
                            while (entries.hasMoreElements()) {
                                JarEntry entry = entries.nextElement();
                                if (entry.getName().endsWith("/") || !library.isExtractionAllowed(entry.getName())) {
                                    continue;
                                }

                                File targetFile = new File(nativesDir, entry.getName());
                                targetFile.getParentFile().mkdirs();
                                try (FileOutputStream fileOutputStream = new FileOutputStream(targetFile)) {
                                    try (InputStream inputStream = jarFile.getInputStream(entry)) {
                                        byte[] buffer = new byte[4096];
                                        int count;
                                        while ((count = inputStream.read(buffer)) > 0) {
                                            fileOutputStream.write(buffer, 0, count);
                                        }
                                    }
                                }
                            }
                        } catch (IOException e) {
                            LaunchMinecraft.log.error("Fehler beim Entpacken von Natives", e);
                        }
                    }
                });

        List<String> gameArguments = Arguments.getFromArguments(modpackManifest.getVersionManifest().getArguments().getGameArguments());

        gameArguments.add("--width");
        gameArguments.add(String.valueOf(Launcher.getInstance().getConfig().getGameWidth()));
        gameArguments.add("--height");
        gameArguments.add(String.valueOf(Launcher.getInstance().getConfig().getGameHeight()));

        List<String> jvmArguments = modpackManifest.getVersionManifest().getArguments().getJvmArguments() != null
                && !modpackManifest.getVersionManifest().getArguments().getJvmArguments().isEmpty()
                ? Arguments.getFromArguments(modpackManifest.getVersionManifest().getArguments().getJvmArguments())
                : new ArrayList<>(Arrays.asList("-Djava.library.path=${natives_directory}",
                "-XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump",
                "-cp", "${classpath}"));

        jvmArguments.add("-Xms${min_memory}M");
        jvmArguments.add("-Xmx${max_memory}M");

        if (modpackManifest.getLaunch().containsKey("flags")) {
            jvmArguments.addAll(modpackManifest.getLaunch().get("flags"));
        }

        jvmArguments.addAll(Arrays.asList(Launcher.getInstance().getConfig().getJvmArgs().split(" ")));

        List<Library> modpackLibs = modpackManifest.getVersionManifest().getLibraries().stream()
                .filter(library -> minecraftManifest.getLibraries().stream().noneMatch(lib -> lib.getName().equals(library.getName())))
                .collect(Collectors.toList());

        File librariesDir = Launcher.getInstance().getSaveSubDirectory("libraries");
        List<String> classpath = minecraftManifest.getLibraries().stream()
                .map(library -> new File(librariesDir, library.getPath(null)))
                .map(File::getAbsolutePath)
                .collect(Collectors.toList());
        classpath.addAll(modpackLibs.stream()
                .map(library -> new File(Launcher.getInstance().getSaveSubDirectory("libraries"),
                        library.getPath(null)).getAbsolutePath())
                .collect(Collectors.toList()));
        classpath.add(new File(Launcher.getInstance().getSaveSubDirectory("versions"),
                minecraftManifest.getId() + ".jar").getAbsolutePath());

        Map<String, String> tokens = new HashMap<>();
        tokens.put("auth_player_name", userAuthentication.getSelectedProfile().getName());
        tokens.put("auth_uuid", userAuthentication.getSelectedProfile().getId().toString());
        tokens.put("auth_access_token", userAuthentication.getAuthenticatedToken());
        tokens.put("auth_session", userAuthentication.getAuthenticatedToken());
        tokens.put("user_type", userAuthentication.getUserType().getName());
        tokens.put("user_properties", userAuthentication.getSelectedProfile().getProperties().toString());

        tokens.put("version_name", minecraftManifest.getId());
        tokens.put("game_directory", instanceDir.getAbsolutePath());
        tokens.put("assets_root", (assetIndex.isVirtual()
                ? Launcher.getInstance().getSaveSubDirectory("assets/virtual/" + minecraftManifest.getAssetIndex().getId())
                : Launcher.getInstance().getSaveSubDirectory("assets")).getAbsolutePath());
        tokens.put("game_assets", tokens.get("assets_root"));
        tokens.put("assets_index_name", minecraftManifest.getAssetIndex().getId());
        tokens.put("version_type", minecraftManifest.getType());

        tokens.put("natives_directory", nativesDir.getAbsolutePath());
        tokens.put("launcher_name", "MyFTBLauncher");
        tokens.put("launcher_version", Launcher.getInstance().getVersion());
        tokens.put("classpath", String.join(";", classpath));

        tokens.put("min_memory", String.valueOf(Launcher.getInstance().getConfig().getMinMemory()));
        tokens.put("max_memory", String.valueOf(Launcher.getInstance().getConfig().getMaxMemory()));

        List<String> arguments = new LinkedList<>();
        arguments.add(new File(runtimeDir, "bin/java.exe").getAbsolutePath());
        arguments.addAll(jvmArguments);
        arguments.add(modpackManifest.getVersionManifest().getMainClass());
        arguments.addAll(gameArguments);

        LaunchMinecraft.log.info("Startargumente: " + Joiner.on(' ').join(arguments));
        LaunchHelper.replaceTokens(arguments, tokens);

        ProcessBuilder builder = new ProcessBuilder(arguments);
        builder.directory(instanceDir);
        builder.inheritIO();

        try {
            LaunchMinecraft.log.info("Alle Dateien aktuell, starte Minecraft");
            Launcher.getInstance().getDiscordIntegration().setRunningModpack(modpackManifest);
            Process minecraftProcess = builder.start();
            minecraftProcess.waitFor();
        } finally {
            Launcher.getInstance().getDiscordIntegration().setRunningModpack(null);

            LaunchMinecraft.log.trace("Lösche entpackte Natives");
            Files.walk(nativesDir.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @FunctionalInterface
    public interface InstallationStatusListener {
        void progressChange(int total, int finished, int failed);
    }

}
