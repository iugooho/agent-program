<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>{{groupId}}</groupId>
  <artifactId>{{backendArtifactId}}</artifactId>
  <version>{{version}}</version>
  <packaging>jar</packaging>
  <name>{{projectName}}</name>
  <description>{{description}}</description>

  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
    <maven.compiler.release>{{javaVersion}}</maven.compiler.release>
    <maven.compiler.parameters>true</maven.compiler.parameters>
{{dependencyProperties}}  </properties>

  <dependencies>
{{dependencies}}  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.14.1</version>
        <configuration>
          <release>${maven.compiler.release}</release>
          <encoding>UTF-8</encoding>
          <compilerArgs>
            <arg>-parameters</arg>
            <arg>-Xlint:all</arg>
          </compilerArgs>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>{{surefireVersion}}</version>
      </plugin>
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>exec-maven-plugin</artifactId>
        <version>3.5.0</version>
        <configuration>
          <mainClass>{{packageName}}.App</mainClass>
        </configuration>
      </plugin>
    </plugins>
  </build>

  <profiles>
    <profile>
      <id>quality</id>
      <build>
        <plugins>
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-checkstyle-plugin</artifactId>
            <version>3.6.0</version>
            <configuration>
              <configLocation>config/checkstyle.xml</configLocation>
              <consoleOutput>true</consoleOutput>
              <failOnViolation>true</failOnViolation>
              <violationSeverity>warning</violationSeverity>
              <includeTestSourceDirectory>true</includeTestSourceDirectory>
            </configuration>
            <executions>
              <execution>
                <id>checkstyle</id>
                <phase>verify</phase>
                <goals>
                  <goal>check</goal>
                </goals>
              </execution>
            </executions>
          </plugin>
        </plugins>
      </build>
    </profile>
  </profiles>
</project>
