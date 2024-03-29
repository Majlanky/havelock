# Havelock
[![Build Status](https://travis-ci.com/Majlanky/havelock.svg?branch=master)](https://travis-ci.com/Majlanky/havelock)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=com.groocraft%3Ahavelock&metric=coverage)](https://sonarcloud.io/dashboard?id=com.groocraft%3Ahavelock)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=com.groocraft%3Ahavelock&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=com.groocraft%3Ahavelock)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=com.groocraft%3Ahavelock&metric=security_rating)](https://sonarcloud.io/dashboard?id=com.groocraft%3Ahavelock)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=com.groocraft%3Ahavelock&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=com.groocraft%3Ahavelock)
[![Known Vulnerabilities](https://snyk.io/test/github/majlanky/havelock/badge.svg)](https://snyk.io/test/github/majlanky/havelock)  
![](https://img.shields.io/badge/compatibility-JDK17%20and%20higher-purple)
![](https://img.shields.io/badge/compatibility-Native%20builds-purple)
![](https://img.shields.io/badge/compatibility-Spring%20Boot%203+-purple)

Havelock project started in 2022. Basic idea and motivation was to invert and simplify logic of Spring security configuration regarding public endpoints.
When security is used especially with other libraries as SpringDoc, it is very non-centralized to make an endpoint public. First things first endpoint must be 
declared in controller, SpringDoc annotation (for example SecurityRequirements) used on the endpoint and then the endpoint must be permitted for all in 
security config. One thing wrong and everything goes wrong. The other thing is repeated things like exposing Swagger UI and api docs.

From version 1.3.0 Havelock can provide actuator endpoint `publicpaths` that provides list of all public path. This feature is disabled by default. See
[wiki](https://github.com/Majlanky/havelock/wiki) for more information...

From version 2.0.0 Havelock is ready for JDK17 and Spring Boot 3+. In the version 2.0.0 deprecated WebSecurityConfigurerAdapter was removed!

Artifacts releases are available on maven central (and on pages indexing central):
* [central](https://repo1.maven.org/maven2/com/groocraft/havelock/)
* [mvnRepository](https://mvnrepository.com/artifact/com.groocraft/havelock)

##Appeal
If you face some case Havelock does not support, let me know to help extends and improve the project. Thx!

## Wiki
This README contains only basic information about project. For more or detailed information, visit the [wiki](https://github.com/Majlanky/havelock/wiki) 

## Project Focus

The main focus is to remove repeated code and lower the effort to proper configuration of Spring security. The other focus is to centralize information
in the code on one place.

## Getting Started 
First things first we have to add Maven dependency
```xml
<dependency>
   <groupId>com.groocraft</groupId>
   <artifactId>havelock</artifactId>
  <version>${version}</version>
</dependency>
```
![](https://img.shields.io/badge/-Warning-red)
Originally Havelock has used WebSecurityConfigurerAdapter internally. Because it cases problem in project where SecurityFilterChain was configured, Havelock 
added support for both and what is used is determined by configuration of `@EnableHavelock`. Check the following chapters to find you how to do it for both 
cases. For backward compatibility of the previous Havelock version, the SecurityFilterChain approach must be explicitly turned on even it is by 
Spring preferred way.

When you have Havelock on you classpath the first thing that must be done is to place @EnableHavelock on Application class of Spring Boot or 
Configuration class for example when you are using SecurityFilterChain:
```java
@Configuration
@EnableWebSecurity
@EnableHavelock(useSecurityFilter = true)
public class WebSecurityConfig{
    ...
}
```

The other thing is to place @Public on method or whole controller that represents an endpoint you want to make public. 
Example of making public all endpoint of a controller:  
```java
@Controller
@Public
@RequestMapping("/base")
public class BasePathController {

    @GetMapping("/get/test")
    public String get() {
        return "test";
    }
}
```
Example of make one endpoint public:
```java
@Controller
public class Controller {

    @Public
    @GetMapping("/get/test")
    public String get() {
        return "test";
    }
}
```

## Building from Source
Despite the fact you can use Havelock as dependency of your project as it is available on maven central, you can build the
project by you own. Havelock is a maven project with prepared maven wrapper. Everything you need to do is call
the following command in the root of the project.
```shell script
$ ./mwnw clean install
```

## Backward Compatibility
Havelock project follows [Apache versioning](https://apr.apache.org/versioning.html)

## Licence
Havelock project is licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
