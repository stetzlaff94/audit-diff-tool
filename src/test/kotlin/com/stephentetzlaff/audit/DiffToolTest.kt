package com.stephentetzlaff.audit

import com.stephentetzlaff.audit.data.PropertyUpdated
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain

data class Foo(
    val bar: Bar,
    val baz: Int,
    val qux: Boolean
)

data class Bar(
    val fizz: String,
    val buzz: String
)

fun fooBuilder(fizz: String) = Foo(
    bar = Bar(
        fizz = fizz,
        buzz = "Buzz"
    ),
    baz = 1,
    qux = true
)

class DiffToolTest : DescribeSpec({
    val diffTool = DefaultDiffTool()
    describe("DiffTool on primitives") {
        it("should diff two booleans") {
            val result = diffTool.diff(true, false)
            result.shouldContain(
                PropertyUpdated(
                    property = "",
                    previous = "true",
                    current = "false"
                )
            )
        }
        it("should diff two ints") {
            val result = diffTool.diff(1, 2)
            result.shouldContain(
                PropertyUpdated(
                    property = "",
                    previous = "1",
                    current = "2"
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
                        property = ".bar.fizz",
                        previous = "Fizz",
                        current = "Buzz"
                    )
                )
            }
        }
    }
})
