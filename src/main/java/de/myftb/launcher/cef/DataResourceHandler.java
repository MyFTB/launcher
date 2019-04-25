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

package de.myftb.launcher.cef;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;

import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandlerAdapter;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DataResourceHandler extends CefResourceHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(DataResourceHandler.class);
    private String mimeType;
    private byte[] data;
    private int offset;

    protected abstract void fillDataForRequest(String path, Consumer<String> mimeTypeSetter, Consumer<byte[]> dataSetter);

    @Override
    public boolean processRequest(CefRequest request, CefCallback callback) {
        this.mimeType = null;
        this.data = new byte[0];
        this.offset = 0;

        try {
            URI uri = new URI(request.getURL());
            this.fillDataForRequest(uri.getPath().substring(1), mimeType -> this.mimeType = mimeType, data -> this.data = data);
            callback.Continue();
            return true;
        } catch (URISyntaxException e) {
            DataResourceHandler.log.warn("Fehler beim Lesen der Request URI", e);
            return false;
        }
    }

    @Override
    public void getResponseHeaders(CefResponse response, IntRef responseLength, StringRef redirectUrl) {
        if (this.mimeType != null) {
            response.setMimeType(this.mimeType);
        }
        response.setStatus(this.data.length > 0 ? 200 : 404);
        responseLength.set(this.data.length);
    }

    @Override
    public boolean readResponse(byte[] dataOut, int bytesToRead, IntRef bytesRead, CefCallback callback) {
        boolean dataAvailable = false;

        if (this.offset < this.data.length) {
            final int min = Math.min(bytesToRead, this.data.length - this.offset);
            System.arraycopy(this.data, this.offset, dataOut, 0, min);
            this.offset += min;
            bytesRead.set(min);
            dataAvailable = true;
        } else {
            bytesRead.set(this.offset = 0);
        }

        return dataAvailable;
    }

}
