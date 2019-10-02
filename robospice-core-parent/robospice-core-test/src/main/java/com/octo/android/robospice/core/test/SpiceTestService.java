package com.octo.android.robospice.core.test;

import android.app.Application;
import android.app.Notification;
import android.content.Context;

import com.octo.android.robospice.SpiceService;
import com.octo.android.robospice.networkstate.NetworkStateChecker;
import com.octo.android.robospice.persistence.CacheManager;
import com.octo.android.robospice.request.RequestProcessor;
import com.octo.android.robospice.stub.DoubleInMemoryPersisterStub;
import com.octo.android.robospice.stub.IntegerPersisterStub;
import com.octo.android.robospice.stub.StringPersisterStub;

/**
 * Only used to test RoboSpice. Will not rely on network state. Multi-threaded
 * spice service.
 * @author sni
 */
public class SpiceTestService extends SpiceService {

    private static final int TEST_THREAD_COUNT = 3;
    private static final int TEST_THREAD_KEEP_ALIVE_TIME = 1000;
    private static final int TEST_THREAD_PRIORITY = Thread.NORM_PRIORITY;
    
    @Override
    public CacheManager createCacheManager(Application application) {
        CacheManager cacheManager = new CacheManager();
        StringPersisterStub stringPersisterStub = new StringPersisterStub(application);
        IntegerPersisterStub integerPersisterStub = new IntegerPersisterStub(application);
        DoubleInMemoryPersisterStub doubleInMemoryPersisterStub = new DoubleInMemoryPersisterStub(application);
        cacheManager.addPersister(stringPersisterStub);
        cacheManager.addPersister(integerPersisterStub);
        cacheManager.addPersister(doubleInMemoryPersisterStub);
        return cacheManager;
    }

    @Override
    protected NetworkStateChecker getNetworkStateChecker() {
        return new NetworkStateChecker() {

            @Override
            public boolean isNetworkAvailable(Context context) {
                return true;
            }

            @Override
            public void checkPermissions(Context context) {
                // do nothing
            }
        };
    }

    @Override
    public int getThreadCount() {
        return TEST_THREAD_COUNT;
    }

    @Override
    public int getCoreThreadCount() {
        return TEST_THREAD_COUNT;
    }

    @Override
    public int getMaximumThreadCount() {
        return TEST_THREAD_COUNT;
    }

    @Override
    public int getThreadPriority() {
        return TEST_THREAD_PRIORITY;
    }

    @Override
    public int getKeepAliveTime() {
        return TEST_THREAD_KEEP_ALIVE_TIME;
    }

    @Override
    public boolean isFailOnCacheError() {
        return true;
    }
    
    @Override
    public Notification createDefaultNotification() {
        Notification notification = super.createDefaultNotification();
        notification.icon = android.R.drawable.stat_sys_download;
        return notification;
    }

    @Override
    public RequestProcessor getRequestProcessor() {
        return super.getRequestProcessor();
    }
}
