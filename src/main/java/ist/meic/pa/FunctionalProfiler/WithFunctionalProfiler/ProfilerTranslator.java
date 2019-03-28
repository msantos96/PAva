package ist.meic.pa.FunctionalProfiler.WithFunctionalProfiler;

import javassist.*;
import javassist.expr.*;
import java.util.HashMap;
import ist.meic.pa.FunctionalProfilerExtended.Skip;

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
            if(ctClass.hasAnnotation​(Skip.class)) return;
            profile(pool, ctClass, ctClass.getDeclaredConstructors());
            profile(pool, ctClass, ctClass.getDeclaredMethods());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void profile(ClassPool pool, CtClass ctClass, CtBehavior[] ctBehaviors) throws ClassNotFoundException, NotFoundException, CannotCompileException {
        String className = ctClass.getName();
        for(CtBehavior ctBehavior : ctBehaviors) {
            ctBehavior.instrument(new ExprEditor() {
                public void edit(FieldAccess fa) throws CannotCompileException {
                    if(fa.isStatic()) return;

                    String cName = fa.getClassName();
                    //não conta reads/writes no construtor de variaveis do objecto.
                    if(ctBehavior instanceof CtConstructor && cName.equals(className)) return;
                    
                    try {
                        pool.get(cName).getField("__rwCounter");
                    } catch(NotFoundException e) {
                        try {
                            pool.get(cName).addField(CtField.make("public static int[] __rwCounter = new int[2];", pool.get(cName)));                            
                        } catch(NotFoundException e1) {}
                    }

                    if(fa.isWriter())   fa.replace(String.format("{ $0.%s = $1; %s.__rwCounter[1] += 1; }", fa.getFieldName(), cName));
                    if(fa.isReader())   fa.replace(String.format("{ $_ = $0.%s; %s.__rwCounter[0] += 1; }", fa.getFieldName(), cName));
                }
            });
            if(ctBehavior instanceof CtConstructor) {
                try {
                    ctClass.getField("__rwCounter");
                } catch(NotFoundException e) {
                    ctClass.addField(CtField.make("public static int[] __rwCounter = new int[2];", ctClass));
                }
                ctBehavior.insertAfter(" { " + mainClassName + ".__rwCounters.putIfAbsent(\"" + className + "\", __rwCounter); } ");
            }
        }
    }
}