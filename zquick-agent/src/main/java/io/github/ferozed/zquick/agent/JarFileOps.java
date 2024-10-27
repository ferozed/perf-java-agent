package io.github.ferozed.zquick.agent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Created by ferozed on 8/19/17.
 */
public class JarFileOps
{
    private static final Log LOG = LogFactory.getLog(JarFileOps.class);

    private String path;
    private Instrumentation instrumentation;
    private ClassTransformer classTransformer;

    private HashMap<String, JarEntry> jarDict = new HashMap<String, JarEntry>();

    public JarFileOps(String path, Instrumentation instrumentation, ClassTransformer classTransformer)
    {
        this.instrumentation = instrumentation;
        this.path = path;
        this.classTransformer = classTransformer;
    }

    public void loadJar()
    {
        //String path = "zquick-example/target/zquick-example-0.0.1-master.605648b.jar";

        try {
            JarFile jf = new JarFile(new File(path));

            instrumentation.appendToSystemClassLoaderSearch(jf);

            Enumeration<JarEntry> jen = jf.entries();

            while(jen.hasMoreElements())
            {
                JarEntry je = jen.nextElement();
                if (LOG.isDebugEnabled())
                    LOG.debug(String.format("%s %s\n", je.getName(), je.getSize()));

                if (!je.getName().endsWith(".class"))
                {
                    continue;
                }

                jarDict.put(je.getName(), je);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reloadClasses()
    {
        LOG.info("reloadClasses() " + path);

        try(JarFile jf = new JarFile(new File(path)))
        {
            Enumeration<JarEntry> jen = jf.entries();

            while(jen.hasMoreElements())
            {
                JarEntry je = jen.nextElement();

                if (!je.getName().endsWith(".class"))
                {
                    continue;
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("test for reload: %s %s\n", je.getName(), je.getSize()));
                }

                JarEntry old = jarDict.get(je.getName());

                boolean reload = false;
                if (old == null || old.getCrc() != je.getCrc())
                {
                    reload = true;
                }

                // replace
                jarDict.put(je.getName(), je);

                if (reload)
                {
                    String className = je.getName().replace("/", ".");
                    int i = className.lastIndexOf(".");
                    className = className.substring(0, i);

                    LOG.info(String.format("do reload: %s %s\n", je.getName(), className));

                    ZipEntry ze = jf.getEntry(je.getName());

                    byte [] classBytes = null;

                    byte [] buffer = new byte[4096];
                    try(ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                        try (InputStream is = jf.getInputStream(ze)) {
                            int read = is.read(buffer, 0, buffer.length);
                            while (read > 0) {
                                bos.write(buffer, 0, read);
                                read = is.read(buffer, 0, buffer.length);
                            }
                        }

                        classBytes = bos.toByteArray();
                    }

                    if (classBytes == null)
                        throw new NullPointerException("classBytes");

                    Class clazz = classTransformer.getClassByName(className);

                    if (clazz == null)
                        throw new NullPointerException("clazz");

                    try {
                        ClassDefinition cd = new ClassDefinition(clazz, classBytes);
                        instrumentation.redefineClasses(cd);
                    } catch (ClassNotFoundException e) {
                        System.err.println("Class not found for reload! " + className + "\n");
                        LOG.error("Exception loading: " + className, e);
                    } catch (UnmodifiableClassException e) {
                        LOG.error("Exception loading: " + className, e);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
