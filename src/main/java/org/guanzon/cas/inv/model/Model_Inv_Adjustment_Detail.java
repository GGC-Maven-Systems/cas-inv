package org.guanzon.cas.inv.model;

import java.sql.SQLException;
import java.util.Date;

import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.Logical;
import org.guanzon.cas.inv.services.InvModels;
import org.json.simple.JSONObject;

public class Model_Inv_Adjustment_Detail extends Model {

    private Model_Inventory poInventory;

    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);

            poEntity.updateObject("nEntryNox", 0);
            poEntity.updateDouble("nCredtQty", 0.00);
            poEntity.updateDouble("nDebitQty", 0.00);
            poEntity.updateDouble("nInvCostx", 0.00);
            poEntity.updateString("cReversex", "+");
            poEntity.updateString("cSerialze", Logical.NO);
            poEntity.updateObject("dModified", poGRider.getServerDate());

            poInventory = new InvModels(poGRider).Inventory();

            poEntity.insertRow();
            poEntity.moveToCurrentRow();
            poEntity.absolute(1);

            ID = poEntity.getMetaData().getColumnLabel(1);  // sTransNox
            ID2 = poEntity.getMetaData().getColumnLabel(2); // nEntryNox

            pnEditMode = EditMode.UNKNOWN;
        } catch (SQLException e) {
            logwrapr.severe(e.getMessage());
            System.exit(1);
        }
    }

    public JSONObject setTransactionNo(String transactionNo) {
        return setValue("sTransNox", transactionNo);
    }

    public String getTransactionNo() {
        return (String) getValue("sTransNox");
    }

    public JSONObject setEntryNo(int entryNo) {
        return setValue("nEntryNox", entryNo);
    }

    public int getEntryNo() {
        return (int) getValue("nEntryNox");
    }

    public JSONObject setStockId(String stockId) {
        return setValue("sStockIDx", stockId);
    }

    public String getStockId() {
        return (String) getValue("sStockIDx");
    }

    public JSONObject setCreditQuantity(double creditQuantity) {
        return setValue("nCredtQty", creditQuantity);
    }

    public double getCreditQuantity() {
        return Double.parseDouble(getValue("nCredtQty").toString());
    }

    public JSONObject setDebitQuantity(double debitQuantity) {
        return setValue("nDebitQty", debitQuantity);
    }

    public double getDebitQuantity() {
        return Double.parseDouble(getValue("nDebitQty").toString());
    }

    public JSONObject setExpiryDate(Date expiryDate) {
        return setValue("dExpiryDt", expiryDate);
    }

    public Date getExpiryDate() {
        return (Date) getValue("dExpiryDt");
    }

    public JSONObject setInventoryCost(double inventoryCost) {
        return setValue("nInvCostx", inventoryCost);
    }

    public double getInventoryCost() {
        return Double.parseDouble(getValue("nInvCostx").toString());
    }

    public JSONObject setRemarks(String remarks) {
        return setValue("sRemarksx", remarks);
    }

    public String getRemarks() {
        return (String) getValue("sRemarksx");
    }

    public JSONObject isReverse(boolean reverse) {
        return setValue("cReversex", reverse ? "-" : "+");
    }

    public boolean isReverse() {
        return "-".equals(getValue("cReversex"));
    }

    public JSONObject setSerialized(String serialized) {
        return setValue("cSerialze", serialized);
    }

    public String getSerialized() {
        return (String) getValue("cSerialze");
    }

    public JSONObject isSerialized(boolean serialized) {
        return setValue("cSerialze", serialized ? Logical.YES : Logical.NO);
    }

    public boolean isSerialized() {
        return Logical.YES.equals(getValue("cSerialze"));
    }

    public JSONObject setModifiedDate(Date modifiedDate) {
        return setValue("dModified", modifiedDate);
    }

    public Date getModifiedDate() {
        return (Date) getValue("dModified");
    }

    @Override
    public String getNextCode() {
        return "";
    }

    public Model_Inventory Inventory() throws SQLException, GuanzonException {
        if (poInventory == null) {
            poInventory = new InvModels(poGRider).Inventory();
        }

        String stockId = (String) (getValue("sStockIDx") == null ? "" : getValue("sStockIDx"));

        if (!"".equals(stockId)) {
            if (poInventory.getEditMode() == EditMode.READY
                    && poInventory.getStockId().equals(stockId)) {
                return poInventory;
            } else {
                poJSON = poInventory.openRecord(stockId);

                if ("success".equals(poJSON.get("result"))) {
                    return poInventory;
                } else {
                    poInventory.initialize();
                    return poInventory;
                }
            }
        } else {
            poInventory.initialize();
            return poInventory;
        }
    }
}
