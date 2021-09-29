package be.vbgn.nuntio.platform.docker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import be.vbgn.nuntio.api.platform.ServiceBinding;
import be.vbgn.nuntio.platform.docker.DockerLabelsParser.Label;
import be.vbgn.nuntio.platform.docker.DockerLabelsParser.ParsedLabel;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DockerLabelsParserTest {

    private final DockerLabelsParser labelsParser = new DockerLabelsParser("nuntio");

    private ParsedLabel getParsedLabelFrom(String label) {

        Map<String, String> labels = Map.of(label, "value1", "unrelated-label", "def");

        Map<ParsedLabel, String> parsedLabels = labelsParser.parseLabels(labels);

        return parsedLabels.keySet().stream().findFirst().get();
    }

    @Test
    void parseLabelsSimpleAny() {
        ParsedLabel parsedLabel = getParsedLabelFrom("nuntio/service");

        assertEquals(ServiceBinding.ANY, parsedLabel.getBinding());
        assertEquals(Label.SERVICE, parsedLabel.getLabelKind());
        assertNull(parsedLabel.getAdditional());
    }

    @Test
    void parseLabelsAdditionalAny() {
        ParsedLabel parsedLabel = getParsedLabelFrom("nuntio/metadata/some-metadata");

        assertEquals(ServiceBinding.ANY, parsedLabel.getBinding());
        assertEquals(Label.METADATA, parsedLabel.getLabelKind());
        assertEquals("some-metadata", parsedLabel.getAdditional());
    }

    @Test
    void parseLabelsSimplePort() {
        ParsedLabel parsedLabel = getParsedLabelFrom("nuntio/80/service");

        assertEquals(ServiceBinding.fromPort(80), parsedLabel.getBinding());
        assertEquals(Label.SERVICE, parsedLabel.getLabelKind());
        assertNull(parsedLabel.getAdditional());
    }

    @Test
    void parseLabelsAdditionalPort() {
        ParsedLabel parsedLabel = getParsedLabelFrom("nuntio/80/metadata/some-metadata");

        assertEquals(ServiceBinding.fromPort(80), parsedLabel.getBinding());
        assertEquals(Label.METADATA, parsedLabel.getLabelKind());
        assertEquals("some-metadata", parsedLabel.getAdditional());
    }

    @Test
    void parseLabelsSimplePortProtocol() {
        ParsedLabel parsedLabel = getParsedLabelFrom("nuntio/udp:80/service");

        assertEquals(ServiceBinding.fromPortAndProtocol(80, "udp"), parsedLabel.getBinding());
        assertEquals(Label.SERVICE, parsedLabel.getLabelKind());
        assertNull(parsedLabel.getAdditional());
    }

    @Test
    void parseLabelsAdditionalPortProtocol() {
        ParsedLabel parsedLabel = getParsedLabelFrom("nuntio/udp:80/metadata/some-metadata");

        assertEquals(ServiceBinding.fromPortAndProtocol(80, "udp"), parsedLabel.getBinding());
        assertEquals(Label.METADATA, parsedLabel.getLabelKind());
        assertEquals("some-metadata", parsedLabel.getAdditional());
    }
}
