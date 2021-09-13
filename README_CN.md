JAsync - 在Java中实现async-await模式
===============

[![Maven Release][maven-shield]][maven-link]

[English Version](/README_CN.md)

**JAsync** 实现了类似 ES，C# 中的async-await模式。这允许开发者以传统顺序的方式编写异步代码。 
它将同时在编码和调试两方面使开发者编写异步代码的体验尽可能接近传统的同步代码。

另一方面本框架通过一套接口分离了上层语法树转换和底层异步工作流的实现。本项目专注于前者，而后者则可以通过封装现有的异步库来实现，比如Reactor，RxJava等。
这样的好处是本项目可以和社区中各种已经被广泛使用的异步库无缝结合，产生1+1>2的效果。

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

    // 一个标准的 JAsync 异步方法必须带上 Async 注解，并且返回一个 Promise 对象.
    @Async()
    private Promise<Double> _getEmployeeTotalSalaryByDepartment(String department) {
        double money = 0.0;
        // 一个 Reactor 的 Mono 对象可以被转换为标准 Promise 对象。 所以我们先获取一个 Mono 对象。
        Mono<List<Employee>> empsMono = employeeRepository.findEmployeeByDepartment(department);
        // 将Mono对象转换为标准 Promise 对象.
        Promise<List<Employee>> empsPromise = JAsync.from(empsMono);
        // 像es和c#里那样使用 await 来在不阻塞当前线程的前提下获取 Promise 的结果。
        for (Employee employee : empsPromise.await()) {
            // 方法 findSalaryByEmployee 同样返回一个 Mono 对象。我们将其转换为标准 Promise 对象，并像上面提到的那样使用 await 获取其结果。
            Salary salary = JAsync.from(salaryRepository.findSalaryByEmployee(employee.id)).await();
            money += salary.total;
        }
        // JAsync 异步方法必须返回一个 Promise 对象，所以我们使用 just 方法封装返回值。
        return JAsync.just(money);
    }

    // 这是一个普通的 webflux 方法.
    @GetMapping("/{department}/salary")
    public Mono<Double> getEmployeeTotalSalaryByDepartment(@PathVariable String department) {
        // 使用 unwrap 方法将 Promise 对象转换回 Mono 对象。
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
    private Promise<Double> _getEmployeeTotalSalaryByDepartment(String department) {
        double money = 0.0;
        DoubleReference moneyRef = new DoubleReference(money);
        Mono<List<Employee>> empsMono = employeeRepository.findEmployeeByDepartment(department);
        Promise<List<Employee>> empsPromise = JAsync.from(empsMono);
        return empsPromise.thenVoid(v0 -> JAsync.doForEachObject(v0, employee -> 
                JAsync.from(salaryRepository.findSalaryByEmployee(employee.id)).thenVoid(v1 -> {
                    moneyRef.addAndGet(v1.total);
                })
            ).thenVoid(() -> JAsync.doReturn(JAsync.just(moneyRef.getValue())))).catchReturn();
    }

    // This is a normal webflux method.
    @GetMapping("/{department}/salary")
    public Mono<Double> getEmployeeTotalSalaryByDepartment(@PathVariable String department) { 
        // Use unwrap method to transform the Promise object back to the Mono object.
        return _getEmployeeTotalSalaryByDepartment(department).unwrap(Mono.class);
    }
}
```

[maven-shield]: https://img.shields.io/maven-central/v/io.github.vipcxj/jasync-parent.png
[maven-link]: https://search.maven.org/artifact/io.github.vipcxj/jasync-parent