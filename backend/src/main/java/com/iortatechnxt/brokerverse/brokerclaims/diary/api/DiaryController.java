package com.iortatechnxt.brokerverse.brokerclaims.diary.api;

import com.iortatechnxt.brokerverse.brokerclaims.diary.api.dto.DiaryDtos.DiaryEntryResponse;
import com.iortatechnxt.brokerverse.brokerclaims.diary.api.dto.DiaryDtos.DiaryRequest;
import com.iortatechnxt.brokerverse.brokerclaims.diary.api.dto.DiaryDtos.DoneRequest;
import com.iortatechnxt.brokerverse.brokerclaims.diary.service.DiaryQuery;
import com.iortatechnxt.brokerverse.brokerclaims.diary.service.DiaryService;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The claims diary: the Diary tab of a claim and My Diary (BRCLM.022/034; FR-CL-052). */
@RestController
@RequestMapping("/api/v1/broker-claims")
public class DiaryController {

  private static final int MAX_PAGE = 200;

  private final DiaryService diary;
  private final DiaryQuery query;

  /**
   * Creates the controller.
   *
   * @param diary diary
   * @param query My Diary
   */
  public DiaryController(DiaryService diary, DiaryQuery query) {
    this.diary = diary;
    this.query = query;
  }

  /**
   * The diary entries of a claim.
   *
   * @param id claim
   * @param companyId company
   * @return entries, newest first
   */
  @GetMapping("/{id}/diary")
  @PreAuthorize("hasAuthority('BCL_VIEW')")
  public List<DiaryEntryResponse> entries(@PathVariable Long id, @RequestParam Long companyId) {
    return diary.entries(companyId, id).stream()
        .map(e -> DiaryEntryResponse.from(e, diary::typeLabel))
        .toList();
  }

  /**
   * Adds a diary entry (also on a closed claim).
   *
   * @param id claim
   * @param companyId company
   * @param request entry
   * @return the entry
   */
  @PostMapping("/{id}/diary")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('BCL_RECORD')")
  public DiaryEntryResponse add(
      @PathVariable Long id, @RequestParam Long companyId, @RequestBody DiaryRequest request) {
    return DiaryEntryResponse.from(diary.add(companyId, id, request.toInput()), diary::typeLabel);
  }

  /**
   * Marks a diary entry done.
   *
   * @param entryId entry
   * @param companyId company
   * @param request remark
   * @return the entry
   */
  @PostMapping("/diary/{entryId}/done")
  @PreAuthorize("hasAuthority('BCL_VIEW')")
  public DiaryEntryResponse markDone(
      @PathVariable Long entryId,
      @RequestParam Long companyId,
      @RequestBody(required = false) DoneRequest request) {
    return DiaryEntryResponse.from(
        diary.markDone(companyId, entryId, request == null ? null : request.remark()),
        diary::typeLabel);
  }

  /**
   * My Diary.
   *
   * @param companyId company
   * @param includeDone whether completed entries are listed too
   * @param page page
   * @param size page size (at most 200)
   * @return entries assigned to the user
   */
  @GetMapping("/diary/mine")
  @PreAuthorize("hasAuthority('BCL_VIEW')")
  public PageResponse<DiaryQuery.DiaryItem> mine(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "false") boolean includeDone,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return query.mine(companyId, includeDone, Math.max(0, page), Math.clamp(size, 1, MAX_PAGE));
  }
}
