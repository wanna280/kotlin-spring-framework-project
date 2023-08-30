dependencies {

    // for runtime
    implementation(project(":kotlin-spring-core"))
    implementation(project(":kotlin-spring-aop"))
    implementation(project(":kotlin-spring-beans"))

    // for compile
    compileOnly(project(":kotlin-spring-instrument"))  // compileOnly
    implementation(project(":kotlin-spring-jcl"))
    compileOnly("org.aspectj:aspectjweaver:$aspectJVersion")


    // for test
    testImplementation("com.alibaba:druid:$druidVersion")
    testImplementation("mysql:mysql-connector-java:$mysqlVersion")
}
