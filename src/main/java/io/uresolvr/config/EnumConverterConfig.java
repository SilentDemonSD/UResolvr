package io.uresolvr.config;

import io.uresolvr.domain.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.H2Dialect;

import java.util.List;

/**
 * R2DBC enum converters — H2 and PostgreSQL store enums as VARCHAR,
 * these converters handle the String ↔ Enum mapping.
 */
@Configuration
public class EnumConverterConfig {

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        return R2dbcCustomConversions.of(H2Dialect.INSTANCE, List.of(
            new RouteStatusReadConverter(),
            new RouteStatusWriteConverter(),
            new TenantPlanReadConverter(),
            new TenantPlanWriteConverter(),
            new TenantStatusReadConverter(),
            new TenantStatusWriteConverter(),
            new TenantRoleReadConverter(),
            new TenantRoleWriteConverter(),
            new ResolutionOutcomeReadConverter(),
            new ResolutionOutcomeWriteConverter()
        ));
    }

    @ReadingConverter
    static class RouteStatusReadConverter implements Converter<String, RouteStatus> {
        @Override public RouteStatus convert(String source) { return RouteStatus.valueOf(source); }
    }
    @WritingConverter
    static class RouteStatusWriteConverter implements Converter<RouteStatus, String> {
        @Override public String convert(RouteStatus source) { return source.name(); }
    }

    @ReadingConverter
    static class TenantPlanReadConverter implements Converter<String, TenantPlan> {
        @Override public TenantPlan convert(String source) { return TenantPlan.valueOf(source); }
    }
    @WritingConverter
    static class TenantPlanWriteConverter implements Converter<TenantPlan, String> {
        @Override public String convert(TenantPlan source) { return source.name(); }
    }

    @ReadingConverter
    static class TenantStatusReadConverter implements Converter<String, TenantStatus> {
        @Override public TenantStatus convert(String source) { return TenantStatus.valueOf(source); }
    }
    @WritingConverter
    static class TenantStatusWriteConverter implements Converter<TenantStatus, String> {
        @Override public String convert(TenantStatus source) { return source.name(); }
    }

    @ReadingConverter
    static class TenantRoleReadConverter implements Converter<String, TenantRole> {
        @Override public TenantRole convert(String source) { return TenantRole.valueOf(source); }
    }
    @WritingConverter
    static class TenantRoleWriteConverter implements Converter<TenantRole, String> {
        @Override public String convert(TenantRole source) { return source.name(); }
    }

    @ReadingConverter
    static class ResolutionOutcomeReadConverter implements Converter<String, ResolutionOutcome> {
        @Override public ResolutionOutcome convert(String source) { return ResolutionOutcome.valueOf(source); }
    }
    @WritingConverter
    static class ResolutionOutcomeWriteConverter implements Converter<ResolutionOutcome, String> {
        @Override public String convert(ResolutionOutcome source) { return source.name(); }
    }
}
