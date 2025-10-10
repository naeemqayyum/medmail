package com.medmail.portal.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.medmail.portal.domain.IngestRecord;
import com.medmail.portal.repo.IngestRecordRepository;
import com.medmail.portal.repo.UploadedFileRepository;
import com.medmail.portal.service.FileIngestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Controller
@RequestMapping("/admin/files")
@RequiredArgsConstructor
public class AdminFileController {

  private final FileIngestService ingest;
  private final UploadedFileRepository files;
  private final IngestRecordRepository records;

  private static final String SESSION_RAW = "upload_raw";
  private static final String SESSION_NAME = "upload_name";

  @GetMapping("/upload")
  public String uploadForm() {
    return "admin/upload";
  }

  @PostMapping("/preview")
  public String preview(@RequestParam("file") MultipartFile file,
                        @RequestParam(value="maxPreview", defaultValue = "200") int maxPreview,
                        Model model,
                        HttpSession session,
                        RedirectAttributes ra) {
    try {
      if (file == null || file.isEmpty()) {
        ra.addFlashAttribute("error", "Please choose a JSON file.");
        return "redirect:/admin/files/upload";
      }
      String original = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "upload.json";
      String raw = new String(file.getBytes(), StandardCharsets.UTF_8);

      session.setAttribute(SESSION_RAW, Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8)));
      session.setAttribute(SESSION_NAME, original);
      List<JsonNode> previewItems = ingest.parseForPreview(raw, Math.min(Math.max(maxPreview, 1), 500));
      model.addAttribute("originalName", original);
      model.addAttribute("count", previewItems.size());
      model.addAttribute("items", previewItems);

// ADD THIS:
      var table = ingest.toTablePreview(previewItems, /*maxColumns*/ 80, /*maxCellChars*/ 400);
      model.addAttribute("headers", table.getHeaders());
      model.addAttribute("rows", table.getRows());
      return "admin/preview";

    } catch (Exception e) {
      ra.addFlashAttribute("error", "Failed to parse file: " + e.getMessage());
      return "redirect:/admin/files/upload";
    }
  }
//
  @PostMapping("/commit")
  public String commit(HttpSession session, RedirectAttributes ra) {
    try {
      String b64 = (String) session.getAttribute(SESSION_RAW);
      String original = (String) session.getAttribute(SESSION_NAME);
      if (b64 == null || original == null) {
        ra.addFlashAttribute("error", "No file in session. Please upload again.");
        return "redirect:/admin/files/upload";
      }
      String raw = new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);

      List<JsonNode> all = ingest.parseForPreview(raw, Integer.MAX_VALUE);
      long fileId = ingest.commit(original, raw, all);

      session.removeAttribute(SESSION_RAW);
      session.removeAttribute(SESSION_NAME);

      ra.addFlashAttribute("success", "Ingested " + all.size() + " records from " + original);
      return "redirect:/admin/files/" + fileId;

    } catch (Exception e) {
      ra.addFlashAttribute("error", "Commit failed: " + e.getMessage());
      return "redirect:/admin/files/upload";
    }
  }

  @GetMapping("/{fileId:\\d+}")
  public String fileDetail(@PathVariable Long fileId,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "25") int size,
                           Model model,
                           RedirectAttributes ra) {
    return files.findById(fileId).map(f -> {
      Page<IngestRecord> recs = records.findByFile_Id(fileId, PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 200)));
      model.addAttribute("file", f);
      model.addAttribute("page", recs);
      return "admin/file_detail";
    }).orElseGet(() -> {
      ra.addFlashAttribute("error", "File not found.");
      return "redirect:/admin/files/upload";
    });
  }

  @PostMapping("/records/{id}/status")
  public String updateStatus(@PathVariable Long id,
                             @RequestParam("status") IngestRecord.Status status,
                             RedirectAttributes ra) {
    try {
      ingest.updateRecordStatus(id, status);
      ra.addFlashAttribute("success", "Record " + id + " updated to " + status);
    } catch (Exception e) {
      ra.addFlashAttribute("error", "Failed updating record: " + e.getMessage());
    }
    return "redirect:/admin/files/upload";
  }


}
