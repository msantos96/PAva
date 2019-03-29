package ist.meic.pa.FunctionalProfiler.WithFunctionalProfiler;

import javassist.*;
import javassist.expr.*;
import java.util.HashMap;
import ist.meic.pa.FunctionalProfilerExtended.Skip;

public class ProfilerTranslator implements Translator {

	static final boolean debug = true;

	private String mainClassName;

	public ProfilerTranslator(String mainClassName) throws NotFoundException {
		this.mainClassName = mainClassName;
	}

	public void start(ClassPool pool) throws NotFoundException, CannotCompileException {
		CtClass mainClass = pool.get(mainClassName);
		mainClass.addField(CtField.make(
				"public static ist.meic.pa.FunctionalProfiler.WithFunctionalProfiler.RWCounter __rwCounters = new ist.meic.pa.FunctionalProfiler.WithFunctionalProfiler.RWCounter();",
				mainClass));
		mainClass.getDeclaredMethod("main").insertAfter(" { __rwCounters.printProfiles(); } ");
	}

	public void onLoad(ClassPool pool, String className) throws NotFoundException, CannotCompileException {
		CtClass ctClass = pool.get(className);
		try {
			if (ctClass.hasAnnotation(Skip.class))
				return;
			profile(pool, ctClass, ctClass.getDeclaredConstructors());
			profile(pool, ctClass, ctClass.getDeclaredMethods());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private void profile(ClassPool pool, CtClass ctClass, CtBehavior[] ctBehaviors)
			throws ClassNotFoundException, NotFoundException, CannotCompileException {
		String className = ctClass.getName();
		String className_formated = className.replaceAll("[./$]", "_");
		if (debug) {
			System.out.println(className_formated);
		}
		for (CtBehavior ctBehavior : ctBehaviors) {
			if (ctBehavior.hasAnnotation(Skip.class))
				continue;
			ctBehavior.instrument(new ExprEditor() {
				public void edit(FieldAccess fa) throws CannotCompileException {
					if (fa.isStatic())
						return;

					String cName = fa.getClassName();

					String cName_formated = className.replaceAll("[./$]", "_");
					if (debug) {
						System.out.println(cName_formated);
					}

					try {
						pool.get(cName).getField(cName_formated + "__rwCounter");
					} catch (NotFoundException e) {
						try {
							pool.get(cName)
									.addField(CtField.make(
											"public static int[] " + cName_formated + "__rwCounter = new int[2];",
											pool.get(cName)));
						} catch (NotFoundException e1) {
						}
					}

					if (debug)
						System.out.println("classname :" + className);
					// não conta reads/writes no construtor de variaveis do objecto.
					if (ctBehavior instanceof CtConstructor && cName.equals(className)) {
						if (fa.isReader()) {
							if (debug) {
								System.out.println("maybe read at: " + fa.getLineNumber() + " " + fa.getClassName()
										+ "." + fa.getFieldName());
								fa.replace(String.format(
										"{ $_ = $0.%s; %s.%s__rwCounter[0] += (($0!=null)? 1 : 0); System.out.println(\"1_\"+%s.%s__rwCounter[0]+\"_\"+\"%s\"+($0!=null)+$0); }",
										fa.getFieldName(), cName, cName_formated, cName, cName_formated, cName));
							} else {
								fa.replace(String.format("{ $_ = $0.%s; %s.%s__rwCounter[0] += (($0!=null)? 1 : 0);}",
										fa.getFieldName(), cName, cName_formated));
							}
						}
						if (fa.isWriter()) {
							if (debug) {
								System.out.println("maybe write at: " + fa.getLineNumber() + " " + fa.getClassName()
										+ "." + fa.getFieldName());
								fa.replace(String.format(
										"{ $0.%s = $1; %s.%s__rwCounter[1] += (($0!=this)? 1 : 0); System.out.println(\"2_\"+%s.%s__rwCounter[1]+\"_\"+\"%s\"+($0!=this)+$0);}",
										fa.getFieldName(), cName, cName_formated, cName, cName_formated, cName));
							} else {
								fa.replace(String.format("{ $0.%s = $1; %s.%s__rwCounter[1] += (($0!=this)? 1 : 0);}",
										fa.getFieldName(), cName, cName_formated));
							}
						}
						return;
					}
					if (fa.isReader()) {
						if (debug) {
							System.out.println("read at: " + fa.getLineNumber() + " " + fa.getClassName() + "."
									+ fa.getFieldName());
							fa.replace(String.format(
									"{ $_ = $0.%s; %s.%s__rwCounter[0] += 1; System.out.println(\"3_\"+%s.%s__rwCounter[0]+\"_\"+\"%s\"+$0);}",
									fa.getFieldName(), cName, cName_formated, cName, cName_formated, cName));
						} else {
							fa.replace(String.format("{ $_ = $0.%s; %s.%s__rwCounter[0] += 1;}", fa.getFieldName(),
									cName, cName_formated));
						}
					}
					if (fa.isWriter()) {
						if (debug) {
							System.out.println("write at: " + fa.getLineNumber() + " " + fa.getClassName() + "."
									+ fa.getFieldName());
							fa.replace(String.format(
									"{ $0.%s = $1; %s.%s__rwCounter[1] += 1; System.out.println(\"4_\"+%s.%s__rwCounter[1]+\"_\"+\"%s\"+$0);}",
									fa.getFieldName(), cName, cName_formated, cName, cName_formated, cName));
						}else{
							fa.replace(String.format("{ $0.%s = $1; %s.%s__rwCounter[1] += 1;}", fa.getFieldName(),
									cName, cName_formated));
						}
					}
				}
			});
			if (ctBehavior instanceof CtConstructor) {
				try {
					ctClass.getField(className_formated + "__rwCounter");
				} catch (NotFoundException e) {
					ctClass.addField(CtField
							.make("public static int[] " + className_formated + "__rwCounter = new int[2];", ctClass));
				}
				ctBehavior.insertAfter(" { " + mainClassName + ".__rwCounters.putIfAbsent(\"" + className + "\", "
						+ className_formated + "__rwCounter); } ");
			}
		}
	}
}