package com.superexercisebook.nbtdemo.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.superexercisebook.jackson.nbt.NbtFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
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
        TestNBTObj(
            "string",
            rand.nextInt(),
            rand.nextDouble(),
            rand.nextFloat(),
            rand.nextBoolean(),
            TestNBTObj.NestedObj(
                "Eric_Lian",
                "public@superexercisebook.com"
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

    @PostMapping("/simple/testPost")
    fun testPost() {
        val byteData: ByteArray = byteArrayOf(
            0x0A.toByte(),
            0x00.toByte(),
            0x00,
            0x01,
            0x00,
            0x07,
            0x62,
            0x6F,
            0x6F,
            0x6C,
            0x65,
            0x61,
            0x6E,
            0x01,
            0x08,
            0x00,
            0x06,
            0x73,
            0x74,
            0x72,
            0x69,
            0x6E,
            0x67,
            0x00,
            0x06,
            0x73,
            0x74,
            0x72,
            0x69,
            0x6E,
            0x67,
            0x05,
            0x00,
            0x05,
            0x66,
            0x6C,
            0x6F,
            0x61,
            0x74,
            0x3F,
            0x71,
            0x54,
            0x10,
            0x0A,
            0x00,
            0x0C,
            0x6E,
            0x65,
            0x73,
            0x74,
            0x65,
            0x64,
            0x4F,
            0x62,
            0x6A,
            0x65,
            0x63,
            0x74,
            0x08,
            0x00,
            0x05,
            0x65,
            0x6D,
            0x61,
            0x69,
            0x6C,
            0x00,
            0x1C,
            0x70,
            0x75,
            0x62,
            0x6C,
            0x69,
            0x63,
            0x40,
            0x73,
            0x75,
            0x70,
            0x65,
            0x72,
            0x65,
            0x78,
            0x65,
            0x72,
            0x63,
            0x69,
            0x73,
            0x65,
            0x62,
            0x6F,
            0x6F,
            0x6B,
            0x2E,
            0x63,
            0x6F,
            0x6D,
            0x08,
            0x00,
            0x08,
            0x75,
            0x73,
            0x65,
            0x72,
            0x6E,
            0x61,
            0x6D,
            0x65,
            0x00,
            0x09,
            0x45,
            0x72,
            0x69,
            0x63,
            0x5F,
            0x4C,
            0x69,
            0x61,
            0x6E,
            0x00,
            0x03,
            0x00,
            0x03,
            0x69,
            0x6E,
            0x74,
            0x81.toByte(),
            0xEE.toByte(),
            0xC4.toByte(),
            0x7A,
            0x06,
            0x00,
            0x06,
            0x64,
            0x6F,
            0x75,
            0x62,
            0x6C,
            0x65,
            0x3F,
            0xEE.toByte(),
            0x54,
            0x6B,
            0xDE.toByte(),
            0x66,
            0xF2.toByte(),
            0xD3.toByte(),
            0x00)
        val nbtParser = NbtFactory.builder().build().createParser(byteData)
        nbtParser.setInput(byteData)
        nbtParser.getCompoundValue();
    }
}