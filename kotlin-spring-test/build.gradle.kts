dependencies {
    implementation(project(":kotlin-spring-core"))
    implementation(project(":kotlin-spring-beans"))
    implementation(project(":kotlin-spring-context"))
    implementation(project(":kotlin-spring-jcl"))

    compileOnly("junit:junit:$junit4Version")
    compileOnly("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    compileOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")

    // for test
    testImplementation("junit:junit:$junit4Version")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}
