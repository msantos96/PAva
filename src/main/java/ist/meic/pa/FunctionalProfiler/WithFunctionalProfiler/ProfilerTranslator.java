package ist.meic.pa.FunctionalProfiler.WithFunctionalProfiler;

import javassist.*;
import javassist.expr.*;

public class ProfilerTranslator implements Translator {
    private String mainClassName;
    public ProfilerTranslator(String mainClassName) throws NotFoundException {
        this.mainClassName = mainClassName;
    }
    
    public void start(ClassPool pool) throws NotFoundException, CannotCompileException {
        CtClass mainClass = pool.get(mainClassName);
        CtMethod main = mainClass.getDeclaredMethod("main");
        CtField ctField = CtField.make("public static ist.meic.pa.FunctionalProfiler.WithFunctionalProfiler.RWCounter __rwCounters = new ist.meic.pa.FunctionalProfiler.WithFunctionalProfiler.RWCounter();", mainClass);
        mainClass.addField(ctField);
        main.insertAfter(" { __rwCounters.printProfiles(); } ");
    }

    public void onLoad(ClassPool pool, String className) throws NotFoundException, CannotCompileException {
        CtClass ctClass = pool.get(className);
        try {
            if(className.equals("ist.meic.pa.FunctionalProfiler.WithFunctionalProfiler.RWCounter")) return;
            if(!ctClass.isInterface())  profile(ctClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void profile(CtClass ctClass) throws ClassNotFoundException, NotFoundException, CannotCompileException {
        CtField ctField = CtField.make("public static int[] __rwCounter = new int[2];", ctClass);
        ctClass.addField(ctField);

        for(CtConstructor ctConstructor : ctClass.getDeclaredConstructors()) {
            int[] __rwCounter = new int[2];
            ctConstructor.instrument(new ExprEditor() {
                public void edit(FieldAccess fa) throws CannotCompileException {
                    if (fa.isStatic()) return;
                    if (fa.isReader())  __rwCounter[0]++;
                    if (fa.isWriter())  __rwCounter[1]++;
                }
            });
            ctConstructor.insertAfter(" { __rwCounter[0] += " + __rwCounter[0] + "; __rwCounter[1] += " + __rwCounter[1] + "; } ");

            ctConstructor.insertAfter(" { " + mainClassName + ".__rwCounters.putIfAbsent(\"" + ctClass.getName() + "\", __rwCounter); } ");
        }

        for(CtMethod ctMethod : ctClass.getDeclaredMethods()) {
            int[] __rwCounter = new int[2];
            ctMethod.instrument(new ExprEditor() {
                public void edit(FieldAccess fa) throws CannotCompileException {
                    if (fa.isStatic()) return;
                    if (fa.isReader())  __rwCounter[0]++;
                    if (fa.isWriter())  __rwCounter[1]++;
                }
            });
            ctMethod.insertAfter(" { __rwCounter[0] += " + __rwCounter[0] + "; __rwCounter[1] += " + __rwCounter[1] + "; } ");
        }
    }
}
