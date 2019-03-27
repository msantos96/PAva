package ist.meic.pa.FunctionalProfiler.WithFunctionalProfiler;

import javassist.*;
import javassist.expr.*;
import java.util.HashMap;

public class ProfilerTranslator implements Translator {
    private String mainClassName;
    public ProfilerTranslator(String mainClassName) throws NotFoundException {
        this.mainClassName = mainClassName;
    }
    
    public void start(ClassPool pool) throws NotFoundException, CannotCompileException {
        CtClass mainClass = pool.get(mainClassName);
        mainClass
            .addField(
                CtField.make("public static ist.meic.pa.FunctionalProfiler.WithFunctionalProfiler.RWCounter __rwCounters = new ist.meic.pa.FunctionalProfiler.WithFunctionalProfiler.RWCounter();"
                , mainClass));
        mainClass
            .getDeclaredMethod("main")
            .insertAfter(" { __rwCounters.printProfiles(); } ");
    }

    public void onLoad(ClassPool pool, String className) throws NotFoundException, CannotCompileException {
        CtClass ctClass = pool.get(className);
        try {
            if(className.startsWith("java")) return;
            if(className.equals(mainClassName)) return;
            if(className.equals("ist.meic.pa.FunctionalProfiler.WithFunctionalProfiler.RWCounter")) return;

            profile(pool, className, ctClass.getDeclaredConstructors());
            profile(pool, className, ctClass.getDeclaredMethods());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void profile(ClassPool pool, String className, CtBehavior[] ctBehaviors) throws ClassNotFoundException, NotFoundException, CannotCompileException {
        for(CtBehavior ctBehavior : ctBehaviors) {
            HashMap<String, int[]> __rwCounters = new HashMap<String, int[]>();
            ctBehavior.instrument(new ExprEditor() {
                public void edit(FieldAccess fa) throws CannotCompileException {
                    String cName = fa.getClassName();
                    int[] __rwCounter = __rwCounters.getOrDefault(cName, new int[2]);
                    __rwCounters.putIfAbsent(cName, __rwCounter);
                    if(ctBehavior instanceof CtConstructor && fa.getClassName().equals(className)) return;
                    if (fa.isStatic()) return;
                    if (fa.isReader())  __rwCounter[0]++;
                    if (fa.isWriter())  __rwCounter[1]++;
                }
            });
            
            for(String key : __rwCounters.keySet()) {
                if(!java.util.Arrays.stream(pool.get(key).getDeclaredFields()).map(f -> f.getName()).anyMatch(fName -> fName.equals("__rwCounter")))
                    pool.get(key).addField(CtField.make("public static int[] __rwCounter = new int[2];", pool.get(key)));
                
                ctBehavior.insertAfter(" { " + key + ".__rwCounter[0] += " + __rwCounters.get(key)[0] + "; " + key + ".__rwCounter[1] += " + __rwCounters.get(key)[1] + "; } ");

                if(ctBehavior instanceof CtConstructor)
                    ctBehavior.insertAfter(" { " + mainClassName + ".__rwCounters.putIfAbsent(\"" + className + "\", __rwCounter); } ");
            }
        }
    }
}