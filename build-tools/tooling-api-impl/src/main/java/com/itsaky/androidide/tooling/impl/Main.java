/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.itsaky.androidide.tooling.impl;

import static com.itsaky.androidide.utils.ILogger.newInstance;

import com.itsaky.androidide.models.LogLine;
import com.itsaky.androidide.tooling.api.IToolingApiClient;
import com.itsaky.androidide.tooling.api.util.ToolingApiLauncher;
import com.itsaky.androidide.tooling.impl.progress.ForwardingProgressListener;
import com.itsaky.androidide.utils.ILogger;
import com.itsaky.androidide.utils.JvmLogger;

import org.gradle.tooling.ConfigurableLauncher;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Main {
    private static final ILogger LOG = newInstance("ToolingApiMain");
    public static IToolingApiClient client;
    public static Future<Void> future;

    static {
        JvmLogger.interceptor = Main::onLog;
    }

    public static void main(String[] args) {
        LOG.debug("Starting Tooling API server...");
        final var server = new ToolingApiServerImpl();
        final var launcher = ToolingApiLauncher.createServerLauncher(server, System.in, System.out);
        Main.future = launcher.startListening();
        Main.client = launcher.getRemoteProxy();
        server.connect(client);

        LOG.debug("Server started. Will run until shutdown message is received...");
        try {
            Main.future.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("An error occurred while waiting for shutdown message", e);
        }
    }

    public static void applyCommonArguments(ConfigurableLauncher<?> launcher) {
        final var out = new LoggingOutputStream();
        launcher.setStandardError(out);
        launcher.setStandardOutput(out);
        launcher.setStandardInput(
                new ByteArrayInputStream("NoOp".getBytes(StandardCharsets.UTF_8)));

        launcher.addProgressListener(new ForwardingProgressListener());
    }

    private static void onLog(LogLine line) {
        if (client != null) {
            client.logMessage(line);
        }
    }
}
