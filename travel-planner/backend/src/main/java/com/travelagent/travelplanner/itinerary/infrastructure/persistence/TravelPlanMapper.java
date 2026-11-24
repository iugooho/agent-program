package com.travelagent.travelplanner.itinerary.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 旅行计划 Mapper。 */
@Mapper
public interface TravelPlanMapper {

    /**
     * 新增旅行计划。
     *
     * @param plan 待插入字段，成功后写入生成的主键
     * @return 插入行数
     */
    @Insert("""
            INSERT INTO travel_plan (
                owner_id, department_id, request_payload, itinerary_payload, mode
            )
            VALUES (
                #{ownerId}, #{departmentId}, #{requestPayload}, #{itineraryPayload}, #{mode}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Map<String, Object> plan);

    /**
     * 查询用户可见计划。
     *
     * @param userId 当前用户主键
     * @param allData 是否全部数据
     * @param selfAllowed 是否允许本人数据
     * @param departmentIds 可见部门主键
     * @return 计划行
     */
    @Select("""
            <script>
            SELECT id, owner_id AS ownerId, department_id AS departmentId,
                   request_payload AS requestPayload,
                   itinerary_payload AS itineraryPayload,
                   mode, create_time AS createTime
            FROM travel_plan
            <where>
                <if test="!allData">
                    <choose>
                        <when test="departmentIds != null and departmentIds.size() > 0">
                            (
                            <if test="selfAllowed">owner_id = #{userId} OR </if>
                            department_id IN
                            <foreach collection="departmentIds" item="departmentId"
                                     open="(" separator="," close=")">
                                #{departmentId}
                            </foreach>
                            )
                        </when>
                        <otherwise>
                            <choose>
                                <when test="selfAllowed">owner_id = #{userId}</when>
                                <otherwise>1 = 0</otherwise>
                            </choose>
                        </otherwise>
                    </choose>
                </if>
            </where>
            ORDER BY id DESC
            </script>
            """)
    List<TravelPlanRow> findVisible(
            @Param("userId") long userId,
            @Param("allData") boolean allData,
            @Param("selfAllowed") boolean selfAllowed,
            @Param("departmentIds") Set<Long> departmentIds);

    /**
     * 按主键查询计划。
     *
     * @param planId 计划主键
     * @return 计划行
     */
    @Select("""
            SELECT id, owner_id AS ownerId, department_id AS departmentId,
                   request_payload AS requestPayload,
                   itinerary_payload AS itineraryPayload,
                   mode, create_time AS createTime
            FROM travel_plan
            WHERE id = #{planId}
            LIMIT 1
            """)
    TravelPlanRow findById(@Param("planId") long planId);

    /**
     * 删除计划。
     *
     * @param planId 计划主键
     * @return 删除行数
     */
    @Delete("DELETE FROM travel_plan WHERE id = #{planId}")
    int delete(@Param("planId") long planId);
}
