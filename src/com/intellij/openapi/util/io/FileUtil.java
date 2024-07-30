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
package com.intellij.openapi.util.io;

import com.intellij.CommonBundle;
import com.intellij.Patches;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.NotNullProducer;
import com.intellij.util.SystemProperties;
import com.intellij.util.ThreeState;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.URLUtil;
import com.intellij.util.text.FilePathHashingStrategy;
import gnu.trove.TObjectHashingStrategy;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.List;
import java.util.regex.Pattern;

@SuppressWarnings({"UtilityClassWithoutPrivateConstructor", "MethodOverridesStaticMethodOfSuperclass"})
public class FileUtil extends FileUtilRt {
  static {
    if (!Patches.USE_REFLECTION_TO_ACCESS_JDK7) throw new RuntimeException("Please migrate FileUtilRt to JDK8");
  }

  public static final String ASYNC_DELETE_EXTENSION = ".__del__";

  public static final int REGEX_PATTERN_FLAGS = SystemInfo.isFileSystemCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE;

  public static final TObjectHashingStrategy<String> PATH_HASHING_STRATEGY = FilePathHashingStrategy.create();

  public static final TObjectHashingStrategy<File> FILE_HASHING_STRATEGY =
    SystemInfo.isFileSystemCaseSensitive ? ContainerUtil.<File>canonicalStrategy() : new TObjectHashingStrategy<File>() {
      @Override
      public int computeHashCode(File object) {
        return fileHashCode(object);
      }

      @Override
      public boolean equals(File o1, File o2) {
        return filesEqual(o1, o2);
      }
    };

  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.util.io.FileUtil");

  @NotNull
  public static String join(@NotNull final String... parts) {
    return StringUtil.join(parts, File.separator);
  }

  public static boolean exists(@Nullable String path) {
    return path != null && new File(path).exists();
  }

  /**
   * Check if the {@code ancestor} is an ancestor of {@code file}.
   *
   * @param ancestor supposed ancestor.
   * @param file     supposed descendant.
   * @param strict   if {@code false} then this method returns {@code true} if {@code ancestor} equals to {@code file}.
   * @return {@code true} if {@code ancestor} is parent of {@code file}; {@code false} otherwise.
   */
  public static boolean isAncestor(@NotNull File ancestor, @NotNull File file, boolean strict) {
    return isAncestor(ancestor.getPath(), file.getPath(), strict);
  }

  public static boolean isAncestor(@NotNull String ancestor, @NotNull String file, boolean strict) {
    return !ThreeState.NO.equals(isAncestorThreeState(ancestor, file, strict));
  }

  /**
   * Checks if the {@code ancestor} is an ancestor of the {@code file}, and if it is an immediate parent or not.
   *
   * @param ancestor supposed ancestor.
   * @param file     supposed descendant.
   * @param strict   if {@code false}, the file can be ancestor of itself,
   *                 i.e. the method returns {@code ThreeState.YES} if {@code ancestor} equals to {@code file}.
   *
   * @return {@code ThreeState.YES} if ancestor is an immediate parent of the file,
   *         {@code ThreeState.UNSURE} if ancestor is not immediate parent of the file,
   *         {@code ThreeState.NO} if ancestor is not a parent of the file at all.
   */
  @NotNull
  public static ThreeState isAncestorThreeState(@NotNull String ancestor, @NotNull String file, boolean strict) {
    String ancestorPath = toCanonicalPath(ancestor);
    String filePath = toCanonicalPath(file);
    return startsWith(filePath, ancestorPath, strict, SystemInfo.isFileSystemCaseSensitive, true);
  }

  public static boolean startsWith(@NotNull String path, @NotNull String start) {
    return !ThreeState.NO.equals(startsWith(path, start, false, SystemInfo.isFileSystemCaseSensitive, false));
  }

  public static boolean startsWith(@NotNull String path, @NotNull String start, boolean caseSensitive) {
    return !ThreeState.NO.equals(startsWith(path, start, false, caseSensitive, false));
  }

  @NotNull
  private static ThreeState startsWith(@NotNull String path, @NotNull String prefix, boolean strict, boolean caseSensitive,
                                       boolean checkImmediateParent) {
    final int pathLength = path.length();
    final int prefixLength = prefix.length();
    if (prefixLength == 0) return pathLength == 0 ? ThreeState.YES : ThreeState.UNSURE;
    if (prefixLength > pathLength) return ThreeState.NO;
    if (!path.regionMatches(!caseSensitive, 0, prefix, 0, prefixLength)) return ThreeState.NO;
    if (pathLength == prefixLength) {
      return strict ? ThreeState.NO : ThreeState.YES;
    }
    char lastPrefixChar = prefix.charAt(prefixLength - 1);
    int slashOrSeparatorIdx = prefixLength;
    if (lastPrefixChar == '/' || lastPrefixChar == File.separatorChar) {
      slashOrSeparatorIdx = prefixLength - 1;
    }
    char next1 = path.charAt(slashOrSeparatorIdx);
    if (next1 == '/' || next1 == File.separatorChar) {
      if (!checkImmediateParent) return ThreeState.YES;

      if (slashOrSeparatorIdx == pathLength - 1) return ThreeState.YES;
      int idxNext = path.indexOf(next1, slashOrSeparatorIdx + 1);
      idxNext = idxNext == -1 ? path.indexOf(next1 == '/' ? '\\' : '/', slashOrSeparatorIdx + 1) : idxNext;
      return idxNext == -1 ? ThreeState.YES : ThreeState.UNSURE;
    }
    else {
      return ThreeState.NO;
    }
  }

  @NotNull
  public static byte[] loadFileBytes(@NotNull File file) throws IOException {
    byte[] bytes;
    final InputStream stream = new FileInputStream(file);
    try {
      final long len = file.length();
      if (len < 0) {
        throw new IOException("File length reported negative, probably doesn't exist");
      }

      if (isTooLarge(len)) {
        throw new FileTooBigException("Attempt to load '" + file + "' in memory buffer, file length is " + len + " bytes.");
      }

      bytes = loadBytes(stream, (int)len);
    }
    finally {
      stream.close();
    }
    return bytes;
  }

  public static boolean delete(@NotNull File file) {
    if (NIOReflect.IS_AVAILABLE) {
      return deleteRecursivelyNIO(file);
    }
    return deleteRecursively(file);
  }

  private static boolean deleteRecursively(@NotNull File file) {
    FileAttributes attributes = FileSystemUtil.getAttributes(file);
    if (attributes == null) return true;

    if (attributes.isDirectory() && !attributes.isSymLink()) {
      File[] files = file.listFiles();
      if (files != null) {
        for (File child : files) {
          if (!deleteRecursively(child)) return false;
        }
      }
    }

    return deleteFile(file);
  }

  public static boolean createParentDirs(@NotNull File file) {
    return FileUtilRt.createParentDirs(file);
  }

  public static boolean createIfDoesntExist(@NotNull File file) {
    return FileUtilRt.createIfNotExists(file);
  }

  public static boolean ensureCanCreateFile(@NotNull File file) {
    return FileUtilRt.ensureCanCreateFile(file);
  }

  public static void copy(@NotNull File fromFile, @NotNull File toFile) throws IOException {
    performCopy(fromFile, toFile, true);
  }

  @SuppressWarnings("Duplicates")
  private static void performCopy(@NotNull File fromFile, @NotNull File toFile, final boolean syncTimestamp) throws IOException {
    if (filesEqual(fromFile, toFile)) return;
    final FileOutputStream fos;
    try {
      fos = openOutputStream(toFile);
    }
    catch (IOException e) {
      if (SystemInfo.isWindows && e.getMessage() != null && e.getMessage().contains("denied") &&
          WinUACTemporaryFix.nativeCopy(fromFile, toFile, syncTimestamp)) {
        return;
      }
      throw e;
    }

    try {
      final FileInputStream fis = new FileInputStream(fromFile);
      try {
        copy(fis, fos);
      }
      finally {
        fis.close();
      }
    }
    finally {
      fos.close();
    }

    if (syncTimestamp) {
      final long timeStamp = fromFile.lastModified();
      if (timeStamp < 0) {
        LOG.warn("Invalid timestamp " + timeStamp + " of '" + fromFile + "'");
      }
      else if (!toFile.setLastModified(timeStamp)) {
        LOG.warn("Unable to set timestamp " + timeStamp + " to '" + toFile + "'");
      }
    }

    if (SystemInfo.isUnix && fromFile.canExecute()) {
      FileSystemUtil.clonePermissionsToExecute(fromFile.getPath(), toFile.getPath());
    }
  }

  private static FileOutputStream openOutputStream(@NotNull final File file) throws IOException {
    try {
      return new FileOutputStream(file);
    }
    catch (FileNotFoundException e) {
      final File parentFile = file.getParentFile();
      if (parentFile == null) {
        throw new IOException("Parent file is null for " + file.getPath(), e);
      }
      createParentDirs(file);
      return new FileOutputStream(file);
    }
  }

  public static void copy(@NotNull InputStream inputStream, @NotNull OutputStream outputStream) throws IOException {
    FileUtilRt.copy(inputStream, outputStream);
  }

  public static void copy(@NotNull InputStream inputStream, int maxSize, @NotNull OutputStream outputStream) throws IOException {
    final byte[] buffer = getThreadLocalBuffer();
    int toRead = maxSize;
    while (toRead > 0) {
      int read = inputStream.read(buffer, 0, Math.min(buffer.length, toRead));
      if (read < 0) break;
      toRead -= read;
      outputStream.write(buffer, 0, read);
    }
  }

  public static void copyFileOrDir(@NotNull File from, @NotNull File to, boolean isDir) throws IOException {
    if (isDir) {
      copyDir(from, to);
    }
    else {
      copy(from, to);
    }
  }

  public static void copyDir(@NotNull File fromDir, @NotNull File toDir) throws IOException {
    copyDir(fromDir, toDir, true);
  }

  public static void copyDir(@NotNull File fromDir, @NotNull File toDir, boolean copySystemFiles) throws IOException {
    copyDir(fromDir, toDir, copySystemFiles ? null : new FileFilter() {
      @Override
      public boolean accept(@NotNull File file) {
        return !StringUtil.startsWithChar(file.getName(), '.');
      }
    });
  }

  public static void copyDir(@NotNull File fromDir, @NotNull File toDir, @Nullable final FileFilter filter) throws IOException {
    ensureExists(toDir);
    if (isAncestor(fromDir, toDir, true)) {
      LOG.error(fromDir.getAbsolutePath() + " is ancestor of " + toDir + ". Can't copy to itself.");
      return;
    }
    File[] files = fromDir.listFiles();
    if (files == null) throw new IOException(CommonBundle.message("exception.directory.is.invalid".toString(), fromDir.getPath()));
    if (!fromDir.canRead()) throw new IOException(CommonBundle.message("exception.directory.is.not.readable".toString(), fromDir.getPath()));
    for (File file : files) {
      if (filter != null && !filter.accept(file)) {
        continue;
      }
      if (file.isDirectory()) {
        copyDir(file, new File(toDir, file.getName()), filter);
      }
      else {
        copy(file, new File(toDir, file.getName()));
      }
    }
  }

  public static void ensureExists(@NotNull File dir) throws IOException {
    if (!dir.exists() && !dir.mkdirs()) {
      throw new IOException(CommonBundle.message("exception.directory.can.not.create".toString(), dir.getPath()));
    }
  }

  public static File findSequentNonexistentFile(@NotNull File parentFolder, @NotNull  String filePrefix, @NotNull String extension) {
    int postfix = 0;
    String ext = extension.isEmpty() ? "" : '.' + extension;
    File candidate = new File(parentFolder, filePrefix + ext);
    while (candidate.exists()) {
      postfix++;
      candidate = new File(parentFolder, filePrefix + Integer.toString(postfix) + ext);
    }
    return candidate;
  }

  @NotNull
  public static String toSystemDependentName(@NotNull String aFileName) {
    return FileUtilRt.toSystemDependentName(aFileName);
  }

  @NotNull
  public static String toSystemIndependentName(@NotNull String aFileName) {
    return FileUtilRt.toSystemIndependentName(aFileName);
  }

  /** @deprecated to be removed in IDEA 17 */
  @SuppressWarnings({"unused", "StringToUpperCaseOrToLowerCaseWithoutLocale"})
  public static String nameToCompare(@NotNull String name) {
    return (SystemInfo.isFileSystemCaseSensitive ? name : name.toLowerCase()).replace('\\', '/');
  }

  /**
   * Converts given path to canonical representation by eliminating '.'s, traversing '..'s, and omitting duplicate separators.
   * Please note that this method is symlink-unfriendly (i.e. result of "/path/to/link/../next" most probably will differ from
   * what { File#getCanonicalPath()} will return) - so use with care.<br>
   * <br>
   * If the path may contain symlinks, use { FileUtil#toCanonicalPath(String, boolean)} instead.
   */
  @Contract("null -> null")
  public static String toCanonicalPath(@Nullable String path) {
    return toCanonicalPath(path, File.separatorChar, true);
  }

  @Contract("null, _, _ -> null")
  private static String toCanonicalPath(@Nullable String path, char separatorChar, boolean removeLastSlash) {
    return toCanonicalPath(path, separatorChar, removeLastSlash, false);
  }

  @Contract("null, _, _, _ -> null")
  private static String toCanonicalPath(@Nullable String path,
                                        final char separatorChar,
                                        final boolean removeLastSlash,
                                        final boolean resolveSymlinks) {
    if (path == null || path.isEmpty()) {
      return path;
    }
    if (StringUtil.startsWithChar(path, '.')) {
      if (path.length() == 1) {
        return "";
      }
      char c = path.charAt(1);
      if (c == '/' || c == separatorChar) {
        path = path.substring(2);
      }
    }

    path = path.replace(separatorChar, '/');
    // trying to speedup the common case when there are no "//" or "/."
    int index = -1;
    do {
      index = path.indexOf('/', index+1);
      char next = index == path.length() - 1 ? 0 : path.charAt(index + 1);
      if (next == '.' || next == '/') {
        break;
      }
    }
    while (index != -1);
    if (index == -1) {
      if (removeLastSlash) {
        int start = processRoot(path, NullAppendable.INSTANCE);
        int slashIndex = path.lastIndexOf('/');
        return slashIndex != -1 && slashIndex > start ? StringUtil.trimEnd(path, '/') : path;
      }
      return path;
    }

    final String finalPath = path;
    NotNullProducer<String> realCanonicalPath = resolveSymlinks ? new NotNullProducer<String>() {
      @NotNull
      @Override
      public String produce() {
        try {
          return new File(finalPath).getCanonicalPath().replace(separatorChar, '/');
        }
        catch (IOException ignore) {
          // fall back to the default behavior
          return toCanonicalPath(finalPath, separatorChar, removeLastSlash, false);
        }
      }
    } : null;

    StringBuilder result = new StringBuilder(path.length());
    int start = processRoot(path, result);
    int dots = 0;
    boolean separator = true;

    for (int i = start; i < path.length(); ++i) {
      char c = path.charAt(i);
      if (c == '/') {
        if (!separator) {
          if (!processDots(result, dots, start, resolveSymlinks)) {
            return realCanonicalPath.produce();
          }
          dots = 0;
        }
        separator = true;
      }
      else if (c == '.') {
        if (separator || dots > 0) {
          ++dots;
        }
        else {
          result.append('.');
        }
        separator = false;
      }
      else {
        if (dots > 0) {
          StringUtil.repeatSymbol(result, '.', dots);
          dots = 0;
        }
        result.append(c);
        separator = false;
      }
    }

    if (dots > 0) {
      if (!processDots(result, dots, start, resolveSymlinks)) {
        return realCanonicalPath.produce();
      }
    }

    int lastChar = result.length() - 1;
    if (removeLastSlash && lastChar >= 0 && result.charAt(lastChar) == '/' && lastChar > start) {
      result.deleteCharAt(lastChar);
    }

    return result.toString();
  }

  private static int processRoot(@NotNull String path, @NotNull Appendable result) {
    try {
      if (SystemInfo.isWindows && path.length() > 1 && path.charAt(0) == '/' && path.charAt(1) == '/') {
        result.append("//");

        int hostStart = 2;
        while (hostStart < path.length() && path.charAt(hostStart) == '/') hostStart++;
        if (hostStart == path.length()) return hostStart;
        int hostEnd = path.indexOf('/', hostStart);
        if (hostEnd < 0) hostEnd = path.length();
        result.append(path, hostStart, hostEnd);
        result.append('/');

        int shareStart = hostEnd;
        while (shareStart < path.length() && path.charAt(shareStart) == '/') shareStart++;
        if (shareStart == path.length()) return shareStart;
        int shareEnd = path.indexOf('/', shareStart);
        if (shareEnd < 0) shareEnd = path.length();
        result.append(path, shareStart, shareEnd);
        result.append('/');

        return shareEnd;
      }
      if (!path.isEmpty() && path.charAt(0) == '/') {
        result.append('/');
        return 1;
      }
      if (path.length() > 2 && path.charAt(1) == ':' && path.charAt(2) == '/') {
        result.append(path, 0, 3);
        return 3;
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    return 0;
  }

  @Contract("_, _, _, false -> true")
  private static boolean processDots(@NotNull StringBuilder result, int dots, int start, boolean resolveSymlinks) {
    if (dots == 2) {
      int pos = -1;
      if (!StringUtil.endsWith(result, "/../") && !StringUtil.equals(result, "../")) {
        pos = StringUtil.lastIndexOf(result, '/', start, result.length() - 1);
        if (pos >= 0) {
          ++pos;  // separator found, trim to next char
        }
        else if (start > 0) {
          pos = start;  // path is absolute, trim to root ('/..' -> '/')
        }
        else if (result.length() > 0) {
          pos = 0;  // path is relative, trim to default ('a/..' -> '')
        }
      }
      if (pos >= 0) {
        if (resolveSymlinks && FileSystemUtil.isSymLink(new File(result.toString()))) {
          return false;
        }
        result.delete(pos, result.length());
      }
      else {
        result.append("../");  // impossible to traverse, keep as-is
      }
    }
    else if (dots != 1) {
      StringUtil.repeatSymbol(result, '.', dots);
      result.append('/');
    }
    return true;
  }

  /**
   * converts back slashes to forward slashes
   * removes double slashes inside the path, e.g. "x/y//z" => "x/y/z"
   */
  @NotNull
  public static String normalize(@NotNull String path) {
    int start = 0;
    boolean separator = false;
    if (SystemInfo.isWindows) {
      if (path.startsWith("//")) {
        start = 2;
        separator = true;
      }
      else if (path.startsWith("\\\\")) {
        return normalizeTail(0, path, false);
      }
    }

    for (int i = start; i < path.length(); ++i) {
      final char c = path.charAt(i);
      if (c == '/') {
        if (separator) {
          return normalizeTail(i, path, true);
        }
        separator = true;
      }
      else if (c == '\\') {
        return normalizeTail(i, path, separator);
      }
      else {
        separator = false;
      }
    }

    return path;
  }

  @NotNull
  private static String normalizeTail(int prefixEnd, @NotNull String path, boolean separator) {
    final StringBuilder result = new StringBuilder(path.length());
    result.append(path, 0, prefixEnd);
    int start = prefixEnd;
    if (start==0 && SystemInfo.isWindows && (path.startsWith("//") || path.startsWith("\\\\"))) {
      start = 2;
      result.append("//");
      separator = true;
    }

    for (int i = start; i < path.length(); ++i) {
      final char c = path.charAt(i);
      if (c == '/' || c == '\\') {
        if (!separator) result.append('/');
        separator = true;
      }
      else {
        result.append(c);
        separator = false;
      }
    }

    return result.toString();
  }

  @NotNull
  public static String unquote(@NotNull String urlString) {
    urlString = urlString.replace('/', File.separatorChar);
    return URLUtil.unescapePercentSequences(urlString);
  }

  public static boolean rename(@NotNull File source, @NotNull String newName) throws IOException {
    File target = new File(source.getParent(), newName);
    if (!SystemInfo.isFileSystemCaseSensitive && newName.equalsIgnoreCase(source.getName())) {
      File intermediate = createTempFile(source.getParentFile(), source.getName(), ".tmp", false, false);
      return source.renameTo(intermediate) && intermediate.renameTo(target);
    }
    else {
      return source.renameTo(target);
    }
  }

  public static void rename(@NotNull File source, @NotNull File target) throws IOException {
    if (source.renameTo(target)) return;
    if (!source.exists()) return;

    copy(source, target);
    delete(source);
  }

  public static boolean filesEqual(@Nullable File file1, @Nullable File file2) {
    // on MacOS java.io.File.equals() is incorrectly case-sensitive
    return pathsEqual(file1 == null ? null : file1.getPath(),
                      file2 == null ? null : file2.getPath());
  }

  public static boolean pathsEqual(@Nullable String path1, @Nullable String path2) {
    if (path1 == path2) return true;
    if (path1 == null || path2 == null) return false;

    path1 = toCanonicalPath(path1);
    path2 = toCanonicalPath(path2);
    return PATH_HASHING_STRATEGY.equals(path1, path2);
  }

  public static int fileHashCode(@Nullable File file) {
    return pathHashCode(file == null ? null : file.getPath());
  }

  public static int pathHashCode(@Nullable String path) {
    return StringUtil.isEmpty(path) ? 0 : PATH_HASHING_STRATEGY.computeHashCode(toCanonicalPath(path));
  }

  /**
   * @deprecated this method returns extension converted to lower case, this may not be correct for case-sensitive FS.
   *             Use { FileUtilRt#getExtension(String)} instead to get the unchanged extension.
   *             If you need to check whether a file has a specified extension use { FileUtilRt#extensionEquals(String, String)}
   */
  @SuppressWarnings("StringToUpperCaseOrToLowerCaseWithoutLocale")
  @NotNull
  public static String getExtension(@NotNull String fileName) {
    return FileUtilRt.getExtension(fileName).toLowerCase();
  }

  @NotNull
  public static String resolveShortWindowsName(@NotNull String path) throws IOException {
    return SystemInfo.isWindows && containsWindowsShortName(path) ? new File(path).getCanonicalPath() : path;
  }

  public static boolean containsWindowsShortName(@NotNull String path) {
    if (StringUtil.containsChar(path, '~')) {
      path = toSystemIndependentName(path);

      int start = 0;
      while (start < path.length()) {
        int end = path.indexOf('/', start);
        if (end < 0) end = path.length();

        // "How Windows Generates 8.3 File Names from Long File Names", https://support.microsoft.com/en-us/kb/142982
        int dot = path.lastIndexOf('.', end);
        if (dot < start) dot = end;
        if (dot - start > 2 && dot - start <= 8 && end - dot - 1 <= 3 &&
            path.charAt(dot - 2) == '~' && Character.isDigit(path.charAt(dot - 1))) {
          return true;
        }

        start = end + 1;
      }
    }

    return false;
  }

  /** @deprecated use { #sanitizeFileName(String, boolean)} (to be removed in IDEA 17) */
  public static String sanitizeName(@NotNull String name) {
    return sanitizeFileName(name, false);
  }

  @NotNull
  public static String sanitizeFileName(@NotNull String name, boolean strict) {
    StringBuilder result = null;

    int last = 0;
    int length = name.length();
    for (int i = 0; i < length; i++) {
      char c = name.charAt(i);
      boolean appendReplacement = true;
      if (c > 0 && c < 255) {
        if (strict ? Character.isLetterOrDigit(c) || c == '_' : Character.isJavaIdentifierPart(c) || c == ' ' || c == '@' || c == '-') {
          continue;
        }
      }
      else {
        appendReplacement = false;
      }

      if (result == null) {
        result = new StringBuilder();
      }
      if (last < i) {
        result.append(name, last, i);
      }
      if (appendReplacement) {
        result.append('_');
      }
      last = i + 1;
    }

    if (result == null) {
      return name;
    }

    if (last < length) {
      result.append(name, last, length);
    }

    return result.toString();
  }

  public static boolean canWrite(@NotNull String path) {
    FileAttributes attributes = FileSystemUtil.getAttributes(path);
    return attributes != null && attributes.isWritable();
  }

  public static void setReadOnlyAttribute(@NotNull String path, boolean readOnlyFlag) {
    boolean writableFlag = !readOnlyFlag;
    if (!new File(path).setWritable(writableFlag, false) && canWrite(path) != writableFlag) {
      LOG.warn("Can't set writable attribute of '" + path + "' to '" + readOnlyFlag + "'");
    }
  }

  public static void writeToFile(@NotNull File file, @NotNull byte[] text) throws IOException {
    writeToFile(file, text, false);
  }

  public static void writeToFile(@NotNull File file, @NotNull byte[] text, boolean append) throws IOException {
    writeToFile(file, text, 0, text.length, append);
  }

  private static void writeToFile(@NotNull File file, @NotNull byte[] text, int off, int len, boolean append) throws IOException {
    createParentDirs(file);

    OutputStream stream = new FileOutputStream(file, append);
    try {
      stream.write(text, off, len);
    }
    finally {
      stream.close();
    }
  }

  @Nullable
  public static File findFirstThatExist(@NotNull String... paths) {
    for (String path : paths) {
      if (!StringUtil.isEmptyOrSpaces(path)) {
        File file = new File(toSystemDependentName(path));
        if (file.exists()) return file;
      }
    }

    return null;
  }

  @NotNull
  public static String expandUserHome(@NotNull String path) {
    if (path.startsWith("~/") || path.startsWith("~\\")) {
      path = SystemProperties.getUserHome() + path.substring(1);
    }
    return path;
  }

  @NotNull
  public static File createTempFile(File dir,
                                    @NotNull String prefix,
                                    @Nullable String suffix,
                                    boolean create,
                                    boolean deleteOnExit) throws IOException {
    return FileUtilRt.createTempFile(dir, prefix, suffix, create, deleteOnExit);
  }

  @NotNull
  public static String getTempDirectory() {
    return FileUtilRt.getTempDirectory();
  }

  @NotNull
  public static String loadFile(@NotNull File file) throws IOException {
    return FileUtilRt.loadFile(file);
  }

  @NotNull
  public static char[] loadText(@NotNull Reader reader, int length) throws IOException {
    return FileUtilRt.loadText(reader, length);
  }

  @NotNull
  public static List<String> loadLines(@NotNull String path) throws IOException {
    return FileUtilRt.loadLines(path);
  }


  @NotNull
  public static byte[] loadBytes(@NotNull InputStream stream) throws IOException {
    return FileUtilRt.loadBytes(stream);
  }

  @NotNull
  public static byte[] loadBytes(@NotNull InputStream stream, int length) throws IOException {
    return FileUtilRt.loadBytes(stream, length);
  }

  public static boolean deleteWithRenaming(File file) {
    File tempFileNameForDeletion = findSequentNonexistentFile(file.getParentFile(), file.getName(), "");
    boolean success = file.renameTo(tempFileNameForDeletion);
    return delete(success ? tempFileNameForDeletion:file);
  }
}
