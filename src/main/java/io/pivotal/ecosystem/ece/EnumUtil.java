package io.pivotal.ecosystem.ece;

import java.util.*;

abstract class EnumUtil {

    private static final List<String> configKeys = EnumUtil.getEnumNames(ClusterConfig.eceApiKeys.class);
    private static final List<String> kibanaKeys = EnumUtil.getEnumNames(KibanaConfig.kibanaApiKeys.class);
    private static List<String> getEnumNames(Class<? extends Enum<?>> e) {
        return Arrays.asList(Arrays.toString(e.getEnumConstants()).replaceAll("^.|.$", "").split(", "));
    }

    static Map<String, String> paramsToKibanaParams(Map<String, Object> params) {

        @SuppressWarnings("unchecked")
        Map<String, Object> kibanaConfigParams = (Map<String, Object>) params.get(KibanaConfig.KIBANA);
        EnumMap<KibanaConfig.kibanaApiKeys, String> e = new EnumMap<>(KibanaConfig.kibanaApiKeys.class);

        if (kibanaConfigParams != null) {
            for (String key : kibanaConfigParams.keySet()) {
                if (kibanaKeys.contains(key)) {
                    e.put(KibanaConfig.kibanaApiKeys.valueOf(key), kibanaConfigParams.get(key).toString());
                }
            }
        }
        return enumsToParams(e);
    }

    static Map<String, String> paramsToClusterConfigParams(Map<String, Object> params) {

        @SuppressWarnings("unchecked")
        Map<String, Object> clusterConfigParams = (Map<String, Object>) params.get(ClusterConfig.ELASTIC_SEARCH);
        EnumMap<ClusterConfig.eceApiKeys, String> e = new EnumMap<>(ClusterConfig.eceApiKeys.class);

        if (clusterConfigParams != null) {
            for (String key : clusterConfigParams.keySet()) {
                if (configKeys.contains(key)) {
                    e.put(ClusterConfig.eceApiKeys.valueOf(key), clusterConfigParams.get(key).toString());
                }
            }
        }
        return enumsToParams(e);
    }

    private static Map<String, String> enumsToParams(EnumMap<?, String> enumMap) {
        Map<String, String> m = new HashMap<>();
        for (Enum<?> e : enumMap.keySet()) {
            m.put(e.name(), enumMap.get(e));
        }

        return m;
    }
}
