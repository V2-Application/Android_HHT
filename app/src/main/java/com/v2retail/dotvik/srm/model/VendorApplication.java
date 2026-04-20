package com.v2retail.dotvik.srm.model;

import org.json.JSONObject;

public class VendorApplication {
    public String id;
    public String ticketNo;
    public String vendorName;
    public String vendorType;
    public String businessType;
    public String vendorRelation;
    public String companyCode;
    public String division;
    public String vendorAddress;
    public String city;
    public String state;
    public String postalCode;
    public String country = "India";
    public String contactPerson;
    public String mobile;
    public String email;
    public String website;
    public String landline;

    // Banking
    public String bankName;
    public String bankAccountNo;
    public String bankBranch;
    public String ifscCode;
    public String panNo;
    public String gstin;
    public String serviceTaxNo;
    public String withholdingTaxCode;

    // Buying
    public String merchandiseCategory;
    public String paymentTermsDays;
    public String vrp;
    public String articleToPurchase;
    public String buyingPerMonth;
    public String purchaseValuePa;
    public String otherBuyers;

    // Status
    public String currentStage;
    public String sapVendorCode;
    public String submittedAt;

    public JSONObject toJson() {
        JSONObject j = new JSONObject();
        try {
            j.put("vendor_type",         nullSafe(vendorType));
            j.put("business_type",       nullSafe(businessType));
            j.put("vendor_relation",     nullSafe(vendorRelation));
            j.put("company_code",        nullSafe(companyCode));
            j.put("division",            nullSafe(division));
            j.put("vendor_name",         nullSafe(vendorName));
            j.put("vendor_address",      nullSafe(vendorAddress));
            j.put("city",                nullSafe(city));
            j.put("state",               nullSafe(state));
            j.put("postal_code",         nullSafe(postalCode));
            j.put("country",             nullSafe(country));
            j.put("contact_person",      nullSafe(contactPerson));
            j.put("mobile",              nullSafe(mobile));
            j.put("email",               nullSafe(email));
            j.put("website",             nullSafe(website));
            j.put("landline",            nullSafe(landline));
            j.put("bank_name",           nullSafe(bankName));
            j.put("bank_account_no",     nullSafe(bankAccountNo));
            j.put("bank_branch",         nullSafe(bankBranch));
            j.put("ifsc_code",           nullSafe(ifscCode));
            j.put("pan_no",              nullSafe(panNo));
            j.put("gstin",               nullSafe(gstin));
            j.put("service_tax_no",      nullSafe(serviceTaxNo));
            j.put("withholding_tax_code",nullSafe(withholdingTaxCode));
            j.put("merchandise_category",nullSafe(merchandiseCategory));
            j.put("payment_terms_days",  nullSafe(paymentTermsDays));
            j.put("vrp",                 nullSafe(vrp));
            j.put("article_to_purchase", nullSafe(articleToPurchase));
            j.put("buying_per_month",    nullSafe(buyingPerMonth));
            j.put("purchase_value_pa",   nullSafe(purchaseValuePa));
            j.put("other_buyers",        nullSafe(otherBuyers));
        } catch (Exception ignored) {}
        return j;
    }

    public static VendorApplication fromJson(JSONObject j) {
        VendorApplication v = new VendorApplication();
        v.id            = j.optString("id");
        v.ticketNo      = j.optString("ticket_no");
        v.vendorName    = j.optString("vendor_name");
        v.vendorType    = j.optString("vendor_type");
        v.division      = j.optString("division");
        v.currentStage  = j.optString("current_stage");
        v.sapVendorCode = j.optString("sap_vendor_code");
        v.submittedAt   = j.optString("submitted_at");
        v.panNo         = j.optString("pan_no");
        v.gstin         = j.optString("gstin");
        v.mobile        = j.optString("mobile");
        v.city          = j.optString("city");
        v.state         = j.optString("state");
        v.contactPerson = j.optString("contact_person");
        v.companyCode   = j.optString("company_code");
        v.bankName      = j.optString("bank_name");
        v.ifscCode      = j.optString("ifsc_code");
        return v;
    }

    private String nullSafe(String s) { return s != null ? s : ""; }
}
