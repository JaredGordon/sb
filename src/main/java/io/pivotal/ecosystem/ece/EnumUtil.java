package io.pivotal.ecosystem.ece;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class EnumUtil {

    private static final List<String> credKeys = EnumUtil.getEnumNames(ClusterConfig.credentialKeys.class);
    private static final List<String> configKeys = EnumUtil.getEnumNames(ClusterConfig.eceApiKeys.class);
    private static final List<String> kibanaKeys = EnumUtil.getEnumNames(KibanaConfig.kibanaApiKeys.class);

    static List<String> getEnumNames(Class<? extends Enum<?>> e) {
        return Arrays.asList(Arrays.toString(e.getEnumConstants()).replaceAll("^.|.$", "").split(", "));
    }

    //generics experts could do better than this...
    static EnumMap<ClusterConfig.credentialKeys, String> paramsToCreds(Map<String, Object> parameters) {
        EnumMap<ClusterConfig.credentialKeys, String> e = new EnumMap<>(ClusterConfig.credentialKeys.class);
        for (String key : parameters.keySet()) {
            if (credKeys.contains(key)) {
                e.put(ClusterConfig.credentialKeys.valueOf(key), parameters.get(key).toString());
            }
        }
        return e;
    }

    static EnumMap<ClusterConfig.eceApiKeys, String> paramsToConfig(Map<String, Object> parameters) {
        EnumMap<ClusterConfig.eceApiKeys, String> e = new EnumMap<>(ClusterConfig.eceApiKeys.class);
        for (String key : parameters.keySet()) {
            if (configKeys.contains(key)) {
                e.put(ClusterConfig.eceApiKeys.valueOf(key), parameters.get(key).toString());
            }
        }
        return e;
    }

    static EnumMap<KibanaConfig.kibanaApiKeys, String> paramsToKibanaConfig(Map<String, Object> parameters) {
        EnumMap<KibanaConfig.kibanaApiKeys, String> e = new EnumMap<>(KibanaConfig.kibanaApiKeys.class);
        for (String key : parameters.keySet()) {
            if (kibanaKeys.contains(key)) {
                e.put(KibanaConfig.kibanaApiKeys.valueOf(key), parameters.get(key).toString());
            }
        }
        return e;
    }
}
