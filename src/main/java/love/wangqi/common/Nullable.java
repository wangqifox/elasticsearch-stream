package love.wangqi.common;

import java.lang.annotation.*;

/**
 * @author: wangqi
 * @description:
 * @Version:
 * @date: Created in 2019/12/31 1:41 下午
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD})
public @interface Nullable {
}
