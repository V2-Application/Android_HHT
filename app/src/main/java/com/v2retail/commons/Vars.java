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
    public static String ZWM_STORE_IROD_NATURE_MAPPING = "ZWM_STORE_IROD_NATURE_MAPPING";

}
