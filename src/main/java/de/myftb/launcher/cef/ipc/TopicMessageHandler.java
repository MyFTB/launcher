/*
 * MyFTBLauncher
 * Copyright (C) 2023 MyFTB <https://myftb.de>
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
package de.myftb.launcher.cef.ipc;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.swing.SwingUtilities;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicMessageHandler extends CefMessageRouterHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(TopicMessageHandler.class);
    private static final Gson gson = new Gson();
    private final Map<String, CefQueryCallback> topicCallbacks = new HashMap<>();
    private final Map<String, BiConsumer<JsonObject, JsonQueryCallback>> topicMessageConsumer = new HashMap<>();

    @Override
    public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
        request = request.trim();
        int topicEnd = request.indexOf(':');
        if (topicEnd == -1) {
            topicEnd = request.length();
        }
        String topic = request.substring(0, topicEnd);
        if (request.length() - (topicEnd + 1) > 0) {
            request = request.substring(topicEnd + 1).trim();
        } else {
            request = "";
        }

        if (persistent && "register".equals(request)) {
            this.topicCallbacks.put(topic, callback);
            TopicMessageHandler.log.info("IPC Topic registriert: {}", topic);
            return true;
        } else if (this.topicCallbacks.containsKey(topic) && "unregister".equals(request)) {
            this.topicCallbacks.remove(topic);
            return true;
        } else if (this.topicMessageConsumer.containsKey(topic)) {
            JsonObject object = null;
            if (!request.isEmpty()) {
                object = TopicMessageHandler.gson.fromJson(request, JsonElement.class).getAsJsonObject();
            }

            this.topicMessageConsumer.get(topic).accept(object, new JsonQueryCallback(callback));
            return true;
        } else {
            callback.failure(-1, "Nicht registriert bzw. nicht registrierbar");
            return true;
        }
    }

    public void listen(String topic, BiConsumer<JsonObject, JsonQueryCallback> messageConsumer) {
        this.topicMessageConsumer.put(topic, messageConsumer);
    }

    public void listenAsync(String topic, BiConsumer<JsonObject, JsonQueryCallback> messageConsumer) {
        this.listen(topic, (data, callback) -> {
            Thread thread = new Thread(() -> messageConsumer.accept(data, callback));

            thread.setDaemon(true);
            thread.start();
        });
    }

    public void send(String topic, Object object) {
        if (this.topicCallbacks.containsKey(topic)) {
            String message = object instanceof JsonObject ? object.toString() : TopicMessageHandler.gson.toJson(object);
            SwingUtilities.invokeLater(() -> this.topicCallbacks.get(topic).success(message));
        }
    }

    public void sendString(String topic, String message) {
        if (this.topicCallbacks.containsKey(topic)) {
            SwingUtilities.invokeLater(() -> this.topicCallbacks.get(topic).success(message));
        }
    }

    public static class JsonQueryCallback {
        private final CefQueryCallback callback;

        public JsonQueryCallback(CefQueryCallback callback) {
            this.callback = callback;
        }

        public void success(Object response) {
            String message = response instanceof JsonObject ? response.toString() : TopicMessageHandler.gson.toJson(response);
            SwingUtilities.invokeLater(() -> this.callback.success(message));
        }

        public void failure(String response) {
            SwingUtilities.invokeLater(() -> this.callback.failure(-1, response));
        }

        public CefQueryCallback getCallback() {
            return this.callback;
        }
    }

}
