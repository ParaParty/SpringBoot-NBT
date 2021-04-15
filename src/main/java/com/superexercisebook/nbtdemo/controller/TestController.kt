package com.superexercisebook.nbtdemo.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import java.util.*

@Controller
@RequestMapping("")
class TestController {
    val rand = Random();

    @GetMapping("/simple/object")
    @ResponseBody
    fun simpleObject() =
        mapOf(
            "string" to "string",
            "int" to rand.nextInt(),
            "double" to rand.nextDouble(),
            "float" to rand.nextFloat(),
            "boolean" to rand.nextBoolean(),
            "nestedObject" to mapOf(
                "username" to "Eric_Lian",
                "email" to "public@superexercisebook.com"
            )
        )


    @GetMapping("/simple/string")
    @ResponseBody
    fun simpleString() = listOf("Test String.")
}