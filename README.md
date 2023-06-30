# Quarkus Panache Reactive Coroutines


## Possible workaround for missing `@WithTransaction` functionality for suspend endpoints

According to issue: https://github.com/quarkusio/quarkus/issues/34101 `@WithTransaction` does not work with `suspend` endpoints. 

After some trials I came up with a solution that 'might' work. 

*Disclaimer:* though I dare to say that I'm quite experience with Kotlin, Reactive Programming, Coroutines, especially in the context of Springboot, I'm not a Quarkus expert. 
I looked today at it for the first time (see initial commit)... 

The solution is basically a simple piece of code that provides a transaction block, rather than an annotation. However, binding this code to an @Annotation is the least of the problems:
```kotlin
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun <T> withTransaction(block: suspend () -> T): T = Panache.withTransaction {
            CoroutineScope(Dispatchers.Unconfined).async { block() }.asUni()
        }.awaitSuspending()
```

The key here is the `Dispatchers.Unconfied`, which must use the current Thread for the Coroutine that is spawn in the async block. 
Since a Panache transaction requires a `VertxThread` to work, which is always present when a `suspend` endpoint is called, this approach seems to work. 

The `withTransaction` code-block can then be used as follows:
```kotlin

 @POST
    suspend fun create(fruit: Fruit): Response = withTransaction {
            fruit.persist<Fruit>().awaitSuspending().let { Response.ok(it).status(Response.Status.CREATED).build() }
        }
    
```


The tests in this project are green with this approach. As reference, I also included a counterpart written in reactive code using standard mutiny. 

If this code is production ready is uncertain. I don't use Quarkus in production, so I won't spend any more time on it to figure out whether this is a production solution or not. 

So it's on your own risk ;-). 

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/code-with-quarkus-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

## Related Guides

- Kotlin ([guide](https://quarkus.io/guides/kotlin)): Write your services in Kotlin
- Reactive PostgreSQL client ([guide](https://quarkus.io/guides/reactive-sql-clients)): Connect to the PostgreSQL database using the reactive pattern

## Provided Code

### RESTEasy Reactive

Easily start your Reactive RESTful Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
# quarkus-panache-kotlin-coroutines
