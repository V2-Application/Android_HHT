package com.v2retail.dotvik.srm;

/**
 * SRM Portal — Constants
 * All API endpoints are relative to SRM_BASE_URL stored in SharedPreferences as "SRM_URL"
 */
public class SrmVars {

    // ── Endpoints (appended to SRM_BASE_URL) ─────────────────────────────
    public static final String AUTH_LOGIN          = "/api/auth/login";
    public static final String AUTH_LOGOUT         = "/api/auth/logout";
    public static final String AUTH_REFRESH        = "/api/auth/refresh";
    public static final String AUTH_ME             = "/api/auth/me";

    public static final String APPLICATIONS_LIST   = "/api/applications";
    public static final String APPLICATIONS_STATS  = "/api/applications/stats";
    public static final String APPLICATIONS_APPROVE= "/api/applications/%s/approve";
    public static final String APPLICATIONS_REJECT = "/api/applications/%s/reject";

    public static final String DOCUMENTS_LIST      = "/api/documents/%s";
    public static final String DOCUMENTS_UPLOAD    = "/api/documents/%s/upload";
    public static final String DOCUMENTS_DOWNLOAD  = "/api/documents/%s/download/%s";

    public static final String MDM_CHECKLIST_GET   = "/api/mdm/checklist/%s";
    public static final String MDM_CHECKLIST_PUT   = "/api/mdm/checklist/%s";
    public static final String MDM_PUSH_SAP        = "/api/mdm/push-sap/%s";
    public static final String MDM_SAP_STATUS      = "/api/mdm/sap-status";

    public static final String NOTIFICATIONS_LIST  = "/api/notifications";
    public static final String NOTIFICATIONS_READ  = "/api/notifications/%s/read";
    public static final String NOTIFICATIONS_READ_ALL = "/api/notifications/read-all";

    // ── SharedPreferences Keys ────────────────────────────────────────────
    public static final String PREF_SRM_URL        = "SRM_URL";
    public static final String PREF_SRM_TOKEN      = "SRM_ACCESS_TOKEN";
    public static final String PREF_SRM_REFRESH    = "SRM_REFRESH_TOKEN";
    public static final String PREF_SRM_USER_ID    = "SRM_USER_ID";
    public static final String PREF_SRM_USER_NAME  = "SRM_USER_FULL_NAME";
    public static final String PREF_SRM_USER_ROLE  = "SRM_USER_ROLE";
    public static final String PREF_SRM_USER_DEPT  = "SRM_USER_DEPT";
    public static final String PREF_SRM_USERNAME   = "SRM_USERNAME";
    public static final String PREF_SRM_PASSWORD   = "SRM_PASSWORD";

    // ── User Roles ────────────────────────────────────────────────────────
    public static final String ROLE_VENDOR  = "vendor";
    public static final String ROLE_ADMIN   = "admin";
    public static final String ROLE_SUBDIV  = "subdiv";
    public static final String ROLE_DIVHEAD = "divhead";
    public static final String ROLE_FINANCE = "finance";
    public static final String ROLE_POCOMM  = "pocomm";
    public static final String ROLE_MDM     = "mdm";

    // ── Workflow Stages ───────────────────────────────────────────────────
    public static final String STAGE_SUBMITTED = "SUBMITTED";
    public static final String STAGE_L1        = "L1";
    public static final String STAGE_L2        = "L2";
    public static final String STAGE_L3        = "L3";
    public static final String STAGE_L4        = "L4";
    public static final String STAGE_L5        = "L5";
    public static final String STAGE_APPROVED  = "APPROVED";
    public static final String STAGE_REJECTED  = "REJECTED";

    // ── Document Types ────────────────────────────────────────────────────
    public static final String DOC_PAN     = "PAN_CARD";
    public static final String DOC_CHEQUE  = "CANCELLED_CHEQUE";
    public static final String DOC_GST     = "GST_CERTIFICATE";
    public static final String DOC_BILL    = "BILL_COPY";
    public static final String DOC_MSME    = "MSME";
    public static final String DOC_TRADE   = "TRADE_LICENCE";
    public static final String DOC_ISO     = "ISO";

    public static String stageName(String stage) {
        switch (stage == null ? "" : stage) {
            case STAGE_SUBMITTED: return "Submitted";
            case STAGE_L1:        return "Sub-Div Head";
            case STAGE_L2:        return "Division Head";
            case STAGE_L3:        return "Finance Dept";
            case STAGE_L4:        return "PO Committee";
            case STAGE_L5:        return "MDM Team";
            case STAGE_APPROVED:  return "SAP Created";
            case STAGE_REJECTED:  return "Rejected";
            default:              return stage;
        }
    }

    public static int stageIndex(String stage) {
        switch (stage == null ? "" : stage) {
            case STAGE_SUBMITTED: return 0;
            case STAGE_L1:        return 1;
            case STAGE_L2:        return 2;
            case STAGE_L3:        return 3;
            case STAGE_L4:        return 4;
            case STAGE_L5:        return 5;
            case STAGE_APPROVED:  return 6;
            default:              return -1;
        }
    }
}
