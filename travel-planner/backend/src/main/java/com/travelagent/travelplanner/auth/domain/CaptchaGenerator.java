package com.travelagent.travelplanner.auth.domain;

/** 人机验证题生成器。 */
public interface CaptchaGenerator {

    /**
     * 生成挑战。
     *
     * @return 验证码文本和图片
     */
    GeneratedCaptcha generate();

    /**
     * 生成结果。
     *
     * @param answer 验证码文本
     * @param imageDataUrl SVG 图片数据地址
     */
    record GeneratedCaptcha(String answer, String imageDataUrl) {
    }
}
