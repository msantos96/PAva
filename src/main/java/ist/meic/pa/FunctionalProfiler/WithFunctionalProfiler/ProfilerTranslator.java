package ist.meic.pa.FunctionalProfiler.WithFunctionalProfiler;

import javassist.*;
import javassist.expr.*;
import ist.meic.pa.FunctionalProfiler.WithFunctionalProfiler.Skip;

public class ProfilerTranslator implements Translator {
    private String mainClassName;
    public ProfilerTranslator(String mainClassName) {
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
            if(ctClass.isInterface()) return;
            if(ctClass.hasAnnotation(Skip.class)) return;
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

                    String fieldClassName = fa.getClassName();
                    //6 - 7
                    CtClass fieldCtClass = get(pool, fieldClassName);
                    
                    if(fa.isReader())
                        fa.replace(String.format(" { $_ = $proceed($$); %s.__rwCounters.incRead(\"%s\"); } ", mainClassName, fieldClassName));
                    if(fa.isWriter() && ctBehavior instanceof CtConstructor && !fieldClassName.equals(className))
                        fa.replace(String.format(" { $_ = $proceed($$); %s.__rwCounters.incWrite(\"%s\"); } ", mainClassName,  fieldClassName));
                    if(fa.isWriter() && ctBehavior instanceof CtMethod)
                        fa.replace(String.format(" { $_ = $proceed($$); %s.__rwCounters.incWrite(\"%s\"); } ", mainClassName,  fieldClassName));
                    
                }
            });

            if(ctBehavior instanceof CtConstructor)
                ctBehavior.insertAfter(String.format(" { %s.__rwCounters.putIfAbsent(\"%s\", new int[2]); } ", mainClassName, className));
        }
    }

    /*private static void addIfNotPresent(CtClass ctClass, String fieldName) throws CannotCompileException {
        if(!java.util.Arrays.stream(ctClass.getDeclaredFields()).anyMatch(f -> f.getName().equals(fieldName)))
            ctClass.addField(CtField.make("public static int[] __rwCounter = new int[2];", ctClass));
    }

    */private CtClass get(ClassPool pool, String className) {
        try {
            return pool.get(className);
        } catch(NotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}