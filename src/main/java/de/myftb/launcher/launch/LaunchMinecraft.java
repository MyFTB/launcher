/*
 * MyFTBLauncher
 * Copyright (C) 2022 MyFTB <https://myftb.de>
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

import de.myftb.launcher.Constants;
import de.myftb.launcher.Launcher;
import de.myftb.launcher.MavenHelper;
import de.myftb.launcher.models.launcher.Index;
import de.myftb.launcher.models.launcher.LauncherProfile;
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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LaunchMinecraft {
    private static final Logger log = LoggerFactory.getLogger(LaunchMinecraft.class);
    private static ExecutorService downloadThreadPool;
    public static boolean cancelDownload = false;

    private static final LogCollector logCollector = new LogCollector();
    private static boolean running = false;
    private static Process minecraftProcess;

    private static List<Library> getAdditionalLibraries(ModpackManifest modpackManifest, MinecraftVersionManifest minecraftManifest) {
        return Collections.emptyList();
    }

    private static List<Library> getAllLibraries(ModpackManifest modpackManifest, MinecraftVersionManifest minecraftManifest) {
        List<Library> modpackLibs = modpackManifest.getVersionManifest().getLibraries().stream()
                .filter(library -> minecraftManifest.getLibraries().stream().noneMatch(lib -> lib.getName().equals(library.getName())))
                .filter(Library::isAllowed)
                .collect(Collectors.toList());

        List<Library> joinedLibs = new ArrayList<>(modpackLibs);
        minecraftManifest.getLibraries().stream()
                .filter(Library::isAllowed)
                .forEach(library -> {
                    Optional<Library> sameLib = joinedLibs.stream()
                            .filter(lib -> lib.getArtifactGroup().equals(library.getArtifactGroup())
                                    && lib.getArtifactName().equals(library.getArtifactName())).findFirst();

                    if (sameLib.isPresent()) {
                        if (sameLib.get().getArtifactVersion().equals(library.getArtifactVersion())) {
                            if (library.getDownloads() != null) {
                                if (sameLib.get().getDownloads() == null) {
                                    sameLib.get().setDownloads(library.getDownloads());
                                } else {
                                    if (sameLib.get().getDownloads().getArtifact() == null) {
                                        sameLib.get().getDownloads().setArtifact(library.getDownloads().getArtifact());
                                    }

                                    if (library.getDownloads().getClassifiers() != null) {
                                        if (sameLib.get().getDownloads().getClassifiers() == null) {
                                            sameLib.get().getDownloads().setClassifiers(library.getDownloads().getClassifiers());
                                        } else {
                                            sameLib.get().getDownloads().getClassifiers().putAll(library.getDownloads().getClassifiers());
                                        }
                                    }
                                }
                            }

                            if (library.getNatives() != null) {
                                if (sameLib.get().getNatives() == null) {
                                    sameLib.get().setNatives(library.getNatives());
                                } else {
                                    sameLib.get().getNatives().putAll(library.getNatives());
                                }
                            }
                        }
                    } else {
                        joinedLibs.add(library);
                    }
                });

        joinedLibs.addAll(LaunchMinecraft.getAdditionalLibraries(modpackManifest, minecraftManifest));

        return joinedLibs;
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

        // Client Jar
        MinecraftVersionManifest.Download clientDownload = minecraftManifest.getDownloads().get("client");
        tasks.add(new DownloadCallable(new DownloadCallable.Downloadable(
                clientDownload.getUrl(),
                clientDownload.getSha1(),
                new File(Launcher.getInstance().getSaveSubDirectory("versions"), minecraftManifest.getId() + ".jar"))));

        // Alle Libraries: Minecraft, Modpack, Launcherfeatures
        List<Library> libraries = LaunchMinecraft.getAllLibraries(modpackManifest, minecraftManifest);
        tasks.addAll(libraries.stream()
                .flatMap(libary -> libary.getLibraryDownloads().stream())
                .collect(Collectors.toList()));

        int majorGameVersion = Integer.parseInt(modpackManifest.getGameVersion().split("[.]")[1]);
        Optional<Library> neededForgeInstaller = majorGameVersion > 12 ? libraries.stream()
                .filter(library -> library.getArtifactGroup().equals("net.minecraftforge") && library.getArtifactName().equals("forge"))
                .findFirst() : Optional.empty();

        if (neededForgeInstaller.isPresent()) {
            MavenHelper.MavenArtifact forgeArtifact = new MavenHelper.MavenArtifact(neededForgeInstaller.get().getName());
            LaunchMinecraft.log.info("Möglichen benötigten Forge-Installer für {} gefunden", forgeArtifact);
            tasks.add(ForgeInstallWrapper.of(forgeArtifact));
        }

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

        // Custom Runtime
        if (modpackManifest.getRuntime() != null) {
            String architecture = System.getProperty("sun.arch.data.model").equals("64") ? "-x64": "";
            String os = Platform.getPlatform().name().toLowerCase(Locale.ROOT);
            String runtimeIndexName = String.format("%s-%s%s", modpackManifest.getRuntime(), os, architecture);

            Index runtimeFileIndex = LaunchHelper.mapper.readValue(LaunchHelper.download(
                    String.format(Constants.runtimeIndex, runtimeIndexName)), Index.class);

            File runtimeDir = new File(new File(Launcher.getInstance().getExecutableDirectory(), "custom-runtimes"), modpackManifest.getRuntime());
            runtimeDir.mkdirs();

            tasks.addAll(runtimeFileIndex.getObjects().stream()
                    .map(object -> new DownloadCallable(new DownloadCallable.Downloadable(object.getUrl(),
                            LaunchHelper::getSha256, object.getHash(), new File(runtimeDir, object.getPath()))))
                    .collect(Collectors.toList()));
        }

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

        if (LaunchMinecraft.downloadThreadPool == null || LaunchMinecraft.downloadThreadPool.isShutdown()) {
            LaunchMinecraft.downloadThreadPool = LaunchHelper.getNewDaemonThreadPool();
        }
        LaunchMinecraft.cancelDownload = false;

        ExecutorCompletionService<File> completionService = new ExecutorCompletionService<>(LaunchMinecraft.downloadThreadPool);
        tasks.forEach(completionService::submit);

        int failed = 0;
        for (int i = 0; i < tasks.size(); i++) {
            if (LaunchMinecraft.cancelDownload) {
                LaunchMinecraft.downloadThreadPool.shutdownNow();
                failed = tasks.size() - i;
                break;
            } else {
                try {
                    completionService.take().get();
                } catch (InterruptedException | ExecutionException e) {
                    LaunchMinecraft.log.warn("Fehler beim Herunterladen von Datei", e);
                    failed++;
                }
                statusListener.progressChange(tasks.size(), i + 1, failed);
            }
        }

        LaunchMinecraft.log.info("Modpack {} installiert", modpackManifest.getTitle());
        boolean success = failed == 0;
        Files.write(new File(instanceDir, ".success").toPath(), new byte[]{(byte) (success ? 1 : 0)});

        return success;
    }

    public static void launch(ModpackManifest modpackManifest, LauncherProfile launcherProfile) throws IOException, InterruptedException {
        if (LaunchMinecraft.running) {
            throw new IllegalStateException("Es läuft bereits ein Modpack");
        }

        MinecraftVersionManifest minecraftManifest = ManifestHelper.getManifest(modpackManifest.getGameVersion());
        File instanceDir = modpackManifest.getInstanceDir();

        Optional<ModpackManifestList.ModpackManifestReference> remoteReference = Launcher.getInstance().getRemotePacks()
                .getPackByName(modpackManifest.getName());
        if (!remoteReference.map(ModpackManifestList.ModpackManifestReference::getVersion).orElse(modpackManifest.getVersion())
                .equals(modpackManifest.getVersion())) {
            throw new ModpackOutdatedException(remoteReference.get());
        }

        AssetIndex assetIndex = LaunchHelper.mapper.readValue(new File(Launcher.getInstance().getSaveSubDirectory("assets/indexes"),
                minecraftManifest.getAssetIndex().getId() + ".json"), AssetIndex.class);

        LaunchMinecraft.log.trace("Extrahiere Natives");
        File nativesDir = new File(Launcher.getInstance().getSaveSubDirectory("temp"), String.valueOf(System.currentTimeMillis()));
        nativesDir.mkdirs();

        List<Library> libraries = LaunchMinecraft.getAllLibraries(modpackManifest, minecraftManifest);

        libraries.stream()
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

        Arguments baseArgs = modpackManifest.getVersionManifest().getInheritsFrom() != null ? minecraftManifest.getArguments() : null;
        List<String> gameArguments = Arguments.getFromArguments(baseArgs,
                modpackManifest.getVersionManifest().getArguments(), Arguments::getGameArguments);

        gameArguments.add("--width");
        gameArguments.add(String.valueOf(Launcher.getInstance().getConfig().getGameWidth()));
        gameArguments.add("--height");
        gameArguments.add(String.valueOf(Launcher.getInstance().getConfig().getGameHeight()));

        List<String> jvmArguments = modpackManifest.getVersionManifest().getArguments().getJvmArguments() != null
                && !modpackManifest.getVersionManifest().getArguments().getJvmArguments().isEmpty()
                ? Arguments.getFromArguments(baseArgs, modpackManifest.getVersionManifest().getArguments(), Arguments::getJvmArguments)
                : new ArrayList<>(Arrays.asList("-Djava.library.path=${natives_directory}",
                "-XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump",
                "-cp", "${classpath}"));

        jvmArguments.add("-Xms${min_memory}M");
        jvmArguments.add("-Xmx${max_memory}M");

        if (modpackManifest.getLaunch().containsKey("flags")) {
            jvmArguments.addAll(modpackManifest.getLaunch().get("flags"));
        }

        if (!Launcher.getInstance().getConfig().getJvmArgs().isEmpty()) {
            String[] customArgs = Launcher.getInstance().getConfig().getJvmArgs().split(" ");
            jvmArguments.addAll(Arrays.asList(customArgs));
        }

        File librariesDir = Launcher.getInstance().getSaveSubDirectory("libraries");
        List<String> classpath = libraries.stream()
                .map(library -> new File(librariesDir, library.getPath()))
                .map(File::getAbsolutePath)
                .collect(Collectors.toList());
        classpath.add(new File(Launcher.getInstance().getSaveSubDirectory("versions"),
                minecraftManifest.getId() + ".jar").getAbsolutePath());

        Map<String, String> tokens = new HashMap<>();
        String authToken = Launcher.getInstance().getConfig().getLauncherProfileStore().getLoginService(launcherProfile).getAuthToken(launcherProfile);
        tokens.put("auth_player_name", launcherProfile.getLastKnownUsername());
        tokens.put("auth_uuid", launcherProfile.getUuid().toString());
        tokens.put("auth_access_token", authToken);
        tokens.put("auth_session", authToken);
        // tokens.put("user_type", userAuthentication.getUserType().getName());
        tokens.put("user_properties", "{}");


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
        tokens.put("launcher_version", Launcher.getVersion());
        tokens.put("classpath", String.join(File.pathSeparator, classpath));

        tokens.put("min_memory", String.valueOf(Launcher.getInstance().getConfig().getMinMemory()));
        tokens.put("max_memory", String.valueOf(Launcher.getInstance().getConfig().getMaxMemory()));

        List<String> arguments = new LinkedList<>();
        File runtimeDir = new File(System.getProperty("java.home"));
        if (modpackManifest.getRuntime() != null) {
            runtimeDir = new File(new File(Launcher.getInstance().getExecutableDirectory(), "custom-runtimes"), modpackManifest.getRuntime());
        }

        arguments.add(new File(runtimeDir, "bin/java" + (Platform.getPlatform() == Platform.WINDOWS ? ".exe" : "")).getAbsolutePath());
        arguments.addAll(jvmArguments);
        arguments.add(modpackManifest.getVersionManifest().getMainClass());
        arguments.addAll(gameArguments);

        LaunchMinecraft.log.info("Startargumente: " + Joiner.on(' ').join(arguments));
        LaunchHelper.replaceTokens(arguments, tokens);

        ProcessBuilder builder = new ProcessBuilder(arguments);
        builder.directory(instanceDir);

        try {
            LaunchMinecraft.log.info("Alle Dateien aktuell, starte Minecraft");
            Launcher.getInstance().getDiscordIntegration().setRunningModpack(modpackManifest);
            LaunchMinecraft.running = true;
            LaunchMinecraft.minecraftProcess = builder.start();
            LaunchMinecraft.logCollector.clear();
            ProcessLogConsumer.attach(LaunchMinecraft.minecraftProcess, LaunchMinecraft.logCollector::log);
            int code = LaunchMinecraft.minecraftProcess.waitFor();
            LaunchMinecraft.logCollector.log("\nProzess mit Code " + code + " beendet\n");
        } finally {
            LaunchMinecraft.log.info("Minecraft Prozess beendet");

            Launcher.getInstance().getDiscordIntegration().setRunningModpack(null);
            LaunchMinecraft.running = false;

            LaunchMinecraft.log.trace("Lösche entpackte Natives");
            Files.walk(nativesDir.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    public static String getLog() {
        return LaunchMinecraft.logCollector.getLog();
    }

    public static void killMinecraft() {
        if (LaunchMinecraft.minecraftProcess != null && LaunchMinecraft.running) {
            LaunchMinecraft.minecraftProcess.destroy();
        }
    }

    @FunctionalInterface
    public interface InstallationStatusListener {
        void progressChange(int total, int finished, int failed);
    }

    public static class ModpackOutdatedException extends RuntimeException {
        public static final long serialVersionUID = 8535148436818832712L;

        private final ModpackManifestList.ModpackManifestReference modpack;

        private ModpackOutdatedException(ModpackManifestList.ModpackManifestReference modpack) {
            this.modpack = modpack;
        }

        public ModpackManifestList.ModpackManifestReference getModpack() {
            return this.modpack;
        }
    }

}
