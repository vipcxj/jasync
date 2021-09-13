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

    // The standard JAsync async method must be annotated with the Async annotation, and return a Promise object.
    @Async()
    private Promise<Double> _getEmployeeTotalSalaryByDepartment(String department) {
        double money = 0.0;
        // A Mono object can be transformed to the Promise object. So we get a Mono object first.
        Mono<List<Employee>> empsMono = employeeRepository.findEmployeeByDepartment(department);
        // Transformed the Mono object to the Promise object.
        Promise<List<Employee>> empsPromise = JAsync.from(empsMono);
        // Use await just like es and c# to get the value of the Promise without blocking the current thread.
        for (Employee employee : empsPromise.await()) {
            // The method findSalaryByEmployee also return a Mono object. We transform it to the Promise just like above. And then await to get the result.
            Salary salary = JAsync.from(salaryRepository.findSalaryByEmployee(employee.id)).await();
            money += salary.total;
        }
        // The async method must return a Promise object, so we use just method to wrap the result to a Promise.
        return JAsync.just(money);
    }

    // This is a normal webflux method.
    @GetMapping("/{department}/salary")
    public Mono<Double> getEmployeeTotalSalaryByDepartment(@PathVariable String department) { 
        // Use unwrap method to transform the Promise object back to the Mono object.
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