JAsync - 在Java中实现async-await模式
===============

[![Maven Release][maven-shield]][maven-link]

[English Version](/README_CN.md)

QQ群： 696195080

**JAsync** 实现了类似 ES，C# 中的async-await模式。这允许开发者以传统顺序的方式编写异步代码。 
它将同时在编码和调试两方面使开发者编写异步代码的体验尽可能接近传统的同步代码。

另一方面本框架通过一套接口分离了上层语法树转换和底层异步工作流的实现。本项目专注于前者，而后者则可以通过封装现有的异步库来实现，比如Reactor，RxJava等。
这样的好处是本项目可以和社区中各种已经被广泛使用的异步库无缝结合，产生1+1>2的效果。

需求
===
jdk >= 8

Examples
=======
#### 使用 JAsync
```java
@RestController
@RequestMapping("/employees")
public class MyRestController {
    @Inject
    private EmployeeRepository employeeRepository;
    @Inject
    private SalaryRepository salaryRepository;

    // 一个标准的 JAsync 异步方法必须带上 Async 注解，并且返回一个 JPromise 对象.
    @Async()
    private JPromise<Double> _getEmployeeTotalSalaryByDepartment(String department) {
        double money = 0.0;
        // 一个 Reactor 的 Mono 对象可以被转换为标准 JPromise 对象。 所以我们先获取一个 Mono 对象。
        Mono<List<Employee>> empsMono = employeeRepository.findEmployeeByDepartment(department);
        // 将Mono对象转换为标准 JPromise 对象.
        JPromise<List<Employee>> empsPromise = Promises.from(empsMono);
        // 像es和c#里那样使用 await 来在不阻塞当前线程的前提下获取 JPromise 的结果。
        for (Employee employee : empsPromise.await()) {
            // 方法 findSalaryByEmployee 同样返回一个 Mono 对象。我们将其转换为标准 JPromise 对象，并像上面提到的那样使用 await 获取其结果。
            Salary salary = Promises.from(salaryRepository.findSalaryByEmployee(employee.id)).await();
            money += salary.total;
        }
        // JAsync 异步方法必须返回一个 JPromise 对象，所以我们使用 just 方法封装返回值。
        return JAsync.just(money);
    }

    // 这是一个普通的 webflux 方法.
    @GetMapping("/{department}/salary")
    public Mono<Double> getEmployeeTotalSalaryByDepartment(@PathVariable String department) {
        // 使用 unwrap 方法将 JPromise 对象转换回 Mono 对象。
        return _getEmployeeTotalSalaryByDepartment(department).unwrap(Mono.class);
    }
}
```
在这个例子中，**JAsync**将类似`XXX.await()`的代码重写成`XXX.thenVoid(() -> { ... })`，以使你的方法非阻塞。
有了**JAsync**，您既可以享受非阻塞编程的高吞吐量，又避免了回调地狱和反直觉的各种链式函数调用。

#### JAsync 都干了什么.
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

怎样使用？
=======
首先选择一个实现库加到maven依赖中。目前只内置了一个实现库。
```xml
<dependency>
    <groupId>io.github.vipcxj</groupId>
    <artifactId>jasync-reactive</artifactId>
    <version>0.1.4</version>
</dependency>
```
这个实现库是基于著名的响应式框架 **Reactor** 的。在这个实现中，`JPromise` 对象是 `Mono` 对象的封装。
所以 `JPromise` 可以使用静态方法 `io.github.vipcxj.jasync.reactive.Promises.from(reactor.core.publisher.Mono<T>)` 来通过`Mono` 构造。 
并且 `JPromise` 也可以使用实例方法 `io.github.vipcxj.jasync.spec.JPromise.unwrap` 转换回 `Mono`。

然后将核心库添加到maven依赖中。核心库用于语法树的转换。
```xml
<dependency>
    <groupId>io.github.vipcxj</groupId>
    <artifactId>jasync-core</artifactId>
    <version>0.1.4</version>
    <scope>provided</scope>
</dependency>
```
核心库仅用于编译阶段，所以这里使用了 **provided** 作用域。
通常，APT（注解处理器）会被JDK自动发现。但如果哪里出了问题，JDK不能找到APT，试试这个：
```xml
<plugins>
  <plugin>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
      <annotationProcessorPaths>
        <path>
          <groupId>io.github.vipcxj</groupId>
          <artifactId>jasync-core</artifactId>
          <version>0.1.4</version>
        </path>
      </annotationProcessorPaths>
    </configuration>
  </plugin>
</plugins>
```
**注意:** 如果已经配置了 `annotationProcessorPaths`，依赖方式的配置将失效。

Debug mode
===
**JAsync** 支持 debug 模式。在 debug 模式下，**JAsync** 会将所有对调试有用的变量注入到当前上下文，即使是那些没有被捕获的变量。
于是断点调试时，开发者可以再监视窗里看到所有可用的变量，就像调试普通代码那样。

举个例子, 当 debug 模式关闭时：

![alt debug mode off](/debug-off.png)

当 debug 模式开启时：

![alt debug mode off](/debug-on.png)

可以看到当 debug 模式开启时， 所有已经定义的变量都能在监视窗中被找到，IDE在代码内的辅助显示也自动生效了。

Known Issues
===
1. 不支持 java17 新引入的新`switch` 语法。这个近期会得到支持。
2. 不支持 ejc (eclipse java compiler)。我将尽我最大的努力来完善 ejc 的支持。
   目前一个不完美的解决方法是先用 maven 或 gradle 来编译，然后再用 ejc 调试。

[maven-shield]: https://img.shields.io/maven-central/v/io.github.vipcxj/jasync-parent.png
[maven-link]: https://search.maven.org/artifact/io.github.vipcxj/jasync-parent