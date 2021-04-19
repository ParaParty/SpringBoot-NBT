package com.superexercisebook.nbtdemo.controller

data class TestNBTObj(
    val string: String,
    val int: Int,
    val double: Double,
    val float: Float,
    val boolean: Boolean,
    val nestedObject: NestedObj
) {
    data class NestedObj(
        val username: String,
        val email: String
    )

    override fun toString(): String {
        return "string:$string\n" +
                "int:$int\n" +
                "double:$double\n" +
                "float:$float\n" +
                "boolean:$boolean\n" +
                "nestedObject:{\n" +
                "\tusername:${nestedObject.username}\n" +
                "\temail:${nestedObject.email}\n" +
                "}\n"
    }
}