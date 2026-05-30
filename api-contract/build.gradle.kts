plugins {
    `java-library`
    id("io.spring.dependency-management")
    id("org.openapi.generator") version "7.19.0"
}

version = "0.0.1"

dependencies {
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-validation")

    api("io.swagger.core.v3:swagger-annotations-jakarta:2.2.42")
    api("org.openapitools:jackson-databind-nullable:0.2.7")
}



dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.14")
    }
}
openApiGenerate {
    generatorName.set("spring")
    inputSpec.set("$projectDir/api.yaml")
    outputDir.set(layout.buildDirectory.dir("generated").get().asFile.absolutePath)

    modelPackage.set("ru.storage.contracts.dto")
    apiPackage.set("ru.storage.contracts.api")

    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "useSpringBoot3" to "true",
            "useJakartaEe" to "true",
            "useTags" to "true",
            "skipDefaultInterface" to "true"
        )
    )
}

tasks.compileJava {
    dependsOn(tasks.openApiGenerate)
}

sourceSets {
    main {
        java {
            srcDir(layout.buildDirectory.dir("generated/src/main/java"))
        }
    }
}
