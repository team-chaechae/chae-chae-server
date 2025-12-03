package com.project.chaechaeserver.application.global.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 배치 처리 메서드에 사용하는 어노테이션
 * 배치 처리 시작/종료 및 진행 상황을 로깅합니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BatchProcessing {

    /**
     * 배치 작업의 이름 (예: "입고", "판매", "재고 수정")
     */
    String value() default "";

    /**
     * 배치 사이즈 (기본값: 100)
     */
    int batchSize() default 100;
}