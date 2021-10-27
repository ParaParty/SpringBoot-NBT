package com.superexercisebook.nbtdemo.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.superexercisebook.jackson.nbt.NbtMapper
import com.superexercisebook.nbtdemo.domain.dto.RegisterFormDto
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import java.io.ByteArrayInputStream
import java.util.*


@Controller
@RequestMapping("")
class TestController {
    val rand = Random();

    /**
     * 普通数据类型测试
     */
    @GetMapping("/simple/int")
    @ResponseBody
    fun simpleInt() = rand.nextInt()

    /**
     * 普通对象测试
     */
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

    /**
     * 以数组为根节点的序列化测试（NBT Explorer 不支持）
     */
    @GetMapping("/simple/stringList")
    @ResponseBody
    fun simpleStringList() = listOf("Test String.")


    /**
     * 以数组为根节点的序列化测试（NBT Explorer 不支持）
     */
    @GetMapping("/simple/longList")
    @ResponseBody
    fun simpleLongList() = (0..rand.nextInt(1000)).map{ rand.nextInt() }

    var client: OkHttpClient = OkHttpClient()

    /**
     * 复杂序列化测试
     */
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

    /**
     * 序列化测试（用户表单）
     */
    @GetMapping("/simple/register")
    @ResponseBody
    fun simpleRegisterGenerate() = RegisterFormDto().apply {
        username = "Eric"
        email = "Test@ttt.me"
    }

    /**
     * 反序列化测试（用户表单）
     */
    @PostMapping("/simple/register", produces = ["text/plain"])
    @ResponseBody
    fun simpleRegister(@RequestBody t : RegisterFormDto): Any {
        return "${t.username}\n${t.email}"
    }

    /**
     * 反序列化测试
     */
    @PostMapping("/simple/testParse", produces = ["application/json"])
    @ResponseBody
    fun simpleParse(@RequestBody t : ByteArray): Any {
        // 以 json 形式输出到页面上
        val mapper = NbtMapper()
        return mapper.readTree(ByteArrayInputStream(t));
    }

}