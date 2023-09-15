package com.wanna.framework.beans.test.propertyaccessor

import com.wanna.framework.beans.BeanWrapperImpl
import org.junit.jupiter.api.Test

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

    var map2: MutableMap<String, MutableMap<String, Int>> = LinkedHashMap()

    var user = User()

    var list = ArrayList<String>()

    var stringArray = arrayOf("0", "1", "2")

    init {
        map["key1"] = "wanna"

        stringMapMap.putIfAbsent("key1", LinkedHashMap())
        stringMapMap["key1"]!!["key2"] = "wanna12"

        stringIntStringMap.putIfAbsent("key1", LinkedHashMap())
        stringIntStringMap["key1"]!!.putIfAbsent(1, "wanna")

        list.add("0")
        list.add("1")
        list.add("2")
    }

    class User {
        var id = 1;
        var name = "wanna"

        var ext = ExtFields()
    }

    class ExtFields {
        var number = 1

        var ext = LinkedHashMap<String, Any>()

        init {
            ext["key"] = "wanna"
        }
    }

    // test get...

    /**
     * 测试简单字段
     */
    @Test
    fun testGetSimpleFiled() {
        val beanWrapper = BeanWrapperImpl(PropertyAccessorTest())
        beanWrapper.autoGrowNestedPaths = true
        assert(beanWrapper.getPropertyValue("name") == "wanna")
    }

    /**
     * 测试简单的Map的Key-Value的情况
     */
    @Test
    fun testGetSimpleMapKeyValue() {
        val beanWrapper = BeanWrapperImpl(PropertyAccessorTest())
        beanWrapper.autoGrowNestedPaths = true
        val propertyValue = beanWrapper.getPropertyValue("map[key1]")
        assert(propertyValue == "wanna")
    }

    /**
     * 测试复杂的Key-Value的情况
     */
    @Test
    fun testGetComplexMapKeyValue1() {
        val beanWrapper = BeanWrapperImpl(PropertyAccessorTest())
        beanWrapper.autoGrowNestedPaths = true

        assert(beanWrapper.getPropertyValue("stringMapMap[key1][key2]") == "wanna12")
    }

    /**
     * 测试内部嵌套对象的情况的取值
     */
    @Test
    fun testGetNestedValue() {
        val beanWrapper = BeanWrapperImpl(PropertyAccessorTest())
        beanWrapper.autoGrowNestedPaths = true

        val pv = beanWrapper.getPropertyValue("user.ext.number")
        assert(pv == 1)
    }

    /**
     * 测试获取内部嵌套对象的情况的取值
     */
    @Test
    fun testGetNestedValue2() {
        val beanWrapper = BeanWrapperImpl(PropertyAccessorTest())
        beanWrapper.autoGrowNestedPaths = true

        val pv = beanWrapper.getPropertyValue("user.ext.ext[key]")
        assert(pv == "wanna")
    }

    /**
     * 测试根据index获取List当中元素
     */
    @Test
    fun testGetListByIndex() {
        val beanWrapper = BeanWrapperImpl(PropertyAccessorTest())
        beanWrapper.autoGrowNestedPaths = true

        assert(
            beanWrapper.getPropertyValue("list[0]") == "0"
                    && beanWrapper.getPropertyValue("list[1]") == "1"
                    && beanWrapper.getPropertyValue("list[2]") == "2"
        )
    }

    /**
     * 测试根据index获取数组当中的元素
     */
    @Test
    fun testGetArrayByIndex() {
        val beanWrapper = BeanWrapperImpl(PropertyAccessorTest())
        beanWrapper.autoGrowNestedPaths = true

        assert(
            beanWrapper.getPropertyValue("stringArray[0]") == "0"
                    && beanWrapper.getPropertyValue("stringArray[1]") == "1"
                    && beanWrapper.getPropertyValue("stringArray[2]") == "2"
        )
    }


    // test set...

    /**
     * 测试复杂的Key-Value的情况
     */
    @Test
    fun testGetComplexMapKeyValue2() {
        val beanWrapper = BeanWrapperImpl(PropertyAccessorTest())
        beanWrapper.autoGrowNestedPaths = true

        val propertyValue = beanWrapper.getPropertyValue("stringIntStringMap[key1][1]")
        assert(propertyValue == "wanna")
    }

    /**
     * 测试复杂的Map的put
     */
    @Test
    fun testSetComplexMap1() {
        val beanWrapper = BeanWrapperImpl(PropertyAccessorTest())
        beanWrapper.autoGrowNestedPaths = true

        // map2原本是一个空的Map, 这里把值注入进去
        beanWrapper.setPropertyValue("map2[keyX][keyY]", "1")
        val pv = beanWrapper.getPropertyValue("map2[keyX][keyY]")
        assert(pv == 1)
    }

    /**
     * 测试复杂Map的put
     */
    @Test
    fun testSetComplexMap2() {
        val beanWrapper = BeanWrapperImpl(PropertyAccessorTest())
        beanWrapper.autoGrowNestedPaths = true

        beanWrapper.setPropertyValue("user.ext.ext[key]", "1")
        val pv = beanWrapper.getPropertyValue("user.ext.ext[key]")
        assert(pv == "1")
    }


    /**
     * 测试数组元素的set
     */
    @Test
    fun testSetArray() {
        val beanWrapper = BeanWrapperImpl(PropertyAccessorTest())
        beanWrapper.autoGrowNestedPaths = true
        beanWrapper.setPropertyValue("stringArray[4]", "4")
        assert(beanWrapper.getPropertyValue("stringArray[4]") == "4")
    }

    /**
     * 测试List元素的set
     */
    @Test
    fun testSetList() {
        val beanWrapper = BeanWrapperImpl(PropertyAccessorTest())
        beanWrapper.autoGrowNestedPaths = true
        beanWrapper.setPropertyValue("list[4]", "4")
        assert(beanWrapper.getPropertyValue("list[4]") == "4")
    }

    /**
     * 测试获取PropertyDescriptor(PD)
     */
    @Test
    fun testGetPropertyDescriptor() {
        val beanWrapper = BeanWrapperImpl(PropertyAccessorTest())
        beanWrapper.autoGrowNestedPaths = true
        val pd = beanWrapper.getPropertyDescriptor("user.ext.ext")
        println(pd)
    }
}