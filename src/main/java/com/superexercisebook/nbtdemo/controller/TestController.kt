package com.superexercisebook.nbtdemo.controller

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import java.util.*


@Controller
@RequestMapping("")
class TestController {
    val rand = Random();

    @GetMapping("/simple/int")
    @ResponseBody
    fun simpleInt() = rand.nextInt()

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

    @GetMapping("/simple/stringList")
    @ResponseBody
    fun simpleStringList() = listOf("Test String.")


    @GetMapping("/simple/longList")
    @ResponseBody
    fun simpleLongList() = (0..rand.nextInt(1000)).map{ rand.nextInt() }

    var client: OkHttpClient = OkHttpClient()

    @GetMapping("/simple/complexObject")
    @ResponseBody
    fun simpleComplexObject(): Any {
        val request: Request = Request.Builder().url("https://reciter.binkic.com/comprehensions/comprehensions.json").build();
        val result = client.newCall(request).execute().use {
                response -> response.body()?.string()
        }

        val mapper = ObjectMapper()
        return mapper.readTree(result);
    }

}