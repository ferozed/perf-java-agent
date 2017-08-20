package com.zillow.zquick.agent;

import java.lang.instrument.*;
import java.util.concurrent.Callable;

/**
 * Created by ferozed on 8/18/17.
 */
public class ZquickAgent {

    private static Thread worker;


    public static void premain(String args, Instrumentation instrumentation)
    {
        System.out.println("premain : " + args);
        instrumentation.addTransformer(new ClassTransformer());

        Reloader r = new Reloader(instrumentation);

        worker = new Thread(r);
        worker.start();

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

        public Reloader(Instrumentation instrumentation) {
            this.instrumentation = instrumentation;
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
            String path = "zquick-example/target/zquick-example-0.0.1-master.605648b.jar";


            JarFileOps jarFileOps = new JarFileOps(path);
            jarFileOps.loadJar();

            while(true)
            {
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                jarFileOps.reloadClasses(instrumentation);

            }

        }
    }

}
