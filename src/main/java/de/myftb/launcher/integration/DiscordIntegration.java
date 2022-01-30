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
package de.myftb.launcher.integration;

import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;
import club.minnced.discord.rpc.DiscordUser;

import de.myftb.launcher.models.modpacks.ModpackManifest;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscordIntegration extends TimerTask {
    private static final Logger log = LoggerFactory.getLogger(DiscordIntegration.class);
    private static final String applicationId = "571102332771893268";

    private final DiscordRPC rpc;
    private final Timer timer = new Timer("DiscordIntegrationUpdater", true);
    private boolean needsPresenceUpdate = true;
    private ModpackManifest runningModpack = null;

    public DiscordIntegration() {
        this.rpc = DiscordRPC.INSTANCE;
    }

    public void setup() {
        DiscordEventHandlers handlers = new DiscordEventHandlers();
        handlers.joinRequest = this::onJoinRequest;

        this.rpc.Discord_Initialize(DiscordIntegration.applicationId, handlers, true, "");
        DiscordRichPresence presence = new DiscordRichPresence();
        this.rpc.Discord_UpdatePresence(presence);
        this.timer.scheduleAtFixedRate(this, TimeUnit.SECONDS.toMillis(5), TimeUnit.SECONDS.toMillis(5));
    }

    private void onJoinRequest(DiscordUser user) {
        this.rpc.Discord_Respond(user.userId, DiscordRPC.DISCORD_REPLY_YES);
    }

    @Override
    public void run() {
        if (this.needsPresenceUpdate) {
            DiscordIntegration.log.info("Discord Presence Informationen aktualisiert");
            DiscordRichPresence presence = new DiscordRichPresence();

            if (this.runningModpack == null) {
                presence.details = "Im Launcher";
                presence.largeImageKey = "myftb";
            } else {
                presence.details = "Spielt " + this.runningModpack.getTitle();
                presence.largeImageKey = "myftb";

                presence.partySize = 1;
                presence.partyMax = 5;
                presence.joinSecret = UUID.randomUUID().toString();
                presence.partyId = UUID.randomUUID().toString();
            }

            this.rpc.Discord_UpdatePresence(presence);
            this.needsPresenceUpdate = false;
        }

        this.rpc.Discord_RunCallbacks();
    }

    public void setRunningModpack(ModpackManifest runningModpack) {
        this.runningModpack = runningModpack;
        this.needsPresenceUpdate = true;
    }

}
