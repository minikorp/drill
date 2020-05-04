# Drill 👷

Drill is an utility library that makes working with immutable data classes easier by generating their mutable counterpart that is later _frozen_ back into their immutable form.

## Getting started

Create a `data class` and annotate it with `@Drill`.

```kotlin
@Drill
data class Person(val name: String)
```

When building, Drill will generate a mutable version of Person called `Person_Mutable` and an
extension function `Person.mutate()` that works similar to default `apply` from standard library.

You can now "mutate" your data class similar to kotlin `copy(...)` method from within the mutate 
block as if your class was mutable.

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

Which becomes harder to read and to maintain as models get more complex and nesting level increases.  
This is the main use case for drill, using the `mutate` extension we can now write:


```kotlin
val person: Person = Person(name = Name(name="Hello", surname="World")
val mutated: Person = person.mutate { 
    name.surname = "Drill" 
}
```

## Lists And Maps Usage

As well as nesting data classes lists and maps are also pretty common when describing models.

```kotlin
@Drill
data class ListItem(val text: String = "item")

@Drill
data class ListClass(
    val list: List<ListItem>
)
```

In order to support mutable like syntax for this types Drill provides two new Types `DrillList` and `DrillMap` that implement like kotlin `MutableList` and a `MutableMap` respectively but perform some bookkeeping in order to maintain data classes copy method semantics.  

This way, we can easily modify items inside lists as if they were mutable lists of mutable items. 

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

Maps behave in a similar way:

```kotlin
val source = MapClass()
val newItem = MapItem("added")
val mutated = source.mutate {
    this.map["a"] = newItem
}
```
Check more example usages in the **[test module](https://github.com/minikorp/drill/tree/master/drill-test/src/test/kotlin/sample)**

## Performance and Reference Equality

Only significant performance impact is one additional object allocation everytime a mutable object is read for the first time in a lazy fashion. This includes nested fields and items in both lists and maps.  

```kotlin
object.mutate { // implicit `this` mandatory allocation
    field = "reference" // no object allocation
    nested.another = "nested" // mutable object `nested` allocated
    list.size // mutable list allocated
    list[0].text = "list access" // mutable item [0] allocated
}
```

For that reason, you should avoid traversing mutables object inside the `mutate` block if running in a critical section like a draw loop to prevent triggering a GC later down the line. Reverting back to regular `copy` will always be possible since original classes are not modified in any way.

Semantics expected from mutable classes mimic behaviour from copy, including reference equality. That is, a non changed field will keep it's reference, so `===` operator will hold true for it's fields unless it was mutated.  

For lists and maps, changing any item in the underlying collection will trigger list or map recreation


## Importing

Add common library and annotation processor (with `kapt` plugin) to your dependencies. 
You can grab the latest version from github maven repository:

[![](https://jitpack.io/v/minikorp/drill.svg)](https://jitpack.io/#minikorp/drill)

```kotlin
implementation("com.github.minikorp.drill:drill-common:$DRILL_VERSION")
kapt("com.github.minikorp.drill:drill-processor:$DRILL_VERSION")
```
