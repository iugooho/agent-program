package com.travelagent.travelplanner.auth.application;

import com.travelagent.travelplanner.auth.domain.AvatarStorage;
import com.travelagent.travelplanner.auth.domain.EmailCodeStore;
import com.travelagent.travelplanner.auth.domain.EmailPurpose;
import com.travelagent.travelplanner.auth.domain.SecurityEventType;
import com.travelagent.travelplanner.auth.domain.UserAccount;
import com.travelagent.travelplanner.auth.domain.UserRepository;
import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 账号注销用例。 */
@Service
public class AccountDeletionService {

    private final UserRepository userRepository;
    private final EmailCodeStore emailCodeStore;
    private final AvatarStorage avatarStorage;
    private final AccountSecurityService accountSecurityService;

    /**
     * 创建账号注销服务。
     *
     * @param userRepository 用户仓储
     * @param emailCodeStore 邮箱验证码存储
     * @param avatarStorage 头像存储
     * @param accountSecurityService 账号安全服务
     */
    public AccountDeletionService(
            UserRepository userRepository,
            EmailCodeStore emailCodeStore,
            AvatarStorage avatarStorage,
            AccountSecurityService accountSecurityService) {
        this.userRepository = userRepository;
        this.emailCodeStore = emailCodeStore;
        this.avatarStorage = avatarStorage;
        this.accountSecurityService = accountSecurityService;
    }

    /**
     * 验证当前邮箱后注销并脱敏账号。
     *
     * @param userId 用户主键
     * @param verificationCode 当前邮箱验证码
     * @param context 客户端信息
     */
    @Transactional
    public void delete(long userId, String verificationCode, ClientContext context) {
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));
        boolean valid = emailCodeStore.consumeCode(
                user.email(),
                EmailPurpose.UPDATE_PROFILE,
                EmailCodeHasher.hash(
                        user.email(),
                        EmailPurpose.UPDATE_PROFILE,
                        verificationCode.strip()));
        if (!valid) {
            throw new BusinessException(ErrorCode.EMAIL_CODE_INVALID);
        }
        accountSecurityService.revokeAllSessions(userId);
        userRepository.softDelete(
                userId,
                "deleted_" + userId,
                "deleted_" + userId + "@deleted.invalid",
                UUID.randomUUID().toString());
        deleteAvatar(user.avatarUrl());
        accountSecurityService.recordEvent(
                userId,
                SecurityEventType.ACCOUNT_DELETED,
                "账号已注销并完成资料脱敏",
                context);
    }

    private void deleteAvatar(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return;
        }
        int separator = avatarUrl.lastIndexOf('/');
        if (separator >= 0 && separator < avatarUrl.length() - 1) {
            avatarStorage.delete(avatarUrl.substring(separator + 1));
        }
    }
}
