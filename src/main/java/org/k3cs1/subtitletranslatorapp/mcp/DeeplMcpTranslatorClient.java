package org.k3cs1.subtitletranslatorapp.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static io.modelcontextprotocol.client.McpClient.sync;

public final class DeeplMcpTranslatorClient implements AutoCloseable {

    private static final ObjectMapper OM = new ObjectMapper();

    private final McpSyncClient client;

    public DeeplMcpTranslatorClient(Path deeplServerJar) {
        ServerParameters params = ServerParameters.builder("java")
                .args("-jar", deeplServerJar.toAbsolutePath().toString())
                .env(Map.of(
                        "DEEPL_API_KEY", System.getenv("DEEPL_API_KEY")
                ))
                .build();

        var jsonMapper = new JacksonMcpJsonMapper(OM);
        McpClientTransport transport = new StdioClientTransport(params, jsonMapper);

        this.client = sync(transport).build();
        this.client.initialize();
    }

    public List<String> translateEnToHu(List<String> texts) {
        McpSchema.CallToolRequest req = new McpSchema.CallToolRequest(
                "deepl_translate",
                Map.of(
                        "texts", texts,
                        "source_lang", "EN",
                        "target_lang", "HU"
                )
        );

        McpSchema.CallToolResult result = client.callTool(req);
        if (Boolean.TRUE.equals(result.isError())) {
            throw new RuntimeException("MCP/DeepL tool error: " + result.content());
        }

        // Tool returned JSON in TextContent
        var text = ((McpSchema.TextContent) result.content().getFirst()).text();
        try {
            Map<?, ?> parsed = OM.readValue(text, Map.class);
            @SuppressWarnings("unchecked")
            List<String> out = (List<String>) parsed.get("translations");
            return out;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse tool output: " + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        client.close();
    }
}
