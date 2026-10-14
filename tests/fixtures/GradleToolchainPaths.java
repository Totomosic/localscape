import java.io.File;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.provider.Providers;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.cli.CommandLineParser;
import org.gradle.cli.ProjectPropertiesCommandLineConverter;
import org.gradle.cli.SystemPropertiesCommandLineConverter;
import org.gradle.jvm.toolchain.internal.LocationListInstallationSupplier;

/** Exercise Gradle's real CLI parser and installation supplier without a daemon. */
public class GradleToolchainPaths {
    public static void main(String[] args) {
        CommandLineParser parser = new CommandLineParser();
        ProjectPropertiesCommandLineConverter projectConverter = new ProjectPropertiesCommandLineConverter();
        projectConverter.configure(parser);
        new SystemPropertiesCommandLineConverter().configure(parser);
        Map<String, String> properties = projectConverter.convert(parser.parse(args), new HashMap<>());

        ProviderFactory providers = (ProviderFactory) Proxy.newProxyInstance(
            ProviderFactory.class.getClassLoader(), new Class<?>[] {ProviderFactory.class},
            (proxy, method, arguments) -> {
                if (method.getName().equals("gradleProperty")) {
                    return Providers.ofNullable(properties.get(arguments[0]));
                }
                throw new UnsupportedOperationException(method.getName());
            }
        );
        FileResolver files = (FileResolver) Proxy.newProxyInstance(
            FileResolver.class.getClassLoader(), new Class<?>[] {FileResolver.class},
            (proxy, method, arguments) -> {
                if (method.getName().equals("resolve")) {
                    return new File(arguments[0].toString());
                }
                throw new UnsupportedOperationException(method.getName());
            }
        );
        new LocationListInstallationSupplier(providers, files).get().stream()
            .map(location -> location.getLocation().getAbsolutePath())
            .sorted()
            .forEach(System.out::println);
    }
}
