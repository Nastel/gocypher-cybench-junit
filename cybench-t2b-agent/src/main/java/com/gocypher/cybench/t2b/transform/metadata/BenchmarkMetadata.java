package com.gocypher.cybench.t2b.transform.metadata;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openjdk.jmh.generators.core.ClassInfo;
import org.openjdk.jmh.generators.core.MetadataInfo;
import org.openjdk.jmh.generators.core.MethodInfo;

import com.gocypher.cybench.Test2Benchmark;
import com.gocypher.cybench.t2b.transform.T2BClassTransformer;

public class BenchmarkMetadata {

    private static final String SYS_PROP_METADATA_CONFIG = "t2b.metadata.cfg.path";
    private static final String DEFAULT_METADATA_CONFIG_PATH = "config/metadata.properties";
    private static String configPath = System.getProperty(SYS_PROP_METADATA_CONFIG, DEFAULT_METADATA_CONFIG_PATH);

    private static final Pattern VARIABLE_EXP_PATTERN = Pattern.compile("(\\$\\{(.[^:]*)\\})+[:]*(\\{(.[^:]*)\\})*");
    private static final Pattern VARIABLE_EXP_RANGE_PATTERN = Pattern.compile("(\\$\\{.+\\})");

    private static final Pattern VARIABLE_METHOD_PATTERN = Pattern.compile("method\\.(.[^:]+)");
    private static final Pattern VARIABLE_CLASS_PATTERN = Pattern.compile("class\\.(.[^:]+)");
    private static final Pattern VARIABLE_PACKAGE_PATTERN = Pattern.compile("package\\.(.[^:]+)");
    private static final Pattern VARIABLE_SYS_PROP_PATTERN = Pattern.compile("sys#(.[^:]+)");
    private static final Pattern VARIABLE_ENV_VAR_PATTERN = Pattern.compile("env#(.[^:]+)");

    private static final Pattern VARIABLE_EXP_METHOD_PATTERN = Pattern.compile("\\$\\{method\\.(.[^:]+)\\}");

    private static final String CLASS_METADATA_MAP_KEY = "class";
    private static final String METHOD_METADATA_MAP_KEY = "method";
    private static final String CLASS_METADATA_PROPS_PREFIX = "class.";
    private static final String METHOD_METADATA_PROPS_PREFIX = "method.";

    private static final String FALLBACK_VALUE = "-";

    private static Map<String, Map<String, String>> metadataConfig = new HashMap<>(2);

    static {
        loadConfig(configPath);
    }

    private BenchmarkMetadata() {
    }

    protected static void loadConfig(String cfgPath) {
        Properties metaDataCfgProps = new Properties();
        if (new File(cfgPath).exists()) {
            try (Reader rdr = new BufferedReader(new FileReader(cfgPath))) {
                metaDataCfgProps.load(rdr);
            } catch (IOException exc) {
                Test2Benchmark.err("failed to load metadata config from: " + cfgPath, exc);
            }
        } else {
            String cfgProp = System.getProperty(SYS_PROP_METADATA_CONFIG);
            if (cfgProp != null) {
                Test2Benchmark.warn("system property " + SYS_PROP_METADATA_CONFIG
                        + " defined aggregations configuration file " + cfgPath + " not found!");
            } else {
                Test2Benchmark.log("default metadata configuration file " + cfgPath + " not found!");
            }
        }

        metadataConfig.put(CLASS_METADATA_MAP_KEY, new HashMap<>());
        metadataConfig.put(METHOD_METADATA_MAP_KEY, new HashMap<>());

        for (Map.Entry<?, ?> mdProp : metaDataCfgProps.entrySet()) {
            String mdpKey = (String) mdProp.getKey();
            String mdpValue = (String) mdProp.getValue();

            if (mdpKey.startsWith(CLASS_METADATA_PROPS_PREFIX)) {
                if (isClassMetadataPropValid(mdpValue)) {
                    metadataConfig.get(CLASS_METADATA_MAP_KEY)
                            .put(mdpKey.substring(CLASS_METADATA_PROPS_PREFIX.length()), mdpValue);
                } else {
                    Test2Benchmark.warn("found invalid metadata configuration property for " + CLASS_METADATA_MAP_KEY
                            + " scope: " + mdpKey + "=" + mdpValue);
                }
            } else if (mdpKey.startsWith(METHOD_METADATA_PROPS_PREFIX)) {
                metadataConfig.get(METHOD_METADATA_MAP_KEY).put(mdpKey.substring(METHOD_METADATA_PROPS_PREFIX.length()),
                        mdpValue);
            } else {
                metadataConfig.get(METHOD_METADATA_MAP_KEY).put(mdpKey, mdpValue);
            }
        }
    }

    protected static boolean isClassMetadataPropValid(String propValue) {
        return !VARIABLE_EXP_METHOD_PATTERN.matcher(propValue).matches();
    }

    public static boolean isClassMetadataEmpty() {
        return isMetadataEmpty(CLASS_METADATA_MAP_KEY);
    }

    public static boolean isMethodMetadataEmpty() {
        return isMetadataEmpty(METHOD_METADATA_MAP_KEY);
    }

    public static boolean isMetadataEmpty(String scope) {
        Map<?, ?> cMap = getMetadata(scope);

        return cMap == null || cMap.isEmpty();
    }

    public static Map<String, String> getClassMetadata() {
        return getMetadata(CLASS_METADATA_MAP_KEY);
    }

    public static Map<String, String> getMethodMetadata() {
        return getMetadata(METHOD_METADATA_MAP_KEY);
    }

    public static Map<String, String> getMetadata(String scope) {
        if (metadataConfig == null) {
            return null;
        }

        return metadataConfig.get(scope);
    }

    public static Map<String, String> fillMetadata(MetadataInfo metadataInfo) {
        Map<String, String> metadataCfg = metadataInfo instanceof ClassInfo ? getClassMetadata() : getMethodMetadata();
        Map<String, String> metaDataMap = new HashMap<>(metadataCfg.size());

        for (Map.Entry<String, String> cfgEntry : metadataCfg.entrySet()) {
            metaDataMap.put(cfgEntry.getKey(), fillMetadataValue(cfgEntry.getValue(), metadataInfo));
        }

        return metaDataMap;
    }

    protected static String fillMetadataValue(String value, MetadataInfo metadataInfo) {
        if (isVariableValue(value)) {
            return VARIABLE_EXP_RANGE_PATTERN.matcher(value).replaceAll(resolveVarExpressionValue(value, metadataInfo));
        }

        return value;
    }

    protected static boolean isVariableValue(String value) {
        return VARIABLE_EXP_PATTERN.matcher(value).find();
    }

    protected static String resolveVarExpressionValue(String value, MetadataInfo metadataInfo) {
        Matcher vMatcher = VARIABLE_EXP_PATTERN.matcher(value);

        while (vMatcher.find()) {
            String varName = vMatcher.group(2);
            String defaultValue = vMatcher.group(4);

            String varValue = resolveVarValue(varName, metadataInfo);
            if (varValue == null && defaultValue != null) {
                varValue = defaultValue;
            }

            if (varValue != null) {
                return varValue;
            }
        }

        return FALLBACK_VALUE;
    }

    protected static String resolveVarValue(String variable, MetadataInfo metadataInfo) {
        try {
            String varValue = null;
            if (VARIABLE_SYS_PROP_PATTERN.matcher(variable).matches()
                    || VARIABLE_ENV_VAR_PATTERN.matcher(variable).matches()) {
                varValue = EnvValueResolver.getValue(variable);
            } else if (VARIABLE_CLASS_PATTERN.matcher(variable).matches()) {
                ClassInfo classInfo;
                if (metadataInfo instanceof ClassInfo) {
                    classInfo = (ClassInfo) metadataInfo;
                } else if (metadataInfo instanceof MetadataInfo) {
                    classInfo = ((MethodInfo) metadataInfo).getDeclaringClass();
                } else {
                    Test2Benchmark.warn("unknown class metadata entity found: " + metadataInfo.getClass().getName());
                    classInfo = null;
                }
                if (classInfo != null) {
                    varValue = ClassValueResolver.getValue(variable, classInfo);
                }
            } else if (VARIABLE_METHOD_PATTERN.matcher(variable).matches()) {
                MethodInfo methodInfo;
                if (metadataInfo instanceof MethodInfo) {
                    methodInfo = (MethodInfo) metadataInfo;
                } else {
                    Test2Benchmark.warn("unknown method metadata entity found: " + metadataInfo.getClass().getName());
                    methodInfo = null;
                }
                if (methodInfo != null) {
                    varValue = MethodValueResolver.getValue(variable, methodInfo);
                }
            } else if (VARIABLE_PACKAGE_PATTERN.matcher(variable).matches()) {
                Package pckg = null;
                if (metadataInfo instanceof ClassInfo) {
                    pckg = T2BClassTransformer.getClass((ClassInfo) metadataInfo).getPackage();
                } else if (metadataInfo instanceof MetadataInfo) {
                    pckg = T2BClassTransformer.getClass(((MethodInfo) metadataInfo).getDeclaringClass()).getPackage();
                } else {
                    Test2Benchmark.warn("unknown package metadata entity found: " + metadataInfo.getClass().getName());
                    pckg = null;
                }

                if (pckg != null) {
                    varValue = PackageValueResolver.getValue(variable, pckg);
                }
            } else {
                throw new InvalidVariableException("Unknown variable: " + variable);
            }

            return varValue;
        } catch (InvalidVariableException exc) {
            Test2Benchmark.warn(exc.getLocalizedMessage());
        } catch (Exception exc) {
            Test2Benchmark.err("failed to resolve variable value for: " + variable, exc);
        }
        return null;
    }

    static class EnvValueResolver {
        static final String SYS_PROP_VAR = "sys#";
        static final String ENV_VAR = "env#";

        static String getValue(String variable) {
            if (variable.startsWith(SYS_PROP_VAR)) {
                String propName = variable.substring(SYS_PROP_VAR.length());

                return System.getProperty(propName);
            } else if (variable.startsWith(ENV_VAR)) {
                String varName = variable.substring(ENV_VAR.length());

                return System.getenv(varName);
            }

            return null;
        }
    }

    static class PackageValueResolver {
        static final String VAR_PREFIX = "package.";
        static final String VAR_NAME = VAR_PREFIX + "name";
        static final String VAR_VERSION = VAR_PREFIX + "version";
        static final String VAR_TITLE = VAR_PREFIX + "title";
        static final String VAR_VENDOR = VAR_PREFIX + "vendor";
        static final String VAR_SPEC_VERSION = VAR_PREFIX + "spec.version";
        static final String VAR_SPEC_TITLE = VAR_PREFIX + "spec.title";
        static final String VAR_SPEC_VENDOR = VAR_PREFIX + "spec.vendor";

        static String getValue(String variable, Package pckg) {
            switch (variable) {
            case VAR_NAME:
                return pckg.getName();
            case VAR_VERSION:
                return pckg.getImplementationVersion();
            case VAR_TITLE:
                return pckg.getImplementationTitle();
            case VAR_VENDOR:
                return pckg.getImplementationVendor();
            case VAR_SPEC_VERSION:
                return pckg.getSpecificationVersion();
            case VAR_SPEC_TITLE:
                return pckg.getSpecificationTitle();
            case VAR_SPEC_VENDOR:
                return pckg.getSpecificationVendor();
            default:
                throw new InvalidVariableException("Unknown PACKAGE scope variable: " + variable);
            }
        }
    }

    static class MethodValueResolver {
        static final String VAR_PREFIX = "method.";
        static final String VAR_NAME = VAR_PREFIX + "name";
        static final String VAR_SIGNATURE = VAR_PREFIX + "signature";
        static final String VAR_CLASS = VAR_PREFIX + "class";
        static final String VAR_RETURN_TYPE = VAR_PREFIX + "return.type";
        static final String VAR_QUALIFIED_NAME = VAR_PREFIX + "qualified.name";
        static final String VAR_PARAMETERS = VAR_PREFIX + "parameters";

        static String getValue(String variable, MethodInfo methodInfo) {
            switch (variable) {
            case VAR_NAME:
                return methodInfo.getName();
            case VAR_SIGNATURE:
                return T2BClassTransformer.getSignature(methodInfo);
            case VAR_CLASS:
                return methodInfo.getDeclaringClass().getQualifiedName();
            case VAR_RETURN_TYPE:
                return methodInfo.getReturnType();
            case VAR_QUALIFIED_NAME:
                return methodInfo.getQualifiedName();
            case VAR_PARAMETERS:
                return methodInfo.getParameters().toString();
            default:
                throw new InvalidVariableException("Unknown METHOD scope variable: " + variable);
            }
        }
    }

    static class ClassValueResolver {
        static final String VAR_PREFIX = "class.";
        static final String VAR_NAME = VAR_PREFIX + "name";
        static final String VAR_QUALIFIED_NAME = VAR_PREFIX + "qualified.name";
        static final String VAR_PACKAGE = VAR_PREFIX + "package";
        static final String VAR_SUPER_CLASS = VAR_PREFIX + "super";

        static String getValue(String variable, ClassInfo classInfo) {
            switch (variable) {
            case VAR_NAME:
                return classInfo.getName();
            case VAR_QUALIFIED_NAME:
                return classInfo.getQualifiedName();
            case VAR_PACKAGE:
                return classInfo.getPackageName();
            case VAR_SUPER_CLASS:
                return classInfo.getSuperClass().getQualifiedName();
            default:
                throw new InvalidVariableException("Unknown CLASS scope variable: " + variable);
            }
        }
    }
}
