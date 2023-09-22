package com.stephentetzlaff.audit

import com.stephentetzlaff.audit.data.AuditId
import com.stephentetzlaff.audit.data.ListUpdate
import com.stephentetzlaff.audit.data.PropertyUpdated
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll

data class Foo(
    val bar: String?,
    val fizz: String?,
    @AuditId val version: String = "v0"
)

data class Bar(
    val fizz: List<String>?,
    val buzz: String
)

data class Fizz(
    val foo: List<Foo>?,
    val bar: String
)

data class Buzz(
    val foo: Foo?,
    val bar: Bar?
)

fun fooBuilder(bar: String) = Foo(bar, "Fizz")
fun barBuilder(fizz: List<String>) = Bar(fizz, "Buzz")
fun fizzBuilder(foo: List<String>, bar: String) = Fizz(
    foo.mapIndexed { i, it ->
        Foo(
            it,
            "Fizz",
            "v$i"
        )
    },
    bar
)

class DiffToolTest : DescribeSpec({
    val diffTool = DefaultDiffTool()

    describe("null handling tests") {
        it("null values test") {
            val output = diffTool.diff(Foo("Bar", null), Foo(null, "Fizz"))
            output.shouldContainAll(
                PropertyUpdated(
                    property = ".bar",
                    previous = "Bar",
                    current = null
                ),
                PropertyUpdated(
                    property = ".fizz",
                    previous = null,
                    current = "Fizz"
                )
            )
        }
        it("null object test") {
            val result = diffTool.diff(Buzz(Foo("bar", "fizz"), null), Buzz(null, Bar(listOf("fizz"), "buzz")))
            result.shouldContainAll(
                PropertyUpdated(
                    property = ".foo.bar",
                    previous = "bar",
                    current = null
                ),
                PropertyUpdated(
                    property = ".foo.fizz",
                    previous = "fizz",
                    current = null
                ),
                ListUpdate(
                    property = ".bar.fizz",
                    added = listOf("fizz"),
                    removed = emptyList()
                ),
                PropertyUpdated(
                    property = ".bar.buzz",
                    previous = null,
                    current = "buzz"
                )
            )
        }
        it("null collection test") {
            val result = diffTool.diff(
                Bar(null, "bar"),
                Bar(listOf("fizz", "buzz"), "bar")
            )
            result.shouldContainAll(
                ListUpdate(
                    property = ".fizz",
                    added = listOf("fizz", "buzz"),
                    removed = emptyList()
                )
            )
        }
    }

    describe("DiffTool on primitives") {
        it("should diff two booleans") {
            val result = diffTool.diff(true, false)
            result.shouldContain(
                PropertyUpdated(
                    property = "",
                    previous = true,
                    current = false
                )
            )
        }
        it("should diff two ints") {
            val result = diffTool.diff(1, 2)
            result.shouldContain(
                PropertyUpdated(
                    property = "",
                    previous = 1,
                    current = 2
                )
            )
        }
        it("should diff two strings") {
            val result = diffTool.diff("Fizz", "Buzz")
            result.shouldContain(
                PropertyUpdated(
                    property = "",
                    previous = "Fizz",
                    current = "Buzz"
                )
            )
        }
    }
    describe("DiffTool on objects without arrays") {
        fooBuilder("Fizz").let { fooOld ->
            fooBuilder("Buzz").let { fooNew ->
                val result = diffTool.diff(fooOld, fooNew)
                result.shouldContain(
                    PropertyUpdated(
                        property = ".bar",
                        previous = "Fizz",
                        current = "Buzz"
                    )
                )
            }
        }
    }
    describe("DiffTool on objects with arrays") {
        it("testing diff on arrays of primitives") {
            barBuilder(listOf("fizz", "buzz")).let { barOld ->
                barBuilder(listOf("fizz", "fuzz")).let { barNew ->
                    val result = diffTool.diff(barOld, barNew)
                    result.shouldContainAll(
                        ListUpdate(
                            property = ".fizz",
                            added = listOf("fuzz"),
                            removed = listOf("buzz")
                        )
                    )
                }
            }
        }
        it("testing diff on arrays of objects") {
            fizzBuilder(listOf("foo", "bar"), "buzz").let { fbOld ->
                fizzBuilder(listOf("foo", "fiz"), "buzz").let { fbNew ->
                    val result = diffTool.diff(fbOld, fbNew)
                    result.shouldContainAll(
                        PropertyUpdated(
                            property = ".foo[v1].bar",
                            previous = "bar",
                            current = "fiz"
                        )
                    )
                }
            }
        }
    }
})
