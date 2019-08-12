package org.robolectric.shadows;

import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup.LayoutParams;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebHistoryItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.robolectric.annotation.HiddenApi;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.annotation.Resetter;
import org.robolectric.fakes.RoboWebSettings;
import org.robolectric.util.ReflectionHelpers;

@SuppressWarnings({"UnusedDeclaration"})
@Implements(value = WebView.class)
public class ShadowWebView extends ShadowViewGroup {
  @RealObject private WebView realWebView;

  private static final String HISTORY_KEY = "ShadowWebView.History";

  private static PackageInfo packageInfo = null;

  private String lastUrl;
  private Map<String, String> lastAdditionalHttpHeaders;
  private HashMap<String, Object> javascriptInterfaces = new HashMap<>();
  private WebSettings webSettings = new RoboWebSettings();
  private WebViewClient webViewClient = null;
  private boolean clearCacheCalled = false;
  private boolean clearCacheIncludeDiskFiles = false;
  private boolean clearFormDataCalled = false;
  private boolean clearHistoryCalled = false;
  private boolean clearViewCalled = false;
  private boolean destroyCalled = false;
  private boolean onPauseCalled = false;
  private boolean onResumeCalled = false;
  private WebChromeClient webChromeClient;
  private boolean canGoBack;
  private int goBackInvocations = 0;
  private int reloadInvocations = 0;
  private LoadData lastLoadData;
  private LoadDataWithBaseURL lastLoadDataWithBaseURL;
  private String originalUrl;
  private ArrayList<String> history = new ArrayList<>();
  private String lastEvaluatedJavascript;
  // TODO: Delete this when setCanGoBack is deleted. This is only used to determine which "path" we
  // use when canGoBack or goBack is called.
  private boolean canGoBackIsSet;
  private PageLoadType pageLoadType = PageLoadType.UNDEFINED;

  @HiddenApi
  @Implementation
  public void ensureProviderCreated() {
    final ClassLoader classLoader = getClass().getClassLoader();
    Class<?> webViewProviderClass = getClassNamed("android.webkit.WebViewProvider");
    Field mProvider;
    try {
      mProvider = WebView.class.getDeclaredField("mProvider");
      mProvider.setAccessible(true);
      if (mProvider.get(realView) == null) {
        Object provider =
            Proxy.newProxyInstance(
                classLoader,
                new Class[] {webViewProviderClass},
                new InvocationHandler() {
                  @Override
                  public Object invoke(Object proxy, Method method, Object[] args)
                      throws Throwable {
                    if (method.getName().equals("getViewDelegate")
                        || method.getName().equals("getScrollDelegate")) {
                      return Proxy.newProxyInstance(
                          classLoader,
                          new Class[] {
                            getClassNamed("android.webkit.WebViewProvider$ViewDelegate"),
                            getClassNamed("android.webkit.WebViewProvider$ScrollDelegate")
                          },
                          new InvocationHandler() {
                            @Override
                            public Object invoke(Object proxy, Method method, Object[] args)
                                throws Throwable {
                              return nullish(method);
                            }
                          });
                    }

                    return nullish(method);
                  }
                });
        mProvider.set(realView, provider);
      }
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  @Implementation
  protected void setLayoutParams(LayoutParams params) {
    ReflectionHelpers.setField(realWebView, "mLayoutParams", params);
  }

  private Object nullish(Method method) {
    Class<?> returnType = method.getReturnType();
    if (returnType.equals(long.class)
        || returnType.equals(double.class)
        || returnType.equals(int.class)
        || returnType.equals(float.class)
        || returnType.equals(short.class)
        || returnType.equals(byte.class)) return 0;
    if (returnType.equals(char.class)) return '\0';
    if (returnType.equals(boolean.class)) return false;
    return null;
  }

  private Class<?> getClassNamed(String className) {
    try {
      return getClass().getClassLoader().loadClass(className);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Implementation
  protected void loadUrl(String url) {
    loadUrl(url, null);
  }

  @Implementation
  protected void loadUrl(String url, Map<String, String> additionalHttpHeaders) {
    history.add(0, url);
    originalUrl = url;
    lastUrl = url;

    if (additionalHttpHeaders != null) {
      this.lastAdditionalHttpHeaders = Collections.unmodifiableMap(additionalHttpHeaders);
    } else {
      this.lastAdditionalHttpHeaders = null;
    }

    performPageLoadType(url);
  }

  @Implementation
  protected void loadDataWithBaseURL(
      String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
    if (historyUrl != null) {
      originalUrl = historyUrl;
      history.add(0, historyUrl);
    }
    lastLoadDataWithBaseURL =
        new LoadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);

    performPageLoadType(baseUrl);
  }

  @Implementation
  protected void loadData(String data, String mimeType, String encoding) {
    lastLoadData = new LoadData(data, mimeType, encoding);

    performPageLoadType(data);
  }

  /**
   * Performs no callbacks on {@link WebViewClient} and {@link WebChromeClient} when any of {@link
   * #loadUrl}, {@link loadData} or {@link #loadDataWithBaseURL} is called.
   */
  public void performNoPageLoadClientCallbacks() {
    this.pageLoadType = PageLoadType.UNDEFINED;
  }

  /**
   * Performs callbacks on {@link WebViewClient} and {@link WebChromeClient} that simulates a
   * successful page load when any of {@link #loadUrl}, {@link loadData} or {@link
   * #loadDataWithBaseURL} is called.
   */
  public void performSuccessfulPageLoadClientCallbacks() {
    this.pageLoadType = PageLoadType.SUCCESS;
  }

  private void performPageLoadType(String url) {
    switch (pageLoadType) {
      case SUCCESS:
        performSuccessfulPageLoad(url);
        break;
      case UNDEFINED:
        break;
    }
  }

  private void performSuccessfulPageLoad(String url) {
    new Handler(Looper.getMainLooper())
        .post(
            () -> {
              if (webChromeClient != null) {
                webChromeClient.onProgressChanged(realWebView, 10);
              }
              if (webViewClient != null) {
                webViewClient.onPageStarted(realWebView, url, /* favicon= */ null);
              }
              if (webChromeClient != null) {
                webChromeClient.onProgressChanged(realWebView, 40);
                webChromeClient.onProgressChanged(realWebView, 80);
              }
              if (webViewClient != null && VERSION.SDK_INT >= 23) {
                webViewClient.onPageCommitVisible(realWebView, url);
              }
              if (webChromeClient != null) {
                webChromeClient.onReceivedTitle(realWebView, url);
                webChromeClient.onProgressChanged(realWebView, 100);
              }
              if (webViewClient != null && VERSION.SDK_INT >= 23) {
                webViewClient.onPageFinished(realWebView, url);
              }
            });
  }

  /** @return the last loaded url */
  public String getLastLoadedUrl() {
    return lastUrl;
  }

  @Implementation
  protected String getOriginalUrl() {
    return originalUrl;
  }

  @Implementation
  protected String getUrl() {
    return originalUrl;
  }

  @Implementation
  protected String getTitle() {
    return originalUrl;
  }

  /** @return the additional Http headers that in the same request with last loaded url */
  public Map<String, String> getLastAdditionalHttpHeaders() {
    return lastAdditionalHttpHeaders;
  }

  @Implementation
  protected WebSettings getSettings() {
    return webSettings;
  }

  @Implementation
  protected void setWebViewClient(WebViewClient client) {
    webViewClient = client;
  }

  @Implementation
  protected void setWebChromeClient(WebChromeClient client) {
    webChromeClient = client;
  }

  public WebViewClient getWebViewClient() {
    return webViewClient;
  }

  @Implementation
  protected void addJavascriptInterface(Object obj, String interfaceName) {
    javascriptInterfaces.put(interfaceName, obj);
  }

  public Object getJavascriptInterface(String interfaceName) {
    return javascriptInterfaces.get(interfaceName);
  }

  @Implementation
  protected void removeJavascriptInterface(String name) {
    javascriptInterfaces.remove(name);
  }

  @Implementation
  protected void clearCache(boolean includeDiskFiles) {
    clearCacheCalled = true;
    clearCacheIncludeDiskFiles = includeDiskFiles;
  }

  public boolean wasClearCacheCalled() {
    return clearCacheCalled;
  }

  public boolean didClearCacheIncludeDiskFiles() {
    return clearCacheIncludeDiskFiles;
  }

  @Implementation
  protected void clearFormData() {
    clearFormDataCalled = true;
  }

  public boolean wasClearFormDataCalled() {
    return clearFormDataCalled;
  }

  @Implementation
  protected void clearHistory() {
    clearHistoryCalled = true;
    history.clear();
  }

  public boolean wasClearHistoryCalled() {
    return clearHistoryCalled;
  }

  @Implementation
  protected void reload() {
    reloadInvocations++;
  }

  /** Returns the number of times {@code android.webkit.WebView#reload()} was invoked */
  public int getReloadInvocations() {
    return reloadInvocations;
  }

  @Implementation
  protected void clearView() {
    clearViewCalled = true;
  }

  public boolean wasClearViewCalled() {
    return clearViewCalled;
  }

  @Implementation
  protected void onPause() {
    onPauseCalled = true;
  }

  public boolean wasOnPauseCalled() {
    return onPauseCalled;
  }

  @Implementation
  protected void onResume() {
    onResumeCalled = true;
  }

  public boolean wasOnResumeCalled() {
    return onResumeCalled;
  }

  @Implementation
  protected void destroy() {
    destroyCalled = true;
  }

  public boolean wasDestroyCalled() {
    return destroyCalled;
  }

  /** @return webChromeClient */
  public WebChromeClient getWebChromeClient() {
    return webChromeClient;
  }

  @Implementation
  protected boolean canGoBack() {
    // TODO: Remove the canGoBack check when setCanGoBack is deleted.
    if (canGoBackIsSet) {
      return canGoBack;
    }
    return history.size() > 1;
  }

  @Implementation
  protected void goBack() {
    if (canGoBack()) {
      goBackInvocations++;
      // TODO: Delete this when setCanGoBack is deleted, since this creates two different behavior
      // paths.
      if (canGoBackIsSet) {
        return;
      }
      history.remove(0);
      if (!history.isEmpty()) {
        originalUrl = history.get(0);
      }
    }
  }

  /**
   * This is only a partial implementation of the method, and <b>only performs backward
   * navigation</b>. Any request to go one or more steps forward will be ignored.
   */
  @Implementation
  protected void goBackOrForward(int steps) {
    if (steps >= 0) {
      // TODO: Handle forward navigation.
      return;
    }

    while (steps++ < 0) {
      goBack();
    }
  }

  @Implementation
  protected WebBackForwardList copyBackForwardList() {
    return new BackForwardList(history);
  }

  @Implementation
  protected static String findAddress(String addr) {
    return null;
  }

  /**
   * Overrides the system implementation for getting the WebView package.
   *
   * <p>Returns null by default, but this can be changed with {@code #setCurrentWebviewPackage()}.
   */
  @Implementation(minSdk = Build.VERSION_CODES.O)
  protected static PackageInfo getCurrentWebViewPackage() {
    return packageInfo;
  }

  /** Sets the value to return from {@code #getCurrentWebviewPackage()}. */
  public static void setCurrentWebViewPackage(PackageInfo webViewPackageInfo) {
    packageInfo = webViewPackageInfo;
  }

  @Implementation(minSdk = Build.VERSION_CODES.KITKAT)
  protected void evaluateJavascript(String script, ValueCallback<String> callback) {
    this.lastEvaluatedJavascript = script;
  }

  public String getLastEvaluatedJavascript() {
    return lastEvaluatedJavascript;
  }

  /**
   * Sets the value to return from {@code android.webkit.WebView#canGoBack()}
   *
   * @param canGoBack Value to return from {@code android.webkit.WebView#canGoBack()}
   * @deprecated Do not depend on this method as it will be removed in a future update. The
   *     preferered method is to populate a fake web history to use for going back.
   */
  @Deprecated
  public void setCanGoBack(boolean canGoBack) {
    canGoBackIsSet = true;
    this.canGoBack = canGoBack;
  }

  /**
   * @return goBackInvocations the number of times {@code android.webkit.WebView#goBack()} was
   *     invoked
   */
  public int getGoBackInvocations() {
    return goBackInvocations;
  }

  public LoadData getLastLoadData() {
    return lastLoadData;
  }

  public LoadDataWithBaseURL getLastLoadDataWithBaseURL() {
    return lastLoadDataWithBaseURL;
  }

  @Implementation
  protected WebBackForwardList saveState(Bundle outState) {
    if (history.size() > 0) {
      outState.putStringArrayList(HISTORY_KEY, history);
    }
    return new BackForwardList(history);
  }

  @Implementation
  protected WebBackForwardList restoreState(Bundle inState) {
    history = inState.getStringArrayList(HISTORY_KEY);
    if (history != null && history.size() > 0) {
      originalUrl = history.get(0);
      lastUrl = history.get(0);
      return new BackForwardList(history);
    }
    return null;
  }

  @Resetter
  public static void reset() {
    packageInfo = null;
  }

  public static void setWebContentsDebuggingEnabled(boolean enabled) {}

  public static class LoadDataWithBaseURL {
    public final String baseUrl;
    public final String data;
    public final String mimeType;
    public final String encoding;
    public final String historyUrl;

    public LoadDataWithBaseURL(
        String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
      this.baseUrl = baseUrl;
      this.data = data;
      this.mimeType = mimeType;
      this.encoding = encoding;
      this.historyUrl = historyUrl;
    }
  }

  public static class LoadData {
    public final String data;
    public final String mimeType;
    public final String encoding;

    public LoadData(String data, String mimeType, String encoding) {
      this.data = data;
      this.mimeType = mimeType;
      this.encoding = encoding;
    }
  }

  /**
   * Defines a type of page load which is associated with a certain order of {@link WebViewClient}
   * and {@link WebChromeClient} callbacks.
   *
   * <p>A page load is triggered either using {@link #loadUrl}, {@link loadData} or {@link
   * loadDataWithBaseURL}.
   */
  private enum PageLoadType {
    /** Default type, triggers no {@link WebViewClient} or {@link WebChromeClient} callbacks. */
    UNDEFINED,
    /**
     * Represents a successful page load, which triggers all the associated {@link WebViewClient} or
     * {@link WebChromeClient} callbacks from {@code onPageStarted} until {@code onPageFinished}
     * without any error.
     */
    SUCCESS
  }

  private static class BackForwardList extends WebBackForwardList {
    private final ArrayList<String> history;

    public BackForwardList(ArrayList<String> history) {
      this.history = (ArrayList<String>) history.clone();
      // WebView expects the most recently visited item to be at the end of the list.
      Collections.reverse(this.history);
    }

    @Override
    public int getCurrentIndex() {
      return history.size() - 1;
    }

    @Override
    public int getSize() {
      return history.size();
    }

    @Override
    public HistoryItem getCurrentItem() {
      if (history.isEmpty()) {
        return null;
      }

      return new HistoryItem(history.get(getCurrentIndex()));
    }

    @Override
    public HistoryItem getItemAtIndex(int index) {
      return new HistoryItem(history.get(index));
    }

    @Override
    protected WebBackForwardList clone() {
      return new BackForwardList(history);
    }
  }

  private static class HistoryItem extends WebHistoryItem {
    private final String url;

    public HistoryItem(String url) {
      this.url = url;
    }

    @Override
    public int getId() {
      return url.hashCode();
    }

    @Override
    public Bitmap getFavicon() {
      return null;
    }

    @Override
    public String getOriginalUrl() {
      return url;
    }

    @Override
    public String getTitle() {
      return url;
    }

    @Override
    public String getUrl() {
      return url;
    }

    @Override
    protected HistoryItem clone() {
      return new HistoryItem(url);
    }
  }
}
