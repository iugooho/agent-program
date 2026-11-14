package com.travelagent.travelplanner.auth.infrastructure.persistence;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** user_preferences 表 Mapper。 */
@Mapper
public interface UserPreferencesMapper {

    /**
     * 查询用户偏好。
     *
     * @param userId 用户主键
     * @return 偏好行
     */
    @Select("""
            SELECT user_id AS userId,
                   travel_style_default AS travelStyleDefault,
                   food_habit AS foodHabit,
                   forbidden_list AS forbiddenList,
                   accommodation_preference AS accommodationPreference,
                   budget_level AS budgetLevel,
                   update_time AS updateTime
            FROM user_preferences
            WHERE user_id = #{userId}
            LIMIT 1
            """)
    UserPreferencesRow findByUserId(@Param("userId") long userId);

    /**
     * 新增偏好。
     *
     * @param userId 用户主键
     * @param travelStyleDefault 默认风格
     * @param foodHabit 饮食偏好
     * @param forbiddenList 禁忌 JSON
     * @param accommodationPreference 住宿偏好
     * @param budgetLevel 预算等级
     * @return 新增行数
     */
    @Insert("""
            INSERT INTO user_preferences (
                user_id,
                travel_style_default,
                food_habit,
                forbidden_list,
                accommodation_preference,
                budget_level
            ) VALUES (
                #{userId},
                #{travelStyleDefault},
                #{foodHabit},
                #{forbiddenList},
                #{accommodationPreference},
                #{budgetLevel}
            )
            """)
    int insert(
            @Param("userId") long userId,
            @Param("travelStyleDefault") String travelStyleDefault,
            @Param("foodHabit") String foodHabit,
            @Param("forbiddenList") String forbiddenList,
            @Param("accommodationPreference") String accommodationPreference,
            @Param("budgetLevel") String budgetLevel);

    /**
     * 更新偏好。
     *
     * @param userId 用户主键
     * @param travelStyleDefault 默认风格
     * @param foodHabit 饮食偏好
     * @param forbiddenList 禁忌 JSON
     * @param accommodationPreference 住宿偏好
     * @param budgetLevel 预算等级
     * @return 更新行数
     */
    @Update("""
            UPDATE user_preferences
            SET travel_style_default = #{travelStyleDefault},
                food_habit = #{foodHabit},
                forbidden_list = #{forbiddenList},
                accommodation_preference = #{accommodationPreference},
                budget_level = #{budgetLevel},
                update_time = CURRENT_TIMESTAMP(6)
            WHERE user_id = #{userId}
            """)
    int update(
            @Param("userId") long userId,
            @Param("travelStyleDefault") String travelStyleDefault,
            @Param("foodHabit") String foodHabit,
            @Param("forbiddenList") String forbiddenList,
            @Param("accommodationPreference") String accommodationPreference,
            @Param("budgetLevel") String budgetLevel);
}
