package com.jushi.library.lottie.network;

import android.content.Context;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.jushi.library.lottie.lottie.L;
import com.jushi.library.lottie.lottie.LottieComposition;
import com.jushi.library.lottie.lottie.LottieCompositionFactory;
import com.jushi.library.lottie.lottie.LottieResult;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipInputStream;

public class NetworkFetcher {

  private final Context appContext;
  private final String url;

  private final NetworkCache networkCache;

  public static LottieResult<LottieComposition> fetchSync(Context context, String url) {
    return new NetworkFetcher(context, url).fetchSync();
  }

  private NetworkFetcher(Context context, String url) {
    appContext = context.getApplicationContext();
    this.url = url;
    networkCache = new NetworkCache(appContext, url);
  }

  @WorkerThread
  public LottieResult<LottieComposition> fetchSync() {
    LottieComposition result = fetchFromCache();
    if (result != null) {
      return new LottieResult<>(result);
    }

    L.debug("Animation for " + url + " not found in cache. Fetching from network.");
    return fetchFromNetwork();
  }

  /**
   * Returns null if the animation doesn't exist in the cache.
   */
  @Nullable
  @WorkerThread
  private LottieComposition fetchFromCache() {
    Pair<FileExtension, InputStream> cacheResult = networkCache.fetch();
    if (cacheResult == null) {
      return null;
    }

    FileExtension extension = cacheResult.first;
    InputStream inputStream = cacheResult.second;
    LottieResult<LottieComposition> result;
    if (extension == FileExtension.ZIP) {
      result = LottieCompositionFactory.fromZipStreamSync(new ZipInputStream(inputStream), url);
    } else {
      result = LottieCompositionFactory.fromJsonInputStreamSync(inputStream, url);
    }
    if (result.getValue() != null) {
      return result.getValue();
    }
    return null;
  }

  @WorkerThread
  private LottieResult<LottieComposition> fetchFromNetwork() {
    try {
      return fetchFromNetworkInternal();
    } catch (IOException e) {
      return new LottieResult<>(e);
    }
  }

  @WorkerThread
  private LottieResult fetchFromNetworkInternal() throws IOException {
    L.debug( "Fetching " + url);
    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

    connection.setRequestMethod("GET");
    connection.connect();

    if (connection.getErrorStream() != null || connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
      BufferedReader r = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
      StringBuilder error = new StringBuilder();
      String line;
      while ((line = r.readLine()) != null) {
        error.append(line).append('\n');
      }
      return new LottieResult<>(new IllegalArgumentException("Unable to fetch " + url + ". Failed with " +
          connection.getResponseCode() + "\n" + error));
    }

    File file;
    FileExtension extension;
    LottieResult<LottieComposition> result;
    switch (connection.getContentType()) {
      case "application/zip":
        L.debug("Handling zip response.");
        extension = FileExtension.ZIP;
        file = networkCache.writeTempCacheFile(connection.getInputStream(), extension);
        result = LottieCompositionFactory.fromZipStreamSync(new ZipInputStream(new FileInputStream(file)), url);
        break;
      case "application/json":
      default:
        L.debug("Received json response.");
        extension = FileExtension.JSON;
        file = networkCache.writeTempCacheFile(connection.getInputStream(), extension);
        result = LottieCompositionFactory.fromJsonInputStreamSync(new FileInputStream(new File(file.getAbsolutePath())), url);
        break;
    }

    if (result.getValue() != null) {
      networkCache.renameTempFile(extension);
    }

    L.debug("Completed fetch from network. Success: " + (result.getValue() != null));
    return result;
  }
}
