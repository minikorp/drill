# Drill
Property drilling for Kotlin

## Getting started

Create a `data class` and annotate it with `@Drill`

```kotlin
@Drill
data class Person(val name: String)
```

When building, Drill will generate a mutable version of Person called `Person_Mutable` and an
extension function `mutate()` that works similar to default `apply` from standard libray.

You can now "mutate" your data class similar to kotlin `copy(...)` method.

```kotlin
val person: Person = Person(name = "Hello")
val mutated: Person = person.mutate { name = "world" }
```

However, Drill advantages come at play when accessing deeply nested information.  
Let's modify name to be a complex structure

```kotlin
@Drill
data class Name(val name: String, val surname: String)

@Drill
data class Person(val name: Name)
```

If we were to modify the surname using copy methods we would write:

```kotlin
val person: Person = Person(name = Name(name="Hello", surname="World")
val mutated: Person = person.copy(name=person.name.copy(surname="Ugly Copy"))
```

Which becomes harder to read and to maintain as models get more complex and nesting level increasees.  
This is the main use case for drill, using the `mutate` extension we can now write:


```kotlin
val person: Person = Person(name = Name(name="Hello", surname="World")
val mutated: Person = person.mutate { name.surame = "Drill" }
```

## Lists And Maps

TODO("Document this")

## Ignoring field

TODO("Document this")

## Performance and reference equality

TODO("Document this")

## Limitations

TODO("Document this")

## Importing

Add common library and annotation processor (with `kapt` plugin) to your dependencies. 
You can grab the latest version from the jitpack tag below:

[![](https://jitpack.io/v/minikorp/drill.svg)](https://jitpack.io/#minikorp/drill)

```kotlin
implementation("com.github.minikorp.drill:drill-common:$DRILL_VERSION")
kapt("com.github.minikorp.drill:drill-processor:$DRILL_VERSION")
```

