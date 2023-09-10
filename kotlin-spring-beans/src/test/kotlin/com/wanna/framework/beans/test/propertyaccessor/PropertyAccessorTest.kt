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

    var mapMap: MutableMap<String, MutableMap<String, String>> = LinkedHashMap()

    init {
        map["key1"] = "wanna"

        mapMap.putIfAbsent("key1", LinkedHashMap())
        mapMap["key1"]!!["key2"] = "wanna12"
    }
}

fun main() {
    val beanWrapper = BeanWrapperImpl(PropertyAccessorTest())
    println(beanWrapper.getPropertyValue("name"))

    val propertyValue = beanWrapper.getPropertyValue("map[key1]")
    println(propertyValue)

    val propertyValue1 = beanWrapper.getPropertyValue("mapMap[key1][key2]")
    println(propertyValue1)
}