package com.example.expense_tracker.service;

import com.example.expense_tracker.dto.ChangeExpenseStatusRequest;
import com.example.expense_tracker.dto.ExpenseCreateDto;
import com.example.expense_tracker.dto.ExpenseCreationResult;
import com.example.expense_tracker.dto.ExpenseDto;
import com.example.expense_tracker.dto.ExpenseStatusChangeDto;
import com.example.expense_tracker.entity.Expense;
import com.example.expense_tracker.entity.ExpenseStatusChange;
import com.example.expense_tracker.entity.IdempotencyKey;
import com.example.expense_tracker.entity.Project;
import com.example.expense_tracker.entity.User;
import com.example.expense_tracker.entity.enums.ExpenseStatus;
import com.example.expense_tracker.exception.ConflictException;
import com.example.expense_tracker.exception.IdempotencyKeyReuseException;
import com.example.expense_tracker.exception.ResourceNotFoundException;
import com.example.expense_tracker.mapper.ExpenseMapper;
import com.example.expense_tracker.repository.ExpenseRepository;
import com.example.expense_tracker.repository.ExpenseStatusChangeRepository;
import com.example.expense_tracker.repository.IdempotencyKeyRepository;
import com.example.expense_tracker.repository.ProjectRepository;
import com.example.expense_tracker.repository.UserRepository;
import com.example.expense_tracker.security.ProjectAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseStatusChangeRepository historyRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ExpenseMapper expenseMapper;
    private final ProjectAccessService accessService;

    @Transactional(readOnly = true)
    public Page<ExpenseDto> getExpensesByProjectId(Long projectId, ExpenseStatus status, Pageable pageable, String username) {
        accessService.checkAccess(projectId, username);
        Page<Expense> page = status == null
                ? expenseRepository.findAllByProjectId(projectId, pageable)
                : expenseRepository.findAllByProjectIdAndStatus(projectId, status, pageable);
        return page.map(expenseMapper::toDto);
    }

    /**
     * Создает расход. Если передан idempotencyKey, операция идемпотентна: повтор с тем же ключом
     * и тем же телом вернет уже созданный расход, а не создаст второй.
     * Запись ключа и расход сохраняются в ОДНОЙ транзакции: либо есть оба, либо ни одного.
     */
    @Transactional
    public ExpenseCreationResult createExpense(Long projectId, ExpenseCreateDto dto, String username, String idempotencyKey) {
        accessService.checkAccess(projectId, username);
        User employee = findUser(username);

        if (idempotencyKey == null) {
            return new ExpenseCreationResult(expenseMapper.toDto(persistNewExpense(projectId, dto, employee)), false);
        }

        String requestHash = fingerprint(projectId, dto);
        int inserted = idempotencyKeyRepository.insertIfAbsent(employee.getId(), idempotencyKey, requestHash);

        if (inserted == 0) {
            // Ключ уже занят. Если параллельный запрос еще не завершился, INSERT выше дождался его коммита
            IdempotencyKey existing = idempotencyKeyRepository.findByUserIdAndKey(employee.getId(), idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException("Idempotency key disappeared: " + idempotencyKey));
            if (!existing.getRequestHash().equals(requestHash)) {
                throw new IdempotencyKeyReuseException("Idempotency-Key was already used with a different request");
            }
            Expense original = expenseRepository.findById(existing.getExpenseId())
                    .orElseThrow(() -> new IllegalStateException("Expense for idempotency key not found"));
            return new ExpenseCreationResult(expenseMapper.toDto(original), true);
        }

        Expense created = persistNewExpense(projectId, dto, employee);
        IdempotencyKey claimed = idempotencyKeyRepository.findByUserIdAndKey(employee.getId(), idempotencyKey)
                .orElseThrow(() -> new IllegalStateException("Claimed idempotency key not found"));
        claimed.setExpenseId(created.getId());
        return new ExpenseCreationResult(expenseMapper.toDto(created), false);
    }

    /**
     * Переводит расход в новый статус. Порядок проверок важен:
     * доступ к проекту (403) -> расход принадлежит проекту (404) -> права на этот переход (403)
     * -> допустимость перехода (409).
     */
    @Transactional
    public ExpenseDto changeStatus(Long projectId, Long expenseId, ChangeExpenseStatusRequest request, String username) {
        accessService.checkAccess(projectId, username);
        User actor = findUser(username);
        Expense expense = findExpenseInProject(projectId, expenseId);
        ExpenseStatus target = request.status();

        authorizeTransition(projectId, expense, target, actor, username);

        ExpenseStatus current = expense.getStatus();
        if (!current.canTransitionTo(target)) {
            throw new ConflictException("Cannot change expense status from " + current + " to " + target);
        }

        expense.setStatus(target);
        recordHistory(expense, current, target, actor, request.comment());
        // Если параллельный запрос успел изменить этот же расход, при коммите сработает @Version -> 409
        return expenseMapper.toDto(expense);
    }

    @Transactional(readOnly = true)
    public List<ExpenseStatusChangeDto> getHistory(Long projectId, Long expenseId, String username) {
        accessService.checkAccess(projectId, username);
        if (!expenseRepository.existsByIdAndProjectId(expenseId, projectId)) {
            throw new ResourceNotFoundException("Expense not found: " + expenseId);
        }
        return historyRepository.findAllByExpenseIdOrderByChangedAtAscIdAsc(expenseId).stream()
                .map(change -> new ExpenseStatusChangeDto(
                        change.getId(),
                        change.getFromStatus(),
                        change.getToStatus(),
                        change.getChangedBy().getUsername(),
                        change.getComment(),
                        change.getChangedAt()))
                .toList();
    }

    private Expense persistNewExpense(Long projectId, ExpenseCreateDto dto, User employee) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        Expense expense = expenseMapper.toEntity(dto);
        expense.setProject(project);
        expense.setEmployee(employee);
        expense.setStatus(ExpenseStatus.PENDING);

        Expense saved = expenseRepository.save(expense);
        recordHistory(saved, null, ExpenseStatus.PENDING, employee, null);
        return saved;
    }

    // Согласовать, отклонить и оплатить может только админ проекта; отменить - только автор расхода
    private void authorizeTransition(Long projectId, Expense expense, ExpenseStatus target, User actor, String username) {
        if (target == ExpenseStatus.CANCELLED) {
            if (!expense.getEmployee().getId().equals(actor.getId())) {
                throw new AccessDeniedException("Only the author can cancel an expense");
            }
            return;
        }
        accessService.checkProjectAdmin(projectId, username);
    }

    private void recordHistory(Expense expense, ExpenseStatus from, ExpenseStatus to, User actor, String comment) {
        ExpenseStatusChange change = new ExpenseStatusChange();
        change.setExpense(expense);
        change.setFromStatus(from);
        change.setToStatus(to);
        change.setChangedBy(actor);
        change.setComment(comment);
        historyRepository.save(change);
    }

    private Expense findExpenseInProject(Long projectId, Long expenseId) {
        return expenseRepository.findByIdAndProjectId(expenseId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found: " + expenseId));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    // Отпечаток бизнес-смысла запроса. Сумма нормализована, чтобы 10.5 и 10.50 считались одним и тем же
    private String fingerprint(Long projectId, ExpenseCreateDto dto) {
        String canonical = projectId
                + "|" + dto.amount().stripTrailingZeros().toPlainString()
                + "|" + dto.currency()
                + "|" + dto.description();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
