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
package de.myftb.launcher.launch;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

import de.myftb.launcher.HttpRequest;
import de.myftb.launcher.models.minecraft.Arguments;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.http.util.EntityUtils;

public class LaunchHelper {
    public static final ObjectMapper mapper = new ObjectMapper()
            .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .registerModule(new SimpleModule()
                    .addDeserializer(Arguments.class, new Arguments.ArgumentsDeserializer())
                    .addSerializer(Arguments.Argument.class, new Arguments.ArgumentSerializer()));

    @FunctionalInterface
    public interface FileHashFunction {
        String getFileHash(File file) throws IOException;
    }

    public static String getFileHash(File file, String hashFunction) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance(hashFunction);

            byte[] buffer = new byte[8192];
            int count;
            try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file))) {
                while ((count = bufferedInputStream.read(buffer)) > 0) {
                    digest.update(buffer, 0, count);
                }
            }

            return Hex.encodeHexString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getStringHash(String value, String hashFunction) {
        try {
            MessageDigest digest = MessageDigest.getInstance(hashFunction);
            digest.update(value.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getSha1(File file) throws IOException {
        return LaunchHelper.getFileHash(file, "SHA-1");
    }

    public static String getSha1(String string) {
        return LaunchHelper.getStringHash(string, "SHA-1");
    }

    public static String getSha256(File file) throws IOException {
        return LaunchHelper.getFileHash(file, "SHA-256");
    }

    public static String getSha256(String string) {
        return LaunchHelper.getStringHash(string, "SHA-256");
    }

    public static void replaceTokens(List<String> list, Map<String, String> tokens) {
        for (int i = 0; i < list.size(); i++) {
            String value = list.get(i);
            for (String token : tokens.keySet()) {
                value = value.replace("${" + token + "}", tokens.get(token));
            }
            list.set(i, value);
        }
    }

    public static String download(String url, Function<String, String> hashFn, String correctHash) throws IOException {
        HttpResponse response = HttpRequest.get(url)
                .execute()
                .returnResponse();

        if (response.getStatusLine().getStatusCode() != 200) {
            throw new HttpResponseException(response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
        }

        String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        if (hashFn != null) {
            String bodyHash = hashFn.apply(body);
            if (correctHash != null && !bodyHash.equals(correctHash)) {
                throw new IOException("Ungültige Prüfsumme beim Download von " + url + ": " + bodyHash + " erwartet: " + correctHash);
            }
        }

        return body;
    }

    public static String download(String url, String correctHash) throws IOException {
        return LaunchHelper.download(url, LaunchHelper::getSha1, correctHash);
    }

    public static String download(String url) throws IOException {
        return LaunchHelper.download(url, null, null);
    }

    public static ExecutorService getNewDaemonThreadPool() {
        return Executors.newFixedThreadPool(java.lang.Runtime.getRuntime().availableProcessors(), runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        });
    }

}
