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
            if(ctClass.hasAnnotation(Skip.class)) return;
            profile(pool, ctClass, ctClass.getDeclaredConstructors());
            profile(pool, ctClass, ctClass.getDeclaredMethods());
            ctClass.writeFile("./theclasses");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    private void profile(ClassPool pool, CtClass ctClass, CtBehavior[] ctBehaviors) throws ClassNotFoundException, NotFoundException, CannotCompileException {
        String className = ctClass.getName();
        for(CtBehavior ctBehavior : ctBehaviors) {
            ctBehavior.instrument(new ExprEditor() {
                public void edit(FieldAccess fa) throws CannotCompileException {
                    if(fa.isStatic()) return;

                    String fieldClassName = fa.getClassName();
                    //9

                    if(fa.isReader()) {
                        fa.replace(String.format(" { $_ = $proceed($$); %s.__rwCounters.incRead(\"%s\"); } ", mainClassName, fieldClassName));
                    }
                    if(fa.isWriter()) {
                        if(ctBehavior instanceof CtConstructor && fieldClassName.equals(className)) {
                            //fa.replace(String.format(" { $_ = $proceed($$); if($0.%s != $_) { %s.__rwCounters.incWrite(\"%s\");} } ", fa.getFieldName(), mainClassName,  fieldClassName));
                            //fa.replace(String.format(" { $_ = $proceed($$); if($0.%s == $_) { %s.__rwCounters.incWrite(\"%s\");} } ", fa.getFieldName(), mainClassName, fieldClassName));
                            //fa.replace(String.format(" { $_ = $proceed($$); if($_ != null || $_ != $0.%s) { %s.__rwCounters.incWrite(\"%s\");} } ", fa.getFieldName(), mainClassName, fieldClassName));
                        }
                        else
                            fa.replace(String.format(" { $_ = $proceed($$); %s.__rwCounters.incWrite(\"%s\"); } ", mainClassName,  fieldClassName));
                    }
                }
            });

            if(ctBehavior instanceof CtConstructor)
                ctBehavior.insertBefore(String.format(" { %s.__rwCounters.putIfAbsent(\"%s\"); } ", mainClassName, className));
        }
    }
}