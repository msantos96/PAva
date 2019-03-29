package ist.meic.pa.FunctionalProfiler.WithFunctionalProfiler;

import javassist.*;
import javassist.expr.*;

import java.io.IOException;

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
            profile(pool, ctClass, ctClass.getDeclaredConstructors());
            profile(pool, ctClass, ctClass.getDeclaredMethods());
            ctClass.writeFile("./theclasses");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
			e.printStackTrace();
		}
    }

    private void profile(ClassPool pool, CtClass ctClass, CtBehavior[] ctBehaviors) throws ClassNotFoundException, NotFoundException, CannotCompileException {
        String className = ctClass.getName();
        if(ctClass.hasAnnotation(Skip.class)) return;
        for(CtBehavior ctBehavior : ctBehaviors) {
        	if(ctBehavior.hasAnnotation(Skip.class)) continue;
            ctBehavior.instrument(new ExprEditor() {
                public void edit(FieldAccess fa) throws CannotCompileException {

                    String fieldClassName = fa.getClassName();
                    
                    if(fa.isStatic()) return;
                    

                    if(fa.isReader()) {
                        	fa.replace(String.format(" { $_ = $proceed($$); %s.__rwCounters.incRead($0.getClass().getName()); } ", mainClassName));
                    }
                    if(fa.isWriter()) {
                        if(ctBehavior instanceof CtConstructor && fieldClassName.equals(className)) {
                            fa.replace(String.format(" { $_ = $proceed($$); if($0 != this) { %s.__rwCounters.incWrite($0.getClass().getName());} } ", mainClassName));
                        }
                        else
                            fa.replace(String.format(" { $_ = $proceed($$); %s.__rwCounters.incWrite($0.getClass().getName()); } ", mainClassName));
                    }
                }
            });

            if(ctBehavior instanceof CtConstructor) {
            	ctBehavior.insertBefore(String.format(" { %s.__rwCounters.putIfAbsent(\"%s\"); } ", mainClassName, className));
            }
        }
    }
}