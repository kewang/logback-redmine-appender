# logback-redmine-appender

![demo](https://cloud.githubusercontent.com/assets/795839/16677110/8127513e-4504-11e6-8df0-25491dc47b4d.gif)

## Dependency

### Maven

```xml
<dependency>
  <groupId>tw.kewang</groupId>
  <artifactId>logback-redmine-appender</artifactId>
  <version>0.1.0</version>
</dependency>
```

### Gradle

```groovy
compile 'tw.kewang:logback-redmine-appender:0.1.0'
```

## How to use

```xml
<appender name="REDMINE" class="tw.kewang.logback.appender.RedmineAppender">
  <url>http://example.com</url> <!-- Your Redmine URL -->
  <apiKey>abcdef1234567890</apiKey> <!-- Your Redmine API key-->
  <projectId>5566</projectId> <!-- Your Redmine Project ID -->
  <title>Logback Redmine Appender</title> <!-- Your Redmine issue title -->
  <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
    <pattern>${PATTERN}</pattern>
    <charset>${CHARSET}</charset>
  </encoder>
</appender>
```

## References

* [Chapter 4: Appenders](http://logback.qos.ch/manual/appenders.html)
* [logback-redis-appender](https://github.com/kmtong/logback-redis-appender)
