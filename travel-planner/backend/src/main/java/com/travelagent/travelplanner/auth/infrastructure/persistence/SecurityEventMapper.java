package com.travelagent.travelplanner.auth.infrastructure.persistence;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** auth_security_event 表 Mapper。 */
@Mapper
public interface SecurityEventMapper {

    /**
     * 写入安全事件。
     *
     * @param userId 用户主键
     * @param eventType 事件类型
     * @param description 描述
     * @param clientIp 客户端 IP
     * @param userAgent 客户端标识
     * @return 插入行数
     */
    @Insert("""
            INSERT INTO auth_security_event (
                user_id, event_type, description, client_ip, user_agent
            ) VALUES (
                #{userId}, #{eventType}, #{description}, #{clientIp}, #{userAgent}
            )
            """)
    int insert(
            @Param("userId") long userId,
            @Param("eventType") String eventType,
            @Param("description") String description,
            @Param("clientIp") String clientIp,
            @Param("userAgent") String userAgent);

    /**
     * 查询最近安全事件。
     *
     * @param userId 用户主键
     * @param limit 最大条数
     * @return 事件行
     */
    @Select("""
            SELECT id, user_id AS userId, event_type AS eventType,
                   description, client_ip AS clientIp, user_agent AS userAgent,
                   create_time AS createTime
            FROM auth_security_event
            WHERE user_id = #{userId}
            ORDER BY create_time DESC, id DESC
            LIMIT #{limit}
            """)
    List<SecurityEventRow> findRecent(
            @Param("userId") long userId,
            @Param("limit") int limit);
}
