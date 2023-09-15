dependencies {
    implementation(project(":kotlin-spring-core"))
    implementation(project(":kotlin-spring-jcl"))

    // junit5 单元测试
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}
