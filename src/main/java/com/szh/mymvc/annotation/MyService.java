package com.szh.mymvc.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE) // 作用范围
@Retention(RetentionPolicy.RUNTIME) // 系统运行时通过反射获取信息
@Documented
//@Inherited 可被继承
public @interface MyService {

	String value() default "";
}
