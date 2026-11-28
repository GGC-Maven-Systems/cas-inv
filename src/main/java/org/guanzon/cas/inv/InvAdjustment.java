package org.guanzon.cas.inv;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.agent.systables.SysTableContollers;
import org.guanzon.appdriver.agent.systables.TransactionAttachment;
import org.guanzon.appdriver.base.*;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.appdriver.token.RequestAccess;
import org.guanzon.cas.inv.model.Model_Inv_Adjustment_Detail;
import org.guanzon.cas.inv.model.Model_Inv_Adjustment_Master;
import org.guanzon.cas.inv.model.Model_Inv_Adjustment_Serial;
import org.guanzon.cas.inv.services.InvModels;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.script.ScriptException;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javax.swing.JButton;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.swing.JRViewer;
import net.sf.jasperreports.swing.JRViewerToolbar;
import net.sf.jasperreports.view.JasperViewer;
import org.guanzon.appdriver.agent.ShowMessageFX;
import org.guanzon.appdriver.constant.Logical;
import org.guanzon.cas.inv.services.InvControllers;
import org.guanzon.cas.inv.status.InvAdjustmentStatus;
import org.guanzon.cas.inv.validator.InvAdjustmentValidator;
import org.guanzon.cas.parameter.Category;
import org.guanzon.cas.parameter.model.Model_Branch;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.guanzon.cas.tbjhandler.TBJEntry;
import org.guanzon.cas.tbjhandler.TBJTransaction;
import org.json.simple.JSONArray;
import ph.com.guanzongroup.cas.cashflow.Journal;
import ph.com.guanzongroup.cas.cashflow.model.Model_Journal_Master;
import ph.com.guanzongroup.cas.cashflow.services.CashflowControllers;
import ph.com.guanzongroup.cas.cashflow.services.CashflowModels;
import ph.com.guanzongroup.cas.cashflow.status.JournalStatus;

public class InvAdjustment extends Transaction {
    public String psCompanyId = "";
    public String psIndustryId = "";
    public String psCategoryId = "";
    public String psCategory = "";
    public String psApprover = "";

    List<TransactionAttachment> paAttachments;
    List<Model_Inv_Adjustment_Serial> paSerial;
    public List<InvAdjustment> paMaster;

    public JSONObject InitTransaction() throws SQLException, GuanzonException {
        SOURCE_CODE = "IAdj";

        poMaster = new InvModels(poGRider).InventoryAdjustmentMaster();
        poDetail = new InvModels(poGRider).InventoryAdjustmentDetail();

        paMaster = new ArrayList<>();
        paSerial = new ArrayList<>();
        paAttachments = new ArrayList<>();
        psCategory = "";
        psApprover = "";
        setApproving("");
        return initialize();
    }

    @Override
    public String getSourceCode() { return SOURCE_CODE; }

    //Helpers
    public void setCompanyId(String companyId) { psCompanyId = companyId; }
    public void setIndustryId(String industryId) { psIndustryId = industryId; }
    public void setCategoryId(String categoryId) { psCategoryId = categoryId; }
    public void setCategory(String category) { psCategory = category; }
    public String getCategory() { return psCategory;}

    private JSONObject setJSON(String fsResult, String fsMessage) {
        JSONObject loJSON = new JSONObject();
        loJSON.put("result", fsResult);
        loJSON.put("message", fsMessage);
        return loJSON;
    }

    public boolean isJSONSuccess(JSONObject foJSON) {
        return ("success".equals((String) foJSON.get("result")) || !"error".equals((String) foJSON.get("result")));
    }

    private boolean checkEmpty(String fsValue){
        return fsValue == null || "".equals(fsValue);
    }
    private boolean checkNotEmpty(String fsValue){
        return fsValue != null && !"".equals(fsValue);
    }
    
    private String getSafe(Object foValue){
        if(foValue == null){ return ""; }
        return String.valueOf(foValue);
    }

    private static String xsDateShort(Date fdValue) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(fdValue);
        return date;
    }

    public void ResetTransaction() throws SQLException, GuanzonException {
        paDetail = new ArrayList<>();
        paMaster = new ArrayList<>();
        paSerial = new ArrayList<>();
        paAttachments = new ArrayList<>();
        psCategory = "";
        psApprover = "";
        setApproving("");
    }

    public JSONObject callApproval(){
        poJSON = new JSONObject();
        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
            poJSON = ShowDialogFX.getUserApproval(poGRider);
            if (!isJSONSuccess(poJSON)) {
                return poJSON;
            }
            String lsUserIDxx = poJSON.get("sUserIDxx").toString();
            if (Integer.parseInt(poJSON.get("nUserLevl").toString()) <= UserRight.ENCODER) {
                poJSON = setJSON("error", "User is not an authorized approving officer.");
                return poJSON;
            }
            setApproving(lsUserIDxx);
            psApprover = lsUserIDxx;
        }

        poJSON = setJSON("success","success");
        return poJSON;
    }

    public String getStatus(String lsStatus) {
        switch (lsStatus) {
            case InvAdjustmentStatus.VOID:
                return "Voided";
            case InvAdjustmentStatus.CANCELLED:
                return "Cancelled";
            case InvAdjustmentStatus.CONFIRMED:
                return "Confirmed";
            case InvAdjustmentStatus.OPEN:
                return "Open";
            default:
                return "Unknown";
        }
    }
    
    @Override
    public Model_Inv_Adjustment_Master Master() {
        return (Model_Inv_Adjustment_Master) poMaster;
    }

    @Override
    public Model_Inv_Adjustment_Detail Detail(int row) {
        return (Model_Inv_Adjustment_Detail) paDetail.get(row);
    }
    public JSONObject AddDetail() throws CloneNotSupportedException {
        if (getDetailCount() > 0) {
            if (checkEmpty(Detail(getDetailCount() - 1).getStockId())){
                poJSON = new JSONObject();
                poJSON = setJSON("error", "Last row has empty item.");
                return poJSON;
            }
        }

        return addDetail();
    }

    public void ReloadDetail() throws CloneNotSupportedException, SQLException, GuanzonException{
        int lnCtr = getDetailCount() - 1;
        while (lnCtr >= 0) {
            if (checkEmpty(Detail(lnCtr).getStockId())) {
                removeSerialsOfEntry(lnCtr+1);
                paDetail.remove(lnCtr);
            }
            lnCtr--;
        }

        if ((getDetailCount() - 1) >= 0) {
            if (
                checkNotEmpty(Detail(getDetailCount() - 1).getStockId())
                ) {
                AddDetail();
            }
        }

        if ((getDetailCount() - 1) < 0) {
            AddDetail();
        }
    }

    public List<Model_Inv_Adjustment_Serial> SerialList() {
        return paSerial;
    }

    public Model_Inv_Adjustment_Serial Serial(int row) {
        return (Model_Inv_Adjustment_Serial) paSerial.get(row);
    }

    public int getSerialListCount() {
        if(paSerial == null){
            return 0;
        }
        return paSerial.size();
    }

    public JSONObject AddSerial() throws CloneNotSupportedException {
        if (getSerialListCount() > 0) {
            if (checkEmpty(Serial(getSerialListCount() - 1).getSerialId())){
                poJSON = new JSONObject();
                poJSON = setJSON("error", "Last row has empty item.");
                return poJSON;
            }
        }

        Model_Inv_Adjustment_Serial loSerial = new InvModels(poGRider).InventoryAdjustmentSerial();
        loSerial.newRecord();
        loSerial.setTransactionNo(Master().getTransactionNo());
        paSerial.add(loSerial);

        poJSON = setJSON("success", "success");
        return poJSON;
    }

    public void ReloadSerial() throws CloneNotSupportedException, SQLException, GuanzonException{
        int lnCtr = getSerialListCount() - 1;
        while (lnCtr >= 0) {
            if (checkEmpty(Serial(lnCtr).getSerialId())) {
                paSerial.remove(lnCtr);
            }
            lnCtr--;
        }

        if ((getSerialListCount() - 1) >= 0) {
            if (checkNotEmpty(Serial(getSerialListCount() - 1).getSerialId())) {
                AddSerial();
            }
        }

        if ((getSerialListCount() - 1) < 0) {
            AddSerial();
        }
    }

    public JSONObject addSerial(int fnRow, String fsSerial) {
        JSONObject loJSON = new JSONObject();
        int lnEntryNo = Detail(fnRow).getEntryNo();
        int lnCreditQty = (int) Math.round(Detail(fnRow).getCreditQuantity());
        int lnDebitQty = (int) Math.round(Detail(fnRow).getDebitQuantity());

        if(lnCreditQty > 0) {
            if (getSerialCount(lnEntryNo) >= lnCreditQty) {
                loJSON.put("result", "error");
                loJSON.put("message", "Serial count cannot exceed the quantity of entry no. " + lnEntryNo + ".");
                return loJSON;
            }
        } else {
            if (getSerialCount(lnEntryNo) >= lnDebitQty) {
                loJSON.put("result", "error");
                loJSON.put("message", "Serial count cannot exceed the quantity of entry no. " + lnEntryNo + ".");
                return loJSON;
            }
        }

        for (Model_Inv_Adjustment_Serial loSerial : paSerial) {
            if (loSerial.getSerialId().equalsIgnoreCase(fsSerial)) {
                loJSON.put("result", "error");
                loJSON.put("message", "Serial " + fsSerial + " is already added to entry no. " + loSerial.getEntryNo() + ".");
                return loJSON;
            }
        }

        Model_Inv_Adjustment_Serial loSerial = new InvModels(poGRider).InventoryAdjustmentSerial();
        loSerial.newRecord();
        loSerial.setTransactionNo(Master().getTransactionNo());
        loSerial.setEntryNo(lnEntryNo);
        loSerial.setSerialId(fsSerial);
        paSerial.add(loSerial);

        loJSON.put("result", "success");
        return loJSON;
    }

    public void removeSerialsOfEntry(int fnEntryNo) {
        paSerial.removeIf(s -> s.getEntryNo() == fnEntryNo && s.getEditMode() == EditMode.ADDNEW);
        for (Model_Inv_Adjustment_Serial loSerial : paSerial) {
            if(loSerial.getEditMode() == EditMode.ADDNEW){
                if (loSerial.getEntryNo() > fnEntryNo) {
                    loSerial.setEntryNo(loSerial.getEntryNo() - 1);
                }
            } else {
                if (loSerial.getEntryNo() == fnEntryNo) {
                    loSerial.isReverse(true);
                }
            }
        }
    }

    public List<Model_Inv_Adjustment_Serial> getSerials(int fnEntryNo, boolean isAll) {
        List<Model_Inv_Adjustment_Serial> loList = new ArrayList<>();
        for (Model_Inv_Adjustment_Serial loSerial : paSerial) {
            if(checkNotEmpty(loSerial.getSerialId())){
                if (loSerial.getEntryNo() == fnEntryNo) {
                    if(isAll){
                        loList.add(loSerial);
                    } else {
                        if(loSerial.isReverse()){
                            loList.add(loSerial);
                        }
                    }
                }
            }
        }
        return loList;
    }

    public int getSerialCount(int fnEntryNo) {
        return getSerials(fnEntryNo, true).size();
    }

    public int getActiveSerialCount(int fnEntryNo) {
        return getSerials(fnEntryNo, false).size();
    }

    public JSONObject NewTransaction()
            throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();

        ResetTransaction();
        poJSON = newTransaction();
        if(!isJSONSuccess(poJSON)){
            return poJSON;
        }

        poJSON.put("result", "success");
        return poJSON;
    }


    public JSONObject OpenTransaction(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException, ScriptException {
        poJSON = new JSONObject();

        poJSON = openTransaction(transactionNo);
        if(!isJSONSuccess(poJSON)){
            return poJSON;
        }

        //load inv adjustment serial
        poJSON = loadInvAdjustmentSerial();
        if(!isJSONSuccess(poJSON)){
            return poJSON;
        }

        if(!pbHistory){
            //load attachment
            poJSON = loadAttachments();
            if (!isJSONSuccess(poJSON)) {
                return poJSON;
            }
        }

        poJSON = setJSON("success", "success");
        return poJSON;
    }

    private JSONObject loadInvAdjustmentSerial() throws SQLException, GuanzonException, CloneNotSupportedException{
        poJSON = new JSONObject();
        paSerial = new ArrayList<>();

        String sql = "SELECT nEntryNox, sSerialID FROM Inv_Adjustment_Serial "
                + " WHERE sTransNox = " + SQLUtil.toSQL(Master().getTransactionNo())
                + " ORDER BY nEntryNox, sSerialID ASC";
        ResultSet rs = this.poGRider.executeQuery(sql);
        while(rs.next()) {
            Model_Inv_Adjustment_Serial loDetail = new InvModels(poGRider).InventoryAdjustmentSerial();
            loDetail.initialize();
            poJSON = loDetail.openRecord( Master().getTransactionNo(), rs.getString("nEntryNox"), rs.getString("sSerialID"));
            if (!"success".equals((String)this.poJSON.get("result"))) {
                poJSON.put("message", "Unable to open inventory adjustment serial record.");
                return poJSON;
            }

            paSerial.add(loDetail);
        }

        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }


    public JSONObject UpdateTransaction() throws SQLException, GuanzonException, CloneNotSupportedException, ScriptException {
        poJSON = new JSONObject();

        poJSON = updateTransaction();
        if(!isJSONSuccess(poJSON)){
            return poJSON;
        }

        for(int lnCtr = 0;lnCtr < getSerialListCount();lnCtr++){
            if(Serial(lnCtr).getEditMode() == EditMode.READY){
                poJSON = Serial(lnCtr).updateRecord();
                if(!isJSONSuccess(poJSON)){
                    return poJSON;
                }
            }
        }

        poJSON = loadAttachments();
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    public JSONObject SaveTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        return saveTransaction();
    }

    public JSONObject ConfirmTransaction() throws ParseException, SQLException, GuanzonException, CloneNotSupportedException, ScriptException {
        poJSON = new JSONObject();
        String lsStatus = InvAdjustmentStatus.CONFIRMED;

        if (getEditMode() != EditMode.READY) {
            poJSON = setJSON("error", "No transacton was loaded.");
            return poJSON;
        }

        Model_Inv_Adjustment_Master loObject = new InvModels(poGRider).InventoryAdjustmentMaster();
        poJSON = loObject.openRecord(Master().getTransactionNo());
        if (!isJSONSuccess(poJSON)) {
            poJSON = setJSON((String) poJSON.get("result"), "Unable to load transaction. " + (String) poJSON.get("message"));
            return poJSON;
        }

        if (lsStatus.equals(loObject.getTransactionStatus())) {
            poJSON = setJSON("error", "Transaction was already confirmed.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(lsStatus);
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }
        
        //Validate credit qty vs on hand qty
        for (int lnRow = 0; lnRow < getDetailCount(); lnRow++) {
            if(Detail(lnRow).isReverse()){
                poJSON = validateCredit(Detail(lnRow).getCreditQuantity(), lnRow);
                if(!isJSONSuccess(poJSON)){
                    return poJSON;
                }
            }
        }

        if(!pbWthParent){
            psApprover = poGRider.getUserID();
            poJSON = callApproval();
            if (!isJSONSuccess(poJSON)) {
                return poJSON;
            }
        }
        
        //change status
        poGRider.beginTrans("UPDATE STATUS", "ConfirmTransaction", SOURCE_CODE, Master().getTransactionNo());

        try {
            //Update Inventory On Hand
            InventoryTransaction loTrans = new InventoryTransaction(poGRider);
            loTrans.InventoryAdjustment((String)poMaster.getValue("sTransNox"), (Date) poMaster.getValue("dTransact"), false);
            String lsCondition  = "0";
            for (Model loDetail : paDetail) {
                Model_Inv_Adjustment_Detail detail = (Model_Inv_Adjustment_Detail) loDetail;
                if(detail.isReverse()){
                    lsCondition  = getInvMasterCondition(detail); //get Condition from Inv_Master

                    if(detail.isSerialized()){
                        String lsStockIDx = detail.getStockId();
                        List<Model_Inv_Adjustment_Serial> laSerial = paSerial.stream()
                            .filter(p -> {
                                try {
                                    if (p == null || p.InventorySerial() == null) {
                                        return false;
                                    }
                                    String lsSerialStock = p.InventorySerial().getStockId();
                                    return lsSerialStock != null && lsSerialStock.equalsIgnoreCase(lsStockIDx);
                                } catch (SQLException | GuanzonException e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .collect(Collectors.toList());
                        for(Model_Inv_Adjustment_Serial loSerial : laSerial) {
                            if(loSerial.isReverse()){ 
                                //get the industry code of the category to prevent conflict in inv master / inventory table industry for general category as per ma'am she 11-28-2026 
                                loTrans.addSerial(Master().Category().getIndustryCode(), loSerial.getSerialId(), false, detail.getInventoryCost(), loSerial.InventorySerial().getWarehouseId());        //(String)poMaster.getValue("sIndstCdx")
                            }
                        }
                    } else {
                        if(detail.getCreditQuantity() > 0.00){
                            //get the industry code of the category to prevent conflict in inv master / inventory table industry for general category as per ma'am she 11-28-2026 
                            loTrans.addDetail(Master().Category().getIndustryCode(), detail.getStockId(), lsCondition, detail.getCreditQuantity(), 0, detail.getInventoryCost());                //(String)poMaster.getValue("sIndstCdx")
                        } else {
                            //get the industry code of the category to prevent conflict in inv master / inventory table industry for general category as per ma'am she 11-28-2026 
                            loTrans.addDetail(Master().Category().getIndustryCode(), detail.getStockId(), lsCondition, detail.getDebitQuantity(), 0, detail.getInventoryCost());                //(String)poMaster.getValue("sIndstCdx")
                        }
                    }
                }
            }
            loTrans.saveTransaction();

            //Generate Journal
            poJSON = generateJournal(lsStatus);
            if (!isJSONSuccess(poJSON)) {
                poGRider.rollbackTrans();
                return poJSON;
            }

            //change status
            poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), "", lsStatus, false, true);
            if (!isJSONSuccess(poJSON)) {
                poGRider.rollbackTrans();
                return poJSON;
            }

            poGRider.commitTrans();

        } catch (GuanzonException | SQLException | CloneNotSupportedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poGRider.rollbackTrans();
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON = setJSON("success", "Transaction confirmed successfully.");
        return poJSON;
    }
    
    public String getInvMasterCondition(Model_Inv_Adjustment_Detail foObject){
        poJSON = new JSONObject();
        String lsReturnVal = "0"; //0 by default
        try {
            
            String lsSQL = MiscUtil.addCondition(InvMaster_SQL(), //Search via Inv Master
                                                " b.sCategCd1 = " + SQLUtil.toSQL(Master().getCategoryCode())
                                                + " AND a.sBranchCd = " + SQLUtil.toSQL(Master().getBranchCode())
                                                + " AND a.sStockIDx = " + SQLUtil.toSQL(foObject.getStockId())
                                                );

            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
                if (MiscUtil.RecordCount(loRS) > 0) {
                    if(loRS.next()){
                        lsReturnVal = loRS.getString("cConditnx");
                    }
                }
            MiscUtil.close(loRS);
        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            return lsReturnVal; 
        }
        
        return lsReturnVal;
    }

    public JSONObject CancelTransaction() throws ParseException, SQLException, GuanzonException, CloneNotSupportedException, ScriptException {
        poJSON = new JSONObject();
        String lsStatus = InvAdjustmentStatus.CANCELLED;

        if (getEditMode() != EditMode.READY) {
            poJSON = setJSON("error", "No transacton was loaded.");
            return poJSON;
        }

        Model_Inv_Adjustment_Master loObject = new InvModels(poGRider).InventoryAdjustmentMaster();
        poJSON = loObject.openRecord(Master().getTransactionNo());
        if (!isJSONSuccess(poJSON)) {
            poJSON = setJSON((String) poJSON.get("result"), "Unable to load transaction. " + (String) poJSON.get("message"));
            return poJSON;
        }

        if (lsStatus.equals(loObject.getTransactionStatus())) {
            poJSON = setJSON("error", "Transaction was already cancelled.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(lsStatus);
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }

        if(!pbWthParent){
            psApprover = poGRider.getUserID();
            poJSON = callApproval();
            if (!isJSONSuccess(poJSON)) {
                return poJSON;
            }
        }

        //change status
        poGRider.beginTrans("UPDATE STATUS", "CancelTransaction", SOURCE_CODE, Master().getTransactionNo());

        try {
            //Update Inventory On Hand
            //Update the inventory for this Received Purchase
            InventoryTransaction loTrans = new InventoryTransaction(poGRider);
            loTrans.InventoryAdjustment((String)poMaster.getValue("sTransNox"), (Date) poMaster.getValue("dTransact"), true);
            String lsCondition  = "0";
            for (Model loDetail : paDetail) {
                Model_Inv_Adjustment_Detail detail = (Model_Inv_Adjustment_Detail) loDetail;
                if(detail.isReverse()){
                    lsCondition  = getInvMasterCondition(detail); //get Condition from Inv_Master

                    if(detail.isSerialized()){
                        String lsStockIDx = detail.getStockId();
                        List<Model_Inv_Adjustment_Serial> laSerial = paSerial.stream()
                            .filter(p -> {
                                try {
                                    if (p == null || p.InventorySerial() == null) {
                                        return false;
                                    }
                                    String lsSerialStock = p.InventorySerial().getStockId();
                                    return lsSerialStock != null && lsSerialStock.equalsIgnoreCase(lsStockIDx);
                                } catch (SQLException | GuanzonException e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .collect(Collectors.toList());
                        for(Model_Inv_Adjustment_Serial loSerial : laSerial) {
                            if(loSerial.isReverse()){ 
                                //get the industry code of the category to prevent conflict in inv master / inventory table industry for general category as per ma'am she 11-28-2026 
                                loTrans.addSerial(Master().Category().getIndustryCode(), loSerial.getSerialId(), false, detail.getInventoryCost(), loSerial.InventorySerial().getWarehouseId()); //(String)poMaster.getValue("sIndstCdx")
                            }
                        }
                    } else {
                        if(detail.getCreditQuantity() > 0.00){
                            //get the industry code of the category to prevent conflict in inv master / inventory table industry for general category as per ma'am she 11-28-2026 
                            loTrans.addDetail(Master().Category().getIndustryCode(), detail.getStockId(), lsCondition, detail.getCreditQuantity(), 0, detail.getInventoryCost()); //(String)poMaster.getValue("sIndstCdx")
                        } else {
                            //get the industry code of the category to prevent conflict in inv master / inventory table industry for general category as per ma'am she 11-28-2026 
                            loTrans.addDetail(Master().Category().getIndustryCode(), detail.getStockId(), lsCondition, detail.getDebitQuantity(), 0, detail.getInventoryCost()); //(String)poMaster.getValue("sIndstCdx")
                        }
                    }
                }
            }
            loTrans.saveTransaction();

            //Generate Journal
            poJSON = generateJournal(lsStatus);
            if (!isJSONSuccess(poJSON)) {
                poGRider.rollbackTrans();
                return poJSON;
            }

            //change status
            poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), "", lsStatus, false, true);
            if (!isJSONSuccess(poJSON)) {
                poGRider.rollbackTrans();
                return poJSON;
            }

            poGRider.commitTrans();

        } catch (GuanzonException | SQLException | CloneNotSupportedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poGRider.rollbackTrans();
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON = setJSON("success", "Transaction cancelled successfully.");
        return poJSON;
    }

    public JSONObject VoidTransaction() throws ParseException, SQLException, GuanzonException, CloneNotSupportedException, ScriptException {
        poJSON = new JSONObject();
        String lsStatus = InvAdjustmentStatus.VOID;

        if (getEditMode() != EditMode.READY) {
            poJSON = setJSON("error", "No transacton was loaded.");
            return poJSON;
        }

        Model_Inv_Adjustment_Master loObject = new InvModels(poGRider).InventoryAdjustmentMaster();
        poJSON = loObject.openRecord(Master().getTransactionNo());
        if (!isJSONSuccess(poJSON)) {
            poJSON = setJSON((String) poJSON.get("result"), "Unable to load transaction. " + (String) poJSON.get("message"));
            return poJSON;
        }

        if (lsStatus.equals(loObject.getTransactionStatus())) {
            poJSON = setJSON("error", "Transaction was already voided.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(lsStatus);
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }

        if(!pbWthParent){
            psApprover = poGRider.getUserID();
            poJSON = callApproval();
            if (!isJSONSuccess(poJSON)) {
                return poJSON;
            }
        }

        //change status
        poGRider.beginTrans("UPDATE STATUS", "VoidTransaction", SOURCE_CODE, Master().getTransactionNo());

        //change status
        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), "", lsStatus, false, true);
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON = setJSON("success", "Transaction voided successfully.");
        return poJSON;
    }

    public InvAdjustment TransactionList(int row) {
        return (InvAdjustment) paMaster.get(row);
    }

    public int getTransactionListCount() {
        if(paMaster == null){
            return 0;
        }
        return paMaster.size();
    }
    
    boolean pbHistory = false;
    public void setIsHistory(boolean fIsHistory){
        pbHistory = fIsHistory;
    }
    
    public JSONObject loadTransactionList(String fsTranRefNo, String fsCategory, Date fDateFrom, Date fDateTo, String fsStatus) throws SQLException, GuanzonException, ScriptException, CloneNotSupportedException {
        poJSON = new JSONObject();
        paMaster = new ArrayList<>();
        if(fDateFrom == null){
            return setJSON("error", "Invalid from date.");
        }

        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE,
                " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                        + " AND a.sCompnyID = " + SQLUtil.toSQL(psCompanyId)
                        + " AND b.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode())
                        + " AND a.dTransact >= " + SQLUtil.toSQL(xsDateShort(fDateFrom))
                        + " AND ( a.sTransNox LIKE " + SQLUtil.toSQL("%" + fsTranRefNo + "%")
                        + " OR a.sReferNox LIKE " + SQLUtil.toSQL("%" + fsTranRefNo + "%")
                        + " ) "
        );
        
        if(fDateTo != null){
            lsSQL = lsSQL + " AND a.dTransact <= " + SQLUtil.toSQL(xsDateShort(fDateTo));
        }
        
        if(checkNotEmpty(fsCategory)){
            lsSQL = lsSQL + " AND c.sDescript LIKE " + SQLUtil.toSQL(fsCategory);
        }
        
        if(checkNotEmpty(fsStatus)){
            lsSQL = lsSQL + " AND a.cTranStat = " + SQLUtil.toSQL(fsStatus);
        }

        lsSQL = lsSQL + " ORDER BY a.dTransact DESC, a.sTransNox DESC ";
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        if (MiscUtil.RecordCount(loRS) <= 0) {
            poJSON = setJSON("error", "No record found.");
            return poJSON;
        }

        while (loRS.next()) {
            InvAdjustment loObject = new InvControllers(poGRider,null).InventoryAdjustment();
            loObject.InitTransaction();
            loObject.setIsHistory(true);
            poJSON = loObject.OpenTransaction(loRS.getString("sTransNox"));
            if (isJSONSuccess(poJSON)) {
                paMaster.add((InvAdjustment) loObject);
            } else {
                return poJSON;
            }
        }
        MiscUtil.close(loRS);

        poJSON.put("result", "success");
        poJSON.put("message", "success");
        return poJSON;
    }
    
    public int getTransactionListTotalAdjustments(String fsStatus){
        int lnTotal = 0;
        for(int lnRow = 0;lnRow < getTransactionListCount();lnRow++){
            if(checkEmpty(fsStatus)){
                lnTotal++;
            } else {
                if(fsStatus.equals(TransactionList(lnRow).Master().getTransactionStatus())){
                    lnTotal++;
                }
            }
        }
        
        return lnTotal;
    }
    
    public Double getTransactionListTotalCreditQty(){
        Double lnTotal = 0.00;
        for(int lnRow = 0;lnRow < getTransactionListCount();lnRow++){
            lnTotal += TransactionList(lnRow).getTotalCreditQty();
        }
        
        return lnTotal;
    }
    
    public Double getTransactionListTotalDebitQty(){
        Double lnTotal = 0.00;
        for(int lnRow = 0;lnRow < getTransactionListCount();lnRow++){
            lnTotal += TransactionList(lnRow).getTotalDebitQty();
        }
        
        return lnTotal;
    }
    
    public Double getTotalCreditQty(){
        Double lnTotal = 0.00;
        for(int lnCtr = 0;lnCtr < getDetailCount();lnCtr++){
            if(Detail(lnCtr).isReverse()){
                lnTotal += Detail(lnCtr).getCreditQuantity();
            }
        }
        
        return lnTotal;
    }
    
    public Double getTotalDebitQty(){
        Double lnTotal = 0.00;
        for(int lnCtr = 0;lnCtr < getDetailCount();lnCtr++){
            if(Detail(lnCtr).isReverse()){
                lnTotal += Detail(lnCtr).getDebitQuantity();
            }
        }
        
        return lnTotal;
    }
    
    public Double getTotalCreditCost(){
        Double lnTotal = 0.00;
        for(int lnCtr = 0;lnCtr < getDetailCount();lnCtr++){
            if(Detail(lnCtr).isReverse()){
                lnTotal += (Detail(lnCtr).getCreditQuantity() * Detail(lnCtr).getInventoryCost());
            }
        }
        
        return lnTotal;
    }
    
    public Double getTotalDebitCost(){
        Double lnTotal = 0.00;
        for(int lnCtr = 0;lnCtr < getDetailCount();lnCtr++){
            if(Detail(lnCtr).isReverse()){
                lnTotal += (Detail(lnCtr).getDebitQuantity() * Detail(lnCtr).getInventoryCost());
            }
        }
        
        return lnTotal;
    }

    public JSONObject SearchTransaction(String fsValue, boolean fbByCode) throws CloneNotSupportedException, SQLException, GuanzonException, ScriptException{
        poJSON = new JSONObject();

        initSQL();
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE,
                            " a.sIndstCdx = " + SQLUtil.toSQL(psIndustryId)
                        + " AND a.sCompnyID = " + SQLUtil.toSQL(psCompanyId)
                        + " AND b.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode())
                    );

        System.out.println("Executing SQL: " + lsSQL);
        if(pbWithUI){
            poJSON = ShowDialogFX.Browse(poGRider,
                    lsSQL,
                    "",
                    "Transaction No»Transaction Date»Branch»Reference No",
                    "sTransNox»dTransact»sBranchNm»sReferNox", 
                    "a.sTransNox»a.dTransact»b.sBranchNm»a.sReferNox" ,
                    fbByCode ? 0 : 2);
        } else {
            poJSON.put("sTransNox", fsValue);
        }
        if (poJSON != null) {
            return OpenTransaction((String) poJSON.get("sTransNox"));
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
    }
    
    public JSONObject SearchCategory(String value, boolean byCode, boolean fbIsSearch) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        Category object = new ParamControllers(poGRider, logwrapr).Category();
        object.setRecordStatus(RecordStatus.ACTIVE);
        String lsSQL =  " SELECT a.sCategrCd " +
                        " ,a.sIndstCdx " +
                        " ,a.cRecdStat " +
                        " ,a.sDescript AS sCategory " +
                        " ,b.sDescript AS sIndustry " +
                        " FROM Category a " +
                        " LEFT JOIN Industry b ON b.sIndstCdx = a.sIndstCdx " +
                        " WHERE a.sIndstCdx = b.sIndstCdx ";
        lsSQL = MiscUtil.addCondition(lsSQL,
                             " a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                            + " AND ( a.sIndstCdx = " + SQLUtil.toSQL("09") //Mandatory get the general industry
                            + " OR a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryCode())
                            + " ) "
                    );
        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                value,
                "Category ID»Description",
                "sCategrCd»sCategory",
                "a.sCategrCd»a.sDescript",
                byCode ? 0 : 1);

        if (poJSON != null) {
            poJSON = object.getModel().openRecord((String) poJSON.get("sCategrCd"));
            if ("success".equals((String) poJSON.get("result"))) {
                if(fbIsSearch) {
                    setCategory(object.getModel().getDescription());
                } else {
                    Master().setCategoryCode(object.getModel().getCategoryId());
                }
            }
            
            System.out.println("Category : " + Master().Category().getDescription());
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
        }
        
        return poJSON;
    }
    
    public JSONObject SearchInventory(String value, boolean byCode, int row) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        poJSON.put("row", row);
        
        if(checkEmpty(Master().getCategoryCode())){
            poJSON = setJSON("error", "Category cannot be empty.");
            poJSON.put("row", row);
            return poJSON;
        }
        
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);
        String lsSQL = MiscUtil.addCondition(InvMaster_SQL(), //Search via Inv Master
                                            " b.sCategCd1 = " + SQLUtil.toSQL(Master().getCategoryCode())
                                            + " AND a.sBranchCd = " + SQLUtil.toSQL(Master().getBranchCode())
                                            );
        
        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                value,
                "Barcode»Description»Brand»Model»Variant»Color",
                "sBarCodex»sDescript»sBrandNme»sModelNme»sVrntName»sColorNme",
                "b.sBarCodex»b.sDescript»IFNULL(c.sDescript, '')»IFNULL(d.sDescript, '')»IFNULL(f.sDescript, '')»IFNULL(e.sDescript, '')",
                byCode ? 0 : 1);

        if (poJSON != null) {
            poJSON = object.getModel().openRecord((String) poJSON.get("sStockIDx"));
            if ("success".equals((String) poJSON.get("result"))) {
                JSONObject loJSON = checkExistingDetail(row,
                        object.getModel().getStockId()
                        );
                if ("error".equals((String) loJSON.get("result"))) {
                    if((boolean) loJSON.get("reverse")){
                        return loJSON;
                    } else {
                        row = (int) loJSON.get("row");
                        Detail(row).isReverse(true);
                    }
                }
                
                Detail(row).setStockId(object.getModel().getStockId());
                Detail(row).isSerialized(object.getModel().isSerialized());
                Detail(row).setInventoryCost(object.getModel().getCost().doubleValue());
            }
            
            System.out.println("Barcode : " + Detail(row).Inventory().getBarCode());
            System.out.println("Description : " + Detail(row).Inventory().getDescription());
            
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
        }
        
        poJSON.put("row", row);
        return poJSON;
    }
    public JSONObject validateDebit(Double fdblDebitQty, int fnRow){
        poJSON = new JSONObject();
        
        if(Detail(fnRow).isSerialized()){
            if (fdblDebitQty != null && fdblDebitQty != Math.floor(fdblDebitQty)) {
                poJSON.put("result", "error");
                poJSON.put("message", "Debit quantity must be a whole number for serialize item.");
                return poJSON;
            }
        }
        
        poJSON = setJSON("success", "success");
        poJSON.put("row", fnRow);
        return poJSON;
    }
    
    public JSONObject validateCredit(Double fdblCreditQty, int fnRow){
        poJSON = new JSONObject();
        
        if(Detail(fnRow).isSerialized()){
            if (fdblCreditQty != null && fdblCreditQty != Math.floor(fdblCreditQty)) {
                poJSON.put("result", "error");
                poJSON.put("message", "Credit quantity must be a whole number for serialize item.");
                return poJSON;
            }
        }
        
        try {
            Double ldblQtyOnHand = 0.00;
            if(checkEmpty(Detail(fnRow).getStockId())){
                poJSON = setJSON("error", "Stock Id cannot be empty.");
                poJSON.put("row", fnRow);
                return poJSON;
            }

            String lsSQL = MiscUtil.addCondition(InvMaster_SQL(), //Search via Inv Master
                                                " b.sCategCd1 = " + SQLUtil.toSQL(Master().getCategoryCode())
                                                + " AND a.sBranchCd = " + SQLUtil.toSQL(Master().getBranchCode())
                                                + " AND a.sStockIDx = " + SQLUtil.toSQL(Detail(fnRow).getStockId())
                                                );

            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
                if (MiscUtil.RecordCount(loRS) > 0) {
                    if(loRS.next()){
                        ldblQtyOnHand = loRS.getDouble("nQtyOnHnd");
                    }
                }
            MiscUtil.close(loRS);
            if(fdblCreditQty > ldblQtyOnHand){
                poJSON = setJSON("error", "Credit cannot be greater than the quantity on hand.");
                poJSON.put("row", fnRow);
                return poJSON;
            }
        
        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON = setJSON("error", MiscUtil.getException(ex));
            poJSON.put("row", fnRow);
            return poJSON; 
        }
        poJSON = setJSON("success", "success");
        poJSON.put("row", fnRow);
        return poJSON;
    }
    
    public JSONObject SearchSerial(String fsValue, boolean fbIsSerail1, int fnEntryNo, int fnRow) throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        poJSON.put("row", fnRow);
        int lnDetailRow = fnEntryNo - 1;
        
        Double ldblQty = Detail(lnDetailRow).getCreditQuantity();
        if(Detail(lnDetailRow).getDebitQuantity() > 0.00){
            ldblQty = Detail(lnDetailRow).getDebitQuantity() ;
        } 
        
        if(getActiveSerialCount(fnEntryNo) >= ldblQty){
            poJSON = setJSON("error", "Serial list cannot be greater than the setted detail quantity.");
            poJSON.put("row", fnRow);
            return poJSON;
        }
        
        String lsSQL = MiscUtil.addCondition(InvSerial_SQL(), " a.sStockIDx = " + SQLUtil.toSQL(Detail(lnDetailRow).getStockId()));
        
        if(Detail(lnDetailRow).getCreditQuantity() > 0.00){ //only serials currently assigned to a branch can be selected
            //Serial not assigned in the current branch
            lsSQL = lsSQL + " AND a.sBranchCd = " + SQLUtil.toSQL(Master().getBranchCode())
                        + " AND a.cLocation = " + SQLUtil.toSQL(Logical.YES); //current location in the branch
        } else { //only serials NOT currently assigned to the logged-in branch can be selected.
            //Serial assigned in the current branch
            lsSQL = lsSQL + " AND a.sBranchCd != " + SQLUtil.toSQL(Master().getBranchCode())
//                        + " AND a.cLocation != " + SQLUtil.toSQL("3") //not yet sold to customer
                        ; 
        }
        
        String lsHeader = "Serial 1»Serial 2»Description»Brand»Model»Variant»Color"; //»Plate No»CS No
        String lsColName = "sSerial01»sSerial02»sDescript»sBrandNme»sModelNme»sVarntNme»sColorNme"; //»sPlateNoP»sCStckrNo
        String lsColCrit = "a.sSerial01»a.sSerial02»a.sDescript»IFNULL(d.sBrandNme, '')»IFNULL(e.sDescript, '')»IFNULL(f.sDescript, '')»IFNULL(g.sDescript, '')"; //»b.sPlateNoP»b.sCStckrNo
        
        System.out.println("Executing SQL: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                fsValue,
                lsHeader,
                lsColName,
                lsColCrit,
                fbIsSerail1 ? 0 : 1);

        if (poJSON != null) {
            String lsSerialId = (String) poJSON.get("sSerialID");
            if(checkNotEmpty(lsSerialId)){
                JSONObject loJSON = checkExistingSerial(fnRow,lsSerialId);
                if ("error".equals((String) loJSON.get("result"))) {
                    if((boolean) loJSON.get("reverse")){
                        return loJSON;
                    } else {
                        fnRow = (int) loJSON.get("row");
                        Serial(fnRow).isReverse(true);
                    }
                }
                
                Serial(fnRow).setSerialId(lsSerialId);
                Serial(fnRow).setEntryNo(fnEntryNo);
            }
            
            System.out.println("Serial 1 : " + Serial(fnRow).InventorySerial().getSerial01());
            System.out.println("Serial 2 : " + Serial(fnRow).InventorySerial().getSerial02());
            
        } else {
            poJSON = setJSON("error", "No record loaded.");
            poJSON.put("row", fnRow);
            return poJSON;
        }
        
        poJSON = setJSON("success", "success");
        poJSON.put("row", fnRow);
        return poJSON;
    }
    
    public JSONObject checkExistingDetail(int row, String stockId){
        JSONObject loJSON = new JSONObject();
        loJSON.put("row", row);
        if(stockId == null){
            stockId = "";
        }
        int lnRow = 0;
        for (int lnCtr = 0; lnCtr <= getDetailCount()- 1; lnCtr++) {
            if(Detail(lnCtr).isReverse()){
                lnRow++;
            }
            if (lnCtr != row) {
                //Check Existing Stock Id
                if(stockId.equals(Detail(lnCtr).getStockId())){
                    if(Detail(lnCtr).isReverse()){
                        loJSON.put("result", "error");
                        loJSON.put("message", "Item already exists in the transaction detail at row "+lnRow+".");
                        loJSON.put("reverse", true);
                        loJSON.put("row", lnCtr);
                        return loJSON;
                    } else {
                        loJSON.put("result", "error");
                        loJSON.put("reverse", false);
                        loJSON.put("row", lnCtr);
                        return loJSON;
                    }
                }   
            }
        }
        
        loJSON.put("result", "success");
        loJSON.put("message", "success");
        return loJSON;
    }
    
    public JSONObject checkExistingSerial(int row, String serialId){
        JSONObject loJSON = new JSONObject();
        loJSON.put("row", row);
        if(serialId == null){
            serialId = "";
        }
        int lnRow = 0;
        for (int lnCtr = 0; lnCtr <= getSerialListCount()- 1; lnCtr++) {
            if(Serial(lnCtr).isReverse()){
                lnRow++;
            }
            if (lnCtr != row) {
                //Check Existing Serial Id
                if(serialId.equals(Serial(lnCtr).getSerialId())){
                    if(Serial(lnCtr).isReverse()){
                        loJSON.put("result", "error");
                        loJSON.put("message", "Serial already exists in the serial detail at row "+lnRow+".");
                        loJSON.put("reverse", true);
                        loJSON.put("row", lnCtr);
                        return loJSON;
                    } else {
                        loJSON.put("result", "error");
                        loJSON.put("reverse", false);
                        loJSON.put("row", lnCtr);
                        return loJSON;
                    }
                }   
            }
        }
        
        loJSON.put("result", "success");
        loJSON.put("message", "success");
        return loJSON;
    }

    @Override
    public JSONObject initFields() {
        poJSON = new JSONObject();

        try {
            //Put initial model values here/
            Master().setIndustryCode(psIndustryId);
            Master().setCompanyId(psCompanyId);
            Master().setBranchCode(poGRider.getBranchCode());
            Date ldServerDate = poGRider.getServerDate();
            Master().setTransactionDate(SQLUtil.toDate(xsDateShort(ldServerDate), SQLUtil.FORMAT_SHORT_DATE));

        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    protected JSONObject isEntryOkay(String status) {
        InvAdjustmentValidator loValidator = new InvAdjustmentValidator();
        loValidator.setApplicationDriver(poGRider);
        loValidator.setTransactionStatus(status);
        loValidator.setMaster(Master());
        
        List<Model_Inv_Adjustment_Detail> laDetail = paDetail.stream()
             .map(m -> (Model_Inv_Adjustment_Detail) m)
             .collect(Collectors.toList());
        loValidator.setInvDetails(laDetail);
        loValidator.setInvSerials(paSerial);
        poJSON = loValidator.validate();
        return poJSON;
    }

    @Override
    public JSONObject willSave() throws SQLException, GuanzonException, CloneNotSupportedException {
        poJSON = new JSONObject();

        /*Put system validations and other assignments here*/
        System.out.println("Class Edit Mode : " + getEditMode());
        System.out.println("Master Edit Mode : " + Master().getEditMode());

        //Re-set the transaction no
        if(getEditMode() == EditMode.ADDNEW){
            Master().setTransactionNo(Master().getNextCode());
        }

        poJSON = isEntryOkay(Master().getTransactionStatus());
        if (!isJSONSuccess(poJSON)) {
            return poJSON;
        }

        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next(); // Store the item before checking conditions
            String lsDetail = (String) item.getValue("sStockIDx");
            String lnCreditQty = item.getValue("nCredtQty").toString();
            String lnDebitQty = item.getValue("nDebitQty").toString(); //Double.valueOf(item.getValue("nDebitQty").toString());
            String lnEntryNo = item.getValue("nEntryNox").toString();
            String lsReverse = item.getValue("cReversex").toString();
            if ((lsDetail == null || "".equals(lsDetail)) || (Double.valueOf(lnDebitQty) <= 0.00 && Double.valueOf(lnCreditQty) <= 0.00) || "-".equals(lsReverse)) {
                if(item.getEditMode() == EditMode.ADDNEW){
                    detail.remove(); // Correctly remove the item
                } else {
                    //Set the original qty value of the deactivated entry no
                    Model_Inv_Adjustment_Detail loObj = new InvModels(poGRider).InventoryAdjustmentDetail();
                    loObj.initialize();
                    poJSON = loObj.openRecord(Master().getTransactionNo(), lnEntryNo);
                    if(!isJSONSuccess(poJSON)){
                        return poJSON;
                    }
                    
                    item.setValue("cReversex","-"); //deactivate the detail
                    item.setValue("nDebitQty",loObj.getDebitQuantity());
                    item.setValue("nCredtQty",loObj.getCreditQuantity());
                    
                    //remove the newly added serial linked to the current entry no
                    int lnCtr = getSerialListCount() - 1;
                    while (lnCtr >= 0) {
                        if(Integer.valueOf(lnEntryNo) == Serial(lnCtr).getEntryNo()
                            && Serial(lnCtr).getEditMode() == EditMode.ADDNEW) {
                            paSerial.remove(lnCtr);
                        }
                        lnCtr--;
                    }
                    
                }
            }
        }

        if (getDetailCount() <= 0) {
            poJSON = setJSON("error", "No record detail to be save.");
            return poJSON;
        }

        for (int lnCtr = 0; lnCtr < getDetailCount(); lnCtr++) {
            Detail(lnCtr).setTransactionNo(Master().getTransactionNo());
            Detail(lnCtr).setEntryNo(lnCtr+1);
            Detail(lnCtr).setModifiedDate(poGRider.getServerDate());
        }
        
        Iterator<Model_Inv_Adjustment_Serial> serial = SerialList().iterator();
        while (serial.hasNext()) {
            Model item = serial.next(); // Store the item before checking conditions
            String lsDetail = (String) item.getValue("sSerialID");
            if ((lsDetail == null || "".equals(lsDetail))) {
                if(item.getEditMode() == EditMode.ADDNEW){
                    serial.remove(); // Correctly remove the item
                }
            }
        }

        for (int lnCtr = 0; lnCtr < getSerialListCount(); lnCtr++) {
            Serial(lnCtr).setTransactionNo(Master().getTransactionNo());
            Serial(lnCtr).setModifiedDate(poGRider.getServerDate());
        }

        //check inv adjustment serial
        int lnRowNum = 0;
        for (int lnRow = 0; lnRow < getDetailCount(); lnRow++) {
            if(Detail(lnRow).isReverse()){
                lnRowNum++;
            
                poJSON = validateCredit(Detail(lnRow).getCreditQuantity(), lnRow);
                if(!isJSONSuccess(poJSON)){
                    return poJSON;
                }
            
                if(Detail(lnRow).isSerialized()){
                    int lnEntryNo = Detail(lnRow).getEntryNo();
                    int lnCreditQty = (int) Math.round(Detail(lnRow).getCreditQuantity());
                    int lnDebitQty = (int) Math.round(Detail(lnRow).getDebitQuantity());
                    int lnCount = getActiveSerialCount(lnEntryNo); //get all active serial only

                    if(lnCreditQty > 0) {
                        if (lnCount != lnCreditQty) {
                            poJSON.put("result", "error");
                            poJSON.put("message", "Entry no. " + lnRowNum + " requires " + lnCreditQty
                                    + " serial(s), but " + lnCount + " were entered.");
                            return poJSON;
                        }
                    } else {
                        if (lnCount != lnDebitQty) {
                            poJSON.put("result", "error");
                            poJSON.put("message", "Entry no. " + lnRowNum + " requires " + lnDebitQty
                                    + " serial(s), but " + lnCount + " were entered.");
                            return poJSON;
                        }
                    }
                }
            }
        }

        //assign other info on attachment
        for (int lnCtr = 0; lnCtr <= getTransactionAttachmentCount()- 1; lnCtr++) {
            TransactionAttachmentList(lnCtr).getModel().setSourceNo(Master().getTransactionNo());
            TransactionAttachmentList(lnCtr).getModel().setSourceCode(SOURCE_CODE);
            TransactionAttachmentList(lnCtr).getModel().setBranchCode(poGRider.getBranchCode());
            TransactionAttachmentList(lnCtr).getModel().setImagePath(System.getProperty("sys.default.path.temp.attachments"));

            String lsOriginalFileName = TransactionAttachmentList(lnCtr).getModel().getFileName();
            //Check existing file name in database
            if(EditMode.ADDNEW == TransactionAttachmentList(lnCtr).getModel().getEditMode()
                    && "0".equals(TransactionAttachmentList(lnCtr).getModel().getSendStatus())){
                int lnCopies = 0;
                String fsFilePath = TransactionAttachmentList(lnCtr).getModel().getImagePath() + "/" + TransactionAttachmentList(lnCtr).getModel().getFileName();
                String lsNewFileName = TransactionAttachmentList(lnCtr).getModel().getFileName();
                while ("error".equals((String)checkExistingFileName(lsNewFileName).get("result"))) {
                    lnCopies++;
                    //Rename the file
                    int dotIndex = TransactionAttachmentList(lnCtr).getModel().getFileName().lastIndexOf(".");
                    if (dotIndex == -1) {
                        lsNewFileName = TransactionAttachmentList(lnCtr).getModel().getFileName() +"_"+lnCopies;
                    } else {
                        lsNewFileName = TransactionAttachmentList(lnCtr).getModel().getFileName().substring(0, dotIndex) +"_"+ lnCopies +TransactionAttachmentList(lnCtr).getModel().getFileName().substring(dotIndex);
                    }
                }

                if(lnCopies > 0){
                    Path source = Paths.get(fsFilePath);
                    try {
                        // Copy file into the target directory with a new name
                        Path target = Paths.get(System.getProperty("sys.default.path.temp.attachments")).resolve(lsNewFileName);
                        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                        //check if file is existing
                        int lnChecker = 0;
                        File file = new File(TransactionAttachmentList(lnCtr).getModel().getImagePath() + "/" + lsNewFileName);
                        while(!file.exists() && lnChecker < 5){
                            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("Re-Copying... " + lnChecker);
                            lnChecker++;
                        }
                        TransactionAttachmentList(lnCtr).getModel().setFileName(lsNewFileName);
                        System.out.println("File copied successfully as " + lsNewFileName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            //Upload Attachment when send status is 0
            try {
                if("0".equals(TransactionAttachmentList(lnCtr).getModel().getSendStatus())){
                    poJSON = uploadCASAttachments(poGRider, System.getProperty("sys.default.access.token"), lnCtr,lsOriginalFileName);
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            }

        }

        Master().setModifiedBy(poGRider.Encrypt(poGRider.getUserID()));
        Master().setModifiedDate(poGRider.getServerDate());
        poJSON = setJSON("success", "success");
        return poJSON;
    }

    @Override
    protected JSONObject saveOthers() {
        try {
            poJSON = new JSONObject();
            
            //Save Serial
            for (int lnCtr = 0; lnCtr < getSerialListCount(); lnCtr++) {
                Model_Inv_Adjustment_Serial loSerial = Serial(lnCtr);
                if (loSerial.getEditMode() == EditMode.ADDNEW || loSerial.getEditMode() == EditMode.UPDATE) {
                    loSerial.setModifiedDate(poGRider.getServerDate());
                    poJSON = loSerial.saveRecord();
                    if (!isJSONSuccess(poJSON)) {
                        return poJSON;
                    }
                }
            }

            //Save Attachments
            for (int lnCtr = 0; lnCtr <= getTransactionAttachmentCount() - 1; lnCtr++) {
                if (paAttachments.get(lnCtr).getEditMode() == EditMode.ADDNEW || paAttachments.get(lnCtr).getEditMode() == EditMode.UPDATE) {
                    paAttachments.get(lnCtr).getModel().setModifyingId(poGRider.Encrypt(poGRider.getUserID()));
                    paAttachments.get(lnCtr).getModel().setModifiedDate(poGRider.getServerDate());
                    paAttachments.get(lnCtr).setWithParentClass(true);
                    System.out.println("CHECK ATTACHMENT RECORD STAT : " + paAttachments.get(lnCtr).getModel().getRecordStatus());
                    poJSON = paAttachments.get(lnCtr).saveRecord();
                    if ("error".equals((String) poJSON.get("result"))) {
                        return poJSON;
                    }
                }
            }

        } catch (ExceptionInInitializerError | SQLException | GuanzonException | CloneNotSupportedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON = setJSON("error", MiscUtil.getException(ex));
            return poJSON;
        }

        return poJSON;
    }


    @Override
    public JSONObject save() throws CloneNotSupportedException, SQLException, GuanzonException {
        /*Put saving business rules here*/
        return isEntryOkay(InvAdjustmentStatus.OPEN);
    }

    @Override
    public void initSQL() {
        String lsCondition = "";

        if(psTranStat != null && !"".equals(psTranStat)){
            if (psTranStat.length() > 1) {
                for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                    lsCondition += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
                }

                lsCondition = "a.cTranStat IN (" + lsCondition.substring(2) + ")";
            } else {
                lsCondition = "a.cTranStat = " + SQLUtil.toSQL(psTranStat);
            }
        }

        SQL_BROWSE ="  SELECT " +
                    "  a.sTransNox " +
                    ", a.sIndstCdx " +
                    ", a.sCompnyID " +
                    ", a.sBranchCd " +
                    ", a.sCategrCd " +
                    ", a.dTransact " +
                    ", a.sReferNox " +
                    ", a.sRemarksx " +
                    ", a.cTranStat " +
                    ", b.sBranchNm " +
                    ", c.sDescript " +
                    ",     CASE    " +
                    "         WHEN a.cTranStat = '0'    " +
                    "         THEN 'OPEN'    " +
                    "         WHEN a.cTranStat = '1'    " +
                    "         THEN 'CONFIRMED'    " +
                    "         WHEN a.cTranStat = '2'    " +
                    "         THEN 'CANCELLED'    " +
                    "         WHEN a.cTranStat = '3'    " +
                    "         THEN 'VOIDED'    " +
                    "         ELSE 'UNKNOWN'   " +
                    "     END AS xTranStat    " +
                    " FROM Inv_Adjustment_Master a " +
                    " LEFT JOIN Branch b ON b.sBranchCd = a.sBranchCd " +
                    " LEFT JOIN Category c ON c.sCategrCd = a.sCategrCd ";
        if(lsCondition != null && !"".equals(lsCondition)){
            SQL_BROWSE = MiscUtil.addCondition(SQL_BROWSE, lsCondition);
        }
    }
    
    private String InvSerial_SQL(){
        return " SELECT " +
                "a.sSerialID " +
                ", a.sStockIDx " +
                ", a.sSerial01 " +
                ", a.sSerial02 " +
                ", a.cConditnx " +
                ", a.sCompnyID " +
                ", a.sBranchCd " +
                ", a.cLocation " +
                ", a.sWHouseID " +
                ", b.sCStckrNo " +
                ", b.sPlateNoP " +
                ", c.sDescript " +
                ", d.sDescript AS sBrandNme " +
                ", e.sDescript AS sModelNme " +
                ", f.sDescript AS sVarntNme " +
                ", g.sDescript AS sColorNme " +
                "FROM Inv_Serial a " +
                "LEFT JOIN Inv_Serial_Registration b ON b.sSerialID = a.sSerialID " +
                "LEFT JOIN Inventory c ON c.sStockIDx = a.sStockIDx " +
                "LEFT JOIN Brand d ON d.sBrandIDx = c.sBrandIDx " +
                "LEFT JOIN Model e ON e.sModelIDx = c.sModelIDx " +
                "LEFT JOIN Model_Variant f ON f.sVrntIDxx = c.sVrntIDxx " +
                "LEFT JOIN Color g ON g.sColorIDx = c.sColorIDx ";
    }
    
    private String InvMaster_SQL(){
        return  " SELECT a.sBranchCd " +
                ", a.sStockIDx " +
                ", b.sBarCodex " +
                ", b.sDescript " +
                ", a.nQtyOnHnd " +
                ", a.cConditnx " +
                ", c.sDescript AS sBrandNme " +
                ", d.sDescript AS sModelNme " +
                ", e.sDescript AS sColorNme " +
                ", f.sDescript AS sVrntName " +
                "FROM Inv_Master a " +
                "LEFT JOIN Inventory b ON b.sStockIDx = a.sStockIDx " +
                "LEFT JOIN Brand c ON c.sBrandIDx = b.sBrandIDx  " +
                "LEFT JOIN Model d ON d.sModelIDx = b.sModelIDx  " +
                "LEFT JOIN Color e ON e.sColorIDx = b.sColorIDx " +
                "LEFT JOIN Model_Variant f ON f.sVrntIDxx = b.sVrntIDxx ";
    }
    
    private JSONObject generateJournal(String fsStatus){
        poJSON = new JSONObject();
        try {
            
            if(getTotalCreditCost() <= 0.00 && getTotalDebitCost() <= 0.00){
                poJSON = setJSON("success", "success");//no journal to be created when total cost of debit / credit is 0.00
                return poJSON;
            }
            
            Journal loJournal = new CashflowControllers(poGRider, logwrapr).Journal();
            poJSON = loJournal.InitTransaction();
            if(!isJSONSuccess(poJSON)){
                return poJSON;
            }
            if(fsStatus.equals(InvAdjustmentStatus.CONFIRMED)){
                poJSON = loJournal.NewTransaction();
                if(!isJSONSuccess(poJSON)){
                    return poJSON;
                }
                
                Date ldServerDate = poGRider.getServerDate();
                
                //Retrieve TBJ
                //retreiving using column index
                JSONObject jsonmaster = new JSONObject();
                for (int lnCtr = 1; lnCtr <= Master().getColumnCount(); lnCtr++){
                    System.out.println(Master().getColumn(lnCtr) + " ->> " + Master().getValue(lnCtr));
                    jsonmaster.put(Master().getColumn(lnCtr),  Master().getValue(lnCtr));
                }
                    
                JSONArray jsondetails = new JSONArray();
                JSONObject jsondetail = new JSONObject();
                for (int lnCtr = 0; lnCtr <= Detail().size() - 1; lnCtr++){
                    jsondetail = new JSONObject();
                    for (int lnCol = 1; lnCol <= Detail(lnCtr).getColumnCount(); lnCol++){
                        System.out.println(Detail(lnCtr).getColumn(lnCol) + " ->> " + Detail(lnCtr).getValue(lnCol));
                        jsondetail.put(Detail(lnCtr).getColumn(lnCol),  Detail(lnCtr).getValue(lnCol));
                    }
                    jsondetails.add(jsondetail);
                }

                jsondetail = new JSONObject();
                jsondetail.put("Inv_Adjustment_Master", jsonmaster);
                jsondetail.put("Inv_Adjustment_Detail", jsondetails);
                    
                TBJTransaction tbj = new TBJTransaction(SOURCE_CODE,Master().getIndustryCode(), Master().getCategoryCode()); 
                tbj.setGRiderCAS(poGRider);
                tbj.setData(jsondetail);
                jsonmaster = tbj.processRequest();
                int lnRow = 0;
                if(jsonmaster.get("result").toString().equalsIgnoreCase("success")){
                    List<TBJEntry> xlist = tbj.getJournalEntries();
                    for (TBJEntry xlist1 : xlist) {
                        System.out.println("Account:" + xlist1.getAccount() );
                        System.out.println("Debit:" + xlist1.getDebit());
                        System.out.println("Credit:" + xlist1.getCredit());
                        lnRow = loJournal.getDetailCount()-1;
                        loJournal.Detail(lnRow).setForMonthOf(SQLUtil.toDate(xsDateShort(ldServerDate), SQLUtil.FORMAT_SHORT_DATE));
                        loJournal.Detail(lnRow).setAccountCode(xlist1.getAccount());
                        loJournal.Detail(lnRow).setCreditAmount(xlist1.getCredit());
                        loJournal.Detail(lnRow).setDebitAmount(xlist1.getDebit());
                        loJournal.AddDetail();
                    }
                } else {
                    System.out.println(jsonmaster.toJSONString());
                    return setJSON("error", jsonmaster.toJSONString());
                }

                //Journa Entry Master
                loJournal.Master().setAccountPerId("");
                loJournal.Master().setIndustryCode(Master().getIndustryCode());
                loJournal.Master().setBranchCode(Master().getBranchCode());
                loJournal.Master().setDepartmentId(poGRider.getDepartment());
                loJournal.Master().setTransactionDate(SQLUtil.toDate(xsDateShort(ldServerDate), SQLUtil.FORMAT_SHORT_DATE)); 
                loJournal.Master().setCompanyId(Master().getCompanyId());
                loJournal.Master().setSourceCode(getSourceCode());
                loJournal.Master().setSourceNo(Master().getTransactionNo());
                
                //Validate Journal Detail
                boolean lbValDetail = false;
                for(int lnCtr = 0; lnCtr < loJournal.getDetailCount(); lnCtr++){
                    if(checkNotEmpty(loJournal.Detail(lnCtr).getAccountCode())){
                        lbValDetail = true;
                        break;
                    }
                }
                
                if(!lbValDetail){
                    poJSON = setJSON("error", "No journal detail to be save."
                                    + "\nCheck TBJ Entry for default journal detail value.");
                    return poJSON;
                }
                
                loJournal.setWithParent(true);
                loJournal.setWithUI(false);
                poJSON = loJournal.SaveTransaction();
                if(!isJSONSuccess(poJSON)){
                    return poJSON;
                }
                
                poJSON = loJournal.OpenTransaction(loJournal.Master().getTransactionNo());
                if(!isJSONSuccess(poJSON)){
                    return poJSON;
                }

                loJournal.setWithParent(true);
                loJournal.setWithUI(false);
                poJSON = loJournal.ConfirmTransaction("ConfirmTransaction"); //Auto confirm journal
                if(!isJSONSuccess(poJSON)){
                    return poJSON;
                }
                
            } else {
                //Cancel the generated Journal
                String lsJournalTransNo = getJournalTransNo();
                if(checkNotEmpty(lsJournalTransNo)){
                    poJSON = loJournal.OpenTransaction(lsJournalTransNo);
                    if(!isJSONSuccess(poJSON)){
                        return poJSON;
                    }

                    loJournal.setWithParent(true);
                    loJournal.setWithUI(false);
                    poJSON = loJournal.CancelTransaction("CancelTransaction");
                    if(!isJSONSuccess(poJSON)){
                        return poJSON;
                    }
                }
            }
            
        } catch (SQLException | GuanzonException | ScriptException | CloneNotSupportedException | ParseException | NullPointerException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            return setJSON("error", MiscUtil.getException(ex));
        } 
            
        return poJSON;
    }
    
    private String getJournalTransNo(){
        String lsTransNo = "";
        try {
            Model_Journal_Master loObj = new CashflowModels(poGRider).Journal_Master();
            String lsSQL = MiscUtil.addCondition(MiscUtil.makeSelect(loObj),
                            " sSourceNo = " + SQLUtil.toSQL(Master().getTransactionNo())
                            + " AND sSourceCD = " + SQLUtil.toSQL(getSourceCode())
                            + " AND sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryCode())
                            + " AND sCompnyCd = " + SQLUtil.toSQL(Master().getCompanyId())
                            + " AND cTranStat != " + SQLUtil.toSQL(JournalStatus.CANCELLED)
                            + " AND cTranStat != " + SQLUtil.toSQL(JournalStatus.VOID)
            );
            System.out.println("Executing SQL: " + lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
                if (MiscUtil.RecordCount(loRS) > 0) {
                    if(loRS.next()){
                        lsTransNo = loRS.getString("sTransNox");
                    }
                }
            MiscUtil.close(loRS);
        } catch (SQLException ex) {
            System.out.println("No record loaded.");
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }

        return lsTransNo;
    }

    private TransactionAttachment TransactionAttachment() throws SQLException, GuanzonException {
        return new SysTableContollers(poGRider, null).TransactionAttachment();
    }

    public TransactionAttachment TransactionAttachmentList(int row) {
        return (TransactionAttachment) paAttachments.get(row);
    }

    public int getTransactionAttachmentCount() {
        if (paAttachments == null) {
            paAttachments = new ArrayList<>();
        }

        return paAttachments.size();
    }

    /**
     * Adds a new blank transaction attachment entry.
     *
     * @return JSON result containing status and message.
     * @throws SQLException If a database access error occurs.
     * @throws GuanzonException If attachment model operations fail.
     */
    public JSONObject addAttachment()
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();

        if (paAttachments.isEmpty()) {
            paAttachments.add(TransactionAttachment());
            poJSON = paAttachments.get(getTransactionAttachmentCount() - 1).newRecord();
        } else {
            if (!paAttachments.get(paAttachments.size() - 1).getModel().getTransactionNo().isEmpty()) {
                paAttachments.add(TransactionAttachment());
            } else {
                poJSON.put("result", "error");
                poJSON.put("message", "Unable to add transaction attachment.");
                return poJSON;
            }
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    /**
     * Removes or deactivates an attachment entry by row index.
     *
     * @param fnRow Attachment row index.
     * @return JSON result containing status and message.
     * @throws GuanzonException If attachment model operations fail.
     * @throws SQLException If a database access error occurs.
     */
    public JSONObject removeAttachment(int fnRow) throws GuanzonException, SQLException{
        poJSON = new JSONObject();
        if(getTransactionAttachmentCount() <= 0){
            poJSON.put("result", "error");
            poJSON.put("message", "No transaction attachment to be removed.");
            return poJSON;
        }

        if(paAttachments.get(fnRow).getEditMode() == EditMode.ADDNEW){
            paAttachments.remove(fnRow);
            System.out.println("Attachment :"+ fnRow+" Removed");
        } else {
            paAttachments.get(fnRow).getModel().setRecordStatus(RecordStatus.INACTIVE);
            System.out.println("Attachment :"+ fnRow+" Deactivate");
        }

        poJSON.put("result", "success");
        return poJSON;
    }

    /**
     * Adds or reactivates an attachment record for the provided file name.
     *
     * @param fFileName Attachment file name.
     * @return Row index of the active attachment entry.
     * @throws SQLException If a database access error occurs.
     * @throws GuanzonException If attachment model operations fail.
     */
    public int addAttachment(String fFileName) throws SQLException, GuanzonException{
        for(int lnCtr = 0;lnCtr <= getTransactionAttachmentCount() - 1;lnCtr++){
            if(fFileName.equals(paAttachments.get(lnCtr).getModel().getFileName())
                    && RecordStatus.INACTIVE.equals(paAttachments.get(lnCtr).getModel().getRecordStatus())){
                paAttachments.get(lnCtr).getModel().setRecordStatus(RecordStatus.ACTIVE);
                System.out.println("Attachment :"+ lnCtr+" Activate");
                return lnCtr;
            }
        }

        addAttachment();
        paAttachments.get(getTransactionAttachmentCount() - 1).getModel().setFileName(fFileName);
        paAttachments.get(getTransactionAttachmentCount() - 1).getModel().setSourceNo(Master().getTransactionNo());
        paAttachments.get(getTransactionAttachmentCount() - 1).getModel().setRecordStatus(RecordStatus.ACTIVE);
        return getTransactionAttachmentCount() - 1;
    }

    /**
     * Copies a local file into the temporary attachment directory.
     *
     * @param fsPath Source file path.
     */
    public void copyFile(String fsPath){
        Path source = Paths.get(fsPath);
        Path targetDir = Paths.get(System.getProperty("sys.default.path.temp.attachments"));

        try {
            // Ensure target directory exists
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            // Copy file into the target directory
            Files.copy(source, targetDir.resolve(source.getFileName()),
                    StandardCopyOption.REPLACE_EXISTING);

            System.out.println("File copied successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads and downloads existing attachments of the current transaction.
     *
     * @return JSON result containing status and message.
     * @throws SQLException If a database access error occurs.
     * @throws GuanzonException If attachment model operations fail.
     */
    public JSONObject loadAttachments()
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        paAttachments = new ArrayList<>();

        TransactionAttachment loAttachment = new SysTableContollers(poGRider, null).TransactionAttachment();
        List loList = loAttachment.getAttachments(SOURCE_CODE, Master().getTransactionNo());
        for (int lnCtr = 0; lnCtr <= loList.size() - 1; lnCtr++) {
            paAttachments.add(TransactionAttachment());
            poJSON = paAttachments.get(getTransactionAttachmentCount() - 1).openRecord((String) loList.get(lnCtr));
            if ("success".equals((String) poJSON.get("result"))) {
                if(getEditMode() == EditMode.UPDATE){
                    poJSON = paAttachments.get(getTransactionAttachmentCount() - 1).updateRecord();
                }
                System.out.println(paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getTransactionNo());
                System.out.println(paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getSourceNo());
                System.out.println(paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getSourceCode());
                System.out.println(paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getFileName());
            }

            //Download Attachments
            poJSON = WebFile.DownloadFile(WebFile.getAccessToken(System.getProperty("sys.default.access.token"))
                    , "0032" //Constant
                    , "" //Empty
                    , paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getFileName()
                    , SOURCE_CODE
                    , paAttachments.get(getTransactionAttachmentCount() - 1).getModel().getSourceNo()
                    , "");
            if ("success".equals((String) poJSON.get("result"))) {

                poJSON = (JSONObject) poJSON.get("payload");
                if(WebFile.Base64ToFile((String) poJSON.get("data")
                        , (String) poJSON.get("hash")
                        , System.getProperty("sys.default.path.temp.attachments") + "/"
                        , (String) poJSON.get("filename"))){
                    System.out.println("poJSON success: " +  poJSON.toJSONString());
                    System.out.println("File downloaded succesfully.");
                } else {
                    poJSON = (JSONObject) poJSON.get("error");
                    poJSON.put("result", "error");
                    System.out.println("ERROR WebFile.DownloadFile: " + poJSON.get("message"));
                    System.out.println("poJSON error WebFile.DownloadFile: " + poJSON.toJSONString());
                }

            } else {
                System.out.println("poJSON error WebFile.DownloadFile: " + poJSON.toJSONString());
            }
        }
        return poJSON;
    }

    /**
     * Checks whether a file name already exists in attachment records.
     *
     * @param fsFileName File name to validate.
     * @return JSON result indicating duplicate detection.
     * @throws SQLException If a database access error occurs.
     * @throws GuanzonException If attachment model operations fail.
     */
    public JSONObject checkExistingFileName(String fsFileName) throws SQLException, GuanzonException{
        poJSON = new JSONObject();

        String lsSQL = MiscUtil.addCondition(MiscUtil.makeSelect(TransactionAttachment().getModel()),
                " sFileName = " + SQLUtil.toSQL(fsFileName)
        );
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) > 0) {
                if(loRS.next()){
                    if(loRS.getString("sFileName") != null && !"".equals(loRS.getString("sFileName"))){
                        poJSON.put("result", "error");
                        poJSON.put("message", "File name already exist in database.\nTry changing the file name to upload.");
                    }
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            System.out.println("No record loaded.");
        }
        return poJSON;
    }

    /**
     * Upload Attachment
     * @param instance
     * @param access
     * @param fnRow
     * @return
     * @throws java.lang.Exception
     */
    public JSONObject uploadCASAttachments(GRiderCAS instance, String access, int fnRow, String fsOriginalFileName) throws Exception{
        poJSON = new JSONObject();
        System.out.println("Uploading... : fsOriginalFileName : " + fsOriginalFileName);
        System.out.println("New File Name... : " + paAttachments.get(fnRow).getModel().getFileName());
        String hash;
        String lsFile = paAttachments.get(fnRow).getModel().getFileName();

        //check if new file is existing
        File file = new File(paAttachments.get(fnRow).getModel().getImagePath() + "/" + lsFile);
        if(!file.exists()){
            //check if original file is existing
            lsFile = fsOriginalFileName;
            file = new File(paAttachments.get(fnRow).getModel().getImagePath() + "/" + lsFile);
            if(!file.exists()){
                poJSON.put("result", "error");
                poJSON.put("message", "Cannot locate file in " + paAttachments.get(fnRow).getModel().getImagePath() + "/" + lsFile
                        + ".\nContact system administrator for assistance.");
                return poJSON;
            }
        }

        //check if file hash is not empty
        hash = paAttachments.get(fnRow).getModel().getMD5Hash();
        if(paAttachments.get(fnRow).getModel().getMD5Hash() == null || "".equals(paAttachments.get(fnRow).getModel().getMD5Hash())){
            hash = MiscReplUtil.md5Hash(paAttachments.get(fnRow).getModel().getImagePath() + "/" + lsFile);
        }

        JSONObject result = WebFile.UploadFile(getAccessToken(access)
                , "0032"
                , ""
                , paAttachments.get(fnRow).getModel().getFileName()
                , instance.getBranchCode()
                , hash
                , encodeFileToBase64Binary(file)
                , paAttachments.get(fnRow).getModel().getSourceCode()
                , paAttachments.get(fnRow).getModel().getSourceNo()
                , "");

        if("error".equalsIgnoreCase((String) result.get("result"))){
            System.out.println("Upload Error : " + result.toJSONString());
            System.out.println("Upload Error : " + paAttachments.get(fnRow).getModel().getFileName());
            poJSON.put("result", "error");
            poJSON.put("message", "System error while uploading file "+ paAttachments.get(fnRow).getModel().getFileName()
                    + ".\nContact system administrator for assistance.");
            return poJSON;
        }
        paAttachments.get(fnRow).getModel().setMD5Hash(hash);
        paAttachments.get(fnRow).getModel().setSendStatus("1");
        System.out.println("Upload Success : " + paAttachments.get(fnRow).getModel().getFileName());
        poJSON.put("result", "success");
        return poJSON;
    }

    /**
     * Encodes a file into Base64 string content for upload.
     *
     * @param file File to encode.
     * @return Base64-encoded content.
     * @throws Exception If file reading fails.
     */
    private static String encodeFileToBase64Binary(File file) throws Exception{
        FileInputStream fileInputStreamReader = new FileInputStream(file);
        byte[] bytes = new byte[(int)file.length()];
        fileInputStreamReader.read(bytes);
        return new String(org.apache.commons.codec.binary.Base64.encodeBase64(bytes), "UTF-8");
    }

    private static JSONObject token = null;
    /**
     * Loads and refreshes API access token from a local token file.
     *
     * @param access Token file path.
     * @return Access key value, or {@code null} when unavailable.
     */
    private static String getAccessToken(String access){
        try {
            JSONParser oParser = new JSONParser();
            if(token == null){
                token = (JSONObject)oParser.parse(new FileReader(access));
            }

            Calendar current_date = Calendar.getInstance();
            current_date.add(Calendar.MINUTE, -25);
            Calendar date_created = Calendar.getInstance();
            date_created.setTime(SQLUtil.toDate((String) token.get("created") , SQLUtil.FORMAT_TIMESTAMP));

            //Check if token is still valid within the time frame
            //Request new access token if not in the current period range
            if(current_date.after(date_created)){
                String[] xargs = new String[] {(String) token.get("parent"), access};
                RequestAccess.main(xargs);
                token = (JSONObject)oParser.parse(new FileReader(access));
            }

            return (String)token.get("access_key");
        } catch (IOException ex) {
            return null;
        } catch (ParseException ex) {
            return null;
        }
    }

    public JSONObject printReport(boolean fbIsSummary, Date fdDateFrom, Date fdDateTo)
        throws CloneNotSupportedException, SQLException, GuanzonException {

        poJSON = new JSONObject();
        pbIsPrinted = false;
        
        if(fdDateFrom == null || fdDateTo == null){
            ShowMessageFX.Warning(null, "Computerized Accounting System","Invalid date range.");
            poJSON = setJSON("error", "Invalid date range.");
            return poJSON;
        }

        try {

            String jrxmlPath = System.getProperty("sys.default.path.config");
            if(fbIsSummary){
                jrxmlPath = jrxmlPath + "/Reports/InventoryAdjustmentSummary.jrxml";
            } else {
                jrxmlPath = jrxmlPath + "/Reports/InventoryAdjustmentDetail.jrxml";
            }


            JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlPath);
            String lsWatermarkPath = System.getProperty("sys.default.path.config") + "/Reports/images/";
            JasperPrint finalPrint = null;

            // ---------------------------------------------
            // Parameters
            // ---------------------------------------------
            Map<String, Object> parameters = new HashMap<>();
            
            Master().setCompanyId(psCompanyId);
            Master().setIndustryCode(psIndustryId);
            parameters.put("sCompanyName", Master().Company().getCompanyName());
            String lsAddress = Master().Company().getCompanyAddress();
            if (Master().Company().TownCity().getDescription() != null && !"".equals(Master().Company().TownCity().getDescription())) {
                lsAddress = lsAddress + " " + Master().Company().TownCity().getDescription();
            }

            if (Master().Company().TownCity().Province().getDescription() != null
                    && !"".equals(Master().Company().TownCity().Province().getDescription())) {
                lsAddress = lsAddress+ ", "+ Master().Company().TownCity().Province().getDescription();
            }
            parameters.put("sCompanyAddress", lsAddress.toUpperCase());
            
            DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // adjust if needed
            DateTimeFormatter outputFormat = DateTimeFormatter.ofPattern("MMMM d, yyyy");
            LocalDate dateFrom = LocalDate.parse(xsDateShort(fdDateFrom), inputFormat);
            LocalDate dateTo = LocalDate.parse(xsDateShort(fdDateTo), inputFormat);
            String formattedFrom = dateFrom.format(outputFormat);
            String formattedTo = dateTo.format(outputFormat);
            
            parameters.put("sDateRange",formattedFrom+ " - "+ formattedTo);
            
            // ---------------------------------------------
            // Build data
            // ---------------------------------------------
            List<Map<String, Object>> laSummary = builSummaryList();
            List<Map<String, ?>> summary = new ArrayList<>(laSummary);
            List<Map<String, Object>> laDetail = buildDetailList();
            List<Map<String, ?>> detail = new ArrayList<>(laDetail);

            JRMapCollectionDataSource dataSource;
            if(fbIsSummary){
                dataSource = new JRMapCollectionDataSource(summary);
            } else {
                dataSource = new JRMapCollectionDataSource(detail);
            }
            
            // ---------------------------------------------
            // Generate JasperPrint
            // ---------------------------------------------

            JasperPrint currentPrint =
                    JasperFillManager.fillReport(
                            jasperReport,
                            parameters,
                            dataSource
                    );
            
//             ---------------------------------------------
//             Combine reports
//             ---------------------------------------------

            if (finalPrint == null) {
                finalPrint = currentPrint;
            } else {

                finalPrint.getPages().addAll(
                        currentPrint.getPages()
                );
            }

            // ---------------------------------------------
            // Display final report
            // ---------------------------------------------

            if (finalPrint != null) {
                CustomJasperViewer viewer = new CustomJasperViewer(finalPrint);
                viewer.setVisible(true);
                viewer.addWindowListener(
                        new WindowAdapter() {
                            @Override
                            public void windowClosed(WindowEvent e) {
                                proceedAfterViewerClosed();
                            }
                        }
                );
            }

        } catch (JRException | SQLException | GuanzonException ex) {
            poJSON.put("result", "error");
            poJSON.put("message","Transaction print aborted!");
            Logger.getLogger(getClass().getName())
                    .log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }

        return poJSON;
    }
    
    private void proceedAfterViewerClosed() {
        Platform.runLater(() -> {
            System.out.println("SHOWED!!!!!!!!!!!");
            if ("error".equals((String) poJSON.get("result"))) {
                ShowMessageFX.Warning(null, "Computerized Accounting System",
                        (String) poJSON.get("message"));
            } else {
                if (pbIsPrinted) {
                    ShowMessageFX.Information(null, "Computerized Accounting System",
                            "Vehicle Price List Printed Successfully");
                } else {
                    ShowMessageFX.Warning(null, "Computerized Accounting System",
                            "Printing was canceled by the user.");
                }
            }
        });
    }

    private boolean pbIsPrinted = false;
    public class CustomJasperViewer extends JasperViewer {

        public CustomJasperViewer(final JasperPrint jasperPrint) {
            super(jasperPrint, false);
            customizePrintButton(jasperPrint);
        }

        /* ---- toolbar patch ------------------------------------------ */
        private JSONObject customizePrintButton(final JasperPrint jasperPrint) {

            try {
                JRViewer viewer = findJRViewer(this);
                if (viewer == null) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "JRViewer not found!");
                    return poJSON;
                }
                for (int i = 0; i < viewer.getComponentCount(); i++) {
                    if (viewer.getComponent(i) instanceof JRViewerToolbar) {

                        JRViewerToolbar toolbar = (JRViewerToolbar) viewer.getComponent(i);

                        for (int j = 0; j < toolbar.getComponentCount(); j++) {
                            if (toolbar.getComponent(j) instanceof JButton) {

                                final JButton button = (JButton) toolbar.getComponent(j);

                                if ("Print".equals(button.getToolTipText())) {
                                    /* remove existing handlers */
                                    ActionListener[] old = button.getActionListeners();
                                    for (int k = 0; k < old.length; k++) {
                                        button.removeActionListener(old[k]);
                                    }

                                    /* add our own (anonymous inner‑class, not lambda) */
                                    button.addActionListener(new ActionListener() {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {
                                            try {
                                                pbIsPrinted = JasperPrintManager.printReport(jasperPrint, true);
                                                if (pbIsPrinted) {
                                                    CustomJasperViewer.this.dispose();
                                                } else {
                                                    poJSON.put("result", "error");
                                                    poJSON.put("message",  "Printing was canceled by the user.");
                                                }
                                            } catch (JRException ex) {
                                                poJSON.put("result", "error");
                                                poJSON.put("message",  "Print Failed: " + ex.getMessage());
                                                Logger.getLogger(getClass().getName()).log(Level.SEVERE,  ex.getMessage(), ex);
                                            }
                                        }
                                    });
                                } else {
                                    // Disable all other buttons
//                                    button.setEnabled(false);
//                                    poJSON.put("result", "error");
//                                    poJSON.put("message",  "Transaction print aborted!");
                                }
                            }
                        }
                        toolbar.revalidate();
                        toolbar.repaint();
                    }
                }
            } catch (Exception e) {
                System.out.println("Error customizing print button: " + e.getMessage());
                poJSON.put("result", "error");
                poJSON.put("message", "Error customizing print button: " + e.getMessage());
            }

            return poJSON;
        }

        private JRViewer findJRViewer(Component parent) {
            if (parent instanceof JRViewer) {
                return (JRViewer) parent;
            }
            if (parent instanceof Container) {
                Component[] comps = ((Container) parent).getComponents();
                for (int i = 0; i < comps.length; i++) {
                    JRViewer v = findJRViewer(comps[i]);
                    if (v != null) {
                        return v;
                    }
                }
            }
            return null;
        }
    }
    
    private List<Map<String, Object>> builSummaryList() {
        List<Map<String, Object>> rows = new ArrayList<>();

        try{
            int lnRow = 0;
            for(int lnCtr = 0;lnCtr < getTransactionListCount();lnCtr++){
                InvAdjustment loObj = TransactionList(lnCtr);
                lnRow++;       
                addSummaryList(rows
                        , lnRow
                        , getSafe(loObj.Master().getTransactionNo())
                        , loObj.Master().getTransactionDate()
                        , getSafe(loObj.Master().getReferenceNo())
                        , getSafe(loObj.Master().Branch().getBranchName())
                        , getSafe(loObj.Master().Company().getCompanyName())
                        , getSafe(loObj.Master().Category().getDescription())
                        , loObj.getTotalCreditQty()
                        , loObj.getTotalDebitQty()
                        , loObj.getTotalCreditCost()
                        , loObj.getTotalDebitCost()
                        , getStatus(loObj.Master().getTransactionStatus()).toUpperCase()
                        , getSafe(loObj.Master().getRemarks())
                );
            }
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }
        return rows;
    }
 
    // Keys here MUST match the <field name="..."> values in the .jrxml exactly.
    private void addSummaryList(List<Map<String, Object>> rows
                                , int nRowNo
                                , String sTransNo
                                , Date dTransDate
                                , String sRefNo
                                , String sBranch
                                , String sCompany
                                , String sCategory
                                , Double nTotalCreditQty
                                , Double nTotalDebitQty
                                , Double nTotalCreditCost
                                , Double nTotalDebitCost
                                , String sStatus
                                , String sRemarks
                        ) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("nRowNo", nRowNo);
        row.put("sTransNo", sTransNo);
        row.put("dTransDate", dTransDate);
        row.put("sRefNo", sRefNo);
        row.put("sBranch", sBranch);
        row.put("sCompany", sCompany);
        row.put("sCategory", sCategory);
        row.put("nTotalCreditQty", nTotalCreditQty);
        row.put("nTotalDebitQty", nTotalDebitQty);
        row.put("nTotalCreditCost", nTotalCreditCost);
        row.put("nTotalDebitCost", nTotalDebitCost);
        row.put("sStatus", sStatus);
        row.put("sRemarks", sRemarks);
        rows.add(row);
    }
    
    private List<Map<String, Object>> buildDetailList() {
        List<Map<String, Object>> rows = new ArrayList<>();

        try{
            int lnCount = 0;
            Double lnTotalCost = 0.00;
            for(int lnCtr = 0;lnCtr < getTransactionListCount();lnCtr++){
                InvAdjustment loObj = TransactionList(lnCtr);
                lnTotalCost = 0.00;
                //List Transaction Details
                for(int lnRow = 0;lnRow < loObj.getDetailCount();lnRow++){
                    Model_Inv_Adjustment_Detail loDetail = loObj.Detail(lnRow);
                    if(!loDetail.isReverse()){
                        continue;
                    }
                    
                    if(loDetail.getCreditQuantity() > 0.00){
                        lnTotalCost = loDetail.getInventoryCost() * loDetail.getCreditQuantity();
                    } else {
                        lnTotalCost = loDetail.getInventoryCost() * loDetail.getDebitQuantity();
                    }
                    
                    //if Serialize load serials per detail
                    if(loDetail.isSerialized()){
                        List<Model_Inv_Adjustment_Serial> loSerial = loObj.getSerials(loDetail.getEntryNo(), false);
                        for(Model_Inv_Adjustment_Serial serial : loSerial){
                            lnCount++; 
                            addDetailList(rows
                                    , lnCount
                                    , getSafe(loObj.Master().getTransactionNo())
                                    , loObj.Master().getTransactionDate()
                                    , getSafe(loObj.Master().getReferenceNo())
                                    , getSafe(loObj.Master().Branch().getBranchName())
                                    , getSafe(loObj.Master().Company().getCompanyName())
                                    , getSafe(loObj.Master().Category().getDescription())
                                    , getSafe(loDetail.Inventory().Brand().getDescription())
                                    , getSafe(loDetail.Inventory().Model().getDescription()) + " " + getSafe(loDetail.Inventory().Variant().getDescription())
                                    , getSafe(loDetail.Inventory().Color().getDescription())
                                    , getSafe(serial.InventorySerial().getSerial01())
                                    , getSafe(loDetail.Inventory().getDescription())
                                    , loDetail.getCreditQuantity() > 0 ? 1.00 : 0.00
                                    , loDetail.getDebitQuantity() > 0 ? 1.00 : 0.00
                                    , loDetail.getInventoryCost()
                                    , loDetail.getInventoryCost() //1 qty by default
                                    , getStatus(loObj.Master().getTransactionStatus()).toUpperCase()
                                    , getSafe(loObj.Master().getRemarks())
                            );
                        }
                    } else {
                        lnCount++; 
                        addDetailList(rows
                                , lnCount
                                , getSafe(loObj.Master().getTransactionNo())
                                , loObj.Master().getTransactionDate()
                                , getSafe(loObj.Master().getReferenceNo())
                                , getSafe(loObj.Master().Branch().getBranchName())
                                , getSafe(loObj.Master().Company().getCompanyName())
                                , getSafe(loObj.Master().Category().getDescription())
                                , getSafe(loDetail.Inventory().Brand().getDescription())
                                , getSafe(loDetail.Inventory().Model().getDescription()) + " " + getSafe(loDetail.Inventory().Variant().getDescription())
                                , getSafe(loDetail.Inventory().Color().getDescription()) 
                                , getSafe(loDetail.Inventory().getBarCode())
                                , getSafe(loDetail.Inventory().getDescription())
                                , loDetail.getCreditQuantity()
                                , loDetail.getDebitQuantity()
                                , loDetail.getInventoryCost()
                                , lnTotalCost
                                , getStatus(loObj.Master().getTransactionStatus()).toUpperCase()
                                , getSafe(loObj.Master().getRemarks())
                        );
                    }
                }
            }
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }
        return rows;
    }
 
    // Keys here MUST match the <field name="..."> values in the .jrxml exactly.
    private void addDetailList(List<Map<String, Object>> rows
                                , int nRowNo
                                , String sTransNo
                                , Date dTransDate
                                , String sRefNo
                                , String sBranch
                                , String sCompany
                                , String sCategory
                                , String sBrand
                                , String sModel
                                , String sColor
                                , String sBarcode
                                , String sDescription
                                , Double nCreditQty
                                , Double nDebitQty
                                , Double nCost
                                , Double nTotalCost
                                , String sStatus
                                , String sRemarks
                        ) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("nRowNo", nRowNo);
        row.put("sTransNo", sTransNo);
        row.put("dTransDate", dTransDate);
        row.put("sRefNo", sRefNo);
        row.put("sBranch", sBranch);
        row.put("sCompany", sCompany);
        row.put("sCategory", sCategory);
        row.put("sBrand", sBrand);
        row.put("sModel", sModel);
        row.put("sColor", sColor);
        row.put("sBarcode", sBarcode);
        row.put("sDescription", sDescription);
        row.put("nCreditQty", nCreditQty);
        row.put("nDebitQty", nDebitQty);
        row.put("nCost", nCost);
        row.put("nTotalCost", nTotalCost);
        row.put("sStatus", sStatus);
        row.put("sRemarks", sRemarks);
        rows.add(row);
    }
    
    public JSONObject getUpdateStatusBy(String fsStatus) throws SQLException, GuanzonException {
        String lsUpdateBy = "";
        String lsDate = "";
        String lsSQL = "SELECT b.sModified,b.dModified FROM "+Master().getTable()+" a "
                + " LEFT JOIN Transaction_Status_History b ON b.sSourceNo = a.sTransNox AND b.sTableNme = "+ SQLUtil.toSQL(Master().getTable())
                + " AND b.cRefrStat = "+ SQLUtil.toSQL(fsStatus) ;
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sTransNox = " + SQLUtil.toSQL(Master().getTransactionNo())) ;
        lsSQL = lsSQL + " ORDER BY b.dModified DESC ";
        System.out.println("Execute SQL STATUS : "+fsStatus+" : " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) > 0L) {
                if (loRS.next()) {
                    if(loRS.getString("sModified") != null && !"".equals(loRS.getString("sModified"))){
                        if(loRS.getString("sModified").length() > 10){
                            lsUpdateBy = getSysUser(poGRider.Decrypt(loRS.getString("sModified")),false);
                        } else {
                            lsUpdateBy = getSysUser(loRS.getString("sModified"),false);
                        }
                        // Get the LocalDateTime from your result set
                        LocalDateTime dModified = loRS.getObject("dModified", LocalDateTime.class);
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");
                        lsDate =  dModified.format(formatter);
                    }
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
            return poJSON;
        }

        poJSON.put("result", "success");
        poJSON.put("sUpdateByx", lsUpdateBy);
        poJSON.put("sUpdateDte", lsDate);
        return poJSON;
    }

    protected CachedRowSet getStatusHistoryTest() throws SQLException {
        String lsSQL = "SELECT  a.sTableNme, a.sSourceNo, a.sRemarksx, a.cRefrStat cTranStat, IFNULL(c.sCompnyNm, '-') xModified, IFNULL(e.sCompnyNm, '-') xApproved, a.dModified, a.dApproved, a.sModified, a.sApproved " +
                " FROM Transaction_Status_History a " +
                "LEFT JOIN xxxSysUser b ON b.sUserIDxx = a.sModified " +
                "LEFT JOIN Client_Master c ON b.sEmployNo = c.sClientID " +
                "LEFT JOIN xxxSysUser d ON d.sUserIDxx = a.sApproved " +
                "LEFT JOIN Client_Master e ON d.sEmployNo = e.sClientID " +
                " WHERE a.sSourceNo = " + SQLUtil.toSQL(Master().getTransactionNo()) +
                " AND a.sTableNme = " + SQLUtil.toSQL(Master().getTable()) + " ORDER BY a.dModified";
        System.out.println("STATUS HISTORY : " + lsSQL);
        ResultSet loRS = this.poGRider.executeQuery(lsSQL);
        RowSetFactory factory = RowSetProvider.newFactory();
        CachedRowSet rowset = factory.createCachedRowSet();
        rowset.populate(loRS);
        MiscUtil.close(loRS);
        return rowset;
    }

    @Override
    protected CachedRowSet getStatusHistory() throws SQLException {
        this.poGRider.ensureConnected();
        String lsSQL = "SELECT  a.sTableNme, a.sSourceNo, a.sRemarksx, a.cRefrStat cTranStat, IFNULL(c.sCompnyNm, '-') xModified, IFNULL(e.sCompnyNm, '-') xApproved, a.dModified, a.dApproved, a.sModified, a.sApproved "
                + " FROM GCASys_DBF.Transaction_Status_History a "
                + " LEFT JOIN GCASys_DBF.xxxSysUser b ON b.sUserIDxx = AES_DECRYPT(UNHEX(a.sModified), '08220326') "
                + " LEFT JOIN GGC_ISysDBF.Client_Master c ON b.sEmployNo = c.sClientID "
                + " LEFT JOIN GCASys_DBF.xxxSysUser d ON d.sUserIDxx = AES_DECRYPT(UNHEX(a.sApproved), '08220326') "
                + " LEFT JOIN GGC_ISysDBF.Client_Master e ON d.sEmployNo = e.sClientID "
                + " WHERE a.sSourceNo = " + SQLUtil.toSQL(Master().getTransactionNo()) + " AND a.sTableNme = " + SQLUtil.toSQL(Master().getTable()) + " ORDER BY a.dModified, a.sTransNox";
        ResultSet loRS = this.poGRider.executeQuery(lsSQL);
        RowSetFactory factory = RowSetProvider.newFactory();
        CachedRowSet rowset = factory.createCachedRowSet();
        rowset.populate(loRS);
        MiscUtil.close(loRS);
        return rowset;
    }

    public void ShowStatusHistory() throws SQLException, GuanzonException, Exception {
        CachedRowSet crs;
        if(pbWithUI){
            crs = getStatusHistory();
        } else {
            crs = getStatusHistoryTest();
        }

        crs.beforeFirst();

        while (crs.next()) {
            switch (crs.getString("cRefrStat")) {
                case "":
                    crs.updateString("cRefrStat", "-");
                    break;
                case InvAdjustmentStatus.OPEN:
                    crs.updateString("cRefrStat", "OPEN");
                    break;
                case InvAdjustmentStatus.CANCELLED:
                    crs.updateString("cRefrStat", "CANCELLED");
                    break;
                case InvAdjustmentStatus.VOID:
                    crs.updateString("cRefrStat", "VOIDED");
                    break;
                case InvAdjustmentStatus.CONFIRMED:
                    crs.updateString("cRefrStat", "CONFIRMED");
                    break;
                default:
                    char ch = crs.getString("cRefrStat").charAt(0);
                    String stat = String.valueOf((int) ch - 64);

                    switch (stat) {
                        case InvAdjustmentStatus.OPEN:
                            crs.updateString("cRefrStat", "OPEN");
                            break;
                        case InvAdjustmentStatus.CANCELLED:
                            crs.updateString("cRefrStat", "CANCELLED");
                            break;
                        case InvAdjustmentStatus.VOID:
                            crs.updateString("cRefrStat", "VOIDED");
                            break;
                        case InvAdjustmentStatus.CONFIRMED:
                            crs.updateString("cRefrStat", "CONFIRMED");
                            break;
                    }
            }
            crs.updateRow();
        }

        JSONObject loJSON = getEntryBy();
        String entryBy = "";
        String entryDate = "";

        if (isJSONSuccess(loJSON)) {
            entryBy = (String) loJSON.get("sCompnyNm");
            entryDate = (String) loJSON.get("sEntryDte");
        }

        if(pbWithUI){
            showStatusHistoryUI("Inventory Adjustment", (String) poMaster.getValue("sTransNox"), entryBy, entryDate, crs);
        }
    }
    public JSONObject getEntryBy() throws SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsEntry = "";
        String lsEntryDate = "";
        String lsSQL = " SELECT b.sModified, b.dModified "
                + " FROM "+Master().getTable()+" a "
                + " LEFT JOIN xxxAuditLogMaster b ON b.sSourceNo = a.sTransNox AND b.sEventNme LIKE 'ADD%NEW' AND b.sRemarksx = " + SQLUtil.toSQL(Master().getTable());
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sTransNox =  " + SQLUtil.toSQL(Master().getTransactionNo()));
        lsSQL = lsSQL + " ORDER BY b.dModified DESC ";
        System.out.println("Execute SQL : " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) > 0L) {
                if (loRS.next()) {
                    if (loRS.getString("sModified") != null && !"".equals(loRS.getString("sModified"))) {
                        if (loRS.getString("sModified").length() > 10) {
                            lsEntry = getSysUser(poGRider.Decrypt(loRS.getString("sModified")),false);
                        } else {
                            lsEntry = getSysUser(loRS.getString("sModified"),false);
                        }
                        // Get the LocalDateTime from your result set
                        LocalDateTime dModified = loRS.getObject("dModified", LocalDateTime.class);
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");
                        lsEntryDate = dModified.format(formatter);
                    }
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            poJSON = setJSON("error", e.getMessage());
            return poJSON;
        }

        poJSON.put("result", "success");
        poJSON.put("sCompnyNm", lsEntry);
        poJSON.put("sEntryDte", lsEntryDate);
        return poJSON;
    }

    public String getSysUser(String fsId, boolean fbIsID) throws SQLException, GuanzonException {
        String lsEntry = "";
        String lsSQL =   " SELECT b.sCompnyNm, a.sEmployNo from xxxSysUser a "
                + " LEFT JOIN Client_Master b ON b.sClientID = a.sEmployNo ";
        lsSQL = MiscUtil.addCondition(lsSQL, " a.sUserIDxx =  " + SQLUtil.toSQL(fsId)) ;
        System.out.println("SQL " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            if (MiscUtil.RecordCount(loRS) > 0L) {
                if (loRS.next()) {
                    if(fbIsID) {
                        lsEntry = loRS.getString("sEmployNo");
                    } else {
                        lsEntry = loRS.getString("sCompnyNm");
                    }
                }
            }
            MiscUtil.close(loRS);
        } catch (SQLException e) {
            poJSON.put("result", "error");
            poJSON.put("message", e.getMessage());
        }
        return lsEntry;
    }


}
