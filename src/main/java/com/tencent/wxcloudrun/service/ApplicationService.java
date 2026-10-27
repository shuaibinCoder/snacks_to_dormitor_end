package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.dao.*;
import com.tencent.wxcloudrun.dto.Data;
import com.tencent.wxcloudrun.model.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import static com.tencent.wxcloudrun.dto.Data.*;
import static com.tencent.wxcloudrun.constant.SnacksConstants.*;

/** 商家入驻申请、线下收款和审核建店事务。 */
@Service
public class ApplicationService {
  private final MerchantApplicationMapper applications;
  private final MerchantPaymentMapper payments;
  private final StoreMapper stores;
  private final UserMapper users;
  private final IdentityService identity;
  private final NotificationService notices;
  private final EntityView view;

  public ApplicationService(MerchantApplicationMapper applications, MerchantPaymentMapper payments, StoreMapper stores,
      UserMapper users, IdentityService identity, NotificationService notices, EntityView view) {
    this.applications = applications;
    this.payments = payments;
    this.stores = stores;
    this.users = users;
    this.identity = identity;
    this.notices = notices;
    this.view = view;
  }

  /** 首次提交或驳回后重提共用资料白名单；平台内部字段不能由客户端覆盖。 */
  public Data submit(Data e, String openid) {
    Data in = e.object("application");
    MerchantApplication fields = Entities.copy(CatalogService.storeFields(in, true), MerchantApplication.class);
    Store store = identity.owned(openid);
    // 固定先锁最近申请，后续平台审核也锁同一行，防止重提与审核交叉覆盖。
    MerchantApplication old = applications.selectLatest(openid, true);
    require(store == null || !StoreApprovalStatus.APPROVED.matches(store.getApprovalStatus()),
        "你已经拥有审核通过的店铺");
    require(old == null || oneOf(old.getStatus(), codes(ApplicationStatus.DRAFT,
        ApplicationStatus.NEED_MORE_INFO, ApplicationStatus.REJECTED, ApplicationStatus.CONTACT_FAILED)),
        "当前申请正在处理中，请勿重复提交");
    List<Object> rawCategories = in.array("businessCategories");
    require(rawCategories.size() <= MAX_APPLICATION_CATEGORIES
        && rawCategories.stream().allMatch(v -> v instanceof String && ((String) v).length() <= 100), "主营品类不正确");
    List<String> categories = new ArrayList<>();
    for (Object value : rawCategories) {
      categories.add((String) value);
    }
    MerchantApplication application = old == null ? new MerchantApplication() : old;
    applySubmission(application, fields);
    application.setApplicantOpenid(openid);
    application.setWechatContact(in.text("wechatContact"));
    application.setBusinessCategories(categories);
    application.setServiceDescription(in.text("serviceDescription"));
    application.setStatus(ApplicationStatus.SUBMITTED.code());
    application.setSubmittedAt(now());
    application.setPublicMessage("申请已提交，平台工作人员将通过电话联系，请保持手机畅通。");
    if (old == null) {
      application.setContactLogs(new ArrayList<>());
      applications.insert(Entities.create(application));
    } else {
      require(applications.updateSubmission(Entities.update(application)) == 1, "申请已变化，请刷新后重试");
    }
    MerchantApplication saved = applications.selectById(application.getId(), false);
    tell(saved, NotificationType.APPLICATION_SUBMITTED.code(), saved.getPublicMessage(), "待联系",
        MERCHANT_APPLY_PAGE, UserRole.CUSTOMER.code(), BizType.MERCHANT_APPLICATION.code(), saved.getId());
    NotificationType platformType = old == null
        ? NotificationType.NEW_MERCHANT_APPLICATION : NotificationType.APPLICATION_RESUBMITTED;
    Data base = notices.notice("", "", NotificationCategory.PLATFORM.code(), platformType.code(),
        saved.getStoreName() + "等待平台联系处理。", "待联系", BizType.MERCHANT_APPLICATION.code(), saved.getId(),
        ADMIN_APPLICATION_PAGE + "?id=" + saved.getId(),
        DedupeEvent.APPLICATION_SUBMIT.code() + ":" + saved.getId() + ":" + saved.getUpdatedAt());
    notices.platform(base, codes(PlatformRole.OPERATOR, PlatformRole.ADMIN, PlatformRole.SUPER));
    return identity.publicApplication(saved);
  }

  private void applySubmission(MerchantApplication target, MerchantApplication source) {
    target.setStoreName(source.getStoreName());
    target.setOwnerName(source.getOwnerName());
    target.setPhone(source.getPhone());
    target.setNotice(source.getNotice());
    target.setBusinessHours(source.getBusinessHours());
    target.setMinimumOrder(source.getMinimumOrder());
    target.setDeliveryFee(source.getDeliveryFee());
    target.setDeliveryMinutes(source.getDeliveryMinutes());
  }

  /** 平台按状态分页查看申请，申请人不能枚举他人资料。 */
  public List<Data> list(Data e, String openid) {
    identity.requirePlatform(openid,
        codes(PlatformRole.OPERATOR, PlatformRole.ADMIN, PlatformRole.SUPER));
    return view.list(applications.selectByStatus(e.text("status"), MAX_LIST_SIZE));
  }

  /** 返回平台申请、收款和审计详情；申请人视图由 publicApplication 单独脱敏。 */
  public Data detail(Data e, String openid) {
    identity.requirePlatform(openid,
        codes(PlatformRole.OPERATOR, PlatformRole.ADMIN, PlatformRole.SUPER));
    MerchantApplication application = applications.selectById(e.text("applicationId"), false);
    require(application != null, "记录不存在");
    return of("application", view.data(application),
        "payments", view.list(payments.selectByApplication(application.getId(), DEFAULT_PAGE_SIZE)));
  }

  /** 所有工作流推进都携带原状态条件，受影响行数为 0 时整个操作回滚。 */
  public Object operate(Action action, Data e, String openid) {
    boolean approval = action == Action.APPROVE_MERCHANT_APPLICATION
        || action == Action.REJECT_MERCHANT_APPLICATION;
    PlatformUser operator = approval
        ? identity.requirePlatform(openid, codes(PlatformRole.ADMIN, PlatformRole.SUPER))
        : identity.requirePlatform(openid,
            codes(PlatformRole.OPERATOR, PlatformRole.ADMIN, PlatformRole.SUPER));
    MerchantApplication application = applications.selectById(e.required("applicationId", 64), true);
    require(application != null, "记录不存在");
    String current = application.getStatus();
    String id = application.getId();
    String next;
    String type;
    String audit;
    String message;
    Data before = Entities.snapshot(application);
    Data patch = new Data();
    String note = e.text("note");
    switch (action) {
      case MARK_APPLICATION_CONTACTED: {
        require(oneOf(current, codes(ApplicationStatus.SUBMITTED, ApplicationStatus.CONTACT_FAILED)),
            "当前状态不能标记为已联系");
        next = ApplicationStatus.CONTACTED.code();
        type = NotificationType.APPLICATION_CONTACTED.code();
        audit = AuditAction.MARK_APPLICATION_CONTACTED.code();
        message = "平台已完成电话联系，正在确认入驻和付款信息。";
        application.setAssignedOperatorOpenid(openid);
        application.setContactedAt(now());
        contact(application, openid, next, note);
        patch.with("assignedOperatorOpenid", openid, "contactedAt", application.getContactedAt(),
            "contactLogs", application.getContactLogs());
        break;
      }
      case MARK_APPLICATION_CONTACT_FAILED: {
        require(oneOf(current, codes(ApplicationStatus.SUBMITTED, ApplicationStatus.CONTACT_FAILED,
            ApplicationStatus.CONTACTED)), "当前状态不能标记联系失败");
        next = ApplicationStatus.CONTACT_FAILED.code();
        type = NotificationType.APPLICATION_CONTACT_FAILED.code();
        audit = AuditAction.MARK_APPLICATION_CONTACT_FAILED.code();
        message = "平台暂未联系到你，请检查联系电话并保持畅通。";
        contact(application, openid, next, note);
        patch.put("contactLogs", application.getContactLogs());
        break;
      }
      case REQUEST_MORE_INFORMATION: {
        require(oneOf(current, codes(ApplicationStatus.SUBMITTED, ApplicationStatus.CONTACT_FAILED,
            ApplicationStatus.CONTACTED, ApplicationStatus.NEED_MORE_INFO, ApplicationStatus.PAYMENT_PENDING,
            ApplicationStatus.PAID)), "当前申请不能要求补资料");
        next = ApplicationStatus.NEED_MORE_INFO.code();
        type = NotificationType.APPLICATION_NEED_MORE_INFO.code();
        audit = AuditAction.REQUEST_MORE_INFORMATION.code();
        message = e.required("message", 1000);
        application.setInternalNote(e.text("internalNote"));
        patch.put("internalNote", application.getInternalNote());
        break;
      }
      case SET_PAYMENT_PENDING: {
        require(oneOf(current, codes(ApplicationStatus.CONTACTED, ApplicationStatus.SUBMITTED)), "请先完成电话联系");
        next = ApplicationStatus.PAYMENT_PENDING.code();
        type = NotificationType.APPLICATION_PAYMENT_PENDING.code();
        audit = AuditAction.SET_PAYMENT_PENDING.code();
        message = e.text("paymentInstruction", "请按电话沟通方式完成线下付款，付款后联系平台确认。");
        application.setFeeAmount(e.integer("feeAmount", 0, 0, MAX_PAYMENT_AMOUNT_FEN));
        application.setPaymentInstruction(message);
        patch.with("feeAmount", application.getFeeAmount(), "paymentInstruction", message);
        break;
      }
      case CONFIRM_OFFLINE_PAYMENT: {
        require(ApplicationStatus.PAYMENT_PENDING.matches(current), "当前申请不处于待付款状态");
        next = ApplicationStatus.PAID.code();
        type = NotificationType.APPLICATION_PAYMENT_CONFIRMED.code();
        audit = AuditAction.CONFIRM_OFFLINE_PAYMENT.code();
        message = "平台已确认收到线下付款，等待管理员最终审核。";
        String method = e.text("paymentMethod", PaymentMethod.WECHAT_OFFLINE.code());
        require(oneOf(method, codes(PaymentMethod.WECHAT_OFFLINE, PaymentMethod.ALIPAY_OFFLINE,
            PaymentMethod.CASH, PaymentMethod.OTHER)), "付款方式不正确");
        MerchantPayment payment = new MerchantPayment();
        payment.setApplicationId(id);
        payment.setApplicantOpenid(application.getApplicantOpenid());
        payment.setAmount(e.integer("amount", application.getFeeAmount(), 0, MAX_PAYMENT_AMOUNT_FEN));
        payment.setPaymentMethod(method);
        payment.setPaymentTime(e.integer("paymentTime", now(), 1, Long.MAX_VALUE));
        payment.setVoucherFileId(e.text("voucherFileId"));
        payment.setStatus(PaymentStatus.CONFIRMED.code());
        payment.setConfirmedBy(openid);
        payment.setNote(note);
        payments.insert(Entities.create(payment));
        application.setPaidAt(now());
        patch.put("paidAt", application.getPaidAt());
        notices.platform(notices.notice("", "", NotificationCategory.PLATFORM.code(),
            NotificationType.APPLICATION_PAID_PENDING_APPROVAL.code(),
            application.getStoreName() + "已确认付款，请完成最终审核。", "待审核",
            BizType.MERCHANT_APPLICATION.code(), id, ADMIN_APPLICATION_PAGE + "?id=" + id,
            DedupeEvent.PAYMENT_CONFIRMED.code() + ":" + id + ":" + now()),
            codes(PlatformRole.ADMIN, PlatformRole.SUPER));
        break;
      }
      case APPROVE_MERCHANT_APPLICATION:
        return approve(application, operator, note);
      case REJECT_MERCHANT_APPLICATION: {
        require(!oneOf(current, codes(ApplicationStatus.APPROVED, ApplicationStatus.SUSPENDED)),
            "已通过的申请不能直接拒绝，请暂停店铺");
        next = ApplicationStatus.REJECTED.code();
        type = NotificationType.APPLICATION_REJECTED.code();
        audit = AuditAction.REJECT_MERCHANT_APPLICATION.code();
        message = e.required("reason", 1000);
        application.setRejectedAt(now());
        application.setRejectedBy(openid);
        patch.with("rejectedAt", application.getRejectedAt(), "rejectedBy", openid);
        break;
      }
      default:
        throw new BusinessException("未知申请操作", ErrorCode.UNKNOWN_ACTION.code(), HTTP_STATUS_OK);
    }
    application.setStatus(next);
    application.setPublicMessage(message);
    patch.with("status", next, "publicMessage", message);
    require(applications.updateProgress(Entities.update(application), current) == 1, "申请状态已变化，请刷新后重试");
    identity.audit(operator, audit, AuditTargetType.MERCHANT_APPLICATION.code(), id, before, patch, note);
    tell(application, type, message, next, MERCHANT_APPLY_PAGE,
        UserRole.CUSTOMER.code(), BizType.MERCHANT_APPLICATION.code(), id);
    return true;
  }

  private void contact(MerchantApplication application, String openid, String result, String note) {
    List<Data> logs = application.getContactLogs() == null
        ? new ArrayList<>()
        : new ArrayList<>(application.getContactLogs());
    logs.add(of("operatorOpenid", openid, "result", result,
        "resultLabel", ApplicationStatus.CONTACTED.matches(result) ? "联系成功" : "联系未成功",
        "note", note, "time", now()));
    application.setContactLogs(logs);
  }

  /** 店铺、商家角色、申请状态和审计在同一事务提交，任何一步失败都整体回滚。 */
  private Data approve(MerchantApplication application, PlatformUser operator, String note) {
    require(ApplicationStatus.PAID.matches(application.getStatus()), "只有已确认付款的申请可以审核通过");
    Data before = Entities.snapshot(application);
    String owner = application.getApplicantOpenid();
    User user = identity.ensure(owner);
    Store existing = identity.owned(owner);
    Store store = new Store();
    store.setOwnerOpenid(owner);
    store.setApplicationId(application.getId());
    store.setName(application.getStoreName());
    store.setOwnerName(application.getOwnerName());
    store.setPhone(application.getPhone());
    store.setNotice(application.getNotice());
    store.setBusinessHours(application.getBusinessHours());
    store.setMinimumOrder(application.getMinimumOrder());
    store.setDeliveryFee(application.getDeliveryFee());
    store.setDeliveryMinutes(application.getDeliveryMinutes());
    store.setApprovalStatus(StoreApprovalStatus.APPROVED.code());
    store.setPlatformStatus(StorePlatformStatus.ACTIVE.code());
    store.setBusinessStatus(StoreBusinessStatus.OPEN.code());
    store.setApprovedAt(now());
    store.setApprovedBy(operator.getOpenid());
    String storeId;
    if (existing == null) {
      store.setLogoEmoji("🍪");
      store.setRating(BigDecimal.valueOf(5));
      store.setMonthlySales(0);
      stores.insert(Entities.create(store));
      storeId = store.getId();
    } else {
      storeId = existing.getId();
      Store locked = stores.selectById(storeId, true);
      require(locked != null, "记录不存在");
      store.setId(storeId);
      require(stores.updateApproval(Entities.update(store)) == 1, "店铺审核保存失败");
    }
    Set<String> roles = new LinkedHashSet<>(user.getRoles() == null ? Collections.emptyList() : user.getRoles());
    roles.add(UserRole.MERCHANT_OWNER.code());
    users.updateRoles(user.getId(), new ArrayList<>(roles), now());
    application.setStatus(ApplicationStatus.APPROVED.code());
    application.setStoreId(storeId);
    application.setApprovedAt(now());
    application.setApprovedBy(operator.getOpenid());
    application.setPublicMessage("入驻申请已审核通过，店铺已正式上线。");
    require(applications.updateProgress(Entities.update(application), ApplicationStatus.PAID.code()) == 1,
        "申请状态已变化，请刷新后重试");
    Data patch = of("status", ApplicationStatus.APPROVED.code(), "storeId", storeId,
        "approvedAt", application.getApprovedAt(), "approvedBy", operator.getOpenid());
    identity.audit(operator, AuditAction.APPROVE_MERCHANT_APPLICATION.code(),
        AuditTargetType.MERCHANT_APPLICATION.code(), application.getId(), before, patch, note);
    tell(application, NotificationType.APPLICATION_APPROVED.code(),
        application.getStoreName() + "已审核通过并正式上线。", "已通过", MERCHANT_PAGE,
        UserRole.MERCHANT_OWNER.code(), BizType.STORE.code(), storeId);
    return of("storeId", storeId);
  }

  private void tell(MerchantApplication application, String type, String summary, String status, String path,
      String role, String bizType, String bizId) {
    notices.create(notices.notice(application.getApplicantOpenid(), role,
        NotificationCategory.MERCHANT_APPLICATION.code(), type, summary,
        status, bizType, bizId, path, type + ":" + application.getId() + ":" + now()), false);
  }
}
