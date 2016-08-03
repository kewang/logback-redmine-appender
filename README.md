# logback-redmine-appender

[![Build Status](https://travis-ci.org/kewang/logback-redmine-appender.svg?branch=master)](https://travis-ci.org/kewang/logback-redmine-appender) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/tw.kewang/logback-redmine-appender/badge.svg)](https://maven-badges.herokuapp.com/maven-central/tw.kewang/logback-redmine-appender)

![demo](https://cloud.githubusercontent.com/assets/795839/16677110/8127513e-4504-11e6-8df0-25491dc47b4d.gif)

## Features

* Create ERROR level log @ Redmine
* Merge the same StackTraces @ one issue
* Link StackTraces with git repo (support GitHub, GitLab, BitBucket)

## Dependency

### Maven

```xml
<dependency>
  <groupId>tw.kewang</groupId>
  <artifactId>logback-redmine-appender</artifactId>
  <version>0.4.0</version>
</dependency>
```

### Gradle

```groovy
compile 'tw.kewang:logback-redmine-appender:0.4.0'
```

## How to use

```xml
<appender name="REDMINE" class="tw.kewang.logback.appender.RedmineAppender">
  <url>http://example.com</url> <!-- Required, Your Redmine URL -->
  <apiKey>abcdef1234567890</apiKey> <!-- Required, Your Redmine API key-->
  <projectId>5566</projectId> <!-- Required, Your Redmine Project ID -->
  <title>Logback Redmine Appender</title> <!-- Optional, Your Redmine issue title -->
  <onlyError>true</onlyError> <!-- Optional, Only show ERROR level log, default value is true -->
  <git class="tw.kewang.logback.appender.tag.TagGit">
    <repoUrl>https://bitbucket.org/kewang/logback-redmine-appender</repoUrl> <!-- Optional, Your git vendor URL, https://github.com / https://gitlab.com / https://gitlab.example.com / https://bitbucket.org -->
    <commit>fa729ff</commit> <!-- Optional, Your git commit -->
    <parentDir>src/main/java</parentDir> <!-- Optional, Your parent directory -->
  </git>
  <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
    <pattern>${PATTERN}</pattern>
    <charset>${CHARSET}</charset>
  </encoder>
</appender>
```

## References

* [Chapter 4: Appenders](http://logback.qos.ch/manual/appenders.html)
* [logback-redis-appender](https://github.com/kmtong/logback-redis-appender)
