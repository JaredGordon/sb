package io.pivotal.ecosystem.ece;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
class EnumUtil {

    private static final List<String> configKeys = EnumUtil.getEnumNames(ClusterConfig.eceApiKeys.class);
    private static final List<String> kibanaKeys = EnumUtil.getEnumNames(KibanaConfig.kibanaApiKeys.class);

    private static List<String> getEnumNames(Class<? extends Enum<?>> e) {
        return Arrays.asList(Arrays.toString(e.getEnumConstants()).replaceAll("^.|.$", "").split(", "));
    }

    EnumMap<ClusterConfig.eceApiKeys, String> paramsToConfig(ServiceInstance instance) {
        EnumMap<ClusterConfig.eceApiKeys, String> e = new EnumMap<>(ClusterConfig.eceApiKeys.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) instance.getParameters().get(ClusterConfig.ELASTIC_SEARCH);

        if (parameters != null) {
            for (String key : parameters.keySet()) {
                if (configKeys.contains(key)) {
                    e.put(ClusterConfig.eceApiKeys.valueOf(key), parameters.get(key).toString());
                }
            }
        }
        return e;
    }

    EnumMap<KibanaConfig.kibanaApiKeys, String> paramsToKibanaConfig(ServiceInstance instance) {
        EnumMap<KibanaConfig.kibanaApiKeys, String> e = new EnumMap<>(KibanaConfig.kibanaApiKeys.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) instance.getParameters().get(KibanaConfig.KIBANA);

        if (parameters != null) {
            for (String key : parameters.keySet()) {
                if (kibanaKeys.contains(key)) {
                    e.put(KibanaConfig.kibanaApiKeys.valueOf(key), parameters.get(key).toString());
                }
            }
        }
        return e;
    }

    void enumsToParams(ClusterConfig config, ServiceInstance instance) {
        enumsToParams(config.getConfig(), ClusterConfig.ELASTIC_SEARCH, instance);
        enumsToParams(config.getCredentials(), ClusterConfig.CREDENTIALS, instance);
    }

    void enumsToParams(KibanaConfig config, ServiceInstance instance) {
        enumsToParams(config.getConfig(), KibanaConfig.KIBANA, instance);
    }

    void enumsToParams(EnumMap<?, String> config, String key, ServiceInstance instance) {
        if (!instance.getParameters().containsKey(key)) {
            instance.getParameters().put(key, new HashMap<String, Object>());
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) instance.getParameters().get(key);
        for (Enum<?> e : config.keySet()) {
            m.put(e.name(), config.get(e));
        }
    }
}
