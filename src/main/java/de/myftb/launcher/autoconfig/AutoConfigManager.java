/*
 * MyFTBLauncher
 * Copyright (C) 2024 MyFTB <https://myftb.de>
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
package de.myftb.launcher.autoconfig;

import com.google.common.base.Splitter;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.myftb.launcher.Launcher;
import de.myftb.launcher.launch.LaunchHelper;
import de.myftb.launcher.launch.ManifestHelper;
import de.myftb.launcher.models.modpacks.ModpackManifest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

public class AutoConfigManager {

    private static final ExecutorService ioPool = LaunchHelper.getNewDaemonThreadPool();
    private final Map<String, String> optionTranslations = new HashMap<>();
    private final Map<String, Class<?>> optionTypes = new HashMap<>();
    private final Map<String, Constraint> optionConstraints = new HashMap<>();
    private final Set<String> knownOptions = new HashSet<>();

    public AutoConfigManager() {
        this.optionTranslations.put("invertYMouse", "Maus umkehren");
        this.optionTypes.put("invertYMouse", boolean.class);

        this.optionTranslations.put("mouseSensitivity", "Maussensitivität");
        this.optionTypes.put("mouseSensitivity", double.class);

        this.optionTranslations.put("fov", "Sichtfeld");
        this.optionTypes.put("fov", double.class);

        this.optionTranslations.put("gamma", "Helligkeit");
        this.optionTypes.put("gamma", double.class);

        this.optionTranslations.put("renderDistance", "Sichtweite");
        this.optionTypes.put("renderDistance", int.class);
        this.optionConstraints.put("renderDistance", new Constraint(2, 16));

        this.optionTranslations.put("guiScale", "GUI-Größe");
        this.optionTypes.put("guiScale", GuiScale.class);

        this.optionTranslations.put("particles", "Partikel");
        this.optionTypes.put("particles", Particles.class);

        this.optionTranslations.put("bobView", "Gehbewegung");
        this.optionTypes.put("bobView", boolean.class);

        this.optionTranslations.put("maxFps", "FPS Begrenzung");
        this.optionTypes.put("maxFps", int.class);
        this.optionConstraints.put("maxFps", new Constraint(10, 260));

        this.optionTranslations.put("fancyGraphics", "Schöne Grafik");
        this.optionTypes.put("fancyGraphics", boolean.class);

        this.optionTranslations.put("ao", "Sanfte Belichtung");
        this.optionTypes.put("ao", SmoothLigting.class);

        this.optionTranslations.put("renderClouds", "Wolken");
        this.optionTypes.put("renderClouds", Clouds.class);

        this.optionTranslations.put("lang", "Sprache");
        this.optionTypes.put("lang", Language.class);

        this.optionTranslations.put("fullscreen", "Vollbild");
        this.optionTypes.put("fullscreen", boolean.class);

        this.optionTranslations.put("enableVsync", "VSync");
        this.optionTypes.put("enableVsync", boolean.class);

        this.optionTranslations.put("advancedItemTooltips", "Erweiterte Itemtooltips");
        this.optionTypes.put("advancedItemTooltips", boolean.class);

        this.optionTranslations.put("mipmapLevels", "Mipmap-Level");
        this.optionTypes.put("mipmapLevels", int.class);
        this.optionConstraints.put("mipmapLevels", new Constraint(0, 4));

        this.optionTranslations.put("showSubtitles", "Untertitel");
        this.optionTypes.put("showSubtitles", boolean.class);

        this.optionTranslations.put("autoJump", "Autosprung");
        this.optionTypes.put("autoJump", boolean.class);
    }

    public void readAll() throws Exception {
        this.readOptions();
        this.readTranslations();

        this.knownOptions.removeIf(option -> !this.getTranslation(option).isPresent());
        this.knownOptions.removeIf(option -> !this.getType(option).isPresent());
    }

    public void readOptions() throws IOException {
        this.knownOptions.clear();
        for (ModpackManifest manifest : ManifestHelper.getInstalledModpacks()) {
            File optionsFile = new File(manifest.getInstanceDir(), "options.txt");
            if (!optionsFile.isFile()) {
                continue;
            }

            Splitter splitter = Splitter.on(":");
            List<String> lines = IOUtils.readLines(new FileInputStream(optionsFile));
            for (String line : lines) {
                try {
                    Iterator<String> values = splitter.omitEmptyStrings().limit(2).split(line).iterator();
                    String optionName = values.next();
                    this.knownOptions.add(optionName);
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    public void readTranslations() throws IOException, InterruptedException, ExecutionException {
        Set<File> filesToExamine = new HashSet<>();

        File versionsDir = Launcher.getInstance().getSaveSubDirectory("versions");
        for (File versionFile : versionsDir.listFiles()) {
            if (!versionFile.getName().endsWith(".jar")) {
                continue;
            }

            filesToExamine.add(versionFile);
        }

        for (ModpackManifest manifest : ManifestHelper.getInstalledModpacks()) {
            File modsDir = new File(manifest.getInstanceDir(), "mods");
            if (!modsDir.isDirectory()) {
                continue;
            }

            Files.walk(modsDir.toPath())
                    .map(Path::toFile)
                    .filter(file -> file.getName().endsWith(".jar"))
                    .forEach(filesToExamine::add);
        }

        List<TranslationReaderCallable> callables = filesToExamine.stream()
                .map(file -> new TranslationReaderCallable(file, this))
                .collect(Collectors.toList());

        for (Future<Map<String, String>> future : AutoConfigManager.ioPool.invokeAll(callables)) {
            this.optionTranslations.putAll(future.get());
        }
    }

    public Set<String> getKnownOptions() {
        return this.knownOptions;
    }

    public Optional<String> getTranslation(String key) {
        if (this.optionTranslations.containsKey(key)) {
            return Optional.of(this.optionTranslations.get(key));
        } else if (key.startsWith("key_")) {
            return Optional.ofNullable(this.optionTranslations.get(key.substring(4)));
        }

        return Optional.empty();
    }

    public Optional<Class<?>> getType(String key) {
        if (this.optionTypes.containsKey(key)) {
            return Optional.of(this.optionTypes.get(key));
        } else if (key.startsWith("key_")) {
            return Optional.of(Keybinding.class);
        } else if (key.startsWith("soundCategory_")) {
            return Optional.of(double.class);
        }

        return Optional.empty();
    }

    public JsonArray getTypes() {
        Set<Class<?>> knownTypes = new HashSet<>(this.optionTypes.values());
        knownTypes.add(Keybinding.class);

        JsonArray jsonArray = new JsonArray();

        for (Class<?> type : knownTypes) {
            if (type.isEnum()) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("type", type.getSimpleName().toLowerCase());
                JsonArray values = new JsonArray();
                Arrays.stream(type.getEnumConstants()).map(Enum.class::cast).map(Enum::name).forEach(values::add);
                jsonObject.add("values", values);
                jsonArray.add(jsonObject);
            }
        }

        return jsonArray;
    }

    public JsonArray getConstraints() {
        JsonArray constraints = new JsonArray();

        this.optionConstraints.forEach((name, constraint) -> {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", name);
            jsonObject.addProperty("min", constraint.min);
            jsonObject.addProperty("max", constraint.max);
            constraints.add(jsonObject);
        });

        return constraints;
    }

    private static class TranslationReaderCallable implements Callable<Map<String, String>> {
        private final File file;
        private final AutoConfigManager autoConfigManager;

        public TranslationReaderCallable(File file, AutoConfigManager autoConfigManager) {
            this.file = file;
            this.autoConfigManager = autoConfigManager;
        }

        @Override
        public Map<String, String> call() throws Exception {
            JarFile jarFile = new JarFile(this.file);
            Map<String, String> translations = new HashMap<>();

            Enumeration<JarEntry> jarEntries = jarFile.entries();
            while (jarEntries.hasMoreElements()) {
                JarEntry entry = jarEntries.nextElement();
                if (entry.getName().toLowerCase().endsWith("lang/en_us.lang")) {
                    Properties properties = new Properties();
                    properties.load(new InputStreamReader(jarFile.getInputStream(entry), StandardCharsets.UTF_8));
                    for (Map.Entry<Object, Object> langEntry : properties.entrySet()) {
                        String key = String.valueOf(langEntry.getKey());
                        String value = String.valueOf(langEntry.getValue());

                        if (key.startsWith("soundCategory.")) {
                            key = key.replace("soundCategory.", "soundCategory_");
                            value = "Sound: " + value;
                        }

                        if (this.autoConfigManager.knownOptions.contains(key) || this.autoConfigManager.knownOptions.contains("key_" + key)) {
                            translations.put(key, value);
                        }
                    }
                }
            }

            return translations;
        }
    }

    public static class Constraint {
        private final int min;
        private final int max;

        public Constraint(int min, int max) {
            this.min = min;
            this.max = max;
        }
    }

    public enum GuiScale {
        Auto,
        Small,
        Normal,
        Large;

        public Object getValue() {
            return this.ordinal();
        }
    }

    public enum Particles {
        All,
        Decreased,
        Minimal;

        public Object getValue() {
            return this.ordinal();
        }
    }

    public enum SmoothLigting {
        Off,
        Minimum,
        Maximum;

        public Object getValue() {
            return this.ordinal();
        }
    }

    public enum Clouds {
        Fancy("true"),
        Fast("fast"),
        Off("false");

        private final String configName;

        Clouds(String configName) {
            this.configName = configName;
        }

        public Object getValue() {
            return this.configName;
        }
    }

    public enum Language {
        Deutsch("de_de"),
        Oesterreich("de_at"),
        Schweiz("de_ch"),
        Englisch("en_us");

        private final String configName;

        Language(String configName) {
            this.configName = configName;
        }

        public Object getValue() {
            return this.configName;
        }
    }

    public enum Keybinding {
        NONE(),
        ESCAPE(),
        NUM_1(),
        NUM_2(),
        NUM_3(),
        NUM_4(),
        NUM_5(),
        NUM_6(),
        NUM_7(),
        NUM_8(),
        NUM_9(),
        NUM_0(),
        MINUS(),
        EQUALS(),
        BACK(),
        TAB(),
        Q(),
        W(),
        E(),
        R(),
        T(),
        Y(),
        U(),
        I(),
        O(),
        P(),
        LBRACKET(),
        RBRACKET(),
        RETURN(),
        LCONTROL(),
        A(),
        S(),
        D(),
        F(),
        G(),
        H(),
        J(),
        K(),
        L(),
        SEMICOLON(),
        APOSTROPHE(),
        GRAVE(),
        LSHIFT(),
        BACKSLASH(),
        Z(),
        X(),
        C(),
        V(),
        B(),
        N(),
        M(),
        COMMA(),
        PERIOD(),
        SLASH(),
        RSHIFT(),
        MULTIPLY(),
        LMENU(),
        SPACE(),
        CAPSLOCK(),
        F1(),
        F2(),
        F3(),
        F4(),
        F5(),
        F6(),
        F7(),
        F8(),
        F9(),
        F10(),
        NUMLOCK(),
        SCROLL(),
        NUMPAD7(),
        NUMPAD8(),
        NUMPAD9(),
        SUBTRACT(),
        NUMPAD4(),
        NUMPAD5(),
        NUMPAD6(),
        ADD(),
        NUMPAD1(),
        NUMPAD2(),
        NUMPAD3(),
        NUMPAD0(),
        DECIMAL(),
        F11(),
        F12(),
        F13(),
        F14(),
        F15(),
        F16(),
        F17(),
        F18(),
        KANA(),
        F19(),
        CONVERT(),
        NOCONVERT(),
        YEN(),
        NUMPADEQUALS(),
        CIRCUMFLEX(),
        AT(),
        COLON(),
        UNDERLINE(),
        KANJI(),
        STOP(),
        AX(),
        UNLABLED(),
        NUMPADENTER(),
        RCONTROL(),
        SECTION(),
        NUMPADCOMMA(),
        DIVIDE(),
        SYSRQ(),
        RMENU(),
        FUNCTION(),
        PAUSE(),
        HOME(),
        UP(),
        PRIOR(),
        LEFT(),
        RIGHT(),
        END(),
        DOWN(),
        NEXT(),
        INSERT(),
        DELETE(),
        LMETA(),
        RMETA(),
        APPS(),
        POWER(),
        SLEEP(),

        LEFTCLICK(-100),
        RIGHTCLICK(-99),
        MIDDLECLICK(-98),
        BUTTON3(-97),
        BUTTON4(-96),
        BUTTON5(-95),
        BUTTON6(-94),
        BUTTON7(-93),
        BUTTON8(-92),
        BUTTON9(-91),
        BUTTON10(-90);

        private final int keyCode;

        Keybinding(int keyCode) {
            this.keyCode = keyCode;
        }

        Keybinding() {
            this.keyCode = this.ordinal();
        }

        public Object getValue() {
            return this.keyCode;
        }
    }

}
