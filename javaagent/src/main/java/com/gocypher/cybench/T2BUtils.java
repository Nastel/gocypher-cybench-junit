package com.gocypher.cybench;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openjdk.jmh.generators.core.ClassInfo;
import org.openjdk.jmh.generators.core.FieldInfo;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.EnumMemberValue;

public final class T2BUtils {

    static final String NEW_CLASS_NAME_SUFIX = "_JMH_State";

    private T2BUtils() {
    }

    public static String getSysClassPath() throws Exception {
        ClassLoader classloader = ClassLoader.getSystemClassLoader();
        if (classloader != null) {
            URL[] urls;
            if (classloader instanceof URLClassLoader) {
                urls = ((URLClassLoader) classloader).getURLs();
            } else {
                Class<?> clCls = classloader.getClass();
                Field ucpField = clCls.getDeclaredField("ucp");
                ucpField.setAccessible(true);
                Object ucp = ucpField.get(classloader);
                Method getUrlsMethod = ucp.getClass().getDeclaredMethod("getURLs");
                getUrlsMethod.setAccessible(true);
                urls = (URL[]) getUrlsMethod.invoke(ucp);
            }
            String classPath = Stream.of(urls) //
                    .map(u -> u.getPath()) //
                    .map(s -> s.substring(1)) //
                    .collect(Collectors.joining(File.pathSeparator));

            return classPath;
        }
        return "";
    }

    public static String getSysPropClassPath() throws Exception {
        return System.getProperty("java.class.path");
    }

    public static String getCurrentClassPath() throws Exception {
        String cp = "";
        ClassLoader sys = ClassLoader.getSystemClassLoader();
        ClassLoader cl = T2BUtils.class.getClassLoader();
        boolean clEq = cl == sys;
        for (; cl != null; cl = cl.getParent()) {
            if (cl == sys && !clEq) {
                break;
            }

            URL[] urls;
            if (cl instanceof URLClassLoader) {
                urls = ((URLClassLoader) cl).getURLs();
            } else {
                Class<?> clCls = cl.getClass();
                Field ucpField = clCls.getDeclaredField("ucp");
                ucpField.setAccessible(true);
                Object ucp = ucpField.get(cl);
                Method getUrlsMethod = ucp.getClass().getDeclaredMethod("getURLs");
                getUrlsMethod.setAccessible(true);
                urls = (URL[]) getUrlsMethod.invoke(ucp);
            }

            cp += Stream.of(urls) //
                    .map(u -> u.getPath()) //
                    .map(s -> s.substring(1)) //
                    .collect(Collectors.joining(File.pathSeparator));

            if (cl == sys && clEq) {
                break;
            }
        }
        return cp;
    }

    public static void addClassPath(File classDir) throws Exception {
        ClassLoader classLoader = T2BUtils.class.getClassLoader();
        if (classLoader != null) {
            URL url = classDir.toURI().toURL();

            Class<?> clCls = classLoader.getClass();
            Field ucpField = clCls.getDeclaredField("ucp");
            ucpField.setAccessible(true);
            Object ucp = ucpField.get(classLoader);
            Method addURLMethod = ucp.getClass().getDeclaredMethod("addURL", URL.class);
            addURLMethod.setAccessible(true);
            addURLMethod.invoke(ucp, url);
        }
        String cpp = getSysPropClassPath();
        cpp += File.pathSeparator + classDir.getAbsolutePath();
        System.setProperty("java.class.path", cpp);
    }

    public static String getClassName(ClassInfo classInfo) {
        try {
            Field f = classInfo.getClass().getSuperclass().getDeclaredField("klass");
            f.setAccessible(true);
            Class<?> cls = (Class<?>) f.get(classInfo);
            return cls.getName();
        } catch (Throwable exc) {
            return classInfo.getQualifiedName();
        }
    }

    public static Collection<FieldInfo> getAllFields(ClassInfo ci) {
        Collection<FieldInfo> ls = new ArrayList<>();
        do {
            ls.addAll(ci.getFields());
        } while ((ci = ci.getSuperClass()) != null);
        return ls;
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

    public static String getStatedClassName(String className) {
        if (className.contains("$")) {
            String[] cnt = className.split("\\$");
            cnt[0] = cnt[0] + NEW_CLASS_NAME_SUFIX;
            return String.join("$", cnt);
        } else {
            return className + NEW_CLASS_NAME_SUFIX;
        }
    }

    public static Class<?> addAnnotation(String className, String annotationName, String typeName, String valueName,
            String classDir) throws Exception {
        ClassPool pool = ClassPool.getDefault();
        CtClass ctClass = pool.getAndRename(className, getStatedClassName(className));

        ClassFile classFile = ctClass.getClassFile();
        ConstPool constpool = classFile.getConstPool();

        AnnotationsAttribute annotationsAttribute = new AnnotationsAttribute(constpool,
                AnnotationsAttribute.visibleTag);
        javassist.bytecode.annotation.Annotation annotation = new javassist.bytecode.annotation.Annotation(
                annotationName, constpool);
        EnumMemberValue emv = new EnumMemberValue(constpool);
        emv.setType(typeName);
        emv.setValue(valueName);
        annotation.addMemberValue("value", emv);
        annotationsAttribute.setAnnotation(annotation);

        classFile.addAttribute(annotationsAttribute);
        ctClass.writeFile(new File(classDir).getCanonicalPath());
        return ctClass.toClass();
    }
}
