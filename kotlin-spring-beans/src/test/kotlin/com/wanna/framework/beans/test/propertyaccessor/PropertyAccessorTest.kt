package com.wanna.framework.beans.test.propertyaccessor

import com.wanna.framework.beans.BeanWrapperImpl

/**
 *
 * @author jianchao.jia
 * @version v1.0
 * @date 2023/9/10
 */
class PropertyAccessorTest {

    var name: String = "wanna"

    var map: MutableMap<String, String> = LinkedHashMap()

    var stringMapMap: MutableMap<String, MutableMap<String, String>> = LinkedHashMap()

    var stringIntStringMap: MutableMap<String, MutableMap<Int, String>> = LinkedHashMap()

    init {
        map["key1"] = "wanna"

        stringMapMap.putIfAbsent("key1", LinkedHashMap())
        stringMapMap["key1"]!!["key2"] = "wanna12"

        stringIntStringMap.putIfAbsent("key1", LinkedHashMap())
        stringIntStringMap["key1"]!!.putIfAbsent(1, "wanna")
    }
}

fun main() {
    val beanWrapper = BeanWrapperImpl(PropertyAccessorTest())
    println(beanWrapper.getPropertyValue("name"))

    val propertyValue = beanWrapper.getPropertyValue("map[key1]")
    println(propertyValue)

    val propertyValue1 = beanWrapper.getPropertyValue("stringMapMap[key1][key2]")
    println(propertyValue1)

    val propertyValue2 = beanWrapper.getPropertyValue("stringIntStringMap[key1][1]")
    println(propertyValue2)
}