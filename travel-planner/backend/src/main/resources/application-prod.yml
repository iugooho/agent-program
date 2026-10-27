spring:
  config:
    activate:
      on-profile: prod
  datasource:
    # MySQL 8。生产建议开启 TLS：useSSL=true 并配置证书；allowPublicKeyRetrieval 仅用于本地/内网。
    url: "${DB_URL:jdbc:mysql://localhost:3306/travel?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true}"
    username: ${DB_USERNAME:travel}
    password: ${DB_PASSWORD:travel}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
