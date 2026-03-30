package com.v2retail.commons;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Vars {

    public static final String UPC_A = "UPC_A";
    public static final String UPC_E = "UPC_E";
    public static final String EAN_8 = "EAN_8";
    public static final String EAN_13 = "EAN_13";
    public static final String RSS_14 = "RSS_14";

    // Other 1D
    public static final String CODE_39 = "CODE_39";
    public static final String CODE_93 = "CODE_93";
    public static final String CODE_128 = "CODE_128";
    public static final String ITF = "ITF";

    public static final String RSS_EXPANDED = "RSS_EXPANDED";

    // 2D
    public static final String QR_CODE = "QR_CODE";
    public static final String DATA_MATRIX = "DATA_MATRIX";
    public static final String PDF_417 = "PDF_417";

    public static final String PAPER_LESS = "paperless";
    public static final String TVS_PAPER_LESS = "tvs_paperless";
    public static final String TVS_PAPER_LESS_LHU = "tvs_paperless_live_hu";
    public static final String TVS_PRINTER = "TVS_PRINTER";
    public static final String LAST_HU = "last_tvs_paperless_hu";

    public static final String BREADCRUMB_DISPLAY_INTERNAL = "Display > Internal";
    public static final String BREADCRUMB_IROD_TO_IROD = "IROD To IROD";
    public static final String PTL_NEW_MODULE_HU_CLOSE = "PTL_NEW_HU_CLOSE";



    public static final Collection<String> PRODUCT_CODE_TYPES = list(CODE_128, CODE_39, EAN_8, EAN_13, CODE_93, QR_CODE);

    private static List<String> list(String... values) {
        return Collections.unmodifiableList(Arrays.asList(values));
    }
    //QC Cancel Putaway

    public static String PUTAWAY_MODE_QC_FAILED = "Q";
    public static String PUTAWAY_MODE_CANCEL = "C";
    public static String CANCELPUT_VALIDATE_PLANT = "ZECOM_CANCELPUT_VALIDATE_PLANT";
    public static String CANCELPUT_VALIDATE_CRATE = "ZECOM_CANCELPUT_VALIDATE_CRATE";
    public static String CANCELPUT_VALIDATE_BIN = "ZECOM_CANCELPUT_VALIDATE_BIN";
    public static String SAVE_MSA_TO_BIN = "ZECOM_CANCELPUT_MSA_SAVE";
    public static String CANCELPUT_SAVE = "ZECOM_CANCELPUT_SAVE";
    public static String GRT_CRATE_PICK_SECTION_LIST = "ZWM_GET_MSA_SECTION_LIST";
    public static String GRT_CRATE_PICK_LIST = "ZGRT_PICK_GET_TO_LIST";
    public static String GRT_GET_PICK_DATA = "ZGRT_PICK_GET_PICK_DATA";
    public static String GRT_SAVE_PICK_DATA = "ZGRT_PICK_SAVE_PICK_DATA";
    public static String GRT_VALIDATE_CRATE = "ZGRT_PICK_CRATE_VALIDATE";
    public static String GRT_VALIDATE_SAVE_SORT = "ZGRT_PICK_VALIDATE_SAVE_SORT";
    public static String GRT_HU_PICK_VALIDATE = "ZGRT_PICK_VALIDATE_HU";
    public static String GRT_HU_PICK_SAVE = "ZGRT_PICK_SAVE_HU_DATA";
    public static String GRT_ZONE_SORT_VALIDATE_CRATE = "ZGRT_PICK_ZONE_CRATE_VALIDATE";
    public static String GRT_ZONE_SORT_VALIDATE_MSACRATE = "ZGRT_ZONE_MSA_CRATE_VALIDATE";
    public static String GRT_ZONE_SORT_VALIDATE_MSACRATE_SAVE = "ZGRT_PICK_MSA_VALIDATE_SAVE";
    public static String GRT_ZONE_SORT_EMPTY_PICKED_CRATE = "ZGRT_PICK_EMPTY_PICKED_CRATE";
    public static String GRT_ZONE_SORT_CRATE_SAVE = "ZGRT_PICK_ZVALIDATE_SAVE_SORT";
    public static String GRT_PUTAWAY_VALIDATE_CRATE = "ZGRT_RETURN_CRATE_VALIDATE";
    public static String GRT_PUTAWAY_VALIDATE_CRATE_SAVE = "ZGRT_ZONE_CRATE_VALIDATE_SAVE";
    public static String GRT_PUTAWAY_VALIDATE_ARTICLE = "ZGRT_RETURN_GET_EAN_DATA";
    public static String GRT_PUTAWAY_SAVE = "ZGRTRET_SAVE_PUTAWAY_DETAILS";
    public static String GRT_CTOMSA_VALIDATE_CRATE = "ZGRT_CANCELPUT_VALIDATE_CRATE";
    public static String GRT_CTOMSA_VALIDATE_BIN = "ZGRT_CANCELPUT_VALIDATE_BIN";
    public static String GRT_CTOMSA_SAVE = "ZGRTRET_CRATE_TO_MSA_SAVE";
    public static String GRT_STORE_PUTWALL_VALIDATE_STORE = "ZGRT_HU_STORE_VALIDATE";
    public static String GRT_STORE_PUTWALL_VALIDATE_HU = "ZGRT_HU_VALIDATE";
    public static String GRT_SINGLE_PICK_TAG_HU_VALIDATE_STORE = "ZGRT_SIN_STORE_VALIDATE";
    public static String GRT_SINGLE_PICK_TAG_HU_VALIDATE_HU_ZONE = "ZGRT_HU_ZONE_VALIDATE";
    public static String GRT_SINGLE_PICK_CRATE_SIN_VALIDATE = "ZGRT_PICK_CRATE_SIN_VALIDATE";
    public static String GRT_SINGLE_PICK_ZONE_CRATE_SIN_VALIDATE = "ZGRT_ZONE_CRATE_SIN_VALIDATE";
    public static String GRT_MIX_XIN_SORT_VALIDATE = "ZGRT_MIX_SIN_SORT_VALIDATE";
    public static String GRT_SIN_CRATE_COMBO_VALIDATE = "ZGRT_SIN_CRATE_COMBO_VALIDATE";
    public static String GRT_COMBO_ZONE_VALIDATE = "ZGRT_COMBO_ZONE_VALIDATE";
    public static String ZGRT_COMBO_ZONE_MSA_CRATE_VALIDATE = "ZGRT_ZONE_MSA_CRATE_VALIDATE";
    public static String ZCOMBO_VALIDATE_SAVE_SORT = "ZCOMBO_PICK_VALIDATE_SAVE_SORT";
    public static String ZCOMBO_PICK_VALIDATE_HU_SAVE = "ZCOMBO_PICK_VALIDATE_HU_SAVE";
    public static String ZCOMBO_ZONE_CRATE_VALIDATE_SAVE = "ZGRT_ZONE_CRATE_VALIDATE_SAVE";
    public static String ZFMS_SCREEN = "ZFMS_SCREEN";
    public static String ZFMS_CRATE_GET_DATA = "ZFMS_CRATE_GET_DATA";

    public static String PTL_PICK_FULL_CRATE = "F";
    public static String PTL_PICK_PARTIAL = "P";
    public static String PTL_PICK_SECTION_LIST = "ZWM_GET_MSA_SECTION_LIST";
    public static String PTL_GET_PICK_LIST = "ZADVERB_PICK_GET_TO_LIST";
    public static String PTL_GET_PICK_DATA = "ZADVERB_GET_PICK_DATA";
    public static String PTL_VALIDATE_CRATE_FOR_PICKDATA = "ZADVERB_CRATE_VALIDATE";
    public static String PTL_SAVE_PICK_DATA = "ZADVERB_SAVE_PICK_DATA";
    public static String PTL_PUTAWAY_VALIDATE_CRATE = "ZPTL_RETURN_CRATE_VALIDATE";
    public static String PTL_PUTAWAY_VALIDATE_ARTICLE = "ZPTL_RETURN_GET_EAN_DATA";
    public static String PTL_PUTAWAY_SAVE = "ZPTLRET_SAVE_PUTAWAY_DETAILS";
    public static String PTL_CTOMSA_VALIDATE_CRATE = "ZPTL_CANCELPUT_VALIDATE_CRATE";
    public static String PTL_CTOPTL_VALIDATE_CRATE = "ZADVERB_CRATE_VALI_TO_CONV";
    public static String PTL_CTOMSA_VALIDATE_BIN = "ZPTL_CANCELPUT_VALIDATE_BIN";
    public static String PTL_CTOMSA_SAVE = "ZPTLRET_CRATE_TO_MSA_SAVE";
    public static String ZOMINI_PICK_STORE = "ZOMINI_PICK_STORE";
    public static String ZOMINI_BIN_BARCODE_VALIDATE = "ZOMINI_BIN_BARCODE_VALIDATE";
    public static String ZOMINI_PICK_STORE_SAVE_TT = "ZOMINI_PICK_STORE_SAVE_TT";
    public static String ZCOMBO_VALIDATE_PALLETE = "ZCOMBO_VALIDATE_PALLETE";
    public static String ZCOMBO_VALIDATE_CRATE = "ZCOMBO_VALIDATE_CRATE";
    public static String ZCOMBO_VALIDATE_PALLETE_REC = "ZCOMBO_VALIDATE_PALLETE_REC";

    public static String PICKING_WITH_CONS_VALIDATE_PICKLIST = "ZWM_RFC_STORE_PICK_VALIDATE";
    public static String PICKING_WITH_CONS_VALIDATE_SOURCE_BIN = "ZWM_STORE_PICKLIST_BIN";
    public static String PICKING_WITH_CONS_VALIDATE_SCAN_BIN = "ZSDC_STORE_PICKBIN_VALIDATION";
    public static String PICKING_WITH_CONS_SAVE_PICK_DATA = "ZSDC_STORE_PICKBIN_SAVE";

    public static String ZFM_HU_WGT = "ZFM_HU_WGT";
    public static String ZFM_HU_WGT_SAVE = "ZFM_HU_WGT_SAVE";
    public static String ZGRT_HU_VALIDATE_PICK_CLOSE = "ZGRT_HU_VALIDATE_PICK_CLOSE";

    //Store Order Reject
    public static String SAVE_STORE_ORDER_REJECT = "ZECOM_STORE_ORDER_REJECT";

    public static String ZWM_STORE_BIN_001_VALIDATION = "ZWM_STORE_BIN_001_VALIDATION";
    public static String ZWM_STORE_IROD_BIN_POST = "ZWM_STORE_IROD_BIN_POST";
    public static String ZWM_STORE_IROD_ARTICLE_FIND = "ZWM_STORE_IROD_ARTICLE_FIND";
    public static String ZWM_STORE_BIN_STOCK = "ZWM_STORE_BIN_STOCK";
    public static String ZWM_STORE_RETURN_TO_MSA = "ZWM_STORE_RETURN_TO_MSA";

    public static String ZWM_STORE_GANDOLA_VALIDATE = "ZWM_STORE_GANDOLA_VALIDATE";
    public static String ZWM_STORE_IROD_VALIDATE = "ZWM_STORE_IROD_VALIDATE";
    public static String ZWM_STORE_IROD_TAG = "ZWM_STORE_IROD_TAG";
    public static String ZWM_STORE_IROD_DTAG_VALIDATE = "ZWM_STORE_IROD_DTAG_VALIDATE";
    public static String ZWM_STORE_IROD_DTAG = "ZWM_STORE_IROD_DTAG";
    public static String ZWM_STORE_IROD_NATURE = "ZWM_STORE_IROD_NATURE";
    public static String ZWM_STORE_GRT_CATEGORY = "ZWM_STORE_GRT_CATEGORY";
    public static String ZWM_STORE_IROD_NATURE_MAPPING = "ZWM_STORE_IROD_NATURE_MAPPING";

    public static String ZWM_STORE_IROD_PICK_VALIDATE = "ZWM_STORE_IROD_PICK_VALIDATE";
    public static String ZWM_STORE_IROD_PUTWAY_VALIDATE = "ZWM_STORE_IROD_PUTWAY_VALIDATE";
    public static String ZWM_STORE_IROD_PUT = "ZWM_STORE_IROD_PUT";
    public static String ZWM_STORE_IROD_EAN_VALIDATE = "ZWM_STORE_IROD_EAN_VALIDATE";
    public static String ZWM_STORE_IROD_PICK = "ZWM_STORE_IROD_PICK";
    public static String ZWM_STORE_IROD_EMPTY = "ZWM_STORE_IROD_EMPTY";
    public static String ZWM_STORE_IROD_EAN_STOCK_V01 = "ZWM_STORE_IROD_EAN_STOCK_V01";
    public static String ZWM_STORE_IROD_PUTWAY_VALID1 = "ZWM_STORE_IROD_PUTWAY_VALID1";

    public static String ZWM_STORE_IROD_TRAN_VALIDATE = "ZWM_STORE_IROD_TRAN_VALIDATE";
    public static String ZWM_STORE_IROD_GANDOLA_TAG = "ZWM_STORE_IROD_GANDOLA_TAG";
    public static String ZWM_PRINT_HU_TVS = "ZWM_PRINT_HU_TVS";

    public static String ZWM_STK_IROD_GANDOLA_0001 = "ZWM_STK_IROD_GANDOLA_0001";
    public static String ZWM_STORE_GRT_FROM_DISP_AREA = "ZWM_STORE_GRT_FROM_DISP_AREA";
    public static String ZWM_GET_PACKING_MATERIAL = "ZWM_GET_PACKING_MATERIAL";
    public static String ZWM_STORE_GET_STOCK = "ZWM_STORE_GET_STOCK";

    public static String ZGRT_VALIDATE_HU = "ZGRT_VALIDATE_HU";
    public static String ZGRT_VALIDATE_HU_BIN = "ZGRT_VALIDATE_HU_BIN";
    public static String ZWM_VALIDATE_PALATE = "ZWM_VALIDATE_PALATE";
    public static String ZWM_VALIDATE_PALATE_HU = "ZWM_VALIDATE_PALATE_HU";
    public static String ZWM_PALATE_HU_SAVE = "ZWM_PALATE_HU_SAVE";
    public static String ZWM_VALIDATE_PAL_BIN = "ZWM_VALIDATE_PAL_BIN";
    public static String ZWM_VALIDATE_PAL_TO_BIN = "ZWM_VALIDATE_PAL_TO_BIN";

    public static String ZWM_PALATE_VALIDATION = "ZWM_PALATE_VALIDATION";
    public static String ZWM_CRATE_VALIDATION = "ZWM_CRATE_VALIDATION";
    public static String ZWM_SAVE_PAL_CRATE = "ZWM_SAVE_PAL_CRATE";

    public static String ZWM_PALATE_RECEIVE = "ZWM_PALATE_RECEIVE";
    public static String ZWM_EXTERNAL_HU_VALIDATE = "ZWM_EXTERNAL_HU_VALIDATE";
    public static String ZWM_ACTUAL_HU_SAVE = "ZWM_ACTUAL_HU_SAVE";

    public static String ZWM_BIN_VALIDATION_PUT = "ZWM_BIN_VALIDATION_PUT";
    public static String ZWM_HU_VALIDATION_PUT = "ZWM_HU_VALIDATION_PUT";

    public static String ZWM_PICKLIST_PPPN = "ZWM_PICKLIST_PPPN";
    public static String ZWM_PICK_SAVE_RFC = "ZWM_PICK_SAVE_RFC";

    public static String ZWM_USER_AUTHORITY_CHECK = "ZWM_USER_AUTHORITY_CHECK";
    public static String ZWM_INV_GRC_VALIDATION = "ZWM_INV_GRC_VALIDATION";
    public static String ZWM_INV_GRC_HUB_SAVE = "ZWM_INV_GRC_HUB_SAVE";

    public static String ZHUB_HU_VALIDATION_RFC = "ZHUB_HU_VALIDATION_RFC";
    public static String ZDIS_HU_DC_HUB_PRO_RFC = "ZDIS_HU_DC_HUB_PRO_RFC";

    public static String ZWM_PTL_GET_ZONE = "ZWM_PTL_GET_ZONE";
    public static String ZWM_GET_MSA_SECTION_LIST = "ZWM_GET_MSA_SECTION_LIST";
    public static String ZGRT_PICK_GET_TO_LIST_PTL = "ZGRT_PICK_GET_TO_LIST_PTL";
    public static String ZGRT_PICK_GET_TO_LIST_PTL_V2 = "ZGRT_PICK_GET_TO_LIST_PTL_V2";
    public static String ZWM_PTL_GET_TO_DETAILS = "ZWM_PTL_GET_TO_DETAILS";
    public static String ZWM_PTL_PALETTE_VALIDATE = "ZWM_PTL_PALETTE_VALIDATE";
    public static String ZWM_PTL_CRATE_VALIDATE = "ZWM_PTL_CRATE_VALIDATE";
    public static String ZWM_PTL_BIN_VALIDATE = "ZWM_PTL_BIN_VALIDATE";
    public static String ZWM_PTL_MSA_CRATE_VALIDATE = "ZWM_PTL_MSA_CRATE_VALIDATE";
    public static String ZWM_PTL_MSA_CRATE_VALIDATE_V2 = "ZWM_PTL_MSA_CRATE_VALIDATE_V2";
    public static String ZWM_PTL_V06_V09 = "ZWM_PTL_V06_V09";

    public static String ZWM_PTL_FLOOR_STAGING_RFC = "ZWM_PTL_FLOOR_STAGING_RFC";
    public static String ZWM_PTL_FLOOR_RECEIVING_RFC = "ZWM_PTL_FLOOR_RECEIVING_RFC";
    public static String ZWM_PTL_STORE_STAGING_RFC = "ZWM_PTL_STORE_STAGING_RFC";

    public static String    ZWM_PTL_ZONE_REC = "ZWM_PTL_ZONE_REC";

    public static String ZWM_PTL_VALIDATE_STORE = "ZWM_PTL_VALIDATE_STORE";
    public static String ZWM_PTL_VALIDATE_HU = "ZWM_PTL_VALIDATE_HU";

    public static String ZWM_PTL_HU_VALIDATE_CLOSE = "ZWM_PTL_HU_VALIDATE_CLOSE";
    public static String ZWM_PTL_TVS_HU_PRINT = "ZWM_PTL_TVS_HU_PRINT";
    public static String ZWM_PTL_TVS_HU_PRINT_2 = "ZWM_PTL_TVS_HU_PRINT_2";

    public static String ZWM_PTL_ZONE_CRATE_VALIDATE = "ZWM_PTL_ZONE_CRATE_VALIDATE";
    public static String ZWM_PTL_ZONE_HU_VALIDATE = "ZWM_PTL_ZONE_HU_VALIDATE";

    public static String ZWM_PTL_FLR_STG_PAL_VALIDAT_V2 = "ZWM_PTL_FLR_STG_PAL_VALIDAT_V2";
    public static String ZWM_PTL_FLR_STG_CRT_VALID_V2 = "ZWM_PTL_FLR_STG_CRT_VALID_V2";
    public static String ZGRT_PICK_GET_TO_LIST_PTL_V3 = "ZGRT_PICK_GET_TO_LIST_PTL_V3";
    public static String ZWM_PTL_CRATE_VALIDATE_V2 = "ZWM_PTL_CRATE_VALIDATE_V2";

    public static String ZPTL_GET_DATA_FROM_BIN_RFC = "ZPTL_GET_DATA_FROM_BIN_RFC";
    public static String ZWM_PTL_BIN_VALIDATE_V3 = "ZWM_PTL_BIN_VALIDATE_V3";
    public static String ZWM_PTL_MSA_CRATE_VALIDATE_V3 = "ZWM_PTL_MSA_CRATE_VALIDATE_V3";
    public static String ZWM_PTL_FLR_STG_PAL_VALIDAT_V3 = "ZWM_PTL_FLR_STG_PAL_VALIDAT_V3";
    public static String ZWM_PTL_FLR_STG_CRT_VALID_V3 = "ZWM_PTL_FLR_STG_CRT_VALID_V3";
    public static String ZPTL_FLR_TAG_BIN_VAL_RFC_V3 = "ZPTL_FLR_TAG_BIN_VAL_RFC_V3";
    public static String ZPTL_PLT_VALIDATE_BIN_TAG_RFC = "ZPTL_PLT_VALIDATE_BIN_TAG_RFC";
    public static String ZPTL_GF_SEC_RFC_V3 = "ZPTL_GF_SEC_RFC_V3";
    public static String ZWM_PTL_FLOOR_RECEIVING_RFC_V3 = "ZWM_PTL_FLOOR_RECEIVING_RFC_V3";
    public static String ZPTL_STN_LIST_RCV_AT_HUBSTN_V3 = "ZPTL_STN_LIST_RCV_AT_HUBSTN_V3";
    public static String ZPTL_PLT_RCV_AT_HUBSTN_RFC = "ZPTL_PLT_RCV_AT_HUBSTN_RFC";
    public static String ZWM_PTL_HUBSTN_DATA_RFC_V3 = "ZWM_PTL_HUBSTN_DATA_RFC_V3";
    public static String ZWM_PTL_CRT_TAG_VAL_RFC_V3 = "ZWM_PTL_CRT_TAG_VAL_RFC_V3";
    public static String ZWM_PTL_CRATE_VALIDATE_V3 = "ZWM_PTL_CRATE_VALIDATE_V3";
    public static String ZWM_PTL_SZ_PALATE_VALIDATE_V3 = "ZWM_PTL_SZ_PALATE_VALIDATE_V3";
    public static String ZWM_PTL_MZ_SART_HUBCRT_VALI_V3 = "ZWM_PTL_MZ_SART_HUBCRT_VALI_V3";
    public static String ZWM_PTL_GET_ZONE_STATION_V3 = "ZWM_PTL_GET_ZONE_STATION_V3";
    public static String ZWM_PTL_VALIDATE_STORE_V3 = "ZWM_PTL_VALIDATE_STORE_V3";
    public static String ZWM_PTL_VALIDATE_HU_V3 = "ZWM_PTL_VALIDATE_HU_V3";
    public static String ZWM_PTL_PLT_REC_AT_ZONE_SRT_V3 = "ZWM_PTL_PLT_REC_AT_ZONE_SRT_V3";
    public static String ZGRT_PICK_GET_PICK_DATA_V4 = "ZGRT_PICK_GET_PICK_DATA_V4";
    public static String ZWM_PTL_CRATE_VALIDATE_V4 = "ZWM_PTL_CRATE_VALIDATE_V4";
    public static String ZWM_PTL_MSA_CRATE_VALIDATE_V4 = "ZWM_PTL_MSA_CRATE_VALIDATE_V4";
    public static String ZWM_PTL_PLT_REC_AT_ZONE_SRT_V4 = "ZWM_PTL_PLT_REC_AT_ZONE_SRT_V4";
    public static String ZWM_PTL_ZONE_CRATE_VALIDATE_V3 = "ZWM_PTL_ZONE_CRATE_VALIDATE_V3";
    public static String ZWM_PTL_ZONE_HU_VALIDATE_V3 = "ZWM_PTL_ZONE_HU_VALIDATE_V3";
    public static String ZSTORE_DISCOUNT_GET_EAN_DATA = "ZSTORE_DISCOUNT_GET_EAN_DATA";
    public static String ZSTORE_DISCOUNT_SAVE_EAN_DATA = "ZSTORE_DISCOUNT_SAVE_EAN_DATA";
    public static String ZSDC_DIRECT_FLR_RFC = "ZSDC_DIRECT_FLR_RFC";
    public static String ZSDC_DIRECT_ART_VAL_BARCOD_RFC = "ZSDC_DIRECT_ART_VAL_BARCOD_RFC";
    public static String ZSDC_DIRECT_ART_VAL1_SAVE1_RFC = "ZSDC_DIRECT_ART_VAL1_SAVE1_RFC";
    public static String ZBIN_GRT_HU_VALIDATION = "ZBIN_GRT_HU_VALIDATION";
    public static String ZBIN_GRT_PICKLIST_VALIDATION = "ZBIN_GRT_PICKLIST_VALIDATION";
    public static String ZBIN_GRT_BIN_DATA = "ZBIN_GRT_BIN_DATA";
    public static String ZBIN_GRT_DATA_SAVE = "ZBIN_GRT_DATA_SAVE";
    public static String ZWM_CRATE_IDENTIFIER_RFC = "ZWM_CRATE_IDENTIFIER_RFC";
    public static String ZWM_BIN_IDENTIFIER_RFC = "ZWM_BIN_IDENTIFIER_RFC";
    public static String ZSDC_DIRECT_HU_VAL_RFC = "ZSDC_DIRECT_HU_VAL_RFC";
    public static String ZSDC_DIRECT_SAVE_RFC = "ZSDC_DIRECT_SAVE_RFC";
    public static String ZWM_GET_STOCK_BIN = "ZWM_GET_STOCK_BIN";
    public static String ZWM_GET_STOCK_TAKE_ID = "ZWM_GET_STOCK_TAKE_ID";
    public static String ZWM_STK_ADJ_MSA_BIN = "ZWM_STK_ADJ_MSA_BIN";
    public static String ZWM_LIVE_STOCK_SCANNING = "ZWM_LIVE_STOCK_SCANNING";

    // HU Swap Print process
    public static String ZWM_HUSWAP        = "ZWM_HUSWAP";
    public static String HU_SWAP_PROCESS   = "HU_SWAP_PRINT";
}
