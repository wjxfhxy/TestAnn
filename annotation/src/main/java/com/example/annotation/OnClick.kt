package com.example.annotation

import androidx.annotation.IdRes
import java.lang.annotation.ElementType
import java.lang.annotation.RetentionPolicy


@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class OnClick(val value: Int)