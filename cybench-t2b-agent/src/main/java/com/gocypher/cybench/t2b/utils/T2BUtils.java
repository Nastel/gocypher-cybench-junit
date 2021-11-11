/*
 * Copyright (C) 2020-2021, K2N.IO.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *  
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *  
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 */

package com.gocypher.cybench.t2b.utils;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class T2BUtils {

    private T2BUtils() {
    }

    public static String getSysClassPath() throws Exception {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        if (cl != null) {
            URL[] urls = getClassLoaderURLs(cl);
            String classPath = toString(urls);

            return classPath;
        }
        return "";
    }

    private static URL[] getClassLoaderURLs(ClassLoader cl) throws Exception {
        URL[] urls;
        if (cl instanceof URLClassLoader) {
            urls = ((URLClassLoader) cl).getURLs();
        } else {
            Object ucp = getFieldValue(cl, "ucp");
            urls = (URL[]) invokeMethod(ucp, "getURLs", null);
        }
        return urls;
    }

    private static String toString(URL[] urls) {
        return Stream.of(urls) //
                .map(u -> u.getPath()) //
                .map(s -> s.substring(1)) //
                .collect(Collectors.joining(File.pathSeparator));
    }

    public static Object getFieldValue(Object obj, String fieldName) throws Exception {
        Class<?> objCls = obj.getClass();
        Field objField = objCls.getDeclaredField(fieldName);
        objField.setAccessible(true);
        Object value = objField.get(obj);

        return value;
    }

    public static Object invokeMethod(Object obj, String methodName, Class<?>[] cls, Object... args)
            throws ReflectiveOperationException {
        Class<?> objCls = obj.getClass();
        Method objMethod = objCls.getDeclaredMethod(methodName, cls);
        objMethod.setAccessible(true);
        Object value = objMethod.invoke(obj, args);

        return value;
    }

    public static String getSysPropClassPath() throws Exception {
        return System.getProperty("java.class.path");
    }

    public static String getCurrentClassPath() throws Exception {
        StringBuilder cp = new StringBuilder();
        ClassLoader sys = ClassLoader.getSystemClassLoader();
        ClassLoader cl = T2BUtils.class.getClassLoader();
        boolean clEq = cl == sys;
        for (; cl != null; cl = cl.getParent()) {
            if (cl == sys && !clEq) {
                break;
            }

            URL[] urls = getClassLoaderURLs(cl);
            cp.append(toString(urls));

            if (cl == sys && clEq) {
                break;
            }
        }
        return cp.toString();
    }

    public static void addClassPath(File classDir) throws Exception {
        ClassLoader classLoader = T2BUtils.class.getClassLoader();
        if (classLoader != null) {
            URL url = classDir.toURI().toURL();

            Object ucp = getFieldValue(classLoader, "ucp");
            invokeMethod(ucp, "addURL", new Class[] { URL.class }, url);
        }
        String cpp = getSysPropClassPath();
        cpp += File.pathSeparator + classDir.getAbsolutePath();
        System.setProperty("java.class.path", cpp);
    }

    public static Collection<File> getUTClasses(File dir) {
        Set<File> fileTree = new HashSet<>();
        if (dir == null || dir.listFiles() == null) {
            return fileTree;
        }
        for (File entry : dir.listFiles()) {
            if (entry.isFile()) {
                if (entry.getName().endsWith(".class")) {
                    fileTree.add(entry);
                }
            } else {
                fileTree.addAll(getUTClasses(entry));
            }
        }
        return fileTree;
    }
}
