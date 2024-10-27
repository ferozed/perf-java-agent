package io.github.ferozed.zquick.agent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.instrument.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;

/**
 * Created by ferozed on 8/18/17.
 */
public class ZquickAgent {
    private static final Log LOG = LogFactory.getLog(ZquickAgent.class);

    private static Thread [] workers;
    private static ClassTransformer classTransformer;


    public static void premain(String args, Instrumentation instrumentation)
    {
        LOG.info("premain : " + args);

        Properties properties = new Properties();

        String props_file_path = System.getProperty("zquick.properties");
        if (props_file_path != null) {
            props_file_path = props_file_path.replace("$HOME", System.getenv("HOME"));
            LOG.info("Reading properties from path: " + props_file_path);

            try (InputStream is = new FileInputStream(props_file_path)) {
                properties.load(is);
            } catch (IOException e) {

            }
        }

        String monitored_jars_path = properties.getProperty("jars_path");

        if (monitored_jars_path == null || monitored_jars_path.length() == 0)
        {
            // if jars_path is not in zquick.properties, or zquick.properties is not specified,
            // try to get it from the system properties of this jvm invocation
            monitored_jars_path = System.getProperty("jars_path");
        }

        List<String> monitored_jars = new ArrayList<String>();

        // jars_path is a properties file containing list of paths to  monitored jars, one per line.
        if (monitored_jars_path != null && monitored_jars_path.length() > 0)
        {
            File p = new File(monitored_jars_path);
            try(FileInputStream is = new FileInputStream(monitored_jars_path))
            {
                try(BufferedReader br = new BufferedReader(new InputStreamReader(is)))
                {
                    String line = br.readLine();
                    while (line != null)
                    {
                        if (line != null && line.length() > 0)
                        {
                            line = line.replace("$HOME", System.getenv("HOME"));
                            monitored_jars.add(line);
                        }

                        line = br.readLine();
                    }
                }
            } catch (IOException e) {

            }
        } else {
            LOG.error("System property -Djars_path is not specified. Nothing is being monitored!");
            return;
        }

        classTransformer = new ClassTransformer();
        instrumentation.addTransformer(classTransformer);

        if (monitored_jars.size() > 0) {
            workers = new Thread[monitored_jars.size()];
            for (int i = 0; i < monitored_jars.size(); i++) {
                Reloader r = new Reloader(instrumentation, monitored_jars.get(i), classTransformer);
                workers[i] = new Thread(r);
                workers[i].start();
            }
        }

        /*
        try {
            worker.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        */

    }

    public static class Reloader implements Runnable
    {
        private Instrumentation instrumentation;
        private String path;
        private ClassTransformer classTransformer;

        public Reloader(Instrumentation instrumentation, String path, ClassTransformer classTransformer) {

            this.instrumentation = instrumentation;
            this.path = path;
            this.classTransformer = classTransformer;
        }

        /**
         * When an object implementing interface <code>Runnable</code> is used
         * to create a thread, starting the thread causes the object's
         * <code>run</code> method to be called in that separately executing
         * thread.
         * <p>
         * The general contract of the method <code>run</code> is that it may
         * take any action whatsoever.
         *
         * @see Thread#run()
         */
        public void run() {
            //String path = "zquick-example/target/zquick-example-0.0.1-master.605648b.jar";
            //String path= System.getenv("HOME") + "/zstash/libs/countydirect-import-workflow/target/countydirect-import-workflow-dev-mode-SNAPSHOT.jar";


            JarFileOps jarFileOps = new JarFileOps(path, instrumentation, classTransformer);
            jarFileOps.loadJar();

            while(true)
            {
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                jarFileOps.reloadClasses();

            }

        }
    }

}
