package ist.meic.pa.FunctionalProfiler.WithFunctionalProfiler;

import javassist.*;

public class App {
    public static void main(String[] args) throws Throwable {
        if(args.length < 2) {
            System.out.println("Usage: java <path> <class>");
            System.exit(1);
        } else {
            Translator translator = new ProfilerTranslator(args[1]);
            ClassPool pool = ClassPool.getDefault();
            pool.insertClassPath(args[0]);
            Loader classLoader = new Loader(pool);
            classLoader.addTranslator(pool, translator);
            String[] restArgs = new String[args.length - 2];
            System.arraycopy(args, 2, restArgs, 0, restArgs.length);
            classLoader.run(args[1], restArgs);
        }
    }
}
