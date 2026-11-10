package com.travelagent.travelplanner.auth.infrastructure.cache;

import com.travelagent.travelplanner.auth.config.AuthProperties;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.net.ssl.SSLSocketFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 认证模块使用的轻量 RESP 客户端。
 *
 * <p>项目当前只依赖 Spring Boot 基础组件；这里直接使用 Redis RESP 协议完成验证码和会话撤销所需的
 * GET、SET、DEL 和 EVAL。每次命令创建短连接，适合当前低频认证场景。</p>
 */
@Component
@ConditionalOnProperty(name = "app.auth.redis.enabled", havingValue = "true")
public class SimpleRedisClient {

    private final AuthProperties.Redis properties;

    /**
     * 创建客户端。
     *
     * @param authProperties 认证配置
     */
    public SimpleRedisClient(AuthProperties authProperties) {
        this.properties = authProperties.redis();
        if (properties == null || properties.host() == null || properties.host().isBlank()) {
            throw new IllegalStateException("app.auth.redis.host 必须配置");
        }
        if (properties.port() < 1 || properties.port() > 65_535) {
            throw new IllegalStateException("app.auth.redis.port 必须在 1 到 65535 之间");
        }
        if (properties.database() < 0) {
            throw new IllegalStateException("app.auth.redis.database 不能小于 0");
        }
        if (properties.timeout() != null && (properties.timeout().isZero() || properties.timeout().isNegative())) {
            throw new IllegalStateException("app.auth.redis.timeout 必须大于 0");
        }
    }

    /**
     * 检查 Redis 连接和认证是否可用。
     */
    public void ping() {
        Object result = execute("PING");
        if (!"PONG".equals(result)) {
            throw new IllegalStateException("Redis PING 响应异常");
        }
    }

    /**
     * SET key value EX ttl NX。
     *
     * @param key 键
     * @param value 值
     * @param ttl 有效期
     * @return true 表示设置成功
     */
    public boolean setIfAbsent(String key, String value, Duration ttl) {
        Object result = execute(
                "SET", key, value, "EX", Long.toString(seconds(ttl)), "NX");
        return "OK".equals(result);
    }

    /**
     * SET key value EX ttl。
     *
     * @param key 键
     * @param value 值
     * @param ttl 有效期
     */
    public void set(String key, String value, Duration ttl) {
        execute("SET", key, value, "EX", Long.toString(seconds(ttl)));
    }

    /**
     * 是否存在。
     *
     * @param key 键
     * @return true 表示存在
     */
    public boolean exists(String key) {
        return ((Long) execute("EXISTS", key)) > 0;
    }

    /**
     * 读取字符串值。
     *
     * @param key 键
     * @return 值，不存在时返回 null
     */
    public String get(String key) {
        return (String) execute("GET", key);
    }

    /**
     * 比较并删除。
     *
     * @param key 键
     * @param expected 期待值
     * @return true 表示匹配并删除
     */
    public boolean compareAndDelete(String key, String expected) {
        String script = "local v=redis.call('GET',KEYS[1]);"
                + "if v==ARGV[1] then redis.call('DEL',KEYS[1]);return 1 else return 0 end";
        return ((Long) execute("EVAL", script, "1", key, expected)) > 0;
    }

    /**
     * 原子递增计数器，并在第一次创建时设置过期时间。
     *
     * @param key 键
     * @param ttl 有效期
     * @return 递增后的值
     */
    public long incrementAndExpire(String key, Duration ttl) {
        String script = "local n=redis.call('INCR',KEYS[1]);"
                + "if n==1 then redis.call('PEXPIRE',KEYS[1],ARGV[1]) end;"
                + "return n";
        return (Long) execute("EVAL", script, "1", key, Long.toString(ttl.toMillis()));
    }

    /**
     * 删除键。
     *
     * @param key 键
     */
    public void delete(String key) {
        execute("DEL", key);
    }

    private Object execute(String... command) {
        int timeoutMillis = timeoutMillis();
        try (Socket socket = openSocket(timeoutMillis)) {
            socket.setSoTimeout(timeoutMillis);
            InputStream input = new BufferedInputStream(socket.getInputStream());
            OutputStream output = new BufferedOutputStream(socket.getOutputStream());
            authenticate(input, output);
            writeCommand(output, command);
            output.flush();
            return readReply(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Redis 命令执行失败", exception);
        }
    }

    private Socket openSocket(int timeoutMillis) throws IOException {
        Socket socket;
        if (properties.ssl()) {
            socket = SSLSocketFactory.getDefault().createSocket();
        } else {
            socket = new Socket();
        }
        socket.connect(new InetSocketAddress(properties.host(), properties.port()), timeoutMillis);
        return socket;
    }

    private void authenticate(InputStream input, OutputStream output) throws IOException {
        if (properties.password() != null && !properties.password().isBlank()) {
            writeCommand(output, new String[] {"AUTH", properties.password()});
            output.flush();
            readReply(input);
        }
        if (properties.database() > 0) {
            writeCommand(output, new String[] {"SELECT", Integer.toString(properties.database())});
            output.flush();
            readReply(input);
        }
    }

    private void writeCommand(OutputStream output, String... parts) throws IOException {
        output.write(("*" + parts.length + "\r\n").getBytes(StandardCharsets.UTF_8));
        for (String part : parts) {
            byte[] bytes = part.getBytes(StandardCharsets.UTF_8);
            output.write(("$" + bytes.length + "\r\n").getBytes(StandardCharsets.UTF_8));
            output.write(bytes);
            output.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
    }

    private Object readReply(InputStream input) throws IOException {
        int prefix = input.read();
        if (prefix < 0) {
            throw new EOFException("Redis 连接提前关闭");
        }
        return switch (prefix) {
            case '+' -> readLine(input);
            case '-' -> throw new IllegalStateException("Redis 返回错误: " + readLine(input));
            case ':' -> Long.parseLong(readLine(input));
            case '$' -> readBulkString(input);
            case '*' -> readArray(input);
            default -> throw new IOException("未知 Redis 响应类型: " + (char) prefix);
        };
    }

    private String readBulkString(InputStream input) throws IOException {
        int length = Integer.parseInt(readLine(input));
        if (length < 0) {
            return null;
        }
        byte[] bytes = input.readNBytes(length);
        if (bytes.length != length) {
            throw new EOFException("Redis bulk string 不完整");
        }
        if (input.read() != '\r' || input.read() != '\n') {
            throw new IOException("Redis bulk string 缺少 CRLF");
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private Object[] readArray(InputStream input) throws IOException {
        int length = Integer.parseInt(readLine(input));
        if (length < 0) {
            return new Object[0];
        }
        Object[] values = new Object[length];
        for (int index = 0; index < length; index++) {
            values[index] = readReply(input);
        }
        return values;
    }

    private String readLine(InputStream input) throws IOException {
        StringBuilder value = new StringBuilder();
        int previous = -1;
        int current;
        while ((current = input.read()) >= 0) {
            if (previous == '\r' && current == '\n') {
                value.setLength(value.length() - 1);
                return value.toString();
            }
            value.append((char) current);
            previous = current;
        }
        throw new EOFException("Redis 行未结束");
    }

    private long seconds(Duration duration) {
        long millis = duration.toMillis();
        return Math.max(1, (millis + 999) / 1000);
    }

    private int timeoutMillis() {
        Duration timeout = properties.timeout();
        if (timeout == null) {
            return 2000;
        }
        long millis = Math.max(100, timeout.toMillis());
        return (int) Math.min(Integer.MAX_VALUE, millis);
    }
}
