package com.travelagent.travelplanner.auth.application;

import com.travelagent.travelplanner.auth.api.ProfileUpdateRequest;
import com.travelagent.travelplanner.auth.api.UserPreferencesRequest;
import com.travelagent.travelplanner.auth.domain.EmailCodeStore;
import com.travelagent.travelplanner.auth.domain.EmailPurpose;
import com.travelagent.travelplanner.auth.domain.UserAccount;
import com.travelagent.travelplanner.auth.domain.UserPreferences;
import com.travelagent.travelplanner.auth.domain.UserPreferencesRepository;
import com.travelagent.travelplanner.auth.domain.UserRepository;
import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 当前用户资料和长期偏好用例。 */
@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final EmailCodeStore emailCodeStore;
    private final PasswordEncoder passwordEncoder;

    /**
     * 创建用户资料服务。
     *
     * @param userRepository 用户仓储
     * @param preferencesRepository 用户偏好仓储
     * @param emailCodeStore 邮箱验证码存储
     * @param passwordEncoder 密码编码器
     */
    public UserProfileService(
            UserRepository userRepository,
            UserPreferencesRepository preferencesRepository,
            EmailCodeStore emailCodeStore,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.preferencesRepository = preferencesRepository;
        this.emailCodeStore = emailCodeStore;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 更新当前用户资料。
     *
     * @param userId 当前用户主键
     * @param request 更新请求
     * @return 更新后的用户
     */
    @Transactional
    public UserAccount updateProfile(long userId, ProfileUpdateRequest request) {
        UserAccount current = requireUser(userId);
        String username = hasText(request.newUsername())
                ? request.newUsername().strip()
                : current.username();
        String email = hasText(request.newEmail())
                ? normalizeEmail(request.newEmail())
                : current.email();
        String passwordHash = hasText(request.newPassword())
                ? passwordEncoder.encode(request.newPassword())
                : current.passwordHash();
        String nickname = request.nickname() == null ? current.nickname() : request.nickname().strip();
        String phone = request.phone() == null ? current.phone() : nullIfBlank(request.phone());

        if (request.changesSensitiveFields() && !hasText(request.verificationCode())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "修改敏感资料需要邮箱验证码");
        }
        if (hasText(request.newEmail())
                && !email.equals(current.email())
                && !hasText(request.newEmailCode())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "修改邮箱需要验证新邮箱");
        }
        if (request.changesSensitiveFields()) {
            consumeCode(current.email(), request.verificationCode());
        }
        if (hasText(request.newEmail()) && !email.equals(current.email())) {
            consumeCode(email, request.newEmailCode());
        }
        if (!username.equals(current.username())
                && userRepository.findByUsername(username).isPresent()) {
            throw new BusinessException(ErrorCode.ACCOUNT_EXISTS);
        }
        if (!email.equals(current.email()) && userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }
        try {
            userRepository.updateProfile(userId, username, email, passwordHash, nickname, phone);
        } catch (DuplicateKeyException exception) {
            throwConcurrentDuplicate(username, email, exception);
        }
        return requireUser(userId);
    }

    /**
     * 查询当前用户长期偏好。
     *
     * @param userId 当前用户主键
     * @return 用户偏好
     */
    @Transactional(readOnly = true)
    public UserPreferences getPreferences(long userId) {
        requireUser(userId);
        return preferencesRepository.findByUserId(userId)
                .orElseGet(() -> new UserPreferences(userId, null, null, List.of(), null, null, null));
    }

    /**
     * 更新当前用户长期偏好。
     *
     * @param userId 当前用户主键
     * @param request 更新请求
     * @return 更新后的偏好
     */
    @Transactional
    public UserPreferences updatePreferences(long userId, UserPreferencesRequest request) {
        requireUser(userId);
        List<String> forbiddenList = request.forbiddenList() == null
                ? List.of()
                : new LinkedHashSet<>(request.forbiddenList()).stream()
                        .filter(UserProfileService::hasText)
                        .map(String::strip)
                        .toList();
        return preferencesRepository.save(new UserPreferences(
                userId,
                nullIfBlank(request.travelStyleDefault()),
                nullIfBlank(request.foodHabit()),
                forbiddenList,
                nullIfBlank(request.accommodationPreference()),
                nullIfBlank(request.budgetLevel()),
                null));
    }

    private UserAccount requireUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));
    }

    private void consumeCode(String email, String code) {
        if (!hasText(code)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "修改敏感资料需要邮箱验证码");
        }
        boolean valid = emailCodeStore.consumeCode(
                email,
                EmailPurpose.UPDATE_PROFILE,
                EmailCodeHasher.hash(email, EmailPurpose.UPDATE_PROFILE, code.strip()));
        if (!valid) {
            throw new BusinessException(ErrorCode.EMAIL_CODE_INVALID);
        }
    }

    private void throwConcurrentDuplicate(
            String username,
            String email,
            DuplicateKeyException originalException) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new BusinessException(ErrorCode.ACCOUNT_EXISTS);
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }
        throw originalException;
    }

    private String normalizeEmail(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }

    private static String nullIfBlank(String value) {
        return hasText(value) ? value.strip() : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
