package org.guanzon.cas.inv.model;

import java.sql.SQLException;
import java.util.Date;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.ReferenceCache;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.Logical;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.cas.inv.services.InvModels;
import org.guanzon.cas.parameter.model.Model_Brand;
import org.guanzon.cas.parameter.model.Model_Category;
import org.guanzon.cas.parameter.model.Model_Category_Level2;
import org.guanzon.cas.parameter.model.Model_Category_Level3;
import org.guanzon.cas.parameter.model.Model_Category_Level4;
import org.guanzon.cas.parameter.model.Model_Color;
import org.guanzon.cas.parameter.model.Model_Industry;
import org.guanzon.cas.parameter.model.Model_Inv_Type;
import org.guanzon.cas.parameter.model.Model_Measure;
import org.guanzon.cas.parameter.model.Model_Model;
import org.guanzon.cas.parameter.model.Model_Model_Variant;
import org.guanzon.cas.parameter.services.ParamModels;
import org.json.simple.JSONObject;

public class Model_Inventory extends Model {

    //All reference fields below are intentionally NOT constructed in initialize() - see their
    //accessors, which build them lazily on first access so opening this record never touches
    //those tables.
    private Model_Industry poIndustry;
    private Model_Category poCategory;
    private Model_Category_Level2 poCategoryLevel2;
    private Model_Category_Level3 poCategoryLevel3;
    private Model_Category_Level4 poCategoryLevel4;
    private Model_Brand poBrand;
    private Model_Model poModel;
    private Model_Color poColor;
    private Model_Measure poMeasure;
    private Model_Inv_Type poInventoryType;
    private Model_Model_Variant poVariant;
    private Model_InventorySuperseded poSuperseded;

    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);

            poEntity.insertRow();
            poEntity.moveToCurrentRow();

            poEntity.absolute(1);
            
            //assign default values
            poEntity.updateObject("nUnitPrce", 0.00);
            poEntity.updateObject("nSelPrice", 0.00);
            poEntity.updateObject("nDiscLev1", 0.00);
            poEntity.updateObject("nDiscLev2", 0.00);
            poEntity.updateObject("nDiscLev3", 0.00);
            poEntity.updateObject("nDealrDsc", 0.00);
            poEntity.updateObject("nMinLevel", 0);
            poEntity.updateObject("nMaxLevel", 0);
            poEntity.updateObject("nShlfLife", 0);
            poEntity.updateString("cComboInv", Logical.NO);
            poEntity.updateString("cWthPromo", Logical.NO);
            poEntity.updateString("cSerialze", Logical.NO);
            poEntity.updateString("cUnitType", Logical.NO);
            poEntity.updateString("cInvStatx", RecordStatus.ACTIVE);
            poEntity.updateString("cRecdStat", RecordStatus.ACTIVE);
            poEntity.updateNull("sAltBarCd");
            poEntity.updateNull("sCategCd1");
            poEntity.updateNull("sCategCd2");
            poEntity.updateNull("sCategCd3");
            poEntity.updateNull("sCategCd4");
            poEntity.updateNull("sBrandIDx");
            poEntity.updateNull("sModelIDx");
            poEntity.updateNull("sColorIDx");
            poEntity.updateNull("sVrntIDxx");
            poEntity.updateNull("sMeasurID");
            poEntity.updateNull("sInvTypCd");
            poEntity.updateNull("sSupersed");
            poEntity.updateObject("dModified", poGRider.getServerDate());
            //end - assign default values

            ID = poEntity.getMetaData().getColumnLabel(1);

            pnEditMode = EditMode.UNKNOWN;
        } catch (SQLException e) {
            logwrapr.severe(e.getMessage());
            System.exit(1);
        }
    }

    public JSONObject setStockId(String stockId) {
        return setValue("sStockIDx", stockId);
    }

    public String getStockId() {
        return (String) getValue("sStockIDx");
    }

    public JSONObject setBarCode(String barCode) {
        return setValue("sBarCodex", barCode);
    }

    public String getBarCode() {
        return (String) getValue("sBarCodex");
    }

    public JSONObject setDescription(String description) {
        return setValue("sDescript", description);
    }

    public String getDescription() {
        return (String) getValue("sDescript");
    }

    public JSONObject setBriefDescription(String briefDescription) {
        return setValue("sBriefDsc", briefDescription);
    }

    public String getBriefDescription() {
        return (String) getValue("sBriefDsc");
    }

    public JSONObject setAlternateBarCode(String alternateBarCode) {
        return setValue("sAltBarCd", alternateBarCode);
    }

    public String getAlternateBarCode() {
        return (String) getValue("sAltBarCd");
    }

    public JSONObject setCategoryFirstLevelId(String cagetoryId) {
        return setValue("sCategCd1", cagetoryId);
    }

    public String getCategoryFirstLevelId() {
        return (String) getValue("sCategCd1");
    }

    public JSONObject setCategoryIdSecondLevel(String cagetoryId) {
        return setValue("sCategCd2", cagetoryId);
    }

    public String getCategoryIdSecondLevel() {
        return (String) getValue("sCategCd2");
    }

    public JSONObject setCategoryIdThirdLevel(String cagetoryId) {
        return setValue("sCategCd3", cagetoryId);
    }

    public String getCategoryIdThirdLevel() {
        return (String) getValue("sCategCd3");
    }

    public JSONObject setCategoryIdFourthLevel(String cagetoryId) {
        return setValue("sCategCd4", cagetoryId);
    }

    public String getCategoryIdFourthLevel() {
        return (String) getValue("sCategCd4");
    }

    public JSONObject setBrandId(String brandId) {
        return setValue("sBrandIDx", brandId);
    }

    public String getBrandId() {
        return (String) getValue("sBrandIDx");
    }

    public JSONObject setModelId(String modelID) {
        return setValue("sModelIDx", modelID);
    }

    public String getModelId() {
        return (String) getValue("sModelIDx");
    }

    public JSONObject setColorId(String colorID) {
        return setValue("sColorIDx", colorID);
    }

    public String getColorId() {
        return (String) getValue("sColorIDx");
    }

    public JSONObject setVariantId(String variantId) {
        return setValue("sVrntIDxx", variantId);
    }

    public String getVariantId() {
        return (String) getValue("sVrntIDxx");
    }

    public JSONObject setMeasurementId(String measurementId) {
        return setValue("sMeasurID", measurementId);
    }

    public String getMeasurementId() {
        return (String) getValue("sMeasurID");
    }

    public JSONObject setInventoryTypeId(String inventoryTypeId) {
        return setValue("sInvTypCd", inventoryTypeId);
    }

    public String getInventoryTypeId() {
        return (String) getValue("sInvTypCd");
    }

    public JSONObject setIndustryCode(String industryCode) {
        return setValue("sIndstCdx", industryCode);
    }

    public String getIndustryCode() {
        return (String) getValue("sIndstCdx");
    }

    public JSONObject setCost(Number cost) {
        return setValue("nUnitPrce", cost);
    }

    public Number getCost() {
        return (Number) getValue("nUnitPrce");
    }

    public JSONObject setSellingPrice(Number sellingPrice) {
        return setValue("nSelPrice", sellingPrice);
    }

    public Number getSellingPrice() {
        return (Number) getValue("nSelPrice");
    }

    public JSONObject setDiscountRateLevel1(Number discountRate) {
        return setValue("nDiscLev1", discountRate);
    }

    public Number getDiscountRateLevel1() {
        return (Number) getValue("nDiscLev1");
    }

    public JSONObject setDiscountRateLevel2(Number discountRate) {
        return setValue("nDiscLev2", discountRate);
    }

    public Number getDiscountRateLevel2() {
        return (Number) getValue("nDiscLev2");
    }

    public JSONObject setDiscountRateLevel3(Number discountRate) {
        return setValue("nDiscLev3", discountRate);
    }

    public Number getDiscountRateLevel3() {
        return (Number) getValue("nDiscLev3");
    }

    public JSONObject setDealerDiscountRate(Number discountRate) {
        return setValue("nDealrDsc", discountRate);
    }

    public Number getDealerDiscountRate() {
        return (Number) getValue("nDealrDsc");
    }

    public JSONObject setMinimumInventoryLevel(int quantity) {
        return setValue("nMinLevel", quantity);
    }

    public int getMinimumInventoryLevel() {
        return (int) getValue("nMinLevel");
    }

    public JSONObject setMaximumInventoryLevel(int quantity) {
        return setValue("nMaxLevel", quantity);
    }

    public int getMaximumInventoryLevel() {
        return (int) getValue("nMaxLevel");
    }

    public JSONObject isComboInventory(boolean isComboInventory) {
        return setValue("cComboInv", isComboInventory ? "1" : "0");
    }

    public boolean isComboInventory() {
        return ((String) getValue("cComboInv")).equals("1");
    }

    public JSONObject isWithPromo(boolean isWithPromo) {
        return setValue("cWthPromo", isWithPromo ? "1" : "0");
    }

    public boolean isWithPromo() {
        return ((String) getValue("cWthPromo")).equals("1");
    }

    public JSONObject isSerialized(boolean isSerialized) {
        return setValue("cSerialze", isSerialized ? "1" : "0");
    }

    public boolean isSerialized() {
        return ((String) getValue("cSerialze")).equals("1");
    }

    public JSONObject setUnitType(String unitType) {
        return setValue("cUnitType", unitType);
    }

    public String getUnitType() {
        return (String) getValue("cUnitType");
    }

    public JSONObject setInventoryStatus(String inventoryStatus) {
        return setValue("cInvStatx", inventoryStatus);
    }

    public String getInventoryStatus() {
        return (String) getValue("cInvStatx");
    }

    public JSONObject setShelfLife(int days) {
        return setValue("nShlfLife", days);
    }

    public int getShelfLife() {
        return (int) getValue("nShlfLife");
    }

    public JSONObject setSupersededId(String supersededId) {
        return setValue("sSupersed", supersededId);
    }

    public String getSupersededId() {
        return (String) getValue("sSupersed");
    }

    public JSONObject setRecordStatus(String recordStatus) {
        return setValue("cRecdStat", recordStatus);
    }

    public String getRecordStatus() {
        return (String) getValue("cRecdStat");
    }

    public JSONObject isRecordActive(boolean isActive) {
        return setValue("cRecdStat", isActive ? "1" : "0");
    }

    public boolean isRecordActive() {
        return ((String) getValue("cRecdStat")).equals("1");
    }

    public JSONObject setModifyingId(String modifyingId) {
        return setValue("sModified", modifyingId);
    }

    public String getModifyingId() {
        return (String) getValue("sModified");
    }

    public JSONObject setModifiedDate(Date modifiedDate) {
        return setValue("dModified", modifiedDate);
    }

    public Date getModifiedDate() {
        return (Date) getValue("dModified");
    }

    @Override
    public String getNextCode() {
        return MiscUtil.getNextCode(getTable(), ID, true, poGRider.getGConnection().getConnection(), poGRider.getBranchCode());
    }

    public Model_Industry Industry() throws SQLException, GuanzonException {
        if (poIndustry == null) {
            poIndustry = new ParamModels(poGRider).Industry();
        }

        String id = (String) (getValue("sIndstCdx") == null ? "" : getValue("sIndstCdx"));
        
        if (!"".equals(id)) {
            if (poIndustry.getEditMode() == EditMode.READY
                    && poIndustry.getIndustryId().equals(id)) {
                return poIndustry;
            } else {
                if (ReferenceCache.tryLoad("Industry", id, poIndustry)) {
                    return poIndustry;
                }

                poJSON = poIndustry.openRecord(id);

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Industry", id, poIndustry);
                    return poIndustry;
                } else {
                    poIndustry.initialize();
                    return poIndustry;
                }
            }
        } else {
            poCategory.initialize();
            return poIndustry;
        }
    }

    public Model_Category Category() throws SQLException, GuanzonException {
        if (poCategory == null) {
            poCategory = new ParamModels(poGRider).Category();
        }

        String id = (String) (getValue("sCategCd1") == null ? "" : getValue("sCategCd1"));
        
        if (!"".equals(id)) {
            if (poCategory.getEditMode() == EditMode.READY
                    && poCategory.getCategoryId().equals(id)) {
                return poCategory;
            } else {
                if (ReferenceCache.tryLoad("Category", id, poCategory)) {
                    return poCategory;
                }

                poJSON = poCategory.openRecord(id);

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Category", id, poCategory);
                    return poCategory;
                } else {
                    poCategory.initialize();
                    return poCategory;
                }
            }
        } else {
            poCategory.initialize();
            return poCategory;
        }
    }

    public Model_Category_Level2 CategoryLevel2() throws SQLException, GuanzonException {
        if (poCategoryLevel2 == null) {
            poCategoryLevel2 = new ParamModels(poGRider).Category2();
        }

        String id = (String) (getValue("sCategCd2") == null ? "" : getValue("sCategCd2"));
        
        if (!"".equals(id)) {
            if (poCategoryLevel2.getEditMode() == EditMode.READY
                    && poCategoryLevel2.getCategoryId().equals(id)) {
                return poCategoryLevel2;
            } else {
                if (ReferenceCache.tryLoad("Category_Level2", id, poCategoryLevel2)) {
                    return poCategoryLevel2;
                }

                poJSON = poCategoryLevel2.openRecord(id);

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Category_Level2", (String) getValue("sCategCd2"), poCategoryLevel2);
                    return poCategoryLevel2;
                } else {
                    poCategoryLevel2.initialize();
                    return poCategoryLevel2;
                }
            }
        } else {
            poCategoryLevel2.initialize();
            return poCategoryLevel2;
        }
    }

    public Model_Category_Level3 CategoryLevel3() throws SQLException, GuanzonException {
        if (poCategoryLevel3 == null) {
            poCategoryLevel3 = new ParamModels(poGRider).Category3();
        }

        String id = (String) (getValue("sCategCd3") == null ? "" : getValue("sCategCd3"));
        
        if (!"".equals(id)) {
            if (poCategoryLevel3.getEditMode() == EditMode.READY
                    && poCategoryLevel3.getCategoryId().equals(id)) {
                return poCategoryLevel3;
            } else {
                if (ReferenceCache.tryLoad("Category_Level3", id, poCategoryLevel3)) {
                    return poCategoryLevel3;
                }

                poJSON = poCategoryLevel3.openRecord(id);

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Category_Level3", id, poCategoryLevel3);
                    return poCategoryLevel3;
                } else {
                    poCategoryLevel3.initialize();
                    return poCategoryLevel3;
                }
            }
        } else {
            poCategoryLevel3.initialize();
            return poCategoryLevel3;
        }
    }

    public Model_Category_Level4 CategoryLevel4() throws SQLException, GuanzonException {
        if (poCategoryLevel4 == null) {
            poCategoryLevel4 = new ParamModels(poGRider).Category4();
        }

        String id = (String) (getValue("sCategCd4") == null ? "" : getValue("sCategCd4"));
        
        if (!"".equals(id)) {
            if (poCategoryLevel4.getEditMode() == EditMode.READY
                    && poCategoryLevel4.getCategoryId().equals(id)) {
                return poCategoryLevel4;
            } else {
                if (ReferenceCache.tryLoad("Category_Level4", id, poCategoryLevel4)) {
                    return poCategoryLevel4;
                }

                poJSON = poCategoryLevel4.openRecord(id);

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Category_Level4", id, poCategoryLevel4);
                    return poCategoryLevel4;
                } else {
                    poCategoryLevel4.initialize();
                    return poCategoryLevel4;
                }
            }
        } else {
            poCategoryLevel4.initialize();
            return poCategoryLevel4;
        }
    }

    public Model_Brand Brand() throws SQLException, GuanzonException {
        if (poBrand == null) {
            poBrand = new ParamModels(poGRider).Brand();
        }

        String id = (String) (getValue("sBrandIDx") == null ? "" : getValue("sBrandIDx"));
        
        if (!"".equals(id)) {
            if (poBrand.getEditMode() == EditMode.READY
                    && poBrand.getBrandId().equals(id)) {
                return poBrand;
            } else {
                if (ReferenceCache.tryLoad("Brand", id, poBrand)) {
                    return poBrand;
                }

                poJSON = poBrand.openRecord(id);

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Brand", id, poBrand);
                    return poBrand;
                } else {
                    poBrand.initialize();
                    return poBrand;
                }
            }
        } else {
            poBrand.initialize();
            return poBrand;
        }
    }

    public Model_Model Model() throws SQLException, GuanzonException {
        if (poModel == null) {
            poModel = new ParamModels(poGRider).Model();
        }
        
        String id = (String) (getValue("sModelIDx") == null ? "" : getValue("sModelIDx"));

        if (!"".equals(id)) {
            if (poModel.getEditMode() == EditMode.READY
                    && poModel.getModelId().equals(id)) {
                return poModel;
            } else {
                if (ReferenceCache.tryLoad("Model", id, poModel)) {
                    return poModel;
                }

                poJSON = poModel.openRecord(id);

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Model", id, poModel);
                    return poModel;
                } else {
                    poModel.initialize();
                    return poModel;
                }
            }
        } else {
            poModel.initialize();
            return poModel;
        }
    }

    public Model_Color Color() throws SQLException, GuanzonException {
        if (poColor == null) {
            poColor = new ParamModels(poGRider).Color();
        }
        
        String id = (String) (getValue("sColorIDx") == null ? "" : getValue("sColorIDx"));

        if (!"".equals(id)) {
            if (poColor.getEditMode() == EditMode.READY
                    && poColor.getColorId().equals(id)) {
                return poColor;
            } else {
                if (ReferenceCache.tryLoad("Color", id, poColor)) {
                    return poColor;
                }

                poJSON = poColor.openRecord(id);

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Color", id, poColor);
                    return poColor;
                } else {
                    poColor.initialize();
                    return poColor;
                }
            }
        } else {
            poColor.initialize();
            return poColor;
        }
    }

    public Model_Measure Measure() throws SQLException, GuanzonException {
        if (poMeasure == null) {
            poMeasure = new ParamModels(poGRider).Measurement();
        }

        String id = (String) (getValue("sMeasurID") == null ? "" : getValue("sMeasurID"));
        
        if (!"".equals(id)) {
            if (poMeasure.getEditMode() == EditMode.READY
                    && poMeasure.getMeasureId().equals(id)) {
                return poMeasure;
            } else {
                if (ReferenceCache.tryLoad("Measure", id, poMeasure)) {
                    return poMeasure;
                }

                poJSON = poMeasure.openRecord(id);

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Measure", id, poMeasure);
                    return poMeasure;
                } else {
                    poMeasure.initialize();
                    return poMeasure;
                }
            }
        } else {
            poMeasure.initialize();
            return poMeasure;
        }
    }

    public Model_Inv_Type InventoryType() throws SQLException, GuanzonException {
        if (poInventoryType == null) {
            poInventoryType = new ParamModels(poGRider).InventoryType();
        }
        
        String id = (String) (getValue("sInvTypCd") == null ? "" : getValue("sInvTypCd"));
        
        if (!"".equals(id)) {
            if (poInventoryType.getEditMode() == EditMode.READY
                    && poInventoryType.getInventoryTypeId().equals(id)) {
                return poInventoryType;
            } else {
                if (ReferenceCache.tryLoad("Inv_Type", id, poInventoryType)) {
                    return poInventoryType;
                }

                poJSON = poInventoryType.openRecord(id);

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Inv_Type", id, poInventoryType);
                    return poInventoryType;
                } else {
                    poInventoryType.initialize();
                    return poInventoryType;
                }
            }
        } else {
            poInventoryType.initialize();
            return poInventoryType;
        }
    }

    public Model_Model_Variant Variant() throws SQLException, GuanzonException {
        if (poVariant == null) {
            poVariant = new ParamModels(poGRider).ModelVariant();
        }

        String id = (String) (getValue("sVrntIDxx") == null ? "" : getValue("sVrntIDxx"));
        
        if (!"".equals(id)) {
            if (poVariant.getEditMode() == EditMode.READY
                    && poVariant.getVariantId().equals(id)) {
                return poVariant;
            } else {
                if (ReferenceCache.tryLoad("Model_Variant", id, poVariant)) {
                    return poVariant;
                }

                poJSON = poVariant.openRecord(id);

                if ("success".equals((String) poJSON.get("result"))) {
                    ReferenceCache.store("Model_Variant", id, poVariant);
                    return poVariant;
                } else {
                    poVariant.initialize();
                    return poVariant;
                }
            }
        } else {
            poVariant.initialize();
            return poVariant;
        }
    }

    public Model_InventorySuperseded Superseded() throws SQLException, GuanzonException {
        if (poSuperseded == null) {
            poSuperseded = new InvModels(poGRider).InventorySuperseded();
        }

        String id = (String) (getValue("sSupersed") == null ? "" : getValue("sSupersed"));
        
        if (!"".equals(id)) {
            if (poSuperseded.getEditMode() == EditMode.READY
                    && poSuperseded.getStockId().equals(id)) {
                return poSuperseded;
            } else {
                poJSON = poSuperseded.openRecord(id);

                if ("success".equals((String) poJSON.get("result"))) {
                    return poSuperseded;
                } else {
                    poSuperseded.initialize();
                    return poSuperseded;
                }
            }
        } else {
            poSuperseded.initialize();
            return poSuperseded;
        }
    }
}
