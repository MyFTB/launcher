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

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefAuthCallback;
import org.cef.callback.CefRequestCallback;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefRequestHandler;
import org.cef.handler.CefResourceHandler;
import org.cef.misc.BoolRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;
import org.cef.network.CefURLRequest;

public class SeqRequestHandler implements CefRequestHandler {
    private final CefRequestHandler[] chain;

    public SeqRequestHandler(CefRequestHandler... chain) {
        this.chain = chain;
    }

    @Override
    public boolean onBeforeBrowse(CefBrowser cefBrowser, CefFrame cefFrame, CefRequest cefRequest, boolean b, boolean b1) {
        for (CefRequestHandler handler : this.chain) {
            if (handler.onBeforeBrowse(cefBrowser, cefFrame, cefRequest, b, b1)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean onBeforeResourceLoad(CefBrowser cefBrowser, CefFrame cefFrame, CefRequest cefRequest) {
        for (CefRequestHandler handler : this.chain) {
            if (handler.onBeforeResourceLoad(cefBrowser, cefFrame, cefRequest)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public CefResourceHandler getResourceHandler(CefBrowser cefBrowser, CefFrame cefFrame, CefRequest cefRequest) {
        for (CefRequestHandler handler : this.chain) {
            CefResourceHandler resourceHandler;
            if ((resourceHandler = handler.getResourceHandler(cefBrowser, cefFrame, cefRequest)) != null) {
                return resourceHandler;
            }
        }

        return null;
    }

    @Override
    public void onResourceRedirect(CefBrowser cefBrowser, CefFrame cefFrame, CefRequest cefRequest, CefResponse cefResponse, StringRef stringRef) {
        for (CefRequestHandler handler : this.chain) {
            handler.onResourceRedirect(cefBrowser, cefFrame, cefRequest, cefResponse, stringRef);
        }
    }

    @Override
    public boolean onResourceResponse(CefBrowser cefBrowser, CefFrame cefFrame, CefRequest cefRequest, CefResponse cefResponse) {
        for (CefRequestHandler handler : this.chain) {
            if (handler.onResourceResponse(cefBrowser, cefFrame, cefRequest, cefResponse)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void onResourceLoadComplete(CefBrowser cefBrowser, CefFrame cefFrame, CefRequest cefRequest, CefResponse cefResponse,
                                       CefURLRequest.Status status, long l) {
        for (CefRequestHandler handler : this.chain) {
            handler.onResourceLoadComplete(cefBrowser, cefFrame, cefRequest, cefResponse, status, l);
        }
    }

    @Override
    public boolean getAuthCredentials(CefBrowser cefBrowser, CefFrame cefFrame, boolean b, String s, int i, String s1, String s2,
                                      CefAuthCallback cefAuthCallback) {
        for (CefRequestHandler handler : this.chain) {
            if (handler.getAuthCredentials(cefBrowser, cefFrame, b, s, i, s1, s2, cefAuthCallback)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean onQuotaRequest(CefBrowser cefBrowser, String s, long l, CefRequestCallback cefRequestCallback) {
        for (CefRequestHandler handler : this.chain) {
            if (handler.onQuotaRequest(cefBrowser, s, l, cefRequestCallback)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void onProtocolExecution(CefBrowser cefBrowser, String s, BoolRef boolRef) {
        for (CefRequestHandler handler : this.chain) {
            handler.onProtocolExecution(cefBrowser, s, boolRef);
        }
    }

    @Override
    public boolean onCertificateError(CefBrowser cefBrowser, CefLoadHandler.ErrorCode errorCode, String s, CefRequestCallback cefRequestCallback) {
        for (CefRequestHandler handler : this.chain) {
            if (handler.onCertificateError(cefBrowser, errorCode, s, cefRequestCallback)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void onPluginCrashed(CefBrowser cefBrowser, String s) {
        for (CefRequestHandler handler : this.chain) {
            handler.onPluginCrashed(cefBrowser, s);
        }
    }

    @Override
    public void onRenderProcessTerminated(CefBrowser cefBrowser, TerminationStatus terminationStatus) {
        for (CefRequestHandler handler : this.chain) {
            handler.onRenderProcessTerminated(cefBrowser, terminationStatus);
        }
    }

}
