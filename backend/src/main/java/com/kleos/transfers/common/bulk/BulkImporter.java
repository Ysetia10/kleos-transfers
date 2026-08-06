package com.kleos.transfers.common.bulk;

import com.kleos.transfers.common.bulk.BulkImportResponse.BulkImportIssue;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Imports identity records in batches, reporting invalid and duplicate rows
 * instead of rejecting the whole request.
 */
@Component
@RequiredArgsConstructor
public class BulkImporter {

    private final Validator validator;

    public <C, R> BulkImportResponse<R> importAll(List<C> requests, BulkImportSpec<C, R> spec) {
        List<BulkImportIssue> failed = new ArrayList<>();
        List<Indexed<C>> valid = new ArrayList<>();

        for (int index = 0; index < requests.size(); index++) {
            C request = requests.get(index);
            String violations = describeViolations(request);
            if (violations == null) {
                valid.add(new Indexed<>(index, request));
            } else {
                failed.add(new BulkImportIssue(index, reference(spec, request), violations));
            }
        }

        List<BulkImportIssue> skipped = new ArrayList<>();
        List<C> accepted = new ArrayList<>();
        Set<String> existingKeys = valid.isEmpty()
                ? Set.of()
                : spec.findExistingKeys(valid.stream().map(Indexed::request).toList());
        Set<String> batchKeys = new HashSet<>();

        for (Indexed<C> item : valid) {
            String key = spec.naturalKey(item.request());
            if (existingKeys.contains(key)) {
                skipped.add(new BulkImportIssue(item.index(), reference(spec, item.request()), "already exists"));
            } else if (!batchKeys.add(key)) {
                skipped.add(new BulkImportIssue(item.index(), reference(spec, item.request()), "duplicate within request"));
            } else {
                accepted.add(item.request());
            }
        }

        List<R> created = accepted.isEmpty() ? List.of() : spec.persist(accepted);
        return BulkImportResponse.of(requests.size(), created, skipped, failed);
    }

    private <C> String describeViolations(C request) {
        if (request == null) {
            return "item must not be null";
        }
        Set<ConstraintViolation<C>> violations = validator.validate(request);
        if (violations.isEmpty()) {
            return null;
        }
        return violations.stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .sorted()
                .collect(Collectors.joining("; "));
    }

    private <C, R> String reference(BulkImportSpec<C, R> spec, C request) {
        return request == null ? "" : spec.reference(request);
    }

    private record Indexed<C>(int index, C request) {
    }
}
