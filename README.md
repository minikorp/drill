# Drill 👷
Property drilling for Kotlin

## Getting started

Create a `data class` and annotate it with `@Drill`

```kotlin
@Drill
data class Person(val name: String)
```

When building, Drill will generate a mutable version of Person called `Person_Mutable` and an
extension function `Person.mutate()` that works similar to default `apply` from standard libray.

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
val mutated: Person = person.copy(
    name=person.name.copy(
        surname="Ugly Copy"
    )
)
```

Which becomes harder to read and to maintain as models get more complex and nesting level increasees.  
This is the main use case for drill, using the `mutate` extension we can now write:


```kotlin
val person: Person = Person(name = Name(name="Hello", surname="World")
val mutated: Person = person.mutate { 
    name.surame = "Drill" 
}
```

## Lists And Maps Usage

As well as nesting data classes lists and maps are also pretty common when describing models.

```
@Drill
data class ListItem(val text: String = "item")

@Drill
data class ListClass(
    val list: List<ListItem>
)
```

In order to support mutable like syntax for this types Drill provides two new Types `DrillList` and `DrillMap` that implement like kotlin `MutableList` and a `MutableMap` respectivly but perform some bookeeping in order to maintain data classes copy method semantics.

```kotlin
val source = ListClass(listOf(ListItem()))
println(source) //ListClass(list=[ListItem(text=item)])
val mutated = source.mutate {
    //Modify item 0 and add some new ones
    list[0].text = "Hello I am first index"
    list.add(ListItem("Second item"))
    list.add(ListItem("Third item"))
}.mutate {
    //Remove second item in second mutation
    list.removeAt(1)
}
println(mutated) //ListClass(list=[ListItem(text=Hello I am first index), ListItem(text=Third item)])
```

This way, we can easily modify items inside lists as if they were mutable lists of mutable items. 

Maps behave in a similar way:

```
val source = MapClass()
val newItem = MapItem("added")
val mutated = source.mutate {
    this.map["a"] = newItem
}
```
Check more example usages in the **[test module](https://github.com/minikorp/drill/tree/master/drill-test/src/test/kotlin/sample)**

## Performance and Reference Equality

Only significant performance impact is one additional object allocation everytime a mutable object is read for the first time in a lazy fasion. This includes nested fields and items in both lists and maps. Mutable object creation is still lazy.  
```kotlin
object.mutate {
    field = "reference" // no object allocation
    nested.another = "nested" // mutable object created for `nested`
    list[0].text = "list access" // mutable list and mutable item created
}

```

For that reason, you should avoid traversing the mutable object inside the `mutate` block if running in a critical section like a draw loop to avoid triggering a GC later down the line.

Semantics expected from mutable clases mimic expected behaviour from copy, including reference equality. That is, a non changed field will keep it's reference, so `===` operator will hold true unless field has mutated. For lists and maps, changing any item in the underlying items will trigger list and map recreation.


## Importing

Add common library and annotation processor (with `kapt` plugin) to your dependencies. 
You can grab the latest version from the jitpack tag below:

[![](https://jitpack.io/v/minikorp/drill.svg)](https://jitpack.io/#minikorp/drill)

```kotlin
implementation("com.github.minikorp.drill:drill-common:$DRILL_VERSION")
kapt("com.github.minikorp.drill:drill-processor:$DRILL_VERSION")
```
