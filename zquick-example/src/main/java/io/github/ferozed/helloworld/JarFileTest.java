package io.github.ferozed.helloworld;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by ferozed on 8/19/17.
 */
public class JarFileTest {
    public void run(String path)
    {
        //String path = "zquick-example/target/zquick-example-0.0.1-master.605648b.jar";

        try {
            JarFile jf = new JarFile(new File(path));

            Enumeration<JarEntry> jen = jf.entries();

            while(jen.hasMoreElements())
            {
                JarEntry je = jen.nextElement();
                System.out.printf("%s %s\n", je.getName(), je.getSize());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
