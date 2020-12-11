package com.example.compiler

import com.example.annotation.BindView
import com.example.annotation.Keep
import com.example.annotation.OnClick
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import java.io.IOException
import java.util.*
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.Elements
import javax.tools.Diagnostic
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.set

class Processor: AbstractProcessor() {

    private lateinit var filer: Filer
    private lateinit var messager: Messager
    private lateinit var elementUtils: Elements

    //每个存在注解的类整理出来，key:package_classname value:被注解的类型元素
    private val annotationClassMap = HashMap<String, ArrayList<Element>>()

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)

        print("init")

        filer = processingEnv.filer
        messager = processingEnv.messager
        elementUtils = processingEnv.elementUtils
    }

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {

        print("begin process")

        if (!roundEnv.processingOver()) {
            buildAnnotatedElement(roundEnv, BindView::class.java)
            buildAnnotatedElement(roundEnv, OnClick::class.java)
        }
        else {
            annotationClassMap.forEach { entry ->

                val packageName = entry.key.split("_".toRegex()).toTypedArray()[0]
                val typeName = entry.key.split("_".toRegex()).toTypedArray()[1]
                val className = ClassName.get(packageName, typeName)
                val generatedClassName =
                    ClassName.get(packageName, NameStore.getGeneratedClassName(typeName))

                /*
                创建要生成的类，如下所示
                @Keep
                public class MainActivity$Binding {}*/
                val classBuilder = TypeSpec
                    .classBuilder(generatedClassName)
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Keep::class.java)

                /*添加构造函数
                *   public MainActivity$Binding(MainActivity activity) {
                    bindViews(activity);
                    bindOnClicks(activity);
                  }
                */
                classBuilder.addMethod(
                    MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(className, NameStore.Variable.ANDROID_ACTIVITY)
                        .addStatement(
                            "\$N(\$N)",
                            NameStore.Method.BIND_VIEWS,
                            NameStore.Variable.ANDROID_ACTIVITY
                        )
                        .addStatement(
                            "\$N(\$N)",
                            NameStore.Method.BIND_ON_CLICKS,
                            NameStore.Variable.ANDROID_ACTIVITY
                        )
                        .build()
                )

                /*创建方法bindViews(MainActivity activity)
                 * private void bindViews(MainActivity activity) {}
                 */
                val bindViewsMethodBuilder = MethodSpec
                    .methodBuilder(NameStore.Method.BIND_VIEWS)
                    .addModifiers(Modifier.PRIVATE)
                    .returns(Void.TYPE)
                    .addParameter(className, NameStore.Variable.ANDROID_ACTIVITY)

                /*增加方法体
                 * activity.tv = (TextView)activity.findViewById(2131165326);
                 * */
                for (variableElement in ElementFilter.fieldsIn(entry.value)) {
                    val bindView = variableElement.getAnnotation(BindView::class.java)
                    if (bindView != null) {
                        bindViewsMethodBuilder.addStatement(
                            "\$N.\$N = (\$T)\$N.findViewById(\$L)",
                            NameStore.Variable.ANDROID_ACTIVITY,
                            variableElement.simpleName,
                            variableElement,
                            NameStore.Variable.ANDROID_ACTIVITY,
                            bindView.value
                        )
                    }
                }

                //将构建出来的方法添加到类里面
                classBuilder.addMethod(bindViewsMethodBuilder.build())

                /*以下构建如下代码
                *   private void bindOnClicks(final MainActivity activity) {
                    activity.findViewById(2131165218).setOnClickListener(new View.OnClickListener() {
                      public void onClick(View view) {
                        activity.onHelloBtnClick(view);
                      }
                    });
                  }
                 */
                val androidOnClickListenerClassName = ClassName.get(
                    NameStore.Package.ANDROID_VIEW,
                    NameStore.Class.ANDROID_VIEW,
                    NameStore.Class.ANDROID_VIEW_ON_CLICK_LISTENER
                )

                val androidViewClassName = ClassName.get(
                    NameStore.Package.ANDROID_VIEW,
                    NameStore.Class.ANDROID_VIEW
                )

                val bindOnClicksMethodBuilder = MethodSpec
                    .methodBuilder(NameStore.Method.BIND_ON_CLICKS)
                    .addModifiers(Modifier.PRIVATE)
                    .returns(Void.TYPE)
                    .addParameter(
                        className,
                        NameStore.Variable.ANDROID_ACTIVITY,
                        Modifier.FINAL
                    )

                for (executableElement in ElementFilter.methodsIn(entry.value)) {

                    val onClick = executableElement.getAnnotation(OnClick::class.java)
                    if (onClick == null)
                        continue

                    val onClickListenerClass = TypeSpec.anonymousClassBuilder("")
                        .addSuperinterface(androidOnClickListenerClassName)
                        .addMethod(
                            MethodSpec.methodBuilder(NameStore.Method.ANDROID_VIEW_ON_CLICK)
                                .addModifiers(Modifier.PUBLIC)
                                .addParameter(
                                    androidViewClassName,
                                    NameStore.Variable.ANDROID_VIEW
                                )
                                .addStatement(
                                    "\$N.\$N(\$N)",
                                    NameStore.Variable.ANDROID_ACTIVITY,
                                    executableElement.simpleName,
                                    NameStore.Variable.ANDROID_VIEW
                                )
                                .returns(Void.TYPE)
                                .build()
                        )
                        .build()
                    bindOnClicksMethodBuilder.addStatement(
                        "\$N.findViewById(\$L).setOnClickListener(\$L)",
                        NameStore.Variable.ANDROID_ACTIVITY,
                        onClick.value,
                        onClickListenerClass
                    )
                }
                classBuilder.addMethod(bindOnClicksMethodBuilder.build())

                //将类写入文件中
                try {
                    JavaFile.builder(packageName, classBuilder.build())
                        .build()
                        .writeTo(filer)
                } catch (e: IOException) {
                    messager.printMessage(Diagnostic.Kind.ERROR, e.toString())
                }
            }
        }

        return true
    }

    override fun getSupportedAnnotationTypes(): Set<String> {
        return TreeSet(
            Arrays.asList(
                BindView::class.java.getCanonicalName(),
                OnClick::class.java.getCanonicalName(),
                Keep::class.java.getCanonicalName()
            )
        )
    }

    override fun getSupportedSourceVersion(): SourceVersion {

        return SourceVersion.latestSupported()
    }

    private fun buildAnnotatedElement(roundEnv: RoundEnvironment, clazz: Class<out Annotation?>) {

        for (element in roundEnv.getElementsAnnotatedWith(clazz)) {
            val className = getFullClassName(element)
            var cacheElements = annotationClassMap[className]
            if (cacheElements == null) {
                cacheElements = ArrayList()
                annotationClassMap[className] = cacheElements
            }
            cacheElements.add(element)
        }
    }

    private fun getFullClassName(element: Element): String {

        val typeElement = element.enclosingElement as TypeElement
        val packageName = elementUtils.getPackageOf(typeElement).qualifiedName.toString()
        return packageName + "_" + typeElement.simpleName.toString()
    }
}