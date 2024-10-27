package io.github.ferozed.helloworld;

/**
 * Created by ferozed on 8/19/17.
 */
public class EntryPoint {

    public static void main(String [] args) {
        new JarFileTest().run("zquick-example/target/zquick-example-0.0.3-master.c334a8e.jar");
        HelloWorld h = new HelloWorld();
        while(true) {
            String ret = h.hello();
            System.out.printf("%s %s\n", System.currentTimeMillis(), ret);

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
