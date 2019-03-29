CC = gradle build jar
OPT = -q

JAVAOPT =
# pasta onde estao guardados os testes
TD = tests_prof/src/
# executavel do programa
EXE = build/libs/functionalProfiler.jar
DEPENDENCIES = javassist.jar
CODE = src/main/java/*

RUN_GRADLE = gradle run --args="../Tests/build/libs/Tests.jar Tests.App" -q  #not finished
RUN_JAVA = java -cp "$(EXE);$(DEPENDENCIES)" ist.meic.pa.FunctionalProfiler.WithFunctionalProfiler.App


default: clean compile verify
	@echo ------------------------------------------
	@echo
	@echo Done
	
compile: $(CODE)
	@echo ------------------------------------------
	@echo
	$(CC)
	@echo
	@echo BUILD Finished
	@echo

clean:
	@echo ------------------------------------------
	@echo
	@echo Cleaning...
	gradle clean
	-rm -f $(EXE) *_output.out
	@echo

verify: verify_short

verify_short: test1 test2 test3 test4 test5 test6 test7 test8 test9 test10 test11 test12 test13 test14 test15 test16 test17 test18

verify_long: 


verify_all: verify_short verify_long
#java -cp "build/libs/functionalProfiler.jar;javassist.jar" ist.meic.pa.FunctionalProfiler.WithFunctionalProfiler.App Tests/build/libs/Tests.jar Tests.App
#test1 test2 test3 test4 test5 test6 test7 test8 test9 test10 test11 test12 test13 test14 test15 test16 test17 test18
#java -cp "build/libs/functionalProfiler.jar;javassist.jar" ist.meic.pa.FunctionalProfiler.WithFunctionalProfiler.App TestBattery.jar tests.Tests test2

run%:
	$(RUN_JAVA) $(JAVAOPT) TestBattery.jar tests.Tests test$*

test%: $(EXE)
	@echo 
	@echo $*
	@-rm -f $*_output.out
	@echo time
	@time $(RUN_JAVA) TestBattery.jar tests.Tests test$* > $*_output.out
	@echo 
	@echo $* Extr :
	@-diff --strip-trailing-cr $*_output.out $(TD)test$*.out | grep "^>" | wc -l
	@echo $* Miss :
	@-diff --strip-trailing-cr $*_output.out $(TD)test$*.out | grep "^<" | wc -l
	@echo side by side:
	@-diff --strip-trailing-cr --side-by-side $*_output.out $(TD)test$*.out
	@echo
	@echo
