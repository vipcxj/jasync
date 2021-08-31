package io.github.vipcxj.jasync.test;

import io.github.vipcxj.jasync.reactive.Promises;
import io.github.vipcxj.jasync.spec.JAsync;
import io.github.vipcxj.jasync.spec.Promise;
import io.github.vipcxj.jasync.spec.annotations.Async;
import reactor.core.publisher.Mono;

import java.io.StringWriter;
import java.time.Duration;

public class Apple {

    private Promise<String> say(String message) {
        return JAsync.just(message);
    }

    public Promise<Boolean> test1() {
        Integer a = Promises.from(Mono.just(6).delayElement(Duration.ofSeconds(1))).await();
        if (a > 3) {
            try {
                System.out.println(say("hello").await());
            } catch (Throwable t) {
                return JAsync.error(t);
            } finally {
                System.out.println(say("finally").await());
            }
            return JAsync.just(true);
        } else if (a == 2) {
            say("a = 2").await();
        } else {
            say("else").await();
        }
        return JAsync.just(false);
    }

    public static String getLineInfo() {
        StackTraceElement ste = new Throwable().getStackTrace()[1];
        return ste.getFileName() + ": Line " + ste.getLineNumber();
    }

    public Promise<Integer> test2(int a) {
        int i = 0, j, o;
        Double k;
        j = 1;
        ++a;
        o = 2;
        Double p = (double) o;
        say("something.").await();
        k = 3.0;
        --a;
        System.out.println(p);
        o = 1;
        return JAsync.just(a + i + j + k.intValue());
    }

    private static final String ABC = "abc";


    public Promise<Void> testMultiLevelScopeVar() {
        String a = "!";
        {
            System.out.println(say(a).await());
            {
                a = "3";
                System.out.println(say(a).await());
            }
        }
        return null;
    }

    @Async
    public Promise<Void> testSwitch() {
        String s = "a";
/*        try {
            say(s).await();
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("1");
        } finally {
            s = "B";
        }*/
        switch (s) {
            case "a" + "b":
                return null;
            case ABC:
                s = "c";
                return null;
        }
        int a = 1;
        switch (a) {
            case 1:
                String message = "1";
                say(message).await();
            case 2 + 4:
            case 3:
                say("2 or 3").await();
                break;
            case 4: {
                message = "2";
                say(message + "2").await();
            }
            case 5: {
                message = "4";
            }
            default:
                int b = 3;
                say("" + a + b + say("abc" + s).await()).await();
        }
        return null;
    }

    private Promise<StringWriter> getWriter() {
        return JAsync.just(new StringWriter());
    }

    public Promise<Void> testTryWith() {
        try (
                StringWriter writer1 = getWriter().await();
                StringWriter writer2 = new StringWriter();
                StringWriter writer3 = getWriter().await();
        ){
            writer1.append('1');
            writer2.append('2');
            writer3.append(say("3").await());
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            System.out.println('3');
        }
        return JAsync.just();
    }

    public void test() {
        try (
                StringWriter writer1 = new StringWriter();
                StringWriter writer2 = new StringWriter();
                StringWriter writer3 = new StringWriter();
        ){
            writer1.append('1');
            writer2.append('2');
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            System.out.println('3');
        }
    }

    public static class B {

    }

    private String b;
}
