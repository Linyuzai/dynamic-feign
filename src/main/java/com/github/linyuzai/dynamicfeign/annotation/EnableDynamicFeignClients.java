package com.github.linyuzai.dynamicfeign.annotation;

import com.github.linyuzai.dynamicfeign.concat.UrlConcat;
import com.github.linyuzai.dynamicfeign.controller.DynamicFeignController;
import com.github.linyuzai.dynamicfeign.initializer.AnnotationDynamicFeignInitializer;
import com.github.linyuzai.dynamicfeign.initializer.DynamicFeignInitializer;
import com.github.linyuzai.dynamicfeign.register.DynamicFeignClientsRegistrar;
import com.github.linyuzai.dynamicfeign.wrapper.DecoderWrapper;
import com.github.linyuzai.dynamicfeign.wrapper.EncoderWrapper;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import({DynamicFeignClientsRegistrar.class, DynamicFeignController.class})
public @interface EnableDynamicFeignClients {

    /**
     * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation
     * declarations e.g.: {@code @ComponentScan("org.my.pkg")} instead of
     * {@code @ComponentScan(basePackages="org.my.pkg")}.
     *
     * @return the array of 'basePackages'.
     */
    String[] value() default {};

    /**
     * Base packages to scan for annotated components.
     * <p>
     * {@link #value()} is an alias for (and mutually exclusive with) this attribute.
     * <p>
     * Use {@link #basePackageClasses()} for a type-safe alternative to String-based
     * package names.
     *
     * @return the array of 'basePackages'.
     */
    String[] basePackages() default {};

    /**
     * Type-safe alternative to {@link #basePackages()} for specifying the packages to
     * scan for annotated components. The package of each class specified will be scanned.
     * <p>
     * Consider creating a special no-op marker class or interface in each package that
     * serves no purpose other than being referenced by this attribute.
     *
     * @return the array of 'basePackageClasses'.
     */
    Class<?>[] basePackageClasses() default {};

    /**
     * A custom <code>@Configuration</code> for all feign clients. Can contain override
     * <code>@Bean</code> definition for the pieces that make up the client, for instance
     * {@link feign.codec.Decoder}, {@link feign.codec.Encoder}, {@link feign.Contract}.
     *
     * @see FeignClientsConfiguration for the defaults
     */
    Class<?>[] defaultConfiguration() default {};

    /**
     * List of classes annotated with @FeignClient. If not empty, disables classpath scanning.
     *
     * @return list of FeignClient classes
     */
    Class<?>[] clients() default {};

    @ProfilePrimary
    String outUrl() default "";

    @ProfilePrimary
    UrlConcat urlConcat() default UrlConcat.SERVICE_LOWER_CASE;

    @ProfilePrimary
    boolean feignOut() default false;

    @ProfilePrimary
    boolean feignMethod() default false;

    @ProfilePrimary
    Class<? extends DecoderWrapper> decoderWrapper() default DecoderWrapper.Default.class;

    @ProfilePrimary
    Class<? extends EncoderWrapper> encoderWrapper() default EncoderWrapper.Default.class;

    @ProfilePrimary
    Class<? extends DynamicFeignInitializer> initializer() default AnnotationDynamicFeignInitializer.class;

    DynamicFeignInitialization[] annotationInitializer() default {};
}
