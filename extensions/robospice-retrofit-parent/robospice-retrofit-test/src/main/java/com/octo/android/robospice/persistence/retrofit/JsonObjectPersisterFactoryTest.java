package com.octo.android.robospice.persistence.retrofit;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Application;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.CacheCreationException;
import com.octo.android.robospice.persistence.file.InFileObjectPersister;
import com.octo.android.robospice.retrofit.test.model.Curren_weather;
import com.octo.android.robospice.retrofit.test.model.Weather;
import com.octo.android.robospice.retrofit.test.model.WeatherResult;

@SmallTest
public abstract class JsonObjectPersisterFactoryTest extends AndroidTestCase {
    private static final String TEST_TEMP_UNIT = "C";
    private static final String TEST_TEMP = "28";
    private static final String TEST_TEMP2 = "30";
    private static final long SMALL_THREAD_SLEEP = 50;
    private InFileObjectPersister<WeatherResult> inFileObjectPersister;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Thread.sleep(SMALL_THREAD_SLEEP);
        Application application = (Application) getContext().getApplicationContext();
        RetrofitObjectPersisterFactory factory = getRetrofitObjectPersisterFactory(application);
        inFileObjectPersister = factory.createObjectPersister(WeatherResult.class);
    }

    protected abstract RetrofitObjectPersisterFactory getRetrofitObjectPersisterFactory(Application application) throws CacheCreationException;

    @Override
    protected void tearDown() throws Exception {
        inFileObjectPersister.removeAllDataFromCache();
        super.tearDown();
    }

    public void test_canHandleClientRequestStatus() {
        boolean canHandleClientWeatherResult = inFileObjectPersister.canHandleClass(WeatherResult.class);
        assertEquals(true, canHandleClientWeatherResult);
    }

    public void test_saveDataAndReturnData() throws Exception {
        // GIVEN
        WeatherResult weatherRequestStatus = buildWeather(TEST_TEMP, TEST_TEMP_UNIT);

        // WHEN
        WeatherResult weatherReturned = inFileObjectPersister.saveDataToCacheAndReturnData(weatherRequestStatus, "weather.json");

        // THEN
        assertEquals(TEST_TEMP, weatherReturned.getWeather().getCurren_weather().get(0).getTemp());
    }

    public void test_loadDataFromCache_no_expiracy() throws Exception {
        // GIVEN
        WeatherResult weatherRequestStatus = buildWeather(TEST_TEMP, TEST_TEMP_UNIT);
        final String fileName = "toto";
        inFileObjectPersister.saveDataToCacheAndReturnData(weatherRequestStatus, fileName);

        // WHEN
        WeatherResult weatherReturned = inFileObjectPersister.loadDataFromCache(fileName, DurationInMillis.ALWAYS_RETURNED);

        // THEN
        assertEquals(TEST_TEMP, weatherReturned.getWeather().getCurren_weather().get(0).getTemp());
    }

    public void test_loadDataFromCache_not_expired() throws Exception {
        // GIVEN
        WeatherResult weatherRequestStatus = buildWeather(TEST_TEMP, TEST_TEMP_UNIT);
        final String fileName = "toto";
        inFileObjectPersister.saveDataToCacheAndReturnData(weatherRequestStatus, fileName);

        // WHEN
        WeatherResult weatherReturned = inFileObjectPersister.loadDataFromCache(fileName, DurationInMillis.ONE_MINUTE);

        // THEN
        assertEquals(TEST_TEMP, weatherReturned.getWeather().getCurren_weather().get(0).getTemp());
    }

    public void test_loadDataFromCache_expired() throws Exception {
        // GIVEN
        WeatherResult weatherRequestStatus = buildWeather(TEST_TEMP2, TEST_TEMP_UNIT);
        final String fileName = "toto";
        inFileObjectPersister.saveDataToCacheAndReturnData(weatherRequestStatus, fileName);
        File cachedFile = ((RetrofitObjectPersister<?>) inFileObjectPersister).getCacheFile(fileName);
        final int secondsCountBackInTime = 5;
        cachedFile.setLastModified(System.currentTimeMillis() - secondsCountBackInTime * DurationInMillis.ONE_SECOND);

        // WHEN
        WeatherResult weatherReturned = inFileObjectPersister.loadDataFromCache(fileName, DurationInMillis.ONE_SECOND);

        // THEN
        assertNull(weatherReturned);
    }

    public void test_loadAllDataFromCache_with_one_request_in_cache() throws Exception {
        // GIVEN
        WeatherResult weatherRequestStatus = buildWeather(TEST_TEMP, TEST_TEMP_UNIT);
        final String fileName = "toto";
        inFileObjectPersister.saveDataToCacheAndReturnData(weatherRequestStatus, fileName);

        // WHEN
        List<WeatherResult> listWeatherResult = inFileObjectPersister.loadAllDataFromCache();

        // THEN
        assertNotNull(listWeatherResult);
        assertEquals(1, listWeatherResult.size());
        assertEquals(weatherRequestStatus, listWeatherResult.get(0));
    }

    public void test_loadAllDataFromCache_with_two_requests_in_cache() throws Exception {
        // GIVEN
        WeatherResult weatherRequestStatus = buildWeather(TEST_TEMP, TEST_TEMP_UNIT);
        final String fileName = "toto";
        inFileObjectPersister.saveDataToCacheAndReturnData(weatherRequestStatus, fileName);

        WeatherResult weatherRequestStatus2 = buildWeather(TEST_TEMP2, TEST_TEMP_UNIT);
        final String fileName2 = "tutu";
        inFileObjectPersister.saveDataToCacheAndReturnData(weatherRequestStatus2, fileName2);

        // WHEN
        List<WeatherResult> listWeatherResult = inFileObjectPersister.loadAllDataFromCache();

        // THEN
        assertNotNull(listWeatherResult);
        assertEquals(2, listWeatherResult.size());
        assertTrue(listWeatherResult.contains(weatherRequestStatus));
        assertTrue(listWeatherResult.contains(weatherRequestStatus2));
    }

    public void test_loadAllDataFromCache_with_no_requests_in_cache() throws Exception {
        // GIVEN

        // WHEN
        List<WeatherResult> listWeatherResult = inFileObjectPersister.loadAllDataFromCache();

        // THEN
        assertNotNull(listWeatherResult);
        assertTrue(listWeatherResult.isEmpty());
    }

    public void test_removeDataFromCache_when_two_requests_in_cache_and_one_removed() throws Exception {
        // GIVEN
        WeatherResult weatherRequestStatus = buildWeather(TEST_TEMP, TEST_TEMP_UNIT);
        final String fileName = "toto";
        inFileObjectPersister.saveDataToCacheAndReturnData(weatherRequestStatus, fileName);

        WeatherResult weatherRequestStatus2 = buildWeather(TEST_TEMP2, TEST_TEMP_UNIT);
        final String fileName2 = "tutu";
        inFileObjectPersister.saveDataToCacheAndReturnData(weatherRequestStatus2, fileName2);

        inFileObjectPersister.removeDataFromCache(fileName2);

        // WHEN
        List<WeatherResult> listWeatherResult = inFileObjectPersister.loadAllDataFromCache();

        // THEN
        assertNotNull(listWeatherResult);
        assertEquals(1, listWeatherResult.size());
        assertTrue(listWeatherResult.contains(weatherRequestStatus));
        assertFalse(listWeatherResult.contains(weatherRequestStatus2));
    }

    private WeatherResult buildWeather(String temp, String tempUnit) {
        WeatherResult weatherRequestStatus = new WeatherResult();
        Weather weather = new Weather();
        List<Curren_weather> currents = new ArrayList<Curren_weather>();
        Curren_weather current_weather = new Curren_weather();
        current_weather.setTemp(temp);
        current_weather.setTemp_unit(tempUnit);
        currents.add(current_weather);
        weather.setCurren_weather(currents);
        weatherRequestStatus.setWeather(weather);
        return weatherRequestStatus;
    }
}
