package org.guanzon.cas.inv.model;

import java.sql.SQLException;
import java.util.Date;

import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.cas.inv.services.InvModels;
import org.json.simple.JSONObject;

public class Model_Inv_Adjustment_Serial extends Model {

    private Model_Inv_Serial poSerial;
    @Override
    public void initialize() {
        try {
            poEntity = MiscUtil.xml2ResultSet(System.getProperty("sys.default.path.metadata") + XML, getTable());

            poEntity.last();
            poEntity.moveToInsertRow();

            MiscUtil.initRowSet(poEntity);

            poEntity.updateObject("nEntryNox", 0);
            poEntity.updateObject("dModified", poGRider.getServerDate());
            poEntity.updateString("cReversex", "-");

            poEntity.insertRow();
            poEntity.moveToCurrentRow();
            poEntity.absolute(1);

            ID = poEntity.getMetaData().getColumnLabel(1);  // sTransNox
            ID2 = poEntity.getMetaData().getColumnLabel(2); // nEntryNox
            ID3 = poEntity.getMetaData().getColumnLabel(3); // sSerialID

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

    public JSONObject setSerialId(String serialId) {
        return setValue("sSerialID", serialId);
    }

    public String getSerialId() {
        return (String) getValue("sSerialID");
    }

    public JSONObject isReverse(boolean reverse) {
        return setValue("cReversex", reverse ? "+" : "-");
    }

    public boolean isReverse() {
        return "+".equals(getValue("cReversex"));
    }

    public JSONObject setRemarks(String remarks) {
        return setValue("sRemarksx", remarks);
    }

    public String getRemarks() {
        return (String) getValue("sRemarksx");
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

    public Model_Inv_Serial InventorySerial() throws SQLException, GuanzonException {
        if (poSerial == null) {
            poSerial = new InvModels(poGRider).InventorySerial();
        }

        String lsId = (String) (getValue("sSerialID") == null ? "" : getValue("sSerialID"));

        if (!"".equals(lsId)) {
            if (poSerial.getEditMode() == EditMode.READY
                    && poSerial.getSerialId().equals(lsId)) {
                return poSerial;
            } else {
                poJSON = poSerial.openRecord(lsId);

                if ("success".equals(poJSON.get("result"))) {
                    return poSerial;
                } else {
                    poSerial.initialize();
                    return poSerial;
                }
            }
        } else {
            poSerial.initialize();
            return poSerial;
        }
    }
}
