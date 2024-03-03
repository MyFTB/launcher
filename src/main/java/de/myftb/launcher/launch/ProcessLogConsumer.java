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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.function.Consumer;

public class ProcessLogConsumer extends Thread {
    private final InputStream inputStream;
    private final PrintWriter outputWriter;

    public ProcessLogConsumer(InputStream inputStream, OutputStream outputStream) {
        this.inputStream = inputStream;
        this.outputWriter = new PrintWriter(outputStream);
        this.setName("ProcessLogConsumer-" + inputStream.hashCode() + "-" + outputStream.hashCode());
        this.setDaemon(true);
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];

        try {
            int read;
            while ((read = this.inputStream.read(buffer)) != -1) {
                String readStr = new String(buffer, 0, read);
                this.outputWriter.append(readStr);
                this.outputWriter.flush();
            }
        } catch (IOException e) {
            // Ignore
        } finally {
            try {
                this.inputStream.close();
                this.outputWriter.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    public static void attach(Process process, Consumer<String> stringConsumer) {
        ConsumingOutputStream outputStream = new ConsumingOutputStream(stringConsumer);
        ProcessLogConsumer stdOutConsumer = new ProcessLogConsumer(process.getInputStream(), outputStream);
        stdOutConsumer.start();
        ProcessLogConsumer stdErrConsumer = new ProcessLogConsumer(process.getErrorStream(), outputStream);
        stdErrConsumer.start();
    }

    private static class ConsumingOutputStream extends ByteArrayOutputStream {
        private final Consumer<String> dataConsumer;

        public ConsumingOutputStream(Consumer<String> dataConsumer) {
            this.dataConsumer = dataConsumer;
        }

        @Override
        public void flush() {
            String data = this.toString();
            if (data.isEmpty()) {
                return;
            }

            this.dataConsumer.accept(data);
            this.reset();
        }
    }

}
