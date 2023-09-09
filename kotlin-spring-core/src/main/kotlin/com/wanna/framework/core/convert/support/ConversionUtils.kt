package com.wanna.framework.core.convert.support

import com.wanna.framework.core.convert.ConversionService
import com.wanna.framework.core.convert.TypeDescriptor
import com.wanna.framework.lang.Nullable
import com.wanna.framework.util.ClassUtils

/**
 * Conversion转换的工具类
 *
 * @author jianchao.jia
 * @version v1.0
 * @date 2023/9/8
 */
object ConversionUtils {


    fun canConvertElements(
        @Nullable sourceElementType: TypeDescriptor?,
        @Nullable targetElementType: TypeDescriptor?,
        conversionService: ConversionService
    ): Boolean {
        if (targetElementType == null) {
            return true
        }
        if (sourceElementType == null) {
            return true
        }
        if (conversionService.canConvert(sourceElementType, targetElementType)) {
            return true
        }
        if (ClassUtils.isAssignable(sourceElementType.type, targetElementType.type)) {
            // maybe
            return true
        }
        // no
        return false
    }
}