# TestAnn

```
gradle.properties文件   --->

org.gradle.daemon=true
org.gradle.jvmargs=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005

terminal   --->

gradle --daemon


Edit Configurations --->

Remote


terminal   --->

gradle clean assembleDebug

```