package com.octo.android.robospice.request;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import roboguice.util.temp.Ln;

import com.octo.android.robospice.SpiceService;
import com.octo.android.robospice.persistence.CacheManager;
import com.octo.android.robospice.request.listener.RequestCancellationListener;
import com.octo.android.robospice.request.listener.RequestListener;
import com.octo.android.robospice.request.listener.SpiceServiceListener;

/**
 * Delegate class of the {@link SpiceService}, easier to test than an Android
 * {@link android.app.Service}.
 * @author jva
 */
public class RequestProcessor {
    // ============================================================================================
    // ATTRIBUTES
    // ============================================================================================

    private final Map<CachedSpiceRequest<?>, Set<RequestListener<?>>> mapRequestToRequestListener = Collections.synchronizedMap(new LinkedHashMap<CachedSpiceRequest<?>, Set<RequestListener<?>>>());
    private final RequestProgressManager requestProgressManager;
    private final RequestRunner requestRunner;
    private final CacheManager cacheManager;
    private boolean isStopped;

    // ============================================================================================
    // CONSTRUCTOR
    // ============================================================================================

    /**
     * Build a request processor using a custom. This feature has been
     * implemented following a feature request from Riccardo Ciovati.
     * @param cacheManager
     *            the {@link CacheManager} that will be used to retrieve
     *            requests' result and store them.
     * @param requestProgressManager
     * @param requestRunner 
     */
    public RequestProcessor(final CacheManager cacheManager, final RequestProgressManager requestProgressManager, final RequestRunner requestRunner) {
        this.cacheManager = cacheManager;
        this.requestProgressManager = requestProgressManager;
        requestProgressManager.setMapRequestToRequestListener(mapRequestToRequestListener);
        this.requestRunner = requestRunner;
    }

    // ============================================================================================
    // PUBLIC
    // ============================================================================================
    public void addRequest(final CachedSpiceRequest<?> request, final Set<RequestListener<?>> listRequestListener) {
        if (isStopped) {
            Ln.d("Dropping request : " + request + " as processor is stopped.");
            return;
        }

        Ln.d("Adding request to queue " + hashCode() + ": " + request + " size is " + mapRequestToRequestListener.size());

        if (request.isCancelled()) {
            synchronized (mapRequestToRequestListener) {
                for (final CachedSpiceRequest<?> cachedSpiceRequest : mapRequestToRequestListener.keySet()) {
                    if (request.equals(cachedSpiceRequest)) {
                        cachedSpiceRequest.cancel();
                        requestProgressManager.notifyListenersOfRequestCancellation(request);
                        return;
                    }
                }
            }
        }

        boolean aggregated = false;
        Set<RequestListener<?>> listRequestListenerForThisRequest;

        synchronized (mapRequestToRequestListener) {
            listRequestListenerForThisRequest = mapRequestToRequestListener.get(request);

            if (listRequestListenerForThisRequest == null) {
                if (request.isProcessable()) {
                    Ln.d("Adding entry for type %s and cacheKey %s.", request.getResultType(), request.getRequestCacheKey());
                    listRequestListenerForThisRequest = Collections.synchronizedSet(new HashSet<RequestListener<?>>());
                    this.mapRequestToRequestListener.put(request, listRequestListenerForThisRequest);
                }
            } else {
                Ln.d("Request for type %s and cacheKey %s already exists.", request.getResultType(), request.getRequestCacheKey());
                aggregated = true;
            }
        }

        if (listRequestListener != null && listRequestListenerForThisRequest != null) {
            listRequestListenerForThisRequest.addAll(listRequestListener);
        }

        if (aggregated) {
            requestProgressManager.notifyListenersOfRequestAggregated(request, listRequestListener);
            return;
        }

        if (request.isProcessable()) {
            requestProgressManager.notifyListenersOfRequestAdded(request, listRequestListener);
        } else {
            if (listRequestListenerForThisRequest == null) {
                requestProgressManager.notifyListenersOfRequestNotFound(request, listRequestListener);
            }
            requestProgressManager.notifyOfRequestProcessed(request, listRequestListener);
            // we have to return if request is not processable.
            // fix bug https://github.com/octo-online/robospice/issues/215
            return;
        }

        final RequestCancellationListener requestCancellationListener = new RequestCancellationListener() {

            @Override
            public void onRequestCancelled() {
                requestProgressManager.notifyListenersOfRequestCancellation(request);
                mapRequestToRequestListener.remove(request);
            }
        };
        request.setRequestCancellationListener(requestCancellationListener);

        if (request.isCancelled()) {
            requestProgressManager.notifyListenersOfRequestCancellation(request);
            mapRequestToRequestListener.remove(request);
            return;
        } else {
            requestRunner.executeRequest(request);
        }
    }

    /**
     * Disable request listeners notifications for a specific request.<br/>
     * All listeners associated to this request won't be called when request
     * will finish.<br/>
     * @param request
     *            Request on which you want to disable listeners
     * @param listRequestListener
     *            the collection of listeners associated to request not to be
     *            notified
     */
    public void dontNotifyRequestListenersForRequest(final CachedSpiceRequest<?> request, final Collection<RequestListener<?>> listRequestListener) {
        requestProgressManager.dontNotifyRequestListenersForRequest(request, listRequestListener);
    }

    public boolean removeDataFromCache(final Class<?> clazz, final Object cacheKey) {
        return cacheManager.removeDataFromCache(clazz, cacheKey);
    }

    public void removeAllDataFromCache(final Class<?> clazz) {
        cacheManager.removeAllDataFromCache(clazz);
    }

    public void removeAllDataFromCache() {
        cacheManager.removeAllDataFromCache();
    }

    public boolean isFailOnCacheError() {
        return requestRunner.isFailOnCacheError();
    }

    public void setFailOnCacheError(final boolean failOnCacheError) {
        requestRunner.setFailOnCacheError(failOnCacheError);
    }

    @Override
    public String toString() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('[');
        stringBuilder.append(getClass().getName());
        stringBuilder.append(" : ");

        stringBuilder.append(" request count= ");
        stringBuilder.append(mapRequestToRequestListener.keySet().size());

        stringBuilder.append(", listeners per requests = [");
        for (final Map.Entry<CachedSpiceRequest<?>, Set<RequestListener<?>>> entry : mapRequestToRequestListener.entrySet()) {
            stringBuilder.append(entry.getKey().getClass().getName());
            stringBuilder.append(":");
            stringBuilder.append(entry.getKey());
            stringBuilder.append(" --> ");
            if (entry.getValue() == null) {
                stringBuilder.append(entry.getValue());
            } else {
                stringBuilder.append(entry.getValue().size());
            }
        }
        stringBuilder.append(']');

        stringBuilder.append(']');
        return stringBuilder.toString();
    }

    public void addSpiceServiceListener(final SpiceServiceListener spiceServiceListener) {
        requestProgressManager.addSpiceServiceListener(spiceServiceListener);
    }

    public void removeSpiceServiceListener(final SpiceServiceListener spiceServiceListener) {
        requestProgressManager.removeSpiceServiceListener(spiceServiceListener);
    }

    public void shouldStop() {
        isStopped = true;
        requestRunner.shouldStop();
    }

    public boolean isStopped() {
        return isStopped;
    }
}
