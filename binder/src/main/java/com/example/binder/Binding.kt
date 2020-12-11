package com.example.binder

import android.app.Activity
import com.example.annotation.internal.BindingSuffix
import java.lang.reflect.InvocationTargetException


class Binding {

    constructor()

    companion object {

        private fun <T : Activity> instantiateBinder(target: T, suffix: String) {
            val targetClass: Class<*> = target.javaClass
            val className = targetClass.name
            try {
                val bindingClass = targetClass.classLoader!!.loadClass(className + suffix)
                val classConstructor = bindingClass.getConstructor(targetClass)
                try {
                    classConstructor.newInstance(target)
                } catch (e: IllegalAccessException) {
                    throw RuntimeException("Unable to invoke $classConstructor", e)
                } catch (e: InstantiationException) {
                    throw RuntimeException("Unable to invoke $classConstructor", e)
                } catch (e: InvocationTargetException) {
                    val cause = e.cause
                    if (cause is RuntimeException) {
                        throw (cause as RuntimeException?)!!
                    }
                    if (cause is Error) {
                        throw (cause as Error?)!!
                    }
                    throw RuntimeException("Unable to create instance.", cause)
                }
            } catch (e: ClassNotFoundException) {
                throw RuntimeException(
                    "Unable to find Class for $className$suffix",
                    e
                )
            } catch (e: NoSuchMethodException) {
                throw RuntimeException(
                    "Unable to find constructor for $className$suffix",
                    e
                )
            }
        }

        fun <T : Activity> bind(activity: T) {

            instantiateBinder(
                activity,
                BindingSuffix.GENERATED_CLASS_SUFFIX
            )
        }
    }
}