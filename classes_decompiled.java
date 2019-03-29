//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package tests;

import ist.meic.pa.FunctionalProfiler.WithFunctionalProfiler.RWCounter;
import ist.meic.pa.FunctionalProfilerExtended.Skip;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import tests.sampleC.Example;

public class Tests {
    public static RWCounter __rwCounters = new RWCounter();

    public Tests() {
        Object var2 = null;
        __rwCounters.putIfAbsent("tests.Tests", new int[2]);
    }

    @Skip
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: Tests test1 ... testn");
            System.exit(1);
        }

        String[] var4 = args;
        int var3 = args.length;

        for(int var2 = 0; var2 < var3; ++var2) {
            String test = var4[var2];

            try {
                Method m = Tests.class.getDeclaredMethod(test);
                m.invoke((Object)null);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException var8) {
                System.out.println("Test method does not exist: " + test);
                System.out.println(var8.getStackTrace());
            }
        }

        Object var7 = null;
        __rwCounters.printProfiles();
    }

    static void test1() {
        new Person();
    }

    static void test2() {
        (new Person()).selfIntroduce();
    }

    static void test3() {
        (new Person()).switchName("Foo");
    }

    static void test4() {
        Person var0;
        Person var10000 = var0 = new Person();
        String var1 = null;
        var1 = var0.firstname;
        __rwCounters.incRead("tests.Person");
        var1 = var1 + "Bar";
        var0 = var10000;
        Object var2 = null;
        var0.firstname = var1;
        var2 = null;
        __rwCounters.incWrite("tests.Person");
    }

    static void test5() {
        Person p = new Person();

        for(int i = 0; i < 5; ++i) {
            p.celebrateBirthday();
        }

        while(true) {
            boolean var3 = false;
            int var4 = p.age;
            __rwCounters.incRead("tests.Person");
            if (var4 >= 70) {
                return;
            }

            p.celebrateBirthday();
        }
    }

    static void test6() {
        new Person("Foo", "Bar");
    }

    static void test7() {
        Person var1 = new Person("Foo", "Bar");
        String var2 = null;
        var2 = var1.firstname;
        __rwCounters.incRead("tests.Person");
        StringBuilder var10000 = new StringBuilder(String.valueOf(var2));
        var1 = new Person("Foo", "Bar");
        var2 = null;
        var2 = var1.surname;
        __rwCounters.incRead("tests.Person");
        String s = var10000.append(var2).append("new Person(\"Foo\", \"Bar\").surname").toString();
    }

    static void test8() {
        new Person(new Person());
    }

    static void test9() {
        new Person(new Student("Harry", "Potter"));
    }

    static void test10() {
        new Student();
    }

    static void test11() {
        Student t = new Student();
        (new Professor()).grade(t);
        (new Professor()).grade(t);
    }

    static void test12() {
        Professor professor = new Professor();
        Student student = new Student();
        Person person = new Person();
        String var4 = "Prof";
        Object var5 = null;
        professor.firstname = var4;
        var5 = null;
        __rwCounters.incWrite("tests.Professor");
        var4 = "Student";
        var5 = null;
        student.firstname = var4;
        var5 = null;
        __rwCounters.incWrite("tests.Student");
        var4 = "Person";
        var5 = null;
        person.firstname = var4;
        var5 = null;
        __rwCounters.incWrite("tests.Person");
    }

    static void test13() {
        Professor p = new Professor();
        p.grade(new StudentPAva());
    }

    static void test14() {
        Example.test();
        tests.sampleA.Example.test();
        tests.sample0.Example.test();
    }

    static void test15() {
        new Car(10.0F);
    }

    static void test16() {
        new Car();
    }

    static void test17() {
        try {
            Throwable var0 = null;
            Object var1 = null;

            try {
                MyFakeFileWriter in = new MyFakeFileWriter("WingardiumLeviosa_ForBeginners.pdf");

                try {
                    in.readLine();
                } finally {
                    if (in != null) {
                        in.close();
                    }

                }
            } catch (Throwable var10) {
                if (var0 == null) {
                    var0 = var10;
                } else if (var0 != var10) {
                    var0.addSuppressed(var10);
                }

                throw var0;
            }
        } catch (IOException var11) {
            ;
        }

    }

    static void test18() {
        for(int i = 1; i < 18; ++i) {
            try {
                Method m = Tests.class.getDeclaredMethod("test" + i);
                m.invoke((Object)null);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException var2) {
                System.out.println("Test method does not exist: test" + i);
            }
        }

    }
}



//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package tests;

class Person {
    String firstname;
    String surname;
    int age;

    Person() {
        Object var2 = null;
        Tests.__rwCounters.putIfAbsent("tests.Person", new int[2]);
    }

    Person(String firstname, String lastname) {
        this.firstname = firstname;
        String var4 = null;
        var4 = this.surname;
        Tests.__rwCounters.incRead("tests.Person");
        this.surname = var4 + lastname;
        Object var6 = null;
        Tests.__rwCounters.putIfAbsent("tests.Person", new int[2]);
    }

    Person(Person p) {
        String var3 = null;
        var3 = p.firstname;
        Tests.__rwCounters.incRead("tests.Person");
        this.firstname = var3;
        p.surname = "";
        var3 = null;
        var3 = p.surname;
        Tests.__rwCounters.incRead("tests.Person");
        this.surname = var3;
        Object var5 = null;
        Tests.__rwCounters.putIfAbsent("tests.Person", new int[2]);
    }

    Person(Student t) {
        String var3 = null;
        var3 = t.firstname;
        Tests.__rwCounters.incRead("tests.Student");
        this.firstname = var3;
        var3 = "Ron";
        Object var4 = null;
        t.firstname = var3;
        var4 = null;
        Tests.__rwCounters.incWrite("tests.Student");
        var3 = null;
        var3 = t.surname;
        Tests.__rwCounters.incRead("tests.Student");
        this.surname = var3;
        boolean var7 = false;
        int var8 = this.age;
        Tests.__rwCounters.incRead("tests.Person");
        var8 += 20;
        var4 = null;
        t.age = var8;
        var4 = null;
        Tests.__rwCounters.incWrite("tests.Student");
        Object var6 = null;
        Tests.__rwCounters.putIfAbsent("tests.Person", new int[2]);
    }

    String selfIntroduce() {
        String var2 = null;
        var2 = this.firstname;
        Tests.__rwCounters.incRead("tests.Person");
        StringBuilder var10000 = (new StringBuilder(String.valueOf(var2))).append(" ");
        var2 = null;
        var2 = this.surname;
        Tests.__rwCounters.incRead("tests.Person");
        return var10000.append(var2).toString();
    }

    void switchName(String name) {
        Object var4 = null;
        this.firstname = name;
        var4 = null;
        Tests.__rwCounters.incWrite("tests.Person");
    }

    void celebrateBirthday() {
        boolean var2 = false;
        int var4 = this.age;
        Tests.__rwCounters.incRead("tests.Person");
        ++var4;
        Object var3 = null;
        this.age = var4;
        var3 = null;
        Tests.__rwCounters.incWrite("tests.Person");
    }
}
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package tests;

class Student extends Person {
    int mark;
    Professor[] favoriteProfessors;

    Student() {
        this.favoriteProfessors = new Professor[5];
        Object var2 = null;
        Tests.__rwCounters.putIfAbsent("tests.Student", new int[2]);
    }

    Student(String firstname, String lastname) {
        this.firstname = firstname;
        this.surname = lastname;
        Object var4 = null;
        Tests.__rwCounters.putIfAbsent("tests.Student", new int[2]);
    }

    Student(Student other) {
        this.firstname = "Foo";
        other.firstname = "Foo";
        this.surname = "Bar";
        other.age = 0;
        other.favoriteProfessors = new Professor[5];
        Object var4 = null;
        Tests.__rwCounters.putIfAbsent("tests.Student", new int[2]);
    }

    void addFavorite(Professor p) {
        Professor[] var4 = null;
        var4 = this.favoriteProfessors;
        Tests.__rwCounters.incRead("tests.Student");
        if (var4 != null) {
            int i;
            for(i = 0; i < 5; ++i) {
                var4 = null;
                var4 = this.favoriteProfessors;
                Tests.__rwCounters.incRead("tests.Student");
                if (var4[i] == null) {
                    break;
                }
            }

            if (i < 5) {
                var4 = null;
                var4 = this.favoriteProfessors;
                Tests.__rwCounters.incRead("tests.Student");
                var4[i] = p;
            }
        }

    }
}
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package tests;

class StudentPAva extends Student {
    boolean pass = false;

    StudentPAva() {
        Object var2 = null;
        Tests.__rwCounters.putIfAbsent("tests.StudentPAva", new int[2]);
    }
}
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package tests;

class Professor extends Person {
    Professor() {
        Object var2 = null;
        Tests.__rwCounters.putIfAbsent("tests.Professor", new int[2]);
    }

    void grade(Student t) {
        byte var3 = 20;
        Object var4 = null;
        t.mark = var3;
        var4 = null;
        Tests.__rwCounters.incWrite("tests.Student");
        t.addFavorite(this);
    }

    void grade(StudentPAva tp) {
        byte var3 = 20;
        Object var4 = null;
        tp.mark = var3;
        var4 = null;
        Tests.__rwCounters.incWrite("tests.StudentPAva");
        tp.addFavorite(this);
        boolean var5 = true;
        var4 = null;
        tp.pass = var5;
        var4 = null;
        Tests.__rwCounters.incWrite("tests.StudentPAva");
    }
}
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package tests;

class Car {
    int maxSpeed;
    float fuelCapacity;

    Car(float fuelCapacity) {
        this.maxSpeed = 15;
        float var3 = 0.0F;
        var3 = this.fuelCapacity;
        Tests.__rwCounters.incRead("tests.Car");
        this.fuelCapacity = var3 + fuelCapacity;
        this.raiseMaxSpeed(200);
        Object var5 = null;
        Tests.__rwCounters.putIfAbsent("tests.Car", new int[2]);
    }

    Car() {
        this.maxSpeed = 20;
        this.fuelCapacity = 6.0F;
        (new Car.Motor(this)).turnOn();
        Object var2 = null;
        Tests.__rwCounters.putIfAbsent("tests.Car", new int[2]);
    }

    void raiseMaxSpeed(int maxSpeed) {
        Object var4 = null;
        this.maxSpeed = maxSpeed;
        var4 = null;
        Tests.__rwCounters.incWrite("tests.Car");
    }

    static class Motor {
        boolean on = false;
        int maxSpeed;

        Motor(Car c) {
            boolean var3 = false;
            int var7 = c.maxSpeed;
            Tests.__rwCounters.incRead("tests.Car");
            this.maxSpeed = var7;
            short var8 = 200;
            Object var4 = null;
            c.maxSpeed = var8;
            var4 = null;
            Tests.__rwCounters.incWrite("tests.Car");
            Object var6 = null;
            Tests.__rwCounters.putIfAbsent("tests.Car$Motor", new int[2]);
        }

        private void turnOn() {
            boolean var2 = true;
            Object var3 = null;
            this.on = var2;
            var3 = null;
            Tests.__rwCounters.incWrite("tests.Car$Motor");
            var2 = false;
            int var4 = this.maxSpeed;
            Tests.__rwCounters.incRead("tests.Car$Motor");
            var4 *= 2;
            var3 = null;
            this.maxSpeed = var4;
            var3 = null;
            Tests.__rwCounters.incWrite("tests.Car$Motor");
        }
    }
}
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package tests;

import java.io.IOException;

class MyFakeFileWriter implements AutoCloseable {
    String in;

    MyFakeFileWriter(String filename) {
        String var4 = null;
        var4 = this.in;
        Tests.__rwCounters.incRead("tests.MyFakeFileWriter");
        this.in = var4 + filename;
        String s = "Opening file this.filename";
        Object var6 = null;
        Tests.__rwCounters.putIfAbsent("tests.MyFakeFileWriter", new int[2]);
    }

    String readLine() {
        return "The first and foremost important aspect of Wingardium Leviosa is to learn the proper accent! Try with me: *wingaaardiuum* *lÃ©Ã©viosa*.";
    }

    public void close() throws IOException {
        String var2 = "";
        Object var3 = null;
        this.in = var2;
        var3 = null;
        Tests.__rwCounters.incWrite("tests.MyFakeFileWriter");
    }
}
