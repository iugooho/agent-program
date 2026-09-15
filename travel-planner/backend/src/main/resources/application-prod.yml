spring:
  config:
    activate:
      on-profile: prod
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/travel}
    username: ${DB_USERNAME:travel}
    password: ${DB_PASSWORD:travel}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
