package com.dataeye.monitor.utils;


import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassReflection {

    public static Set<Class<?>> getClassFromPackage(ClassLoader classLoader, String pack){

        Set<Class<?>> classes = new HashSet<>();

        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }

        String packageName = pack;
        String packagePath = packageName.replace(".", "/");
        try {
            Enumeration<URL> resources = classLoader.getResources(packagePath);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    String decode = URLDecoder.decode(url.getFile(), "UTF-8");
                    findClass(packageName, decode, classes, classLoader);
                } else if ("jar".equals(protocol)) {
                    JarURLConnection jarurlConn = (JarURLConnection) url.openConnection();
                    JarFile jarFile = jarurlConn.getJarFile();
                    Enumeration<JarEntry> jarEntries = jarFile.entries();
                    while (jarEntries.hasMoreElements()) {
                        JarEntry jarEntry = jarEntries.nextElement();
                        String jarEntryName = jarEntry.getName();
                        if (jarEntryName.endsWith(".class")) {
                            String className = jarEntryName.substring(0, jarEntryName.lastIndexOf(".")).replaceAll("/", ".");
                            try {
                                if (className.contains(packageName)) {
                                    classes.add(classLoader.loadClass(className));
                                }
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return classes;
    }

    private static void findClass(String packageName, String decode, Set<Class<?>> classes, ClassLoader classLoader) {

        File dir = new File(decode);

        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        File[] files = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return (pathname.isDirectory() || pathname.getName().endsWith(".class"));
            }
        });

        for (File file : files) {
            if (file.isDirectory()) {
                findClass(packageName + "." + file.getName(), file.getAbsolutePath(), classes, classLoader);
            } else {

                String className = file.getName().substring(0, file.getName().length() - 6);
                try {
                    classes.add(classLoader.loadClass(packageName + "." +className));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public static Set<Class<?>> getClassFilterByAnnotation(Class<? extends Annotation> clazz,String scanPackage) {
        Set<Class<?>> classes = getClassFromPackage(null, scanPackage);
        Set<Class<?>> ret = new HashSet<>();
        for (Class c : classes) {
            if (c.isAnnotationPresent(clazz)) {
                ret.add(c);
            }
        }
        return ret;
    }


    public static void setFiledValue(Field field, Object target, Object value){
        boolean acc = field.isAccessible();

        field.setAccessible(true);
        try {
            field.set(target, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } finally {
            field.setAccessible(acc);
        }
    }

    public static Set<Field> getFieldsByClassAndAnnotation(Class<?> clazz, Class<? extends Annotation> annotation){
        Set<Field> fields = new HashSet<>();

        Field[] declaredFields = clazz.getDeclaredFields();

        for (int i = 0; i < declaredFields.length; i++) {
            Field f = declaredFields[i];
            if (f.isAnnotationPresent(annotation)) {
                fields.add(f);
            }
        }
        return fields;
    }

}
