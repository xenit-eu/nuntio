package be.vbgn.nuntio.platform.docker.config.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import be.vbgn.nuntio.api.platform.ServiceBinding;
import be.vbgn.nuntio.platform.docker.config.parser.ParsedServiceConfiguration.ConfigurationKind;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NuntioLabelsParserTest {

    private final NuntioLabelsParser labelsParser = new NuntioLabelsParser("nuntio");

    private ParsedServiceConfiguration getParsedLabelFrom(String label) {

        Map<String, String> labels = Map.of(label, "value1", "unrelated-label", "def");

        Map<ParsedServiceConfiguration, String> parsedLabels = labelsParser.parseContainerMetadata(new SimpleContainerMetadata(Collections.emptyMap(), labels));

        return parsedLabels.keySet().stream().findFirst().get();
    }

    @Test
    void parseLabelsSimpleAny() {
        ParsedServiceConfiguration parsedLabel = getParsedLabelFrom("nuntio/service");

        assertEquals(ServiceBinding.ANY, parsedLabel.getBinding());
        assertEquals(ConfigurationKind.SERVICE, parsedLabel.getConfigurationKind());
        assertNull(parsedLabel.getAdditional());
    }

    @Test
    void parseLabelsAdditionalAny() {
        ParsedServiceConfiguration parsedLabel = getParsedLabelFrom("nuntio/metadata/some-metadata");

        assertEquals(ServiceBinding.ANY, parsedLabel.getBinding());
        assertEquals(ConfigurationKind.METADATA, parsedLabel.getConfigurationKind());
        assertEquals("some-metadata", parsedLabel.getAdditional());
    }

    @Test
    void parseLabelsSimplePort() {
        ParsedServiceConfiguration parsedLabel = getParsedLabelFrom("nuntio/80/service");

        assertEquals(ServiceBinding.fromPort(80), parsedLabel.getBinding());
        assertEquals(ConfigurationKind.SERVICE, parsedLabel.getConfigurationKind());
        assertNull(parsedLabel.getAdditional());
    }

    @Test
    void parseLabelsAdditionalPort() {
        ParsedServiceConfiguration parsedLabel = getParsedLabelFrom("nuntio/80/metadata/some-metadata");

        assertEquals(ServiceBinding.fromPort(80), parsedLabel.getBinding());
        assertEquals(ConfigurationKind.METADATA, parsedLabel.getConfigurationKind());
        assertEquals("some-metadata", parsedLabel.getAdditional());
    }

    @Test
    void parseLabelsSimplePortProtocol() {
        ParsedServiceConfiguration parsedLabel = getParsedLabelFrom("nuntio/udp:80/service");

        assertEquals(ServiceBinding.fromPortAndProtocol(80, "udp"), parsedLabel.getBinding());
        assertEquals(ConfigurationKind.SERVICE, parsedLabel.getConfigurationKind());
        assertNull(parsedLabel.getAdditional());
    }

    @Test
    void parseLabelsAdditionalPortProtocol() {
        ParsedServiceConfiguration parsedLabel = getParsedLabelFrom("nuntio/udp:80/metadata/some-metadata");

        assertEquals(ServiceBinding.fromPortAndProtocol(80, "udp"), parsedLabel.getBinding());
        assertEquals(ConfigurationKind.METADATA, parsedLabel.getConfigurationKind());
        assertEquals("some-metadata", parsedLabel.getAdditional());
    }

    @Test
    void parseLabelsUnknownKind() {
        Map<String, String> labels = Map.of("nuntio/8080/bla", "value1");

        Map<ParsedServiceConfiguration, String> parsedLabels = labelsParser.parseContainerMetadata(new SimpleContainerMetadata(Collections.emptyMap(), labels));

        assertEquals(Collections.emptyMap(), parsedLabels);
    }

}
