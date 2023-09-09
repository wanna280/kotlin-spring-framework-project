package com.wanna.framework.core.convert.converter

import com.wanna.framework.core.convert.TypeDescriptor

/**
 *
 * @author jianchao.jia
 * @version v1.0
 * @date 2023/9/6
 */
interface ConditionalConverter {

    fun matches(sourceType: TypeDescriptor, targetType: TypeDescriptor): Boolean
}