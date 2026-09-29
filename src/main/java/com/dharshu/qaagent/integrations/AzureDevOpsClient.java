package com.dharshu.qaagent.integrations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Creates work items ("Bug" type) in Azure DevOps via the REST API using a
 * Personal Access Token (PAT). Reads ADO_ORG, ADO_PROJECT, and ADO_PAT from
 * environment variables -- never hardcode these.
 */
public class AzureDevOpsClient {

    private final OkHttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String organization;
    private final String project;
    private final String authHeader;

    public AzureDevOpsClient() {
        this(System.getenv("ADO_ORG"), System.getenv("ADO_PROJECT"), System.getenv("ADO_PAT"));
    }

    public AzureDevOpsClient(String organization, String project, String pat) {
        if (organization == null || project == null || pat == null) {
            throw new IllegalStateException(
                    "ADO_ORG, ADO_PROJECT, and ADO_PAT must all be set as environment variables.");
        }
        this.organization = organization;
        this.project = project;
        String token = Base64.getEncoder().encodeToString((":" + pat).getBytes());
        this.authHeader = "Basic " + token;
        this.http = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Checks whether an open bug with a matching title already exists, to
     * avoid filing duplicates when the same failure repeats across runs.
     */
    public boolean bugAlreadyExists(String title) {
        try {
            String wiql = "SELECT [System.Id] FROM WorkItems WHERE [System.WorkItemType] = 'Bug' "
                    + "AND [System.State] <> 'Closed' AND [System.Title] = '" + title.replace("'", "''") + "'";

            ObjectNode body = mapper.createObjectNode();
            body.put("query", wiql);

            String url = String.format(
                    "https://dev.azure.com/%s/%s/_apis/wit/wiql?api-version=7.1", organization, project);

            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", authHeader)
                    .post(RequestBody.create(mapper.writeValueAsString(body), MediaType.parse("application/json")))
                    .build();

            try (Response response = http.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) return false;
                JsonNode json = mapper.readTree(response.body().string());
                return json.path("workItems").size() > 0;
            }
        } catch (IOException e) {
            // If the duplicate check itself fails, fail open (don't block a real bug report).
            return false;
        }
    }

    /**
     * Creates a Bug work item with a title, description, and (optionally) a
     * screenshot attached. Returns the new work item's URL, or null if the
     * duplicate check found an existing open bug for this title.
     */
    public String createBug(String title, String description, byte[] screenshotPng) {
        if (bugAlreadyExists(title)) {
            System.out.println("[BUG FILING] Skipped -- an open bug with this title already exists: " + title);
            return null;
        }

        try {
            ArrayNode patchDoc = mapper.createArrayNode();
            addOp(patchDoc, "/fields/System.Title", title);
            addOp(patchDoc, "/fields/System.Description", description.replace("\n", "<br/>"));
            addOp(patchDoc, "/fields/System.Tags", "auto-filed; qa-agent");

            String createUrl = String.format(
                    "https://dev.azure.com/%s/%s/_apis/wit/workitems/$Bug?api-version=7.1", organization, project);

            Request request = new Request.Builder()
                    .url(createUrl)
                    .header("Authorization", authHeader)
                    .header("Content-Type", "application/json-patch+json")
                    .post(RequestBody.create(mapper.writeValueAsString(patchDoc),
                            MediaType.parse("application/json-patch+json")))
                    .build();

            try (Response response = http.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new IOException("Azure DevOps bug creation failed: HTTP " + response.code());
                }
                JsonNode json = mapper.readTree(response.body().string());
                String bugUrl = json.path("_links").path("html").path("href").asText();
                System.out.println("[BUG FILED] " + title + " -> " + bugUrl);

                if (screenshotPng != null) {
                    attachScreenshot(json.path("id").asInt(), screenshotPng);
                }
                return bugUrl;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create Azure DevOps bug", e);
        }
    }

    private void addOp(ArrayNode patchDoc, String path, String value) {
        ObjectNode op = patchDoc.addObject();
        op.put("op", "add");
        op.put("path", path);
        op.put("value", value);
    }

    private void attachScreenshot(int workItemId, byte[] screenshotPng) {
        try {
            String uploadUrl = String.format(
                    "https://dev.azure.com/%s/%s/_apis/wit/attachments?fileName=failure.png&api-version=7.1",
                    organization, project);

            Request uploadRequest = new Request.Builder()
                    .url(uploadUrl)
                    .header("Authorization", authHeader)
                    .post(RequestBody.create(screenshotPng, MediaType.parse("application/octet-stream")))
                    .build();

            try (Response uploadResponse = http.newCall(uploadRequest).execute()) {
                if (!uploadResponse.isSuccessful() || uploadResponse.body() == null) return;
                JsonNode uploaded = mapper.readTree(uploadResponse.body().string());
                String attachmentUrl = uploaded.path("url").asText();

                ArrayNode patchDoc = mapper.createArrayNode();
                ObjectNode op = patchDoc.addObject();
                op.put("op", "add");
                op.put("path", "/relations/-");
                ObjectNode value = op.putObject("value");
                value.put("rel", "AttachedFile");
                value.put("url", attachmentUrl);

                String linkUrl = String.format(
                        "https://dev.azure.com/%s/%s/_apis/wit/workitems/%d?api-version=7.1",
                        organization, project, workItemId);

                Request linkRequest = new Request.Builder()
                        .url(linkUrl)
                        .header("Authorization", authHeader)
                        .header("Content-Type", "application/json-patch+json")
                        .patch(RequestBody.create(mapper.writeValueAsString(patchDoc),
                                MediaType.parse("application/json-patch+json")))
                        .build();
                http.newCall(linkRequest).execute().close();
            }
        } catch (IOException e) {
            System.err.println("Screenshot attach failed (bug was still filed): " + e.getMessage());
        }
    }
}
