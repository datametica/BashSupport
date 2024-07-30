/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.application;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.io.URLUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static com.intellij.util.SystemProperties.getUserHome;

public class PathManager {
  public static final String PROPERTIES_FILE_NAME = "idea.properties";
  public static final String PROPERTY_HOME_PATH = "idea.home.path";
  public static final String PROPERTY_SYSTEM_PATH = "idea.system.path";
  public static final String PROPERTY_PATHS_SELECTOR = "idea.paths.selector";

  private static final String PROPERTY_HOME = "idea.home";  // reduced variant of PROPERTY_HOME_PATH, now deprecated

  private static final String LIB_FOLDER = "lib";
  private static final String BIN_FOLDER = "bin";
  private static final String SYSTEM_FOLDER = "system";
  private static final String PATHS_SELECTOR = System.getProperty(PROPERTY_PATHS_SELECTOR);

  private static String ourHomePath;
  private static String ourSystemPath;

  // IDE installation paths

  @NotNull
  public static String getHomePath() {
    if (ourHomePath != null) return ourHomePath;

    String fromProperty = System.getProperty(PROPERTY_HOME_PATH, System.getProperty(PROPERTY_HOME));
    if (fromProperty != null) {
      ourHomePath = getAbsolutePath(fromProperty);
      if (!new File(ourHomePath).isDirectory()) {
        throw new RuntimeException("Invalid home path '" + ourHomePath + "'");
      }
    }
    else {
      ourHomePath = getHomePathFor(PathManager.class);
      if (ourHomePath == null) {
        String advice = SystemInfo.isMac ? "reinstall the software."
                                         : "make sure bin/idea.properties is present in the installation directory.";
        throw new RuntimeException("Could not find installation home path. Please " + advice);
      }
    }

    if (SystemInfo.isWindows) {
      try {
        ourHomePath = new File(ourHomePath).getCanonicalPath();
      }
      catch (IOException ignored) { }
    }

    return ourHomePath;
  }

  @Nullable
  public static String getHomePathFor(@NotNull Class aClass) {
    String rootPath = getResourceRoot(aClass, "/" + aClass.getName().replace('.', '/') + ".class");
    if (rootPath == null) return null;

    File root = new File(rootPath).getAbsoluteFile();
    do {
      String parent = root.getParent();
      if (parent == null) return null;
      root = new File(parent).getAbsoluteFile();  // one step back to get folder
    }
    while (!isIdeaHome(root));
    return root.getAbsolutePath();
  }

  private static boolean isIdeaHome(final File root) {
    return new File(root, FileUtil.toSystemDependentName("bin/" + PROPERTIES_FILE_NAME)).exists() ||
           new File(root, FileUtil.toSystemDependentName("bin/" + getOSSpecificBinSubdir() + "/" + PROPERTIES_FILE_NAME)).exists() ||
           new File(root, FileUtil.toSystemDependentName("community/bin/" + PROPERTIES_FILE_NAME)).exists();
  }

  @NotNull
  public static String getBinPath() {
    return getHomePath() + File.separator + BIN_FOLDER;
  }

  private static String getOSSpecificBinSubdir() {
    if (SystemInfo.isWindows) return "win";
    if (SystemInfo.isMac) return "mac";
    return "linux";
  }

  @NotNull
  public static String getLibPath() {
    return getHomePath() + File.separator + LIB_FOLDER;
  }

  // runtime paths

  @NotNull
  public static String getSystemPath() {
    if (ourSystemPath != null) return ourSystemPath;

    if (System.getProperty(PROPERTY_SYSTEM_PATH) != null) {
      ourSystemPath = getAbsolutePath(trimPathQuotes(System.getProperty(PROPERTY_SYSTEM_PATH)));
    }
    else if (PATHS_SELECTOR != null) {
      ourSystemPath = platformPath(PATHS_SELECTOR, "Library/Caches", SYSTEM_FOLDER);
    }
    else {
      ourSystemPath = getHomePath() + File.separator + SYSTEM_FOLDER;
    }

    checkAndCreate(ourSystemPath, true);
    return ourSystemPath;
  }

  @NotNull
  public static File getIndexRoot() {
    String indexRoot = System.getProperty("index_root_path", getSystemPath() + "/index");
    checkAndCreate(indexRoot, true);
    return new File(indexRoot);
  }

  // misc stuff

  /**
   * Attempts to detect classpath entry which contains given resource.
   */
  @Nullable
  public static String getResourceRoot(@NotNull Class context, String path) {
    URL url = context.getResource(path);
    if (url == null) {
      url = ClassLoader.getSystemResource(path.substring(1));
    }
    return url != null ? extractRoot(url, path) : null;
  }

  /**
   * Attempts to extract classpath entry part from passed URL.
   */
  @Nullable
  private static String extractRoot(URL resourceURL, String resourcePath) {
    if (!(StringUtil.startsWithChar(resourcePath, '/') || StringUtil.startsWithChar(resourcePath, '\\'))) {
      log("precondition failed: " + resourcePath);
      return null;
    }

    String resultPath = null;
    String protocol = resourceURL.getProtocol();
    if (URLUtil.FILE_PROTOCOL.equals(protocol)) {
      String path = URLUtil.urlToFile(resourceURL).getPath();
      String testPath = path.replace('\\', '/');
      String testResourcePath = resourcePath.replace('\\', '/');
      if (StringUtil.endsWithIgnoreCase(testPath, testResourcePath)) {
        resultPath = path.substring(0, path.length() - resourcePath.length());
      }
    }
    else if (URLUtil.JAR_PROTOCOL.equals(protocol)) {
      Pair<String, String> paths = URLUtil.splitJarUrl(resourceURL.getFile());
      if (paths != null) {
        resultPath = paths.first;
      }
    }

    if (resultPath == null) {
      log("cannot extract: " + resourcePath + " from " + resourceURL);
      return null;
    }

    return StringUtil.trimEnd(resultPath, File.separator);
  }

  @Nullable
  public static String getJarPathForClass(@NotNull Class aClass) {
    String resourceRoot = getResourceRoot(aClass, "/" + aClass.getName().replace('.', '/') + ".class");
    return resourceRoot != null ? new File(resourceRoot).getAbsolutePath() : null;
  }

  // helpers

  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  private static void log(String x) {
    System.err.println(x);
  }

  private static String getAbsolutePath(String path) {
    path = FileUtil.expandUserHome(path);
    return FileUtil.toCanonicalPath(new File(path).getAbsolutePath());
  }

  private static String trimPathQuotes(String path) {
    if (path != null && path.length() >= 3 && StringUtil.startsWithChar(path, '\"') && StringUtil.endsWithChar(path, '\"')) {
      path = path.substring(1, path.length() - 1);
    }
    return path;
  }

  // todo[r.sh] XDG directories, Windows folders
  // http://standards.freedesktop.org/basedir-spec/basedir-spec-latest.html
  // http://www.microsoft.com/security/portal/mmpc/shared/variables.aspx
  private static String platformPath(@NotNull String selector, @Nullable String macPart, @NotNull String fallback) {
    return platformPath(selector, macPart, null, null, null, fallback);
  }

  private static String platformPath(@NotNull String selector,
                                     @Nullable String macPart,
                                     @Nullable String winVar,
                                     @Nullable String xdgVar,
                                     @Nullable String xdgDir,
                                     @NotNull String fallback) {
    if (macPart != null && SystemInfo.isMac) {
      return getUserHome() + File.separator + macPart + File.separator + selector;
    }

    if (winVar != null && SystemInfo.isWindows) {
      String dir = System.getenv(winVar);
      if (dir != null) {
        return dir + File.separator + selector;
      }
    }

    if (xdgVar != null && xdgDir != null && SystemInfo.hasXdgOpen()) {
      String dir = System.getenv(xdgVar);
      if (dir == null) dir = getUserHome() + File.separator + xdgDir;
      return dir + File.separator + selector;
    }

    return getUserHome() + File.separator + "." + selector + (!fallback.isEmpty() ? File.separator + fallback : "");
  }

  private static boolean checkAndCreate(String path, boolean createIfNotExists) {
    if (createIfNotExists) {
      File file = new File(path);
      if (!file.exists()) {
        return file.mkdirs();
      }
    }
    return false;
  }
}