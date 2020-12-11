package com.example.annotation

import java.lang.annotation.ElementType
import java.lang.annotation.RetentionPolicy

@Target(AnnotationTarget.CLASS,
    AnnotationTarget.TYPE)
annotation class Keep