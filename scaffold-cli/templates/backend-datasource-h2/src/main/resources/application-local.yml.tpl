spring:
  config:
    activate:
      on-profile: local
  datasource:
    url: jdbc:h2:mem:{{backendArtifactId}};MODE=PostgreSQL;DB_CLOSE_DELAY=-1
    username: sa
    password: ""
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true
      path: /h2-console
