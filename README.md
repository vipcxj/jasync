JAsync - the async-await pattern of Java
===============

[![Maven Release][maven-shield]][maven-link]

[中文版](/README_CN.md)

**JAsync** implements Async-Await pattern just like es in Java. 
It allows developers to write asynchronous code in a sequential fashion.
It makes the developer's asynchronous programming experience as close as possible to the usual synchronous programming, including code style and debugging.

On the other hand, this framework separates the realization of the upper-level syntax tree conversion and the lower-level asynchronous workflow through a set of interfaces. This project focuses on the former, while the latter can be achieved by encapsulating existing asynchronous libraries, such as Reactor, RxJava, etc.
The advantage of this is that this project can be seamlessly integrated with various asynchronous libraries that have been widely used in the community to produce an effect of 1+1>2.

Examples
=======
#### With JAsync
```java
@RestController
@RequestMapping("/employees")
public class MyRestController {
    @Inject
    private EmployeeRepository employeeRepository;
    @Inject
    private SalaryRepository salaryRepository;

    // The standard JAsync async method must be annotated with the Async annotation, and return a JPromise object.
    @Async()
    private JPromise<Double> _getEmployeeTotalSalaryByDepartment(String department) {
        double money = 0.0;
        // A Mono object can be transformed to the JPromise object. So we get a Mono object first.
        Mono<List<Employee>> empsMono = employeeRepository.findEmployeeByDepartment(department);
        // Transformed the Mono object to the JPromise object.
        JPromise<List<Employee>> empsPromise = Promises.from(empsMono);
        // Use await just like es and c# to get the value of the JPromise without blocking the current thread.
        for (Employee employee : empsPromise.await()) {
            // The method findSalaryByEmployee also return a Mono object. We transform it to the JPromise just like above. And then await to get the result.
            Salary salary = Promises.from(salaryRepository.findSalaryByEmployee(employee.id)).await();
            money += salary.total;
        }
        // The async method must return a JPromise object, so we use just method to wrap the result to a JPromise.
        return JAsync.just(money);
    }

    // This is a normal webflux method.
    @GetMapping("/{department}/salary")
    public Mono<Double> getEmployeeTotalSalaryByDepartment(@PathVariable String department) { 
        // Use unwrap method to transform the JPromise object back to the Mono object.
        return _getEmployeeTotalSalaryByDepartment(department).unwrap(Mono.class);
    }
}
```
In this example, **JAsync** rewrite the code like `XXX.await()` to `XXX.thenVoid(v -> { ... })` to making your methods non-blocking.
With **JAsync**, you can not only enjoy the high throughput of non-blocking programming, but also avoid callback hell and counter-intuitive chained function calls.

#### What JAsync do.
```java
@RestController
@RequestMapping("/employees")
public class MyRestController {
    @Inject
    private EmployeeRepository employeeRepository;
    @Inject
    private SalaryRepository salaryRepository;

    @Async()
    private JPromise<Double> _getEmployeeTotalSalaryByDepartment(String department) {
        double money = 0.0;
        DoubleReference moneyRef = new DoubleReference(money);
        Mono<List<Employee>> empsMono = employeeRepository.findEmployeeByDepartment(department);
        JPromise<List<Employee>> empsPromise = Promises.from(empsMono);
        return empsPromise.thenVoid(v0 -> JAsync.doForEachObject(v0, employee ->
                Promises.from(salaryRepository.findSalaryByEmployee(employee.id)).thenVoid(v1 -> {
                    moneyRef.addAndGet(v1.total);
                })
            ).thenVoid(() -> JAsync.doReturn(JAsync.just(moneyRef.getValue())))).catchReturn();
    }

    // This is a normal webflux method.
    @GetMapping("/{department}/salary")
    public Mono<Double> getEmployeeTotalSalaryByDepartment(@PathVariable String department) { 
        // Use unwrap method to transform the JPromise object back to the Mono object.
        return _getEmployeeTotalSalaryByDepartment(department).unwrap(Mono.class);
    }
}
```

How to use?
=======
First, select a implementation library to the Maven dependency. Currently, only one implementation is available.
```xml
<dependency>
    <groupId>io.github.vipcxj</groupId>
    <artifactId>jasync-reactive</artifactId>
    <version>0.1.0</version>
</dependency>
```
This implementation uses the famous library **Reactor**. The `JPromise` object is a wrapper of `Mono` object.
So the `JPromise` object can be created from a `Mono` object using static method `io.github.vipcxj.jasync.reactive.Promises.from(reactor.core.publisher.Mono<T>)`.
And the `JPromise` object can be converted back to the `Mono` object using instance method `io.github.vipcxj.jasync.spec.JPromise.unwrap`.

Then add the core library to the Maven dependency.
```xml
<dependency>
    <groupId>io.github.vipcxj</groupId>
    <artifactId>jasync-core</artifactId>
    <version>0.1.0</version>
    <scope>provided</scope>
</dependency>
```
The core library is only need at compile stage, so here use the **provided** scope.
Generally, the annotation processor should be discovered by jdk automatically.
However, if something went wrong, jdk can not find the annotation processor, try this:
```xml
<plugins>
  <plugin>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
      <annotationProcessorPaths>
        <path>
          <groupId>io.github.vipcxj</groupId>
          <artifactId>jasync-core</artifactId>
          <version>0.1.0</version>
        </path>
      </annotationProcessorPaths>
    </configuration>
  </plugin>
</plugins>
```
If you are using a jdk >= 9, you should use this instead:
```xml
<dependency>
    <groupId>io.github.vipcxj</groupId>
    <artifactId>jasync-core-java9</artifactId>
    <version>0.1.0</version>
    <scope>provided</scope>
</dependency>
```
or
```xml
<plugins>
  <plugin>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
      <annotationProcessorPaths>
        <path>
          <groupId>io.github.vipcxj</groupId>
          <artifactId>jasync-core-java9</artifactId>
          <version>0.1.0</version>
        </path>
      </annotationProcessorPaths>
    </configuration>
  </plugin>
</plugins>
```

Debug mode
===
**JAsync** support a debug mode. With debug mode on, **JAsync** will inject all useful variable to the current context, even they are not captured.
As a result, When debugging, the developer can see all the variables in the monitor window just like debugging normal code.

For example, with debug mode off:

![alt debug mode off](/debug-off.png)

With debug mode on:

![alt debug mode off](/debug-on.png)

It can be seen that when the debug mode is turned on, all the defined variables can be found in the monitoring window.

[maven-shield]: https://img.shields.io/maven-central/v/io.github.vipcxj/jasync-parent.png
[maven-link]: https://search.maven.org/artifact/io.github.vipcxj/jasync-parent