package com.medmail.portal.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.medmail.portal.domain.IngestRecord;
import com.medmail.portal.domain.OutboxEvent;
import com.medmail.portal.domain.UploadedFile;
import com.medmail.portal.repo.IngestRecordRepository;
import com.medmail.portal.repo.OutboxEventRepository;
import com.medmail.portal.repo.UploadedFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FileIngestService {

  private final UploadedFileRepository files;
  private final IngestRecordRepository records;
  private final OutboxEventRepository outbox;

  private final ObjectMapper mapper = new ObjectMapper();
  private final JsonFactory factory = new JsonFactory();

  /** Parse JSON array or NDJSON into a preview list (streaming where possible). */
  public List<JsonNode> parseForPreview(String raw, int maxItems) throws Exception {
    List<JsonNode> out = new ArrayList<>(Math.min(maxItems, 200));
    String trimmed = raw == null ? "" : raw.trim();
    if (trimmed.isEmpty()) return out;

    if (trimmed.startsWith("[")) {
      try (JsonParser p = factory.createParser(trimmed)) {
        if (p.nextToken() == null) return out;
        while (p.nextToken() != null && out.size() < maxItems) {
          JsonNode n = mapper.readTree(p);
          if (n != null && !n.isMissingNode() && !n.isNull()) out.add(n);
        }
      }
    } else {
      String[] lines = trimmed.split("\\R");
      for (String line : lines) {
        if (line.isBlank()) continue;
        out.add(mapper.readTree(line));
        if (out.size() >= maxItems) break;
      }
    }
    return out;
  }

  private static String sha256(String s) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
    StringBuilder sb = new StringBuilder(d.length * 2);
    for (byte b : d) sb.append(String.format("%02x", b));
    return sb.toString();
  }

  /** Persist uploaded file and split into per-record rows + outbox events. */
  @Transactional
  public long commit(String originalName, String raw, List<JsonNode> items) throws Exception {
    UploadedFile uf = new UploadedFile();
    uf.setOriginalName(originalName);
    uf.setContentType("application/json");
    uf.setSizeBytes(raw.getBytes(StandardCharsets.UTF_8).length);
    uf.setSha256(sha256(raw));
    uf.setRawJson(raw);
    uf.setUploadedAt(Instant.now());
    files.save(uf);

    List<IngestRecord> batch = new ArrayList<>(256);
    List<OutboxEvent> evBatch = new ArrayList<>(256);

    for (JsonNode n : items) {
      IngestRecord r = new IngestRecord();
      r.setFile(uf);
      r.setPayloadJson(n.toString());
      r.setStatus(IngestRecord.Status.RECEIVED);
      batch.add(r);

      if (batch.size() == 256) {
        records.saveAll(batch);
        for (IngestRecord rec : batch) {
          OutboxEvent e = new OutboxEvent();
          e.setAggregateType("record");
          e.setAggregateId(rec.getId());
          e.setEventType("RecordReceived");
          e.setPayloadJson(rec.getPayloadJson());
          e.setCreatedAt(Instant.now());
          evBatch.add(e);
        }
        outbox.saveAll(evBatch);
        batch.clear();
        evBatch.clear();
      }
    }

    if (!batch.isEmpty()) {
      records.saveAll(batch);
      for (IngestRecord rec : batch) {
        OutboxEvent e = new OutboxEvent();
        e.setAggregateType("record");
        e.setAggregateId(rec.getId());
        e.setEventType("RecordReceived");
        e.setPayloadJson(rec.getPayloadJson());
        e.setCreatedAt(Instant.now());
        evBatch.add(e);
      }
      outbox.saveAll(evBatch);
    }

    return uf.getId();
  }

  @Transactional
  public void updateRecordStatus(Long recordId, IngestRecord.Status status) {
    IngestRecord r = records.findById(recordId).orElseThrow();
    r.setStatus(status);
    r.setErrorMessage(null);
  }

  // ===== Preview table helpers =====

  @Value
  public static class TablePreview {
    List<String> headers;
    List<List<String>> rows;
  }

  /**
   * Convert JSON nodes to a flat table:
   * - Nested objects -> dot keys (e.g., exam.type)
   * - Arrays of scalars -> single column joined by ", "
   * - Mixed/object arrays -> compact JSON chunks joined by "; "
   * - Drops columns that are empty across all rows
   */
  public TablePreview toTablePreview(List<JsonNode> items, int maxColumns, int maxCellChars) {
    List<Map<String, String>> flattened = new ArrayList<>(items.size());
    for (JsonNode n : items) {
      Map<String, String> m = new LinkedHashMap<>();
      flattenJsonForTable(n, "", m);
      flattened.add(m);
    }

    LinkedHashSet<String> headerSet = new LinkedHashSet<>();
    for (Map<String, String> m : flattened) headerSet.addAll(m.keySet());

    headerSet.removeIf(h -> flattened.stream().allMatch(m -> isBlank(m.get(h))));

    List<String> headers = new ArrayList<>(headerSet);
    if (headers.size() > maxColumns) headers = headers.subList(0, maxColumns);

    List<List<String>> rows = new ArrayList<>(flattened.size());
    for (Map<String, String> m : flattened) {
      List<String> row = new ArrayList<>(headers.size());
      for (String h : headers) {
        String v = defaultString(m.get(h));
        if (v.length() > maxCellChars) v = v.substring(0, maxCellChars - 1) + "…";
        row.add(v);
      }
      rows.add(row);
    }

    return new TablePreview(headers, rows);
  }

  private void flattenJsonForTable(JsonNode node, String prefix, Map<String, String> out) {
    if (node == null || node.isNull()) {
      if (!prefix.isEmpty()) out.put(prefix, "");
      return;
    }

    if (node.isObject()) {
      node.fields().forEachRemaining(e -> {
        String key = joinKey(prefix, e.getKey());
        flattenJsonForTable(e.getValue(), key, out);
      });
      return;
    }

    if (node.isArray()) {
      ArrayNode arr = (ArrayNode) node;
      if (arr.isEmpty()) {
        out.put(prefix, "");
        return;
      }
      boolean allScalar = true;
      List<String> vals = new ArrayList<>(arr.size());
      for (JsonNode el : arr) {
        if (isScalar(el)) {
          vals.add(asScalarString(el));
        } else {
          allScalar = false;
          break;
        }
      }
      if (allScalar) {
        out.put(prefix, String.join(", ", vals));
      } else {
        List<String> chunks = new ArrayList<>(arr.size());
        for (JsonNode el : arr) {
          chunks.add(isScalar(el) ? asScalarString(el) : el.toString());
        }
        out.put(prefix, String.join("; ", chunks));
      }
      return;
    }

    if (!prefix.isEmpty()) out.put(prefix, asScalarString(node));
  }

  private static String joinKey(String prefix, String key) {
    return (prefix == null || prefix.isEmpty()) ? key : (prefix + "." + key);
  }

  private static boolean isScalar(JsonNode n) {
    return n == null || n.isNull() || n.isValueNode();
  }

  private static String asScalarString(JsonNode n) {
    if (n == null || n.isNull()) return "";
    if (n.isTextual()) return n.asText();
    if (n.isNumber()) return n.numberValue().toString();
    if (n.isBoolean()) return Boolean.toString(n.asBoolean());
    if (n.isValueNode()) return ((ValueNode) n).asText();
    return n.toString();
  }

  private static boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }

  private static String defaultString(String s) {
    return s == null ? "" : s;
  }
}
